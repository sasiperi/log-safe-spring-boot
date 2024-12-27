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

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
/**
 * Filter class that aims to guarantee a single execution per http request
 * dispatch, on any servlet container. Unlike Spring OOTB Filter {@link #OncePerRequestFilter} 
 * {@link ContentCachingRequestWrapper}, this one wraps request into a repeatable request/response. 
 * That's it can be read more than once. For more details check {@link RepeatableContentCachingRequestWrapper}
 * method with HttpServletRequest and HttpServletResponse arguments.
 * 
 * @author sasiperi
 * @since 11.20.2023
 */
@Slf4j
@AutoConfiguration
@Component
public class HttpRequestResponseLogFilter extends OncePerRequestFilter {
  
  private final RequestMappingHandlerMapping handlerMapping;
  private final HttpLogFilterHelper logFilterHelper;
  
  private final boolean logRequest;
  private final boolean logResponse;

  public HttpRequestResponseLogFilter(
          @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping,
          HttpLogFilterHelper logFilterHelper,
          @Value("${logsafe.logger.in.log-request:true}") boolean logRequest,
          @Value("${logsafe.logger.in.log-response:false}") boolean logResponse) {
      this.handlerMapping = handlerMapping;
      this.logFilterHelper = logFilterHelper;
      this.logRequest = logRequest;
      this.logResponse = logResponse;
  }
  
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
     
            //Find the handler (end-point map) of the controller, that would be handling this http request.
            HandlerExecutionChain handlerChain;
            try
            {
                handlerChain = handlerMapping.getHandler(request);
            }
            catch (Exception e)
            {
                handlerChain = null;
                log.info("LG000: Error occured during resolving request handler", e.getMessage());
            }
          
            
            // this can be null, if there is an exception in getHandler 
            // OR there is no request mapping (no controller/paths to handle) to process
            // OR the service does not have "default handler set to throw a default page/response" like I-Am-teapot or something.
            // If there is a handlerChain then proceed with logging.
            if(handlerChain != null)
            {
              //Find the (endpoint) method that would be handling this http request.
              HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
              
              //Log Request with buffered, caching req wrapper, so that the req is still available after read from streamed, at the begining.
              RepeatableContentCachingRequestWrapper repeatableContentCachingRequestWrapper = new RepeatableContentCachingRequestWrapper(request);

              if(logRequest)
              {

                  logFilterHelper.logRequest(repeatableContentCachingRequestWrapper, handlerMethod);
              }

              if(logResponse)
              {

                  ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
                  filterChain.doFilter(repeatableContentCachingRequestWrapper, responseWrapper);

                  //Log Response and reset, or else it's not available after read once (it can be read only once by default).
                  logFilterHelper.logResponse(responseWrapper, handlerMethod);

                  //copy back after reading once to log
                  responseWrapper.copyBodyToResponse();

              }
              // By adding this additional condition, Saving a bit of computation (performance) if the response is not required to be logged
              // by not wrapping it into repeatable response and copying it back to body
              // Also this will take care if both are marked false, it would still filter through.
              else {
                  //filter chain with repeatable request.
                  filterChain.doFilter(repeatableContentCachingRequestWrapper, response);
              }

            }else {

                // Default spring implementation (mostly no match path/handler found, resulting 404.)
                filterChain.doFilter(request, response);
            }

  }

}
