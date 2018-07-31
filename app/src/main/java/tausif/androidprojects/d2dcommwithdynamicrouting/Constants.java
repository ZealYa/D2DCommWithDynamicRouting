package tausif.androidprojects.d2dcommwithdynamicrouting;

public class Constants {
    public static String hostBluetoothAddress = "";
    public static String hostBluetoothName = "";
    public static String hostWifiAddress = "";

    public static final int WIFI_DEVICE = 1;
    public static final int BLUETOOTH_DEVICE = 0;

    public static int RTT_PKT_SIZE = 225;  //in bytes
    public static final int LOSS_RATIO_PKT_SIZE = 50;   //in bytes
    public static final int MAX_LOSS_RATIO_PKTS = 100;

    public static int timeSlotNo = 0;
    public static final int timeSlotLength = 10;    // in seconds

    public static final int BT_RSSI = 0;
    public static final int BT_RTT = 1;
    public static final int BT_PACKET_LOSS = 2;
    public static final int WIFI_RTT = 3;
    public static final int WIFI_PACKET_LOSS = 4;

    public static final int TYPE_RTT = 1;
    public static final int TYPE_RTT_RET = 2;
    public static final int TYPE_PKT_LOSS = 3;

    public static final int noOfRuns = 60;
}
