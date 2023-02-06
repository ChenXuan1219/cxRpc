package com.pinkMan.callback;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/02 11:15
 */
public interface INotifyProvider {
    /**
     * 回调操作，RpcServer给RpcProvider上报接收到的rpc服务调用相关参数信息
     * @param serviceName
     * @param methodName
     * @param args
     * @return 把rpc调用完成后的数据响应返回
     */
    byte[] notify(String serviceName, String methodName, byte[] args);
}
