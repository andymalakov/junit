package junit.runner;

/**
 * This class defines the current version of Concutest
 */
public class ConcutestVersion {
 private ConcutestVersion() {
  // don't instantiate
 }

 public static String id() {
  return "2.01 for 4.11";
 }

 public static void main(String[] args) {
  System.out.println(id());
 }
}
