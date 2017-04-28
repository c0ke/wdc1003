package com.nasket.wdc1003;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Coke on 4/21/17.
 */

class UsbHandler extends Handler {
    private final WDC1003Interface usbInterface;

    UsbHandler(WDC1003Interface usbInterface) {
        this.usbInterface = usbInterface;
    }

    @Override
    public void handleMessage(Message msg) {
        Log.i("UsbHandler", "handleMessage: " + msg.toString());
        switch (msg.what) {
            case UsbService.MESSAGE_FROM_SERIAL_PORT:
                String data = (String) msg.obj;
                usbInterface.message(data);
                //mActivity.get().txtResult.append(data + "\n");
                break;
            case UsbService.CTS_CHANGE:
                //Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                usbInterface.message("CTS_CHANGE");
                break;
            case UsbService.DSR_CHANGE:
                //Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                usbInterface.message("DSR_CHANGE");
                break;
            case UsbService.SYNC_READ:
                String buffer = (String) msg.obj;
                usbInterface.message(buffer);
                //mActivity.get().txtResult.append(buffer + "\n");
                break;
        }
    }
}