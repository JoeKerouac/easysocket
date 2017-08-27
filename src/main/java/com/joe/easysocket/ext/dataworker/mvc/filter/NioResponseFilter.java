package com.joe.easysocket.ext.dataworker.mvc.filter;


import com.joe.easysocket.ext.dataworker.mvc.context.RequestContext;
import com.joe.easysocket.ext.dataworker.mvc.context.ResponseContext;

/**
 * 响应filter
 * 
 * @author joe
 *
 */
public abstract class NioResponseFilter implements NioFilter {

	@Override
	public final void requestFilter(RequestContext.RequestWrapper request) {
		//加上final防止子类继承
	}

	@Override
	public abstract void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response);
}
