package me.iscle.ferrisfyer;

public class JniUtils {

    static {
        System.loadLibrary("ble-lib");
    }

    public static native byte[] nativeMpDecrypt(byte[] data);
}