package com.joe.easysocket.server.ext.mvc.exceptionmapper;

import com.joe.easysocket.server.ext.mvc.Bean;
import com.joe.easysocket.server.ext.mvc.context.ResponseContext;

/**
 * 异常处理器
 * 
 * @author joe
 *
 */
public interface ExceptionMapper extends Bean {
	/**
	 * 该处理器是否能处理
	 * 
	 * @param e
	 *            异常原因
	 * @return
	 *         <li>true：能处理</li>
	 *         <li>false：不能处理</li>
	 */
	boolean mapper(Throwable e);

	/**
	 * 请求中发生异常时会回调此方法
	 * 
	 * @param e
	 *            请求中的异常
	 * @return 异常处理后的响应
	 */
	ResponseContext.Response toResponse(Throwable e);
}
