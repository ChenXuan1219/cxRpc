package com.pinkMan.provider;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/01 18:10
 */

import com.google.protobuf.*;
import com.pinkMan.callback.INotifyProvider;
import com.pinkMan.util.ZkClientUtils;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * rpc方法发布的站点，只需要一个站点就可以发布当前主机上所有的rpc方法，使用单例模式
 *
 */
//静态内部类
public class RpcProvider implements INotifyProvider {
    private static final String SERVER_IP = "IP";
    private static final String SERVER_PORT = "PORT";
    private static final String ZK_SERVER = "ZOOKEEPER";
    private String ZkServer;
    private String server_ip;
    private int server_port;
    private ThreadLocal<byte[]> responseBufThreadLocal;

    /**
     * 包含所有的rpc服务对象和服务方法
     */
    private Map<String,ServiceInfo> serviceInfoMap;

    /**
     * 服务方法的类型信息
     */
    class ServiceInfo{
        public ServiceInfo(){
            this.service = null;
            this.methodMap = new HashMap<>();
        }
        Service service;
        Map<String, Descriptors.MethodDescriptor> methodMap;
    }



    public void start() {
//        System.out.println("================================");
//        serviceInfoMap.forEach((k ,v)-> {
//            System.out.println("服务对象名： "+k);
//            v.methodMap.forEach((a,b)-> {
//                System.out.println("服务对象名为： "+k+"的"+"服务方法名： "+a);
//            });
//        });
//        System.out.println("rpc server start at: " + server_ip + ":" + server_port);
//        System.out.println("================================");
        ZkClientUtils zkClientUtils = new ZkClientUtils(ZkServer);
        serviceInfoMap.forEach((k ,v)-> {
            String path = "/" + k;
            zkClientUtils.createPersistent(path, null);
            v.methodMap.forEach((a, b) -> {

                String createPath = path + "/" + a;

                zkClientUtils.createEphemeral(createPath, server_ip + ":" + server_port);

                //给临时性节点 添加watcher
                zkClientUtils.addWatcher(createPath);

                System.out.println("reg zk ->  " + createPath);
            });
        });
        System.out.println("rpc server start at: " + server_ip + ":" + server_port);
        //启动rpc server网络服务
        RpcServer server = new RpcServer(this);
        server.start(server_ip,server_port);

    }




    /**
     * 注册服务方法 只要支持rpc方法的类，都实现了com.google.protobuf.service 这个接口
     * @param
     */
    public void registerRpcService(Service service) {
        Descriptors.ServiceDescriptor sd = service.getDescriptorForType();

        //获取服务对象的名称
        String serviceName = sd.getName();

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.service = service;

        //获取对象的所有方法
        List<Descriptors.MethodDescriptor> methodList = sd.getMethods();

        methodList.forEach(method-> {
            //获取服务方法名称
            String methodName = method.getName();
            serviceInfo.methodMap.put(methodName, method);
        });

        serviceInfoMap.put(serviceName,serviceInfo);

    }

    /**
     * 再多线程环境中被调用
     * 接收RpcServer网络模块上报的rpc调用相关的信息参数，执行具体的rpc方法调用
     * @param serviceName
     * @param methodName
     * @param args
     * @return  把rpc方法调用完成后的响应值进行返回
     */
    @Override
    public byte[] notify(String serviceName, String methodName, byte[] args) {
        ServiceInfo serviceInfo = serviceInfoMap.get(serviceName);
        Service service = serviceInfo.service;//获取服务对象
        Descriptors.MethodDescriptor method = serviceInfo.methodMap.get(methodName);//获取服务方法

        //从args反序列化出method方法的参数
        Message request = service.getRequestPrototype(method).toBuilder().build();
        try {
            request = request.getParserForType().parseFrom(args);//反序列化操作
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }

        /**
         * rpc对象： service
         * rpc对象的方法： method
         * rpc对象的参数： request
         * 根据method.getName() => login
         */
        service.callMethod(method, null, request, response -> {
            responseBufThreadLocal.set(response.toByteArray());
        });

        return responseBufThreadLocal.get();
    }

    public static class Builder {

        private static RpcProvider INSTANCE = new RpcProvider();

        public RpcProvider build(String file) {
            Properties properties = new Properties();
            try {
                properties.load(Builder.class.getClassLoader().getResourceAsStream(file));
                INSTANCE.setServer_port(Integer.parseInt(properties.getProperty(SERVER_PORT)));
                INSTANCE.setServer_ip(properties.getProperty(SERVER_IP));
                INSTANCE.setZkServer(properties.getProperty(ZK_SERVER));
                return INSTANCE;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;

        }

    }

    private RpcProvider(){
        this.serviceInfoMap = new HashMap<>();
        this.responseBufThreadLocal = new ThreadLocal<>();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getServer_ip() {
        return server_ip;
    }

    public void setServer_ip(String server_ip) {
        this.server_ip = server_ip;
    }

    public int getServer_port() {
        return server_port;
    }

    public void setServer_port(int server_port) {
        this.server_port = server_port;
    }

    public void setZkServer(String zkServer) {
        ZkServer = zkServer;
    }
}
