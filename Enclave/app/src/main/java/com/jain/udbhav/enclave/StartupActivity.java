package com.jain.udbhav.enclave;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * Created by udbhav on 21/6/17.
 */

public class StartupActivity extends AppCompatActivity implements CommServiceCommunicator {


    private ServiceConnection serviceConnection;
    private CommService commService;
    private Intent service_intent;
    private boolean bound = false;
    private SharedPreferences prefs;

    private static final String prefsInitialised = "INITIALISED";
    private static final String prefsLogged = "LOGGED";
    private static final String KeyStoreFile = "store.bks";
    private static final String KeyStoreType = "BKS";


    @Override
    protected void onDestroy() {

        boolean stopService = commService.getServiceLoggedStatus();
        commService.setActivityOnState(false);

        getApplicationContext().unbindService(serviceConnection);
        if(!stopService)
        {
            //Log.d("STOPPED_SERVICE",Boolean.toString(stopService(service_intent)));
            stopService(service_intent);
        }



        super.onDestroy();



    }



    @Override
    public void login(String ipaddr, String port, String name, String identity)
    {

        commService.login(ipaddr, port, identity, name);
    }

    @Override
    public void logout() {
        commService.logout();
    }

    @Override
    public void sendMessage(String msg) {
        commService.sendMsg(msg);
    }

    @Override
    public void reConnect() {commService.reConnect();}


    @Override
    public void clearMsgs() {
        commService.clearMessages();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup_activity_layout);
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.startup_fragment_holder);
        service_intent = new Intent(this, CommService.class);
        startService(service_intent);


        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(!prefs.contains(prefsInitialised))
        {
            //Log.d("INIT_CHECK","RUNNING");
            SharedPreferences.Editor prefEditor = prefs.edit();
            prefEditor.putBoolean(prefsInitialised,true);
            prefEditor.putBoolean(prefsLogged,false);
            prefEditor.apply();

            try {
                File folder = getFilesDir();
                File store = new File(folder,KeyStoreFile);
            KeyStore ks = KeyStore.getInstance(KeyStoreType);
            ks.load(null,null);
            ks.store(new FileOutputStream(store),null);
            }catch(KeyStoreException |NoSuchAlgorithmException |CertificateException |IOException ksx)
            {
               // Log.d("KEYSTORE_CREATE",ksx.toString());
                }





        }






        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                CommService.CommBinder bin = (CommService.CommBinder) service;
                commService = bin.getService();
                commService.setActivityOnState(true);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        bound = getApplicationContext().bindService(service_intent, serviceConnection, 0);










        if (fragment == null) {

            if (prefs.getBoolean("LOGGED", false)) {
                fragment = new ChatRoomFragment();
            } else {
                fragment = new StartupFragment();
            }
            fm.beginTransaction().add(R.id.startup_fragment_holder, fragment).commit();


        }


    }
}
