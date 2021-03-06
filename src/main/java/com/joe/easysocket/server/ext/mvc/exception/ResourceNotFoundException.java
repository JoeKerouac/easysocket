package com.joe.easysocket.server.ext.mvc.exception;


import com.joe.easysocket.server.exception.SystemException;

/**
 * 找不到请求的路径
 * @author joe
 *
 */
public class ResourceNotFoundException extends SystemException {
	private static final long serialVersionUID = -8274552015420114367L;
	public ResourceNotFoundException(String uri){
		super("没有找到指定资源:" + uri);
	}
}
