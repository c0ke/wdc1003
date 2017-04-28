package com.nasket.wdc1003;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.felhr.utils.HexData;

import java.util.Set;

/**
 * Created by Coke on 4/22/17.
 */

public class WDC1003 {
    private Context context;
    private BroadcastReceiver usbReceiver;
    private ServiceConnection usbConnection;
    private UsbHandler usbHandler;
    private UsbService usbService;
    private WDC1003Interface usbInterface;

    public void setup(Activity activity) {
        context = activity.getBaseContext();
        usbInterface = ((WDC1003Interface) activity);

        usbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                        Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                        // write default config to scanner
                        //String buadRate = WDC1003Utils.buildCommand("50", "C0006", "FF");
                        //String hidCommunication = WDC1003Utils.buildCommand("50", "A0003", "FF");
                        //String transmitNoRead = WDC1003Utils.buildCommand("50", "G0002", "FF");
                        //String lightTimeout = WDC1003Utils.buildCommand("50", "F0499", "FF");

                        //usbService.write(HexData.stringTobytes(buadRate));
                        //usbService.write(HexData.stringTobytes(hidCommunication));
                        //usbService.write(HexData.stringTobytes(transmitNoRead));
                        //usbService.write(HexData.stringTobytes(lightTimeout));
                        break;
                    case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                        Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                        break;
                    case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                        Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                        break;
                    case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                        Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                        break;
                    case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                        Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        usbHandler = new UsbHandler(usbInterface);

        usbConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName arg0, IBinder arg1) {
                usbService = ((UsbService.UsbBinder) arg1).getService();
                usbService.setHandler(usbHandler);
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                usbService = null;
            }
        };

        context.unregisterReceiver(usbReceiver);
        context.unbindService(usbConnection);

        setFilter(context);
        startService(UsbService.class, usbConnection, null);
    }

    public void onPause() {
        context.unregisterReceiver(usbReceiver);
        context.unbindService(usbConnection);
    }

    public void onResume() {
        if (context != null) {
            setFilter(context);
            startService(UsbService.class, usbConnection, null);
        }
    }

    public void sendCommand(String cmd) {
        if (usbService != null) {
            usbService.write(HexData.stringTobytes(cmd));
        }
    }

    public void turnOn() {
        sendCommand(WDC1003.buildCommand("26", "LT", "FF"));
    }

    public void turnOff() {
        sendCommand(WDC1003.buildCommand("27", "LS", "FF"));
    }

    private void setFilter(Context context) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        context.registerReceiver(usbReceiver, filter);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(context, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            context.startService(startService);
        }
        Intent bindingIntent = new Intent(context, service);
        context.bindService(bindingIntent, serviceConnection, Context.BIND_IMPORTANT);
    }

    /**
     * @param hexString
     * @return
     */

    public static String calculateChecksum(String hexString) {
        String stringProcessed = hexString.trim().replaceAll("0x", "");
        stringProcessed = stringProcessed.replaceAll("\\s+", "");

        //Log.i("Utils", "calculateChecksum: " + hexString);
        //Log.i("Utils", "calculateChecksum: " + stringProcessed);

        int checksum = 0;
        int i = 0;

        for (int j = 0; i <= stringProcessed.length() - 1; i += 2) {
            int character = Integer.parseInt(stringProcessed.substring(i, i + 2), 16);
            checksum += character;
            ++j;
        }

        //Log.i("Utils", "calculateChecksum: " + checksum);
        //Log.i("Utils", "calculateChecksum: " + Integer.toHexString(checksum));
        //Log.i("Utils", "calculateChecksum: " + ("0000000000000000" + Integer.toBinaryString(checksum)).substring(Integer.toBinaryString(checksum).length()));

        String binary = ("0000000000000000" + Integer.toBinaryString(checksum)).substring(Integer.toBinaryString(checksum).length());
        String reverse = "";
        for (char index : binary.toCharArray()) {
            if (index == '0') {
                reverse += '1';
            } else {
                reverse += '0';
            }
        }

        return Integer.toHexString(Integer.valueOf(reverse, 2) + 1);
    }

    public static String buildCommand(String opCode, String command, String beeper) {
        /* Length | Message Source | Message target | Reserve | Opcode | Command | Beeper | Check Sum
        *  1 byte | 1 byte         | 1 byte         | 1 byte  | 1 byte | VAR/NO  | 1 byte | 2 bytes
        *  0xFF   | 04 = PC        | 04 = PC        | 0x00    | OpCode | ASCII   | 31=ENA |
        *         | 31 = Decoder   | 31 = Decoder   | 0x00    | OpCode |         | FF=DIS |
        */
        String header = "043100";
        String send = header + opCode;

        for (char cmd : command.toCharArray()) {
            send += ("00" + Integer.toHexString((int) cmd)).substring(Integer.toHexString((int) cmd).length());
        }

        send += beeper;

        int length = (send.length() / 2) + 1;

        send = ("00" + Integer.toHexString(length)).substring(Integer.toHexString(length).length()) + send;
        String checksum = calculateChecksum(send);

        send += checksum;
        send = send.toUpperCase();

        return send;
    }
}
