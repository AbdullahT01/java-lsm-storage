package com.lsm;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.*;

public class WriteAheadLog {
  private final String WALFileName = "WAL.log";

  public WriteAheadLog() {
    System.out.println("Checking to see if there is anything to recover...");
  }

  public boolean checkWAL() {
    return Files.exists(Path.of(WALFileName));
  }

  public void append(String operation, String key, String value) {
    try (
        FileWriter fw = new FileWriter(WALFileName, true);
        BufferedWriter bw = new BufferedWriter(fw);) {
      bw.write(operation + " " + key + (value != null ? " " + value : ""));
      bw.newLine();
      bw.flush();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public void restore(MemTable memTable) {
    if (!checkWAL())
      return;
    try (
        FileReader fr = new FileReader(WALFileName);
        BufferedReader br = new BufferedReader(fr);) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] parts = line.split(" ", 3);
        if (parts[0].equals("PUT"))
          memTable.put(parts[1], parts[2]);
        if (parts[0].equals("DELETE"))
          memTable.delete(parts[1]);
      }
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  public void clear() {
    try {
      new FileWriter(WALFileName, false).close();
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }
}
