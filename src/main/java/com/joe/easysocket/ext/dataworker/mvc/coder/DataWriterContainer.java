package com.joe.easysocket.ext.dataworker.mvc.coder;

import com.joe.easysocket.ext.dataworker.mvc.BeanContainer;
import com.joe.easysocket.ext.dataworker.mvc.container.AbstractSpringContainer;

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
