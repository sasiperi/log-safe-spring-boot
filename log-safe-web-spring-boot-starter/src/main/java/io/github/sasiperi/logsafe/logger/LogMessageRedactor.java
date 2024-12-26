/*
 * Copyright [2024] [author: Sasi Peri] [company: FourthQuest]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.github.sasiperi.logsafe.logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sasiperi.logsafe.config.SensitiveDataConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * This class redacts any HttpRequest and/or HttpResponses are being logged.
 * This takes {@link HttpLogMessage} as input.
 * Redacts any fields in HttpHeaders, HttpParams and HttpRequestAttributes based on the configuration via application properties.
 * Redacts body based on the fields marked as {@link @Redact}
 * 
 * @author sasiperi
 * @since 11.20.2023
 * 
 */

@Component
@RequiredArgsConstructor
@AutoConfiguration
@Slf4j
public class LogMessageRedactor {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REDACTED = "[REDACTED]";
    
    @Value("${logger.base-package-name}")
    private String packageNameToScan;
    
    private final SensitiveDataConfig sensitiveData;

    public HttpLogMessage redactLogMessage(HttpLogMessage httpLogMessage) throws RedactionException {
       
        //redact headers
        if(httpLogMessage.getHeaders() != null)
        {
            Map<String, String> headers = redactSensitiveFields(httpLogMessage.getHeaders(), sensitiveData.getHeaders());
            httpLogMessage.setHeaders(headers);
        }
        
        //redact request params
        if(httpLogMessage.getRequestParams() != null )
        {
            Map<String, String> reqParams = redactSensitiveFields(httpLogMessage.getRequestParams(), sensitiveData.getQueryParams());
            httpLogMessage.setRequestParams(reqParams);
        }
        
        
        //redact request attributes
        if(httpLogMessage.getRequestAttributes() != null)
        {
            Map<String, Object> reqAttribs = redactSensitiveFields(httpLogMessage.getRequestAttributes(), sensitiveData.getRequestAttributes());
            httpLogMessage.setRequestAttributes(reqAttribs);
        }
       
        
        redactBody(httpLogMessage);
        
        return httpLogMessage;
    }

    private void redactBody(HttpLogMessage httpLogMessage) throws RedactionException{
        try 
        {
            Object body = httpLogMessage.getBody();
            
            if(body == null)//this means either the body is not supported content type (!JSON) OR body has nothing.
                return;
            
            // Recursively process the object
            Object redactedObject = processObject(body);
            
            String sanitizedBody  = objectMapper.writeValueAsString(redactedObject);
            httpLogMessage.setBody(sanitizedBody);
            
        } catch (Exception e) {
            throw new RedactionException("LG005-R2: Error redacting sensitive fields", e);
        }
    }
    
    private Object processObject(Object object) throws IllegalAccessException {
        if (object == null)
            return null;
        
        // ResponseEntity Type a DTO
        if (object.getClass().getName().startsWith(packageNameToScan) && !(object instanceof Enum) ) {
         // Handle regular objects (POJOs)
            return processDTO(object);
        }

        // Handle collections (2nd more likely)
        if (object instanceof Collection<?>) {
            return processCollection((Collection<?>) object);
        }

        // Handle maps
        if (object instanceof Map<?, ?>) {
            return processMap((Map<?, ?>) object);
        }
        
        // Handle arrays (adding at the end, chances of reaching here is very less likely or zero.)
        if (object.getClass().isArray()) {
            return processArray(object);
        }
        
        // Handle all others such as primitive types, enums or generic objects etc..
        // That's still application/json compatable (as we filtered allowed mimes in the begining) 
        return object;
        
    }

   
    
    private  Map<String, Object> processDTO(Object object) throws SecurityException{
        Map<String, Object> redactedMap = new HashMap<>();
        
        log.trace(" LG00-R-TR1: CLASS Name IS: {} ", object.getClass().getName());

        for (Field field : object.getClass().getDeclaredFields()) {
           
            try {
                
                if (field == null || Modifier.isFinal(field.getModifiers())) {
                    continue; // Skip nulls and final fields, to protect immutability.
                }
                
                field.setAccessible(true);
                Object fieldValue = field.get(object);
                
                if(fieldValue != null)
                {
                    if (field.isAnnotationPresent(Redact.class)) {
                        // Redact sensitive fields
                        redactedMap.put(field.getName(), REDACTED);
                    } else {

                        log.trace("LG00-R-TR2: Field  Name: {}", fieldValue.getClass().getName());
                        // Recursively process nested objects
                        redactedMap.put(field.getName(), processObject(fieldValue));
                    } 
                }
                
                
            }catch(Exception e)
            {
                log.warn("LG00-R3:  Error during redaction of the field {} {}" , field.getName(), e.getMessage());
            }
            
        }

        return redactedMap;
    }
    
    private static <V> Map<String, V> redactSensitiveFields(Map<String, V> data, List<String> sensitiveFields) 
            throws RedactionException{
            
        try 
        {
          // Normalize sensitive fields to lowercase once
          Set<String> normalizedSensitiveFields = sensitiveFields.stream()
                                                    .map(String::toLowerCase)
                                                    .collect(Collectors.toSet()); 
            
            return data.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> normalizedSensitiveFields.contains(entry.getKey().toLowerCase()) 
                                    ? (V) REDACTED : entry.getValue()
                    ));
        }catch(Exception e)
        {
            throw new RedactionException("LG005-R1: Error redacting sensitive fields", e);
        }
       
    }
    
    private Object processArray(Object array) throws IllegalAccessException {
        int length = java.lang.reflect.Array.getLength(array);
        Object[] redactedArray = new Object[length];
        for (int i = 0; i < length; i++) {
            Object element = java.lang.reflect.Array.get(array, i);
            redactedArray[i] = processObject(element);
        }
        return redactedArray;
    }

    private Collection<?> processCollection(Collection<?> collection) throws IllegalAccessException {
        Collection<Object> redactedCollection = new ArrayList<>();
        for (Object item : collection) {
            redactedCollection.add(processObject(item));
        }
        return redactedCollection;
    }

    private Map<?, ?> processMap(Map<?, ?> map) throws IllegalAccessException {
        Map<Object, Object> redactedMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            redactedMap.put(entry.getKey(), processObject(entry.getValue()));
        }
        return redactedMap;
    }
    
}