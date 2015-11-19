/**
 * 
 */
package com.alibaba.middleware.race.rpc.network;

import com.alibaba.middleware.race.rpc.api.impl.RpcProviderImpl;

/**
 * @author yuyang
 *
 */
public abstract class RpcServer implements Runnable{		
	
	 protected final RpcProviderImpl provider;
	 protected final int 			 port;
	/**
	 * @param provider
	 */
	public RpcServer(RpcProviderImpl provider,int port ) {
		super();
		this.provider = provider;
		this.port     = port;
	}
	public abstract void start() throws Throwable;				//启动Server
	public abstract void stop()  throws Throwable;				//停止Server
}
