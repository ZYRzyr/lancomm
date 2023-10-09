package com.fighter.lancomm.ptop;

import com.fighter.common.ConvertUtils;
import com.fighter.lancomm.data.Const;
import com.fighter.lancomm.data.Error;
import com.fighter.lancomm.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author fighter_lee
 * @date 2020/1/16
 */
public class CommandRunnable implements Runnable {

    Command command;

    public CommandRunnable(Command command) {
        this.command = command;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(command.getDestIp(), Const.COMMAND_PORT));
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream();
            byte[] buffer = new byte[1024];
            //发送命令内容
            os.write(packCommandData(command.getData()));

            if (command.getCallback() != null) {
                command.getCallback().onSuccess();
            }

            int len = is.read(buffer);
            if (len != -1) {
                parseCommandRspData(buffer);
            }
        } catch (IOException e) {
            if (command.getCallback() != null) {
                command.getCallback().onError(Error.ERROR_CONNECT_FAIL);
            }
        }

    }

    private void parseCommandRspData(byte[] data) throws UnknownHostException {
        if (data[0] != Const.PACKET_PREFIX) {
            return;
        }

        if (data[1] != Const.PACKET_TYPE_COMMAND_RSP) {
            return;
        }

        byte[] ipData = new byte[4];
        System.arraycopy(data, 2, ipData, 0, 4);
        //收到响应
        if (command.getCallback() != null) {
            command.getCallback().onReceived();
        }
    }

    private byte[] packCommandData(byte[] content) throws SocketException {
        byte[] localIPAddress = Utils.getLocalIPAddress();
        byte[] lengthBytes = ConvertUtils.intToByteArray(content.length);
        byte[] data = new byte[2 + localIPAddress.length + lengthBytes.length + content.length];
        data[0] = Const.PACKET_PREFIX;
        data[1] = Const.PACKET_TYPE_COMMAND;
        System.arraycopy(localIPAddress, 0, data, 2, localIPAddress.length);
        System.arraycopy(lengthBytes, 0, data, 2 + localIPAddress.length, lengthBytes.length);
        System.arraycopy(content, 0, data, 2 + localIPAddress.length + lengthBytes.length, content.length);
        return data;
    }
}
