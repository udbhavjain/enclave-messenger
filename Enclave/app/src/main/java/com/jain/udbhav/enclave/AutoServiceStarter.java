package com.jain.udbhav.enclave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by udbhav on 27/6/17.
 */

public class AutoServiceStarter extends BroadcastReceiver {



    private static final String NetStatus = "NETSTATUS";



    @Override
    public void onReceive(Context context, Intent intent) {

       // Log.d("SERVICE_STARTER", "RECEIVED");
       // Intent serviceIntent = new Intent(context, CommService.class);
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


        boolean netAvailable = cm.getActiveNetworkInfo() != null;
        boolean netConnected = netAvailable && cm.getActiveNetworkInfo().isConnected();

        Intent netIntent = new Intent(NetStatus);
        netIntent.putExtra(NetStatus,netConnected);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(netIntent);







    }

}
