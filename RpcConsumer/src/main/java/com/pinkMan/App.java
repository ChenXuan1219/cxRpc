package com.pinkMan;

import com.google.protobuf.RpcController;
import com.pinkMan.consumer.RpcConsumer;
import com.pinkMan.controller.NrpcContrller;

/**
 * Hello world!
 *
 */
public class App {
    public static void main( String[] args ) {

        /**
         * 模拟RPC方法调用者
         */

        UserServiceProto.UserServiceRpc.Stub stub = UserServiceProto.UserServiceRpc.newStub(new RpcConsumer("config.properties"));

        UserServiceProto.LoginRequest.Builder builder = UserServiceProto.LoginRequest.newBuilder();
        builder.setName("pinkMan");
        builder.setPwd("123456789");

        NrpcContrller con = new NrpcContrller();

        stub.login(con, builder.build(), response ->{
            /**
             * 这就是rpc方法调用完成以后的返回值
             *
             */
            if (con.failed()){ //rpc方法没有调用成功
                System.out.println(con.errorText());
            } else {
                System.out.println("receive rpc call response!");
                if (response.getErrno() == 0) {
                    System.out.println(response.getResult());
                } else {
                    System.out.println(response.getErrinfo());
                }
            }
        });

    }
}
