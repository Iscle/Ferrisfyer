package me.iscle.ferrisfyer.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import me.iscle.ferrisfyer.BleService;
import me.iscle.ferrisfyer.IDeviceCallback;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.activity.BtDeviceChooserActivity;
import me.iscle.ferrisfyer.databinding.FragmentDeviceControlBinding;
import me.iscle.ferrisfyer.model.Device;

import static android.content.Context.BIND_AUTO_CREATE;

public class DeviceControlFragment extends BaseFragment {
    private static final String TAG = "DeviceControlFragment";

    private FragmentDeviceControlBinding binding;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CHOOSE_BT_DEVICE = 2;

    private BleService service;
    private Mode mode;

    private Handler handler;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
        mode = (Mode) requireArguments().get("mode");
        if (mode == null) throw new IllegalArgumentException("Mode can't be null!");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceControlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: IMPLEMENT GRAPH CODE

        binding.motorSlider.addOnChangeListener(changeListener);
        binding.scanForDevices.setOnClickListener((v) -> {
            Intent i = new Intent(requireActivity(), BtDeviceChooserActivity.class);
            startActivityForResult(i, REQUEST_CHOOSE_BT_DEVICE);
        });

        if (mode == Mode.LOCAL) {
            if (BluetoothAdapter.getDefaultAdapter() == null || !isBluetoothLeSupported()) {
                Toast.makeText(requireContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
                requireActivity().finishAffinity();
                return;
            }

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) requestEnableBluetooth();

            Intent serviceIntent = new Intent(getContext(), BleService.class);
            requireActivity().bindService(serviceIntent, bleServiceConnection, BIND_AUTO_CREATE);
        } else if (mode == Mode.REMOTE) {
            requireActivity().setTitle(R.string.controlling_remote_device);

            // showSelectUserDialog();
        } else {
            throw new RuntimeException("Invalid mode!");
        }
    }

    private final Slider.OnChangeListener changeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (!bleServiceConnection.isServiceConnected()) return;

            if (value == 0f) {
                service.stopMotor();
                handler.post(() -> binding.motorStatus.setText(R.string.stopped));
            } else {
                service.startMotor((byte) value);
                handler.post(() -> binding.motorStatus.setText(R.string.running));
            }

            // TODO: modificar dataset
            binding.chart.notifyDataSetChanged();
            binding.chart.invalidate();
        }
    };

    private final IDeviceCallback deviceCallback = new IDeviceCallback() {
        @Override
        public void onRssiUpdated(Device device) {
            int bars = WifiManager.calculateSignalLevel(device.getRssi(), 5);
            Log.d(TAG, "onRssiUpdated: " + bars);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBatteryUpdated(Device device) {
            handler.post(() -> binding.batteryLevel.setText(Byte.toString(device.getBattery())));
        }

        @Override
        public void onMacUpdated(Device device) {

        }

        @Override
        public void onSnUpdated(Device device) {

        }
    };

    @Override
    public void onDestroy() {
        if (service != null) service.removeDeviceCallback(deviceCallback);

        if (bleServiceConnection.isServiceConnected()) {
            requireActivity().unbindService(bleServiceConnection);
        }
        super.onDestroy();
    }

    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private boolean isBluetoothLeSupported() {
        return requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private final ExtendedServiceConnection bleServiceConnection = new ExtendedServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((BleService.BLEBinder) binder).getService();
            service.setDeviceCallback(deviceCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
        }
    };

    private class ExtendedServiceConnection implements ServiceConnection {

        private boolean isServiceConnected = false;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isServiceConnected = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceConnected = false;
        }

        public boolean isServiceConnected() {
            return isServiceConnected;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHOOSE_BT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                service.connectDevice(data.getStringExtra("device_address"));
                Toast.makeText(requireContext(), R.string.bluetooth_connected, Toast.LENGTH_LONG).show();
                handler.post(() -> binding.connectionStatus.setText(R.string.connected));
            } else {
                Toast.makeText(requireContext(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
                handler.post(() -> binding.connectionStatus.setText(R.string.disconnected));
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public enum Mode {
        LOCAL,
        REMOTE
    }
}
