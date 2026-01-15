package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final StompMessagingProtocol<T> stompProtocol;
    private final MessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, MessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.stompProtocol = null;
    }

        public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader,  MessagingProtocol<T> protocol, StompMessagingProtocol<T> stompProtocol) {
        this.sock = sock;
        this.encdec = reader;
        this.stompProtocol = stompProtocol;
        this.protocol = null;
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;

            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (connected && (read = in.read()) >= 0 && !shouldTerminate()) {                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    if (protocol == null) {
                        stompProtocol.process(nextMessage);
                    } else {
                        T response = protocol.process(nextMessage);
                        if (response != null) {
                            out.write(encdec.encode(response));
                            out.flush();
                        }
                    }
                }
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    private boolean shouldTerminate() {
            if (protocol != null) return protocol.shouldTerminate();
            if (stompProtocol != null) return stompProtocol.shouldTerminate();
            return false;
        }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(T msg) {
        try {
            out.write(encdec.encode(msg));
            out.flush();

        } catch(Exception e) {
            System.out.println("errorrrr "  + e);
        }
    }
}
