package com.lsm;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.TreeMap;

import com.lsm.MemTable;
import com.lsm.SSTable;

import java.io.*;

public class Compaction {
  private final TreeMap<String, String> compactedTable = new TreeMap<>();

  public Compaction() {
    // just wanted to keep pattern of calling constructor
  }

  public void compact(MemTable memTable) {
    File dir = new File(".");

    // we need to Override because the list file function takes
    // in a FilenameFilter object and we want to only get our sst files.
    FilenameFilter sstFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("data-") && name.endsWith(".sst");
      }
    };

    File[] sstFiles = dir.listFiles(sstFilter);

    if (sstFiles == null || sstFiles.length == 0) {
      System.out.println("No sst files found, compaction will be skipped");
      return;
    }

    // we need to Override comparitor because we want to sort them in order of
    // oldest to newest
    Comparator sstComparer = new Comparator<File>() {
      @Override
      public int compare(File file1, File file2) {
        String name1 = file1.getName();
        String name2 = file2.getName();

        String stringID1 = name1.replace("data-", "").replace(".sst", "");
        String stringID2 = name2.replace("data-", "").replace(".sst", "");

        int id1 = Integer.parseInt(stringID1);
        int id2 = Integer.parseInt(stringID2);

        // compare the 2 numbers so they sort correctly
        return Integer.compare(id1, id2);
      }
    };

    // sort sstFiles
    Arrays.sort(sstFiles, sstComparer);

    for (File file : sstFiles) {
      try (
          FileInputStream fis = new FileInputStream(file);
          BufferedInputStream bis = new BufferedInputStream(fis);
          DataInputStream dis = new DataInputStream(bis);) {
        while (dis.available() > 0) {
          String key = dis.readUTF();
          String value = dis.readUTF();

          if (value.equals("TOMBSTONE")) {
            compactedTable.remove(key);
            continue;
          }

          compactedTable.put(key, value);
        }
      } catch (IOException e) {
        // here we will catch the end of line exception
        throw new RuntimeException(e);
      }
    }

    // now we need to go ahead and create the compacted sst file
    String tempCompactedFileName = "temp-data-0.sst";
    SSTable compactedSSTABLE = new SSTable(tempCompactedFileName);

    compactedSSTABLE.createSSTable(compactedTable);

    // now we delete the files that we compacted

    for (File file : sstFiles) {
      if (file.exists()) {
        boolean isDeleted = file.delete();
        if (!isDeleted) {
          System.out.print("was not able to delete compacted file");
        }
      }
    }
    File tempFile = new File(tempCompactedFileName);
    File finalFile = new File("data-0.sst");

    if (tempFile.renameTo(finalFile)) {
      System.out.println("Compaction completely successful! Merged into data-0.sst");

      memTable.resetSSTables();
      SSTable compacted = new SSTable("data-0.sst");
      compacted.loadFromDisk();
      memTable.addSSTable(compacted);
    } else {
      System.out.println("Failed to rename temporary compaction file.");
    }
  }
}
