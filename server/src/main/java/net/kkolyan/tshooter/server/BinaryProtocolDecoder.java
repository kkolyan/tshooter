package net.kkolyan.tshooter.server;

import net.kkolyan.tshooter.protocol.Decoder;
import net.kkolyan.tshooter.protocol.DecoderImpl;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class BinaryProtocolDecoder implements ProtocolDecoder {
    private Decoder decoder = new DecoderImpl();
    private int expected;
    private IoBuffer buffer = IoBuffer.allocate(1024);

    public BinaryProtocolDecoder() {
        buffer.setAutoExpand(true);
        buffer.clear();
    }

    @Override
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        while (true) {
            if (in.remaining() < 0) {
                break;
            }
            if (expected == 0) {
                expected = in.getInt();
                continue;
            }
            int rem = buffer.remaining();
            if (buffer.remaining() > expected) {

            }
        }
    }

    @Override
    public void finishDecode(IoSession session, ProtocolDecoderOutput out) throws Exception {
    }

    @Override
    public void dispose(IoSession session) throws Exception {
    }
}
