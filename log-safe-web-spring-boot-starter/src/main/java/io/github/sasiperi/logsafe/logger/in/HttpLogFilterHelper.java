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
package io.github.sasiperi.logsafe.logger.in;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sasiperi.logsafe.logger.HttpLogMessage;
import io.github.sasiperi.logsafe.logger.LogMessageRedactor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Helper component to {@link HttpRequestResponseLogFilter}.
 * This class constructs the Log Message POJO {@link HttpLogMessage}, with all content we like to log.
 * @implNote
 * - Retrieves the body of the {@link @HttpServletRequest}, for relevant HTTP Method Handlers, sets's the type to original request type based on {@link @RequestBody}
 * - Retrieves the body of the {link @HttpServletResponse} to the actual type T (POJO) returned by ResponseEntity<T>
 * @implNote This currently supports only mime type application/json mime-type compatable with {@link MediaType.APPLICATION_JSON}.
 * @Todo : Extend the impl to plan/text, text/html etc.. and exclude binary (e.g. pdf, multi-part)
 * @author sasiperi
 * @since 11.20.2023
 * 
 **/

@Component
@Slf4j
@RequiredArgsConstructor
@AutoConfiguration
public class HttpLogFilterHelper
{
    
    private final LogMessageRedactor redactor;
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public void logRequest(RepeatableContentCachingRequestWrapper request, HandlerMethod handlerMethod) {
        try {
            
            HttpLogMessage httpLogMessage = HttpLogMessage
                                                .builder()
                                                .uri(request.getRequestURI())
                                                .remoteHost(request.getRemoteHost())
                                                .httpMethod(request.getMethod())
                                                .headers(getRequestHeadersAsMap(request))
                                                .requestParams(getRequestParametersAsMap(request))
                                                //commenting below for now. As below prints too much, once we learn (feedback) and 
                                                //understand the fields that can be filtered out, we can uncomment or remove permanently
                                                //.requestAttributes(getRequestAttributesAsMap(request))
                                                .body(getRequestBody(request, handlerMethod))
                                                .build();
        
           redactor.redactLogMessage(httpLogMessage);
          
           log.info("REQUEST DATA: {}",httpLogMessage);
          
        } catch (Exception e) {
          log.warn("LG002: Failed to log request with error: ", e);
        }
      }

    
    private Map<String, String> getRequestHeadersAsMap(HttpServletRequest request) {
        
        return Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(
                        headerName -> headerName, 
                        request::getHeader,
                        (existingValue, newValue) ->  newValue
                        ));
    }
    
    
    private Map<String, String> getRequestParametersAsMap(HttpServletRequest request) {
        
        return Collections.list(request.getParameterNames())
                .stream()
                .collect(Collectors.toMap(
                        headerName -> headerName, 
                        request::getParameter,
                        (existingValue, newValue) ->  newValue
                        ));
    }
    
    private Map<String, Object> getRequestAttributesAsMap(HttpServletRequest request) {
        
        return Collections.list(request.getAttributeNames())
                .stream()
                .collect(Collectors.toMap(
                        attribName -> attribName, 
                        request::getAttribute,
                        (existingValue, newValue) ->  newValue
                        ));
    }
    
    
    /**
     * This method will process the request stream (body), deserialize it into it's respective POJO/DTO type 
     * of the original request-type, based on the @RequestBody.
     * This method will NOT throw any exception up the stack, but handled here, so that Logrequest has a chance to process with
     * And log other important elements like Headers, RequestParams etc..
     * @param request
     * @param handlerMethod
     * @return Object (instance of original request type)
     */
    public Object getRequestBody(RepeatableContentCachingRequestWrapper request, HandlerMethod handlerMethod) {
      
      // Read the request JSON body
      if(handlerMethod != null && isSupportedMediaType(request.getContentType()))
      {
          StringBuilder body = new StringBuilder();
          try (BufferedReader reader = request.getReader()) {

              String line;
              while ((line = reader.readLine()) != null) {
                body.append(line.trim());
              }
          
              // Get the DTO class dynamically
              Class<?> dtoClass = getRequestTypeFromHandlerMethod(handlerMethod);
    
              // If DTO class is found, deserialize into the object
              if (dtoClass != null) {

                 return objectMapper.readValue(body.toString(), dtoClass);
              }

          }catch(JsonProcessingException e) {
             log.error("LG002-01: Error deserializing request body to DTO: {}", e.getMessage());
          }
          catch (IOException e) {
              log.error("LG002-02: error while extracting body from the incoming request: {} ", e.getMessage());
         }
          catch (Exception e) {
              log.error("LG002-03 error while extracting and processing request body: {}", e.getMessage());
         }

       }
      
      // Return null. So that subsequent processors (e.g. Redactor will ignore nulls)
      // This helps in excluding non-supported content-types and/or verbs (e.g. GET) excluded 
      // From attempting to redact. Checking if body null is simple than checking if 
      // Body after casting to string has any text or is it empty.
      return null;
      
    }
    
    private Class<?> getRequestTypeFromHandlerMethod(HandlerMethod handlerMethod) {
       
        // Find the parameter annotated with @RequestBody in the controller
        for (Parameter parameter : handlerMethod.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                
                return parameter.getType(); // Return the DTO class type
            }
        }

        return null; // No @RequestBody parameter found
    }
   
    
    public void logResponse(ContentCachingResponseWrapper responseWrapper, HandlerMethod handlerMethod) {
        try 
        {
               
            HttpLogMessage httpLogMessage = HttpLogMessage
                    .builder()
                    .headers(getResponseHeadersAsMap(responseWrapper))
                    .body(getResponseBody(responseWrapper, handlerMethod))
                    .build();  
          

            redactor.redactLogMessage(httpLogMessage);
           
            log.info("RESPONSE DATA: {}",httpLogMessage);
        
          
        } catch (Exception e) {
            log.warn("LG003: Failed to log response with error:", e);
        }
      }
     
     private Map<String, String> getResponseHeadersAsMap(ContentCachingResponseWrapper responseWrapper) {
         return responseWrapper.getHeaderNames()
                 .stream()
                 .collect(Collectors.toMap(
                         headerName -> headerName,
                         responseWrapper::getHeader,
                         (existingValue, newValue) ->  newValue // Handle duplicates
                 ));
         
     }
     
     public Object getResponseBody(ContentCachingResponseWrapper responseWrapper, HandlerMethod handlerMethod) throws IOException {
         
         if(handlerMethod != null && isSupportedMediaType(responseWrapper.getContentType()))
         {
             try {
                 
                 String body = new String(responseWrapper.getContentAsByteArray(), responseWrapper.getCharacterEncoding());
                 Class<?> dtoClass = determineReturnTypeOfHandlerMethod(handlerMethod);
                 // If DTO class is found, deserialize into the object
                 if (dtoClass != null) {
                     
                     try {
                         
                         return objectMapper.readValue(body, dtoClass);
                         
                     }catch(JsonProcessingException e) {
                         
                         log.info("LG003-01: Error deserializing response body to response DTO, doing raw: {}", e.getMessage());
                         
                         // This means mostly this reached A Global Exception Handler (e.g. ControllerAdvise), 
                         // thus can not parse it to be the Controller Response ReturnType.
                         return body; 
                     }
                    
                 }
                 
             }catch (IOException e) {
                 log.error("LG003-02: error while extracting processing response body: {}", e.getMessage());
            }
             catch (Exception e) {
                 log.error("LG003-03: error while extracting reponse body: {}", e.getMessage());
            }
             
             
         }
         // Return null. So that subsequent processors (e.g. Redactor will ignore nulls)
         // This helps in excluding non-supported content-types and/or verbs (e.g. GET) excluded 
         // From attempting to redact. Checking if body null is simple than checking if 
         // Body after casting to string has any text or is it empty.
         return null;
         
       }
     
     public Class<?> determineReturnTypeOfHandlerMethod(HandlerMethod handlerMethod) {
         
         MethodParameter returnType = handlerMethod.getReturnType();
         Class<?> returnTypeClass = returnType.getParameterType();

         if (returnTypeClass == Void.TYPE || returnTypeClass == Void.class) {
             // Method returns void
             return null;
         }

         if (ResponseEntity.class.isAssignableFrom(returnTypeClass)) {
             // Extract the generic type from ResponseEntity<T>
             Type genericType = returnType.getGenericParameterType();
             if (genericType instanceof ParameterizedType parameterizedType) {
                 Type[] typeArguments = parameterizedType.getActualTypeArguments();
                 if (typeArguments.length > 0 && typeArguments[0] instanceof Class<?>) {
                     return (Class<?>) typeArguments[0];
                 }
             }
             return null; 
         }

         // For other return types, directly return the class
         return returnTypeClass;
     }
    
     
     // Right now configurable redaction and logging req/response payload is supported only for mime-type JSON.
     private static boolean isSupportedMediaType(String contentType) {
         if (contentType == null || contentType.isBlank()) {
             return false;
         }
         MediaType inputMediaType = MediaType.parseMediaType(contentType);
         return MediaType.APPLICATION_JSON.isCompatibleWith(inputMediaType);
     }
     

}
