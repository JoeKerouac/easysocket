package com.joe.easysocket.server.ext.mvc.coder;

import com.joe.easysocket.server.ext.mvc.BeanContainer;
import com.joe.easysocket.server.ext.mvc.container.AbstractSpringContainer;

/**
 * 响应数据处理器容器
 *
 * @author joe
 */
public class DataWriterContainer extends AbstractSpringContainer<WriterInterceptor> {
    public DataWriterContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
