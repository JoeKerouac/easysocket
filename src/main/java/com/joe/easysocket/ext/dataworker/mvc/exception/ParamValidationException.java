package com.joe.easysocket.ext.dataworker.mvc.exception;


import com.joe.easysocket.exception.SystemException;

/**
 * 数据校验异常
 * 
 * @author joe
 *
 */
public class ParamValidationException extends SystemException {
	private static final long serialVersionUID = 8169673786131228822L;

	/**
	 * 校验异常
	 * 
	 * @param param
	 *            异常的参数名
	 * @param message
	 *            异常原因
	 */
	public ParamValidationException(String param, String message) {
		super(String.format("参数%s校验异常，异常原因：%s", param , message));
	}
}
