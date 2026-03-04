package com.lsm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.nio.*;

public class Server {

  private final MemTable db;
  private final WriteAheadLog wal;
  private static final int PORT = 8080;

  public Server(MemTable db, WriteAheadLog wal) {
    this.db = db;
    this.wal = wal;
  }

  public void start() throws IOException {
    // open and reserve port on your machine
    ServerSocket serverSocket = new ServerSocket(PORT);
    System.out.println("started server on port: " + PORT);

    while (true) {

      // block code until connection is established
      Socket clientSocket = serverSocket.accept();
      System.out.print("Client has connected");

      BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

      String command;
      while ((command = in.readLine()) != null) {

        switch (command.toUpperCase()) {
          case "PUT":
            String key = in.readLine();
            String value = in.readLine();
            wal.append("PUT", key, value);
            db.put(key, value);
            break;
          case "GET":
            String getKey = in.readLine();
            String result = db.get(getKey);

            out.println("");
            out.println(result != null ? "value is: " + result : "RESULT WAS NOT FOUND");
            break;
          case "DELETE":
            String deleteKey = in.readLine();
            wal.append("DELETE", deleteKey, null);
            db.delete(deleteKey);
            break;
          case "STOP":
            db.flush();
            return;
          default:
            out.println("Invalid command, please choose one of the given options");
        }
      }
      System.out.println("Client disconnected!");
      clientSocket.close();
    }
  }
}
