/**
 * 
 */
package com.alibaba.middleware.race.rpc.serialization;

import org.nustaq.serialization.FSTConfiguration;


/**
 * @author jinhang
 *	序列化工具类
 */
public class FstSerialization {
	static FSTConfiguration configuration = FSTConfiguration
			.createStructConfiguration();

	public static byte[] serialize(Object obj)  {
		return configuration.asByteArray(obj);
	}

	public static Object unserialize(byte[] sec)  {
		return configuration.asObject(sec);
	}

}