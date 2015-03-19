package com.example.george.bluetoothdongle;

import java.security.Timestamp;

/**
 * Created by George on 3/18/2015.
 */
public class OBDIIData {

    private String dongleId;

    public String getDongleID(){
        return dongleId.trim();
    }

    public void setDongleId(String str){
        dongleId = new String(str);
    }

    public float CoolantTemp;
    public float FuelLevel;
    public int Rpm;
    public float Speed;

    public Timestamp timestamp;
}
