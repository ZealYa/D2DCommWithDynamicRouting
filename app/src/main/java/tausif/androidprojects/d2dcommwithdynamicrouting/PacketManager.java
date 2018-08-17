package tausif.androidprojects.d2dcommwithdynamicrouting;

public class PacketManager {
    public static String createRTTPacket(int pktType, String sourceAddress, String destinationAddress, int pktSize) {
        String packet = String.valueOf(pktType) + "#" + sourceAddress + "#" + destinationAddress + "#" + String.valueOf(pktSize);
        packet = packet + "#";
        int remaining = pktSize - packet.length();
        for (int i = 0; i < remaining; i++) {
            packet += ".";
        }
        return packet;
    }

    public static String createLossRatioPacket(int pktType, String sourceAddress, String destinationAddress) {
        String packet = String.valueOf(pktType) + "#" + sourceAddress + "#" + destinationAddress;
        packet = packet + "#";
        int remaining = Constants.LOSS_RATIO_PKT_SIZE - packet.length();
        for (int i=0;i<remaining; i++) {
            packet = packet + ".";
        }
        return packet;
    }

    public static String createIpMacSyncPkt(int pktType, String macAddr) {
        return (String.valueOf(pktType) + "#" + macAddr + "#");
    }
}
