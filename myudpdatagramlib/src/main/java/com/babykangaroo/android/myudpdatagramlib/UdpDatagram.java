package com.babykangaroo.android.myudpdatagramlib;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by sport on 7/17/2017.
 */

public class UdpDatagram {

    private Context mContext;
    private DatagramSocket mDatagramSocket;
    private InetAddress mDestinationIP;
    private int mDestinationPort;

    public UdpDatagram(Context context, String ipAddress, int port){
        mContext = context;
        try {
            mDestinationIP = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        mDestinationPort = port;
        initializeUdp();

    }

    public void initializeUdp(){
        try {
            mDatagramSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendPacket(String string){
            final DatagramPacket pack = new DatagramPacket(string.getBytes(),string.length()
                    ,mDestinationIP, mDestinationPort);
        udpAsyncTask send = new udpAsyncTask();
        DatagramPacket[] overpack = new DatagramPacket[1];
        overpack[0] = pack;
        send.execute(overpack);
    }

    public void setDestination(String ipAddress, int port){
        try {
            mDestinationIP = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        mDestinationPort = port;
    }

    class udpAsyncTask extends AsyncTask<DatagramPacket, Void, Void>{

        @Override
        protected Void doInBackground(DatagramPacket... params) {
            try {
                mDatagramSocket.send(params[0]);
                Log.v("DATAGRAM", "SEND SUCCESS");
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
