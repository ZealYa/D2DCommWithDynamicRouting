package tausif.androidprojects.files;

import android.os.Environment;
import android.util.Log;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;
import java.util.Calendar;

import tausif.androidprojects.d2dcommwithdynamicrouting.Constants;


public class FileReceiver implements Runnable{

    private final Socket clientSocket;
    private final OnTransferFinishListener onTransferFinishListener;
    int filesize = Constants.getThroughputFileLength();

    public FileReceiver(OnTransferFinishListener onTransferFinishListener, Socket clientSocket){
        this.clientSocket = clientSocket;
        this.onTransferFinishListener = onTransferFinishListener;
    }

    @Override
    public void run() {
        while (clientSocket != null && clientSocket.isConnected()){
            DataInputStream dataInputStream = null;
            try {
                dataInputStream = new DataInputStream(clientSocket.getInputStream());
                byte[] buffer = new byte[20];
                int totalRead = 0;
                String fileName = "fileTransfer.txt";
                File file = new File(Environment.getExternalStorageDirectory(),fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                long startTime = Calendar.getInstance().getTimeInMillis();
                while (totalRead < filesize && clientSocket.isConnected()) {
                    int currentread = dataInputStream.read(buffer);
                    totalRead+= currentread;
                    fileOutputStream.write(buffer);
                }
                fileOutputStream.close();
                long endTime = Calendar.getInstance().getTimeInMillis();
                long timeTaken = endTime - startTime;
                double MB = (double)totalRead / 1000000.0;
                double s = (timeTaken) / 1000.0;
                double throughput = MB / s;
                if(onTransferFinishListener != null){
                    onTransferFinishListener.onReceiveSuccess(totalRead, timeTaken, throughput);
                }
                Log.e("file received",String.valueOf(timeTaken)+" milliseconds");
                Log.d("file received",String.valueOf(totalRead)+" bytes");
                Log.d("throughput", String.valueOf(throughput));
            }
            catch (EOFException ex){
                ex.printStackTrace();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
                if(onTransferFinishListener != null){
                    onTransferFinishListener.onError(e.toString());
                }
            }
        }
    }
}
