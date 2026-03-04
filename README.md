# LSM-Storage

A persistent, write-optimized key-value storage engine built in Java, implementing the **Log-Structured Merge-tree (LSM Tree)** architecture — the same foundation behind **RocksDB**, **LevelDB**, and **Apache Cassandra**.

---

## Benchmarks

> Measured on a single node, 10,000 operations, no concurrency.

| Operation | Throughput | Avg Latency |
|-----------|------------|-------------|
| Write (PUT) | **271,270 ops/sec** | 0.0037ms |
| Read — key exists (GET) | **13,223 ops/sec** | 0.076ms |
| Read — key missing (Bloom Filter) | **133,062 ops/sec** | 0.0075ms |
| Delete | **100/100 correct** | 0.003ms total |

**Bloom Filter impact:** Non-existent key lookups are **10x faster** than regular reads because the Bloom Filter eliminates disk I/O entirely before any file is touched.

**Compaction impact:** Read speed improves after compaction as multiple SSTable files are merged into one, reducing the number of files searched per GET.

---

## Features

- **MemTable** — sorted in-memory write buffer using a `TreeMap`
- **SSTables** — immutable, sorted on-disk storage files
- **Bloom Filters** — probabilistic filtering to skip unnecessary disk reads entirely
- **Sparse Index** — byte-offset index enabling direct seeks into SSTable files
- **Write-Ahead Log (WAL)** — crash recovery by replaying logged operations on startup
- **Compaction** — merges SSTable files, purges tombstones, and restores read performance
- **Tombstone Deletes** — LSM-style deletion without mutating immutable files
- **TCP Server** — accepts client connections over a socket, Redis-style interface

---

## Architecture

```
PUT  →  WAL (wal.log)  →  MemTable (TreeMap)
                               ↓ (when full)
                          flush() → SSTable (data-N.sst)
                                        → Bloom Filter built
                                        → Sparse Index built

GET  →  MemTable  →  (not found)  →  SSTable N (newest first)
                                          → Bloom Filter check
                                          → Sparse Index seek
                                          → linear scan from offset

COMPACTION  →  merge all SSTables  →  resolve tombstones
            →  write data-0.sst   →  delete old files
```

### MemTable
Acts as the primary write cache. All `put` and `delete` operations hit here first. Backed by a `TreeMap` to keep keys sorted — critical for writing SSTables in sorted order. When the size threshold is exceeded, the MemTable is flushed to disk as a new SSTable.

### SSTable (Sorted String Table)
Immutable on-disk files written in sorted key order using `DataOutputStream`. Each SSTable maintains its own **Bloom Filter** and **Sparse Index** in memory. On program restart, these are rebuilt from the file itself via `loadFromDisk()`.

### Bloom Filter
A space-efficient probabilistic structure using 3 hash functions and a `BitSet`. Before any disk read, the Bloom Filter is queried — if it returns `false`, the key definitely does not exist in that file, and disk I/O is skipped entirely. Sized to maintain a **1% false positive rate**. This is what drives the 10x speedup on negative lookups shown in the benchmarks above.

### Sparse Index
Records one key-to-byte-offset entry every 2048 bytes of file data. On a read, `floorKey()` finds the closest checkpoint and `RandomAccessFile.seek()` jumps directly to that byte position — avoiding full file scans.

### Write-Ahead Log (WAL)
Every `PUT` and `DELETE` is appended to `wal.log` before hitting the MemTable. On startup, if the log exists, operations are replayed to restore any data that hadn't been flushed to disk. The WAL is cleared after successful recovery.

### Compaction
On startup, all existing SSTable files are merged into a single file. Duplicate keys are resolved (newest value wins) and tombstoned keys are purged. The MemTable's SSTable references are updated to reflect the new merged file, preventing stale file reads.

### TCP Server
Listens on port 8080 and accepts commands from any TCP client — `nc`, a Python script, a Node.js app, or anything that can open a socket. The same `PUT`, `GET`, `DELETE` interface is exposed over the network.

---

## Getting Started

### Prerequisites
- Java Development Kit (JDK) 14 or higher

### Build
```bash
javac -d out/production/lsm-storage src/com/lsm/*.java
```

### Run
```bash
java -cp out/production/lsm-storage com.lsm.Main
```

### Connect via TCP
```bash
nc localhost 8080
```

```
*****************************************
* PUT                                   *
* GET                                   *
* DELETE                                *
* STOP                                  *
*****************************************
PUT
user_1
john
> OK
GET
user_1
> value is: john
DELETE
user_1
> OK
```

### Run Benchmarks
```bash
java -cp out/production/lsm-storage com.lsm.Benchmark
```

---

## Read Path

1. Check active **MemTable** — O(log n) TreeMap lookup
2. If not found, iterate SSTables **newest to oldest**
3. For each SSTable:
   - Query **Bloom Filter** — if negative, skip file entirely
   - Use **Sparse Index** `floorKey()` to find closest byte offset
   - `seek()` to that offset and scan forward until key found or passed

## Write Path

1. Append operation to **WAL** (`wal.log`)
2. Insert into **MemTable** (`TreeMap`)
3. If MemTable exceeds size threshold:
   - Write sorted entries to new `data-N.sst` file
   - Build **Bloom Filter** and **Sparse Index** in memory
   - Clear MemTable

---

## Design Decisions

| Decision | Reasoning |
|----------|-----------|
| `TreeMap` for MemTable | Keeps keys sorted for sequential SSTable writes |
| Newest-to-oldest SSTable search | Ensures most recent value returned for updated keys |
| Tombstones for deletes | SSTables are immutable — deletion requires a marker |
| Sparse vs dense index | Dense index would store every key offset, using far more RAM |
| Plain text WAL | Human-readable for debugging; binary WAL would improve performance at scale |
| Persistent RAF handle | `RandomAccessFile` kept open per SSTable to avoid repeated file open/close overhead on reads |

---

## Concepts Demonstrated

- LSM Tree architecture (as used in RocksDB, Cassandra, LevelDB)
- Bloom Filter design and sizing formula
- Sparse indexing with byte-offset seeks
- Write-Ahead Logging for crash recovery
- Immutable file design and tombstone-based deletion
- Compaction: merging, deduplication, and tombstone purging
- TCP server with socket I/O
- Java I/O streams: `DataOutputStream`, `RandomAccessFile`, `BufferedWriter`
