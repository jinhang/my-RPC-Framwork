/**
 * 
 */
package com.alibaba.middleware.race.rpc.model;

/**
 * @author yuyang
 *
 */
public class utils {
	private static volatile Long requestId = 0L;		//请求值从0开始
	
	public synchronized static long generateRequestId(){	//这里必须要保证线程安全
		return (requestId = ((requestId+1)% Long.MAX_VALUE)).longValue();									
	}
}
