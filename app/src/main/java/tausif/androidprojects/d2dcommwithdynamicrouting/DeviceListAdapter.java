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
        if (devices.get(i).deviceType == -1)
            title.setTextColor(Color.GREEN);
        title.setText(devices.get(i).deviceName);
        return rowView;
    }
}
