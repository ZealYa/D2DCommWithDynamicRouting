package tausif.androidprojects.d2dcommwithdynamicrouting;

public class Constants {
    public static String hostBluetoothAddress = "";
    public static String hostWifiAddress = "";

    public static final int WIFI_DEVICE = 1;
    public static final int BLUETOOTH_DEVICE = 0;

    public static final int RTT_PACKET_SIZE = 200;  //in bytes
    public static final int LOSS_RATIO_PACKET_SIZE = 100;   //in bytes
    public static final int MAX_LOSS_RATIO_PACKETS_TO_SENT = 50;

    public static int timeSlotCount = 0;
    public static final int timeSlotLength = 15;    // in seconds
}
