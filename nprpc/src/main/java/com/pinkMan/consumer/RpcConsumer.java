package com.pinkMan.consumer;

import com.google.protobuf.*;
import com.pinkMan.RpcMetaProto;
import com.pinkMan.provider.RpcProvider;
import com.pinkMan.util.ZkClientUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/02 15:09
 */
public class RpcConsumer implements RpcChannel {
    /**
     * stub代理对象，需要实现一了RpcChannel的对象，当用stub调用任意rpc方法的时候
     * 全部调用了当前这个RpcChannel的callMethod方法
     * @param methodDescriptor
     * @param rpcController
     * @param message
     * @param message1
     * @param rpcCallback
     */
    private static final String ZK_SERVER = "ZOOKEEPER";
    private String Zk_SERVER;
    public RpcConsumer(String file){
        Properties properties = new Properties();
        try {
            properties.load(RpcProvider.Builder.class.getClassLoader().getResourceAsStream(file));
            this.Zk_SERVER = properties.getProperty(ZK_SERVER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void callMethod(Descriptors.MethodDescriptor methodDescriptor,
                           RpcController rpcController,
                           Message message, //request
                           Message message1, //response
                           RpcCallback<Message> rpcCallback) {

        /**
         * 打包参数，底角网络发送
         * rpc调用参数格式： header_size + service_name + method_name + args
         */
        Descriptors.ServiceDescriptor service = methodDescriptor.getService();
        String serviceName = service.getName();
        String methodName = methodDescriptor.getName();

        //在zookeeper上查询serviceName-methodName在哪个主机上 ip和port
        String IP = "";
        int PORT = 0;
        ZkClientUtils zk = new ZkClientUtils(Zk_SERVER);
        String path = "/" + serviceName + "/" + methodName;
        String hostStr = zk.read(path);
        zk.close();
        if (hostStr == null) {
            rpcController.setFailed("read path:" + path +"data from zk is failed! ");
            rpcCallback.run(message1);
        } else {
            String[] host = hostStr.split(":");
            IP = host[0];
            PORT = Integer.parseInt(host[1]);
        }

        //序列化头部信息
        RpcMetaProto.RpcMeta.Builder builder = RpcMetaProto.RpcMeta.newBuilder();
        builder.setServiceName(serviceName);
        builder.setMethodName(methodName);
        byte[] metaBuf = builder.build().toByteArray();

        //参数
        byte[] argBuf = message.toByteArray();

        //组织rpc参数信息
        ByteBuf buf = Unpooled.buffer(4 + metaBuf.length + argBuf.length);
        buf.writeInt(metaBuf.length);
        buf.writeBytes(metaBuf);
        buf.writeBytes(argBuf);

        //代发送的数据
        byte[] sendBuf = buf.array();

        //通过网络发送rpc调用请求信息
        Socket client = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;

        try {
            client = new Socket();
            client.connect(new InetSocketAddress(IP,PORT));
            outputStream = client.getOutputStream();
            inputStream = client.getInputStream();

            //发送数据
            outputStream.write(sendBuf);
            outputStream.flush();

            //wait 等待rpc调用响应返回
            ByteArrayOutputStream recBuf = new ByteArrayOutputStream();
            byte[] rBuf = new byte[1024];
            int size = inputStream.read(rBuf);

            /**
             * size 有可能是0
             */
            if (size > 0) {
                recBuf.write(rBuf,0,size);
                rpcCallback.run(message1.getParserForType().parseFrom(recBuf.toByteArray()));
            } else {
                rpcCallback.run(message1);
            }

        } catch (IOException e) {
            rpcController.setFailed("server connect error,check server！");
            rpcCallback.run(message1);
        } finally {
            try {

                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
