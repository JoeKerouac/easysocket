package com.joe.easysocket.ext.dataworker.mvc.resource;

import com.joe.easysocket.ext.dataworker.mvc.resource.annotation.Path;
import com.joe.scan.MethodFilter;

import java.lang.reflect.Method;


/**
 * resource方法过滤器
 *
 * @author joe
 */
public class ResourceMethodFilter implements MethodFilter {
    public boolean filter(Method method) {
        return method.isAnnotationPresent(Path.class);
    }
}
