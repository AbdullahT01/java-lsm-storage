package com.lsm;

import java.io.IOException;
import com.lsm.MemTable;
import com.lsm.WriteAheadLog;

public class Main {
  public static void main(String[] args) {
    MemTable db = new MemTable();
    WriteAheadLog wal = new WriteAheadLog();
    Compaction compactor = new Compaction();

    wal.restore(db);
    wal.clear();
    compactor.compact(db);

    Server server = new Server(db, wal);
    try {
      server.start();
    } catch (IOException e) {
      System.out.println("server failed" + e.getMessage());
    }

  }
}
