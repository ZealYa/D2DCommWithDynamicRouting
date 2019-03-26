package tausif.androidprojects.files;



public interface OnTransferFinishListener {
    public void onError(String msg);
    public void onSendSuccess();
    public void onReceiveSuccess(int filesize, long totalTime, double throughput);
}
