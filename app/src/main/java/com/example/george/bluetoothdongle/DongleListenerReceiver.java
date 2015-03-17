package com.example.george.bluetoothdongle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class DongleListenerReceiver extends BroadcastReceiver {
    private static final String TAG = "DongleListenerReceiver";

    public DongleListenerReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast Received");
        // an Intent broadcast.
        String action = intent.getAction();
        if(action.equals(BluetoothScannerService.DONGLE_DETECTED)) {
            Toast.makeText(context, "Dongle Detected.", Toast.LENGTH_LONG).show();
        }else if(action.equals(BluetoothScannerService.CABTAG_DETECTED)){
            Toast.makeText(context, "CabTag Detected.", Toast.LENGTH_LONG).show();
        }
    }
}
