package me.iscle.ferrisfyer;

import android.app.Notification;
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
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.iscle.ferrisfyer.activity.LocalControlActivity;
import me.iscle.ferrisfyer.model.Device;
import me.iscle.ferrisfyer.model.WebSocketCapsule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT32;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
import static me.iscle.ferrisfyer.Constants.ACTION_GATT_CHARACTERISTIC_CHANGED;
import static me.iscle.ferrisfyer.Constants.ACTION_GATT_DEVICE_CONNECTED;
import static me.iscle.ferrisfyer.Constants.ACTION_GATT_DEVICE_DISCONNECTED;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_ACCELERATION;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_BATTERY;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_INFO;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_PRESSURE;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_RSSI;
import static me.iscle.ferrisfyer.Constants.ACTION_READ_REMOTE_TEMPERATURE;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_ACCELERATION;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_BATTERY;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_DECRYPT;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_HV;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_LIGHT;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_LIGHT_END;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_MAC;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_OFFLINECOUNT;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_OUT_STREET;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_PID;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_POWERCOUNT;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_PRESSURE;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_SN;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_START_DUAL_MOTOR;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_START_SINGLE_MOTOR;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_STOP_MOTOR;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_SV;
import static me.iscle.ferrisfyer.Constants.CHARACTERISTIC_TEMPERATURE;
import static me.iscle.ferrisfyer.Constants.SERVICE_ACCELERATION;
import static me.iscle.ferrisfyer.Constants.SERVICE_BATTERY;
import static me.iscle.ferrisfyer.Constants.SERVICE_DECRYPT;
import static me.iscle.ferrisfyer.Constants.SERVICE_INFO;
import static me.iscle.ferrisfyer.Constants.SERVICE_MOTOR;
import static me.iscle.ferrisfyer.Constants.SERVICE_OUT_STREET;
import static me.iscle.ferrisfyer.Constants.SERVICE_PRESSURE;
import static me.iscle.ferrisfyer.Constants.SERVICE_TEMPERATURE;
import static me.iscle.ferrisfyer.CustomUtils.bytesToHexString;
import static me.iscle.ferrisfyer.CustomUtils.genBleData;
import static me.iscle.ferrisfyer.CustomUtils.validateBleData;

public class BLEService extends Service {
    private static final String TAG = "BLEService";

    public static final int SERVICE_NOTIFICATION_ID = 1;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED = 1;

    private final IBinder binder = new BLEBinder();

    private LocalBroadcastManager localBroadcastManager;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private int state;
    private Timer rssiTimer;
    private Device deviceInfo;
    private int deviceMode;
    private WebSocket webSocket;
    private BluetoothGattCharacteristic decryptCharacteristic;
    private BluetoothGattCharacteristic accelerationCharacteristic;
    private BluetoothGattCharacteristic temperatureCharacteristic;
    private BluetoothGattCharacteristic pressureCharacteristic;
    private BluetoothGattCharacteristic batteryCharacteristic;
    private BluetoothGattCharacteristic stopMotorCharacteristic;
    private BluetoothGattCharacteristic startSingleMotorCharacteristic;
    private BluetoothGattCharacteristic startDualMotorCharacteristic;
    private BluetoothGattCharacteristic lightCharacteristic;
    private BluetoothGattCharacteristic lightEndCharacteristic;
    private BluetoothGattCharacteristic outStreetCharacteristic;
    private boolean dualMotor = false;

    private GattManager gattManager;

    private final BluetoothGattCallback callback = new BluetoothGattCallback() {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite: status = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.getCharacteristic().getUuid().toString().equals(CHARACTERISTIC_OUT_STREET)) {
                    readDeviceMode();
                }
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: status = " + status + ", newState = " + newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                if (rssiTimer != null) rssiTimer.cancel();
                gatt.discoverServices();
                state = STATE_CONNECTED;
                rssiTimer = new Timer();
                rssiTimer.schedule(rssiTimerTask, 0, 2500);
                sendLocalBroadcast(ACTION_GATT_DEVICE_CONNECTED);
                updateNotification(createNotification("Connected to " + gatt.getDevice().getAddress(), "Tap to open the app"));
            } else {
                if (rssiTimer != null) rssiTimer.cancel();
                state = STATE_DISCONNECTED;
                sendLocalBroadcast(ACTION_GATT_DEVICE_DISCONNECTED);
                updateNotification(createNotification("Waiting for connection...", "Tap to open the app"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered: status = " + status);
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
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattManager.onCharacteristicRead(characteristic);
                onCharacteristicChanged(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gattManager.onCharacteristicWrite(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            handleDataReceived(characteristic);
            sendLocalBroadcast(ACTION_GATT_CHARACTERISTIC_CHANGED);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Intent i = new Intent(ACTION_READ_REMOTE_RSSI);
                i.putExtra("rssi", rssi);
                localBroadcastManager.sendBroadcast(i);
            }
        }
    };

    private final TimerTask rssiTimerTask = new TimerTask() {
        @Override
        public void run() {
            if (state == STATE_CONNECTED) {
                gatt.readRemoteRssi();
            }
        }
    };

    private final WebSocketListener webSocketListener = new WebSocketListener() {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "onOpen");
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            Log.d(TAG, "onMessage: text = " + text);
            WebSocketCapsule capsule = WebSocketCapsule.fromJson(text);
            switch (capsule.getCommand()) {
                case "SET_MOTOR_SPEED":

                    break;
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(TAG, "onClosing: code = " + code + ", reason = " + reason);
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            Log.d(TAG, "onFailure: " + t.getMessage());
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(SERVICE_NOTIFICATION_ID, createNotification("Waiting for connection...", "Tap to open the app"));

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(App.BROADCAST_SEND_COMMAND);
    }

    public void disconnect() {
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
            gatt = null;
        }
    }

    private void updateNotification(Notification notification) {
        startForeground(SERVICE_NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String title, String text) {
        Intent notificationIntent = new Intent(this, LocalControlActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, App.SERVICE_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .build();

        return notification;
    }

    public void connectDevice(String address) {
        device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        deviceInfo = new Device("Ferrisfyer");
        gatt = device.connectGatt(this, true, callback);
        gattManager = new GattManager(gatt);
    }

    private void sendLocalBroadcast(String action) {
        Intent i = new Intent(action);
        localBroadcastManager.sendBroadcast(i);
    }

    public void write(BluetoothGattCharacteristic characteristic, byte... data) {
        if (characteristic == null) {
            Log.d(TAG, "write: characteristic = null!");
            return;
        }

        gattManager.write(characteristic, data);
    }

    public void read(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.d(TAG, "read: characteristic = null!");
            return;
        }

        gattManager.read(characteristic);
    }

    public void notify(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (characteristic == null) {
            Log.d(TAG, "notify: characteristic = null!");
            return;
        }

        gattManager.notify(characteristic, enable);
    }

    private void handleDataReceived(BluetoothGattCharacteristic characteristic) {
        byte[] data = characteristic.getValue();
        Integer intData = characteristic.getIntValue(FORMAT_SINT32, 0);
        String characteristicUuid = characteristic.getUuid().toString();
        String serviceUuid = characteristic.getService().getUuid().toString();

        handleDataReceived(data, intData != null ? intData : 0, characteristicUuid, serviceUuid);
    }

    private void handleServicesDiscovered(List<BluetoothGattService> services) {
        for (BluetoothGattService service : services) {
            String serviceUuid = service.getUuid().toString();
            switch (serviceUuid) {
                case SERVICE_MOTOR:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        switch (characteristicUuid) {
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
                                dualMotor = true;
                                break;
                            case CHARACTERISTIC_LIGHT:
                                characteristic.setWriteType(WRITE_TYPE_NO_RESPONSE);
                                lightCharacteristic = characteristic;
                                break;
                            case CHARACTERISTIC_LIGHT_END:
                                characteristic.setWriteType(WRITE_TYPE_DEFAULT);
                                lightEndCharacteristic = characteristic;
                                break;
                        }
                    }
                    break;
                case SERVICE_OUT_STREET:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_OUT_STREET)) {
                            characteristic.setWriteType(WRITE_TYPE_DEFAULT);
                            outStreetCharacteristic = characteristic;
                        }
                    }
                    break;
                case SERVICE_BATTERY:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_BATTERY)) {
                            batteryCharacteristic = characteristic;
                            read(characteristic);
                            notify(characteristic, true);
                        }
                    }
                    break;
                case SERVICE_PRESSURE:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_PRESSURE)) {
                            pressureCharacteristic = characteristic;
                        }
                    }
                    break;
                case SERVICE_TEMPERATURE:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_TEMPERATURE)) {
                            temperatureCharacteristic = characteristic;
                        }
                    }
                    break;
                case SERVICE_ACCELERATION:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_ACCELERATION)) {
                            accelerationCharacteristic = characteristic;
                        }
                    }
                    break;
                case SERVICE_INFO:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        switch (characteristicUuid) {
                            case CHARACTERISTIC_HV:
                            case CHARACTERISTIC_SV:
                            case CHARACTERISTIC_SN:
                            case CHARACTERISTIC_MAC:
                            case CHARACTERISTIC_PID:
                            case CHARACTERISTIC_OFFLINECOUNT:
                            case CHARACTERISTIC_POWERCOUNT:
                                read(characteristic);
                                break;
                        }
                    }
                    break;
                case SERVICE_DECRYPT:
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String characteristicUuid = characteristic.getUuid().toString();
                        if (characteristicUuid.equals(CHARACTERISTIC_DECRYPT)) {
                            decryptCharacteristic = characteristic;
                            read(characteristic);
                            notify(characteristic, true);
                        }
                    }
                    break;
            }
        }
    }

    private void handleDataReceived(byte[] data, int intData, String characteristicUuid, String serviceUuid) {
        switch (serviceUuid) {
            case SERVICE_BATTERY:
                if (characteristicUuid.equals(CHARACTERISTIC_BATTERY)) {
                    if (data != null && data.length > 0) {
                        byte battery = data[0];
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_BATTERY = " + battery);
                        Intent i = new Intent(ACTION_READ_REMOTE_BATTERY);
                        i.putExtra("battery", battery);
                        localBroadcastManager.sendBroadcast(i);
                    }
                }
                break;
            case SERVICE_PRESSURE:
                if (characteristicUuid.equals(CHARACTERISTIC_PRESSURE)) {
                    int pressure = intData;
                    Log.d(TAG, "handleDataReceived: CHARACTERISTIC_PRESSURE = " + pressure);
                    Intent i = new Intent(ACTION_READ_REMOTE_PRESSURE);
                    i.putExtra("pressure", pressure);
                    localBroadcastManager.sendBroadcast(i);
                }
                break;
            case SERVICE_TEMPERATURE:
                if (characteristicUuid.equals(CHARACTERISTIC_TEMPERATURE)) {
                    if (data != null && data.length > 1) {
                        float a = data[0];
                        float b = data[1];
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_TEMPERATURE: a = " + a + ", b = " + b);
                        float temperature = a + (b / 100.0f);
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_TEMPERATURE = " + temperature);
                        Intent i = new Intent(ACTION_READ_REMOTE_TEMPERATURE);
                        i.putExtra("temperature", temperature);
                        localBroadcastManager.sendBroadcast(i);
                    }
                }
                break;
            case SERVICE_ACCELERATION:
                if (characteristicUuid.equals(CHARACTERISTIC_ACCELERATION)) {
                    String onResultSix = bytesToHexString(data);
                    Log.d(TAG, "handleDataReceived: CHARACTERISTIC_ACCELERATION = " + onResultSix);
                    Intent i = new Intent(ACTION_READ_REMOTE_ACCELERATION);
                    i.putExtra("acceleration", onResultSix);
                    localBroadcastManager.sendBroadcast(i);
                }
                break;
            case SERVICE_INFO:
                switch (characteristicUuid) {
                    case CHARACTERISTIC_MAC:
                        String mac = bytesToHexString(data);
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_MAC = " + mac);
                        deviceInfo.setMac(mac);
                        break;
                    case CHARACTERISTIC_SV:
                        String sv = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_SV = " + sv);
                        deviceInfo.setSv(sv);
                        break;
                    case CHARACTERISTIC_HV:
                        String hv = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_HV = " + hv);
                        deviceInfo.setHv(hv);
                        break;
                    case CHARACTERISTIC_SN:
                        String sn = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_SN = " + sn);
                        deviceInfo.setSn(sn);
                        break;
                    case CHARACTERISTIC_PID:
                        String pid = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_PID = " + pid);
                        deviceInfo.setPid(pid);
                        break;
                    case CHARACTERISTIC_OFFLINECOUNT:
                        String offlineCount = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_OFFLINECOUNT = " + offlineCount);
                        deviceInfo.setOfflineCount(offlineCount);
                        break;
                    case CHARACTERISTIC_POWERCOUNT:
                        String powerCount = new String(data).trim();
                        Log.d(TAG, "handleDataReceived: CHARACTERISTIC_POWERCOUNT = " + powerCount);
                        deviceInfo.setPowerCount(powerCount);
                        break;
                }
                if (webSocket != null) {
                    webSocket.send(new WebSocketCapsule("SET_DATA", deviceInfo).toJson());
                }
                sendLocalBroadcast(ACTION_READ_REMOTE_INFO);
                break;
            case SERVICE_DECRYPT:
                if (characteristicUuid.equals(CHARACTERISTIC_DECRYPT)) {
                    onResultDecrypt(data);
                }
                break;
            case SERVICE_OUT_STREET:
                if (characteristicUuid.equals(CHARACTERISTIC_OUT_STREET)) {
                    onResultProtocolV2(data);
                }
                break;
        }
    }

    public Device getDevice() {
        return deviceInfo;
    }

    public void onResultDecrypt(byte[] data) {
        Log.d(TAG, "onResultDecrypt: data = " + Arrays.toString(data));
        if (data != null && data.length > 0) {
            byte[] nativeMpDecrypt = JniUtils.nativeMpDecrypt(data);
            Log.d(TAG, "onResultDecrypt: nativeMpDecrypt = " + Arrays.toString(nativeMpDecrypt));
            write(decryptCharacteristic, nativeMpDecrypt);
        }
    }

    public void onResultProtocolV2(byte[] data) {
        if (data != null && data.length > 0) {
            for (byte datum : data) {
                Log.d(TAG, "onResultProtocolV2: " + Integer.toHexString(datum));
            }

            if (validateBleData(data)) {
                byte cmd = data[0];
                Log.d(TAG, "onResultProtocolV2: cmd = " + cmd);
                if (cmd == 21) { // Set device mode
                    deviceMode = data[2];
                    Log.d(TAG, "onResultProtocolV2: deviceMode = " + data[2]);
                }
            }
        }
    }

    public void readDeviceMode() {
        if (outStreetCharacteristic == null) {
            Log.d(TAG, "readDeviceMode: outStreetCharacteristic = null!");
            return;
        }

        byte[] dataWithChecksum = genBleData((byte) 21);
        if (dataWithChecksum != null && dataWithChecksum.length > 0) {
            for (int i = 0; i < dataWithChecksum.length; i++) {
                Log.d(TAG, "readDeviceMode: dataWithChecksum[" + i + "]: " + Integer.toHexString(dataWithChecksum[i]));
            }
        }

        write(outStreetCharacteristic, dataWithChecksum);
    }

    public void startMotor(byte percent) {
        startMotor(percent, percent);
    }

    public void startMotor(byte percent1, byte percent2) {
        if (percent1 < 0) {
            percent1 = 0;
        } else if (percent1 > 100) {
            percent1 = 100;
        }

        if (percent2 < 0) {
            percent2 = 0;
        } else if (percent2 > 100) {
            percent2 = 100;
        }

        if (dualMotor) {
            write(startDualMotorCharacteristic, percent1, percent2);
        } else {
            write(startSingleMotorCharacteristic, percent1);
        }
    }

    public void stopMotor() {
        if (dualMotor) {
            write(stopMotorCharacteristic, (byte) 2);
        } else {
            write(stopMotorCharacteristic, (byte) 0);
        }
    }

    public void onBright(byte b, byte[] data) {
        if (this.outStreetCharacteristic == null) {
            Log.d(TAG, "onBright: can't find outStreetCharacteristic");
            return;
        }

        if (b >= 0) {
            byte[] dataWithChecksum = genBleData((byte) 18, data);
            write(outStreetCharacteristic, dataWithChecksum);
        } else {
            write(outStreetCharacteristic, (byte) 0);
        }
    }

    public void onLight(byte percent) {
        if (percent < 0) {
            percent = 0;
        } else if (percent > 100) {
            percent = 100;
        }

        write(lightCharacteristic, percent);
    }

    public void onLightEnd() {
        write(lightEndCharacteristic, (byte) 0);
    }

    public void startRemoteControl() {
        stopRemoteControl();

        Request request = new Request.Builder()
                .url("wss://ferrisfyer.selepdf.com/")
                .build();

        OkHttpClient client = new OkHttpClient();
        webSocket = client.newWebSocket(request, webSocketListener);
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    public void stopRemoteControl() {
        if (webSocket != null) {
            webSocket.close(1000, "stopRemoteControl() called");
            webSocket = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class BLEBinder extends Binder {
        public BLEService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BLEService.this;
        }
    }
}
