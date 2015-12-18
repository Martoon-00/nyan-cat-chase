package ru.ifmo.nyan.sender.listeners;


import ru.ifmo.nyan.sender.main.Message;
import ru.ifmo.nyan.sender.main.MessageHandler;

@FunctionalInterface
public interface ReceiveListener<ReplyType extends Message, Handler extends MessageHandler<ReplyType>> {
    /**
     * Action performed when got an answer
     */
    void onReceive(Handler handler);
}
