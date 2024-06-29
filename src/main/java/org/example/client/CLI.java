package org.example.client;

import org.example.Connection;
import org.example.client.udp.UDPRequestHandler;
import org.example.records.Chunk;
import org.example.server.AccountState;


import java.io.*;
import java.net.DatagramSocket;
import java.net.Socket;


import java.util.Scanner;

import static java.lang.System.out;
import static org.example.Config.*;
import static org.example.NetworkUtils.*;

import static org.example.server.AccountState.guest;

public class CLI {
    private final Scanner consuleScanner, in;
    private final PrintWriter echo;
    private AccountState accountState = guest;
    private final UDPRequestHandler reqHandler;


    public CLI(Socket socket) throws IOException {
        consuleScanner = new Scanner(System.in);
        in = new Scanner(socket.getInputStream());
        echo = new PrintWriter(socket.getOutputStream(), true);
        reqHandler = new UDPRequestHandler();
    }

    public void guestPage() throws IOException {
        out.println("||GUEST PAGE||\n\nWelcome to MyDrive\n\t1- Log-In\t\t2- Sign-Up ");
        String req;
        do {
            req = consuleScanner.nextLine();

        } while (req.equals("\n") || req.isEmpty());

        echo.println(req);
        switch (req) {
            case (REQ_LOGIN) -> requestLogin();
            case (REQ_SIGNUP) -> requestSignup();
            default -> out.println("error: " + req + " is an undefined command");
        }

    }

    public void homePage() throws IOException {
        out.println("||HOME PAGE||\n\n\t1- Upload\t\t2- Download\t\t3-View history\t\t4-Logout ");
        String req;
        do {
            req = consuleScanner.nextLine();

        } while (req.equals("\n") || req.isEmpty());
        echo.println(req);
        switch (req) {
            case REQ_UPLOAD -> requestUpload();
            case REQ_DOWNLOAD -> requestDownload();
            case REQ_VIEW -> requestView();
            case REQ_LOGOUT -> accountState = guest;
            default -> out.println("error: " + req + " is an undefined command");
        }

    }

    private void requestDownload() throws IOException {
        out.println("||DOWNLOAD PAGE||\n\nProvide the file ID: ");
        String fileID = consuleScanner.nextLine();

        long fileSize;
        try {
            fileSize = reqHandler.requestFileSize(fileID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Chunk chunk = reqHandler.handleChunkInfo(fileSize);

        out.println("\nProvide the path you wanna save it to:  ");
        String savePath = consuleScanner.nextLine();

        reqHandler.handleDownload(chunk, fileID, savePath);

        out.println("Download complete, finishing up...");
        combineChunks(fileID, savePath, chunk.totalChunks());
        out.println("file saved to the path " + savePath);
    }




    private void requestView() throws IOException {
        try (DatagramSocket udpSocket = new DatagramSocket()) {
            String response = reqHandler.handleView(udpSocket);
            System.out.println("Files on server:\n" + response);
        }
    }


    private void requestUpload() {
        out.println("||UPLOAD PAGE||\n\nProvide your filepath: ");
        String filePath = consuleScanner.nextLine();
        File file = new File(filePath);
        long fileSize = file.length();
        if (file.exists()) {
            var threads = reqHandler.startUploadThreads((double) fileSize, file);
            while (countOngoingThreads(threads) > 0) {
                showProgress(countOngoingThreads(threads), threads.size());
            }
            System.out.println("upload completed");
        }else out.println("no such file found!");
    }

    private void requestSignup() {
        boolean assigned = false;
        while (!assigned) {
            out.println("||SIGNUP PAGE||\n\nPropose your userID: ");
            String proposedUsername = consuleScanner.next();
            echo.println(proposedUsername);
            String available = in.next();
            if (available.equals(Connection.available.name())) {
                out.println("Propose your password");
                String proposedPassword = consuleScanner.next();
                echo.println(proposedPassword);

                assigned = true;
                out.println("Account built successfully. Redirecting you to Login page...");
            } else {
                out.println("Username " + proposedUsername + " is not available, please retry.");
            }
        }
    }

    public AccountState getAccountState() {
        return accountState;
    }

    private void requestLogin() {
        out.println("||LOGIN PAGE||\n\nEnter your userID: ");
        String enteredUsername = consuleScanner.next();
        echo.println(enteredUsername);
        out.println("Enter your password: ");
        String enteredPassword = consuleScanner.next();
        echo.println(enteredPassword);

        String ans = in.next();
        if (ans.equals(Connection.verified.name())) {
            accountState = AccountState.signedIn;
            out.println("Login was successful. Redirecting you to your account page...");
        } else {
            out.println("Error: The provided username doesn't have an account.");
        }
    }
}
