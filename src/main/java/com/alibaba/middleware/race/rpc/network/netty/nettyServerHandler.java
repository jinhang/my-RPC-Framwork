package com.alibaba.middleware.race.rpc.network.netty;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.middleware.race.rpc.api.impl.RpcProviderImpl;
import com.alibaba.middleware.race.rpc.context.RpcContext;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.serialization.JavaSerialization;		//之前用的FST的序列化，在进行getDo测试时无法通过，原因应该是包装在内部的对象应该没有被完全序列化

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class nettyServerHandler extends ChannelInboundHandlerAdapter {
	
	private final RpcProviderImpl provider;
	
	public nettyServerHandler(RpcProviderImpl provider) {
		super();
		this.provider = provider;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
			throws Exception {
		ByteBuf recvBuf = (ByteBuf)msg;
		byte[]  recvMsg = new byte[recvBuf.readableBytes()];
		recvBuf.readBytes(recvMsg);
		
		RpcRequest request = (RpcRequest)JavaSerialization.unserialize(recvMsg);
		if(request.getContext() != null){
			Iterator<Entry<String, Object>> entries = request.getContext().entrySet().iterator();
			while(entries.hasNext()){
				Map.Entry<String, Object> entry = entries.next();
				RpcContext.addProp(entry.getKey(), entry.getValue());
			}
		}
		
		RpcResponse response = this.provider.requestHandler(request);
		byte[] 		sendMsg	 = JavaSerialization.serialize(response);	//序列化操作
		ByteBuf		sendBuf  = Unpooled.copiedBuffer(sendMsg);			//放入到发送缓冲区当中
		ctx.write(sendBuf);										
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();	//打印异常栈
		ctx.close();
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("接收到一个连接");
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();				//将缓冲区的信息发送出去
	}
}
