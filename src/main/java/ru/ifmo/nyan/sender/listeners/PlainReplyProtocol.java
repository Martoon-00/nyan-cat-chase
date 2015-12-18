package ru.ifmo.nyan.sender.listeners;

import com.sun.istack.internal.Nullable;
import ru.ifmo.nyan.sender.main.RequestHandler;
import ru.ifmo.nyan.sender.main.RequestMessage;
import ru.ifmo.nyan.sender.main.ResponseMessage;

public interface PlainReplyProtocol<RequestType extends RequestMessage<ReplyType>, ReplyType extends ResponseMessage>
        extends ReplyProtocol<RequestType, ReplyType>{

    @Nullable
    ReplyType makeResponse(RequestType type);

    @Override
    default ReplyType makeResponse(RequestHandler<RequestType, ReplyType> handler) {
        return makeResponse(handler.getMessage());
    }
}
