package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class FileWriter {
    public static boolean writeRTTResult(String deviceName, String pktSize, String distance, long RTTValues[]) {
        String filename = "RTT_" + deviceName + "_" + pktSize + "_" + distance + "_meters.txt";
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i = 0; i < Constants.noOfExps; i++) {
                outputStreamWriter.append(String.valueOf(RTTValues[i]));
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }

    public static void writeRSSIResult(String distance, String timestamp, ArrayList<Device> bluetoothDevices) {
        String filename = "RSSI_" + timestamp + "_" + distance + "_meters.txt";
        File RSSIResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RSSIResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (Device device:bluetoothDevices
                 ) {
                String rssiString = device.bluetoothDevice.getName() + " " + String.valueOf(device.rssi);
                outputStreamWriter.append(rssiString);
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
        } catch (IOException FIOExec) {
        }
    }
}
