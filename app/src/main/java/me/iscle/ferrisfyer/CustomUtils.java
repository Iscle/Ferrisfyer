package me.iscle.ferrisfyer;

import java.util.Arrays;

public class CustomUtils {

    public static byte[] genBleData(byte cmd) {
        return genBleData(cmd, null);
    }

    public static byte[] genBleData(byte cmd, byte[] data) {
        byte[] bleData;

        if (data == null || data.length == 0) {
            bleData = new byte[2];
            bleData[0] = cmd;
            bleData[1] = 0;
        } else {
            bleData = new byte[data.length + 2];
            bleData[0] = cmd;
            bleData[1] = (byte) data.length;
            System.arraycopy(data, 0, bleData, 2, data.length);
        }

        // Calculate the checksum
        byte[] checksum = calculateCrc(bleData);
        // Create a new array copy with space for the checksum
        byte[] bleDataWithChecksum = Arrays.copyOf(bleData, bleData.length + 2);
        // Add the calculated checksum to the payload
        bleDataWithChecksum[bleDataWithChecksum.length - 2] = checksum[0];
        bleDataWithChecksum[bleDataWithChecksum.length - 1] = checksum[1];

        return bleDataWithChecksum;
    }

    public static String bytesToHexString(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        for (byte b : data) {
            String hexString = Integer.toHexString(b & 255);
            if (hexString.length() < 2) {
                sb.append(0);
            }
            sb.append(hexString);
        }

        return sb.toString();
    }

    public static byte[] calculateCrc(byte[] bArr) {
        int b = 65535;
        for (byte b2 : bArr) {
            int b3 = (((b << 8) | (b >> 8)) & 65535) ^ b2;
            int b4 = b3 ^ ((b3 & 255) >> 4);
            int b5 = b4 ^ (((b4 << 8) << 4) & 65535);
            b = b5 ^ ((((b5 & 255) << 4) << 1) & 65535);
        }

        byte[] checksum = new byte[2];
        checksum[1] = (byte) ((b >> 8) & 255);
        checksum[0] = (byte) (b & 255);
        return checksum;
    }

    public static boolean validateBleData(byte[] data) {
        int length = data.length;
        if (length < 4) {
            return false;
        }

        byte[] receivedChecksum = Arrays.copyOfRange(data, length - 2, length);
        byte[] calculatedChecksum = calculateCrc(Arrays.copyOfRange(data, 0, length - 2));

        return Arrays.equals(receivedChecksum, calculatedChecksum);
    }
}
