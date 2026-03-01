package com.lsm;

import java.util.Scanner;

public class Main {
  public static void main(String[] args) {
    MemTable db = new MemTable();

    Scanner scanner = new Scanner(System.in);

    System.out.println("Custom database has started!");
    System.out.println("Please choose among the available options");
    System.out.println("*****************************************");
    System.out.println("* 1. PUT                                *");
    System.out.println("* 2. GET                                *");
    System.out.println("* 3. DELETE                             *");
    System.out.println("* 4. STOP                               *");
    System.out.println("*****************************************");

    boolean firstCommandFlag = false;
    while (true) {

      if (firstCommandFlag)
        System.out.println("Choose another command");
      firstCommandFlag = true;

      String command = scanner.nextLine();

      switch (command.toUpperCase()) {
        case "PUT":
          System.out.println("Please input key");
          String key = scanner.nextLine();

          System.out.println("Please input value associated with your key");
          String value = scanner.nextLine();

          db.put(key, value);
          break;
        case "GET":
          System.out.println("What value do you want to retreive, please input a key");
          String getKey = scanner.nextLine();

          String result = db.get(getKey);
          System.out.println("value ----> " + result);
          break;
        case "DELETE":
          System.out.println("What value do you want to delete. please give a key");
          String deleteKey = scanner.nextLine();

          db.delete(deleteKey);
          break;
        case "STOP":
          db.flush();
          scanner.close();
          return;
        default:
          System.out.println("Invalid command, please choose one of the given options");
      }
    }
  }
}
