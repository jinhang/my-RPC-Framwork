/**
 * 
 */
package com.alibaba.middleware.race.rpc.api.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import com.alibaba.middleware.race.rpc.aop.ConsumerHook;
import com.alibaba.middleware.race.rpc.api.RpcConsumer;
import com.alibaba.middleware.race.rpc.async.ResponseCallbackListener;
import com.alibaba.middleware.race.rpc.async.ResponseFuture;
import com.alibaba.middleware.race.rpc.conf.configure;
import com.alibaba.middleware.race.rpc.model.RpcRequest;
import com.alibaba.middleware.race.rpc.model.RpcResponse;
import com.alibaba.middleware.race.rpc.model.utils;
import com.alibaba.middleware.race.rpc.network.RpcClient;
import com.alibaba.middleware.race.rpc.network.netty.nettyClient;
/**
 * @author yuyang
 *
 */
public class RpcConsumerImpl extends RpcConsumer {
	private String 									version; 														// 服务的版本
	private int 									timeout; 														// 客户端的超时时间
	private ConcurrentMap<String, Method> 			serviceList = new ConcurrentHashMap<String, Method>();  		// 服务列表
	private ConsumerHook hook 									= null;
	private ConcurrentMap<String, Future<Object>> 	taskList 	= new ConcurrentHashMap<String, Future<Object>>(); 	// 当前计算的任务列表	
	@Override
	public RpcConsumer interfaceClass(Class<?> interfaceClass) {													//服务列表
		if (null == interfaceClass) {
			throw new IllegalArgumentException("interfaceClass is null!");
		}
		if (!interfaceClass.isInterface()) {
			throw new IllegalArgumentException("The" + interfaceClass.getName() + "is not a interface!");
		}
		Method[] methodList = interfaceClass.getMethods();
		// 将serviceList方法放到服务列表
		for (Method method : methodList) {
			serviceList.put(method.getName(), method);
			System.out.println(method.getName());
		}
		return super.interfaceClass(interfaceClass);
	}

	@Override
	public RpcConsumer version(String version) {
		this.version = version;
		return super.version(version);
	}
	
	@Override
	public RpcConsumer clientTimeout(int clientTimeout) {
		this.timeout = clientTimeout;
		return super.clientTimeout(clientTimeout);
	}

	@Override
	public RpcConsumer hook(ConsumerHook hook) {
		this.hook = hook;
		return super.hook(hook);
	}
	
	@Override
	public Object instance() {
		return super.instance();
	}

	// 一个是异步调用；Callback()
	// 处理超时情景 future

	@Override
	public void asynCall(String methodName) {
		asynCall(methodName,null);
	}  

	@Override
	public <T extends ResponseCallbackListener> void asynCall(String methodName, T callbackListener) {
		ExecutorService executor = Executors.newCachedThreadPool();
		Future<Object>  task   = executor.submit(new CallMethod(this,serviceList.get(methodName),null));
		executor.shutdown();							//关闭线程池，不再接受新的计算任务
		new ResponseFuture().setFuture(task);
		this.taskList.put(methodName, task);
		if(null != callbackListener){			
			try{
				callbackListener.onResponse(ResponseFuture.getResponse(this.timeout));
			}catch(InterruptedException e){
				if(e.getMessage().contains("Time out")){
					callbackListener.onTimeout();
				}
				else{
					callbackListener.onException(e);
				}
			}
		}
	}
	@Override
	public void cancelAsyn(String methodName) {
		if (this.taskList.containsKey(methodName)) { 					// 如果当前运行的任务当中有methodName这个服务
			Future<Object> cancelTask = this.taskList.get(methodName);	// 取得这个Task
			if (!cancelTask.isCancelled()) { 							// 判断是否已经被取消
				cancelTask.cancel(true);
			}
			this.taskList.remove(methodName);
		}
		super.cancelAsyn(methodName);
	}

	void removeTask(String methodName) { 								// 将methodName指出的方法从当前执行的任务列表当中移除
		if (this.taskList.containsKey(methodName)) {
			this.taskList.remove(methodName);
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		RpcResponse response = RpcResponse.factory(-1, null, null);
		if (!this.serviceList.containsKey(method.getName())) {
			response.setErrorMsg("The method:" + method.getName() + " is not existing!");
			return response;
		}
		if(this.taskList.containsKey(method.getName())){
			return null;
		}
		
		String className = method.getDeclaringClass().getName(); 	// 返回声明了该方法的类或者接口的class对象的名字
		String methodName = method.getName();
		List<String> parameterTypes = new ArrayList<String>(); 		// 参数的类型
		for (Class<?> parameterType : method.getParameterTypes()) {
			parameterTypes.add(parameterType.getName());	  		// 存放参数类型
		}
		
		RpcRequest request = RpcRequest.factory(utils.generateRequestId(),className, methodName, parameterTypes.toArray(new String[0]), args);
		this.hook.before(request); 									// 调用之前,先用hook处理一下
		RpcClient client = new nettyClient(this.timeout);		
		try{
			client.Connect(configure.get().SIP, configure.get().port);
			client.sendRequest(request);
			response = client.getResponse();			 			//这个会阻塞timeout的时间
			if(response.isError()){
				System.out.println(response.getErrorMsg());
				throw new RuntimeException(response.getErrorMsg());
			}
			this.hook.after(request);								//调用结束以后的hook								
			return response.getAppResponse();
		}
		finally{
			client.close();											//无论如何要主动保证连接关闭
		}
	}

	class CallMethod implements Callable<Object> {
		/**
		 * 负责调用Method的任务类
		 * 
		 * @param method
		 * @param args
		 */
		public CallMethod(RpcConsumerImpl consumer, Method method, Object[] args) {
			super();
			this.consumer 	= consumer;
			this.method 	= method;
			this.args 		= args;
		}

		private RpcConsumerImpl consumer;
		private Method 			method;
		private Object[] 		args;
		
		public Object call() throws Exception {
			RpcResponse response = RpcResponse.factory(-2, null, null);
			try {
				RpcRequest request = RpcRequest.factory(utils.generateRequestId(),this.consumer.getClass().getName(), this.method.getName(), null, this.args);
				this.consumer.hook.before(request); 						// 调用之前,先用hook处理一下
				
				RpcClient client = new nettyClient(this.consumer.timeout);		
				try{
					client.Connect(configure.get().SIP, configure.get().port);
					client.sendRequest(request);
					response = client.getResponse();			 			//这个会阻塞timeout的时间
					if(response.isError()){
						System.out.println(response.getErrorMsg());
						throw new RuntimeException(response.getErrorMsg());
					}
				}
				finally{
					client.close();											//无论如何要主动保证连接关闭
				}
			}
			catch(Throwable e){
				response.setErrorMsg(e.getMessage());
			}
			return response;
		}

	}
}