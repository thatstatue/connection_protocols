package org.example.client;


import org.example.client.tcp.TCPClient;

import java.io.IOException;

import static org.example.Config.*;

public class ClientApplication implements Runnable{
    @Override
    public void run() {
        try {
            TCPClient client = new TCPClient(SERVER_IP_ADDRESS, SERVER_PORT);
            client.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}