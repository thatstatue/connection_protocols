package org.example.server.tcp;

import org.example.*;
import org.example.records.User;
import org.example.server.Database;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import java.util.Scanner;

import static java.lang.System.out;
import static org.example.Config.REQ_LOGIN;
import static org.example.Config.REQ_SIGNUP;
import static org.example.NetworkUtils.hashPassword;

public class TCPResponseHandler implements Runnable{
    private final Socket socket;
    private User user;

    private Scanner socketScanner;
    private PrintWriter socketPrintWriter;

    public TCPResponseHandler(Socket socket) throws IOException {
        this.socket = socket;
        prepareStream();
    }


    private void prepareStream() throws IOException {
        socketScanner = new Scanner(socket.getInputStream());
        socketPrintWriter = new PrintWriter(socket.getOutputStream(), true);
    }
    @Override
    public void run(){
        boolean isLoggedIn = false;
        while (!isLoggedIn && socketScanner.hasNextLine()) {
            String req = socketScanner.nextLine();
            if (req.equals("\n") || req.isEmpty()) req = socketScanner.nextLine();
            switch (req) {
                case REQ_SIGNUP -> obtainAccount();
                case REQ_LOGIN -> isLoggedIn = loginAccount();
                default -> out.println(req + "is an undefined command!");
            }
        }
    }

    private void obtainAccount() {
        boolean assigned = false;
        while (!assigned) {
            String proposedUsername = socketScanner.nextLine();
            System.out.println("User proposed username: " + proposedUsername);
            if(NetworkUtils.isUsernameAvailable(proposedUsername)) {
                socketPrintWriter.println(Connection.available.name());
                System.out.println("Username " + proposedUsername + " is assigned.");
                String proposedPassword = socketScanner.nextLine();

                user = new User(proposedUsername, hashPassword(proposedPassword));

                Database.getInstance().createAccount(proposedUsername, user);


                assigned = true;
            } else {
                socketPrintWriter.println("unavailable");
                System.out.println("Username " + proposedUsername + " is not available.");

            }
        }

    }

    public boolean loginAccount(){
        String enteredUsername = socketScanner.nextLine();
        String enteredPassword = socketScanner.nextLine();
        var clients = Database.getInstance().getClients();
        if(clients.containsKey(enteredUsername)) {
            if(Database.getInstance().isEnteredUserVerified(enteredUsername, enteredPassword)) {
                socketPrintWriter.println(Connection.verified.name());
                user = clients.get(enteredUsername);
                out.println("User " +enteredUsername + " is logged-in.");
                return true;
            }
        }
        socketPrintWriter.println("unverified");
        return false;
    }

}
