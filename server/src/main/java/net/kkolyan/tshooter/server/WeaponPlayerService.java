package net.kkolyan.tshooter.server;

import net.kkolyan.tshooter.protocol.messages.Player;
import net.kkolyan.tshooter.protocol.messages.Weapon;

import java.io.OutputStream;

/**
 * @author nplekhanov
 */
public class WeaponPlayerService {

    @HandleMessage
    public void giveWeapon(Weapon o) {}

    public void m2() {}

    @HandleMessage
    public void addPlayer(Player o) {}

    public void m4() {}
}
