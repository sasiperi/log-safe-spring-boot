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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sasiperi.logsafe.logger.HttpLogMessage;
import io.github.sasiperi.logsafe.logger.LogMessageRedactor;
import io.github.sasiperi.logsafe.testdata.Address;
import io.github.sasiperi.logsafe.testdata.Employee;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HttpLogFilterHelperTest {

    @Mock
    private LogMessageRedactor redactor;

    @Mock
    private RepeatableContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private HandlerMethod handlerMethod;
    
    /*
    @Mock
    Method method;
    
    @Mock
    Parameter parameter;
    
   */
    private HttpLogFilterHelper helper;

    @BeforeEach
    void setup() {
    	
    	helper = new HttpLogFilterHelper(redactor);
    	reset(request, response, handlerMethod, redactor);
        
    }
   

    @Test
    void testLogRequest_SuccessfulLogging(CapturedOutput output) throws Exception {
    	
        // Setup, Mock
    	Employee requestBody = new Employee("John", "Doe", "123-45-6789", new Address("NY", "New York", "555-1234"));
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Content-Type")));
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("apiKey")));
        when(request.getParameter("apiKey")  ).thenReturn("apiKeyValue");
        
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBodyJson)));
        
        
        Method method = this.getClass().getDeclaredMethod("testRequestMethod", Employee.class);
        
        when(handlerMethod.getMethod()).thenReturn(method);
        
        // Do Stuff
        helper.logRequest(request, handlerMethod);
        
        
        // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> requestCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);

        verify(redactor).redactLogMessage(requestCaptor.capture());
        
        assertNotNull(requestCaptor.getValue().getBody());
        
        assertTrue(output.getOut().contains("REQUEST DATA:"),"Expected log output REQUEST DATA");
        
        
    }

    @Test
    void testLogRequest_UnsupportedContentType(CapturedOutput output) throws Exception {
    	
    	// Setup, Mock
    	
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Content-Type")));
        when(request.getHeader("Content-Type")).thenReturn("application/json");
        
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("apiKey")));
        when(request.getParameter("apiKey")  ).thenReturn("apiKeyValue");
        
        when(request.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        
        // Act
        helper.logRequest(request, handlerMethod);

        // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> requestCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);
         
        verify(redactor).redactLogMessage(requestCaptor.capture());
        
        assertNull(requestCaptor.getValue().getBody());
        
        assertTrue(output.getOut().contains("REQUEST DATA:"),"Expected log output REQUEST DATA");
    }

    @Test
    void testLogResponse_SuccessfulLogging(CapturedOutput output) throws Exception {
        // Arrange
        when(response.getHeaderNames()).thenReturn(List.of("Content-Type"));
        when(response.getHeader("Content-Type")).thenReturn("application/json");
        
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader("{\"key\":\"value\"}")));
        
        // Act
        helper.logResponse(response, handlerMethod);

        // Assert
        verify(redactor).redactLogMessage(any());
        
        assertTrue(output.getOut().contains("RESPONSE DATA:"),"Expected log output RESPONSE DATA");
    }
    
    
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee testRequestMethod(@RequestBody Employee employee) {
    	Employee employeeResp = new Employee("John", "Doe", "123-45-6789", new Address("NY", "New York", "555-1234"));
    	return employeeResp;
    }
   
}


