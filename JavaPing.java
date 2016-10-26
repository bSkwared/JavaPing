import java.text.*;
import java.io.*;
import java.net.*;

/**
 * @author Blake Lasky
 * @version 1.0
 * File: JavaPing.java
 * Created: oct 2016
 *
 * Description: Provides a ping clinet which mimics the native windows ping tool
 * while using the echo protocol. Also provides a public ping method which can
 * be used for locating machines still running the echo service on port 7.
 */
public class JavaPing {

    // The port that echo will be running on
    private static final int ECHO_PORT = 7;

    // Buffers used for sending and receiving data
    private static byte[] send;
    private static byte[] recv;
    
    // Used to format ping response times, can be changed for more precision
    private static NumberFormat fOut = new DecimalFormat("#0");

    // User configuration variables, set to default values
    private static int waitTime   = 2000;  // Timeout to wait for packet, in ms
    private static int packetSize = 32;    // How many bytes to send in a packet
    private static int numPackets = 4;     // How many packets to send
    private static int sourcePort = 0;     // Source port. 0 lets OS assign

    // Do not allow packets larger than this to be sent
    private static final int MAX_PACKET_SIZE = 1024;


    public static void main(String[] args) {

        // Networking variables
        InetAddress    localIP;
        DatagramSocket sock;
        String         remoteHost;
        InetAddress    remoteIP;

        // Need at least a host to ping
        if (args.length == 0) {
            printUsage();
            return;

        }
        
        // Parse arguments
        remoteHost = parseArgs(args);
        if (remoteHost == null) {
            printUsage();
            return;
        }


        // Setup DatagramSocket
        try {
            localIP  = InetAddress.getLocalHost();
            remoteIP = InetAddress.getByName(remoteHost);
            sock = new DatagramSocket(sourcePort, localIP);
            sock.setSoTimeout(waitTime);
            recv = new byte[packetSize];
        } catch (IOException ioe) {
            System.out.println("Ping request could not find host " + remoteHost
                            + ". Please check the name and try again.\n");
            return;
        }


        // Response times of sent packets
        double[] responseTimes = new double[numPackets];
        System.out.println("\nPinging " + remoteHost + " with " 
                                + packetSize + " bytes of data:");

        // Ping away
        for (int i = 0; i < numPackets; ++i) {
            // Time of each response
            double time;

            // Sleep for a second between pings
            if (i != 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    // This shouldn't happen since this is the only thread
                    System.out.println("Error: " + ie.getMessage());
                }
            }

            // Send a new ping
            responseTimes[i] = -1.0;
            try {
                time = ping(sock, localIP, remoteIP, packetSize);
                
                if (time > 0) {
                    System.out.println("Reply from " + remoteHost + ":"
                                        + " bytes=" + packetSize 
                                        + " time=" + fOut.format(time) + "ms");
                } else {
                    // Ping timedout
                    System.out.println("Request timed out.");

                    // Get new port so we don't just receive last message
                    if (sourcePort == 0) {
                        sock = new DatagramSocket(sourcePort, localIP);
                        sock.setSoTimeout(waitTime);
                    }
                }

                responseTimes[i] = time;

            } catch (IOException ioe) {
                System.out.println("Error: " + ioe.getMessage());
                return;
            } catch (Exception e) {
                return;
            }
        }

        printStats(remoteHost, responseTimes);
    }


    /**
    * Sends a random message to port 7 of the specified remoteIP 
    * and checks for valid response, returning the response time.
    * 
    * @param sock Socket to use for sending the packet.
    * @param localIP IP address of the local machine sending the packet
    * @param remoteIP IP address of the machine receiving the packet
    * @param size Size of the packet to send in bytes.
    *
    * @return Time in milliseconds of response, or negative if mangled packet or timedout
    */
    public static double ping(DatagramSocket sock, InetAddress localIP, 
                            InetAddress remoteIP, int size) throws IOException {

        // Repeated hashes of current time, just random data to send         
        String hashes;

        // Packets used to send and to receive from remoteIP
        DatagramPacket sendPacket;
        DatagramPacket recvPacket;

        // Generate random string to send
        hashes = Integer.toString(new Long(System.nanoTime()).hashCode());
        while (hashes.length() < size) {
            hashes += hashes;
        }
        hashes = hashes.substring(0, size);


        // Put string in buffer and create packets for sending and receiving
        send = hashes.getBytes();
        sendPacket = new DatagramPacket(send, size, remoteIP, ECHO_PORT);
        recvPacket = new DatagramPacket(recv, size);


        long startTime;
        long endTime;

        try {
            // Start timer and send packet
            startTime = System.nanoTime();
            sock.send(sendPacket);

            // Stop timer once packet has been received
            sock.receive(recvPacket);
            endTime = System.nanoTime();
        } catch (SocketTimeoutException ste) {
            return -1.0;
        }

        // Check that we received what we sent
        String response = new String(recv);
        if (response.equals(hashes)) {
            // Get runtime and convert to milliseconds
            return (endTime - startTime) / 1000000.0;
        } else {
            return -1.0;
        }
    }
    

    /**
    * Prints statistics for response times of packets sent to a certain IP.
    *
    * @param ip IP address to which the packets were sent.
    * @param tiems Times in ms of responses, negative values indcating timouts.
    */
    public static void printStats(String ip, double[] times) {

        // Data regarding packets
        int numSent     = times.length;
        int numReceived = 0;
        int numLost     = 0;
        int percentLost;

        // Data regarding response times
        double avgTime = 0;
        double minTime = 0;
        double maxTime = 0;
        double sumTime = 0;

        
        // Iterate through times getting stats
        for (int i = 0; i < numSent; ++i) {
            double curTime = times[i];

            // If this packet was received
            if (curTime > 0) {

                // Initialize min and max if this is the first packet
                if (numReceived == 0) {
                    minTime = curTime;
                    maxTime = curTime;
                }
                
                sumTime += curTime;
                ++numReceived;

                // Get new min and max
                if (curTime < minTime) {
                    minTime = curTime;
                } else if (curTime > maxTime) {
                    maxTime = curTime;
                }

            // Else the packet was lost
            } else {
                ++numLost;
            }
        }

        // Get average response time and percent of packets lost
        avgTime = sumTime / numReceived;
        percentLost = (int) (100.0 * numLost / numSent + 0.5);

        // Packet reliability stats
        String pingStats = "\nPing statistics for " + ip + ":\n"
            + "    Packets: Sent = " + numSent + ", Received = " + numReceived
            + ", Lost = " + numLost + " (" + percentLost + "% loss),\n";

        // If we received a packet
        if (numReceived > 1) {
            // Add information about response times
            pingStats += "Approximate round trip times in milli-seconds:\n    "
            + "Minimum = " + fOut.format(minTime) + "ms, "
            + "Maximum = " + fOut.format(maxTime) + "ms, "
            + "Average = " + fOut.format(avgTime) + "ms\n";
        }

        System.out.println(pingStats);
    }


    // Used for parsing command line arguments.
    // Modifies static class variables
    private static String parseArgs(String[] args) {
        String host = null;
        char flag = '\0';

        for (int i = 0; i < args.length; ++i) {
            String curArg = args[i];

            // If last argument was a flag, set appropriate value
            if (flag != '\0') {

                // Parse value of argument
                int argValue;
                try {
                    argValue = Integer.parseInt(curArg);
                } catch (NumberFormatException nfe) {
                    System.out.println(curArg + " must be a positive integer");
                    return null;
                }

                if (argValue < 1) {
                    System.out.println("Error: value must be positive integer");
                    return null;
                }

                // Set appropriate value
                switch (flag) {
                    // Number of packets
                    case 'n':
                        numPackets = argValue;
                        break;

                    // Size of packets
                    case 'l':
                        packetSize = argValue;
                        if (packetSize > MAX_PACKET_SIZE) {
                            packetSize = MAX_PACKET_SIZE;
                        }
                        break;

                    // Timeout for packets
                    case 'w':
                        waitTime = argValue;
                        break;

                    // Source port number
                    case 'p':
                        sourcePort = argValue;
                        break;
                }
                
                // Reset flag
                flag = '\0';

            } else {
                // Get new flag, or this argument specifies the hsot
                if (curArg.length() == 2 && curArg.charAt(0) == '-') {
                    flag = curArg.charAt(1);
                } else {
                    if (host == null) {
                        host = curArg;
                    } else {
                        System.out.println("Invalid syntax: multiple hosts");
                        return null;
                    }
                }
            }
        }

        // Check for trailing flag
        if (flag != '\0') {
            System.out.println("Invalid syntax: " + args[args.length-1]);
            return null;
        }

        return host;
    }

    private static void printUsage() {
        System.out.println("\nUsage: ping [-n count] [-l size] "
                                + "[-w timeout] host\n");
    }
}
