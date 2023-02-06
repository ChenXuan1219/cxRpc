package com.pinkMan.util;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/03 11:03
 */

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 和zookeeper通信用的辅助工具类
 */
public class ZkClientUtils {

    private static String rootPath = "/nprpc";

    private ZkClient zkClient;

    private Map<String,String> ephemeralMap = new HashMap<>();

    /**
     * 通过serverList 字符串信息连接zkServer
     * @param serverList
     */
    public ZkClientUtils(String serverList) {
        this.zkClient = new ZkClient( serverList,3000);

        //  如果root节点不存在，创建root节点
        if (!this.zkClient.exists(rootPath)) { //rootPath不存在创建root节点
            this.zkClient.createPersistent(rootPath,null);
        }

    }

    /**
     * 关闭和zkServer的连接
     */
    public void close() {
        this.zkClient.close();
    }

    /**
     * 创建临时性节点，zk断开，节点会被释放
     * @param path
     * @param data
     */
    public void createEphemeral(String path,String data) {
        path = rootPath + path;
        ephemeralMap.put(path, data);
        if (!this.zkClient.exists(path)) { //znode不存在创建path节点
            this.zkClient.createEphemeral(path,data);
        }
    }

    /**
     * 创建永久性节点
     * @param path
     * @param data
     */
    public void createPersistent(String path, String data) {
        path = rootPath + path;
        if (!this.zkClient.exists(path)) { //znode不存在创建path节点
            this.zkClient.createPersistent(path,data);
        }
    }

    /**
     * 给zk上指定的znode添加watcher监听
     * @param path
     */
    public void addWatcher(String path) {
        this.zkClient.subscribeDataChanges(rootPath + path, new IZkDataListener() {
            @Override
            public void handleDataChange(String s, Object o) throws Exception {

            }

            /**
             * 设置节点znode节点监听
             * 因为zkClient断掉，由于zkserver无法及时获取zkclient的关闭状态
             * 所以zkserver会等待session timeout时间以后，会把zkclient创建的临时节点全部删除
             * 若果在session timeout时间内，有启动了同样的zkclient，那么等待session timeoyt时间超时以后
             * 原先创建的临时节点都没了
             * @param path
             * @throws Exception
             */
            @Override
            public void handleDataDeleted(String path) throws Exception {
                System.out.println("watcher -> handleDataDeleted: " + path);

                //把删除的znode临时性节点重新创建一下
                String str = ephemeralMap.get(path);
                if (str != null) {
                    zkClient.createEphemeral(path,str);
                }

            }
        });
    }

    /**
     * 读取节点
     * @return
     */
    public String read(String path) {
        return this.zkClient.readData(rootPath + path,null);
    }

    /**
     * 删除节点
     * @return
     */
    public boolean delete(String path) {
        return this.zkClient.delete(rootPath + path);
    }

    /**
     * 更改节点
     * @return
     */
    public void writeData(String path,String data) {
        this.zkClient.writeData(rootPath + path, data);
    }

    public static String getRootPath() {
        return rootPath;
    }

    public static void setRootPath(String rootPath) {
        ZkClientUtils.rootPath = rootPath;
    }


    /**
     * 测试
     * @param args
     */
//    public static void main(String[] args) {
//        ZkClientUtils zkClientUtils = new ZkClientUtils("127.0.0.1:2181");
//
//        //zkClientUtils.createPersistent("/ProductService","123456");
//
//        zkClientUtils.writeData("/ProductService","cx2");
//
//        System.out.println(zkClientUtils.read("/ProductService"));
//
//        zkClientUtils.close();
//
//    }
}
