package com.lsm;

import java.util.BitSet;

public class BloomFilter {
  private final BitSet bitSet;
  private final int bitSize;
  private static final int[] HASH_SEEDS = { 3, 7, 11 };

  public BloomFilter(int numElements) {
    this.bitSize = calculateBloomFilterSize(numElements, 0.01);
    this.bitSet = new BitSet(this.bitSize);
  }

  // This is a function that calculateBloomFilterSize to be able to have a 1%
  // false positive rate
  private static int calculateBloomFilterSize(int knownElements, double desiredFP) {
    double size;

    size = knownElements * Math.log(desiredFP);
    size *= -1;
    size /= (Math.log(2) * Math.log(2));

    return (int) size;
  }

  // function to flip the bits on for the bitset
  public void add(String key) {
    for (int seed : HASH_SEEDS) {
      int hash = getHash(key, seed);
      bitSet.set(hash, true);
    }
  }

  public boolean mightContain(String key) {
    for (int seed : HASH_SEEDS) {
      int hash = getHash(key, seed);
      if (!bitSet.get(hash)) {
        return false;
      }
    }
    return true;
  }

  private int getHash(String key, int seed) {
    int hash = 0;
    for (int i = 0; i < key.length(); i++) {
      hash = seed * hash + key.charAt(i);
    }
    return Math.abs(hash % bitSize);
  }

}
