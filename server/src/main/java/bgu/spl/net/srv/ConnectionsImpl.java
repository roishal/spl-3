package bgu.spl.net.srv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections <T >
{
    private final ConcurrentMap<Integer, ConnectionHandler<T>> active;
    private final ConcurrentMap<String, Set<Integer>> channelToIds;
    private final ConcurrentMap<Integer, Set<String>> idToChannels;
    private AtomicInteger counter;

    public ConnectionsImpl(){
        active = new ConcurrentHashMap<>();
        channelToIds = new ConcurrentHashMap<>();
        idToChannels = new ConcurrentHashMap<>();
        counter = new AtomicInteger();
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = active.get(connectionId);
        if (handler!=null){
            try{
                handler.send(msg);
            }
            catch(Exception e){
                System.out.println("disconnecting client" + connectionId);
                disconnect(connectionId);
                return false;
            }
            return true;
            }
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        Set<Integer> clients = channelToIds.get(channel);
        if (clients!=null){
            for (Integer id : clients){
            send(id, msg);
        }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        ConnectionHandler<T> handler = active.remove(connectionId);
        if (handler!=null){
            try{
                handler.close();
            }
            catch(Exception e){
                System.out.println("handler error" + e);
            }
        }
        Set<String> channels = idToChannels.remove(connectionId);
        if (channels!=null){
            for (String channel : channels){
                unSubscribe(channel, connectionId);
        }
        }
    }

   public void subscribe(int connectionId, String channel) {
        channelToIds.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(connectionId);
        idToChannels.computeIfAbsent(connectionId, k -> ConcurrentHashMap.newKeySet()).add(channel);
    }

    public int connectToActive(ConnectionHandler<T> handler) {
        int connectionId = counter.incrementAndGet();
        active.putIfAbsent(connectionId, handler);
        idToChannels.putIfAbsent(connectionId, ConcurrentHashMap.newKeySet());
        return connectionId;
    }



    public void unSubscribe(String channel, int connectionId){
            Set<Integer> clients = channelToIds.get(channel);
            if (clients!=null){
                clients.remove(connectionId);
                if (clients.isEmpty()){
                    channelToIds.remove(channel, clients);
                }
            }


    }

}
