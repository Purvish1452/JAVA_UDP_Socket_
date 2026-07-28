package server;

import model.Packet;
import util.ConsoleReport;
import util.PacketUtil;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * UDP Server responsible for managing client registrations and broadcasting packets.
 * Demonstrates high-throughput UDP packet generation, network transmission,
 * and performance metrics logging.
 */
public class UDPServer {

    public static final int DEFAULT_PORT = 9876;
    public static final int TOTAL_PACKETS = 10000;
    private static final String REGISTRATION_KEYWORD = "REGISTER";

    private final int port;
    private DatagramSocket socket;
    private final ClientRegistry registry;

    private long packetsGenerated = 0;
    private long totalPacketsSent = 0;
    private long totalBytesSent = 0;
    private long sendingTimeMs = 0;

    public UDPServer(int port) {
        this.port = port;
        this.registry = new ClientRegistry();
    }

    /**
     * Starts the UDP Server socket.
     *
     * @throws SocketException If socket creation or binding fails
     */
    public void start() throws SocketException {
        this.socket = new DatagramSocket(port);
        System.out.println("[SERVER] UDP Server started on port " + port);
    }

    /**
     * Programmatically registers a client endpoint.
     *
     * @param address Client InetAddress
     * @param port    Client UDP Port
     */
    public void registerClient(InetAddress address, int port) {
        if (registry.registerClient(address, port)) {
            System.out.println("[SERVER] Registered client: " + address.getHostAddress() + ":" + port);
        }
    }

    /**
     * Waits for registration packets from client sockets until the expected number of clients register.
     *
     * @param expectedClients Total clients to await before starting broadcast
     * @throws IOException If socket read errors occur
     */
    public void awaitClientRegistrations(int expectedClients) throws IOException {
        System.out.println("[SERVER] Waiting for " + expectedClients + " client(s) to register...");
        byte[] buffer = new byte[1024];

        while (registry.getClientCount() < expectedClients) {
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            String message = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8).trim();
            if (REGISTRATION_KEYWORD.equalsIgnoreCase(message)) {
                InetAddress clientAddr = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                registerClient(clientAddr, clientPort);

                // Send ACK confirmation back to client
                byte[] ack = "REGISTERED_ACK".getBytes(StandardCharsets.UTF_8);
                DatagramPacket ackPacket = new DatagramPacket(ack, ack.length, clientAddr, clientPort);
                socket.send(ackPacket);
            }
        }
        System.out.println("[SERVER] All " + expectedClients + " clients registered successfully.");
    }

    /**
     * Generates exactly 10,000 packets and broadcasts them to all registered clients.
     * Measures total transmission execution time, byte counts, and prints statistics.
     *
     * @throws IOException If socket transmission fails
     */
    public void broadcastPackets() throws IOException {
        int connectedClients = registry.getClientCount();
        if (connectedClients == 0) {
            System.out.println("[SERVER] No clients registered. Broadcast aborted.");
            return;
        }

        System.out.println("[SERVER] Starting broadcast of " + TOTAL_PACKETS + " packets to " + connectedClients + " client(s)...");

        // Sample binary payload bytes
        byte[] payloadData = "UDP_MONITOR_PAYLOAD_SAMPLE_DATA_BYTES".getBytes(StandardCharsets.UTF_8);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= TOTAL_PACKETS; i++) {
            long packetId = i;
            long sequenceNumber = i;
            long timestamp = System.currentTimeMillis();

            Packet packet = new Packet(packetId, sequenceNumber, timestamp, payloadData);
            packetsGenerated++;

            byte[] serializedData = PacketUtil.serialize(packet);

            for (ClientRegistry.ClientInfo client : registry.getRegisteredClients()) {
                DatagramPacket datagramPacket = new DatagramPacket(
                        serializedData,
                        serializedData.length,
                        client.getAddress(),
                        client.getPort()
                );

                socket.send(datagramPacket);
                totalPacketsSent++;
                totalBytesSent += serializedData.length;
            }
        }

        long endTime = System.currentTimeMillis();
        this.sendingTimeMs = Math.max(1, endTime - startTime); // Guard against 0ms division

        System.out.println("[SERVER] Broadcast complete.");
        printReport();
    }

    /**
     * Displays the server execution report via ConsoleReport.
     */
    public void printReport() {
        ConsoleReport.printServerReport(
                packetsGenerated,
                registry.getClientCount(),
                totalPacketsSent,
                totalBytesSent,
                sendingTimeMs
        );
    }

    /**
     * Closes the UDP socket resource.
     */
    public void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[SERVER] Server socket closed.");
        }
    }

    public ClientRegistry getRegistry() {
        return registry;
    }
}
