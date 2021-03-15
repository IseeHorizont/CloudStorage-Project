package ru.geekbrains;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainServer {
    private ConcurrentHashMap<ChannelHandlerContext, String> clients;
    private BaseAuthService authService;
    public static final Logger logger = Logger.getLogger("");

    public MainServer(){
        clients = new ConcurrentHashMap<>();
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                                channel.pipeline().addLast(
                                        new ObjectEncoder(),
                                        new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                        new AuthHandler(MainServer.this)
                                );
                        }
            });
            ChannelFuture future = bootstrap.bind(8189).sync();
            logger.log(Level.INFO, "Server started on port 8189");
            authService = new BaseAuthService();
            future.channel().closeFuture().sync();
        }
        catch (InterruptedException ex){
            logger.log(Level.INFO, "Server was broken!");
        }
        finally {
            authService.disconnect();
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new MainServer();
    }

    public ConcurrentHashMap<ChannelHandlerContext, String> getClients(){
        return clients;
    }

    public BaseAuthService getAuthService() {
        return authService;
    }
}
