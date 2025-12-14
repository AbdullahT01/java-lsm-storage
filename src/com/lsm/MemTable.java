package com.lsm;
import java.util.TreeMap;

public class MemTable {
    private final TreeMap<String, String> table;
    private int currentByteSize;
    private static int segmentID = 0;

    private static final int MEMTABLE_BYTE_LIMIT = 4096;

    public MemTable(){
        this.table = new TreeMap<>();
        this.currentByteSize = 0;
    }

    public void put(String key, String value){

        int entrySize = key.length() + value.length();

        table.put(key, value);
        currentByteSize += entrySize;

        if (currentByteSize > MEMTABLE_BYTE_LIMIT){
            flush();
        }
    }

    public String get(String key){
        return table.get(key);
    }

    public void flush() {
        System.out.println("We hit the limit for this MemTable, will now flush!");

        // creating the file using the createSSTable function
        String fileName = "data-" + segmentID + ".sst";
        SSTable sst = new SSTable();
        sst.createSSTable(table, fileName);
        sst.readSSTable(fileName);

        table.clear();
        currentByteSize = 0;
        segmentID++;

        System.out.println("flush has been complete");
    }

}
