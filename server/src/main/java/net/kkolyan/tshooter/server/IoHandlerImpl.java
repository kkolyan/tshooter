package net.kkolyan.tshooter.server;

import net.kkolyan.tshooter.protocol.DispatchingMessageVisitor;
import net.kkolyan.tshooter.protocol.MessageVisitor;
import net.kkolyan.tshooter.protocol.VisitableMessage;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IoHandlerImpl implements IoHandler, Scope {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final ThreadLocal<IoSession> currentSession = new ThreadLocal<IoSession>();

    private static final Object SESSION_LOCK_KEY = new NamedObject("SESSION_LOCK_KEY") ;
    private static final Object SESSION_DESTRUCTION_CALLBACK_QUEUE_KEY = new NamedObject("SESSION_DESTRUCTION_CALLBACK_QUEUE_KEY");
    private static final Object CLIENT_ID_KEY = new NamedObject("CLIENT_ID_KEY");

    private final Map<String,ClientData> clientDataMap = new ConcurrentHashMap<String, ClientData>();

    private MessageVisitor dispatchingVisitor;

    @Override
    public void sessionCreated(IoSession ioSession) throws Exception {
    }

    private static class ClientData {
        String clientId;
        IoSession connection;
        IoSession unreliableChannel;
        Map<String,Object> context = new HashMap<String, Object>();
    }

    @Override
    public void sessionOpened(IoSession ioSession) throws Exception {
        ioSession.setAttribute(SESSION_LOCK_KEY, new Object());
        ioSession.setAttribute(SESSION_DESTRUCTION_CALLBACK_QUEUE_KEY, new ConcurrentLinkedQueue());
    }

    public void registerClientConnection(String clientId) {
        IoSession ioSession = currentSession.get();
        ioSession.setAttribute(CLIENT_ID_KEY, clientId);
    }

    public void registerClientUnreliableChannel(String clientId) {

    }

    @Override
    public void sessionClosed(IoSession ioSession) throws Exception {
        @SuppressWarnings("unchecked")
        Queue<Runnable> queue = (Queue) ioSession.getAttribute(SESSION_DESTRUCTION_CALLBACK_QUEUE_KEY);
        while (true) {
            Runnable callback = queue.poll();
            if (callback == null) {
                break;
            }
            try {
                callback.run();
            } catch (Exception e) {
                logger.warn("destruction callback of " + ioSession + " failed - ignoring.", e);
            }
        }
    }

    @Override
    public void exceptionCaught(IoSession ioSession, Throwable throwable) throws Exception {
        logger.error(ioSession.toString(), throwable);
    }

    @Override
    public void messageReceived(IoSession ioSession, Object o) throws Exception {
        currentSession.set(ioSession);
        try {
            VisitableMessage message = (VisitableMessage) o;
            message.acceptVisitor(dispatchingVisitor);
        } finally {
            currentSession.remove();
        }
    }

    @Override
    public void messageSent(IoSession ioSession, Object o) throws Exception {
    }

    @Override
    public void sessionIdle(IoSession ioSession, IdleStatus idleStatus) throws Exception {
    }

    @Override
    public Object get(String name, ObjectFactory<?> objectFactory) {
        IoSession ioSession = currentSession.get();

        Object sessionLock = ioSession.getAttribute(SESSION_LOCK_KEY);
        synchronized (sessionLock) {
            Object bean = ioSession.getAttribute(name);
            if (bean == null) {
                bean = objectFactory.getObject();
                ioSession.setAttribute(name, bean);
            }
            return bean;
        }
    }

    @Override
    public Object remove(String name) {
        throw new UnsupportedOperationException("bean removing unsupported");
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
        IoSession ioSession = currentSession.get();
        @SuppressWarnings("unchecked")
        Queue<Runnable> queue = (Queue) ioSession.getAttribute(SESSION_DESTRUCTION_CALLBACK_QUEUE_KEY);
        queue.offer(callback);
    }

    @Override
    public Object resolveContextualObject(String key) {
        return currentSession.get();
    }

    @Override
    public String getConversationId() {
        return currentSession.get().getId() + "";
    }

    @Resource
    public void setDispatchingVisitor(MessageVisitor dispatchingVisitor) {
        this.dispatchingVisitor = dispatchingVisitor;
    }
}
