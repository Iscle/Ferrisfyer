package me.iscle.ferrisfyer.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import me.iscle.ferrisfyer.R;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_CHOOSE_BT_DEVICE = 2;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !isBluetoothLeSupported()) {
            Toast.makeText(this, "Bluetooth Low Energy is not supported in your device!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            requestEnableBluetooth();
            return;
        }

        onBluetoothEnabled();
    }

    public void onBluetoothEnabled() {
        Intent i = new Intent(this, BtDeviceChooserActivity.class);
        startActivityForResult(i, REQUEST_CHOOSE_BT_DEVICE);
    }

    public void connectToDevice(BluetoothDevice device) {

    }

    public boolean isBluetoothLeSupported() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void requestEnableBluetooth() {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Bluetooth is required for this app to work!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            onBluetoothEnabled();
            return;
        }

        if (requestCode == REQUEST_CHOOSE_BT_DEVICE) {
            if (resultCode == RESULT_OK) {
                connectToDevice(bluetoothAdapter.getRemoteDevice(data.getStringExtra("device_address")));
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
