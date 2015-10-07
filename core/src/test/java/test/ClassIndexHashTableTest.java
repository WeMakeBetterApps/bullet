package test;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import bullet.impl.ClassIndexHashTable;

import static org.junit.Assert.*;

public class ClassIndexHashTableTest {
  @Test public void simpleTest() {
    int size = 5;
    size += (int) (size * 0.3);
    size = getNextPrime(size);
    ClassIndexHashTable table = new ClassIndexHashTable(size);
    table.put(List.class, (char) 1);
    table.put(Set.class, (char) 2);
    table.put(Map.class, (char) 3);
    table.put(Integer.class, (char) 4);
    table.put(String.class, (char) 5);

    assertEquals(1, table.get(List.class));
    assertEquals(2, table.get(Set.class));
    assertEquals(3, table.get(Map.class));
    assertEquals(4, table.get(Integer.class));
    assertEquals(5, table.get(String.class));
  }

  @Test public void getAbsentTest() {
    int size = 5;
    size += (int) (size * 0.3);
    size = getNextPrime(size);
    ClassIndexHashTable table = new ClassIndexHashTable(size);
    table.put(List.class, (char) 1);
    table.put(Set.class, (char) 2);
    table.put(Map.class, (char) 3);
    table.put(Integer.class, (char) 4);
    table.put(String.class, (char) 5);

    assertEquals(-1, table.get(Thread.class));
  }

  private static int getNextPrime(int value) {
    while (true) {
      value++;
      if (isPrime(value)) {
        return value;
      }
    }
  }

  private static boolean isPrime(int value) {
    for (int i = 2; i < value; i++) {
      if (value % i == 0) {
        return false;
      }
    }
    return true;
  }

}
