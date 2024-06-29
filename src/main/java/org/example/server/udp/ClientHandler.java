package org.example.server.udp;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import static org.example.Config.*;

public class ClientHandler extends Thread { //UDPServerSide
    private final DatagramSocket udpSocket;

    public ClientHandler() {
        try {
            udpSocket = new DatagramSocket(UDP_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while (true) {
            byte[] buffer = new byte[65535];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                udpSocket.receive(packet);
                var handler = new UDPResponseHandler(packet, udpSocket);
                Thread handlerThread = new Thread(handler);
                handlerThread.start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}


