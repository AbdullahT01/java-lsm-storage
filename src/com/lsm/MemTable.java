package com.lsm;
import java.util.TreeMap;

import static com.lsm.SSTable.getValue;

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
        if(table.get(key) != null){

            System.out.println("found the value in the memtable");
            return table.get(key);
        }
        else{
            for(int i = segmentID - 1; i >= 0; i--){
                String fileName = "data-" + i + ".sst";
                String result = getValue(fileName, key);

                if(result != null){
                    System.out.println("found the value in sst table: " + fileName);
                    return result;
                }
            }
        }
        return null;
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
