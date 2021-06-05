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
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.slider.Slider;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.iscle.ferrisfyer.BleService;
import me.iscle.ferrisfyer.IDeviceCallback;
import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.activity.BtDeviceChooserActivity;
import me.iscle.ferrisfyer.databinding.FragmentDeviceControlBinding;
import me.iscle.ferrisfyer.model.Device;
import me.iscle.ferrisfyer.model.VibrationMode;

import static android.content.Context.BIND_AUTO_CREATE;

public class DeviceControlFragment extends BaseFragment {
    private static final String TAG = "DeviceControlFragment";

    private static final int SPEED = 100;
    private static final int CHART_VALUES = 120;

    private FragmentDeviceControlBinding binding;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CHOOSE_BT_DEVICE = 2;

    private BleService service;
    private Mode mode;

    private Thread modeThread;
    private Thread updateDataSetThread;

    private boolean isServiceConnected;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        initChart();

        binding.motorSlider.addOnChangeListener(changeListener);

        VibrationMode[] vibrationModes = defineVibrationModes();
        ArrayAdapter<VibrationMode> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, vibrationModes);

        binding.vibrationModesSpn.setAdapter(spinnerAdapter);
        binding.changeModeBtn.setOnClickListener((v) -> setMode(vibrationModes[binding.vibrationModesSpn.getSelectedItemPosition()]));

        launchUpdateDataSetThread();

        if (mode == Mode.LOCAL) {
            if (BluetoothAdapter.getDefaultAdapter() == null || !isBluetoothLeSupported()) {
                Toast.makeText(requireContext(), R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
                requireActivity().finishAffinity();
                return;
            }

            if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) requestEnableBluetooth();

            Intent serviceIntent = new Intent(getContext(), BleService.class);
            requireActivity().bindService(serviceIntent, bleServiceConnection, BIND_AUTO_CREATE);
            isServiceConnected = true;

            Intent i = new Intent(requireActivity(), BtDeviceChooserActivity.class);
            startActivityForResult(i, REQUEST_CHOOSE_BT_DEVICE);
        } else if (mode == Mode.REMOTE) {
            requireActivity().setTitle(R.string.controlling_remote_device);
        } else {
            throw new RuntimeException("Invalid mode!");
        }
    }

    private void initChart() {
        // Hide description
        binding.chart.getDescription().setEnabled(false);

        // Hide axis labels
        binding.chart.getAxisLeft().setDrawLabels(false);
        binding.chart.getAxisRight().setDrawLabels(false);
        binding.chart.getXAxis().setDrawLabels(false);

        // Hide grid lines
        binding.chart.getAxisLeft().setDrawGridLines(false);
        binding.chart.getAxisRight().setDrawGridLines(false);
        binding.chart.getXAxis().setDrawGridLines(false);

        // Hide border and axis
        binding.chart.setDrawBorders(false);
        binding.chart.getAxisLeft().setDrawAxisLine(false);
        binding.chart.getAxisRight().setDrawAxisLine(false);
        binding.chart.getXAxis().setDrawAxisLine(false);

        // Hide legend
        binding.chart.getLegend().setEnabled(false);

        // Disable touch
        binding.chart.setTouchEnabled(false);

        // Set maximum and minimum values
        binding.chart.getAxisLeft().setAxisMaximum(100);
        binding.chart.getAxisLeft().setAxisMinimum(0);
        binding.chart.getAxisLeft().setSpaceTop(0);
        binding.chart.getAxisLeft().setSpaceBottom(0);

        // Create the data set
        initDataSet();
    }

    private void initDataSet() {
        LineDataSet set;

        LinkedList<Entry> values = new LinkedList<>();
        for (int i = 0; i < CHART_VALUES; i++) {
            values.add(new Entry(i, 0));
        }

        int colorCode = getResources().getColor(R.color.colorPrimary);
        set = new LineDataSet(values, "");
        set.setDrawIcons(false);
        set.setDrawValues(false);
        set.setDrawCircles(false);

        set.setColor(colorCode);
        set.setCircleColor(colorCode);

        set.setDrawFilled(true);
        set.setFillFormatter((dataSet, dataProvider) -> binding.chart.getAxisLeft().getAxisMinimum());
        set.setFillColor(colorCode);

        ArrayList<ILineDataSet> datasets = new ArrayList<>();
        datasets.add(set);

        LineData data = new LineData(datasets);
        binding.chart.setData(data);
    }

    private void updateDataSetInfo(float value) {
        LineDataSet set = (LineDataSet) binding.chart.getData().getDataSetByIndex(0);
        List<Entry> values = set.getValues();

        List<Entry> newValues = new LinkedList<>();
        for (int i = 0; i < CHART_VALUES - 1; i++) {
            newValues.add(new Entry(i, values.get(i + 1).getY()));
        }
        newValues.add(new Entry(CHART_VALUES - 1, value));

        set.setValues(newValues);
        set.notifyDataSetChanged();
        binding.chart.getData().notifyDataChanged();
        binding.chart.notifyDataSetChanged();
        binding.chart.invalidate();
    }

    private void launchUpdateDataSetThread() {
        updateDataSetThread = new Thread(() -> {
            try {
                while (true) {
                    if (binding.motorSlider.isEnabled()) {
                        updateDataSetInfo(binding.motorSlider.getValue());
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        updateDataSetThread.start();

    }

    private String loadJSONFromAsset() {
        String json;
        try {
            InputStream is = requireActivity().getAssets().open("vibrationModes.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    private VibrationMode[] defineVibrationModes() {
        return new Gson().fromJson(loadJSONFromAsset(), VibrationMode[].class);
    }

    private final Slider.OnChangeListener changeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (service == null) return;

            if (value == 0f) {
                service.stopMotor();
            } else {
                service.startMotor((byte) value);
            }
        }
    };

    private void setMode(VibrationMode mode) {
        if (service == null) return;

        if (modeThread == null) {
            binding.motorSlider.setEnabled(false);

            modeThread = new Thread(() -> {
                try {
                    while (true) {
                        for (byte vibrationValue : mode.getPattern()) {
                            if (vibrationValue == 0) {
                                service.stopMotor();
                            } else {
                                service.startMotor(vibrationValue);
                            }

                            Thread.sleep(SPEED);
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            modeThread.start();
        } else {
            modeThread.interrupt();
            modeThread = null;
            service.stopMotor();
            binding.motorSlider.setEnabled(true);
        }
    }

    private final IDeviceCallback deviceCallback = new IDeviceCallback() {
        @Override
        public void onConnectionStateUpdated(BleService.State state) {
            requireActivity().runOnUiThread(() -> {
                binding.progressContainer.setVisibility(View.GONE);
                binding.container.setVisibility(View.VISIBLE);
            });

            if (state == BleService.State.CONNECTED) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.bluetooth_connected, Toast.LENGTH_LONG).show());
            }
            if (state == BleService.State.DISCONNECTED) {
                requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), R.string.bluetooth_disconnected, Toast.LENGTH_LONG).show());
            }
        }

        @Override
        public void onRssiUpdated(Device device) {
            int bars = WifiManager.calculateSignalLevel(device.getRssi(), 5);
            requireActivity().runOnUiThread(() -> binding.rssiLevel.setText(String.valueOf(bars)));
            Log.d(TAG, "onRssiUpdated: " + bars);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBatteryUpdated(Device device) {
            requireActivity().runOnUiThread(() -> binding.batteryLevel.setText(Byte.toString(device.getBattery())));
            Log.d(TAG, "onBatteryUpdated: " + device.getBattery());
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
        if (isServiceConnected) requireActivity().unbindService(bleServiceConnection);

        super.onDestroy();
    }

    private void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    private boolean isBluetoothLeSupported() {
        return requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
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
            if (resultCode == Activity.RESULT_OK && data != null) {
                requireActivity().runOnUiThread(() -> {
                    binding.progressContainer.setVisibility(View.VISIBLE);
                    binding.container.setVisibility(View.GONE);
                });

                service.connectDevice(data.getStringExtra("device_address"));
            } else {
                Toast.makeText(requireContext(), R.string.bluetooth_error, Toast.LENGTH_LONG).show();
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
