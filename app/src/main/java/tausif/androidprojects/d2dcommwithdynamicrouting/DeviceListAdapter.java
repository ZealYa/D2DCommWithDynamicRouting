package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.content.Context;
import android.graphics.Color;
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
        View rowView = mInflater.inflate(R.layout.device_list_row, viewGroup, false);

        TextView title = rowView.findViewById(R.id.device_name_textView);
        TextView deviceType = rowView.findViewById(R.id.device_type_textView);
        Device currentDevice = devices.get(i);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            title.setText(currentDevice.wifiDevice.deviceAddress);
            deviceType.setText("WiFi Direct");
        }
        else {
            title.setText(currentDevice.bluetoothDevice.getAddress());
            deviceType.setText("Bluetooth");
        }

        Button connect = (Button)rowView.findViewById(R.id.connect_button);
        connect.setTag(i);
        if (currentDevice.connected)
            connect.setText("disconnect");
        else
            connect.setText("connect");

        Button rtt = (Button)rowView.findViewById(R.id.rtt_button);
        rtt.setTag(i);

        Button pktLoss = (Button)rowView.findViewById(R.id.pkt_loss_button);
        pktLoss.setTag(i);

        Button throughput = (Button)rowView.findViewById(R.id.throughput_button);
        throughput.setTag(i);

        return rowView;
    }
}
