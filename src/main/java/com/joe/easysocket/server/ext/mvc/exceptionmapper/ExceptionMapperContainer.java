package com.joe.easysocket.server.ext.mvc.exceptionmapper;

import com.joe.easysocket.server.ext.mvc.BeanContainer;
import com.joe.easysocket.server.ext.mvc.container.AbstractSpringContainer;

/**
 * 异常处理器容器
 * 
 * @author joe
 *
 */
public class ExceptionMapperContainer extends AbstractSpringContainer<ExceptionMapper> {
    public ExceptionMapperContainer(BeanContainer beanContainer) {
        super(beanContainer);
    }
}
