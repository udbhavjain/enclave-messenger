package com.jain.udbhav.enclave;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.renderscript.RenderScript;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

/**
 * Created by udbhav on 27/6/17.
 */

public class MsgNotifReceiver extends BroadcastReceiver {

    private static final String logoutMsg = "16c1a9c2-a9d2-4ac3-a7b2-7065ffc5a57c";
    private static final String reconMsg = "c95e43b6-aa12-4a93-b0c2-4735b22e7b3b";
    private static final String logoutNotif = "Kicked Out Of The Server!";
    private static final String reconNotif = "Lost Connection To Server!";
    private static final String msgNotif = "New Messages!";
    private static final String MsgContent = "MSG";
    private static final String NotifTitle = "Enclave";
    private static final int MsgNotifCode = 1;
    private static final int LogoutNotifCode = 2;
    private static final int ReconNotifCode = 3;

    @Override
    public void onReceive(Context context, Intent intent) {

       // Log.d("MSG_NOTIF_RECEIVER","RECEIVED");

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String msg = intent.getStringExtra(MsgContent);
        String notifText;
        int notifCode;
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setContentTitle(NotifTitle)
                .setAutoCancel(true)
                .setSound(uri)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setLights(Color.YELLOW,500,2000)
                .setVibrate(new long[]{0,500,500,500})
                .setSmallIcon(android.R.drawable.arrow_up_float);

        if(msg.equals(reconMsg))
        {
            notifText = reconNotif;
            Intent conintent = new Intent(context,ReConnService.class);
            PendingIntent pendingIntent = PendingIntent.getService(context,0,conintent,0);
            NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.arrow_up_float,"RECONNECT",pendingIntent);
            notifBuilder.addAction(action);
            notifBuilder.setOngoing(true);
            notifCode = ReconNotifCode;

        }

        else if(msg.equals(logoutMsg))
        {
            notifText = logoutNotif;
            NotificationManagerCompat.from(context).cancelAll();
            notifCode = LogoutNotifCode;
        }

        else
        {
            Intent actIntent = new Intent(context,StartupActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context,1,actIntent,0);
            notifBuilder.setContentIntent(pendingIntent);
            notifText = msgNotif;
            notifCode = MsgNotifCode;
        }

        notifBuilder.setContentText(notifText);
        NotificationManagerCompat.from(context).notify(notifCode,notifBuilder.build());
        abortBroadcast();

    }
}
