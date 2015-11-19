package com.alibaba.middleware.race.rpc.network.netty;

import com.alibaba.middleware.race.rpc.context.RpcContext;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.network.RpcClient;
import com.alibaba.middleware.race.rpc.serialization.JavaSerialization;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class nettyClient extends ChannelInboundHandlerAdapter implements RpcClient {

	private volatile Channel		clientChannel;
	private EventLoopGroup 			workerGroup;
	private Bootstrap 				clientBootstrap;
	private final int 				timeout;
	private volatile RpcResponse    response;
	private volatile boolean 		isInit;
	/**
	 * 
	 */
	public nettyClient(int timeout){
		this.workerGroup 		= new NioEventLoopGroup();
		this.clientBootstrap 	= new Bootstrap();
		this.clientChannel 		= null;
		this.timeout       		= timeout;
		this.response	   		= null;
		this.isInit 	  		= false;
	}

	protected boolean init() {					//完成初始化的方法
		if (this.isInit) {
			return true;
		} else {
			if (this.workerGroup == null || this.clientBootstrap == null) {
				this.isInit = false;
			} else {
				this.clientBootstrap.group(this.workerGroup).channel(NioSocketChannel.class)
						.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.timeout).handler(new InitializerHandler(this));
				this.isInit = true;
			}
		}
		return this.isInit;
	}

	public boolean Connect(String sip, int port) throws Throwable {
		boolean isConnect = false;					
		if(!this.isInit){						//如果没有初始化，则先初始化
			this.isInit = this.init();
		}
		if(this.isInit){
			this.clientBootstrap.connect(sip, port).sync();
			isConnect = true;
		}
		else{
			isConnect = false;
		}
		return isConnect;
	}

	public boolean sendRequest(RpcRequest request) throws Throwable {
		boolean isSend 	= false;									
		this.response   = null;
		long startTime  = System.currentTimeMillis();
		while(this.clientChannel == null){
			if(System.currentTimeMillis() - startTime >= this.timeout){
				System.out.println("Timeout Exception");
				throw new RuntimeException("Timeout Exception");
			}
		}
		if(this.clientChannel.isActive()){									//检查连接是否已经建立
			request.setContext(RpcContext.getProps()); 		//放入Context
			byte[] sendMsg = JavaSerialization.serialize(request);			
			ByteBuf buf    = Unpooled.copiedBuffer(sendMsg.clone());							
			this.clientChannel.writeAndFlush(buf);							//发送调用请求
			isSend = true;
		}
		else{
			isSend = false;
		}
		return isSend;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		this.clientChannel = ctx.channel(); 								// 把context的channel保存下来留用
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);											//当channel断开连接的时候触发这个方法
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("收到回应");
		ByteBuf recvBuf = (ByteBuf)msg;
		byte[]  recvMsg = new byte[recvBuf.readableBytes()];
		recvBuf.readBytes(recvMsg);
		this.response = (RpcResponse)JavaSerialization.unserialize(recvMsg);
		synchronized(this.clientChannel){
			this.clientChannel.notifyAll();						//通知等待线程结果已经返回
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		this.response.setErrorMsg(cause.getMessage());			//把异常信息放到响应消息里面
		ctx.close();											//关闭这个channel
	}

	public boolean close() throws Throwable {
		if(this.clientChannel != null && this.clientChannel.isActive()){
			this.clientChannel.disconnect();
			this.clientChannel.close();
			synchronized(this.clientChannel){
				this.clientChannel.notifyAll();
			}
		}
		this.workerGroup.shutdownGracefully();
		return true;
	}

	public boolean isConnected() {
		return this.clientChannel.isActive();					//返回是否连接的
	}

	class InitializerHandler extends ChannelInitializer<SocketChannel> {
		private ChannelInboundHandlerAdapter handler;
		
		/**
		 * @param handler
		 */
		public InitializerHandler(ChannelInboundHandlerAdapter handler) {
			this.handler = handler;
		}

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast(this.handler);
		}
	}

	public RpcResponse getResponse() throws Throwable {
		synchronized(this.clientChannel){
			long startTime = System.currentTimeMillis();
			this.clientChannel.wait(this.timeout);
			if(System.currentTimeMillis() - startTime > this.timeout){
				throw new RuntimeException("Time out exception");
			}
		}
		return this.response;
	}
}
