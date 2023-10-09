package com.fighter.lancomm.search;

import android.app.Service;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import com.fighter.common.ContextVal;
import com.fighter.lancomm.data.Const;
import com.fighter.lancomm.utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author fighter_lee
 * @date 2020/1/15
 */
public class SearchRspThread extends Thread {

    private static SearchRspThread searchRspThread;
    private String tarIp = "";

    /**
     * 启动响应线程，收到设备搜索命令后，自动响应
     */
    public static void open() {
        if (searchRspThread == null) {
            searchRspThread = new SearchRspThread();
            searchRspThread.start();
        }
    }

    /**
     * 停止响应
     */
    public static void close() {
        if (searchRspThread != null) {
            searchRspThread.destory();
            searchRspThread = null;
        }
    }

    DatagramSocket socket;
    volatile boolean openFlag;

    public void destory() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        openFlag = false;
        releaseLock();
    }


    @Override
    public void run() {
        wakeLock();
        try {
            //指定接收数据包的端口
            socket = new DatagramSocket(Const.DEVICE_SEARCH_PORT);
            byte[] buf = new byte[1024];
            DatagramPacket recePacket = new DatagramPacket(buf, buf.length);
            openFlag = true;
            while (openFlag) {
                socket.receive(recePacket);
                //校验数据包是否是搜索包
                if (verifySearchData(recePacket)) {
                    //发送搜索应答包
                    byte[] sendData = packSearchRespData();
                    DatagramPacket sendPack = new DatagramPacket(sendData, sendData.length, recePacket.getSocketAddress());
                    socket.send(sendPack);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            destory();
        }
    }

    private byte[] packSearchRespData() throws SocketException, UnknownHostException {
        byte[] localIPAddress = Utils.getLocalIPAddress();
        byte[] data = new byte[2 + localIPAddress.length];
        data[0] = Const.PACKET_PREFIX;
        data[1] = Const.PACKET_TYPE_SEARCH_DEVICE_RSP;
        System.arraycopy(localIPAddress, 0, data, 2, 4);
        return data;
    }

    private boolean verifySearchData(DatagramPacket pack) throws IOException {
        if (pack.getLength() < 2) {
            return false;
        }
        byte[] data = pack.getData();
        if (data[0] != Const.PACKET_PREFIX) {
            return false;
        }

        if (data[1] != Const.PACKET_TYPE_SEARCH_DEVICE) {
            return false;
        }

        byte[] ipData = new byte[4];
        System.arraycopy(data, 2, ipData, 0, 4);
        tarIp = Utils.ipbyteToString(ipData);
        return true;
    }

    private WakeLock wakelock;

    private void wakeLock() {
        PowerManager pm = (PowerManager) ContextVal.getContext()
                .getSystemService(Service.POWER_SERVICE);
        wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
        wakelock.acquire();
    }

    private void releaseLock() {
        if (wakelock != null && wakelock.isHeld()) {
            wakelock.release();
        }
    }
}
