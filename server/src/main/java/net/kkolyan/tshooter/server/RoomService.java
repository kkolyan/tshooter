package net.kkolyan.tshooter.server;

import net.kkolyan.tshooter.protocol.messages.JoinRoom;
import net.kkolyan.tshooter.protocol.messages.RoomState;

import java.math.BigInteger;

/**
 * @author nplekhanov
 */
public class RoomService {

    @HandleMessage
    public void joinRoom(JoinRoom o) {}
    public void m2() {}

    public void m3() {}

    @HandleMessage
    public void showRoomState(RoomState o) {}
}
