package me.iscle.ferrisfyer;

import me.iscle.ferrisfyer.model.Device;

public interface IDeviceCallback {
    void onRssiUpdated(Device device);
    void onBatteryUpdated(Device device);
    void onMacUpdated(Device device);

    void onSnUpdated(Device device);
}
