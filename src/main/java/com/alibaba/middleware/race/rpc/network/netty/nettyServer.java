package com.alibaba.middleware.race.rpc.network.netty;

import java.util.concurrent.CancellationException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import com.alibaba.middleware.race.rpc.api.impl.RpcProviderImpl;
import com.alibaba.middleware.race.rpc.network.RpcServer;

public class nettyServer extends RpcServer {

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	private ServerBootstrap serverBootstrap;
	private ChannelFuture serverChannel = null;

	public nettyServer(RpcProviderImpl provider, int port) {
		super(provider, port);
		this.bossGroup = new NioEventLoopGroup();
		this.workerGroup = new NioEventLoopGroup();
		this.serverBootstrap = new ServerBootstrap();
		this.serverBootstrap.group(this.bossGroup, this.workerGroup)
				.channel(NioServerSocketChannel.class)
				.option(ChannelOption.SO_BACKLOG, 1024)
				.childHandler(new serverInitializerHandler(super.provider));
	}

	public void run() {
		try {
			this.start();
		} catch (Throwable e) {
			e.printStackTrace();
		}finally{
			this.bossGroup.shutdownGracefully();
			this.workerGroup.shutdownGracefully();
		}
	}

	@Override
	public void start() throws Throwable {
		this.serverChannel = this.serverBootstrap.bind(super.port).sync(); 	// 绑定端口
		System.out.println("开启监听");
		this.serverChannel.channel().closeFuture().sync(); 					// 等待关闭
		System.out.println("结束监听");
	}

	@Override
	public void stop() throws Throwable {
		if (null != this.serverChannel
				&& (this.serverChannel.channel().isOpen() || !this.serverChannel
						.isCancelled())) {
			try {
				this.serverChannel.channel().disconnect();
				this.serverChannel.channel().close();
				this.serverChannel.cancel(true);
			} catch (CancellationException e) {
				throw new Throwable("Stop server occur a exception!");
			}
		}
	}

	class serverInitializerHandler extends ChannelInitializer<SocketChannel> {
		private final RpcProviderImpl provider;

		public serverInitializerHandler(RpcProviderImpl provider) {
			super();
			this.provider = provider;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast(new nettyServerHandler(this.provider));
		}
	}
}
