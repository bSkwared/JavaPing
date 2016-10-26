public class Main {
    public static void main(String[] args) {
        if (doStuff()) {
            System.out.println("hey");
        } else {
            System.out.println("no");
        }
    }

    public static boolean doStuff() {
        if (1 == 2) {
            return true;
        } else {
            return false;
        }
    }

}
