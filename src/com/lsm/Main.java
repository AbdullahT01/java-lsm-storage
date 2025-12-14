package com.lsm;

public class Main {
    public static void main(String[] args) {
        MemTable db = new MemTable();

        System.out.println("Writing data to trigger a flush...");

        // Loop to fill up the 4KB limit
        for (int i = 0; i < 400; i++) {

            String key = "user_" + i;
            String value = "data_payload_" + i;

            db.put(key, value);
        }

        System.out.println("Done.");
    }
}