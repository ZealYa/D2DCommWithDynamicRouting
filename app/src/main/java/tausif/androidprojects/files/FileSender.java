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

import tausif.androidprojects.d2dcommwithdynamicrouting.Constants;


public class FileSender implements Runnable{

    private final String path;
    private final String name;
    private final Socket clientSocket;
    private final OnTransferFinishListener onTransferFinishListener;


    FileSender(OnTransferFinishListener onTransferFinishListener,Socket clientSocket, String path, String name){
        this.path = path;
        this.name = name;
        this.clientSocket = clientSocket;
        this.onTransferFinishListener = onTransferFinishListener;
    }

    @Override
    public void run() {
        try {
            OutputStream outputStream = clientSocket.getOutputStream();
            String data = "this is a test file transfer";
            while (data.length() < 800) {
                data = data.concat(data);
            }
            byte[] buffer = data.getBytes();
            int totalWrite = 0;
            while (totalWrite < Constants.getThroughputFileLength()) {
                outputStream.write(buffer, 0, buffer.length);
                totalWrite += buffer.length;
                outputStream.flush();
            }
            if(onTransferFinishListener != null){
                onTransferFinishListener.onSendSuccess();
            }
        } catch (Exception e) {
            e.printStackTrace();
            if(onTransferFinishListener != null){
                onTransferFinishListener.onError(e.toString());
            }
        }
    }
}
