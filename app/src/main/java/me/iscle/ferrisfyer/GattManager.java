package me.iscle.ferrisfyer;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.AsyncTask;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.iscle.ferrisfyer.Constants.CLIENT_CHARACTERISTIC_CONFIG;

public class GattManager extends Thread {
    private static final String TAG = "GattManager";

    private BluetoothGatt gatt;
    private GattOperation currentOperation;
    private final ConcurrentLinkedQueue<GattOperation> queue;
    private boolean running;
    private boolean currentOperationFinished;

    public GattManager() {
        this.currentOperation = null;
        this.queue = new ConcurrentLinkedQueue<>();
    }

    public void write(BluetoothGattCharacteristic characteristic, byte... data) {
        queue(new GattWriteOperation(characteristic, data));
    }

    public void read(BluetoothGattCharacteristic characteristic) {
        queue(new GattReadOperation(characteristic));
    }

    public void notify(BluetoothGattCharacteristic characteristic, boolean enable) {
        queue(new GattNotifyOperation(characteristic, enable));
    }

    private void queue(GattOperation gattOperation) {
        queue.add(gattOperation);
        synchronized (this) {
            notify();
        }
    }

    public void run() {
        running = true;

        while (running) {
            if (queue.isEmpty()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        continue;
                    }
                }
            }

            currentOperation = queue.poll();
            currentOperationFinished = false;
            currentOperation.execute(gatt);

            if (currentOperation.needsCallback()) {
                while (!currentOperationFinished);
            }

            currentOperation = null;
        }

        Log.d(TAG, "run: exited gatt loop");
    }

    public void setGattAndStart(BluetoothGatt gatt) {
        this.gatt = gatt;
        start();
    }

    public void cancel() {
        running = false;
        synchronized (this) {
            if (currentOperation != null && currentOperation.needsCallback())
                currentOperationFinished = true;

            notify();
        }
    }

    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        if (currentOperation == null ||
                !currentOperation.characteristic.getUuid().equals(characteristic.getUuid())) {
            return;
        }

        currentOperationFinished = true;
    }

    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        if (currentOperation == null ||
                !currentOperation.characteristic.getUuid().equals(characteristic.getUuid())) {
            return;
        }

        currentOperationFinished = true;
    }

    public class GattReadOperation extends GattOperation {
        public GattReadOperation(BluetoothGattCharacteristic characteristic) {
            super(characteristic);
        }

        @Override
        public void execute(BluetoothGatt gatt) {
            gatt.readCharacteristic(characteristic);
        }

        @Override
        public boolean needsCallback() {
            return true;
        }
    }

    public class GattWriteOperation extends GattOperation {
        private final byte[] data;

        public GattWriteOperation(BluetoothGattCharacteristic characteristic, byte... data) {
            super(characteristic);
            this.data = data;
        }

        @Override
        public void execute(BluetoothGatt gatt) {
            // TODO: FIX CHARACTERISTIC IS NULL
            characteristic.setValue(data);
            gatt.writeCharacteristic(characteristic);
        }

        @Override
        public boolean needsCallback() {
            return true;
        }
    }

    public class GattNotifyOperation extends GattOperation {
        private final boolean notify;

        public GattNotifyOperation(BluetoothGattCharacteristic characteristic, boolean notify) {
            super(characteristic);
            this.notify = notify;
        }

        @Override
        public void execute(BluetoothGatt gatt) {
            gatt.setCharacteristicNotification(characteristic, notify);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            if (notify) {
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            }
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public boolean needsCallback() {
            return false;
        }
    }

    private abstract class GattOperation {
        BluetoothGattCharacteristic characteristic;

        private GattOperation(BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        public abstract void execute(BluetoothGatt gatt);
        public abstract boolean needsCallback();
    }
}
