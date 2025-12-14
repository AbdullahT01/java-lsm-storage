package com.lsm;
import java.util.TreeMap;

public class MemTable {
    private final TreeMap<String, String> table;
    private int currentByteSize;

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

        table.clear();
        currentByteSize = 0;
        System.out.println("flush has been complete");
    }
}
