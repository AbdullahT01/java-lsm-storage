package com.lsm;

public class Benchmark {

  private static final int WRITE_COUNT = 10000;
  private static final int READ_COUNT = 10000;

  public static void main(String[] args) {
    System.out.println("=========================================");
    System.out.println("        LSM ENGINE BENCHMARKS            ");
    System.out.println("=========================================\n");

    MemTable db = new MemTable();

    benchmarkWrites(db);
    benchmarkReads(db);
    benchmarkBloomFilter(db);
    benchmarkDeleteAndRead(db);
    benchmarkCompaction(db);

    System.out.println("\n=========================================");
    System.out.println("         BENCHMARKS COMPLETE             ");
    System.out.println("=========================================");
  }

  // 1. Write throughput
  static void benchmarkWrites(MemTable db) {
    System.out.println("--- WRITE BENCHMARK (" + WRITE_COUNT + " entries) ---");

    long start = System.nanoTime();
    for (int i = 0; i < WRITE_COUNT; i++) {
      db.put("key" + i, "value" + i);
    }
    long end = System.nanoTime();

    double seconds = (end - start) / 1_000_000_000.0;
    double writesPerSecond = WRITE_COUNT / seconds;
    double avgLatencyMs = (seconds / WRITE_COUNT) * 1000;

    System.out.printf("Total time:        %.3f seconds%n", seconds);
    System.out.printf("Writes per second: %.0f%n", writesPerSecond);
    System.out.printf("Avg write latency: %.4f ms%n%n", avgLatencyMs);
  }

  // 2. Read throughput - keys that exist
  static void benchmarkReads(MemTable db) {
    System.out.println("--- READ BENCHMARK (" + READ_COUNT + " reads, keys exist) ---");

    long start = System.nanoTime();
    for (int i = 0; i < READ_COUNT; i++) {
      db.get("key" + i);
    }
    long end = System.nanoTime();

    double seconds = (end - start) / 1_000_000_000.0;
    double readsPerSecond = READ_COUNT / seconds;
    double avgLatencyMs = (seconds / READ_COUNT) * 1000;

    System.out.printf("Total time:        %.3f seconds%n", seconds);
    System.out.printf("Reads per second:  %.0f%n", readsPerSecond);
    System.out.printf("Avg read latency:  %.4f ms%n%n", avgLatencyMs);
  }

  // 3. Bloom filter - reading keys that DON'T exist
  // bloom filter should skip disk reads entirely
  static void benchmarkBloomFilter(MemTable db) {
    System.out.println("--- BLOOM FILTER BENCHMARK (keys do NOT exist) ---");

    long start = System.nanoTime();
    int nullCount = 0;
    for (int i = 0; i < READ_COUNT; i++) {
      String result = db.get("nonexistent-key-" + i);
      if (result == null)
        nullCount++;
    }
    long end = System.nanoTime();

    double seconds = (end - start) / 1_000_000_000.0;
    double readsPerSecond = READ_COUNT / seconds;

    System.out.printf("Total time:              %.3f seconds%n", seconds);
    System.out.printf("Reads per second:        %.0f%n", readsPerSecond);
    System.out.printf("Correctly returned null: %d / %d%n%n", nullCount, READ_COUNT);
  }

  // 4. Delete + read - confirm tombstones work
  static void benchmarkDeleteAndRead(MemTable db) {
    System.out.println("--- DELETE BENCHMARK ---");

    // write keys
    for (int i = 0; i < 100; i++) {
      db.put("deleteKey" + i, "value" + i);
    }

    // delete them and time it
    long start = System.nanoTime();
    for (int i = 0; i < 100; i++) {
      db.delete("deleteKey" + i);
    }
    long end = System.nanoTime();

    // verify they're gone
    int correctlyDeleted = 0;
    for (int i = 0; i < 100; i++) {
      if (db.get("deleteKey" + i) == null)
        correctlyDeleted++;
    }

    double seconds = (end - start) / 1_000_000_000.0;
    System.out.printf("100 deletes in:          %.4f seconds%n", seconds);
    System.out.printf("Correctly deleted:       %d / 100%n%n", correctlyDeleted);
  }

  // 5. Compaction impact on read speed
  static void benchmarkCompaction(MemTable db) {
    System.out.println("--- COMPACTION BENCHMARK ---");

    // flush to create multiple SST files
    db.flush();

    // read speed BEFORE compaction
    long start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      db.get("key" + i);
    }
    long end = System.nanoTime();
    double beforeSeconds = (end - start) / 1_000_000_000.0;

    // run compaction
    Compaction compactor = new Compaction();
    compactor.compact(db);

    // read speed AFTER compaction
    start = System.nanoTime();
    for (int i = 0; i < 1000; i++) {
      db.get("key" + i);
    }
    end = System.nanoTime();
    double afterSeconds = (end - start) / 1_000_000_000.0;

    double improvement = ((beforeSeconds - afterSeconds) / beforeSeconds) * 100;

    System.out.printf("Read time before compaction: %.3f seconds%n", beforeSeconds);
    System.out.printf("Read time after compaction:  %.3f seconds%n", afterSeconds);
    System.out.printf("Read speed improvement:      %.1f%%%n%n", improvement);
  }
}
