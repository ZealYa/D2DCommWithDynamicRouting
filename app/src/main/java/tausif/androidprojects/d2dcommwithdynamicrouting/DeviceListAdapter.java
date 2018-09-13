package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;



public class DeviceListAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private ArrayList<Device> devices;

    public DeviceListAdapter(Context context, ArrayList<Device> devices) {
        mContext = context;
        this.devices = devices;
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return devices.size();
    }

    @Override
    public Object getItem(int i) {
        return devices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        Device currentDevice = devices.get(i);
        View rowView;
        String deviceName;

        if (currentDevice.deviceType == Constants.BLUETOOTH_DEVICE) {
            rowView = mInflater.inflate(R.layout.bt_device_list_row, viewGroup, false);
            deviceName = currentDevice.bluetoothDevice.getName();

            Button connect = (Button)rowView.findViewById(R.id.connect_button);
            connect.setTag(i);
            if (currentDevice.connected)
                connect.setText("disconnect");
            else
                connect.setText("connect");

            Button rtt = (Button)rowView.findViewById(R.id.rtt_button);
            rtt.setTag(i);

            Button tcpThroughput = (Button)rowView.findViewById(R.id.tcp_throughput_button);
            tcpThroughput.setTag(i);

            Button disconnect = (Button)rowView.findViewById(R.id.disconnect_button);
            disconnect.setTag(i);
        }
        else {
            rowView = mInflater.inflate(R.layout.wd_device_list_row, viewGroup, false);
            deviceName = currentDevice.wifiDevice.deviceName;

            Button connect = (Button)rowView.findViewById(R.id.connect_button);
            connect.setTag(i);
            if (currentDevice.connected)
                connect.setText("disconnect");
            else
                connect.setText("connect");

            Button rttUDP = (Button)rowView.findViewById(R.id.rtt_udp_button);
            rttUDP.setTag(i);

            Button pktLoss = (Button)rowView.findViewById(R.id.pkt_loss_button);
            pktLoss.setTag(i);

            Button udpThroughput = (Button)rowView.findViewById(R.id.udp_throughput_button);
            udpThroughput.setTag(i);

            Button tcpThroughput = (Button)rowView.findViewById(R.id.tcp_throughput_button);
            tcpThroughput.setTag(i);

            Button disconnect = (Button)rowView.findViewById(R.id.disconnect_button);
            disconnect.setTag(i);
        }

        TextView title = rowView.findViewById(R.id.device_name_textView);
        title.setText(deviceName);
        return rowView;
    }
}
