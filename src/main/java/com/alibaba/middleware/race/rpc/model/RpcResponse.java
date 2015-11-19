/**
 * 
 */
package com.alibaba.middleware.race.rpc.model;

import java.io.Serializable;

/**
 * @author yuyang
 *
 */
public class RpcResponse implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -225640454370459555L;
	
    private String errorMsg;			//这个用来返回异常信息

    private Object appResponse;			//这个用来返回结果
    
    //一切响应消息都通过调用下面的工厂方法创建
    
    public synchronized static RpcResponse factory(long requestId,Object appResponse,String errorMsg){		//这个方法需要一定的线程安全性
    	return new RpcResponse(requestId,appResponse,errorMsg);
    }
    
    private RpcResponse(long requestId,Object appResponse, String errorMsg){
    	this.requestId 		= requestId;			
    	this.appResponse 	= appResponse;
    	this.errorMsg 		= errorMsg;
    }
    
    public Object getAppResponse() {
        return appResponse;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public boolean isError(){
        return errorMsg == null ? false:true;
    }
    
    private long requestId;			//增加一个请求Id,用以区别请求
    
    public long getRequestId(){
    	return requestId;
    }

	/**
	 * @param errorMsg the errorMsg to set
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * @param appResponse the appResponse to set
	 */
	public void setAppResponse(Object appResponse) {
		this.appResponse = appResponse;
	}

	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(long requestId) {
		this.requestId = requestId;
	}
}
