#include <stdlib.h>
#include <ConnectionHandler.h>
#include <iostream>
#include <thread>
#include <vector>
#include <StompProtocol.h>

std::string loginFrame(std::string username, std::string password) {
    std::string frame = "CONNECT\n";
    frame += "accept-version:1.2\n";
    frame += "host:stomp.cs.bgu.ac.il\n";
    frame += "login:" + username + "\n";
    frame += "passcode:" + password + "\n";
    frame += "\n";
    return frame;
}

int main(int argc, char *argv[]) {
    ConnectionHandler* connectionHandler = nullptr;

    while (true) {
		std::string line;
        std::getline(std::cin, line);

        if (line.length() > 0) {
            std::stringstream data(line);
            std::string command;
            std::string ip;
            std::string username;
            std::string password;
            data >> command >> ip >> username >> password;

            if (command == "login") {
                if (connectionHandler != nullptr) {
                    delete connectionHandler;
                    connectionHandler = nullptr;
                }

                std::size_t divider = ip.find(':');
                std::string host = ip.substr(0, divider);
                short port = (short) std::stoi(ip.substr(divider + 1));

                connectionHandler = new ConnectionHandler(host, port);

                if (!connectionHandler->connect()) {
                    std::cerr << "Could not connect to server" << std::endl;
                    delete connectionHandler;
                    connectionHandler = nullptr;
                    continue;
                }
                connectionHandler->sendFrameAscii(loginFrame(username, password), '\0');
                std::string answer;
                connectionHandler->getFrameAscii(answer, '\0');
                if (answer.find("CONNECTED") != std::string::npos) {
                    std::cout << "Login successful!" << std::endl;

					StompProtocol protocol(*connectionHandler);
					std::thread t1(&StompProtocol::run, &protocol);
					while (true) {
						std::string cmdLine;
                        std::getline(std::cin, cmdLine);
                        if (!protocol.processInput(cmdLine)) {
                            break; 
                        }
					}
					t1.join();
					delete connectionHandler;
                    connectionHandler = nullptr;
                    std::cout << "Client disconnected" << std::endl;
                }	else {
                    	std::cout << "Login failed: " << answer << std::endl;
                }
            }
        }
    }
    return 0;
}