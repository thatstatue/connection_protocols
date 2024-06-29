package org.example.server.udp;

import org.example.NetworkUtils;
import org.example.records.UniqueFile;
import org.example.server.Database;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;

import static java.lang.System.out;
import static org.example.Config.*;

public class UDPResponseHandler implements Runnable {
    private final DatagramPacket packet;
    private final DatagramSocket udpSocket;

    public UDPResponseHandler(DatagramPacket packet, DatagramSocket udpSocket) {
        this.packet = packet;
        this.udpSocket = udpSocket;
    }

    @Override
    public void run() {
        String command = new String(packet.getData(), 0, packet.getLength());
        String[] parts = command.split("#");
        String action = parts[0];

        try {
            if (REQ_UPLOAD.equals(action)) handleUpload(parts);
            else if (REQ_DOWNLOAD.equals(action)) handleDownload(parts);
            else if (REQ_VIEW.equals(action)) handleViews();
            else if (REQ_FILE_SIZE.equals(action)) handleSize(parts);
            else if (REQ_TRANSFER_INFO.equals(action)) handleTransferInfo();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSize(String[] parts) throws IOException {
        String fileID = parts[1];
        var uniqueFile = Database.getInstance().getFileFromID(fileID);
        String size = RES_ERR;
        if (uniqueFile!= null) {
            size = String.valueOf(uniqueFile.data().length);
        }
        DatagramPacket responsePacket = new DatagramPacket(size.getBytes(), size.length(), packet.getAddress(), packet.getPort());
        udpSocket.send(responsePacket);

    }
    private void handleUpload(String[] parts) throws IOException {
        String id = parts[1];
        int chunkIndex = Integer.parseInt(parts[2]);
        int totalChunks = Integer.parseInt(parts[3]);
        int chunkSize = Integer.parseInt(parts[4]);

        byte[] chunkData = new byte[chunkSize];
        String chunkString = parts[5];
        byte[] chunkStringBytes = chunkString.getBytes();

        int lengthToCopy = Math.min(chunkStringBytes.length, chunkSize);
        System.arraycopy(chunkStringBytes, 0, chunkData, 0, lengthToCopy);

        File directory = new File(FILE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File file = new File(directory, "chunk_" + chunkIndex + "_" + id);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(chunkData);
        }
        out.println("Received chunk index: " + chunkIndex + " for file: " + id);

        // send acknowledgment
        String ack = "ACK";
        DatagramPacket ackPacket = new DatagramPacket(ack.getBytes(), ack.length(), packet.getAddress(), packet.getPort());
        udpSocket.send(ackPacket);

        // combine if all chunks are received
        if (areAllChunksReceived(directory, id, totalChunks)) {
            out.println("All chunks received, combining...");
            combineChunks(id, totalChunks);
        }


    }
    private boolean areAllChunksReceived(File directory, String fileName, int totalChunks) {
        for (int i = 0; i < totalChunks; i++) {
            File chunkFile = new File(directory, "chunk_" + i + "_" + fileName);
            if (!chunkFile.exists()) {
                out.println("Missing chunk: " + i + " for file: " + fileName);
                return false;
            }
        }
        return true;
    }


    private void handleDownload(String[] parts) throws IOException {
        String fileName = parts[1];
        int chunkIndex = Integer.parseInt(parts[2]);

        File directory = new File(FILE_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "chunk_" + chunkIndex + "_" + fileName);
        if (!file.exists()) {
            System.out.println("File: " + file.getAbsolutePath() + " does not exist");
            return;
        }

        byte[] chunkData = new byte[(int) file.length()];

        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(chunkData);
        }

        DatagramPacket responsePacket = new DatagramPacket(chunkData, chunkData.length, packet.getAddress(), packet.getPort());
        udpSocket.send(responsePacket);
    }
    private void handleViews() throws IOException {
        String list = Database.getInstance().getUploadedFiles();
        DatagramPacket responsePacket = new DatagramPacket(list.getBytes(), list.length(), packet.getAddress(), packet.getPort());
        udpSocket.send(responsePacket);
    }
    private void combineChunks(String fileName, int totalChunks) throws IOException {
        File saveFile = NetworkUtils.combineChunks(fileName, totalChunks);
        saveToDatabase(saveFile, fileName);
    }
    private void saveToDatabase(File file, String id ) {
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var uniqueFile = new UniqueFile(fileBytes,id);
        Database.getInstance().addNewUpload( uniqueFile);
        out.println("MESSAGE : file is uploaded with the id " + uniqueFile.id());
    }
    public void handleTransferInfo() {
        try {
            // echo the packet back to the client
            DatagramPacket sendPacket = new DatagramPacket(
                    packet.getData(), packet.getLength(),
                    packet.getAddress(), packet.getPort());
            udpSocket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}