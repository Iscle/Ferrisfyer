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
                        Log.d(TAG, "run: Queue wait got interrupted!");
                        continue;
                    }
                }
            }

            currentOperation = queue.poll();
            currentOperation.execute(gatt);

            if (currentOperation.needsCallback()) {
                try {
                    currentOperation.timeout.execute();
                    synchronized (currentOperation.timeout) {
                        currentOperation.timeout.wait();
                    }
                } catch (IllegalStateException | InterruptedException ignored) {
                }
            }

            currentOperation = null;
        }
    }

    public void setGattAndStart(BluetoothGatt gatt) {
        this.gatt = gatt;
        start();
    }

    public void cancel() {
        running = false;
        synchronized (this) {
            if (currentOperation != null && currentOperation.needsCallback())
                currentOperation.timeout.cancel(true);

            notify();
        }
    }

    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic) {
        if (currentOperation == null ||
                !currentOperation.characteristic.getUuid().equals(characteristic.getUuid())) {
            return;
        }

        currentOperation.timeout.cancel(true);
    }

    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        if (currentOperation == null ||
                !currentOperation.characteristic.getUuid().equals(characteristic.getUuid())) {
            return;
        }

        currentOperation.timeout.cancel(true);
    }

    public class Timeout extends AsyncTask<Void, Void, Void> {
        @Override
        protected synchronized Void doInBackground(Void... voids) {
            try {
                wait(500);
            } catch (InterruptedException ignored) {
            }

            if (!isCancelled()) {
                synchronized (this) {
                    notifyAll();
                }
            }

            return null;
        }

        @Override
        protected synchronized void onCancelled() {
            super.onCancelled();
            synchronized (this) {
                notifyAll();
            }
        }
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
        final Timeout timeout;

        private GattOperation(BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
            if (needsCallback()) this.timeout = new Timeout();
            else this.timeout = null;
        }

        public abstract void execute(BluetoothGatt gatt);
        public abstract boolean needsCallback();
    }
}
