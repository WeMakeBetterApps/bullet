package bullet.impl;

public class ClassIndexHashTable {
  private final Class<?>[] classes;
  private final char[] values; // Using char as an unsigned 16-bit integer

  /**
   * @param size should be a prime number and at least 30% larger than the number of entries to store.
   */
  public ClassIndexHashTable(int size) {
    classes = new Class[size];
    values = new char[size];
  }

  private int lookup(Class<?> clazz) {
    int hash = clazz.hashCode();

    int index;
    do {
      hash = hash * 57 + 43;
      index = Math.abs(hash % classes.length);
    } while(classes[index] != null && classes[index] != clazz);

    return index;
  }

  /**
   * @param clazz the Class to get the index for.
   * @return the found index, otherwise -1.
   */
  public int get(Class<?> clazz) {
    int index = lookup(clazz);
    return classes[index] == null ? -1 : values[index];
  }

  public void put(Class<?> c, char value) {
    int hash = lookup(c);
    classes[hash] = c;
    values[hash] = value;
  }
}
