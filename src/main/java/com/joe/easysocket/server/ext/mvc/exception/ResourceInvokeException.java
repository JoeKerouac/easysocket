package com.joe.easysocket.server.ext.mvc.exception;


import com.joe.easysocket.server.exception.SystemException;

/**
 * 资源调用异常
 * @author joe
 *
 */
public class ResourceInvokeException extends SystemException {
	private static final long serialVersionUID = -8802366810043428339L;

	public ResourceInvokeException(Throwable cause) {
		super(cause);
	}
}
