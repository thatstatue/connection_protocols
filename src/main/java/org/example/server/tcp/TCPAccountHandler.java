package org.example.server.tcp;

import java.io.IOException;
import java.net.Socket;

public class TCPAccountHandler extends Thread {
    private final Socket socket;
    public TCPAccountHandler(Socket socket){
        this.socket = socket;
    }
    @Override
    public void run(){

        TCPResponseHandler requestHandler = null;
        try {
            requestHandler = new TCPResponseHandler(socket);
            requestHandler.run();//finishes when user gets logged in
            System.out.println("Client logged into their account");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
