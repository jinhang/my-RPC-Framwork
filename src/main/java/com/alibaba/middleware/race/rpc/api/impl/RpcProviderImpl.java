/**
 * 
 */
package com.alibaba.middleware.race.rpc.api.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.middleware.race.rpc.api.RpcProvider;
import com.alibaba.middleware.race.rpc.conf.configure;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.network.RpcServer;
import com.alibaba.middleware.race.rpc.network.netty.nettyServer;

/**
 * @author yuyang
 *
 */
public class RpcProviderImpl extends RpcProvider{
	private String 				version;			//设置服务版本号
	private int 				timeout;			//设置超时时间
	private ConcurrentMap<String,Method>	serviceList = new ConcurrentHashMap<String,Method>();
	private Object 				serviceInstance = null;
	private Class<?>			serviceType = null;
	private volatile boolean 	start = false;
	private RpcServer			server = null;
	
	@Override
	public RpcProvider serviceInterface(Class<?> serviceInterface) {
		if(null == serviceInterface){ 
			throw new IllegalArgumentException("Service Interface is null !");
		}
		
		if(!serviceInterface.isInterface()){
			throw new IllegalArgumentException("Service Interface :" + serviceInterface.getName() + " is not a interface!");
		}
		Method[] methods = serviceInterface.getMethods();
		for(Method method:methods){
			serviceList.put(method.getName(),method);		//Service List
			System.out.println(method.getName());				
		}
		this.serviceType = serviceInterface;
		return super.serviceInterface(serviceInterface);
	}

	@Override
	public RpcProvider version(String version) {
		this.version = version;
		return super.version(version);
	}

	@Override
	public RpcProvider impl(Object serviceInstance) {
		if(null == serviceInstance){
			throw new IllegalArgumentException("serviceInstance is null object!");
		}
		if(!serviceType.isInstance(serviceInstance)){
			throw new IllegalArgumentException(serviceInstance.getClass().getName() + " do not implement the service: "  + serviceType.getName());
		}
		System.out.println("ok");
		this.serviceInstance = serviceInstance;
		return super.impl(serviceInstance);
	}

	@Override
	public RpcProvider timeout(int timeout) {
		this.timeout = timeout;
		return super.timeout(timeout);
	}

	@Override
	public RpcProvider serializeType(String serializeType) {	//这里是指明序列化类型的，我们采用的是fst的序列化工具
		return super.serializeType(serializeType);
	}	
	
	@Override
	public void publish() {
		try {
			this.server = new nettyServer(this,configure.get().port);
			Thread t = new Thread(this.server);			//开启服务线程
			t.start();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public synchronized RpcResponse requestHandler(RpcRequest request){
		System.out.println("a request call:"+ request.getMethodName());
		RpcResponse resp = RpcResponse.factory(request.getRequestId(),null, null);
		if(this.serviceList.containsKey(request.getMethodName())){
			//开始调用
			Method method = this.serviceList.get(request.getMethodName());				//获取方法
			try {
				Object result = method.invoke(this.serviceInstance, request.getArgs());	
				resp.setAppResponse(result);											//设置返回值
			} catch (IllegalAccessException e) {
				resp.setErrorMsg(e.getMessage());
			} catch (IllegalArgumentException e) {
				resp.setErrorMsg(e.getMessage());
			} catch (InvocationTargetException e) {
				System.out.println(e.getCause().getMessage());
				resp.setErrorMsg(e.getCause().getMessage());
			}
		}
		else{
			resp.setAppResponse(null);
			resp.setErrorMsg("Method: " + request.getMethodName() + " do not exists!");
		}
		return resp;
	}
}
