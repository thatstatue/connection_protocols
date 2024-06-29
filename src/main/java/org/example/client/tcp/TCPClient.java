package org.example.client.tcp;

import org.example.client.CLI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static org.example.server.AccountState.guest;
import static org.example.server.AccountState.signedIn;


public class TCPClient extends Thread{
    private final String serverIPAddress;
    private final Integer serverPort;

    private Socket clientSocket;
    Scanner socketScanner;
    PrintWriter socketPrintWriter;

    Scanner consoleScanner;

    private CLI CLI;

    private void initSocket() throws IOException {
        clientSocket  = new Socket(serverIPAddress,serverPort);
    }

    private void initIOStreams() throws IOException {
        socketScanner = new Scanner(clientSocket.getInputStream());
        socketPrintWriter = new PrintWriter(clientSocket.getOutputStream(), true);
    }

    public TCPClient(String serverIPAddress, int serverPort) throws IOException {
        this.serverIPAddress=serverIPAddress;
        this.serverPort=serverPort;

        consoleScanner = new Scanner(System.in);
        initSocket();
        initIOStreams();

    }
    public void mainPage(){
        try {
            CLI = new CLI(clientSocket);

            do {
                CLI.guestPage();
            }while (CLI.getAccountState().equals(guest));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void accountPage(){
        do {
            try {
                CLI.homePage();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }while (CLI.getAccountState().equals(signedIn));
    }

    @Override
    public void run() {
        if(clientSocket.isConnected()) {
            mainPage();
            accountPage();
        }
    }

}
