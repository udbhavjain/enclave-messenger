package com.jain.udbhav.enclave;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by udbhav on 18/6/17.
 */

public class CommService extends Service {


    private static final String logoutMsg = "16c1a9c2-a9d2-4ac3-a7b2-7065ffc5a57c";
    private static final String reconMsg  = "c95e43b6-aa12-4a93-b0c2-4735b22e7b3b";
    private static final String prefsLogged = "LOGGED";
    private static final String prefsIP = "IP";
    private static final String prefsPort = "PORT";

    private static final String NetStatus = "NETSTATUS";
    private static final String NetStatusExtra = "NETSTATUS";
    private static final String ClearMsgsQuery = "delete from messages where msgID not null";
    private static final String Nfeedback = "N_FEEDBACK";
    private static final String LoginResult = "LOGIN_RESULT";
    private static final String HashAlgo = "SHA-256";
    private static final String LoginName = "NAME";
    private static final String LoginHash = "ID";
    private static final String LoginUUID = "UUID";
    private static final String KeyStoreType = "BKS";
    private static final String KeyStoreFileName = "store.bks";
    private static final String SocketTypeSSL = "SSL";
    private static final String MsgRCfilter = "com.jain.udbhav.enclave.MSGRC";
    private static final String MsgContent = "MSG";
    private static final String MsgDate = "DATE";
    private static final String MsgTime = "TIME";
    private static final String MsgFrom = "FROM";
    private static final String SenderHash = "HASH";







    private static boolean isRunning = false;
    private static boolean isActivityOn = false;
    private static ObjectInputStream oin;
    private static ObjectOutputStream out;
    private static Socket socket;
    private static boolean logged = false;
    private static BroadcastReceiver stopSignalReceiver;
    private static IntentFilter stopSigFilter;
    private boolean connected;
    private final IBinder mIBinder = new CommBinder();
    private static SQLiteDatabase msgDB;
    private static final String insertQuery = "insert into messages values(?,?,?,?,?,?)";
    private static SQLiteStatement insertStatement;
    private static final int ReconnMaxAttempts = 10;
    private static final int ReconNotifCode = 3;



    private static String IP;
    private static String port;
    private static String name;
    private static String ID;
    private static UUID userID;

    private MsgReader readTask = null;

    private static ExecutorService exec;
    private static int reConnCount = 0;
    public class CommBinder extends Binder
    {
        public CommService getService()
        {
            return CommService.this;
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        //Log.d("COMM_SERVICE","UNBOUND");
        //return super.onUnbind(intent);

        return true;
    }



    @Override
    public void onRebind(Intent intent) {

        reConnect();
        super.onRebind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        //Log.d("COMMSERVICE_STARTER",Boolean.toString(isRunning));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        logged = prefs.getBoolean(prefsLogged,false);
        if(!isRunning)
        {
            msgDB = new MsgStoreHelper(getApplicationContext()).getWritableDatabase();
            insertStatement = msgDB.compileStatement(insertQuery);


            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);


            boolean netAvailable = cm.getActiveNetworkInfo() != null;
            connected = netAvailable && cm.getActiveNetworkInfo().isConnected();

            Notification startNotif = new NotificationCompat.Builder(getApplicationContext())
                    .setSmallIcon(android.R.drawable.arrow_up_float)
                    .setContentTitle("Enclave")
                    .setContentText("Service running.")
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .build();

            startForeground(128,startNotif);


            stopSigFilter = new IntentFilter(NetStatus);
            stopSignalReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    boolean status = intent.getBooleanExtra(NetStatusExtra,false);
                    if(status)
                    {
                        connected = true;
                        if(logged)reConnect();
                       // Log.d("COMMRECEIVER","ACCESS YES");

                    }
                    else
                    {
                        connected = false;
                        if(readTask!=null)
                        {readTask.readMsgs = false;
                        readTask = null;}
                        NotificationManagerCompat.from(getApplicationContext()).cancel(ReconNotifCode);
                        //Log.d("COMMRECEIVER","ACCESS NOPE");
                    }

                }
            };
            LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(stopSignalReceiver,stopSigFilter);
            isRunning = true;
            exec = Executors.newCachedThreadPool();

            if(logged) {
               // Log.d("COMMSERVICE_LOGGED","YES");

                reConnect();
            }
        }

        return Service.START_STICKY;
    }


    public void clearMessages()
    {
        new MessageCleanerTask().executeOnExecutor(exec);
    }

    class MessageCleanerTask extends AsyncTask<Void,Void,Void>
    {
        @Override
        protected Void doInBackground(Void... params) {

            msgDB.execSQL(ClearMsgsQuery);


            return null;
        }
    }



    public void logout()
    {
        logged = false;
        /*if(readTask!=null)
        {readTask.readMsgs = false;
            readTask = null;}*/
       // Log.d("COMMSERVICE","LOGGING OUT");

        //NotificationManagerCompat.from(getApplicationContext()).cancel(0);
        //NotificationManagerCompat.from(getApplicationContext()).cancel(1);

        new LogoutTask().executeOnExecutor(exec);

    }

    class LogoutTask extends AsyncTask<Void,Void,Void>
    {

        @Override
        protected Void doInBackground(Void... params) {

            try {
                msgDB.execSQL(ClearMsgsQuery);
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .putBoolean(prefsLogged,false).apply();

                if(readTask != null && readTask.readMsgs)
                {
                    readTask.readMsgs = false;
                    readTask = null;


                    out.writeObject(logoutMsg);

                }

                socket.close();


            }catch(IOException x){}
            if(!isActivityOn) {stopSelf();}

            return null;
        }
    }


    public void setActivityOnState(boolean ActivityState)
    {
        isActivityOn = ActivityState;
    }


    @Override
    public void onDestroy() {


        //Log.d("COMM_SERVICE","DESTROYING");
        //oin = null;
        //out = null;
        //socket = null;
        //isRunning = false;
        //readTask = null;
        stopForeground(true);
        super.onDestroy();
    }

    public boolean getServiceLoggedStatus()
    {
        return logged;
    }

    public void startRead()
    {
        if(readTask == null) {
            readTask = new MsgReader();
            readTask.readMsgs = true;
           // Log.d("READTASK_FUNC","STARTING NEW");

            readTask.executeOnExecutor(exec);
        }
    }

    public static boolean getStatus()
    {
        return isRunning;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //Log.d("COMM_SERVICE","BOUND");
        reConnect();

        return mIBinder;

    }

    public void sendMsg(String msg)
    {
       // Log.d("SENDER_FUNC",msg);
        if(readTask != null && readTask.readMsgs)
        {
            new MsgSender().executeOnExecutor(exec,msg);
        }
        else
        {
            Intent feedback = new Intent(Nfeedback);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(feedback);
        }


    }


    public void login(String ipaddr, String port, String id, String name)
    {
        new LoginTask().executeOnExecutor(exec,ipaddr,port,id,name);
    }



    class LoginTask extends AsyncTask<String,Void,Void>
    {

        @Override
        protected Void doInBackground(String... params) {

            Intent response = new Intent(LoginResult);
            int resultcode =2;


            try {

                String ipaddr = params[0];
                int portno = Integer.parseInt(params[1]);
                String id = params[2];
                String name = params[3];

                byte[] encodedID = MessageDigest.getInstance(HashAlgo).digest(id.getBytes());
                String encodedID64 = Base64.encodeToString(encodedID,Base64.NO_WRAP);




                socket = getConnectionToServer();
                socket.connect(new InetSocketAddress(ipaddr,portno),4000);
                socket.setSoTimeout(4000);


                out = new ObjectOutputStream(socket.getOutputStream());

                JSONObject logInfo = new JSONObject();

                logInfo.put(LoginName,name);
                logInfo.put(LoginHash,encodedID64);



                out.writeObject(logInfo.toString());
               // Log.d("WRITING_LOG_INFO","WRITTEN");
                oin = new ObjectInputStream(socket.getInputStream());



                Boolean available =(Boolean) oin.readObject();

                if(available)
                {
                    socket.setSoTimeout(0);
                    resultcode = 0;
                    userID = (UUID)oin.readObject();
                    SharedPreferences.Editor editor = PreferenceManager
                            .getDefaultSharedPreferences(getApplicationContext())
                            .edit();

                    logged = true;
                    editor.putBoolean(prefsLogged,true);
                    editor.putString(prefsIP,ipaddr);
                    editor.putString(prefsPort,Integer.toString(portno));
                    editor.putString(LoginHash,encodedID64);
                    editor.putString(LoginName,name);
                    editor.putString(LoginUUID,userID.toString());
                    editor.apply();
                    startRead();


                }
                else
                {
                    resultcode = 1;
                }


            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException
                    | KeyManagementException|ClassNotFoundException|JSONException| NumberFormatException ex) {
               // Log.d("SOCKET_CREATN", ex.toString());

                resultcode =2;
            }
            response.putExtra(LoginResult,resultcode);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(response);

            return null;

        }
    }

    private Socket getConnectionToServer() throws IOException,KeyManagementException,NoSuchAlgorithmException,KeyStoreException,CertificateException
    {

        KeyStore keyStore = KeyStore.getInstance(KeyStoreType);
        File keyFile = new File(getFilesDir(), KeyStoreFileName);
        FileInputStream is = new FileInputStream(keyFile);
        keyStore.load(is, null);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance(SocketTypeSSL);
        sslContext.init(null,
                tmf.getTrustManagers(),
                new SecureRandom());

        socket = sslContext.getSocketFactory().createSocket();
        return socket;

    }





    public void reConnect()
    {
       // Log.d("READTASK AND LOGGED",(boolean)(readTask==null) + " : " + logged );
        if(readTask == null && logged)new ReconnectTask().executeOnExecutor(exec);



    }


    private class ReconnectTask extends AsyncTask<Void,Void,Void>
    {

        @Override
        protected Void doInBackground(Void... params) {

            try{
                reConnCount++;
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                IP = prefs.getString(prefsIP, null);
                port = prefs.getString(prefsPort, null);
                name = prefs.getString(LoginName, null);
                ID = prefs.getString(LoginHash, null);

                userID = UUID.fromString(prefs.getString(LoginUUID, null));

            /*Log.d("RECON_TASK","BEGIN");
                Log.d("RECONNECT PORT NO: ",port);
                Log.d("RECONNECT IP ADDR: ",IP);
                Log.d("RECONNECT NAME: ",name);
                Log.d("RECONNECT ID: ",ID);
                Log.d("RECONNECT UUID: ",userID.toString());*/

                socket = getConnectionToServer();
                socket.connect(new InetSocketAddress(IP,Integer.parseInt(port)));
               // socket.setSoTimeout(20000);
            out = new ObjectOutputStream(socket.getOutputStream());
                //Log.d("RECON_TASK","Outputstream null :" + Boolean.toString(out==null));

            JSONObject logInfo = new JSONObject();
            logInfo.put(LoginName, name);
            logInfo.put(LoginHash, ID);
            logInfo.put(LoginUUID, userID.toString());

                out.writeObject(logInfo.toString());
           // Log.d("WRITING_LOG_INFO", "WRITTEN");
            oin = new ObjectInputStream(socket.getInputStream());
                Boolean result = (Boolean)oin.readObject();
               // Log.d("RECON_TASK_RC",result.toString());
                if(result.booleanValue())
                {//Log.d("RECON_TASK","SUCCESS");

                    //NotificationManagerCompat.from(getApplicationContext()).cancel(0);
                startRead();}
                else
                {
                   // Log.d("RECON_TASK","REJECT");
                    Intent i = new Intent(MsgRCfilter);
                    i.putExtra(MsgContent,logoutMsg);
                    sendOrderedBroadcast(i,null);

                    PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                            .edit().putBoolean("LOGGED",false).apply();
                    logged = false;
                    logout();


                }
                reConnCount = 0;
        }catch(IOException | NoSuchAlgorithmException | KeyStoreException | CertificateException
        | KeyManagementException|JSONException|ClassNotFoundException ex)
            {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);


                boolean netAvailable = cm.getActiveNetworkInfo() != null;
                boolean netConnected = netAvailable && cm.getActiveNetworkInfo().isConnected();

                if(logged && netConnected)
                {

                    if(reConnCount >= ReconnMaxAttempts)
                {
                    reConnCount = 0;



                    Intent i = new Intent(MsgRCfilter);
                    i.putExtra(MsgContent,reconMsg);

                    sendOrderedBroadcast(i,null);
                } else
                {
                        reConnect();
                }

                }



            }


            return null;
        }
    }


    public void confirm(String msg)
    {

            new confirmTask().executeOnExecutor(exec,msg);

    }
    class confirmTask extends AsyncTask<String,Void,Void>
    {

        @Override
        protected Void doInBackground(String... params) {

            try {
                String msg = params[0];
                JSONObject msgObject = new JSONObject(msg);
                UUID mID = UUID.fromString(msgObject.getString("MID"));

                out.writeObject(mID);
                //Log.d("CONFIRMATION","SENT");
                insertMessage(mID.toString(),
                        msgObject.getString(MsgDate),
                        msgObject.getString(MsgTime),
                        msgObject.getString(MsgFrom),
                        msgObject.getString(SenderHash),
                        msgObject.getString(MsgContent));



                    Intent i = new Intent(MsgRCfilter);
                    i.putExtra(MsgContent, msg);

                    sendOrderedBroadcast(i, null);



            }catch(IOException|JSONException iox){//Log.d("CONFIRMATION","FAILED");
                 }
            return null;
        }
    }

    private void insertMessage(String msgID,String date,String time,String sender,String hash,String message)
    {
        insertStatement.bindString(1,msgID);
        insertStatement.bindString(2,date);
        insertStatement.bindString(3,time);
        insertStatement.bindString(4,sender);
        insertStatement.bindString(5,hash);
        insertStatement.bindString(6,message);
        long id = insertStatement.executeInsert();
       // Log.d("MSG_INSERT","RESULT: " + id);
    }


    class MsgSender extends AsyncTask<String,Void,Void>
    {



        @Override
        protected Void doInBackground(String... params) {


            try{

                JSONObject msg = new JSONObject();
                msg.put(MsgContent,params[0]);
                //Log.d("BEFORE_OUT","LOG");
                out.writeObject(msg.toString());
                out.flush();
               // Log.d("AFTER_OUT","LOG");
            }catch(IOException|JSONException iox)
            {
                if(readTask!=null)
                {readTask.readMsgs = false;
                readTask = null;}
              //  Log.d("SEND_ERROR",iox.getMessage());
                Intent feedback = new Intent(Nfeedback);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(feedback);
                //reConnect();
            }

            return null;
        }
    }

    class MsgReader extends AsyncTask<Void,Void,Void>
    {

        boolean readMsgs = false;


        @Override
        protected Void doInBackground(Void... params) {


            while(readMsgs)
            {
                try {
                   // Log.d("MSG_READER","READY!");


                    String msg = (String)oin.readObject();
                    //Log.d("MSG_READER","MSG_RC: "+msg);


                    if(msg.matches(logoutMsg))
                    {
                       // Log.d("READER","RECEIVED LOGOUT");
                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                .edit().putBoolean(prefsLogged,false).apply();
                        //readMsgs = false;
                        Intent i = new Intent(MsgRCfilter);
                        i.putExtra(MsgContent,msg);

                        sendOrderedBroadcast(i,null);
                        logout();
                        //stopSelf();
                    }
                    else
                    {
                        confirm(msg);

                    }






                }catch(IOException|ClassNotFoundException iox){
                    readMsgs = false;
                    readTask = null;

                    if(reConnCount >= ReconnMaxAttempts)
                    {
                        reConnCount = 0;
                    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

                    boolean netAvailable = cm.getActiveNetworkInfo() != null;
                    boolean netConnected = netAvailable && cm.getActiveNetworkInfo().isConnected();

                    if(netConnected && logged)
                    {
                        // Log.d("RECON_NOTIF","SENT");
                        Intent i = new Intent(MsgRCfilter);
                        i.putExtra(MsgContent,reconMsg);

                        sendOrderedBroadcast(i,null);
                    }

                    }
                    else
                    {
                        reConnect();
                    }



                    //readTask = null;
                    //reConnect();
                    break;
                }
            }
           // Log.d("READTASK","LOOP ENDING");
            readMsgs = false;
            readTask = null;
            return null;
        }
    }



}
