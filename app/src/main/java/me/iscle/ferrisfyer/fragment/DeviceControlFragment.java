package me.iscle.ferrisfyer.fragment;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.slider.Slider;

import me.iscle.ferrisfyer.BLEService;
import me.iscle.ferrisfyer.Constants;
import me.iscle.ferrisfyer.DeviceControlActivity;
import me.iscle.ferrisfyer.Ferrisfyer;
import me.iscle.ferrisfyer.IDeviceControl;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.WebSocketManager;
import me.iscle.ferrisfyer.activity.BtDeviceChooserActivity;
import me.iscle.ferrisfyer.activity.MainActivity;
import me.iscle.ferrisfyer.databinding.FragmentDeviceControlBinding;
import me.iscle.ferrisfyer.model.Device;

import static android.content.Context.BIND_AUTO_CREATE;

public class DeviceControlFragment extends Fragment implements ServiceConnection {

    private FragmentDeviceControlBinding binding;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CHOOSE_BT_DEVICE = 2;
    private static final int REQUEST_LOGIN = 3;

    private byte lastSliderValue = -1;
    private IDeviceControl deviceControl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDeviceControlBinding.inflate(inflater, container, false);

        requireActivity().setTitle(R.string.app_name);

        // TODO: IMPLEMENT GRAPH CODE

        binding.motorSlider.addOnSliderTouchListener(touchListener);

        binding.scanForDevices.setOnClickListener((v) -> {
            Intent i = new Intent(requireActivity(), BtDeviceChooserActivity.class);
            startActivityForResult(i, REQUEST_CHOOSE_BT_DEVICE);
        });

        Ferrisfyer.Mode mode = ((MainActivity) requireActivity()).getFerrisfyer().getMode();
        if (mode == Ferrisfyer.Mode.LOCAL) {
            requireActivity().setTitle(R.string.controlling_local_device);
            binding.remoteControl.setVisibility(View.VISIBLE);

            Intent serviceIntent = new Intent(requireActivity(), BLEService.class);
            requireActivity().bindService(serviceIntent, this, BIND_AUTO_CREATE);

            if (BluetoothAdapter.getDefaultAdapter() == null || !isBluetoothLeSupported()) {
                Toast.makeText(requireContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
                return binding.getRoot();
            }

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                requestEnableBluetooth();
            }
        } else if (mode == Ferrisfyer.Mode.REMOTE) {
            requireActivity().setTitle(R.string.controlling_remote_device);
            binding.remoteControl.setVisibility(View.GONE);

            // showSelectUserDialog();
        } else {
            throw new RuntimeException("Invalid mode!");
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_WEBSOCKET_CONNECTED);
        intentFilter.addAction(Constants.ACTION_WEBSOCKET_DISCONNECTED);
        intentFilter.addAction(Constants.ACTION_READ_REMOTE_INFO);
        intentFilter.addAction(Constants.ACTION_READ_REMOTE_BATTERY);
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(broadcastReceiver, intentFilter);

        return binding.getRoot();
    }

    private final Slider.OnSliderTouchListener touchListener =
            new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(Slider slider) {
                    ((MainActivity) requireActivity()).service.startMotor((byte) slider.getValue());
                }

                @Override
                public void onStopTrackingTouch(Slider slider) {

                }
            };

    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private boolean isBluetoothLeSupported() {
        return requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_WEBSOCKET_CONNECTED:
                    setServerConnectionStatus(1);
                    break;

                case Constants.ACTION_WEBSOCKET_DISCONNECTED:
                    setServerConnectionStatus(0);
                    break;

                case Constants.ACTION_READ_REMOTE_INFO:
                case Constants.ACTION_READ_REMOTE_BATTERY:
                    // binding.batteryLevel.setText(intent.getByteExtra("battery", (byte) 0));
                    break;
            }
        }
    };

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

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        ((MainActivity) requireActivity()).service = ((BLEService.BLEBinder) binder).getService();
        if (((MainActivity) requireActivity()).getFerrisfyer().getMode() == Ferrisfyer.Mode.LOCAL) {
            this.deviceControl = ((MainActivity) requireActivity()).service;
            if (((MainActivity) requireActivity()).service.isConnected()) {
                setServerConnectionStatus(1);
                setConnectionStatus(((MainActivity) requireActivity()).service.getDeviceStatus());
                setMotorStatus(((MainActivity) requireActivity()).service.getDeviceMotorStatus());
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        ((MainActivity) requireActivity()).service = null;
        if (((MainActivity) requireActivity()).getFerrisfyer().getMode() == Ferrisfyer.Mode.LOCAL) {
            this.deviceControl = ((MainActivity) requireActivity()).service;
        }
    }

    private void onSliderChange() {
        // TODO: modificar dataset

        binding.chart.notifyDataSetChanged();
        binding.chart.invalidate();
    }
}
