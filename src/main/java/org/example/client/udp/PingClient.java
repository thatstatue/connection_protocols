package org.example.client.udp;

import org.example.Config;
import org.example.records.TransferInfo;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static org.example.Config.MAX_BANDWIDTH;
import static org.example.Config.REQ_TRANSFER_INFO;

public class PingClient {
    public static TransferInfo getTransferInfo() {
        long duration = Config.MAX_PING;
        int bandwidth = 1;
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress address = InetAddress.getByName(Config.SERVER_IP_ADDRESS);
            byte[] sendBuffer = (REQ_TRANSFER_INFO+ "#test").getBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, Config.UDP_PORT);
            long startTime = System.currentTimeMillis();
            socket.send(sendPacket);

            byte[] receiveBuffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            socket.receive(receivePacket);
            long endTime = System.currentTimeMillis();

            if ((endTime - startTime) < Config.MAX_PING) {
                duration = endTime - startTime;
            }

            int packetCount = 1000;
            startTime = System.currentTimeMillis();

            for (int i = 0; i < packetCount; i++) {
                socket.send(sendPacket);
            }

            endTime = System.currentTimeMillis();

            long totalData = 1024 * packetCount; // total data in bytes
            bandwidth = (int)((totalData / 1024.0 / 1024.0) / ((endTime - startTime) / 1000));

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new TransferInfo((int) duration, Math.min(Math.max(bandwidth, 100), MAX_BANDWIDTH));

    }
}