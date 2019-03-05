package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

class FileWriter {

    static boolean writeRSSIResult(String distance, ArrayList<Device> bluetoothDevices) {
        String filename = "RSSI_" + "_" + distance + "_meters.txt";
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
        String filename = "PktLoss" + "_" + deviceName + "_" + "TO_" + Constants.hostWifiName + "_" + distance + "_meters.txt";
        File pktLossResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
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
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append("RTTs\n");
            for (int i = 0; i < Constants.MAX_NO_OF_EXPS; i++) {
                outputStreamWriter.append(String.valueOf(RTTs[i]));
                outputStreamWriter.append("\n");
            }
            outputStreamWriter.append("\nPkt sizes\n");
            for (int i=0; i<Constants.MAX_NO_OF_EXPS; i++) {
                outputStreamWriter.append(String.valueOf(pktSizes[i]));
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

    static boolean writeTCPThroughput(String deviceName, int filesize, long totalTime, double throughput, String distance) {
        String filename = "TcpThrpt_" + deviceName + "_TO_" + Constants.hostWifiName + "_" + distance + "_meters.txt";
        File results = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(results);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            outputStreamWriter.append(String.valueOf(filesize));
            outputStreamWriter.append("\n");
            outputStreamWriter.append(String.valueOf(totalTime));
            outputStreamWriter.append("\n");
            outputStreamWriter.append(String.valueOf(throughput));
            outputStreamWriter.close();
            fileOutputStream.close();
            return true;
        } catch (IOException FIOExec) {
            return false;
        }
    }
}
