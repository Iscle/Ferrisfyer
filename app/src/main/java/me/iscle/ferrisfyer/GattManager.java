package me.iscle.ferrisfyer;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.AsyncTask;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import static me.iscle.ferrisfyer.Constants.CLIENT_CHARACTERISTIC_CONFIG;

public class GattManager {
    private static final String TAG = "GattManager";

    private BluetoothGatt gatt;
    private ConcurrentLinkedQueue<GattOperation> queue;
    private GattOperation currentOperation;
    private CurrentOperationTimeout currentOperationTimeout;


    public GattManager(BluetoothGatt gatt) {
        this.gatt = gatt;
        queue = new ConcurrentLinkedQueue<>();
        currentOperation = null;
        currentOperationTimeout = null;
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

    public synchronized void queue(GattOperation gattOperation) {
        queue.add(gattOperation);
        run();
    }

    public synchronized void onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        currentOperation = null;
        run();
    }

    public synchronized void onCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        currentOperation = null;
        run();
    }

    public synchronized void run() {
        while (true) {
            if (currentOperation != null) {
                Log.d(TAG, "run: currentOperation != null!");
                return;
            }

            if (queue.isEmpty()) {
                Log.d(TAG, "run: queue is empty!");
                return;
            }

            if (currentOperationTimeout != null) {
                currentOperationTimeout.cancel(true);
            }

            currentOperation = queue.poll();
            currentOperation.execute(gatt);

            if (currentOperation.needsCallback()) {
                currentOperationTimeout = new CurrentOperationTimeout(this);
                currentOperationTimeout.execute();

                break;
            }

            currentOperation = null;
        }

    }

    public GattOperation getCurrentOperation() {
        return currentOperation;
    }

    public synchronized void setCurrentOperation(GattOperation currentOperation) {
        this.currentOperation = currentOperation;
    }

    public static class CurrentOperationTimeout extends AsyncTask<Void, Void, Void> {
        private GattManager gattManager;

        private CurrentOperationTimeout(GattManager gattManager) {
            this.gattManager = gattManager;
        }

        @Override
        protected synchronized Void doInBackground(Void... voids) {
            try {
                //wait(gattManager.getCurrentOperation().getTimeoutMillis());
                wait(1000);
            } catch (InterruptedException ignored) {
            }

            if (isCancelled()) {
                return null;
            }

            gattManager.setCurrentOperation(null);
            gattManager.run();

            return null;
        }

        @Override
        protected synchronized void onCancelled() {
            super.onCancelled();
            notify();
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
        private byte[] data;

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
        private boolean notify;

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
        public long getTimeoutMillis() {
            return 1000;
        }
    }
}
