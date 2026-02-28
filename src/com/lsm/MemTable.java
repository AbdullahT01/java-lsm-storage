package com.lsm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.lsm.SSTable;

public class MemTable {
  private final TreeMap<String, String> table;
  private int currentByteSize;
  private final List<SSTable> sstables = new ArrayList<>();
  private static int segmentID = 0;

  private static final int MEMTABLE_BYTE_LIMIT = 16000;

  public int getSegmentID() {
    return segmentID;
  }

  public MemTable() {
    this.table = new TreeMap<>();
    this.currentByteSize = 0;
    startUpRecovery();
    System.out.println("getting old segmentID value --> " + getSegmentID());
  }

  public void put(String key, String value) {

    int entrySize = key.length() + value.length();

    table.put(key, value);
    currentByteSize += entrySize;

    if (currentByteSize > MEMTABLE_BYTE_LIMIT) {
      flush();
    }
  }

  public String get(String key) {
    long startTime = System.nanoTime();
    String result = table.get(key);
    if (result != null) {

      System.out.println("found the value in the memtable");
    } else {
      for (int i = sstables.size() - 1; i >= 0; i--) {
        result = sstables.get(i).getValue(key);

        if (result != null) {
          System.out.println("found the value in sst table: ");
          break;
        }
      }
    }
    long endTime = System.nanoTime();
    float durationInMs = ((endTime) - startTime) / 1000000.0f;
    System.out.println();
    System.out.println("Time to retrieve value ---> " + durationInMs + "ms");
    return result;
  }

  public void flush() {
    System.out.println("We hit the limit for this MemTable, will now flush!");

    // creating the file using the createSSTable function and adding it to our sst
    // list
    String fileName = "data-" + segmentID + ".sst";
    SSTable sst = new SSTable(fileName);
    sstables.add(sst);

    sst.createSSTable(table);
    // sst.readSSTable(fileName);

    table.clear();
    currentByteSize = 0;
    segmentID++;

    System.out.println("flush has been complete");
  }

  public void startUpRecovery() {

    File dir = new File(".");
    File[] allFiles = dir.listFiles();

    for (File file : allFiles) {
      if (file.getName().endsWith(".sst")) {
        SSTable sst = new SSTable(file.getName());
        sstables.add(sst);

        // figure out current segmentID
        int id = Integer.parseInt(file.getName().replace("data-", "").replace(".sst", ""));
        if (segmentID <= id)
          segmentID = id + 1;
      }
    }
  }

}
