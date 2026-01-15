package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Map;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsImpl;

public class StompProtocol implements StompMessagingProtocol<String> {

    private boolean shouldTerminate = false;
    private int connectionId = -1;
    private Connections<String> connections = null;

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public void process(String message) {
        StompFrame frame = splitMessage(message);
        switch (frame.command) {
            case "CONNECT":
                //SQL
                ((ConnectionsImpl<String>) connections).send(connectionId, connectedFrame());
                
                break;
            case "DISCONNECT":
                ((ConnectionsImpl<String>) connections).disconnect(connectionId);
                ((ConnectionsImpl<String>) connections).send(connectionId, reciptFrame(frame.headers.get("receipt")));
                break;
            case "SUBSCRIBE":
               ((ConnectionsImpl<String>) connections).subscribe(connectionId, frame.headers.get("destination"), frame.headers.get("id"));
                break;
            case "UNSUBSCRIBE":
               ((ConnectionsImpl<String>) connections).unsubscribe(connectionId, frame.headers.get("id"));
                break;
            case "SEND":
                
                break;
            default:
                break;
        }
    }

    private String errorFrame(String messageHeader, String descriptionBody, String originalMessage) {
        return "ERROR\n" +
            "message:" + messageHeader + "\n" +
            "\n" +
            "The message:\n" +
            "-----\n" +
            originalMessage + "\n" +
            "-----\n" +
            descriptionBody + "\n" + 
            "\u0000";
}

    private String connectedFrame() {
        return "CONNECTED\n" +
                "version:1.2" + "\n" +
                "\n" +
                "\u0000";
    }

    private String reciptFrame(String receiptId) {
        return "RECEIPT\n" +
                "receipt-id:" + receiptId + "\n" +
                "\n" +
                "\u0000";
    }

    private StompFrame splitMessage(String message) {
        StompFrame frame = new StompFrame();
        String[] lines = message.split("\n"); 
    
        if (lines.length > 0) {
            frame.command = lines[0].trim(); 
        }
        int currentLine = 1; // skip the command line
        while (currentLine < lines.length && !lines[currentLine].isEmpty()) {
            String[] headerParts = lines[currentLine].split(":");
            if (headerParts.length == 2) {
                frame.headers.put(headerParts[0].trim(), headerParts[1].trim());
            }
            currentLine++;
        }
        currentLine++; //skip the empty line before body

        String body = "";
        for (int i = currentLine; i < lines.length; i++) {
            body = body + lines[i];
            if (i < lines.length - 1) { //add the new line
                body = body + "\n";
            }
        }
        frame.body = body.replace("\u0000", ""); // remove null sign
        return frame;
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
}

class StompFrame {
    String command;
    Map<String, String> headers = new HashMap<>();
    String body;
}
