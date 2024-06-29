package org.example.server;



import org.example.server.tcp.TCPServer;

import java.io.IOException;

import static org.example.Config.SERVER_PORT;

public class ServerApplication implements Runnable{
    @Override
    public void run() {
        try {
            TCPServer server = new TCPServer(SERVER_PORT); //extends Thread
            server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
