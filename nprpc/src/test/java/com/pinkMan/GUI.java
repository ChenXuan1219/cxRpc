package com.pinkMan;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/07/27 23:05
 */

/**
 * 事件回掉机制
 */
public class GUI implements NotifyCallBack{
    private DownLoad downLoad;

    public GUI() {
        this.downLoad = new DownLoad(this);
    }

    /**
     * 下载文件
     */
    public void progress(String file,int progress){
        System.out.println("down file:" + file + " progress: " + progress + "%");
    }
    public void result(String file){
        System.out.println("down file: "+ file + "over.");
    }

    private void downLoad(String file) {
        System.out.println("begin start download file: " + file);
        downLoad.start(file);
    }

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.downLoad("我要学Java");
    }



}

interface NotifyCallBack{
    void progress(String file,int progress);
    void result(String file);
}

class DownLoad{
    /**
     * 底层执行下载任务的方法
     *
     */

    private NotifyCallBack callBack;

    public DownLoad(NotifyCallBack callBack){
        this.callBack = callBack;
    }

    public void start(String file) {
        int count = 0;

        try {
            while (count <= 100) {
                callBack.progress(file, count);
                Thread.sleep(100);
                count = count + 20;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        callBack.result(file);
    }



}
