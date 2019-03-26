package tausif.androidprojects.d2dcommwithdynamicrouting;

public class PacketManager {
    public static String createBluetoothRTTPacket(int pktType, String sourceName, String destinationName, int pktSize) {
        String packet = String.valueOf(pktType) + "#" + sourceName + "#" + destinationName + "#" + String.valueOf(pktSize);
        packet = packet + "#";
        int remaining = pktSize - packet.length();
        for (int i = 0; i < remaining; i++) {
            packet += ".";
        }
        return packet;
    }

    public static String createWDRTTPacket(int pktType, int seqNo, String sourceAddress, String destinationAddress, int pktSize) {
        String packet = String.valueOf(pktType) + "#" + String.valueOf(seqNo) + "#" + sourceAddress + "#" + destinationAddress + "#" + String.valueOf(pktSize);
        packet = packet + "#";
        int remaining = pktSize - packet.length();
        for (int i = 0; i < remaining; i++) {
            packet += ".";
        }
        return packet;
    }

    public static String createLossRatioPacket(int pktType, int expNo, String sourceAddress, String destinationAddress) {
        String packet = String.valueOf(pktType) + "#" + String.valueOf(expNo) + "#" + sourceAddress + "#" + destinationAddress;
        packet = packet + "#";
        int remaining = Constants.LOSS_RATIO_PKT_SIZE - packet.length();
        for (int i=0;i<remaining; i++) {
            packet += ".";
        }
        return packet;
    }

    public static String createIpMacSyncPkt(int pktType, String macAddr) {
        return (String.valueOf(pktType) + "#" + macAddr + "#");
    }
}
