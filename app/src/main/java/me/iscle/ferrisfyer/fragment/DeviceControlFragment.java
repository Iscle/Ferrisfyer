package me.iscle.ferrisfyer.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.data.LineData;
import com.google.android.material.slider.Slider;

import me.iscle.ferrisfyer.BleService;
import me.iscle.ferrisfyer.IDeviceCallback;
import me.iscle.ferrisfyer.IDeviceControl;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mode = (Mode) getArguments().get("mode");
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
                getActivity().finishAffinity();
                return;
            }

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) requestEnableBluetooth();

            binding.remoteControl.setVisibility(View.VISIBLE);

            Intent serviceIntent = new Intent(getContext(), BleService.class);
            getActivity().bindService(serviceIntent, bleServiceConnection, BIND_AUTO_CREATE);
        } else if (mode == Mode.REMOTE) {
            requireActivity().setTitle(R.string.controlling_remote_device);
            binding.remoteControl.setVisibility(View.GONE);

            // showSelectUserDialog();
        } else {
            throw new RuntimeException("Invalid mode!");
        }
    }

    private final Slider.OnChangeListener changeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (value == 0f) {
                service.stopMotor();
            } else {
                service.startMotor((byte) value);
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
            binding.batteryLevel.setText(Byte.toString(device.getBattery()));
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
        getActivity().unbindService(bleServiceConnection);
        super.onDestroy();
    }

    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private boolean isBluetoothLeSupported() {
        return getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private void setServerConnectionStatus(int status) {
        if (status == 0) {
            binding.serverConnectionStatus.setText(R.string.disconnected);
        } else if (status == 1) {
            binding.serverConnectionStatus.setText(R.string.connected);
        } else if (status == 2) {
            binding.serverConnectionStatus.setText(R.string.reconnecting);
        }
    }

    private void setConnectionStatus(int status) {
        if (status == 0) {
            binding.connectionStatus.setText(R.string.disconnected);
        } else if (status == 1) {
            binding.connectionStatus.setText(R.string.connected);
        } else if (status == 2) {
            binding.connectionStatus.setText(R.string.reconnecting);
        }
    }

    private void setMotorStatus(boolean running) {
        if (running) {
            binding.motorStatus.setText(R.string.running);
        } else {
            binding.motorStatus.setText(R.string.stopped);
        }
    }

    private final ServiceConnection bleServiceConnection = new ServiceConnection() {
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHOOSE_BT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                service.connectDevice(data.getStringExtra("device_address"));

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public enum Mode {
        LOCAL,
        REMOTE
    }
}
