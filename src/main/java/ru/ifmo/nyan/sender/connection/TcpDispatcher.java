package ru.ifmo.nyan.sender.connection;

import java.io.IOException;
import java.net.Socket;

public class TcpDispatcher extends NetDispatcher {

    @Override
    protected void submit(SendInfo sendInfo) throws IOException {
        try (Socket socket = new Socket(sendInfo.ipAddress.address, sendInfo.ipAddress.port)) {
            socket.getOutputStream()
                    .write(sendInfo.data);
        }
    }
}
