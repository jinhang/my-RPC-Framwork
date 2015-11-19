package com.alibaba.middleware.race.rpc.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JavaSerialization {
	public static byte[] serialize(Object obj){
		try{
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(outStream);
			os.writeObject(obj);
			return outStream.toByteArray();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Object unserialize(byte[] bytes){
		try{
			ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
			ObjectInputStream	 is 		 = new ObjectInputStream(inputStream);
			return is.readObject();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
};