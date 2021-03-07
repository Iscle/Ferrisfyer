package me.iscle.ferrisfyer;

public class Utils {
    public static String bytesToHexString(byte[] data) {
        if (data == null || data.length == 0) return "";

        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            String hexString = Integer.toHexString(b & 0xFF);
            if (hexString.length() < 2) sb.append(0);
            sb.append(hexString);
        }

        return sb.toString();
    }
}
