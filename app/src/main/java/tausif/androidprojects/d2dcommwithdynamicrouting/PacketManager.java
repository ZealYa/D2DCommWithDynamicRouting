package tausif.androidprojects.d2dcommwithdynamicrouting;

public class PacketManager {
    public static String createRTTPacket(int pktType, int timeSlotNo, String sourceAddress, String destinationAddress) {
        String packet = String.valueOf(pktType) + " " + String.valueOf(timeSlotNo) + " " +sourceAddress + " " + destinationAddress;
        int remaining = Constants.RTT_PACKET_SIZE - packet.length();
        packet = packet + " ";
        for (int i = 0; i < remaining; i++) {
            packet += "#";
        }
        return packet;
    }

    public static String createPacketLossRatioPacket(int timeSlotNo, String sourceAddress, String destinationAddress) {
        String packet = sourceAddress + " " + destinationAddress;
        return packet;
    }
}
