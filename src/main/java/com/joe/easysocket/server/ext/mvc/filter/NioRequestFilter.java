package com.joe.easysocket.server.ext.mvc.filter;


import com.joe.easysocket.server.ext.mvc.context.RequestContext;
import com.joe.easysocket.server.ext.mvc.context.ResponseContext;

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
