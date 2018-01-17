package com.jain.udbhav.enclave;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.input.InputManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by udbhav on 22/6/17.
 */

public class ChatRoomFragment extends Fragment {


    private CommService commService;
    private RecyclerView recycler;
    private Button sendButton;
    private EditText sendText;
    private BroadcastReceiver msgReceiver;
    private BroadcastReceiver fbReceiver;
    private BroadcastReceiver NetStatusReceiver;

    private CommServiceCommunicator communicator;
    private ArrayList<String> msgs;
    private MsgAdapter listAdapter;
    private SQLiteDatabase msgDB;
    private AlertDialog ReconDialog;
    private AlertDialog LogoutDialog;
    private AlertDialog ClearMessagesDialog;
    private AlertDialog KickedOutDialog;

    private static final String logoutMsg = "16c1a9c2-a9d2-4ac3-a7b2-7065ffc5a57c";
    private static final String reconMsg  = "c95e43b6-aa12-4a93-b0c2-4735b22e7b3b";
    private static final String Nfeedback = "N_FEEDBACK";
    private static final String MsgRCfilter = "com.jain.udbhav.enclave.MSGRC";
    private static final String NetStatus = "NETSTATUS";


    private static final String MsgDate = "DATE";
    private static final String MsgTime = "TIME";
    private static final String MsgFrom = "FROM";
    private static final String SenderHash = "HASH";
    private static final String MsgContent = "MSG";

    private static final int MsgNotifCode = 1;
    private static final int LogoutNotifCode = 2;
    private static final int ReconNotifCode = 3;







    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        communicator = (StartupActivity)context;
    }


    @Override
    public void onPause() {
        super.onPause();
        //LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(msgReceiver);
        getActivity().unregisterReceiver(msgReceiver);

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(fbReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(NetStatusReceiver);

    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(MsgRCfilter);
        filter.setPriority(1);

        getActivity().registerReceiver(msgReceiver,filter);
        IntentFilter feedback = new IntentFilter(Nfeedback);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(fbReceiver,feedback);

        IntentFilter netStatusFilter = new IntentFilter(NetStatus);


        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(NetStatusReceiver,netStatusFilter);



    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.log_menu,menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logout:

            {LogoutDialog.show();

                return true;}

            case R.id.clear_messages:
            {
                ClearMessagesDialog.show();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.activity_main,container,false);
        sendButton = (Button)view.findViewById(R.id.button);
        sendText = (EditText)view.findViewById(R.id.editText);
        msgDB  = new MsgStoreHelper(getActivity()).getWritableDatabase();
        NotificationManagerCompat.from(getActivity()).cancelAll();

        NetStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                boolean netAccess = intent.getBooleanExtra(NetStatus,false);
                if(!netAccess)
                {
                    ReconDialog.dismiss();
                }

            }
        };

        IntentFilter netStatusFilter = new IntentFilter(NetStatus);

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(NetStatusReceiver,netStatusFilter);

        KickedOutDialog = new AlertDialog.Builder(getActivity()).setMessage("Kicked out of the server!")
                .setPositiveButton("DISMISS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        KickedOutDialog.dismiss();

                    }
                }).create();



        ClearMessagesDialog = new AlertDialog.Builder(getActivity()).setMessage("Clear all messages?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        msgs.clear();
                        communicator.clearMsgs();
                        listAdapter.notifyDataSetChanged();


                    }
                })
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ClearMessagesDialog.dismiss();
                    }
                }).create();

        ReconDialog = new AlertDialog.Builder(getActivity()).setMessage("Lost connection to server!")
                        .setPositiveButton("RECONNECT", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                communicator.reConnect();
                                ReconDialog.dismiss();
                            }
                        }).setNegativeButton("DISMISS", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReconDialog.dismiss();
                    }
                }).create();

        LogoutDialog = new AlertDialog.Builder(getActivity())
                .setMessage("Are you sure you wish to log out?")
                .setPositiveButton("LOG OUT", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                        boolean netAvailable = cm.getActiveNetworkInfo() != null;
                        boolean netConnected = netAvailable && cm.getActiveNetworkInfo().isConnected();

                        if(netConnected) {
                            communicator.logout();
                            LogoutDialog.dismiss();
                            NotificationManagerCompat.from(getActivity()).cancelAll();
                            getFragmentManager().beginTransaction().replace(R.id.startup_fragment_holder, new StartupFragment())
                                    .commit();
                        }
                        else
                        {
                            Toast.makeText(getActivity(),"No Connectivity!",Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LogoutDialog.dismiss();
                    }
                }).create();

        msgs = new ArrayList<>();
        Cursor cursor = msgDB.rawQuery("select * from messages",null);

        while (cursor.moveToNext()) {
                try {
                    JSONObject msg = new JSONObject();
                    msg.put(MsgDate, cursor.getString(1));
                    msg.put(MsgTime, cursor.getString(2));
                    msg.put(MsgFrom, cursor.getString(3));
                    msg.put(SenderHash, cursor.getString(4));
                    msg.put(MsgContent, cursor.getString(5));
                    msgs.add(msg.toString());
                } catch (JSONException jsx) {}
            }

        setHasOptionsMenu(true);
        sendButton.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {

                String msg = sendText.getText().toString().trim();
                if(!msg.isEmpty())
                {communicator.sendMessage(sendText.getText().toString());
                sendText.setText("");}

            }
        });

        fbReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               // Log.d("CHATROOM_FB_RECEIVER","GOT ONE");
                //new AlertDialog.Builder(getActivity())
                //        .setMessage("Message could not be sent!").create().show();

                Toast.makeText(getActivity().getApplicationContext(),"Message could not be sent.",Toast.LENGTH_SHORT).show();

                ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

                boolean netAvailable = cm.getActiveNetworkInfo() != null;
                boolean netConnected = netAvailable && cm.getActiveNetworkInfo().isConnected();

                if(netConnected) {
                    ReconDialog.show();
                }
            }
        };

        IntentFilter feedback = new IntentFilter(Nfeedback);

        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(fbReceiver,feedback);


        msgReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Log.d("CHATROOM_MSGRECEIVER","RECEIVED");

                String message = intent.getStringExtra("MSG");
                if(message.equals(logoutMsg))
                {
                    //Toast.makeText(getActivity(),"Logged out from server!",Toast.LENGTH_LONG).show();
                    //new AlertDialog.Builder(getActivity()).setMessage("Logged out from the server!")
                    //        .create().show();

                    InputMethodManager im = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    im.hideSoftInputFromWindow(sendText.getWindowToken(),0);
                    KickedOutDialog.show();

                    getFragmentManager().beginTransaction().replace(R.id.startup_fragment_holder,new StartupFragment())
                            .commit();

                }
                else if(message.equals(reconMsg))
                {

                    ReconDialog.show();
                }
                else {
                    msgs.add(message);
                    //recycler.getAdapter().notifyDataSetChanged();
                    recycler.invalidate();
                    recycler.scrollToPosition(msgs.size()-1);

                }
                abortBroadcast();



            }
        };




        recycler =(RecyclerView) view.findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        listAdapter = new MsgAdapter();
        recycler.setAdapter(listAdapter);
        recycler.scrollToPosition(msgs.size()-1);


        IntentFilter filter = new IntentFilter(MsgRCfilter);
        filter.setPriority(1);
        getActivity().registerReceiver(msgReceiver,filter);






        return view;
    }
    private class MsgHolder extends RecyclerView.ViewHolder
    {
        TextView msgView;
        TextView senderView;
        TextView hashView;
        TextView dateView;
        TextView timeView;

        public MsgHolder(View itemView) {
            super(itemView);
            msgView = (TextView)itemView.findViewById(R.id.msg_text);
            senderView = (TextView)itemView.findViewById(R.id.msg_sender);
            hashView = (TextView)itemView.findViewById(R.id.sender_hash);
            dateView = (TextView)itemView.findViewById(R.id.msg_date);
            timeView = (TextView)itemView.findViewById(R.id.msg_time);
        }

    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder>
    {


        @Override
        public MsgHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.message_view,parent,false);
            return new MsgHolder(view);
        }

        @Override
        public void onBindViewHolder(MsgHolder holder, int position)
        {
            try {
                JSONObject msg = new JSONObject(msgs.get(position));
                holder.msgView.setText(msg.getString(MsgContent));
                holder.dateView.setText(msg.getString(MsgDate));
                holder.timeView.setText(msg.getString(MsgTime));
                holder.hashView.setText(msg.getString(SenderHash));
                holder.senderView.setText(msg.getString(MsgFrom));
            }catch(JSONException jsx){}
        }

        @Override
        public int getItemCount() {
            return msgs.size();
        }
    }


}




