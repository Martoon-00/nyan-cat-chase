package ru.ifmo.nyan.sender.main;

import org.apache.log4j.Logger;
import ru.ifmo.nyan.misc.Colorer;
import ru.ifmo.nyan.sender.connection.*;
import ru.ifmo.nyan.sender.listeners.Cancellation;
import ru.ifmo.nyan.sender.listeners.ReceiveListener;
import ru.ifmo.nyan.sender.listeners.ReplyProtocol;
import ru.ifmo.nyan.sender.message.MessageIdentifier;
import ru.ifmo.nyan.sender.message.ReminderMessage;
import ru.ifmo.nyan.sender.util.Serializer;
import ru.ifmo.nyan.sender.util.StreamUtil;
import ru.ifmo.nyan.sender.util.UniqueValue;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * API for Message sending (lower) layer.
 */
public class MessageSender implements Closeable {
    private static Logger logger = Logger.getLogger(MessageSender.class);

    private ExecutorService executor = Executors.newCachedThreadPool();

    private final BlockingQueue<MessageContainer> received = new LinkedBlockingQueue<>();

    private final TcpListener tcpListener;
    private final UdpListener udpListener;
    private final TcpDispatcher tcpDispatcher;
    private final UdpDispatcher udpDispatcher;

    private final UniqueValue unique;
    private final InetAddress listeningAddress;

    private final Serializer serializer = new Serializer();

    private final Semaphore freezeControl = new Semaphore(1);

    private final Scheduler scheduler = new Scheduler();

    private final Collection<ReplyProtocol> replyProtocols = new ConcurrentLinkedQueue<>();
    private final Map<MessageIdentifier, Consumer<MessageContainer>> responseWaiters = new ConcurrentHashMap<>();
    private BlockingQueue<Runnable> toProcess = new LinkedBlockingQueue<>();


    public MessageSender(NetworkInterface networkInterface, int listeningUdpPort, UniqueValue unique) throws IOException {
        List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
        if (inetAddresses.isEmpty())
            throw new IllegalArgumentException(String.format("Network interface %s has no inet addresses", networkInterface));
        this.listeningAddress  = inetAddresses.stream()
                .filter(inetAddress -> inetAddress instanceof Inet4Address)
                .findFirst().orElseGet(() -> inetAddresses.get(0));  // TODO: It seems to work only with IP-v4 :(

        this.unique = unique;

        logger.info(Colorer.format("Initiating", Colorer.Format.PLAIN));
        printLegend();
        freeze();
        executor.submit(udpListener = new UdpListener(listeningUdpPort, this::acceptMessage));
        executor.submit(tcpListener = new TcpListener(this::acceptMessage));
        executor.submit(udpDispatcher = new UdpDispatcher(networkInterface));
        executor.submit(tcpDispatcher = new TcpDispatcher());
        executor.submit(new IncomeMessagesProcessor());
        executor.submit(new MainProcessor());
    }

    public MessageSender(NetworkInterface networkInterface, int listeningUdpPort) throws IOException {
        this(networkInterface, listeningUdpPort, UniqueValue.getLocal(networkInterface));
    }

    private void printLegend() {
        logger.info(Colorer.paint("----------------------------------------------------------", Colorer.Format.BLACK));
        logger.info("Sender legend:");
        logger.info(String.format("%s for sending UDP", ColoredArrows.UDP));
        logger.info(String.format("%s for sending UDP broadcasts", ColoredArrows.UDP_BROADCAST));
        logger.info(String.format("%s for sending TCP", ColoredArrows.TCP));
        logger.info(String.format("%s for sending to self", ColoredArrows.LOOPBACK));
        logger.info(String.format("%s for received", ColoredArrows.RECEIVED));
        logger.info(String.format("%s for imitating message received again", ColoredArrows.RERECEIVE));
        logger.info(Colorer.paint("----------------------------------------------------------", Colorer.Format.BLACK));
    }

    /**
     * Simply sends message, waits for result during some sensible time.
     * <p>
     * Current thread is blocked during method call.
     *
     * @param address     receiver of message
     * @param message     mail entry
     * @param type        way of sending a message: TCP, single UPD...
     * @param timeout     timeout in milliseconds
     * @param <ReplyType> response message type
     * @return response message
     * @throws SendingException when timeout exceeded
     */
    public <ReplyType extends ResponseMessage> ReplyType sendAndExpect(IpAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout) throws SendingException {
        return sendAndWait(address, message, type, timeout)
                .orElseThrow(() -> new SendingException(address));
    }

    /**
     * Same as <tt>sendAndExpect</tt>, but in case of no answer returns empty Optional instead of throwing exception
     */
    public <ReplyType extends ResponseMessage> Optional<ReplyType> sendAndWait(IpAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout) {
        try {
            MessageContainer<ReplyType> response = submit(address, message, type, timeout, () -> {
            })
                    .poll(timeout, TimeUnit.MILLISECONDS);
            // TODO: make smarter last parameter into submit

            return Optional.ofNullable(response).map(messageContainer -> (messageContainer.message));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }


    /**
     * Sends a message.
     * <p>
     * Current thread is NOT blocked by this method call.
     * But no two response-actions (onReceive or onTimeout on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     * <p>
     * You may not specify port when sending UDP
     *
     * @param address         receiver of message
     * @param message         mail entry
     * @param type            way of sending a message: TCP, single UPD...
     * @param timeout         timeout in milliseconds
     * @param receiveListener an action to invoke when got an answer
     * @param onFail          an action to invoke when timeout exceeded and no message has been received
     * @param <ReplyType>     response message type
     */
    public <ReplyType extends ResponseMessage> void send(
            IpAddress address,
            RequestMessage<ReplyType> message,
            DispatchType type,
            int timeout,
            ReceiveListener<ReplyType, ResponseHandler<ReplyType>> receiveListener,
            Runnable onFail
    ) {
        // 0 for idling, -1 for fail, 1 for received
        AtomicInteger ok = new AtomicInteger();
        // remember current processing queue, otherwise fail timeout can put its task after freezing to new queue
        BlockingQueue<Runnable> processingQueue = this.toProcess;

        submit(address, message, type, timeout, response -> {
            if (ok.compareAndSet(0, 1)) {
                processingQueue.offer(() -> receiveListener.onReceive(new ResponseHandler<>(this, response)));
            }
        }, onFail);

        scheduler.schedule(timeout, () -> {
            if (ok.compareAndSet(0, -1)) {
                processingQueue.offer(onFail::run);
            }
        });
    }

    public <ReplyType extends ResponseMessage> void send(
            IpAddress address, RequestMessage<ReplyType> message, DispatchType type,
            int timeout, ReceiveListener<ReplyType, ResponseHandler<ReplyType>> receiveListener
    ) {
        send(address, message, type, timeout, receiveListener, () -> {
        });
    }

    /**
     * Sends a broadcast, and returns stream of answers which would be collected during timeout.
     * <p>
     * Node will receive its own request.
     * But you can detect self-sent message: response.getIdentifier().unique.equals(sender.getUnique())
     * <p>
     * Note, that this method doesn't block the thread, but accessing elements of result stream does (in lazy way).
     *
     * @param message     mail entry
     * @param timeout     timeout in milliseconds
     * @param <ReplyType> responses type
     * @return stream of replies
     */
    public <ReplyType extends ResponseMessage> Stream<ReplyType> broadcastAndWait(RequestMessage<ReplyType> message, int timeout) {
        BlockingQueue<MessageContainer<ReplyType>> responseContainer = submit(null, message, DispatchType.UDP, timeout, () -> {
        });
        // TODO: make smart last param in submit
        return StreamUtil.fromBlockingQueue(responseContainer, timeout)
                .map(messageContainer -> (messageContainer.message));
    }

    /**
     * Sends a broadcast.
     * <p>
     * Node will receive its own request.
     * But you can detect self-sent message: response.getIdentifier().unique.equals(sender.getUnique())
     * <p>
     * Current thread is NOT blocked by this method call.
     * But no two response-actions (onReceive or onTimeout on any request) or response protocols will be executed at same time,
     * so you can write not thread-safe code inside them.
     *
     * @param message         mail entry
     * @param timeout         timeout in milliseconds
     * @param receiveListener is executed when get a response
     * @param onTimeout       is executed when timeout expires. No receiveListener will be invoked after this.
     *                        Note that this listener is invoked even if no message has been received
     * @param <ReplyType>     response type
     */
    public <ReplyType extends ResponseMessage> void broadcast(int port, RequestMessage<ReplyType> message, int timeout, ReceiveListener<ReplyType, ResponseHandler<ReplyType>> receiveListener, Runnable onTimeout) {
        AtomicBoolean timeoutExpired = new AtomicBoolean();
        BlockingQueue<Runnable> processingQueue = this.toProcess;

        Runnable onFinish = () -> {
            timeoutExpired.set(true);
            processingQueue.offer(onTimeout::run);
        };

        submit(new IpAddress(port), message, DispatchType.UDP, timeout, response -> {
            if (timeoutExpired.get()) return;
            processingQueue.offer(() -> receiveListener.onReceive(new ResponseHandler<>(this, response)));
        }, onFinish::run);

        scheduler.schedule(timeout, onFinish);
    }

    public <ReplyType extends ResponseMessage> void broadcast(int port, RequestMessage<ReplyType> message, int timeout, ReceiveListener<ReplyType, ResponseHandler<ReplyType>> receiveListener) {
        broadcast(port, message, timeout, receiveListener, () -> {
        });
    }

    public <ReplyType extends ResponseMessage> void broadcast(int port, RequestMessage<ReplyType> message) {
        broadcast(port, message, 0, (handler) -> {
        });
    }


    /**
     * Sends message to itself in specified delay.
     * <p>
     * Used to schedule some tasks and execute them sequentially with other response-actions.
     * Executed action must be specified as response protocol.
     *
     * @param message reminder message
     * @param delay   when to send a mention
     */
    public void remind(ReminderMessage message, int delay) {
        Runnable remindTask = () -> send(null, message, DispatchType.LOOPBACK, 10000, (handler) -> {
        });
        scheduler.schedule(delay, remindTask);
    }

    void receiveAgain(MessageContainer msgContainer) {
        if (msgContainer.message.logOnReceive()) {
            logger.info(ColoredArrows.RERECEIVE + String.format(" %s", msgContainer.message));
        }
        received.offer(msgContainer);
    }

    private <ReplyType extends ResponseMessage> BlockingQueue<MessageContainer<ReplyType>> submit(IpAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout, Runnable onFail) {
        LinkedBlockingQueue<MessageContainer<ReplyType>> container = new LinkedBlockingQueue<>();
        submit(address, message, type, timeout, container::offer, onFail);
        return container;
    }

    /**
     * Puts identifier into message,
     * puts message into output queue,
     * puts reply consumer to responseWaiters (and scheduling its removal)
     */
    private <ReplyType extends ResponseMessage> void submit(IpAddress address, RequestMessage<ReplyType> message, DispatchType type, int timeout, Consumer<MessageContainer<ReplyType>> onReceive, Runnable onFail) {
        MessageIdentifier identifier = new MessageIdentifier(unique);
        MessageContainer msgContainer = new MessageContainer<>(identifier, getUdpListenerAddress(), message);
        responseWaiters.put(identifier, responseMessage -> {
                    try {
                        //noinspection unchecked
                        onReceive.accept(responseMessage);
                    } catch (ClassCastException e) {
                        logger.warn("Accepted message of wrong type", e);
                    }
                }
        );

        forwardToDispatcher(address, msgContainer, type, onFail);
        scheduler.schedule(timeout, () -> responseWaiters.remove(identifier));
    }

    /**
     * Determines behaviour on receiving request-message of specified type.
     * <p>
     * No any two response protocols or response-actions will be executed at the same time.
     *
     * @param protocol way on response on specified request-message
     * @return function to unregister this protocol.
     */
    public <Q extends RequestMessage<A>, A extends ResponseMessage> Cancellation registerReplyProtocol(ReplyProtocol<Q, A> protocol) {
        replyProtocols.add(protocol);
        return () -> replyProtocols.remove(protocol);
    }

    private void forwardToDispatcher(IpAddress address, MessageContainer msgContainer, DispatchType dispatchType, Runnable failListener) {
        boolean whetherLog = msgContainer.message.logOnSend();

        if (dispatchType == DispatchType.LOOPBACK) {
            if (whetherLog)
                logger.info(ColoredArrows.LOOPBACK + String.format(" %s", msgContainer.message));
            received.offer(msgContainer);
            return;
        }

        if (dispatchType == DispatchType.UDP) {
            if (whetherLog) {
                if (address == null) {
                    logger.info(ColoredArrows.UDP_BROADCAST + String.format(" %s", msgContainer.message));
                } else {
                    logger.info(ColoredArrows.UDP + String.format(" %s: %s", address, msgContainer.message));
                }
            }
            udpDispatcher.send(makeSendInfo(address, msgContainer, failListener));
        } else if (dispatchType == DispatchType.TCP) {
            if (whetherLog)
                logger.info(ColoredArrows.TCP + String.format(" %s: %s", address, msgContainer.message));
            tcpDispatcher.send(makeSendInfo(address, msgContainer, failListener));
        } else {
            throw new IllegalArgumentException("Can't process dispatch type of " + dispatchType);
        }
    }

    private SendInfo makeSendInfo(IpAddress address, MessageContainer msgContainer, Runnable failListener) {
        return new SendInfo(address, serializer.serialize(msgContainer), failListener);
    }

    private void acceptMessage(byte[] bytes) {
        try {
            MessageContainer msgContainer = (MessageContainer) serializer.deserialize(bytes);
            Message message = msgContainer.message;
            if (message instanceof RequestMessage && msgContainer.identifier.unique.equals(unique))
                return;  // skip if sent by someone with same unique value; loopback messages are put to processing queue directly and hence not lost

            received.offer(msgContainer);
        } catch (IOException | ClassCastException e) {
            logger.trace(Colorer.paint("??", Colorer.Format.RED) + " Got some trash", e);
        }
    }

    /**
     * Freezes request-messages receiver.
     * <p>
     * In frozen state no any response protocol is activated, all received request-messages are stored and not processed
     * until unfreezing. So you can safely change response protocols without scaring of missing any request.
     * <p>
     * Sender is initiated in frozen state
     * <p>
     * Call of this method also destroys all registered response protocols and response-actions of send- and broadcastAndWait
     * methods
     * <p>
     * Caution: this method MUST be invoked inside response-action, in order to avoid unexpected concurrent effects
     */
    public void freeze() {
        freezeControl.acquireUninterruptibly();
        responseWaiters.clear();  // responses are put here only from MainProcessor, but we are inside its execution
        scheduler.abort();
        replyProtocols.clear();

        BlockingQueue<Runnable> processingQueue = this.toProcess;
        this.toProcess = new LinkedBlockingQueue<>();
        // Unblock processing thread
        processingQueue.offer(() -> {
        });

        logger.info(Colorer.paint("***", Colorer.Format.BLUE) + " Freeze");
    }

    /**
     * Unfreezes request-messages receiver. Messages received in frozen state begin to be processed.
     */
    public void unfreeze() {
        logger.info(Colorer.paint("&&&", Colorer.Format.CYAN) + " Defrost");
        freezeControl.release();
    }

    public UniqueValue getUnique() {
        return unique;
    }

    public InetSocketAddress getUdpListenerAddress() {
        return new InetSocketAddress(listeningAddress, udpListener.getListeningPort());
    }

    public InetSocketAddress getTcpListenerAddress() {
        return new InetSocketAddress(listeningAddress, tcpListener.getListeningPort());
    }

    public NodeInfo getNodeInfo() {
        return new NodeInfo(unique, listeningAddress);
    }

    @Override
    public void close() throws IOException {
        logger.info(Colorer.paint("Shutdown", Colorer.Format.PLAIN));
        scheduler.shutdownNow();
        executor.shutdownNow();
        udpListener.close();
        tcpListener.close();
    }

    public void joinGroup(InetAddress address) throws IOException {
        udpListener.joinGroup(address);
    }

    public void leaveGroup(InetAddress address) throws IOException {
        udpListener.leaveGroup(address);
    }


    private class IncomeMessagesProcessor implements Runnable {
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    MessageContainer msgContainer = received.take();
                    Message message = msgContainer.message;

                    // if in frozen state, wait for unfreezing
                    freezeControl.acquire();
                    try {
                        if (message instanceof RequestMessage) {
                            if (message.logOnReceive())
                                logger.info(ColoredArrows.RECEIVED + String.format(" %s %s", msgContainer.identifier.unique, message));
                            toProcess.offer(() -> processRequestMsg(msgContainer));
                        } else if (message instanceof ResponseMessage) {
                            if (message.logOnReceive())
                                logger.info(ColoredArrows.RECEIVED + String.format(" %s", message));
                            processResponseMsg(msgContainer);
                        } else
                            logger.warn("Got message of unknown type: " + message.getClass().getSimpleName());
                    } finally {
                        freezeControl.release();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }

        private <R extends RequestMessage> void processRequestMsg(MessageContainer<R> requestContainer) {
            R request = requestContainer.message;
            for (ReplyProtocol replyProtocol : replyProtocols) {
                if (request.getClass().isAssignableFrom(replyProtocol.requestType())) {
                    //noinspection unchecked
                    answerWith(replyProtocol, requestContainer);
                    return;
                }
            }
            logger.trace(Colorer.format("%1`(ignored)%` %s", request));
        }

        private <R extends RequestMessage<A>, A extends ResponseMessage> void answerWith(ReplyProtocol<R, A> replyProtocol, MessageContainer<R> requestContainer) {
            LinkedList<ResponseMessage> answers = new LinkedList<>();

            // collect both answered via handler and return value
            Optional.ofNullable(
                    replyProtocol.makeResponse(new RequestHandler<>(MessageSender.this, requestContainer, answers::add))
            ).ifPresent(answers::add);

            if (answers.size() > 1) {
                logger.error(String.format("Got too much answers, only one will be sent (got %s)", answers));
            }

            if (!answers.isEmpty()) {
                ResponseMessage response = answers.element();
                MessageContainer responseContainer = new MessageContainer<>(requestContainer.identifier, getUdpListenerAddress(), response);
                forwardToDispatcher(IpAddress.valueOf(requestContainer.responseListenerAddress), responseContainer, DispatchType.UDP, () -> {
                });
            }
        }

        private void processResponseMsg(MessageContainer msgContainer) {
            Consumer<MessageContainer> responseWaiter = responseWaiters.get(msgContainer.identifier);
            if (responseWaiter != null) {
                responseWaiter.accept(msgContainer);
            }  // otherwise it has been removed due to timeout expiration
        }
    }

    private class MainProcessor implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    toProcess.take().run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable e) {
                    logger.trace("Processing threw an exception", e);
                }
            }
        }
    }

    private enum ColoredArrows {
        UDP_BROADCAST(Colorer.format("--%4`>%`--%4`>>%`--%4`>>%`")),
        UDP(Colorer.format("--%4`>%`--%4`>%`--%4`>%`")),
        TCP(Colorer.format("--%1`>%`--%1`>%`--%1`>%`")),
        LOOPBACK(Colorer.format("-%5`>%`---%5`<|%`  ")),
        RECEIVED(Colorer.format("%3`<%`--%3`<%`--%3`<%`--")),
        RERECEIVE(Colorer.format("-%6`<%`----%6`|%`  "));

        private final String text;

        ColoredArrows(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

}
