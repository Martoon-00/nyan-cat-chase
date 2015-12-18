package ru.ifmo.nyan.sender.connection;

import com.sun.istack.internal.Nullable;
import ru.ifmo.nyan.sender.main.IpAddress;

public class SendInfo {
    @Nullable
    public final IpAddress ipAddress;
    public final byte[] data;

    public final Runnable failListener;

    public SendInfo(IpAddress ipAddress, byte[] data, Runnable failListener) {
        this.ipAddress = ipAddress;
        this.data = data;
        this.failListener = failListener;
    }
}
