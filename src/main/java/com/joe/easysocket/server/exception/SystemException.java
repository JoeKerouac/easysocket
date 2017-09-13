package com.joe.easysocket.server.exception;

/**
 * 系统异常
 * 
 * @author joe
 *
 */
public class SystemException extends RuntimeException {
	private static final long serialVersionUID = 8644970275108500846L;

	public SystemException() {
		super();
	}

	public SystemException(String message) {
		super(message);
	}

	public SystemException(String message, Throwable cause) {
		super(message, cause);
	}

	public SystemException(Throwable cause) {
		super(cause);
	}
}
