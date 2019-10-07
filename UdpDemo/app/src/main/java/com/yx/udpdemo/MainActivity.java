package com.yx.udpdemo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MainActivity";

    private static final int MSG_SEND_DATA = 1001;
    private static final int MSG_RECEIVE_DATA = 1002;

    private volatile boolean mStopReceiver = true;
    Button mBtnReceiveUdpData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSendUdpData = (Button) findViewById(R.id.btn_send_udp_data);
        btnSendUdpData.setOnClickListener(this);
        mBtnReceiveUdpData = (Button) findViewById(R.id.btn_receive_udp_data);
        mBtnReceiveUdpData.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send_udp_data:
                mainHandler.sendEmptyMessage(MSG_SEND_DATA);
                break;
            case R.id.btn_receive_udp_data:
                mainHandler.sendEmptyMessage(MSG_RECEIVE_DATA);
                break;

            default:
                break;
        }
    }

    Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SEND_DATA:
                    sendUDPMessage(System.currentTimeMillis() + "");
                    break;
                case MSG_RECEIVE_DATA:
                    mStopReceiver = !mStopReceiver;
                    if (mStopReceiver) {
                        mBtnReceiveUdpData.setText(R.string.start_receive_udp_data);
                    } else {
                        mBtnReceiveUdpData.setText(R.string.stop_receive_udp_data);
                    }
                    if (!mStopReceiver) {
                        receiveMessage();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private InetAddress mAddress;
    private DatagramSocket mSocket = null;
    /**
     * 发送给整个局域网,局域网网段192.168.0.X
     */
    private String mBroadCastIp = "192.168.0.255";
    /**
     * 发送方和接收方需要端口一致
     */
    private int mSendPort = 8888;
    private byte[] mSendBuf;

    public void sendUDPMessage(final String msg) {

        // 初始化socket
        try {
            mSocket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        try {
            mAddress = InetAddress.getByName(mBroadCastIp);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ThreadPoolManager.getInstance().startTaskThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSendBuf = msg.getBytes("utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                DatagramPacket recvPacket1 =
                        new DatagramPacket(mSendBuf, mSendBuf.length, mAddress, mSendPort);
                try {
                    mSocket.send(recvPacket1);
                    mSocket.close();
                    Log.e(TAG, "sendUDPMessage msg：" + msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "");
    }

    /**
     * 发送方和接收方需要端口一致
     */
    private int mReceivePort = 9999;
    private byte[] mReceiveBuf;
    private DatagramSocket mReceiveSocket = null;
    private void receiveMessage() {
        ThreadPoolManager.getInstance().startTaskThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mReceiveSocket = new DatagramSocket(mReceivePort);
                } catch (SocketException e) {
                    e.printStackTrace();
                }
                mReceiveBuf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(mReceiveBuf, mReceiveBuf.length);
                while (!mStopReceiver) {
                    try {
                        mReceiveSocket.receive(packet);
                        String receive =
                                new String(packet.getData(), 0, packet.getLength(), "utf-8");
                        Log.e(TAG, "receiveMessage msg：" + receive);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                mReceiveSocket.close();
                mReceiveSocket.disconnect();
            }
        }, "");
    }
}
