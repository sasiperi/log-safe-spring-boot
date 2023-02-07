package com.example.aop;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

@Aspect
@Component
public class LoggingAspectAnother
{	
	//Setup the logger, defaulting it to the current class
	private static Logger LOG = LoggerFactory.getLogger(LoggingAspect.class);
	
	private static ObjectMapper MAPPER = new ObjectMapper();
			
	private static final String POINTCUT = "@within(org.springframework.data.rest.webmvc.RepositoryRestController) || @annotation(org.springframework.data.rest.webmvc.RepositoryRestController)";
	
		
	@Around(POINTCUT)
	public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable 
	{	
		logPhi(joinPoint);
		
		if(LOG.isDebugEnabled())
		{
			//Measure execution time
			StopWatch stopWatch = new StopWatch();
			stopWatch.start();
			
			//Continue with intercepted method
			Object returnValue = joinPoint.proceed();
			
			stopWatch.stop();
			
			//Attempt to get the arguments, do this after execution so we do not affect the time measurements
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
	    				argumentsJson += MAPPER.writeValueAsString(joinPoint.getArgs()[parameterIndex]);
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
			
			//Output values
			LOG = LoggerFactory.getLogger(joinPoint.getStaticPart().getSignature().getDeclaringTypeName());
			LOG.debug("Method: " + joinPoint.getStaticPart().getSignature().getName() + "(), Argumemts: " + argumentsJson + ", Execution Time: " + stopWatch.getTotalTimeSeconds() + "(s)");
			
			//Return control
			return returnValue;
		}		
		else
		{
			//Simply return control back to intercepted method, do nothing else
			return joinPoint.proceed();
		}
	}
	
	private void logPhi(JoinPoint joinPoint) throws Throwable
	{
		//Log PHI access attempts
		MethodSignature signature = (MethodSignature)joinPoint.getStaticPart().getSignature();
	    Method method = signature.getMethod();
	    
	    //Check for our custom auditing annotation
	   	
	    	String className = joinPoint.getStaticPart().getSignature().getDeclaringTypeName();
			String methodName = joinPoint.getStaticPart().getSignature().getName();
	    	
	    	String username = "sasi";
    		   		
    		
    		
    		Map<Object,Object> identifierMap = new HashMap<Object, Object>();
    		
    		//Loop over parameters, looking for PHI identifiers
	    	for(Parameter parameter : method.getParameters())
	    	{		
	    			//Cross-ref against arguments
	    			String argumentIndexString = StringUtils.replace(parameter.getName(), "arg", "");
	    			int argumentIndex = Integer.parseInt(argumentIndexString);
	    			Object value = joinPoint.getArgs()[argumentIndex];
	    			
	    			identifierMap.put(parameter.getName(), value.toString());
	    		
	    	}
	    	
	    	//Write to standard out
	    	System.out.println("**PHI ACCESS** Class: " + className + ", Method: " + methodName + "(), Username: " + username + ", Identifiers: " + identifierMap.toString());
	    	LOG.info("**PHI ACCESS** Class: " + className + ", Method: " + methodName + "(), Username: " + username + ", Identifiers: " + identifierMap.toString());
	    
	}
 	
	@AfterReturning(pointcut=POINTCUT, returning="result")
	public void logAfterReturning(JoinPoint joinPoint, Object result) 
	{
		if(LOG.isDebugEnabled())
		{
			LOG = LoggerFactory.getLogger(joinPoint.getStaticPart().getSignature().getDeclaringTypeName());
			LOG.debug("Method: " + joinPoint.getStaticPart().getSignature().getName() + "(), Return Value: " + result);
		}
	}
	
	@AfterThrowing(pointcut=POINTCUT, throwing= "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) 
	{
		if(LOG.isErrorEnabled())
		{
			LOG = LoggerFactory.getLogger(joinPoint.getStaticPart().getSignature().getDeclaringTypeName());
			LOG.error("Method: " + joinPoint.getStaticPart().getSignature().getName() + "(), Exception: ", error);
		}
    }
}
