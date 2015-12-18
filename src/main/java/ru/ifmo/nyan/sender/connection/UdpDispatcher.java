package ru.ifmo.nyan.sender.connection;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.Optional;

public class UdpDispatcher extends NetDispatcher {
    public static final int MAX_PACKET_SIZE = 1500;

    private InetAddress broadcastAddress;

    public UdpDispatcher(NetworkInterface networkInterface) throws SocketException {
        try {
            broadcastAddress = networkInterface.getInterfaceAddresses().stream()
                    .map(InterfaceAddress::getBroadcast)
                    .filter(Objects::nonNull)
                    .findAny()
                    .orElse(Inet4Address.getByName("255.255.255.255"));

        } catch (UnknownHostException e) {
            throw new Error("Unexpected exception", e);
        }

    }

    /**
     * Set sendInfo.address as <tt>null</tt> to broadcast
     */
    @Override
    protected void submit(SendInfo sendInfo) throws IOException {
        if (sendInfo.data.length > MAX_PACKET_SIZE) {
            logger.warn(String.format("Attempt to send UDP data of more than %d bytes. Data may get truncated", MAX_PACKET_SIZE));
        }

        InetAddress address = Optional.ofNullable(sendInfo.ipAddress.address)
                .orElse(broadcastAddress);

        try (MulticastSocket socket = new MulticastSocket()) { // TODO: do not create socket every time
            socket.setTimeToLive(1);
            DatagramPacket packet = new DatagramPacket(sendInfo.data, sendInfo.data.length, address, sendInfo.ipAddress.port);
            socket.send(packet);
        }
    }

}
