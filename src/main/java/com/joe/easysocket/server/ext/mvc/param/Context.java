package com.joe.easysocket.server.ext.mvc.param;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 通过该注解注入request
 * 
 * @author joe
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface Context {

}
