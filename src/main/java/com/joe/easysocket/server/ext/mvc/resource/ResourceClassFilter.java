package com.joe.easysocket.server.ext.mvc.resource;



import com.joe.easysocket.server.ext.mvc.resource.annotation.Path;
import com.joe.scan.ClassFilter;

/**
 * resource类过滤器
 * @author joe
 *
 */
public class ResourceClassFilter implements ClassFilter{

	public boolean filter(Class<?> clazz) {
		return clazz.isAnnotationPresent(Path.class);
	}
}
