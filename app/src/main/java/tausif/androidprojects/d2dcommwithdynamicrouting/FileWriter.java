package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class FileWriter {
    public static boolean writeRTTResult(String deviceName, String distance, String pktSize, long RTTValues[]) {
        final String filename = "RTT_" + deviceName + "_" + pktSize + "_" + distance + "_meters.txt";
        File RTTResults = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), filename);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(RTTResults);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
            for (int i = 0; i < Constants.noOfRuns; i++) {
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
}
