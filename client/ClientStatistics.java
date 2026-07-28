package client;

import model.Packet;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks and calculates real-time network performance metrics for a UDP client.
 * Analyzes sequence numbers and timestamps to detect packet loss, duplicate packets,
 * out-of-order packets, throughput, and latency.
 */
public class ClientStatistics {

    private final long expectedPackets;

    private long receivedPacketsCount = 0;
    private long uniquePacketsCount = 0;
    private long duplicatePacketsCount = 0;
    private long outOfOrderPacketsCount = 0;
    private long totalBytesReceived = 0;

    private long minDelayMs = Long.MAX_VALUE;
    private long maxDelayMs = 0;
    private long totalDelayMs = 0;

    private long firstPacketTimeMs = 0;
    private long lastPacketTimeMs = 0;

    private long maxSequenceSeen = 0;
    private final Set<Long> receivedSequences = new HashSet<>();

    /**
     * Constructs ClientStatistics with an expected total packet count.
     *
     * @param expectedPackets Total number of packets sent by the server
     */
    public ClientStatistics(long expectedPackets) {
        this.expectedPackets = expectedPackets;
    }

    /**
     * Records and analyzes an incoming packet.
     *
     * @param packet            Deserialized Packet object received
     * @param receiveTimeMillis Epoch timestamp (ms) when packet arrived
     * @param packetSizeBytes   Total raw byte size of the received datagram packet
     */
    public synchronized void recordPacket(Packet packet, long receiveTimeMillis, int packetSizeBytes) {
        if (receivedPacketsCount == 0) {
            firstPacketTimeMs = receiveTimeMillis;
        }
        lastPacketTimeMs = receiveTimeMillis;

        receivedPacketsCount++;
        totalBytesReceived += packetSizeBytes;

        // Calculate latency (transit delay)
        long delay = receiveTimeMillis - packet.getTimestamp();
        if (delay < 0) {
            delay = 0; // Guard against minor clock drift
        }
        totalDelayMs += delay;
        if (delay < minDelayMs) {
            minDelayMs = delay;
        }
        if (delay > maxDelayMs) {
            maxDelayMs = delay;
        }

        long seqNum = packet.getSequenceNumber();

        // Sequence number analysis for duplicates and out-of-order detection
        if (receivedSequences.contains(seqNum)) {
            duplicatePacketsCount++;
        } else {
            receivedSequences.add(seqNum);
            uniquePacketsCount++;

            // Out of order: sequence number is smaller than the maximum sequence number already seen
            if (seqNum < maxSequenceSeen) {
                outOfOrderPacketsCount++;
            } else {
                maxSequenceSeen = seqNum;
            }
        }
    }

    public long getExpectedPackets() {
        return expectedPackets;
    }

    public synchronized long getReceivedPackets() {
        return receivedPacketsCount;
    }

    public synchronized long getLostPackets() {
        long lost = expectedPackets - uniquePacketsCount;
        return Math.max(0, lost);
    }

    public synchronized long getDuplicatePackets() {
        return duplicatePacketsCount;
    }

    public synchronized long getOutOfOrderPackets() {
        return outOfOrderPacketsCount;
    }

    public synchronized long getBytesReceived() {
        return totalBytesReceived;
    }

    public synchronized double getAverageDelayMs() {
        return receivedPacketsCount > 0 ? (double) totalDelayMs / receivedPacketsCount : 0.0;
    }

    public synchronized long getMinDelayMs() {
        return minDelayMs == Long.MAX_VALUE ? 0 : minDelayMs;
    }

    public synchronized long getMaxDelayMs() {
        return maxDelayMs;
    }

    public synchronized double getPacketsPerSecond() {
        long elapsedMs = lastPacketTimeMs - firstPacketTimeMs;
        if (elapsedMs <= 0) {
            return receivedPacketsCount;
        }
        return (receivedPacketsCount * 1000.0) / elapsedMs;
    }
}
