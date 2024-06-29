package org.example.client.udp;

import org.example.Config;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import static org.example.Config.REQ_UPLOAD;

public class ChunkUploader extends Thread {
    private final String fileName;
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] chunkData;

    public ChunkUploader(String fileName, int chunkIndex, int totalChunks, byte[] chunkData) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
    }

    @Override
    public void run() {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(Config.SERVER_IP_ADDRESS);
            String header = REQ_UPLOAD + "#" + fileName+ "#" +
                    chunkIndex + "#" + totalChunks + "#" + chunkData.length + "#";
            byte[] headerBytes = header.getBytes();
            byte[] packetData = new byte[headerBytes.length + chunkData.length];
            System.arraycopy(headerBytes, 0, packetData, 0, headerBytes.length);
            System.arraycopy(chunkData, 0, packetData, headerBytes.length, chunkData.length);
            DatagramPacket packet = new DatagramPacket(packetData, packetData.length, address, Config.UDP_PORT);

            acknowledgePacket(udpSocket, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void acknowledgePacket(DatagramSocket udpSocket, DatagramPacket packet) throws IOException {
        boolean acknowledged = false;
        while (!acknowledged) {
            udpSocket.send(packet);

            byte[] ackBuffer = new byte[256];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            try {
                udpSocket.setSoTimeout(1000);
                udpSocket.receive(ackPacket);
                String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if (ack.equals("ACK")) {
                    acknowledged = true;
                }
            } catch (SocketTimeoutException e) {
                // resend if acknowledgment not received within timeout
//                System.out.println("Resending chunk index: " + chunkIndex);
            }
        }
    }
}