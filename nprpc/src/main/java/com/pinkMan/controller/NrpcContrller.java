package com.pinkMan.controller;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/02 19:20
 */
public class NrpcContrller implements RpcController {
    private String errText;
    private boolean isFailed;

    @Override
    public void reset() {
        this.isFailed = false;
        this.errText = "";
    }

    @Override
    public boolean failed() {
        return isFailed;
    }

    @Override
    public String errorText() {
        return errText;
    }

    @Override
    public void startCancel() {

    }

    @Override
    public void setFailed(String s) {
        this.isFailed = true;
        this.errText = s;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> rpcCallback) {

    }
}
