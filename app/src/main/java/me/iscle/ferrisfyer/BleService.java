package me.iscle.ferrisfyer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.iscle.ferrisfyer.activity.MainActivity;
import me.iscle.ferrisfyer.model.Device;

import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_BATTERY;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_MAC;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_SN;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_START_DUAL_MOTOR;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_START_SINGLE_MOTOR;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_STOP_MOTOR;
import static me.iscle.ferrisfyer.Constants.SERVICE_BATTERY;
import static me.iscle.ferrisfyer.Constants.SERVICE_INFO;
import static me.iscle.ferrisfyer.Constants.SERVICE_MOTOR;

public class BleService extends Service implements IDeviceControl {
    private static final String TAG = "BleService";
    private static final int SERVICE_NOTIFICATION_ID = 1;

    private final BLEBinder binder = new BLEBinder();

    private BluetoothGatt gatt;
    private State state;
    private Timer timer;
    private Device device;
    private IDeviceCallback deviceCallback;

    private BluetoothGattCharacteristic batteryCharacteristic;
    private BluetoothGattCharacteristic stopMotorCharacteristic;
    private BluetoothGattCharacteristic startSingleMotorCharacteristic;
    private BluetoothGattCharacteristic startDualMotorCharacteristic;

    private GattManager gattManager;

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            // Unused
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothGatt.STATE_DISCONNECTED:
                    state = State.DISCONNECTED;
                    //timer.cancel();
                    break;
                case BluetoothGatt.STATE_CONNECTING:
                    state = State.CONNECTING;
                    break;
                case BluetoothGatt.STATE_CONNECTED:
                    state = State.CONNECTED;
                    timer.schedule(getRssiTimerTask(), 0, 2500);
                    gatt.discoverServices();
                    break;
                case BluetoothGatt.STATE_DISCONNECTING:
                    state = State.DISCONNECTING;
                    break;
            }

            if (deviceCallback != null) deviceCallback.onConnectionStateUpdated(state);

            updateNotification();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> gattServices = gatt.getServices();
                if (gattServices != null && !gattServices.isEmpty()) {
                    handleServicesDiscovered(gattServices);
                    return;
                }
            }

            disconnect();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) gattManager.onCharacteristicWrite(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleDataReceived(characteristic);
            gattManager.onCharacteristicChanged(characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                device.setRssi(rssi);
                if (deviceCallback != null) deviceCallback.onRssiUpdated(device);
            }
        }
    };

    private TimerTask getBatteryTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (state == State.CONNECTED) read(batteryCharacteristic);
            }
        };
    }

    private TimerTask getRssiTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                if (state == State.CONNECTED) gatt.readRemoteRssi();
            }
        };
    }

    @Override
    public void onCreate() {
        super.onCreate();

        state = State.DISCONNECTED;
        timer = new Timer();

        startForeground(SERVICE_NOTIFICATION_ID, getNotification());
    }

    public void disconnect() {
        gatt.disconnect();
        gatt.close();
    }

    private void updateNotification() {
        NotificationManager notificationManager
                = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(SERVICE_NOTIFICATION_ID, getNotification());
    }

    public void connectDevice(String address) {
        BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        device = new Device(bluetoothDevice);
        gattManager = new GattManager();
        gatt = bluetoothDevice.connectGatt(this, true, callback);
        gattManager.setGattAndStart(gatt);
    }

    public void write(@NotNull BluetoothGattCharacteristic characteristic, byte... data) {
        gattManager.write(characteristic, data);
    }

    public void read(@NotNull BluetoothGattCharacteristic characteristic) {
        gattManager.read(characteristic);
    }

    public void notify(@NotNull BluetoothGattCharacteristic characteristic, boolean enable) {
        gattManager.notify(characteristic, enable);
    }

    private void handleDataReceived(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        String characteristicUuid = characteristic.getUuid().toString();
        String serviceUuid = characteristic.getService().getUuid().toString();

        switch (serviceUuid) {
            case SERVICE_BATTERY:
                if (CHARACTERISTIC_BATTERY.equals(characteristicUuid)) {
                    if (data != null && data.length > 0) {
                        byte battery = data[0];
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_BATTERY = " + battery);
                        device.setBattery(battery);
                        if (deviceCallback != null) deviceCallback.onBatteryUpdated(device);
                    }
                }
                break;
            case SERVICE_INFO:
                if (CHARACTERISTIC_MAC.equals(characteristicUuid)) {
                    String mac = Utils.bytesToHexString(data);
                    Log.d(TAG, "handleDataReceived: CHARACTERISTIC_MAC = " + mac);
                    device.setMac(mac);
                    if (deviceCallback != null) deviceCallback.onMacUpdated(device);
                } else if (CHARACTERISTIC_SN.equals(characteristicUuid)) {
                    String sn = new String(data).trim();
                    Log.d(TAG, "handleDataReceived: CHARACTERISTIC_SN = " + sn);
                    device.setSn(sn);
                    if (deviceCallback != null) deviceCallback.onSnUpdated(device);
                }
                break;
        }
        // TODO: Call device info callback
    }

    // TODO: Wait until (single motor || dual motor) && stop motor characteristics have been found
    private void handleServicesDiscovered(List<BluetoothGattService> services) {
        for (BluetoothGattService service : services) {
            switch (service.getUuid().toString()) {
                case SERVICE_MOTOR:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        switch (characteristic.getUuid().toString()) {
                            case CHARACTERISTIC_START_SINGLE_MOTOR:
                                characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
                                startSingleMotorCharacteristic = characteristic;
                                break;
                            case CHARACTERISTIC_STOP_MOTOR:
                                characteristic.setWriteType(WRITE_TYPE_DEFAULT);
                                stopMotorCharacteristic = characteristic;
                                break;
                            case CHARACTERISTIC_START_DUAL_MOTOR:
                                characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
                                startDualMotorCharacteristic = characteristic;
                                device.setDualMotor(true);
                                break;
                        }
                    }
                    break;
                case SERVICE_BATTERY:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().toString().equals(CHARACTERISTIC_BATTERY)) {
                            batteryCharacteristic = characteristic;
                            read(characteristic);
                            timer.schedule(getBatteryTimerTask(), 0, 2500);
                            notify(characteristic, true);
                        }
                    }
                    break;
                case SERVICE_INFO:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        switch (characteristic.getUuid().toString()) {
                            case CHARACTERISTIC_MAC:
                            case CHARACTERISTIC_SN:
                                read(characteristic);
                                break;
                        }
                    }
                    break;
            }
        }
    }

    public void setDeviceCallback(IDeviceCallback callback) {
        this.deviceCallback = callback;
    }

    public void removeDeviceCallback(IDeviceCallback callback) {
        if (this.deviceCallback == callback) this.deviceCallback = null;
    }

    public void startMotor(byte percent) {
        startMotor(percent, percent);
    }

    public void startMotor(byte percent1, byte percent2) {
        if (device == null) return;
        if (percent1 < 0) {
            percent1 = 0;
        } else if (percent1 > 100) {
            percent1 = 100;
        }

        if (device.isDualMotor()) {
            if (percent2 < 0) {
                percent2 = 0;
            } else if (percent2 > 100) {
                percent2 = 100;
            }
            
            write(startDualMotorCharacteristic, percent1, percent2);
        } else {
            write(startSingleMotorCharacteristic, percent1);
        }
    }

    public void stopMotor() {
        if (device == null) return;
        if (device.isDualMotor()) {
            write(stopMotorCharacteristic, (byte) 2);
        } else {
            write(stopMotorCharacteristic, (byte) 0);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        String title;
        String text;
        switch (state) {
            case DISCONNECTED:
                title = "Disconnected";
                text = "Waiting for device";
                break;
            case SEARCHING:
                title = "Searching";
                text = "Please wait...";
                break;
            case CONNECTING:
                title = "Connecting";
                text = "Device: " + gatt.getDevice().getName();
                break;
            case CONNECTED:
                title = "Connected";
                text = "Device: " + gatt.getDevice().getName();
                break;
            default:
                throw new RuntimeException();
        }


        return new NotificationCompat.Builder(this, Ferrisfyer.SERVICE_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();
    }

    public enum State {
        DISCONNECTED,
        SEARCHING,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    public class BLEBinder extends Binder {
        public BleService getService() {
            return BleService.this;
        }
    }
}
