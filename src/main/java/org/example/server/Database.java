package org.example.server;

import org.example.NetworkUtils;
import org.example.records.UniqueFile;
import org.example.records.User;

import java.util.*;

public class Database {
    private static Database database;

    public  Map<String,User> getClients() {
        return clients;
    }

    private static final Map<String, User> clients = Collections.synchronizedMap(new HashMap<>());

    private final List<UniqueFile> uploadedFiles = new ArrayList<>();

    public void addNewUpload( UniqueFile uniqueFile) {
        uploadedFiles.add(uniqueFile);
    }

    public UniqueFile getFileFromID( String id) {

        for (UniqueFile uniqueFile : uploadedFiles) {
            if (uniqueFile.id().equals(id)) return uniqueFile;
        }
        return null;
    }


    public synchronized void createAccount(String username, User user) {
        clients.put(username, user);
    }

    public static Database getInstance() {
        if (database == null) database = new Database();
        return database;
    }
    public String getUploadedFiles() {
        final String[] ans = {""};
        for (UniqueFile file : uploadedFiles){
            ans[0] +=  "\t" + file.id() + "\n";
        }
        return ans[0];
    }

    public synchronized boolean isEnteredUserVerified(String username, String password) {
        for (Map.Entry<String, User> entry : clients.entrySet()) {
            if (entry.getKey().equals(username) &&
                    NetworkUtils.checkPassword(password, entry.getValue().hashedPassword())) {
                return true;
            }
        }
        return false;
    }
}
