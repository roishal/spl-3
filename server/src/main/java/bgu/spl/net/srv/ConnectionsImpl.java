package bgu.spl.net.srv;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsImpl<T> implements Connections <T >
{
    private final ConcurrentMap<Integer, ConnectionHandler<T>> active;
    private final ConcurrentMap<String, ConcurrentMap<Integer, String>> channelToIds;
    private final ConcurrentMap<Integer, ConcurrentMap<String, String>> idToChannels;
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
    public void send(String channel, T msg) { //msg will be String type
        ConcurrentMap<Integer, String> clients = channelToIds.get(channel);
        if (clients != null) {
        clients.forEach((connectionId, subscriptionId) -> {
            String msgStr = ((String) msg).replaceFirst("\n\n", "\nsubscription:" + subscriptionId + "\n\n");
            send(connectionId, (T) msgStr);
        });
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
        ConcurrentMap<String, String> subscriptions = idToChannels.remove(connectionId);
        if (subscriptions!=null){
            subscriptions.forEach((subscriptionId, channel) -> {
            subscriptions.remove(subscriptionId);
            if (channel != null) {
                ConcurrentMap<Integer, String> clients = channelToIds.get(channel);
                if (clients != null) {
                    clients.remove(connectionId);
                    if (clients.isEmpty()) {
                        channelToIds.remove(channel); 
                    }
                    }
                }
            });
        }
        
    }

   public void subscribe(int connectionId, String channel, String subscriptionId) {
        channelToIds.computeIfAbsent(channel, k -> new ConcurrentHashMap<>()).put(connectionId, subscriptionId);    
        idToChannels.get(connectionId).put(subscriptionId, channel);
    }

    public int connectToActive(ConnectionHandler<T> handler) {
        int connectionId = counter.incrementAndGet();
        active.putIfAbsent(connectionId, handler);
        idToChannels.putIfAbsent(connectionId, new ConcurrentHashMap<>());
        return connectionId;
    }



    public void unsubscribe(int connectionId, String subscriptionId){
        ConcurrentMap<String, String> subscriptions = idToChannels.get(connectionId);
        if (subscriptions != null) {
            String channel = subscriptions.remove(subscriptionId);
            if (channel != null) {
                ConcurrentMap<Integer, String> clients = channelToIds.get(channel);
                if (clients != null) {
                    clients.remove(connectionId);
                    if (clients.isEmpty()) {
                        channelToIds.remove(channel); 
                    }
                }
            }
        }
    }

}
