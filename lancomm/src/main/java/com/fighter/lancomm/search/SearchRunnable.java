package com.fighter.lancomm.search;

import com.fighter.common.UIThreadUtil;
import com.fighter.lancomm.data.Const;
import com.fighter.lancomm.data.Device;
import com.fighter.lancomm.inter.SearchListener;
import com.fighter.lancomm.utils.Utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

/**
 * @author fighter_lee
 * @date 2020/1/14
 */
public class SearchRunnable implements Runnable {

    private final SearchListener listener;

    public SearchRunnable(SearchListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        UIThreadUtil.postUI(new Runnable() {
            @Override
            public void run() {
                listener.onSearchStart();
            }
        });

        HashMap<String, Device> devices = new HashMap<>();
        try {
            DatagramSocket socket = new DatagramSocket();
            //设置接收等待时长
            socket.setSoTimeout(Const.RECEIVE_TIME_OUT);
            byte[] sendData = new byte[1024];
            byte[] receData = new byte[1024];
            DatagramPacket recePack = new DatagramPacket(receData, receData.length);
            //使用广播形式（目标地址设为255.255.255.255）的udp数据包
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName("255.255.255.255"), Const.DEVICE_SEARCH_PORT);
            //用于存放已经应答的设备
            byte[] localIPAddress = Utils.getLocalIPAddress();
            //搜索指定次数
            sendPacket.setData(packSearchData(localIPAddress));
            //发送udp数据包
            socket.send(sendPacket);
            try {
                //限定搜索设备的最大数量
                int rspCount = Const.SEARCH_DEVICE_MAX;
                while (rspCount > 0) {
                    socket.receive(recePack);
                    final Device device = parseRespData(recePack);

                    if (devices.get(device.getIp()) == null) {
                        //保存新应答的设备
                        devices.put(device.getIp(), device);
                        UIThreadUtil.postUI(new Runnable() {
                            @Override
                            public void run() {
                                listener.onSearchedNewOne(device);
                            }
                        });
                    }
                    rspCount--;
                }
            } catch (SocketTimeoutException e) {
            }
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        listener.onSearchFinish(devices);
    }

    private Device parseRespData(DatagramPacket recePack) throws SocketException {
        if (recePack.getLength() < 2) {
            return null;
        }
        byte[] data = recePack.getData();
        if (data[0] != Const.PACKET_PREFIX || data[1] != Const.PACKET_TYPE_SEARCH_DEVICE_RSP) {
            return null;
        }
        byte[] ip = new byte[4];
        System.arraycopy(data, 2, ip, 0, 4);
        String ip1 = recePack.getAddress().getHostAddress();
        return new Device(ip1);
    }

    private byte[] packSearchData(byte[] hostAddress) {
        byte[] data = new byte[2 + hostAddress.length];
        data[0] = Const.PACKET_PREFIX;
        data[1] = Const.PACKET_TYPE_SEARCH_DEVICE;
        System.arraycopy(hostAddress, 0, data, 2, hostAddress.length);
        return data;
    }
}
