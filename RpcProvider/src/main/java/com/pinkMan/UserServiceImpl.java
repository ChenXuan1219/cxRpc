package com.pinkMan;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/01 16:10
 */

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * 原来是本地服务方法，现在发布成RPC方法
 */
public class UserServiceImpl extends UserServiceProto.UserServiceRpc {
    /**
     * 登陆业务
     */
    public boolean login(String name,String pwd){
        System.out.println("call UserServiceImpl -> login");
        System.out.println("name" + name );
        System.out.println("pwd"  + pwd);
        return true;
    }

    /**
     * 注册业务
     * @param name
     * @param pwd
     * @param age
     * @param sex
     * @param phone
     * @return
     */
    public boolean reg(String name,String pwd,int age,String sex,String phone){
        System.out.println("call UserServiceImpl -> reg");
        System.out.println("name " + name);
        System.out.println("pwd" + pwd);
        System.out.println("sex" + sex);
        System.out.println("phone" + phone);
        return true;
    }

    @Override
    public void login(RpcController controller, UserServiceProto.LoginRequest request, RpcCallback<UserServiceProto.Response> done) {

        //1 从request里面读取到远程rpc调用请求的参数了
        String name = request.getName();
        String pwd = request.getPwd();

        //2 根据解析的参数，做本地业务
        boolean result = login(name, pwd);

        //3 填写方法的响应值
        UserServiceProto.Response.Builder response_builder = UserServiceProto.Response.newBuilder();
        response_builder.setErrno(0);
        response_builder.setErrinfo("正常");
        response_builder.setResult(result);

        //4 把response对象给nprpc框架，由框架负责处理发送rpc调用响应值
        done.run(response_builder.build());

    }

    @Override
    public void reg(RpcController controller, UserServiceProto.RegRequest request, RpcCallback<UserServiceProto.Response> done) {

        int age = request.getAge();
        String name = request.getName();
        String phone = request.getPhone();
        String pwd = request.getPwd();
        UserServiceProto.RegRequest.SEX sex = request.getSex();

        boolean result = reg(name, pwd, age, String.valueOf(sex), phone);

        UserServiceProto.Response.Builder response_builder = UserServiceProto.Response.newBuilder();
        response_builder.setErrno(0);
        response_builder.setErrinfo("正常");
        response_builder.setResult(result);

        done.run(response_builder.build());

    }

    public static void main(String[] args) {

        UserServiceImpl userService = new UserServiceImpl();
        userService.reg(null, null, null);

    }
}
