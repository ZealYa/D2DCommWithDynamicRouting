package tausif.androidprojects.d2dcommwithdynamicrouting;

public class PacketMaker {

    public static String createRTTPacket(String sourceAddress, String destinationAddress) {
        String packet = sourceAddress + " " + destinationAddress;
        return packet;
    }

    public static String createPacketLossRatioPacket(String sourceAddress, String destinationAddress) {
        String packet = sourceAddress + " " + destinationAddress;
        return packet;
    }
}
