package me.iscle.ferrisfyer.model;

import android.bluetooth.BluetoothDevice;

public class Device {
    private BluetoothDevice bluetoothDevice;
    private String mac;
    private String sv;
    private String hv;
    private String sn;
    private String pid;
    private String offlineCount;
    private String powerCount;

    public Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
        this.mac = "-";
        this.sv = "-";
        this.hv = "-";
        this.sn = "-";
        this.pid = "-";
        this.offlineCount= "-";
        this.powerCount = "-";
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSv() {
        return sv;
    }

    public void setSv(String sv) {
        this.sv = sv;
    }

    public String getHv() {
        return hv;
    }

    public void setHv(String hv) {
        this.hv = hv;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getOfflineCount() {
        return offlineCount;
    }

    public void setOfflineCount(String offlineCount) {
        this.offlineCount = offlineCount;
    }

    public String getPowerCount() {
        return powerCount;
    }

    public void setPowerCount(String powerCount) {
        this.powerCount = powerCount;
    }

    @Override
    public String toString() {
        return "Device{" +
                ", mac='" + mac + '\'' +
                ", sv='" + sv + '\'' +
                ", hv='" + hv + '\'' +
                ", sn='" + sn + '\'' +
                ", pid='" + pid + '\'' +
                ", offlineCount='" + offlineCount + '\'' +
                ", powerCount='" + powerCount + '\'' +
                '}';
    }
}
