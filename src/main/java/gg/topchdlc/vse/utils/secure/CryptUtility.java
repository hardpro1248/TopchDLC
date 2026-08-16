package gg.topchdlc.vse.utils.secure;

public class CryptUtility {
    final static byte[] MAGIC = new byte[] {(byte) 0xABC, (byte) 0xBCC, (byte) 0xDCD, (byte) 0xDDD};
    public static byte[] proccessXOR(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ (1337 * i));
        }
        return output;
    }
}
