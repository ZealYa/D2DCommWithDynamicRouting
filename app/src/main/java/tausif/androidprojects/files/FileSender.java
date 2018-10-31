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

    private final String path;
    private final String name;
    private final Socket clientSocket;
//    private final String TAG = FileSender.class.getName();
    private final OnTransferFinishListener onTransferFinishListener;
    private final int FILE_CHUNK_SIZE = 256;


    public FileSender(OnTransferFinishListener onTransferFinishListener,Socket clientSocket, String path, String name){
        this.path = path;
        this.name = name;
        this.clientSocket = clientSocket;
        this.onTransferFinishListener = onTransferFinishListener;
    }

    @Override
    public void run() {
        File file = new File(path,name);
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[10096];
            int read = 0;
            buffer[0] = (byte) (name.length() / FILE_CHUNK_SIZE);
            buffer[1] = (byte) (name.length() % FILE_CHUNK_SIZE);
            System.arraycopy(name.getBytes(),0,buffer,2,name.length());
            OutputStream outputStream = clientSocket.getOutputStream();
            outputStream.write(buffer,0,name.length()+2);
            outputStream.flush();
            buffer[0] = (byte) (file.length() / FILE_CHUNK_SIZE);
            buffer[1] = (byte) (file.length() % FILE_CHUNK_SIZE);
            outputStream.write(buffer,0,2);
            outputStream.flush();
            while( (read = fileInputStream.read(buffer,0,buffer.length)) > 0){
                outputStream.write(buffer,0,read);
                outputStream.flush();
            }
            if(onTransferFinishListener != null){
                onTransferFinishListener.onSendSuccess(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(onTransferFinishListener != null){
                onTransferFinishListener.onError(e.toString());
            }
        }
    }
}
