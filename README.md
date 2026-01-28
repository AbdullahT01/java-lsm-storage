# LSM-Storage

## Overview
**LSM-Storage** is a custom implementation of a **Log-Structured Merge-tree (LSM Tree)** storage engine. This project demonstrates the core architecture behind high-performance, write-optimized databases like RocksDB, LevelDB, and Cassandra. It efficiently manages data by buffering writes in memory before flushing them to immutable on-disk structures, utilizing probabilistic filtering to accelerate read performance.

## Core Architecture

The system is built upon three fundamental components:

### 1. MemTable (In-Memory Buffer)
*   **Role:** Acts as the primary write cache.
*   **Mechanism:** All incoming write operations (`put`, `delete`) are first directed here. The MemTable maintains data in a sorted structure to allow for efficient range scans and quick lookups.
*   **Lifecycle:** When the MemTable reaches a predefined size threshold, it is frozen and flushed to disk as an SSTable.

### 2. SSTable (Sorted String Table)
*   **Role:** Provides persistent, immutable on-disk storage.
*   **Mechanism:** Data flushed from the MemTable is written sequentially to disk in sorted order. This immutability ensures data integrity and simplifies crash recovery.
*   **Compaction:** (Future/Current feature) Multiple SSTables can be merged to reclaim space and remove overwritten or deleted data.

### 3. Bloom Filter
*   **Role:** Read optimization.
*   **Mechanism:** A space-efficient probabilistic data structure that predicts whether an element is a member of a set.
*   **Benefit:** Before accessing the disk to search an SSTable, the system queries the Bloom Filter. If the filter returns `false`, the key definitely does not exist in that file, saving expensive disk I/O operations.

## How It Works

1.  **Write Path:** Data is written to the **MemTable**. Once full, it is serialized to an **SSTable** on disk.
2.  **Read Path:** The system first searches the active **MemTable**. If not found, it checks the **Bloom Filters** of the on-disk SSTables. If a filter indicates a possible match, the corresponding **SSTable** is scanned.

## Getting Started

### Prerequisites
*   Java Development Kit (JDK) 8 or higher.

### Usage
Run the main entry point to demonstrate the storage engine operations:

```bash
java com.lsm.Main
