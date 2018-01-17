package com.jain.udbhav.enclave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by udbhav on 18/7/17.
 */

public class BootTimeStarter extends BroadcastReceiver {

    private static final String prefsLogged = "LOGGED";



    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        if(prefs.getBoolean(prefsLogged,false)) {
            Intent service_intent = new Intent(context, CommService.class);
            context.startService(service_intent);
        }

    }
}
