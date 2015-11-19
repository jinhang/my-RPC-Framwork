package com.alibaba.middleware.race.rpc.model;

import java.io.Serializable;
import java.util.Map;

import com.alibaba.middleware.race.rpc.context.RpcContext;

public class RpcRequest implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3050395738507191119L;
	
	private long 		requestId;		//请求Id
	private String 		className;		//服务的名字
	private String 		methodName;		//请求调用的方法名字
	private String[] 	parameterTypes;	//参数类型
	private Object[] 	args;			//参数
	private Map<String,Object>	context = null;//RPC context
	
	
	//所有的请求消息都通过下面的工厂方法创建得到,这个方法需要一定的线程安全性
	public static synchronized RpcRequest factory(long requestId,String className, String methodName, String[] parameterTypes,Object[] args){
		return new RpcRequest(requestId,className,methodName,parameterTypes,args);
	}
	
	private RpcRequest(long requestId, String className,String methodName,String[] parameterTypes,Object[] args){
		this.className 		= className;
		this.methodName	 	= methodName;
		this.parameterTypes = parameterTypes;
		this.args 			= args;
	}
	
	public long getRequestId(){				//获取请求Id
		return this.requestId;
	}
	
	public String getClassName(){			//获取该服务的类名
		return this.className;
	}
	
	/**
	 * @return the context
	 */
	public Map<String, Object> getContext() {
		return context;
	}

	/**
	 * @param context the context to set
	 */
	public void setContext(Map<String, Object> context) {
		this.context = context;
	}

	public String getMethodName(){			//获取调用方法的名字
		return this.methodName;
	}
	
	public String[] getParameterTypes(){	//获取参数类型
		return this.parameterTypes;
	}
	
	public Object[] getArgs(){				//获取参数	
		return this.args;
	}

	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}

	/**
	 * @param className the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @param methodName the methodName to set
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/**
	 * @param parameterTypes the parameterTypes to set
	 */
	public void setParameterTypes(String[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	/**
	 * @param args the args to set
	 */
	public void setArgs(Object[] args) {
		this.args = args;
	}
}