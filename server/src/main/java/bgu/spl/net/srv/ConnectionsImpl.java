package bgu.spl.net.srv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T>  implements Connections<T> {
    private final ConcurrentHashMap<Integer, ConnectionHandler<T>> active = new ConcurrentHashMap<>(
    private AtomicInteger counter;
    private final Set<T> clientMap;
    
    @Override
    public boolean send(int connectionId, T msg) {
    }

    @Override
    public void send(String channel, T msg) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'send'");
    }

    @Override
    public void disconnect(int connectionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'disconnect'");
    }
    
}
