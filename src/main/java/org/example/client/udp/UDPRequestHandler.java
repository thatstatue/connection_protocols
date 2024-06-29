package org.example.client.udp;

import org.example.records.Chunk;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

import static org.example.Config.*;
import static org.example.NetworkUtils.*;

public class UDPRequestHandler {


    public Chunk handleChunkInfo(long fileSize){
        Chunk chunk;
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address= InetAddress.getByName(SERVER_IP_ADDRESS);
            byte[] headerBytes = (REQ_TRANSFER_INFO + "#check").getBytes();
            DatagramPacket requestPacket = new DatagramPacket(headerBytes, headerBytes.length, address, UDP_PORT);//todo
            udpSocket.send(requestPacket);
            chunk = getChunk(fileSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return chunk;
    }

    public String handleView(DatagramSocket udpSocket) throws IOException {
        InetAddress address = InetAddress.getByName(SERVER_IP_ADDRESS);
        byte[] headerBytes = (REQ_VIEW+ "#all").getBytes();
        DatagramPacket requestPacket = new DatagramPacket(headerBytes, headerBytes.length, address, UDP_PORT);
        udpSocket.send(requestPacket);

        byte[] buffer = new byte[65535];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        udpSocket.receive(responsePacket);

        return new String(responsePacket.getData(), 0, responsePacket.getLength());
    }
    public void handleDownload(Chunk chunk, String fileID, String savePath) {
        ArrayList<Thread> downloadThreads = new ArrayList<>(chunk.totalChunks());

        for (int i = 0; i < chunk.totalChunks(); i++) {
            var downloadThread = new Thread(new ChunkDownloader( fileID, i, savePath));
            downloadThreads.add(downloadThread);
            downloadThread.start();
        }

        while (countOngoingThreads(downloadThreads) > 0) {
            showProgress(countOngoingThreads(downloadThreads), downloadThreads.size());
        }
    }

    public long requestFileSize(String fileId) throws IOException {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName(SERVER_IP_ADDRESS);
            String header = REQ_FILE_SIZE + "#" + fileId;
            byte[] headerBytes = header.getBytes();
            DatagramPacket requestPacket = new DatagramPacket(headerBytes, headerBytes.length, address, UDP_PORT);
            udpSocket.send(requestPacket);

            byte[] buffer = new byte[65535];
            DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(responsePacket);

            String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
            if (response.startsWith(RES_ERR)) {
                return -1;
            }
            String[] parts = response.split("#");
            return Long.parseLong(parts[0]);
        }
    }


    public ArrayList<Thread> startUploadThreads(double fileSize, File file) {
        Chunk chunk = handleChunkInfo((long) fileSize);
        byte[] fileData;
        try {
            fileData = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ArrayList<Thread> threads = new ArrayList<>();
        String id = UUID.randomUUID().toString();
        for (int i = 0; i < chunk.totalChunks(); i++) {

            int start = i * chunk.chunkSize();
            int end = Math.min(start + chunk.chunkSize(), fileData.length);
            byte[] chunkData = new byte[end - start];
            System.arraycopy(fileData, start, chunkData, 0, end - start);
            var chunkUploader = new ChunkUploader( id, i, chunk.totalChunks(), chunkData);
            chunkUploader.start();
            threads.add(chunkUploader);

        }
        return threads;
    }
}
