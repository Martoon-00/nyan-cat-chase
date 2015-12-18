package ru.ifmo.nyan.sender.main;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Stores the same as InetSocketAddress
 */
public class IpAddress {
    public final InetAddress address;
    public final int port;

    public IpAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public IpAddress(int port) {
        this.address = null;
        this.port = port;
    }

    public static IpAddress valueOf (InetSocketAddress addr) {
        return new IpAddress(addr.getAddress(), addr.getPort());
    }

    public InetSocketAddress toInetSocketAddress(){
        return new InetSocketAddress(address, port);
    }

    @Override
    public String toString() {
        return toInetSocketAddress().toString();
    }
}
