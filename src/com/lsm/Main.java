package com.lsm;

public class Main {
  public static void main(String[] args) {
    MemTable db = new MemTable();

    System.out.println("Writing data to trigger a flush...");

    // Loop to fill up the 4KB limit
    for (int i = 0; i < 2000; i++) {

      String key = "user_" + i;
      String value = "data_payload_" + i;

      db.put(key, value);
    }
    // Testinng to see if db can handle startUp recovery by checking
    // retreival of formar segmentID
    System.out.println("retreiving current segmentID --> " + db.getSegmentID());

    // db.put("user_1598", "testing new data");
    String testingGetFunction = db.get("user_1598");
    System.out.println(testingGetFunction);

    System.out.println("Done.");
  }
}
