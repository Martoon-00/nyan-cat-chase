package ru.ifmo.nyan.sender.main;

import java.util.function.Consumer;

public class RequestHandler<Req extends RequestMessage<Res>, Res extends ResponseMessage> extends MessageHandler<Req> {
    private final Consumer<Res> callback;
    private boolean answered;

    public RequestHandler(MessageSender sender, MessageContainer<Req> container, Consumer<Res> callback) {
        super(sender, container);
        this.callback = callback;
    }

    public void answer(Res answer) {
        if (answered)
            throw new IllegalStateException("Answer has already be sent");

        answered = true;
        callback.accept(answer);
    }

}
