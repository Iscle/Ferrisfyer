package me.iscle.ferrisfyer;

public interface IDeviceControl {
    void startMotor(byte percent);

    void startMotor(byte percent1, byte percent2);

    void stopMotor();
}
