package me.iscle.ferrisfyer.model;

import android.bluetooth.BluetoothDevice;

public class Device {
    private final BluetoothDevice bluetoothDevice;
    private String mac;
    private String sn;
    private int rssi;
    private byte battery;
    private boolean dualMotor;

    public Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        this.rssi = -1;
        this.battery = -1;
        this.dualMotor = false;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public byte getBattery() {
        return battery;
    }

    public void setBattery(byte battery) {
        this.battery = battery;
    }

    public boolean isDualMotor() {
        return dualMotor;
    }

    public void setDualMotor(boolean dualMotor) {
        this.dualMotor = dualMotor;
    }

    @Override
    public String toString() {
        return "Device{" +
                "bluetoothDevice=" + bluetoothDevice +
                ", mac='" + mac + '\'' +
                ", rssi=" + rssi +
                '}';
    }
}
