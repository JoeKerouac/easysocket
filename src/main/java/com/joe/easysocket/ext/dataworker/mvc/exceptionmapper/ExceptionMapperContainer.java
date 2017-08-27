package com.joe.easysocket.ext.dataworker.mvc.exceptionmapper;

import com.joe.easysocket.ext.dataworker.mvc.BeanContainer;
import com.joe.easysocket.ext.dataworker.mvc.container.AbstractSpringContainer;

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
