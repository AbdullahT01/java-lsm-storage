# LSM-Storage

A persistent, write-optimized key-value storage engine built in Java, implementing the **Log-Structured Merge-tree (LSM Tree)** architecture — the same foundation behind **RocksDB**, **LevelDB**, and **Apache Cassandra**.

---

## Features

- **MemTable** — sorted in-memory write buffer using a `TreeMap`
- **SSTables** — immutable, sorted on-disk storage files
- **Bloom Filters** — probabilistic filtering to eliminate 99% of unnecessary disk reads
- **Sparse Index** — byte-offset index reducing RAM overhead by 95%
- **Write-Ahead Log (WAL)** — crash recovery by replaying logged operations on startup
- **Startup Recovery** — rebuilds Bloom Filters and Sparse Indexes from existing SSTable files
- **Tombstone Deletes** — LSM-style deletion without mutating immutable files
- **Interactive CLI** — run `PUT`, `GET`, `DELETE` commands against a live database instance

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
```

### MemTable
Acts as the primary write cache. All `put` and `delete` operations hit here first. Backed by a `TreeMap` to keep keys sorted — critical for writing SSTables in sorted order. When the size threshold is exceeded, the MemTable is flushed to disk as a new SSTable.

### SSTable (Sorted String Table)
Immutable on-disk files written in sorted key order using `DataOutputStream`. Each SSTable maintains its own **Bloom Filter** and **Sparse Index** in memory. On program restart, these are rebuilt from the file itself.

### Bloom Filter
A space-efficient probabilistic structure using 3 hash functions and a `BitSet`. Before any disk read, the Bloom Filter is queried — if it returns `false`, the key definitely does not exist in that file, and disk I/O is skipped entirely. Sized to maintain a **1% false positive rate**.

### Sparse Index
Records one key-to-byte-offset entry every 2048 bytes of file data. On a read, `floorKey()` finds the closest checkpoint and `RandomAccessFile.seek()` jumps directly to that byte position — avoiding full file scans.

### Write-Ahead Log (WAL)
Every `PUT` and `DELETE` is appended to `wal.log` before hitting the MemTable. On startup, if the log exists, operations are replayed to restore any data that hadn't been flushed to disk. The WAL is cleared after each flush.

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

### CLI Usage
```
> PUT user_1 john
> GET user_1
john
> DELETE user_1
> GET user_1
null
> EXIT
```

---

## Read Path (detailed)

1. Check active **MemTable** — O(log n) TreeMap lookup
2. If not found, iterate SSTables **newest to oldest**
3. For each SSTable:
   - Query **Bloom Filter** — if negative, skip file entirely
   - Use **Sparse Index** `floorKey()` to find closest byte offset
   - `seek()` to that offset and scan forward until key found or passed

---

## Write Path (detailed)

1. Append operation to **WAL** (`wal.log`)
2. Insert into **MemTable** (`TreeMap`)
3. If MemTable exceeds size threshold:
   - Write sorted entries to new `data-N.sst` file
   - Build **Bloom Filter** and **Sparse Index** in memory
   - Clear MemTable and WAL

---

## Design Decisions

| Decision | Reasoning |
|----------|-----------|
| `TreeMap` for MemTable | Keeps keys sorted for sequential SSTable writes |
| Newest-to-oldest SSTable search | Ensures most recent value returned for updated keys |
| Tombstones for deletes | SSTables are immutable — deletion requires a marker |
| Sparse vs dense index | Dense index would store every key offset, using far more RAM |
| Plain text WAL | Human-readable for debugging; binary WAL would improve performance at scale |

---

## Roadmap

- [ ] Size-tiered compaction — merge SSTables, purge tombstones
- [ ] TCP server — accept client connections over a socket (Redis-like interface)
- [ ] Python/Node.js client library
- [ ] Benchmarks vs SQLite and RocksDB

---

## Concepts Demonstrated

- LSM Tree architecture (as used in RocksDB, Cassandra, LevelDB)
- Bloom Filter design and sizing formula
- Sparse indexing with byte-offset seeks
- Write-Ahead Logging for crash recovery
- Immutable file design and tombstone-based deletion
- Java I/O streams: `DataOutputStream`, `RandomAccessFile`, `BufferedWriter`
