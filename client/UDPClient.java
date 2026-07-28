package client;

import model.Packet;
import util.ConsoleReport;
import util.PacketUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * UDP Client that registers with the server, receives continuous datagrams,
 * deserializes packets, and tracks network delivery statistics.
 * Implements Runnable for multi-threaded client execution.
 */
public class UDPClient implements Runnable {

    private static final int BUFFER_SIZE = 4096;
    private static final int BROADCAST_TIMEOUT_MS = 2000; // 2 seconds timeout to signal end of stream

    private final String clientId;
    private final InetAddress serverAddress;
    private final int serverPort;
    private DatagramSocket socket;
    private final ClientStatistics statistics;

    /**
     * Constructs a UDP Client.
     *
     * @param clientId        Unique string name for client logging
     * @param serverAddress   InetAddress of the UDP server
     * @param serverPort      Port number of the UDP server
     * @param expectedPackets Total expected packets to be broadcasted
     */
    public UDPClient(String clientId, InetAddress serverAddress, int serverPort, long expectedPackets) {
        this.clientId = clientId;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.statistics = new ClientStatistics(expectedPackets);
    }

    /**
     * Sends a registration datagram to the UDP Server and awaits an ACK confirmation.
     *
     * @return true if successfully registered, false otherwise
     * @throws IOException If network transmission errors occur
     */
    public boolean registerWithServer() throws IOException {
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket(); // Binds to an ephemeral OS available port
        }

        byte[] regData = "REGISTER".getBytes(StandardCharsets.UTF_8);
        DatagramPacket regPacket = new DatagramPacket(regData, regData.length, serverAddress, serverPort);

        System.out.println("[" + clientId + "] Sending REGISTER datagram to "
                + serverAddress.getHostAddress() + ":" + serverPort + " from local port " + socket.getLocalPort());
        socket.send(regPacket);

        // Await confirmation ACK
        byte[] ackBuffer = new byte[1024];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
        socket.setSoTimeout(3000); // 3-second timeout for registration ACK

        try {
            socket.receive(ackPacket);
//            Thread.sleep(2000);   //to see how packets are dropped
            String response = new String(ackPacket.getData(), 0, ackPacket.getLength(), StandardCharsets.UTF_8).trim();
            if ("REGISTERED_ACK".equalsIgnoreCase(response)) {
                System.out.println("[" + clientId + "] Registration ACK received!");
                return true;
            }
        } catch (SocketTimeoutException e) {
            System.err.println("[" + clientId + "] Registration timed out waiting for server ACK.");
        }
        return false;
    }

    /**
     * Continuously listens for incoming UDP datagram packets, deserializes them,
     * updates client performance statistics, and terminates when the stream finishes.
     */
    public void startReceiving() {
        byte[] buffer = new byte[BUFFER_SIZE];
        System.out.println("[" + clientId + "] Listening for incoming UDP packets...");

        try {
            // Set socket timeout so loop exits when broadcast finishes (idle timeout)
            socket.setSoTimeout(BROADCAST_TIMEOUT_MS);

            while (true) {
                DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(datagram);
                    long receiveTimestamp = System.currentTimeMillis();

                    // Convert binary datagram buffer back to Packet object
                    Packet packet = PacketUtil.deserialize(datagram.getData(), datagram.getLength());

                    // Feed metrics engine
                    statistics.recordPacket(packet, receiveTimestamp, datagram.getLength());

                } catch (SocketTimeoutException e) {
                    // Timeout reached -> broadcast complete
                    System.out.println("[" + clientId + "] Broadcast finished (socket idle for " + BROADCAST_TIMEOUT_MS + "ms).");
                    break;
                }
            }

        } catch (Exception e) {
            System.err.println("[" + clientId + "] Error during packet receive loop: " + e.getMessage());
        } finally {
            close();
        }
    }

    @Override
    public void run() {
        try {
            if (registerWithServer()) {
                startReceiving();
            }
        } catch (IOException e) {
            System.err.println("[" + clientId + "] Failed to complete client execution: " + e.getMessage());
        }
    }

    /**
     * Displays the client statistics report using ConsoleReport.
     */
    public void printReport() {
        ConsoleReport.printClientReport(clientId, statistics);
    }

    /**
     * Safely closes the underlying UDP socket.
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public ClientStatistics getStatistics() {
        return statistics;
    }

    public String getClientId() {
        return clientId;
    }
}
