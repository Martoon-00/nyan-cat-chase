package ru.ifmo.nyan.sender.connection;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.function.Consumer;

public class UdpListener extends NetListener<MulticastSocket> {
    public UdpListener(int port, Consumer<byte[]> dataConsumer) throws IOException {
        super(port, dataConsumer);
    }

    @Override
    protected MulticastSocket createSocket(int port) throws IOException {
        return new MulticastSocket(port);
    }

    @Override
    protected byte[] receive(MulticastSocket socket) throws IOException {
        byte[] bytes = new byte[1500];
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
        socket.receive(packet);

        return Arrays.copyOf(packet.getData(), packet.getLength());
    }

    public void changePort(int newPort) {
        super.changePort(newPort);
    }


    public void joinGroup(InetAddress address) throws IOException {
        getSocket().joinGroup(address);
    }

    public void leaveGroup(InetAddress address) throws IOException {
        getSocket().leaveGroup(address);
    }

}
