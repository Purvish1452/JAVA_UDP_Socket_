import client.UDPClient;
import server.UDPServer;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Main launcher class for the UDP Monitoring System.
 * Demonstrates starting a UDP Server and multiple concurrent UDP Clients (5 clients),
 * registering clients, broadcasting 10,000 packets, and collecting metrics.
 */
public class Main {

    private static final int CLIENT_COUNT = 5;
    private static final int SERVER_PORT = UDPServer.DEFAULT_PORT;
    private static final long EXPECTED_PACKETS = UDPServer.TOTAL_PACKETS;

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  STARTING UDP MONITORING SYSTEM (JAVA 17)       ");
        System.out.println("=================================================");

        UDPServer server = null;
        try {
            // 1. Initialize and start UDP Server
            server = new UDPServer(SERVER_PORT);
            server.start();

            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");

            // 2. Start server registration listener in background thread
            final UDPServer finalServer = server;
            Thread serverRegistrationThread = new Thread(() -> {
                try {
                    finalServer.awaitClientRegistrations(CLIENT_COUNT);
                } catch (Exception e) {
                    System.err.println("[SERVER THREAD ERROR] " + e.getMessage());
                }
            });
            serverRegistrationThread.start();

            // Brief sleep to ensure server socket listener is ready
            Thread.sleep(200);

            // 3. Create and launch 5 concurrent UDP Client threads
            List<Thread> clientThreads = new ArrayList<>();
            List<UDPClient> clients = new ArrayList<>();

            for (int i = 1; i <= CLIENT_COUNT; i++) {
                String clientId = "Client-" + i;
                UDPClient client = new UDPClient(clientId, serverAddress, SERVER_PORT, EXPECTED_PACKETS);
                clients.add(client);

                Thread clientThread = new Thread(client, "Thread-" + clientId);
                clientThreads.add(clientThread);
                clientThread.start();
            }

            // 4. Await registration of all clients
            serverRegistrationThread.join();

            // Brief pause before broadcast starts
            Thread.sleep(300);

            // 5. Server broadcasts 10,000 packets to all 5 clients
            server.broadcastPackets();

            // 6. Wait for all client threads to complete receiving
            for (Thread t : clientThreads) {
                t.join();
            }

            // 7. Print client reports sequentially for clean output formatting
            for (UDPClient client : clients) {
                client.printReport();
            }

            System.out.println("=================================================");
            System.out.println("  UDP MONITORING SYSTEM EXECUTION COMPLETED     ");
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("[MAIN ERROR] " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }
}
