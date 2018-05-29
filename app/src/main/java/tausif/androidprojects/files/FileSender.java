package tausif.androidprojects.files;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.Socket;



public class FileSender implements Runnable{

//    private final String path;
//    private final String name;
    private final String packet;
    private final Socket clientSocket;
    private final String TAG = FileSender.class.getName();
    private final OnTransferFinishListener onTransferFinishListener;


    public FileSender(OnTransferFinishListener onTransferFinishListener,Socket clientSocket, /*String path, String name*/String packet){
//        this.path = path;
//        this.name = name;
        this.packet = packet;
        this.clientSocket = clientSocket;
        this.onTransferFinishListener = onTransferFinishListener;
    }

    @Override
    public void run() {
//        File file = new File(path,name);
        try {
//            FileInputStream fileInputStream = new FileInputStream(file);
//            byte[] buffer = new byte[4096];
//            int read = 0;
//            buffer[0] = (byte) (name.length() / 256);
//            buffer[1] = (byte) (name.length() % 256);
//            System.arraycopy(name.getBytes(),0,buffer,2,name.length());
//            OutputStream outputStream = clientSocket.getOutputStream();
//            outputStream.write(buffer,0,name.length()+2);
//            outputStream.flush();
//            buffer[0] = (byte) (file.length() / 256);
//            buffer[1] = (byte) (file.length() % 256);
//            outputStream.write(buffer,0,2);
//            outputStream.flush();
//            while( (read = fileInputStream.read(buffer,0,buffer.length)) > 0){
//                outputStream.write(buffer,0,read);
//                outputStream.flush();
//            }
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(packet.getBytes());
            if(onTransferFinishListener != null){
                onTransferFinishListener.onSendSuccess(/*name*/"");
            }
//            Toast.makeText(context,"File send successfully " + name, Toast.LENGTH_SHORT).show();

            Log.e(TAG,"Send file successfully");
        } catch (Exception e) {
            e.printStackTrace();
            if(onTransferFinishListener != null){
                onTransferFinishListener.onError(e.toString());
            }
        }
    }
}
