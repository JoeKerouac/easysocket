package com.joe.easysocket.ext.dataworker.mvc.exception;


import com.joe.easysocket.exception.SystemException;

public class MediaTypeNoSupportException extends SystemException {
	private static final long serialVersionUID = -4053758179057526205L;
	public MediaTypeNoSupportException(String mediaType){
		super("不支持" + mediaType);
	}
}
