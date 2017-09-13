package com.joe.easysocket.server.ext.mvc.exception;

import com.joe.easysocket.server.exception.SystemException;

/**
 * filter异常
 * 
 * @author joe
 *
 */
public class FilterException extends SystemException {
	private static final long serialVersionUID = -5810990794143921258L;

	public FilterException(Throwable cause) {
		super(cause);
	}
}
