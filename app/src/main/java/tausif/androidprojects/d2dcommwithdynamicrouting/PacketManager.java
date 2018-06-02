package tausif.androidprojects.d2dcommwithdynamicrouting;

public class PacketManager {
    public static String createRTTPacket(int pktType, String sourceAddress, String destinationAddress) {
        String packet = String.valueOf(pktType) + "#" + sourceAddress + "#" + destinationAddress;
        packet = packet + "#";
        int remaining = Constants.RTT_PACKET_SIZE - packet.length();
        for (int i = 0; i < remaining; i++) {
            packet += ".";
        }
        return packet;
    }

    public static String createPacketLossRatioPacket(int pktType, int sequenceNo, String sourceAddress, String destinationAddress) {
        String packet = String.valueOf(sequenceNo) + "#" + sourceAddress + "#" + destinationAddress;
        packet = packet + "#";
        int remaining = Constants.LOSS_RATIO_PACKET_SIZE - packet.length();
        for (int i=0;i<remaining; i++) {
            packet = packet + ".";
        }
        return packet;
    }
}
