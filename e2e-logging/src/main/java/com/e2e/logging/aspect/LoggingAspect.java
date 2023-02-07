package com.e2e.logging.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.stereotype.Component;

import com.e2e.logging.annotation.Logged;
import com.e2e.logging.annotation.Logged.MethodComponent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static java.lang.reflect.Modifier.TRANSIENT;

@Aspect
@Component
public class LoggingAspect 
{	
	//Setup the logger, defaulting it to the current class
	private static Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);
		
	//Determine what annotation will be used as the pointcut
	//
	
	private static final String POINTCUT = "@within(com.e2e.logging.annotation.Logged) || @annotation(com.e2e.logging.annotation.Logged)";
	
	private static final String POINTCUT_JPA = "execution(public !void org.springframework.data.repository.Repository+.*(..))";
	private static final String POINTCUT_DATAREST = "@within(org.springframework.data.rest.webmvc.RepositoryRestController) || @annotation(org.springframework.data.rest.webmvc.RepositoryRestController)";
	//private static final String pt = "execution(public !void org.springframework.data.repository.Repository+.*(..))";
	//Arguments
	@Before(POINTCUT)
	public void logBefore(JoinPoint joinPoint) throws Throwable 
	{	
		//Log the message
		log(joinPoint, MethodComponent.ARGUMENTS, joinPoint.getArgs());
	}
 	
	@Before(POINTCUT_JPA)
    public void logBeforeJPA(JoinPoint joinPoint) throws Throwable 
    {   
	    MethodSignature signature = (MethodSignature)joinPoint.getStaticPart().getSignature();
        Class<?> methodClass = joinPoint.getTarget().getClass();
        Method method = signature.getMethod();
        
        Object componentValue = joinPoint.getArgs();
        LOGGER = LoggerFactory.getLogger(methodClass);
        String loggedComponentText  = new Gson().toJson(componentValue);
        String logMessage = String.format("Method: %s(), %s: %s", method.getName(), MethodComponent.ARGUMENTS, loggedComponentText);
        
        LOGGER.warn(logMessage);
    }
	
	@Before(POINTCUT_DATAREST)
	public void logBeforeDataRest(JoinPoint joinPoint) throws Throwable 
	{
	    MethodSignature signature = (MethodSignature)joinPoint.getStaticPart().getSignature();
        Class<?> methodClass = joinPoint.getTarget().getClass();
        Method method = signature.getMethod();
        
        Object componentValue = joinPoint.getArgs();
        if (componentValue instanceof org.springframework.data.rest.webmvc.RootResourceInformation) 
        {
            componentValue = ((org.springframework.data.rest.webmvc.RootResourceInformation)componentValue).getPersistentEntity();
        }
        LOGGER = LoggerFactory.getLogger(methodClass);
        final Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .excludeFieldsWithModifiers(TRANSIENT) // STATIC|TRANSIENT in the default configuration
                .create();
        String loggedComponentText  = gson.toJson(componentValue);
        String logMessage = String.format("Method: %s(), %s: %s", method.getName(), MethodComponent.ARGUMENTS, loggedComponentText);
        
        LOGGER.warn(logMessage);
	    
	   /* Class<?> methodClass = joinPoint.getTarget().getClass();
	    LOGGER = LoggerFactory.getLogger(methodClass);
	    
	    String argumentsJson = "";
        
        if(joinPoint.getArgs() != null && joinPoint.getArgs().length != 0)
        {
            MethodSignature signature = (MethodSignature)joinPoint.getStaticPart().getSignature();
            Method method = signature.getMethod();
            
            //Start building argument array
            argumentsJson += "[";               
            
            //Loop over parameters
            int parameterIndex = 0;
            for(Parameter parameter : method.getParameters())
            {
                //Do not log PHI
                try
                {
                    argumentsJson += joinPoint.getArgs()[parameterIndex];
                    
                    //if(org.springframework.data.rest.webmvc.RootResourceInformation)
                }
                catch(Exception exception)
                {
                    //No serializer available
                    argumentsJson += "{";
                    argumentsJson += parameter.toString();
                    argumentsJson += "}";                           
                }
                
                if(parameterIndex != joinPoint.getArgs().length)
                {
                    argumentsJson += ",";
                }
                
                //Increment the parameter index
                parameterIndex++;
            }
            
            //End building argument array
            argumentsJson += "]";
        }   
        
        LOGGER.warn("Method: " + joinPoint.getStaticPart().getSignature().getName() + "(), Argumemts: " + argumentsJson);*/
	    
	}
    
	//ReturnValue
	@AfterReturning(pointcut=POINTCUT, returning="result")
	public void logAfterReturning(JoinPoint joinPoint, Object result) 
	{
		//Log the message
		log(joinPoint, MethodComponent.RETURN_VALUE, result);
	}
	
	//Exception
	@AfterThrowing(pointcut=POINTCUT, throwing="exception")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable exception) 
	{
		//Log the message
		log(joinPoint, MethodComponent.EXCEPTION, exception);
    }
	
	private void log(JoinPoint joinPoint, MethodComponent methodComponent, Object componentValue)
	{
		MethodSignature signature = (MethodSignature)joinPoint.getStaticPart().getSignature();
		Class<?> methodClass = joinPoint.getTarget().getClass();
		Method method = signature.getMethod();
		
		//Determine what the log level should be
		Logged logged = methodClass.getAnnotation(Logged.class) != null ? methodClass.getAnnotation(Logged.class) : method.getAnnotation(Logged.class);		
		LogLevel logLevel = getLogLevel(methodComponent, logged);		
		
		//Set the logger for the correct class (not this one!)
		LOGGER = LoggerFactory.getLogger(methodClass);
		
		//Determine if we should log based on the logger level
		if(logLevel.ordinal() >= getLoggerLevel().ordinal())
		{
			
			//Format the message
			String loggedComponentText  = new Gson().toJson(componentValue);
			String logMessage = String.format("Method: %s(), %s: %s", method.getName(), methodComponent, loggedComponentText);
			
			//Determine the correct logger			
			switch(logLevel)
			{
				case DEBUG:
					LOGGER.debug(logMessage);
					break;
				case INFO:
					LOGGER.info(logMessage);
					break;
				case TRACE:
					LOGGER.trace(logMessage);
					break;
				case WARN:
					LOGGER.warn(logMessage);
					break;
				case ERROR:
					LOGGER.error(logMessage);
					break;
				default:
					LOGGER.debug(logMessage);
					break;
			}
		}
	}
	
	private LogLevel getLoggerLevel()
	{
		if(LOGGER.isTraceEnabled())
		{
			return LogLevel.TRACE;
		}
		else if(LOGGER.isDebugEnabled())
		{
			return LogLevel.DEBUG;
		}
		else if(LOGGER.isInfoEnabled())
		{
			return LogLevel.INFO;
		}
		else if(LOGGER.isWarnEnabled())
		{
			return LogLevel.WARN;
		}
		else if(LOGGER.isErrorEnabled())
		{
			return LogLevel.ERROR;
		}
		else
		{
			return LogLevel.OFF;
		}
	}
	
	private LogLevel getLogLevel(MethodComponent methodComponent, Logged logged)
	{
		switch(methodComponent)
		{
			case ARGUMENTS:
				return logged.argumentsLogLevel();
			case RETURN_VALUE:
				return logged.returnValueLogLevel();
			case EXCEPTION:
				return logged.exceptionLogLevel();
			default:
				return LogLevel.DEBUG;
		}
	}
}
