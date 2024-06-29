package org.example.server.tcp;

import org.example.server.udp.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPServer extends Thread{

    ServerSocket serverSocket;
    Integer serverPort;



    public TCPServer(Integer serverPort) throws IOException {
        this.serverPort = serverPort;
        serverSocket = new ServerSocket(serverPort);
    }

    @Override
    public void run() {
        var clientHandler = new ClientHandler();
        clientHandler.start();
        while(true) {

            //create a socket to pair with the client
            Socket socket;
            try {
                socket = serverSocket.accept();
                System.out.println("A client connected with Address: " + socket.getRemoteSocketAddress());
                new TCPAccountHandler(socket).start();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
