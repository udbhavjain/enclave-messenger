package com.jain.udbhav.enclave;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by udbhav on 21/6/17.
 */

public class ConnectDialogFragment extends DialogFragment {
    private CommServiceCommunicator communicator;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        communicator = (StartupActivity)context;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        final View view = LayoutInflater.from(getActivity()).inflate(R.layout.connect_dialog_layout,null);

        final EditText ipaddr =  (EditText)view.findViewById(R.id.ip_addr_field);
        final EditText port = (EditText)view.findViewById(R.id.port_field);
        final EditText name = (EditText)view.findViewById(R.id.name_field);
        final EditText id = (EditText)view.findViewById(R.id.id_field);



        Dialog dialog = new AlertDialog.Builder(getActivity()).setView(view)
                .setTitle("Connect to a server: ")
                .setPositiveButton(getResources().getText(R.string.connect_dialog_connect), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                            final String ip = ipaddr.getText().toString().trim();
                            final String portno = port.getText().toString().trim();
                            final String uname = name.getText().toString().toLowerCase().trim();
                            final String idval = id.getText().toString().trim();

                            if(ip.isEmpty() || portno.isEmpty() || uname.isEmpty() || idval.isEmpty())
                            {
                                Toast.makeText(getActivity(),"Please fill all the fields.",Toast.LENGTH_SHORT).show();

                            }
                            else if(!uname.matches("[a-zA-Z]+"))
                            {
                                Toast.makeText(getActivity(),"Name should only contain alphabets.",Toast.LENGTH_SHORT).show();
                            }
                            else {


                                communicator.login(ip, portno, uname, idval);


                            }



                    }
                })
        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ConnectDialogFragment.this.dismiss();
            }
        }).create();
        return dialog;
    }
}
