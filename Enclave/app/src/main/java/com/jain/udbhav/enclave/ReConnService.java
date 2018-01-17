package com.jain.udbhav.enclave;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

/**
 * Created by udbhav on 3/7/17.
 */

public class ReConnService extends IntentService {


    ServiceConnection serviceConnection;
    CommService commService;
    Intent service_intent;
    boolean mBound = false;
    private static final int ReconNotifCode = 3;

    public ReConnService()
    {
        super("ReConnService");
    }

    public ReConnService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
      //  Log.d("RECON_SERVICE","STARTED");
        NotificationManagerCompat.from(getApplicationContext()).cancel(ReconNotifCode);
        service_intent = new Intent(this, CommService.class);
        getBaseContext().startService(service_intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                CommService.CommBinder bin = (CommService.CommBinder) service;
                commService = bin.getService();
                //commService.reConnect();

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };


        while(!mBound) {
           // Log.d("RECONN_SERVICE","NOT_BOUND");
            mBound = bindService(service_intent, serviceConnection, 0);
            //Log.d("BIND_LOOP",mBound + "");
        }










    }


    @Override
    public void onDestroy()
    {
        this.unbindService(serviceConnection);
        super.onDestroy();


    }
}
