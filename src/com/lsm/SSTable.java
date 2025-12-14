package com.lsm;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.TreeMap;

public class SSTable {

    public void createSSTable(TreeMap<String,String> data, String fileName){
        try(
                FileOutputStream fos = new FileOutputStream(fileName);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DataOutputStream dos = new DataOutputStream(bos);
        )
        {
                for(var entry : data.entrySet()){
                    String key = entry.getKey();
                    String value = entry.getValue();

                    dos.writeUTF(key);
                    dos.writeUTF(value);
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

    public static String getValue(String fileName, String key){
        try(
                FileInputStream fis = new FileInputStream(fileName);
                BufferedInputStream bis = new BufferedInputStream(fis);
                DataInputStream dis = new DataInputStream(bis);
        ){
                while (dis.available() > 0){
                    String currentKey = dis.readUTF();

                    if(currentKey.equals(key)){
                        return dis.readUTF();
                    }

                    // skip the value because we are searching for keys
                    dis.readUTF();
                }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
