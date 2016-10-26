import java.io.*;
import java.net.*;

public class JavaPing {


    private static class PingRunnable implements Runnable {
        private DatagramSocket sock;
        int num;
        InetAddress localIP;
        public PingRunnable(DatagramSocket s, int n, InetAddress ip) {
            sock = s;
            num = n%256;
            localIP = ip;
        }

        public void run() {


           byte[] addr = new byte[4];
           addr[0] = (byte)17;
           addr[1] = (byte)num;
           System.out.println("started: " + num + " at port: " + sock.getLocalPort());

           for (int j = 0; j < 256; ++j) {
            addr[2] = (byte)j;
            for (int k = 0; k < 256; ++k) {
              addr[3] = (byte)k;

              try {

          ping(sock, localIP, InetAddress.getByAddress(addr), 32);
          System.out.println("found: " + addr[0]+"."+addr[1]+"."+addr[2]+"."+addr[3]);

              } catch (IOException ioe) { }


             }
              if ((j+1)%10 == 0) {
                System.out.println(num + " got to: "+addr[0]+""+addr[1]+""+addr[2]);
                }
           }

        }
    }


    private static final int LOCAL_PORT = 7777;
    private static final int REMOTE_PORT = 7;


    private static byte[] send;
    private static byte[] recv = new byte[32];

    public static void main(String[] args) {
        Double time;
        InetAddress localIP;
        DatagramSocket sock;

        final int TC = 256;

        Thread[] pool = new Thread[TC];


        for (int i = 0; i < TC; ++i) {
            try {
                localIP = InetAddress.getLocalHost();
                sock = new DatagramSocket(0, localIP);
                sock.setSoTimeout(110);
                PingRunnable pr = new PingRunnable(sock, i, localIP);
                pool[i] = new Thread(pr);
                pool[i].start();
                Thread.sleep(13);
            } catch (Exception ioe) {
                System.out.println("Error: " + ioe.getMessage());
                return;
            }
        }

    }

    public static Double ping(DatagramSocket sock, InetAddress localIP, 
                            InetAddress remoteIP, int size) throws IOException {

        // Repeated hashes of current time, just random data to send         
        String hashes;

        // Packets used to send and to receive from remoteIP
        DatagramPacket sendPacket;
        DatagramPacket recvPacket;

        // Generate random string to send
        hashes = new String();
        while (hashes.length() < size) {
            hashes += Integer.toString(new Long(System.nanoTime()).hashCode());
        }
        hashes = hashes.substring(0, size);


        // Put string in buffer and create packets for sending and receiving
        send = hashes.getBytes();
        sendPacket = new DatagramPacket(send, size, remoteIP, REMOTE_PORT);
        recvPacket = new DatagramPacket(recv, size);


        // Start timer and send packet
        long startTime = System.nanoTime();
        sock.send(sendPacket);

        // 
        sock.receive(recvPacket);

        long endTime = System.nanoTime();

        // Check that we received what we sent
        String response = new String(recv);
        if (response.equals(hashes)) {
            // Get runtime and convert to milliseconds
            return (endTime - startTime) / 1000000.0;
        } else {
            return null;
        }
    }

}
