package com.example.george.bluetoothdongle;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.logging.LogRecord;


public class MainActivity extends ActionBarActivity implements IScannedDevices {

    private static final String TAG = "MainActivity";
    private BluetoothScannerService mService;
    private ScannerListenerReceiver rec;
    private boolean mBound;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current application state

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            // device does not have a bluetooth radio

            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        // start listener
        rec = new ScannerListenerReceiver(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        Intent intent = new Intent(this, BluetoothScannerService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        startService(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        mService.cancelScanner();
        if(mConnection != null){
            boolean isBound = false;
            isBound = bindService( new Intent(getApplicationContext(), BluetoothScannerService.class), mConnection, Context.BIND_AUTO_CREATE );
            if(isBound)
                unbindService(mConnection);
        }

        if(rec != null)
            unregisterReceiver(rec);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if(rec != null) {
            registerReceiver(rec, new IntentFilter(BluetoothScannerService.DONGLE_DETECTED));
            registerReceiver(rec, new IntentFilter(BluetoothScannerService.CABTAG_DETECTED));
        }

        if(mService == null){
            Log.d(TAG,"onResume has a null mService Reference");
            mBound = false;
        }
        else
            mService.startScanner();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDongleDetected(String address) {
        Log.d(TAG, "onDongleDetected");
        mService.disableDongleDiscovery();

        // Dongle in range
        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if(fragment != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if(device != null)
                fragment.setupDongleChannel(device);
            else
                Log.w(TAG,"Dongle Name NOT FOUND");
        }
        else
            Log.d(TAG,"Fragment is NULL");

    }

    @Override
    public void onCabTagDetected(String address) {
        Log.d(TAG,"onCabTagDetected");
        mService.disableCabTagDiscovery();
       // mService.cancelScanner();
        // cabtag in range
        PlaceholderFragment fragment = (PlaceholderFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if(fragment != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if(device != null)
                fragment.setupCabTagChannel(device);
            else
                Log.w(TAG,"Dongle Name NOT FOUND");
        }
        else
            Log.d(TAG,"Fragment is NULL");
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        private static String FTAG = "PlaceholderFragment";
        /**
         * Name of the connected dongle
         */
        private String mConnectedDongleName = null;

        /**
         * String buffer for outgoing messages
         */
        private StringBuffer mOutStringBuffer;

        /**
         * Member object for the Dongle Communication service
         */
        private DongleCommService mDongleService = null;
        private CabTagCommService mCabTagService = null;

        private TextView tvStatus;

        public PlaceholderFragment() {
        }

        public void setupDongleChannel(BluetoothDevice device){
            Log.d(FTAG, "setupDongleChannel");
            if(mDongleService != null){
                // we are already connected
                Log.d(FTAG, "Already connected to a dongle");

                if(mDongleService.getState() == DongleCommService.STATE_NONE){
                    mDongleService.connect(device ,false);
                }
            }
            else {
                // initialize the DongleCommService to perform a bluetooth connection
                mDongleService = new DongleCommService(getActivity(), mHandler);
                // Initialize the buffer for outgoing messages
                mOutStringBuffer = new StringBuffer("");
                mDongleService.connect(device, false);
            }
        }


        public void setupCabTagChannel(BluetoothDevice device){
            Log.d(FTAG, "setupCabTagChannel");
            if(mCabTagService != null){
                // we are already connected
                Log.d(FTAG, "Already connected to a CabTag");
            }
            else {

            }

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            tvStatus = (TextView)rootView.findViewById(R.id.tv_status);
            return rootView;
        }

        @Override
        public void onResume() {
            super.onResume();
            Log.d(FTAG,"onResume()");
            if(mDongleService != null){
                if(mDongleService.getState() == DongleCommService.STATE_NONE){
                    mDongleService.start();
                }
            }
        }

        @Override
        public void onPause() {
            super.onPause();
        }

        @Override
        public void onStop() {
            super.onStop();
            if (mDongleService != null) {
                mDongleService.stop();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d(FTAG, "onDestroy()");
        }

        private void sendCommand(String dongleCommand){
        // Check that we're actually connected before trying anything
            if (mDongleService.getState() != DongleCommService.STATE_CONNECTED) {
                Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
                return;
            }

            // Check that there's actually something to send
            if (dongleCommand.length() > 0) {
                if(!dongleCommand.endsWith("\r"))
                    dongleCommand = dongleCommand + "\r";

                // Get the message bytes and tell the BluetoothChatService to write
                byte[] send = dongleCommand.getBytes();
                mDongleService.write(send);

                // Reset out string buffer to zero
                mOutStringBuffer.setLength(0);
                // and clear the edit text field
                //mOutEditText.setText(mOutStringBuffer);
            }
        }
        /**
         * Updates the status on the action bar.
         *
         * @param subTitle status
         */
        private void setStatus(CharSequence subTitle) {
            FragmentActivity activity = getActivity();
            if(tvStatus != null)
                tvStatus.setText(subTitle);
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(subTitle);
        }

        /**
         * Updates the status on the action bar.
         *
         * @param resId a string resource ID
         */
        private void setStatus(int resId) {
            FragmentActivity activity = getActivity();
            if(tvStatus != null)
                tvStatus.setText(getResources().getText(resId));
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(resId);
        }

        private final Handler mHandler = new Handler(){

            @Override
            public void handleMessage(Message msg) {
                FragmentActivity activity = getActivity();
                Log.d(FTAG,"Message Received");

                switch (msg.what) {
                    case Constants.MESSAGE_STATE_CHANGE:
                        Log.d(FTAG,"Message: MESSAGE_STATE_CHANGE");
                        switch (msg.arg1) {
                            case DongleCommService.STATE_CONNECTED:
                                setStatus(getString(R.string.title_connected_to, mConnectedDongleName));
                                break;
                            case DongleCommService.STATE_CONNECTING:
                                setStatus(R.string.title_connecting);
                                break;
                            case DongleCommService.STATE_LISTEN:
                            case DongleCommService.STATE_NONE:
                                setStatus(R.string.title_not_connected);
                                break;
                        }
                        break;
                    case Constants.MESSAGE_WRITE:
                        Log.d(FTAG,"Message: MESSAGE_WRITE");
                        byte[] writeBuf = (byte[]) msg.obj;
                        // construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        if(writeMessage.endsWith("\r"))
                            writeMessage = writeMessage.substring(0, writeMessage.length()-1);
                        Toast.makeText(activity,"Sent: " + writeMessage,
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_READ:
                        Log.d(FTAG,"Message: MESSAGE_READ");
                        byte[] readBuf = (byte[]) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);

                        Toast.makeText(activity,mConnectedDongleName + ": " + readMessage,
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_DEVICE_NAME:
                        Log.d(FTAG,"Message: MESSAGE_DEVICE_NAME");
                        // save the connected device's name
                        mConnectedDongleName = msg.getData().getString(Constants.DEVICE_NAME);
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDongleName, Toast.LENGTH_SHORT).show();
                        break;
                    case Constants.MESSAGE_TOAST:
                        Log.d(FTAG,"Message: MESSAGE_TOAST");
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            BluetoothScannerService.LocalBinder binder = (BluetoothScannerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.startScanner();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };



}
