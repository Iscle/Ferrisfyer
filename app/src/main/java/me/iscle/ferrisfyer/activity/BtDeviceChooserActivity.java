package me.iscle.ferrisfyer.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import me.iscle.ferrisfyer.R;
import me.iscle.ferrisfyer.adapter.BluetoothDeviceAdapter;

public class BtDeviceChooserActivity extends AppCompatActivity {
    private static final String TAG = "BtDeviceChooserActivity";

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private final static long SCAN_PERIOD = 10000;

    private RecyclerView recyclerView;
    private Button scanButton;

    private BluetoothDeviceAdapter adapter;

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler;
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            adapter.addBluetoothDevice(device);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bt_device_chooser);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        setResult(RESULT_CANCELED);

        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        onPermissionGranted();
    }

    @Override
    protected void onResume() {
        super.onResume();

        setTitle("Choose a device... - " + getString(R.string.app_name));
    }

    private void onPermissionGranted() {
        bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanning = false;
        handler = new Handler();

        recyclerView = findViewById(R.id.recycler_view);
        scanButton = findViewById(R.id.scan_button);

        configureRecyclerView();
        scanButton.setOnClickListener(v -> scanButtonClick());
    }

    private void configureRecyclerView() {
        adapter = new BluetoothDeviceAdapter(BtDeviceChooserActivity.this);
        adapter.setOnItemClickListener(this::deviceClick);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    private void scanButtonClick() {
        if (scanning) {
            stopScan();
        } else {
            startScan();
        }
    }

    private synchronized void startScan() {
        if (scanning) return;
        scanButton.setText("Stop scan");
        scanning = true;
        bluetoothLeScanner.startScan(scanCallback);

        handler.postDelayed(this::stopScan, SCAN_PERIOD);
        Log.d(TAG, "Bluetooth LE scan started!");
    }

    private synchronized void stopScan() {
        if (!scanning) return;
        scanButton.setText("Start scan");
        scanning = false;
        bluetoothLeScanner.stopScan(scanCallback);

        Log.d(TAG, "Bluetooth LE scan stopped!");
    }

    private void deviceClick(BluetoothDevice device) {
        stopScan();
        Intent data = new Intent();
        data.putExtra("device_address", device.getAddress());
        setResult(RESULT_OK, data);
        finish();
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permissions are needed to get Bluetooth devices!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            onPermissionGranted();
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
