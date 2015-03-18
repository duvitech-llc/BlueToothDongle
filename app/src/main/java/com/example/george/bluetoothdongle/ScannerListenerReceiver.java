package com.example.george.bluetoothdongle;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ScannerListenerReceiver extends BroadcastReceiver {
    private static final String TAG = "DongleListenerReceiver";

    static IScannedDevices mHandle;

    public ScannerListenerReceiver() {
        mHandle = null;
    }

    public ScannerListenerReceiver(IScannedDevices handle){
        mHandle = handle;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Broadcast Received");

        // an Intent broadcast.
        String action = intent.getAction();
        if(action.equals(BluetoothScannerService.DONGLE_DETECTED)) {
            String sBluetoothAddress = intent.getExtras().getString("BT_ADDRESS");
            Toast.makeText(context, "Dongle Detected.", Toast.LENGTH_LONG).show();
            if(mHandle != null)
                mHandle.onDongleDetected(sBluetoothAddress);
            else
                Log.d(TAG, "Handle is null");
        }else if(action.equals(BluetoothScannerService.CABTAG_DETECTED)){
            String sBluetoothAddress = intent.getExtras().getString("BT_ADDRESS");
            Toast.makeText(context, "CabTag Detected.", Toast.LENGTH_LONG).show();
            if(mHandle != null)
                mHandle.onCabTagDetected(sBluetoothAddress);
            else
                Log.d(TAG, "Handle is null");
        }
    }
}
