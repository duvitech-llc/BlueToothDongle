package com.example.george.bluetoothdongle;

/**
 * Created by George on 3/19/2015.
 */

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment implements  IDataServiceListener{
    private static final String TAG = "PlaceholderFragment";
    private static String FTAG = "PlaceholderFragment";
    private TextView tvStatus;

    private final IDataServiceListener activity = this;

    protected DataUpdateService mDataService;
    private boolean mDataServiceBound;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    public PlaceholderFragment() {
    }

    public void setupCabTagChannel(BluetoothDevice device){
        Log.d(FTAG, "setupCabTagChannel");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        tvStatus = (TextView)rootView.findViewById(R.id.tv_status);

        Button btnSendCommand = (Button)rootView.findViewById(R.id.btnSend);
        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity temp = (MainActivity)getActivity();
                sendDongleCommand("0105");
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            // device does not have a bluetooth radio

            Toast.makeText(getActivity(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
            getActivity().finish();
        }


        // start data service
        Intent intent = new Intent(getActivity(), DataUpdateService.class);
        getActivity().startService(intent);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(FTAG,"onResume()");

    }

    @Override
    public void onPause() {
        super.onPause();

        if(mDataConnection != null){
            boolean isBound = false;
            isBound = getActivity().bindService(new Intent(getActivity(), DataUpdateService.class), mDataConnection, Context.BIND_AUTO_CREATE);
            if(isBound)
                getActivity().unbindService(mDataConnection);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = new Intent(getActivity(), DataUpdateService.class);
        getActivity().bindService(intent, mDataConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(FTAG, "onDestroy()");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }


    public void sendDongleCommand(String command){
        if(mDataService != null)
            if(mDataService.isDongleConnectd())
                mDataService.sendDongleCommand(command);
            else
                Log.i(TAG, "No Dongle connected");
        else
            Log.i(TAG, "Dataservice not available");
    }

    public void updateStatus(String status){
        tvStatus.setText(status);
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


    @Override
    public void onDongleDetected(String address) {
        Log.d(TAG, "onDongleDetected");
        Toast.makeText(getActivity(), "Dongle Detected.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCabTagDetected(String address) {
        Log.d(TAG,"onCabTagDetected");
        Toast.makeText(getActivity(), "CabTag Detected.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationUpdated(Location location) {

        Toast.makeText(getActivity(), ""+location.getLatitude()+", "+location.getLongitude(), Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onDongleResponse(String message) {
        Log.d(TAG,"Dongle Response: " + message);
    }

    @Override
    public void onDongleStateChange(int dongleState) {

        switch (dongleState){
            case DongleCommService.STATE_NONE:
            case DongleCommService.STATE_CONNECTING:
            case DongleCommService.STATE_LISTEN:
                updateStatus("Not Connected");
                break;
            case DongleCommService.STATE_CONNECTED:
                updateStatus("Connected");
                break;
            default:
                updateStatus("Unknown");
                break;
        }
    }


    private ServiceConnection mDataConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("ServiceConnection", "DataUpdateService Bound");
            DataUpdateService.LocalBinder binder = (DataUpdateService.LocalBinder) iBinder;
            mDataService = binder.getService();
            mDataServiceBound = true;

            mDataService.RegisterListener(activity);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d("ServiceConnection", "DataUpdateService UnBound");

            mDataService.UnregisterListener(activity);
            mDataServiceBound = false;

        }
    };
}
