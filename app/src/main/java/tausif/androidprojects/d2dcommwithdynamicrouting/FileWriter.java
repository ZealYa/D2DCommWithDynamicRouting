package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

class FileWriter {
    static boolean writeRTTResult(String deviceName, String pktSize, String distance, long[] RTTs, int deviceType, long[] cumulativeRTTs) {
        String prefix;
        if (deviceType == Constants.BLUETOOTH_DEVICE)
            prefix = "BT_";
        else
            prefix = "WD_";
        String filename = prefix + "RTT_" + deviceName + "_" + pktSize + "_" + distance + "_meters.txt";
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        if (deviceType == Constants.BLUETOOTH_DEVICE) {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                for (int i = 0; i < Constants.MAX_NO_OF_EXPS; i++) {
                    outputStreamWriter.append(String.valueOf(RTTs[i]));
                    outputStreamWriter.append("\n");
                }
                outputStreamWriter.close();
                fileOutputStream.close();
                return true;
            } catch (IOException FIOExec) {
                return false;
            }
        }
        else {
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                outputStreamWriter.append("RTTs\n");
                for (int i=0; i<Constants.MAX_NO_OF_EXPS; i++) {
                    outputStreamWriter.append(String.valueOf(RTTs[i]));
                    outputStreamWriter.append("\n");
                }
                outputStreamWriter.append("\nAccumulated RTTs\n");
                for (int i=0; i<Constants.MAX_NO_OF_EXPS; i++) {
                    outputStreamWriter.append(String.valueOf(cumulativeRTTs[i]));
                    outputStreamWriter.append("\n");
                }
                outputStreamWriter.close();
                fileOutputStream.close();
                return true;
            } catch (IOException FIOExec) {
                return false;
            }
        }

    }

    static boolean writePktLossResult(String deviceName, String distance, int pktReceiveCount[]) {
        String filename = "PKT_LOSS" + "_" + deviceName + "_" + distance + "_meters.txt";
        File pktLossResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pktLossResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i = 0; i < Constants.MAX_NO_OF_EXPS; i++) {
                int pktLossRatio = Constants.MAX_LOSS_RATIO_PKTS - pktReceiveCount[i];
                outputStreamWriter.append(String.valueOf(pktLossRatio));
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }

    static boolean writeThroughputRTTs(String deviceName, String distance, long[] RTTs) {
        String prefix = "WD_";
        String filename = prefix + "THROUGHPUT_RTT_" + deviceName + "_" + distance + "_meters.txt";
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i = 0; i < Constants.MAX_NO_OF_EXPS; i++) {
                outputStreamWriter.append(String.valueOf(RTTs[i]));
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }


    static void writeRSSIResult(String distance, String timestamp, ArrayList<Device> bluetoothDevices) {
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
