/**
 * 
 */
package com.alibaba.middleware.race.rpc.conf;

/**
 * @author yuyang
 *	配置文件
 */
public class configure {
	private static configure instance = null;
	
	public static synchronized configure get(){
		if(null == instance){
			instance = new configure();
		}
		return instance;
	}
	
	public static synchronized void clear(){
		instance = null;		//清空配置
	}
	
	//以下就是参数配置
				
	public final int 	port;			//Provider的服务端口号
	public final String SIP;			//Provider的IP地址
	
	private configure(){
		//获取环境参数
		this.SIP 	= System.getProperty("SIP", "127.0.0.1");	//SIP通过-D参数传入JVM中
		this.port 	= 8888;										//provider的服务端口默认为8888
	}
}
