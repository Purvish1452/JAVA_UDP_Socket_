package util;

import model.Packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Utility class responsible for serializing Packet objects into raw byte arrays
 * for UDP transmission, and deserializing received byte arrays back into Packet objects.
 * Uses Java Standard Library binary I/O streams (DataOutputStream / DataInputStream).
 */
public class PacketUtil {

    /**
     * Serializes a Packet object into a binary byte array suitable for DatagramPacket payload.
     *
     * Binary Wire Format:
     * +-------------------+-----------------------+-------------------+--------------------+-----------------------+
     * | packetId (8 B)    | sequenceNumber (8 B)  | timestamp (8 B)   | payloadSize (4 B)  | payload (variable B)  |
     * +-------------------+-----------------------+-------------------+--------------------+-----------------------+
     *
     * @param packet The Packet object to convert to bytes
     * @return Raw byte array formatted for network transfer
     * @throws IOException If an encoding or memory stream error occurs
     */
    public static byte[] serialize(Packet packet) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeLong(packet.getPacketId());
            dos.writeLong(packet.getSequenceNumber());
            dos.writeLong(packet.getTimestamp());

            byte[] payload = packet.getPayload();
            int payloadSize = (payload != null) ? payload.length : 0;
            dos.writeInt(payloadSize);

            if (payloadSize > 0) {
                dos.write(payload, 0, payloadSize);
            }

            dos.flush();
            return baos.toByteArray();
        }
    }

    /**
     * Deserializes a binary byte array back into a Packet object.
     *
     * @param data Raw byte array received from a DatagramPacket
     * @return Reconstructed Packet instance
     * @throws IOException If decoding fails or stream ends prematurely
     */
    public static Packet deserialize(byte[] data) throws IOException {
        return deserialize(data, data.length);
    }

    /**
     * Deserializes a slice of a binary byte array back into a Packet object.
     *
     * @param data   Raw byte buffer received from UDP DatagramPacket
     * @param length Actual number of bytes received in the DatagramPacket
     * @return Reconstructed Packet instance
     * @throws IOException If decoding fails or buffer data is incomplete
     */
    public static Packet deserialize(byte[] data, int length) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data, 0, length);
        try (DataInputStream dis = new DataInputStream(bais)) {
            long packetId = dis.readLong();
            long sequenceNumber = dis.readLong();
            long timestamp = dis.readLong();
            int payloadSize = dis.readInt();

            byte[] payload = new byte[payloadSize];
            if (payloadSize > 0) {
                dis.readFully(payload);
            }

            return new Packet(packetId, sequenceNumber, timestamp, payload, payloadSize);
        }
    }
}
