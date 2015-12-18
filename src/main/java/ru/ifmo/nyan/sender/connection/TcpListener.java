package ru.ifmo.nyan.sender.connection;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class TcpListener extends NetListener<ServerSocket> {
    public TcpListener(Consumer<byte[]> dataConsumer) throws IOException {
        super(0, dataConsumer);  // with random port
    }

    @Override
    protected ServerSocket createSocket(int port) throws IOException {
        return new ServerSocket(port);
    }

    @Override
    protected byte[] receive(ServerSocket serverSocket) throws IOException {
        Socket socket = serverSocket.accept();
        try {
            InputStream in = socket.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            IOUtils.copy(in, bos);
            return bos.toByteArray();
        } catch (IOException e) {
            logger.debug("Reading from TCP socket failed", e);
        }
        return null;
    }

    @Override
    public int getListeningPort() {
        return getSocket().getLocalPort();
    }

    public void changePort() {
        changePort(0);
    }
}
