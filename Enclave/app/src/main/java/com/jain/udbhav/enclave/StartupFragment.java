package com.jain.udbhav.enclave;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Enumeration;
import java.util.UUID;

/**
 * Created by udbhav on 21/6/17.
 */

public class StartupFragment extends Fragment {

    private BroadcastReceiver loginReceiver;
    private ConnectDialogFragment connFrag;
    private AlertDialog ErrorDialog;
    private AlertDialog RejectDialog;
    private AlertDialog AppInfoDialog;
    private AlertDialog ClearCertsDialog;
    private File KeyStoreFile;

    private static final String LoginResultFilter = "LOGIN_RESULT";
    private static final String LoginResultExtra = "LOGIN_RESULT";
    private static final String KeyStoreFileName = "store.bks";
    private static final String KeyStoreType = "BKS";
    private static final String CertificateType = "X.509";


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main_menu,menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_cert:
                {selectCert();
                return true;}

            case R.id.app_info:
            {
                AppInfoDialog.show();
                return true;
            }

            case R.id.clear_certs:
            {
                ClearCertsDialog.show();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void selectCert()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 0);
    }




    @Override
    public void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(loginReceiver);

    }

    public void onResume()
    {
        super.onResume();


        IntentFilter filter = new IntentFilter(LoginResultFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(loginReceiver,filter);


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.startup_fragment_layout,container,false);
        setHasOptionsMenu(true);

        NotificationManagerCompat.from(getActivity()).cancelAll();
        File folder = getActivity().getFilesDir();
        KeyStoreFile = new File(folder, KeyStoreFileName);


        ClearCertsDialog = new AlertDialog.Builder(getActivity())
                .setMessage("Remove all certificates?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            KeyStore ks = KeyStore.getInstance(KeyStoreType);
                            ks.load(new FileInputStream(KeyStoreFile), null);
                            Enumeration<String> certs = ks.aliases();
                            while(certs.hasMoreElements())
                            {
                                String cert = certs.nextElement();
                                //Log.d("CLEAR_CERT",cert);
                                ks.deleteEntry(cert);


                            }

                            ks.store(new FileOutputStream(KeyStoreFile), null);
                            Toast.makeText(getActivity(),"Certificates deleted!",Toast.LENGTH_SHORT);
                        }catch(KeyStoreException | IOException | NoSuchAlgorithmException |CertificateException ex)
                        {
                            Toast.makeText(getActivity(),"Error clearing certificates!",Toast.LENGTH_SHORT).show();
                        }



                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClearCertsDialog.dismiss();
                    }
                }).create();

        AppInfoDialog = new AlertDialog.Builder(getActivity())
                .setView(R.layout.info_dialog_layout)
                .setPositiveButton("DISMISS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppInfoDialog.dismiss();
                    }
                }).create();

        ErrorDialog = new AlertDialog.Builder(getActivity()).setMessage("Could not connect to server!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ErrorDialog.dismiss();
                    }
                }).create();

        RejectDialog = new AlertDialog.Builder(getActivity()).setMessage("Request rejected by server!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RejectDialog.dismiss();
                    }
                }).create();

        FloatingActionButton connectButton = (FloatingActionButton) v.findViewById(R.id.connect_floating_button);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                connFrag = new ConnectDialogFragment();

                connFrag.show(getActivity().getFragmentManager(),"ConnectFrag");



            }
        });

        loginReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int result = intent.getIntExtra(LoginResultExtra,2);
                connFrag.dismiss();
                switch (result)
                {
                    case 0:
                    {


                        getFragmentManager().beginTransaction().replace(R.id.startup_fragment_holder,new ChatRoomFragment())
                        .commit();

                        break;
                    }

                    case 2:
                    {
                        ErrorDialog.show();
                        break;
                    }

                    case 1:
                    {
                        RejectDialog.show();
                        break;

                    }
                }


            }
        };

        IntentFilter filter = new IntentFilter(LoginResultFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(loginReceiver,filter);

        return v;

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)

    {

        if(requestCode == 0) {
            if (data != null) {
                Uri fileUri = data.getData();





                ContentResolver resolver = getActivity().getContentResolver();

                try {
                    //FileInputStream is = new FileInputStream(resolver.openInputStream(fileUri));
                    KeyStore ks = KeyStore.getInstance(KeyStoreType);
                    ks.load(new FileInputStream(KeyStoreFile), null);
                    Certificate cert = CertificateFactory.getInstance(CertificateType).generateCertificate(resolver.openInputStream(fileUri));
                    ks.setCertificateEntry(UUID.randomUUID().toString(), cert);
                    ks.store(new FileOutputStream(KeyStoreFile), null);



                } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException ksx) {
                   // Log.d("KEYSTORE_CREATE", ksx.toString());
                    Toast.makeText(getActivity(),"Error adding certificate!",Toast.LENGTH_SHORT).show();
                }

            }
        }


    }




}
