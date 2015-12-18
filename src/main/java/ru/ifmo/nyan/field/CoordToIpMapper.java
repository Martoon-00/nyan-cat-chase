package ru.ifmo.nyan.field;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CoordToIpMapper implements CoordToIp {
    @Override
    public InetAddress toIp(Coord coord) {
        if (coord.x < 0 || coord.x >= 256 || coord.y < 0 || coord.y >= 256)
            throw new IllegalArgumentException(String.format("Coordinates value should have size of byte, but got %s", coord));

        byte[] bytes = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)  // first byte is highest
                .put((byte) 225)
                .put((byte) 0)
                .put((byte) coord.x)
                .put((byte) coord.y)
                .array();

        try {
            return InetAddress.getByAddress(bytes);
        } catch (UnknownHostException e) {
            throw new Error("Failed to convert coordinates to address", e);
        }
    }

    @Override
    public Coord toCoord(InetAddress ip) {
        throw new UnsupportedOperationException("lol");
    }
}
