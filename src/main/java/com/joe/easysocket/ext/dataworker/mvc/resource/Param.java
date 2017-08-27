package com.joe.easysocket.ext.dataworker.mvc.resource;

import com.joe.type.JavaType;
import lombok.Data;

/**
 * API参数
 * 
 * @author joe
 *
 * @param <T>
 */
@Data
public class Param<T> {
	/*
	 * 参数名
	 */
	private String name;
	/*
	 * 参数类型
	 */
	private JavaType type;
}