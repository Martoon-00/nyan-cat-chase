package ru.ifmo.nyan.field;

import java.net.InetAddress;

public interface CoordToIp {
    InetAddress toIp(Coord coord);

    Coord toCoord(InetAddress ip);
}
