package com.joe.easysocket.ext.dataworker.mvc.exception;


import com.joe.easysocket.exception.SystemException;

import java.lang.reflect.Parameter;

/**
 * 资源方法参数未命名，当资源方法有超过一个参数时必须命名
 * 
 * @author joe
 *
 */
public class ParamterNoNamingException extends SystemException {
	private static final long serialVersionUID = -4906144883508163062L;

	public ParamterNoNamingException(Parameter parameter) {
		super(parameter + "没有命名");
	}
}
