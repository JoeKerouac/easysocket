package com.joe.easysocket.ext.dataworker.mvc.filter;


import com.joe.easysocket.ext.dataworker.mvc.context.RequestContext;
import com.joe.easysocket.ext.dataworker.mvc.context.ResponseContext;

/**
 * 请求filter
 *
 * @author joe
 */
public abstract class NioRequestFilter implements NioFilter {

    @Override
    public abstract void requestFilter(RequestContext.RequestWrapper request);

    @Override
    public final void responseFilter(RequestContext.RequestWrapper request, ResponseContext.Response response) {
        //加上final防止子类继承
    }

}
