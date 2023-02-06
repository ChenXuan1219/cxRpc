package com.pinkMan.provider;

import com.pinkMan.RpcMetaProto;
import com.pinkMan.callback.INotifyProvider;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

/**
 * TODO
 *
 * @author chenxuan
 * @version 1.0.0
 * @since 2022/08/01 23:30
 */
public class RpcServer {

    private INotifyProvider iNotifyProvider;

    public RpcServer(INotifyProvider iNotifyProvider){
        this.iNotifyProvider = iNotifyProvider;
    }

    public void start(String ip,int port){
        //创建主事件循环，对应io线程，主要用来出来新用户的连接事件
        EventLoopGroup mainGroup = new NioEventLoopGroup(1);

        //创建work工作线程事件循环，主要用来处理已连接用户的可读写事件
        EventLoopGroup workerGroup = new NioEventLoopGroup(3);

        //netty网络服务的启动辅助类
        ServerBootstrap bootstrap = new ServerBootstrap();

        bootstrap.group(mainGroup,workerGroup)
                .channel(NioServerSocketChannel.class) //底层使用Nio Selector模型
                .option(ChannelOption.SO_BACKLOG,1024) //设置Tcp参数
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        /**
                         * 1 设置数据的编码器和解码器，  网络的字节流 和 业务要处理的数据类型 进行序列化和反序列化
                         * 2 设置具体的处理器回调
                         */
                        socketChannel.pipeline().addLast(new ObjectEncoder()); //编码

                        socketChannel.pipeline().addLast(new RpcServerChannel()); //设置事件回调处理器
                    }
                });//注册事件回掉，把业务层代码和网络层的代码完全区分开

        try {
            //阻塞，开启网络服务
            ChannelFuture sync = bootstrap.bind(ip, port).sync();

            //关闭网络服务
            sync.channel().closeFuture().sync();

        } catch (InterruptedException e) {

            e.printStackTrace();

        } finally {

            workerGroup.shutdownGracefully();

        }
    }

    /**
     * 继承netty的ChannelInboundHandlerAdapter适配器类，提供相应的回调操作
     */
    private class RpcServerChannel extends ChannelInboundHandlerAdapter{
        /**
         * 处理接收到的事件
         *
         * request 就是远端发送过来的rpc调用请求包含的所有信息参数
         *
         * 20 + UserServiceRpcLogin + 参数
         *
         * @param ctx
         * @param msg
         * @throws Exception
         */
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            ByteBuf request = (ByteBuf) msg;

            //1 先读头部信息的长度
            int header_size = request.readInt(); //读取request的前4个字节

            //2 读取头部信息,包含服务对象名称和服务方法名称
            byte[] metaBuf = new byte[header_size];
            request.readBytes(metaBuf);

            //3 反序列化生成RpcMeta
            RpcMetaProto.RpcMeta rpcMeta = RpcMetaProto.RpcMeta.parseFrom(metaBuf);
            String serviceName = rpcMeta.getServiceName();
            String methodName = rpcMeta.getMethodName();

            //4 剩下的就是参数
            byte[] argBuf = new byte[request.readableBytes()];
            request.readBytes(argBuf);

            //5
            byte[] response = iNotifyProvider.notify(serviceName, methodName, argBuf);

            //6 把rpc方法响应的response通过网络发送给rpc调用方
            ByteBuf buf = Unpooled.buffer(response.length);
            buf.writeBytes(response);
            ChannelFuture channelFuture = ctx.writeAndFlush(buf);

            //7 模拟http响应完成后，直接关闭连接
            if (channelFuture.sync().isSuccess()) {
                ctx.close();
            }

        }

        /**
         * 连接异常处理
         * @param ctx
         * @param cause
         * @throws Exception
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        }
    }

}
