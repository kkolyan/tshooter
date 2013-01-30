package net.kkolyan.tshooter.protocol;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;

/**
 * @author nplekhanov
 */
public interface Encoder {
    void encode(Object o, IoBuffer stream) throws IOException;
}
