package com.lsm;
import java.io.*;
import java.util.TreeMap;

public class SSTable {

    private TreeMap<String, Long> sparseIndex = new TreeMap<>();
    private String fileName;
    private BloomFilter bloomFilter;
    private static final int SPARSE_INDEX_OFFSET = 256;

    public SSTable(String fileName){
        this.fileName = fileName;
    }

    public void createSSTable(TreeMap<String,String> data){
        // creating the bloom filter here since we know about the size of the data
        this.bloomFilter = new BloomFilter(data.size());
        try(
                FileOutputStream fos = new FileOutputStream(fileName);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream dos = new DataOutputStream(bos);
        )
        {
                long currentOffset = 0;      // Total bytes written to file
                long currentBlockSize = 0;   // Bytes written in THIS block
                for(var entry : data.entrySet()){

                    String key = entry.getKey();
                    String value = entry.getValue();

                    // 1. Snapshot the stream size BEFORE writing
                    int startSize = dos.size();

                    if (currentBlockSize == 0) {
                        sparseIndex.put(key, currentOffset);
                    }

                    dos.writeUTF(key);
                    dos.writeUTF(value);

                    // 4. Calculate exact bytes written
                    int endSize = dos.size();
                    int bytesWritten = endSize - startSize;

                    currentOffset += bytesWritten;
                    currentBlockSize += bytesWritten;

                    if (currentBlockSize >= SPARSE_INDEX_OFFSET) {
                        currentBlockSize = 0;
                    }

                    // adding the value to the bloom filter for this specific file
                    bloomFilter.add(key);
                }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readSSTable(String fileName){
        try(
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream dis = new DataInputStream(bis);
        )
        {
                while(dis.available() > 0){
                    String key = dis.readUTF();
                    String value = dis.readUTF();

                    System.out.println("Key: " + key + " | Value: " + value);
                }
        }
        catch (IOException e){
            // here we will catch the end of line exception
            throw new RuntimeException(e);
        }
    }

    public String getValue(String key){
        // return null immediately if bloomfilter says so
        if (!bloomFilter.mightContain(key)){
            return null;
        }

        try(
                RandomAccessFile raf = new RandomAccessFile(fileName, "r");
        ){
                String closestKey = sparseIndex.floorKey(key);
                if (closestKey == null) {
                    return null; // The key is smaller than the first item in the file, so it doesn't exist.
                }

                long offset = sparseIndex.get(closestKey);

                raf.seek(offset);

                while (raf.getFilePointer() < raf.length()) {
                    String currentKey = raf.readUTF();
                    String currentValue = raf.readUTF();

                    if (currentKey.equals(key)) {
                        return currentValue;
                    }

                    // Optimization: If we passed the key, stop.
                    if (currentKey.compareTo(key) > 0) {
                        return null;
                    }
                }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
