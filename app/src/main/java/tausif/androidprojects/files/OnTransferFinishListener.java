package tausif.androidprojects.files;



public interface OnTransferFinishListener {
    public void onError(String msg);
    public void onSendSuccess(String name);
    public void onReceiveSuccess(String name);
}
