package com.joe.easysocket.server.ext.mvc.resource.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 定义响应数据的处理方式
 * @author joe
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ METHOD })
public @interface Produces {
	/**
	 * 响应数据的处理方式，默认json
	 * @return
	 */
	String value() default "json";
}
