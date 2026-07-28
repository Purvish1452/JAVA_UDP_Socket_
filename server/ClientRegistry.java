package server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages client registrations for the UDP Server.
 * Because UDP is a connectionless protocol, the server must explicitly
 * store client IP addresses and ports to know where to broadcast data datagrams.
 */
public class ClientRegistry {

    /**
     * Encapsulates a registered client's IP address and UDP port.
     */
    public static class ClientInfo {
        private final InetAddress address;
        private final int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }

        public InetAddress getAddress() {
            return address;
        }

        public int getPort() {
            return port;
        }

        public InetSocketAddress getSocketAddress() {
            return new InetSocketAddress(address, port);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClientInfo that = (ClientInfo) o;
            return port == that.port && Objects.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(address, port);
        }

        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port;
        }
    }

    // Thread-safe set storing registered clients
    private final Set<ClientInfo> clients = ConcurrentHashMap.newKeySet();

    /**
     * Registers a client with the server using its IP Address and UDP port.
     *
     * @param address Client's InetAddress
     * @param port    Client's UDP port number
     * @return true if registration was successful, false if client was already registered
     */
    public boolean registerClient(InetAddress address, int port) {
        if (address == null || port <= 0 || port > 65535) {
            return false;
        }
        return clients.add(new ClientInfo(address, port));
    }

    /**
     * Unregisters a client from the server.
     *
     * @param address Client's InetAddress
     * @param port    Client's UDP port number
     * @return true if client was removed, false if it was not found
     */
    public boolean unregisterClient(InetAddress address, int port) {
        return clients.remove(new ClientInfo(address, port));
    }

    /**
     * Retrieves an unmodifiable view of all currently registered clients.
     *
     * @return Set of ClientInfo objects
     */
    public Set<ClientInfo> getRegisteredClients() {
        return Collections.unmodifiableSet(clients);
    }

    /**
     * Gets the total count of registered clients.
     *
     * @return Number of registered clients
     */
    public int getClientCount() {
        return clients.size();
    }

    /**
     * Clears all registered clients from the registry.
     */
    public void clear() {
        clients.clear();
    }
}
