package co.arago.hiro.client.util;

public class HexUtil {

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int pos = 0; pos < bytes.length; pos++) {
            int b = Byte.toUnsignedInt(bytes[pos]);
            hexChars[pos * 2] = Character.forDigit(b >>> 4, 16);
            hexChars[pos * 2 + 1] = Character.forDigit(b & 0x0F, 16);
        }
        return new String(hexChars);
    }

}
