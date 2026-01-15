#include "../include/StompProtocol.h"
#include <iostream>
#include <sstream>
#include <vector>

StompProtocol::StompProtocol(ConnectionHandler& connectionHandler) 
    : connectionHandler(connectionHandler), 
      shouldTerminate(false),
      topicToId(), 
      subIdCounter(0), 
      receiptIdCounter(0) {}

void StompProtocol::run() {
    while (!shouldTerminate) {
        std::string ans;
        if (!connectionHandler.getFrameAscii(ans, '\0')) {
            std::cout << "Disconnected from server" << std::endl;
            shouldTerminate = true;
            break;
        }
        std::cout << ans << std::endl;
    }
}

void StompProtocol::sendFrame(std::string frame) {
    if (!connectionHandler.sendFrameAscii(frame, '\0')) {
        std::cout << "Error sending frame" << std::endl;
        shouldTerminate = true;
    }
}

bool StompProtocol::processInput(std::string line) {
    std::stringstream ss(line);
    std::string command;
    ss >> command;

    if (command == "join") {
        std::string topic;
        ss >> topic;
        
        // יצירת מזהים חדשים
        int id = ++subIdCounter;
        int receipt = ++receiptIdCounter;

        // שמירה במילון לצורך יציאה עתידית
        topicToId[topic] = id;

        // בניית הפריים
        std::string frame = "SUBSCRIBE\n";
        frame += "destination:/" + topic + "\n";
        frame += "id:" + std::to_string(id) + "\n";
        frame += "receipt:" + std::to_string(receipt) + "\n";
        frame += "\n";

        sendFrame(frame);
        std::cout << "Sent SUBSCRIBE request for topic: " << topic << " (id: " << id << ")" << std::endl;
    }
    else if (command == "exit") {
        std::string topic;
        ss >> topic;

        if (topicToId.find(topic) != topicToId.end()) {
            int id = topicToId[topic];
            int receipt = ++receiptIdCounter;

            std::string frame = "UNSUBSCRIBE\n";
            frame += "id:" + std::to_string(id) + "\n";
            frame += "receipt:" + std::to_string(receipt) + "\n";
            frame += "\n";

            sendFrame(frame);
            topicToId.erase(topic); // מחיקה מהמילון
            std::cout << "Sent UNSUBSCRIBE request for topic: " << topic << std::endl;
        } else {
            std::cout << "Error: Not subscribed to topic " << topic << std::endl;
        }
    }
    else if (command == "logout") {
        int receipt = ++receiptIdCounter;
        std::string frame = "DISCONNECT\n";
        frame += "receipt:" + std::to_string(receipt) + "\n";
        frame += "\n";
        
        sendFrame(frame);
        return false; // מסמן ל-Main לעצור את הלולאה ולחכות לניתוק
    }
    else if (command == "send") {
        // מימוש בסיסי ל-SEND (אופציונלי כרגע)
        std::string topic;
        std::string message;
        ss >> topic;
        std::getline(ss, message); // קריאת שאר השורה כהודעה
        
        // הסרת רווח מיותר בהתחלה אם יש
        if (!message.empty() && message[0] == ' ') message = message.substr(1);

        std::string frame = "SEND\n";
        frame += "destination:/" + topic + "\n";
        frame += "\n";
        frame += message + "\n"; // גוף ההודעה
        
        sendFrame(frame);
    }
    else {
        // סתם הודעה שהמשתמש כתב ולא פקודה מוכרת
        std::cout << "Unknown command" << std::endl;
    }

    return true; // להמשיך לרוץ
}