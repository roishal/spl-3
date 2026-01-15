package bgu.spl.net.impl.stomp;

import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Reactor;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        // TODO: implement this
    }

    public static <T> Server<T>  stompThreadPerClient(
            int port,
            Supplier<StompMessagingProtocol<T> > stompProtocolFactory,
            Supplier<MessageEncoderDecoder<T> > encoderDecoderFactory) {

        return new BaseServer<T>(port, null, stompProtocolFactory, encoderDecoderFactory) {
            @Override
            protected void execute(BlockingConnectionHandler<T>  handler) {
                new Thread(handler).start();
            }
        };

    }

        public static <T> Server<T> reactor(
            int nthreads,
            int port,
            Supplier<StompMessagingProtocol<T>> stompProtocolFactory,
            Supplier<MessageEncoderDecoder<T>> encoderDecoderFactory) {
        return new Reactor<T>(nthreads, port, null, stompProtocolFactory, encoderDecoderFactory);
    }
}
