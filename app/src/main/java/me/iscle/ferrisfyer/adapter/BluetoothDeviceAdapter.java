package me.iscle.ferrisfyer.adapter;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.iscle.ferrisfyer.R;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private List<BluetoothDevice> devices;
    private OnItemClickListener listener;

    public BluetoothDeviceAdapter() {
        this.devices = new ArrayList<>();
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.setBluetoothDevice(devices.get(position));
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void addBluetoothDevice(BluetoothDevice device) {
        for (BluetoothDevice dev : devices)
            if (dev.getAddress().equals(device.getAddress()))
                return;

        devices.add(device);
        notifyItemInserted(devices.size() - 1);
    }

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private View view;
        private TextView deviceName;
        private TextView deviceAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.view = itemView;
            this.deviceName = itemView.findViewById(R.id.device_name);
            this.deviceAddress = itemView.findViewById(R.id.device_address);
        }

        public void setBluetoothDevice(BluetoothDevice device) {
            if (device.getName() == null || device.getName().isEmpty())
                deviceName.setText("No name");
            else
                deviceName.setText(device.getName());
            deviceAddress.setText(device.getAddress());

            view.setOnClickListener(v -> {
                if (listener != null)
                    listener.onItemClick(device);
            });
        }
    }
}
