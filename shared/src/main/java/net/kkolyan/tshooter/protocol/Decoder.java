package net.kkolyan.tshooter.protocol;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;

public interface Decoder {
    Object decode(IoBuffer in) throws IOException;
}
