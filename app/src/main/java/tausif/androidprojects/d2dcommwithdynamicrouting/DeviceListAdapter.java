package tausif.androidprojects.d2dcommwithdynamicrouting;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
        View rowView = mInflater.inflate(android.R.layout.simple_list_item_1, viewGroup, false);
        TextView title = (TextView)rowView.findViewById(android.R.id.text1);
        Device currentDevice = devices.get(i);
        if (currentDevice.deviceType == Constants.WIFI_DEVICE) {
            if (currentDevice.wifiDevice == null) {
                title.setTextColor(Color.GREEN);
                title.setText("Wifi Devices");
            }
            else
                title.setText(currentDevice.wifiDevice.deviceName);
        }
        else {
            if (currentDevice.bluetoothDevice == null) {
                title.setTextColor(Color.GREEN);
                title.setText("Bluetooth Devices");
            }
            else
                title.setText(currentDevice.bluetoothDevice.getName() + " " + String.valueOf(currentDevice.packetLossRatio));
        }
        return rowView;
    }
}
