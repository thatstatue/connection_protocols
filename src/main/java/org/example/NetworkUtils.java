package org.example;

import org.example.records.Chunk;
import org.example.records.TransferInfo;
import org.example.server.Database;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;


import static java.lang.System.out;
import static org.example.Config.*;
import static org.example.client.udp.PingClient.getTransferInfo;


public class NetworkUtils {
    public static String hashPassword(String password) {
        // Generate a salt and hash the password
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        // Check if the plaintext password matches the hashed password
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    public static boolean isUsernameAvailable(String username) {
        return !Database.getInstance().getClients().containsKey(username);
    }

    public static void showProgress(int count, int total) {
        System.out.println("\rProgress: %" + (100 - count * 100 / total) + " completed");
        try {
            Thread.sleep(500);
        }catch (InterruptedException e){
            throw new RuntimeException();
        }
    }

    public static Chunk getChunk(double fileSize) {
        TransferInfo transferInfo = getTransferInfo();

        int chunkSize = calculateChunkSize(transferInfo.bandwidth(), transferInfo.ping());
        int totalChunks = calculateTotalChunks(fileSize, chunkSize);

        return new Chunk(chunkSize, totalChunks);
    }



    public static int countOngoingThreads(ArrayList<Thread> threads) {
        int count = 0;
        for (Thread thread : threads) {
            if (thread.isAlive()) count++;
        }
        return count;
    }

    private static int calculateChunkSize(int bandwidth, int pingTimeMs) {

        double transferTime = Math.min(MAX_PING, pingTimeMs) / 100.0;
        if (bandwidth == MAX_BANDWIDTH) transferTime = 1;
        return (int) (bandwidth * transferTime);
    }

    private static int calculateTotalChunks(double fileSize, int chunkSize) {
        return (int) Math.ceil(fileSize / chunkSize);
    }
    public static void combineChunks(String fileID, String fileName, int totalChunks) throws IOException {
        File directory = new File(fileName);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File saveFile = new File(directory, fileID);
        getCombinedFile(fileName, totalChunks, saveFile, directory,true);
    }
    public static File combineChunks(String fileName, int totalChunks) throws IOException {
        File directory = new File(FILE_DIR);

        if (!directory.exists()) {
            directory.mkdirs();
        }
        File saveFile = new File(directory, fileName);

        return getCombinedFile(fileName, totalChunks, saveFile, directory, false);
    }

    private static File getCombinedFile(String fileName, int totalChunks, File saveFile, File directory, boolean delete) throws IOException {

        try (FileOutputStream fos = new FileOutputStream(saveFile)) {
            for (int i = 0; i < totalChunks; i++) {
                File chunkFile = new File(directory, "chunk_" + i + "_" + fileName);
                if (delete) chunkFile = new File(directory, "chunk_" + i + "_" + saveFile.getName());

                if (chunkFile.exists()) {
                    byte[] chunkData = new byte[(int) chunkFile.length()];
                    try (FileInputStream fis = new FileInputStream(chunkFile)) {
                        fis.read(chunkData);
                    }
                    fos.write(chunkData);
                    if (delete) chunkFile.delete();
                } else {
                    out.println("Missing chunk: " + i + " for file: " + fileName);
                    throw new IOException("Missing chunk: " + i);
                }
            }
        }
        return saveFile;
    }
}
