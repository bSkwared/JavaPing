import java.util.*;
import java.lang.*;
import java.io.*;

public class ConvertIPs {

    public static void main(String[] args) throws Exception {
        Scanner scan = new Scanner(System.in);

        while (scan.hasNext()) {
            scan.next();
            String str = scan.next();
            String[] ipS = str.split("\\.");

            for (int i = 0; i < 4; ++i) {
                byte b = Byte.parseByte(ipS[i]);
                if (i != 0) System.out.print(".");
                for (int k = 0; k < 256; ++k) {
                    if ((byte)k == b) {
                        System.out.print(k);
                        break;
                    }
                }
            }
            System.out.println();
        }


    }

}
