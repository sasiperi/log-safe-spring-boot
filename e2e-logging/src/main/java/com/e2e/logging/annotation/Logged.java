package com.e2e.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.logging.LogLevel;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Logged 
{
	public enum MethodComponent
	{
		ARGUMENTS,
		RETURN_VALUE,
		EXCEPTION
	}
	
	MethodComponent[] methodComponents() default { MethodComponent.ARGUMENTS, MethodComponent.RETURN_VALUE, MethodComponent.EXCEPTION };	
	LogLevel argumentsLogLevel() default LogLevel.DEBUG;	
	LogLevel returnValueLogLevel() default LogLevel.DEBUG;	
	LogLevel exceptionLogLevel() default LogLevel.ERROR;
}