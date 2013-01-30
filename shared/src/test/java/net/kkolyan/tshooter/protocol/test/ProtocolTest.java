package net.kkolyan.tshooter.protocol.test;

import net.kkolyan.tshooter.protocol.*;
import net.kkolyan.tshooter.protocol.messages.*;
import org.junit.Assert;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
* @author nplekhanov
*/
public class ProtocolTest {

    private Decoder decoder = new DecoderImpl();
    private Encoder encoder = new EncoderImpl();

    @Test
    public void testJoinRoom() throws IOException {
        JoinRoom joinRoom = new JoinRoom();
        joinRoom.setRoomIndex(45);

        JoinRoom m = encodeDecode(joinRoom);
        assertEquals(joinRoom.getRoomIndex(), m.getRoomIndex());
        assertNotSame(joinRoom, m);
    }

    @Test
    public void test2() throws IOException {
        RoomState state = new RoomState();
        state.setIndex(67);
        state.setName("Name of the room");
        state.setMessages(Arrays.asList("A","Z","Q"));
        state.setPorts(Arrays.asList(8080,443,3306));
        state.setPlayers(new ArrayList<Player>());
        {
            Player player = new Player();
            player.setHp(78);
            player.setName("Player 1");
            player.setPosition(vector3f(16.432F, 16.90F, 543.78F));
            player.setWeapons(Arrays.asList(weapon("pistol",12),weapon("rifle",34)));
            player.setIndex(0);
            state.getPlayers().add(player);
        }
        {
            Player player = new Player();
            player.setHp(65);
            player.setName("Player 2");
            player.setPosition(vector3f(163.432F, 17F, 43.783F));
            player.setWeapons(Arrays.asList(weapon("pistol",20),weapon("shotgun",70),weapon("rpg", 120)));
            player.setIndex(0);
            state.getPlayers().add(player);
        }
        {
            Player player = new Player();
            player.setHp(16);
            player.setName("Player 3");
            player.setPosition(vector3f(186.432F, 1.90F, 53.78F));
            player.setWeapons(Arrays.asList(weapon("pistol", 12)));
            player.setIndex(0);
            state.getPlayers().add(player);
        }
        RoomState m = encodeDecode(state);
        assertNotSame(state, m);
        assertEquals(state.getIndex(), m.getIndex());
        assertEquals(state.getName(), m.getName());
        assertEquals(state.getPorts(), m.getPorts());
        assertEquals(state.getMessages(), m.getMessages());
        assertEquals(state.getPlayers().size(), m.getPlayers().size());
        for (int pid = 0; pid < state.getPlayers().size(); pid ++) {
            Player expP = state.getPlayers().get(pid);
            Player actP = m.getPlayers().get(pid);
            assertEquals(expP.getPosition().getX(), actP.getPosition().getX(), 0.001);
            assertEquals(expP.getPosition().getY(), actP.getPosition().getY(), 0.001);
            assertEquals(expP.getPosition().getZ(), actP.getPosition().getZ(), 0.001);
            assertEquals(expP.getHp(), actP.getHp(), 0.001);
            assertEquals(expP.getName(), actP.getName());
            assertEquals(expP.getWeapons().size(), actP.getWeapons().size());
            for (int wid = 0; wid < expP.getWeapons().size(); wid ++) {
                Weapon expW = expP.getWeapons().get(wid);
                Weapon actW = actP.getWeapons().get(wid);
                assertEquals(expW.getDamage(), actW.getDamage(), 0.001);
                assertEquals(expW.getName(), actW.getName());
            }

        }

    }

    private Vector3f vector3f(float x, float y, float z) {
        Vector3f v = new Vector3f();
        v.setX(x);
        v.setY(y);
        v.setZ(z);
        return v;
    }

    private Weapon weapon(String name, float damage) {
        Weapon w = new Weapon();
        w.setDamage(damage);
        w.setName(name);
        return w;
    }

    private <T> T encodeDecode(T o) throws IOException {
        IoBuffer buffer = IoBuffer.allocate(4096);
        buffer.clear();
        encoder.encode(o, buffer);
        buffer.flip();
        return (T) decoder.decode(buffer);
    }
}
