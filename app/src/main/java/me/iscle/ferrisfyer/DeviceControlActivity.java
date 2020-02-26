package me.iscle.ferrisfyer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.ramotion.fluidslider.FluidSlider;

import kotlin.Unit;
import me.iscle.ferrisfyer.activity.BaseAppCompatActivity;
import me.iscle.ferrisfyer.activity.LoginActivity;
import me.iscle.ferrisfyer.model.Device;

public class DeviceControlActivity extends BaseAppCompatActivity implements ServiceConnection, View.OnClickListener {
    private static final String TAG = "DeviceControlActivity";

    private final int REQUEST_LOGIN = 1;

    private TextView serverConnectionStatus;
    private TextView connectionStatus;
    private TextView motorStatus;
    private TextView batteryLevel;

    private Button remoteControl;
    private Button startMotor;
    private Button stopMotor;

    private TextView mac;
    private TextView sv;
    private TextView hv;
    private TextView sn;
    private TextView pid;
    private TextView offlineCount;
    private TextView powerCount;

    private FluidSlider slider;

    private IDeviceControl deviceControl;

    private BLEService service = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device_control_layout);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        serverConnectionStatus = findViewById(R.id.server_connection_status);
        connectionStatus = findViewById(R.id.connection_status);
        motorStatus = findViewById(R.id.motor_status);
        batteryLevel = findViewById(R.id.battery_level);

        remoteControl = findViewById(R.id.remote_control);
        startMotor = findViewById(R.id.start_motor);
        stopMotor = findViewById(R.id.stop_motor);
        slider = findViewById(R.id.motor_slider);

        mac = findViewById(R.id.mac);
        sv = findViewById(R.id.sv);
        hv = findViewById(R.id.hv);
        sn = findViewById(R.id.sn);
        pid = findViewById(R.id.pid);
        offlineCount = findViewById(R.id.offline_count);
        powerCount = findViewById(R.id.power_count);

        remoteControl.setOnClickListener(this);
        startMotor.setOnClickListener(this);
        stopMotor.setOnClickListener(this);
        slider.setPositionListener(aFloat -> {
            deviceControl.startMotor((byte) (aFloat * 100));
            return Unit.INSTANCE;
        });

        App.Mode mode = getApp().getMode();
        if (mode == App.Mode.LOCAL) {
            setTitle("Local mode - Ferrisfyer");
            remoteControl.setVisibility(View.VISIBLE);
            Intent serviceIntent = new Intent(this, BLEService.class);
            bindService(serviceIntent, this, BIND_AUTO_CREATE);
        } else if (mode == App.Mode.REMOTE) {
            setTitle("Remote mode - Ferrisfyer");
            remoteControl.setVisibility(View.GONE);
            showSelectUserDialog();
            //deviceControl = new WebSocketManager(this, getIntent().getStringExtra("name"));
            //((WebSocketManager) deviceControl).startRemoteControl();
            //((WebSocketManager) deviceControl).setCallback(webSocketCallback);
        } else {
            throw new RuntimeException("Invalid mode!");
        }
    }

    private void showSelectUserDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.user_dialog, null);
        EditText username = view.findViewById(R.id.username);

        new AlertDialog.Builder(this)
                .setTitle("Type in the name of the user")
                .setView(view)
                .setPositiveButton(android.R.string.ok,
                        (dialog, whichButton) -> {
                            if (username.getText().toString().isEmpty()) {
                                Toast.makeText(DeviceControlActivity.this, "Username can't be empty!", Toast.LENGTH_LONG).show();
                                finishAffinity();
                                return;
                            }

                            deviceControl = new WebSocketManager(this, username.getText().toString());
                            ((WebSocketManager) deviceControl).startRemoteControl();
                            ((WebSocketManager) deviceControl).setCallback(webSocketCallback);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    Toast.makeText(DeviceControlActivity.this, "Bye!", Toast.LENGTH_SHORT).show();
                    finishAffinity();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    protected void onDestroy() {
        if (getApp().getMode() == App.Mode.LOCAL) {
            unbindService(this);
        } else if (getApp().getMode() == App.Mode.REMOTE) {
            ((WebSocketManager) deviceControl).stopRemoteControl();
        }
        super.onDestroy();
    }

    private void setDeviceInfo(Device device) {
        mac.setText("MAC: " + device.getMac());
        sv.setText("Software version: " + device.getSv());
        hv.setText("Hardware version: " + device.getHv());
        sn.setText("Serial number: " + device.getSn());
        pid.setText("Product ID: " + device.getPid());
        offlineCount.setText("Offline count: " + device.getOfflineCount());
        powerCount.setText("Power count: " + device.getPowerCount());
    }

    private void setServerConnectionStatus(int status) {
        if (status == 0) {
            serverConnectionStatus.setText("Server connection status: disconnected");
        } else if (status == 1) {
            serverConnectionStatus.setText("Server connection status: connected");
        } else if (status == 2) {
            serverConnectionStatus.setText("Server connection status: reconnecting");
        }
    }

    private void setConnectionStatus(int status) {
        if (status == 0) {
            connectionStatus.setText("Connection status: disconnected");
        } else if (status == 1) {
            connectionStatus.setText("Connection status: connected");
        } else if (status == 2) {
            connectionStatus.setText("Connection status: reconnecting");
        }
    }

    private void setMotorStatus(boolean running) {
        if (running) {
            motorStatus.setText("Motor status: running");
        } else {
            motorStatus.setText("Motor status: stopped");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        this.service = ((BLEService.BLEBinder) binder).getService();
        if (getApp().getMode() == App.Mode.LOCAL) {
            this.deviceControl = this.service;
            if (this.service.isConnected()) {
                setServerConnectionStatus(1);
                setConnectionStatus(this.service.getDeviceStatus());
                setMotorStatus(this.service.getDeviceMotorStatus());
                setDeviceInfo(this.service.getDevice());
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        this.service = null;
        if (getApp().getMode() == App.Mode.LOCAL) {
            this.deviceControl = this.service;
        }
    }

    private WebSocketManager.WebSocketCallback webSocketCallback = new WebSocketManager.WebSocketCallback() {
        @Override
        public void onConnect() {
            runOnUiThread(() -> setServerConnectionStatus(1));
        }

        @Override
        public void onDisconnect() {
            runOnUiThread(() -> setServerConnectionStatus(0));
        }

        @Override
        public void onError(String error) {
            runOnUiThread(() ->
                    Toast.makeText(DeviceControlActivity.this,
                            "WebSocket error: " + error, Toast.LENGTH_LONG).show());
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_motor:

                break;
            case R.id.stop_motor:

                break;
            case R.id.remote_control:
                if (service.getWebSocket() != null) {
                    service.stopRemoteControl();
                    remoteControl.setText("Enable remote control");
                } else {
                    Intent i = new Intent(this, LoginActivity.class);
                    startActivityForResult(i, REQUEST_LOGIN);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                remoteControl.setText("Disable remote control");
                if (service != null) {
                    service.startRemoteControl();
                }
            }
        }
    }
}
