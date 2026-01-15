#pragma once

#include "../include/ConnectionHandler.h"

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    ConnectionHandler& connectionHandler;
    bool shouldTerminate;
    std::map<std::string, int> topicToId; 
    int subIdCounter;
    int receiptIdCounter;
public:
    StompProtocol(ConnectionHandler& connectionHandler) 
            : connectionHandler(connectionHandler), shouldTerminate(false) {}
    void run(); 
    bool processInput(std::string line);
    void sendFrame(std::string frame);
};
