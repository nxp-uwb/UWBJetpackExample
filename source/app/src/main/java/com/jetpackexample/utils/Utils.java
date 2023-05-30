/*
 * Copyright 2022 NXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetpackexample.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Utils {

    /**
     * Convert an hexadecimal string into a byte array.
     *
     * @param hexString the string to be converted
     * @return the corresponding byte array
     * @throws IllegalArgumentException if not a valid string representation of a byte array
     */
    public static byte[] hexStringToByteArray(String hexString) {
        byte[] result;

        if (hexString == null) {
            throw new IllegalArgumentException("Null input");
        }

        if ((hexString.length() == 0) || (1 == hexString.length() % 2)) {
            throw new IllegalArgumentException("Invalid length");
        }

        result = new byte[hexString.length() / 2];

        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hexString.substring(2 * i, 2 * i + 2), 16);
        }

        return result;
    }

    /**
     * Convert a byte array to an hexadecimal string
     *
     * @param bytes the byte array to be converted
     * @return the output string
     */
    public static String byteArrayToHexString(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Null input");
        }

        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Trim leading bytes from byte array
     *
     * @param inputBytes          String to trim
     * @param amountOfBytesToTrim Number of bytes to trim
     * @return trimmed String
     */
    public static byte[] trimLeadingBytes(byte[] inputBytes, final int amountOfBytesToTrim) {
        final byte[] outputBytes = new byte[inputBytes.length - amountOfBytesToTrim];
        System.arraycopy(inputBytes, amountOfBytesToTrim, outputBytes, 0, inputBytes.length - amountOfBytesToTrim);
        return outputBytes;
    }

    /**
     * Trim bytes from byte array
     *
     * @param inputBytes          String to trim
     * @param amountOfBytesToTrim Number of bytes to trim
     * @return trimmed String
     */
    public static byte[] trimByteArray(byte[] inputBytes, final int amountOfBytesToTrim) {
        final byte[] outputBytes = new byte[inputBytes.length - amountOfBytesToTrim];
        System.arraycopy(inputBytes, 0, outputBytes, 0, inputBytes.length - amountOfBytesToTrim);
        return outputBytes;
    }

    /**
     * Shows dialog with only positive button
     *
     * @param title               Dialog Title
     * @param message             Dialog Message
     * @param rightButtonTxt      Positive button text
     * @param rightButtonListener Positive button listener
     */
    public static void showDialog(Context context, String title, String message, String rightButtonTxt,
                                  final DialogInterface.OnClickListener rightButtonListener) {
        showDialog(context, title, message, null, null, rightButtonTxt, rightButtonListener);
    }

    /**
     * Shows dialog with both positive and negative buttons
     *
     * @param title               Dialog Title
     * @param message             Dialog Message
     * @param leftButtonTxt       Negative button listener
     * @param leftButtonListener  Negative button listener
     * @param rightButtonTxt      Positive button text
     * @param rightButtonListener Positive button listener
     */
    public static void showDialog(Context context, String title, String message, final String leftButtonTxt, final DialogInterface.OnClickListener leftButtonListener,
                                  final String rightButtonTxt, final DialogInterface.OnClickListener rightButtonListener) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);

        if (leftButtonTxt != null && !leftButtonTxt.isEmpty()) {
            builder.setNegativeButton(leftButtonTxt, (dialog, id) -> {
                dialog.dismiss();

                if (leftButtonListener != null) {
                    leftButtonListener.onClick(dialog, id);
                }
            });
        }

        if (rightButtonTxt != null && !rightButtonTxt.isEmpty()) {
            builder.setPositiveButton(rightButtonTxt, (dialog, id) -> {
                dialog.dismiss();

                if (rightButtonListener != null) {
                    rightButtonListener.onClick(dialog, id);
                }
            });
        }

        try {
            builder.create();
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Concatenates the two given arrays
     *
     * @param b1 the first byte array
     * @param b2 the second byte array
     * @return the resulting byte array
     */
    public static byte[] concat(byte[] b1, byte[] b2) {
        if (b1 == null) {
            return b2;
        } else if (b2 == null) {
            return b1;
        } else {
            byte[] result = new byte[b1.length + b2.length];
            System.arraycopy(b1, 0, result, 0, b1.length);
            System.arraycopy(b2, 0, result, b1.length, b2.length);
            return result;
        }
    }

    /**
     * Convert the byte array to an int
     *
     * @param b The byte array
     * @return The integer
     */
    public static int byteArrayToInt(byte[] b) {
        if (b.length == 1) {
            return b[0] & 0xFF;
        } else if (b.length == 2) {
            return ((b[0] & 0xFF) << 8) + (b[1] & 0xFF);
        } else if (b.length == 3) {
            return ((b[0] & 0xFF) << 16) + ((b[1] & 0xFF) << 8) + (b[2] & 0xFF);
        } else if (b.length == 4) {
            return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
        } else
            throw new IndexOutOfBoundsException();
    }

    /**
     * Convert the int to a byte array
     *
     * @param value The integer
     * @return The byte array
     */
    public static byte[] intToByteArray(int value) {
        byte[] result = new byte[4];
        result[3] = (byte) (value & 0xff);
        result[2] = (byte) ((value >> 8) & 0xff);
        result[1] = (byte) ((value >> 16) & 0xff);
        result[0] = (byte) ((value >> 24) & 0xff);
        return result;
    }

    /**
     * Convert the short to a byte array
     *
     * @param value The short
     * @return The byte array
     */
    public static byte[] shortToByteArray(short value) {
        byte[] result = new byte[2];
        result[1] = (byte) (value & 0xff);
        result[0] = (byte) ((value >> 8) & 0xff);
        return result;
    }

    /**
     * Convert the byte array to a short
     *
     * @param b The byte array
     * @return The short
     */
    public static short byteArrayToShort(byte[] b) {
        if (b.length == 1) {
            return (short) (b[0] & 0xFF);
        } else if (b.length == 2) {
            return (short) (((b[0] & 0xFF) << 8) + (b[1] & 0xFF));
        } else
            throw new IndexOutOfBoundsException();
    }

    /**
     * Convert the byte array to a short
     *
     * @param b The byte array
     * @return The short
     */
    public static byte byteArrayToByte(byte[] b) {
        if (b.length == 1) {
            return (byte) (b[0] & 0xFF);
        } else
            throw new IndexOutOfBoundsException();
    }

    /**
     * Convert the short to a byte array
     *
     * @param value The short
     * @return The byte array
     */
    public static byte[] byteToByteArray(byte value) {
        byte[] result = new byte[1];
        result[0] = (byte) (value & 0xff);
        return result;
    }

    /**
     * Extract data from byte array buffer
     *
     * @param buffer Byte array
     * @param length Length to extract
     * @param offset Offset in byte array
     * @return Extracted byte array
     */
    public static byte[] extract(byte[] buffer, int length, int offset) {
        byte[] result = new byte[length];
        System.arraycopy(buffer, offset, result, 0, length);
        return result;
    }

    /**
     * Revert byte array
     *
     * @param data Byte array
     * @return Reverted byte array
     */
    public static byte[] revert(byte[] data) {
        int length = data.length;
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = data[length - 1 - i];
        }
        return result;
    }
}
