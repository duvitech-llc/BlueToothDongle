package com.example.george.bluetoothdongle;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by George on 3/17/2015.
 */
public class BluetoothScannerService extends Service {

    public static final String DONGLE_DETECTED = "com.example.george.bluetoothdongle.DONGLE_DETECTED";
    public static final String CABTAG_DETECTED = "com.example.george.bluetoothdongle.CABTAG_DETECTED";

    public enum BL_SCANNER_STATE{
        SLEEPING ,
        SCANNING_LEGACY,
        SCANNING_LE
    }

    private static final String TAG = "BluetoothScannerService";
    private static final int INTERVAL_SECONDS = 20;
    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private BL_SCANNER_STATE mScannerState;
    private BL_SCANNER_STATE mLastScanState;
    private DiscoveryTimer timerTask;
    private Timer timer;

    private final IBinder mBinder = new LocalBinder();

    private static boolean mDiscoverReceiverEnabled = false;
    private boolean mRun = false;
    private boolean mDiscoveryInProgress = false;

    public BluetoothScannerService() {

        timerTask = new DiscoveryTimer();
        mLastScanState = BL_SCANNER_STATE.SLEEPING;
        mScannerState = BL_SCANNER_STATE.SLEEPING;
    }

    private final BroadcastReceiver mDiscoveredDeviceReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                String name = device.getName();

                if(name != null && name.compareTo("OBDII") == 0 )
                {
                    stopBluetoothDiscovery();
                    Log.d(TAG, "Found ODBII dongle ID: " + device.getAddress());
                    broadCastDeviceDetectedIntent(BluetoothScannerService.DONGLE_DETECTED, device.getAddress());
                }
            }
        }
    };

    private void stopBluetoothDiscovery(){
        Log.d(TAG, "Stop Bluetooth Discovery...");
        mBluetoothAdapter.cancelDiscovery();
        mDiscoveryInProgress = false;
        mScannerState = BL_SCANNER_STATE.SLEEPING;
    }

    BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
            Log.d("LeScanCallback", bluetoothDevice.getAddress());

            broadCastDeviceDetectedIntent(BluetoothScannerService.CABTAG_DETECTED, bluetoothDevice.getAddress());
            stopBluetoothLeScanner();
        }
    };

    private void startBluetoothLeScanner(){
        Log.d(TAG, "Start Bluetooth LE Scan...");
        mBluetoothAdapter.startLeScan(leScanCallback);
        mScannerState = BL_SCANNER_STATE.SCANNING_LE;
        mLastScanState = BL_SCANNER_STATE.SCANNING_LE;
    }

    private void stopBluetoothLeScanner(){
        Log.d(TAG, "Stop Bluetooth LE Scan...");
        mBluetoothAdapter.stopLeScan(leScanCallback);
        mScannerState = BL_SCANNER_STATE.SLEEPING;
    }

    private void startBluetoothDiscovery(){
        Log.d(TAG, "Start Bluetooth Discovery...");
        if(!mDiscoverReceiverEnabled){
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mDiscoveredDeviceReceiver, filter); // Don't forget to unregister during onDestroy
            mDiscoverReceiverEnabled = true;
        }

        mBluetoothAdapter.startDiscovery();
        mDiscoveryInProgress = true;
        mScannerState = BL_SCANNER_STATE.SCANNING_LEGACY;
        mLastScanState = BL_SCANNER_STATE.SCANNING_LEGACY;
    }

    private void broadCastDeviceDetectedIntent(String action, String address){
        Log.d(TAG, "Sending broadcast...");
        Intent intent = new Intent(action);
        intent.putExtra("BT_ADDRESS", address);
        sendBroadcast(intent);
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");

        if (timer == null) {
            Log.d(TAG, "starting timer");
            mRun = true;
            timerTask = new DiscoveryTimer();
            timer = new Timer(true);
            timer.scheduleAtFixedRate(timerTask, 0, INTERVAL_SECONDS * 1000);

            mScannerState = BL_SCANNER_STATE.SLEEPING;
            changeScannerState();

        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        cancelScannerState();

        if (timer != null) {
            Log.d(TAG, "purging timer");
            timerTask.cancel();
            timer.cancel();
            timer.purge();
        }
    }

    public void startScanner() {
        Log.d(TAG, "startScanner()");
        if (!mRun) {
            mRun = true;
            timerTask = new DiscoveryTimer();
            timer = new Timer(true);
            timer.scheduleAtFixedRate(timerTask, 0, INTERVAL_SECONDS * 1000);
        }

        mScannerState = BL_SCANNER_STATE.SLEEPING;
        changeScannerState();


    }

    public void cancelScanner() {
        Log.d(TAG, "cancelScanner()");
        cancelScannerState();
        mRun = false;
        if(timerTask != null)
            timerTask.cancel();

        if(timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public void changeScannerState(){
        Log.d(TAG, "changeScannerState");
        switch (mScannerState){
            case SLEEPING:
                if(mBluetoothAdapter.isDiscovering()) {
                    if (mLastScanState == BL_SCANNER_STATE.SCANNING_LE || mLastScanState == BL_SCANNER_STATE.SLEEPING) {
                        stopBluetoothDiscovery();
                    }else{
                        stopBluetoothLeScanner();
                    }
                }

                if(mLastScanState == BL_SCANNER_STATE.SCANNING_LE || mLastScanState == BL_SCANNER_STATE.SLEEPING){
                    startBluetoothDiscovery();
                }else {
                    startBluetoothLeScanner();
                }
                break;
            case SCANNING_LEGACY:
                stopBluetoothDiscovery();
                break;
            case SCANNING_LE:
                stopBluetoothLeScanner();
                break;
            default:
                break;
        }
    }

    public void cancelScannerState(){
        Log.d(TAG, "changeScannerState");
        switch (mScannerState){
            case SCANNING_LEGACY:
                stopBluetoothDiscovery();
                break;
            case SCANNING_LE:
                stopBluetoothLeScanner();
                break;
            default:
                break;
        }
    }

    private class DiscoveryTimer extends TimerTask {
        private static final String DTAG = "DiscoveryTimer";

        @Override
        public void run() {
            Log.d(DTAG, "Checking if discovery is needed");

            changeScannerState();

        }

        @Override
        public long scheduledExecutionTime() {
            Log.d(DTAG, "scheduledExecutionTime()");
            return super.scheduledExecutionTime();
        }

        @Override
        public boolean cancel() {
            Log.d(DTAG, "cancel()");
            return super.cancel();
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        BluetoothScannerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BluetoothScannerService.this;
        }
    }
}