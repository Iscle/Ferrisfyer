package me.iscle.ferrisfyer.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.iscle.ferrisfyer.BLEService;
import me.iscle.ferrisfyer.R;

public class LocalControlActivity extends BaseAppCompatActivity implements View.OnClickListener {
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_CHOOSE_BT_DEVICE = 2;
    private static final String TAG = "LocalControlActivity";
    private BLEService bleService;
    private boolean bound = false;

    private SeekBar seekBar;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleService = ((BLEService.BLEBinder) service).getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_control);

        findViewById(R.id.button).setOnClickListener(this);
        findViewById(R.id.button2).setOnClickListener(this);
        findViewById(R.id.button3).setOnClickListener(this);
        findViewById(R.id.button4).setOnClickListener(this);
        seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //bleService.startMotor((byte) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startActivity();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_device:
                Intent i = new Intent(this, BtDeviceChooserActivity.class);
                startActivityForResult(i, REQUEST_CHOOSE_BT_DEVICE);
                return true;
        }

        return false;
    }

    public void startActivity() {
        if (BluetoothAdapter.getDefaultAdapter() == null || !isBluetoothLeSupported()) {
            Toast.makeText(this, "Bluetooth Low Energy is not supported in your device!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            requestEnableBluetooth();
            return;
        }

        Intent intent = new Intent(this, BLEService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        intent = new Intent(this, BLEService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }

    public void connectToDevice(String address) {
        Log.d(TAG, "connectToDevice called: " + address);

        bleService.connectDevice(address);
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
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, "Bluetooth is required for this app to work!", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            startActivity();
            return;
        }

        if (requestCode == REQUEST_CHOOSE_BT_DEVICE) {
            if (resultCode == RESULT_OK) {
                connectToDevice(data.getStringExtra("device_address"));
            }
            return;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                // start motor
                bleService.startMotor((byte) seekBar.getProgress());
                break;
            case R.id.button2:
                // stop motor
                bleService.stopMotor();
                break;
            case R.id.button3:
                //bleService.onLight((byte) seekBar.getProgress());
                bleService.startRemoteControl();
                break;
            case R.id.button4:
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
                break;
        }
    }
}