package com.pinkMan;

import com.pinkMan.provider.RpcProvider;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {

        /**
         * 启动一个可以提供rpc远程方法调用的Server
         *
         * 1 需要一个RpcProvider对象
         * 2 向RpcProvider上面注册rpc方法 UserServiceImpl.log UserServiceImpl.reg 方法
         * 3 启动RpcProvider这个Server站点 阻塞等待远程rpc方法调用请求
         */

        RpcProvider.Builder builder = RpcProvider.newBuilder();
        RpcProvider rpcProvider = builder.build("config.properties");

        /**
         * UserServiceImpl : 服务对象名称
         * login，reg：服务方法的名称
         */
        rpcProvider.registerRpcService(new UserServiceImpl());

        /**
         * 启动rpc server站点，阻塞等待远程rpc调用请求
         */
        rpcProvider.start();
    }
}
