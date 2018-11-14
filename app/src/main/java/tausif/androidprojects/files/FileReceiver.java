package tausif.androidprojects.files;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Calendar;

import tausif.androidprojects.d2dcommwithdynamicrouting.Constants;


public class FileReceiver implements Runnable{

    private final Socket clientSocket;
//    private final String TAG = FileReceiver.class.getName();
    private final OnTransferFinishListener onTransferFinishListener;
//    private final int FILE_CHUNK_SIZE = 256;
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
//                String fileName = null;
                byte[] buffer = new byte[20];
//                int contentLength = 0;
//                dataInputStream.readFully(buffer,0,2);
//                contentLength = buffer[0] * FILE_CHUNK_SIZE + buffer[1];
//                dataInputStream.readFully(buffer,0,contentLength);
//                fileName = new String(buffer,0,contentLength);
//                dataInputStream.readFully(buffer,0,2);
//                contentLength = buffer[0] * FILE_CHUNK_SIZE + buffer[1];
//                int read = 0;
//                int totalRead = 0;
//                File file = new File(Environment.getExternalStorageDirectory(),fileName);
//                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                while (totalRead < contentLength && clientSocket.isConnected()){
//                    read = dataInputStream.read(buffer,0,buffer.length);
//                    fileOutputStream.write(buffer,0,read);
//                    totalRead += read;
//                }
                int totalRead = 0;
//                Log.d("file size",String.valueOf(filesize));
                String fileName = "fileTransfer.txt";
                File file = new File(Environment.getExternalStorageDirectory(),fileName);
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                while (totalRead < filesize && clientSocket.isConnected()) {
                    int currentread = dataInputStream.read(buffer);
                    totalRead+= currentread;
                    String dataReceived = new String(buffer);
                    fileOutputStream.write(buffer);
                }
                fileOutputStream.close();
//                fileOutputStream.close();
//                if(onTransferFinishListener != null){
//                    onTransferFinishListener.onReceiveSuccess(fileName);
//                }
                Log.e("file received","File received successfully");
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
