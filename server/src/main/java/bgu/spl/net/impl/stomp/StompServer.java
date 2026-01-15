package bgu.spl.net.impl.stomp;

import java.util.function.Supplier;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.impl.echo.LineMessageEncoderDecoder;
import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.srv.BlockingConnectionHandler;
import bgu.spl.net.srv.Reactor;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        // 1. בדיקת תקינות ארגומנטים
        if (args.length < 2) {
            System.out.println("Usage: java StompServer <port> <server_type(tpc/reactor)>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        String serverType = args[1];
        Server<String> server = null; 

        // 2. יצירת השרת עם הגדרת טיפוס מפורשת <String>
        if (serverType.equals("tpc")) {
            server = StompServer.<String>stompThreadPerClient(
                    port,
                    () -> new StompProtocol(), 
                    LineMessageEncoderDecoder::new //message encoder decoder factory
            );
        } else if (serverType.equals("reactor")) {
            server = StompServer.<String>reactor(
                    Runtime.getRuntime().availableProcessors(),
                    port,
                    () -> new StompProtocol(), 
                    LineMessageEncoderDecoder::new //message encoder decoder factory
            );
        } else {
            System.out.println("Server type must be 'tpc' or 'reactor'");
            System.exit(1);
        }

        // 3. הרצת השרת
        if (server != null) {
            System.out.println("Server started on port " + port + " (" + serverType + ")");
            server.serve();
        }
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
