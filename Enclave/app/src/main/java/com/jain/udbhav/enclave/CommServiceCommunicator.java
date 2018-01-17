package com.jain.udbhav.enclave;

import android.app.Service;
import android.content.ServiceConnection;

/**
 * Created by udbhav on 22/6/17.
 */

public interface CommServiceCommunicator {

    public void login(String ipaddr, String port, String name, String identity);
    public void sendMessage(String msg);
    public void logout();
    public void reConnect();
    public void clearMsgs();

}
