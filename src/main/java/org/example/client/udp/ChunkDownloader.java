package org.example.client.udp;
import org.example.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.Arrays;

import static org.example.Config.UDP_PORT;

public class ChunkDownloader implements Runnable {
    private final String fileName;
    private final int chunkIndex;
    private final String savePath;

    public ChunkDownloader(String fileName, int chunkIndex, String savePath) {
        this.fileName = fileName;
        this.chunkIndex = chunkIndex;
        this.savePath = savePath;
    }

    @Override
    public void run() {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(Config.SERVER_IP_ADDRESS);
            String header = Config.REQ_DOWNLOAD + "#" + fileName + "#" + chunkIndex;
            byte[] headerBytes = header.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(headerBytes, headerBytes.length, address, Config.UDP_PORT);
            udpSocket.send(requestPacket);

            byte[] buffer = new byte[65535];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(responsePacket);

            byte[] chunkData = Arrays.copyOf(responsePacket.getData(), responsePacket.getLength());

            File directory = new File(savePath);
            if (!directory.exists()) {
                System.out.println("it doesn't exist");
                return;
            }

            File file = new File(directory, "chunk_" + chunkIndex + "_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(chunkData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}