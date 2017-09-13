package com.joe.easysocket.server.ext.mvc.exception;


import com.joe.easysocket.server.exception.SystemException;

public class MediaTypeNoSupportException extends SystemException {
	private static final long serialVersionUID = -4053758179057526205L;
	public MediaTypeNoSupportException(String mediaType){
		super("不支持" + mediaType);
	}
}
