package model;

import java.io.Serializable;

/**
 * Represents a network packet transferred over UDP between the Server and Clients.
 * Serves as the core Data Transfer Object (DTO) containing metadata for monitoring:
 * packet identification, sequencing, timestamps, and actual byte payload.
 */
public class Packet implements Serializable {
    private static final long serialVersionUID = 1L;

    private long packetId;
    private long sequenceNumber;
    private long timestamp;
    private byte[] payload;
    private int payloadSize;

    /**
     * Default no-argument constructor.
     */
    public Packet() {
    }

    /**
     * Convenient parameterized constructor.
     *
     * @param packetId       Unique identifier for the packet
     * @param sequenceNumber Sequential order index of the packet
     * @param timestamp      System time (in milliseconds) when the packet was created
     * @param payload        The raw byte content of the packet
     */
    public Packet(long packetId, long sequenceNumber, long timestamp, byte[] payload) {
        this.packetId = packetId;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.payload = (payload != null) ? payload : new byte[0];
        this.payloadSize = this.payload.length;
    }

    /**
     * Full parameterized constructor specifying explicit payload size.
     *
     * @param packetId       Unique identifier for the packet
     * @param sequenceNumber Sequential order index of the packet
     * @param timestamp      System time (in milliseconds) when the packet was created
     * @param payload        The raw byte content of the packet
     * @param payloadSize    Size of the payload in bytes
     */
    public Packet(long packetId, long sequenceNumber, long timestamp, byte[] payload, int payloadSize) {
        this.packetId = packetId;
        this.sequenceNumber = sequenceNumber;
        this.timestamp = timestamp;
        this.payload = (payload != null) ? payload : new byte[0];
        this.payloadSize = payloadSize;
    }

    // Getters and Setters

    public long getPacketId() {
        return packetId;
    }

    public void setPacketId(long packetId) {
        this.packetId = packetId;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = (payload != null) ? payload : new byte[0];
        this.payloadSize = this.payload.length;
    }

    public int getPayloadSize() {
        return payloadSize;
    }

    public void setPayloadSize(int payloadSize) {
        this.payloadSize = payloadSize;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "packetId=" + packetId +
                ", sequenceNumber=" + sequenceNumber +
                ", timestamp=" + timestamp +
                ", payloadSize=" + payloadSize +
                '}';
    }
}
