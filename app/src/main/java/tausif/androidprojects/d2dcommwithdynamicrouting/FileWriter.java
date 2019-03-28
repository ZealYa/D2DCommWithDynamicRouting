package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;

class FileWriter {

    static boolean writeRSSIResult(String distance, ArrayList<Device> bluetoothDevices) {
        long currentTime = Calendar.getInstance().getTimeInMillis();
        String filename = "RSSI_" + String.valueOf(currentTime) + "_" + distance + "_meters.txt";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.RESULT_FOLDER_NAME;
        path = path + "/" + filename;
        File RSSIResults = new File(path);
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
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }

    static boolean writeRTTResult(String deviceName, String distance, long[] RTTs, int deviceType, long[] cumulativeRTTs) {
        String prefix;
        if (deviceType == Constants.BLUETOOTH_DEVICE)
            prefix = "BT_";
        else
            prefix = "WD_";
        String filename = prefix + "RTT_" + deviceName + "_" + distance + "_meters.txt";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.RESULT_FOLDER_NAME;
        path = path + "/" + filename;
        File RTTResults = new File(path);
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
                outputStreamWriter.append("RTT AccRTT\n");
                for (int i=0; i<Constants.MAX_NO_OF_EXPS; i++) {
                    String str = String.valueOf(RTTs[i]) + " " + String.valueOf(cumulativeRTTs[i]);
                    outputStreamWriter.append(str);
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
        String filename = "PktLoss" + "_" + deviceName + "_" + "TO_" + Constants.hostWifiName + "_" + distance + "_meters.txt";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.RESULT_FOLDER_NAME;
        path = path + "/" + filename;
        File pktLossResults = new File(path);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(pktLossResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i = 0; i < Constants.MAX_PKT_LOSS_EXPS; i++) {
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

    static boolean writeThroughputRTTs(String deviceName, String distance, long[] RTTs, int pktSizes[], long[] cumulativeRTTs) {
        String prefix = "WD_";
        String filename = prefix + "THRPT_RTT_" + deviceName + "_" + distance + "_meters.txt";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.RESULT_FOLDER_NAME;
        path = path + "/" + filename;
        File RTTResults = new File(path);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append("RTT PktSize AccRTT\n");
            for (int i = 0; i < Constants.MAX_NO_OF_EXPS; i++) {
                String str = String.valueOf(RTTs[i]) + " " + String.valueOf(pktSizes[i]) + " " + String.valueOf(cumulativeRTTs[i]);
                outputStreamWriter.append(str);
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }

    static boolean writeTCPThroughput(String deviceName, String distance, int deviceType, int filesize, long totalTime, double throughput) {
        String prefix = "";
        if (deviceType == Constants.WIFI_DEVICE)
            prefix = "WD_";
        else
            prefix = "BT_";
        String filename = prefix + "TcpThrpt_" + deviceName + "_TO_" + Constants.hostWifiName + "_" + distance + "_meters.txt";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Constants.RESULT_FOLDER_NAME;
        path = path + "/" + filename;
        File results = new File(path);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(results);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            String str = "FileSize TotalTime Throughput\n";
            outputStreamWriter.append(str);
            str = String.valueOf(filesize) + " " + String.valueOf(totalTime) + " " + String.valueOf(throughput) + "\n";
            outputStreamWriter.append(str);
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }
}
