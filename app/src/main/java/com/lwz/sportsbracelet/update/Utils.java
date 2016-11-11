package com.lwz.sportsbracelet.update;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

public class Utils {

    /**
     * 根据手机分辨率把dp转换成px(像素)
     *
     * @param context
     * @param dpValue
     * @return
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机分辨率把px转换成dp
     *
     * @param context
     * @param pxValue
     * @return
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    /**
     * 格式化手环返回的数据
     *
     * @param data
     * @param characteristic
     * @return
     */
    public static String[] formatData(byte[] data,
                                      BluetoothGattCharacteristic characteristic) {
        if (data != null && data.length > 0) {
            StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            LogModule.i("16位进制数：" + stringBuilder.toString());
            String[] datas = stringBuilder.toString().split(" ");
            return datas;
        } else {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                LogModule.i("Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                LogModule.i("Heart rate format UINT8.");
            }
            int heartRate = characteristic.getIntValue(format, 1);
            LogModule.i(String.format("Received heart rate: %d", heartRate));
            return null;
        }
    }

    /**
     * 16进制数组转10进制数组
     *
     * @param data
     * @return
     */
    public static String[] decode(String data) {
        String[] datas = data.split(" ");
        String[] stringDatas = new String[datas.length];
        for (int i = 0; i < datas.length; i++) {
            stringDatas[i] = Integer.toString(Integer.valueOf(datas[i], 16));
        }
        return stringDatas;
    }

    /**
     * 10进制转16进制
     *
     * @param data
     * @return
     */
    public static String decodeToHex(String data) {
        String string = Integer.toHexString(Integer.valueOf(data));
        return string;
    }

    /**
     * 16进制转10进制
     *
     * @param data
     * @return
     */
    public static String decodeToString(String data) {
        String string = Integer.toString(Integer.valueOf(data, 16));
        return string;
    }

    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0 || s.trim().equals("")
                || s.trim().equals("null");
    }

    public static boolean isNotEmpty(String s) {
        return s != null && s.length() != 0 && !s.trim().equals("")
                && !s.trim().equals("null");
    }

    /**
     * 16进制的字符串表示转成字节数组
     *
     * @param hexString 16进制格式的字符串
     * @return 转换后的字节数组
     **/
    public static byte[] hexStr2ByteArray(String hexString) {
        if (isEmpty(hexString))
            throw new IllegalArgumentException(
                    "this hexString must not be empty");

        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {
            // 因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            // 将hex 转换成byte "&" 操作为了防止负数的自动扩展
            // hex转换成byte 其实只占用了4位，然后把高位进行右移四位
            // 然后“|”操作 低四位 就能得到 两个 16进制数转换成一个byte.
            //
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    private static char[] HexCode = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * byte2HexString
     *
     * @param b
     * @return
     */
    public static String byte2HexString(byte b) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(HexCode[(b >>> 4) & 0x0f]);
        buffer.append(HexCode[b & 0x0f]);
        return buffer.toString();
    }
}
