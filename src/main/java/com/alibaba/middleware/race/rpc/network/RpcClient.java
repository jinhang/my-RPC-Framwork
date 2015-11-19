/**
 * 
 */
package com.alibaba.middleware.race.rpc.network;

import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;

/**
 * @author yuyang
 *
 */
public interface RpcClient {
	public  boolean     Connect(String sip, int port) 	throws Throwable;		//连接服务器
	public  boolean		sendRequest(RpcRequest request) throws Throwable;		//发送请求
	public  RpcResponse getResponse()					throws Throwable;		//获取回应
	public  boolean 	close()							throws Throwable;		//关闭连接
	public  boolean 	isConnected();											//是否已经连接
}