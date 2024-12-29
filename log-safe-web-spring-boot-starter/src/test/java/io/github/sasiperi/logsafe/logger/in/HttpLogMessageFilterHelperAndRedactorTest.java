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

import static io.github.sasiperi.logsafe.testdto.EmployeeType.FULL_TIME;
import static io.github.sasiperi.logsafe.testdto.EmployeeType.PART_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.util.WebUtils.DEFAULT_CHARACTER_ENCODING;

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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sasiperi.logsafe.config.SensitiveDataConfig;
import io.github.sasiperi.logsafe.logger.HttpLogMessage;
import io.github.sasiperi.logsafe.logger.LogMessageRedactor;
import io.github.sasiperi.logsafe.testdto.Address;
import io.github.sasiperi.logsafe.testdto.Employee;


@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HttpLogMessageFilterHelperAndRedactorTest {


    @Mock
    private RepeatableContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private HandlerMethod handlerMethod;
    
    @Mock
    SensitiveDataConfig sensitiveData;
   
    private HttpLogFilterHelper helper;
    
    private LogMessageRedactor redactor;
    private LogMessageRedactor spyRedactor;
    
    
    private static final String REDACTED = "[REDACTED]";
    
    private static final String AUTH_HEADER = "Authorization";//Sensitive
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CUSTOM_HEADER = "Custom-Header";
    
    private static final String QUERY_PARAM_APIKEY = "apiKey";//Sensitive
    private static final String QUERY_PARAM_TEST = "testParam";
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    

    @BeforeEach
    void setup() {
    	
    	redactor = new LogMessageRedactor("io.github.sasiperi.logsafe",sensitiveData);
    	spyRedactor = Mockito.spy(redactor);
    	helper = new HttpLogFilterHelper(spyRedactor);
        
    	// Below is all small case, so as to test case in-senstive header config		
        List<String> headers = List.of("authorization");
        
        when(sensitiveData.getHeaders()).thenReturn(headers);
        
    	reset(request, response, handlerMethod);
        
    }
   

    @Test
    void testLogRequest_SuccessfullyRedactedAndLogged(CapturedOutput output) throws Exception {
    	
        // Setup, Mock
    	Employee requestBody = new Employee("John", "Doe", "123-45-6789", FULL_TIME, new Address("NY", "New York", "555-1234"));
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        
        List<String> queryParams = List.of("apiKey");
        when(sensitiveData.getQueryParams()).thenReturn(queryParams);
        
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        
        //To test case in-senstive headers are accepted and redacted
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(AUTH_HEADER,CONTENT_TYPE_HEADER, CUSTOM_HEADER)));
        when(request.getHeader(CONTENT_TYPE_HEADER)).thenReturn("application/json");
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer blahblah");
        when(request.getHeader(CUSTOM_HEADER)).thenReturn("Custom Header Value");
        
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of(QUERY_PARAM_APIKEY,QUERY_PARAM_TEST)));
        when(request.getParameter(QUERY_PARAM_APIKEY)  ).thenReturn("apiKeyValue");
        when(request.getParameter(QUERY_PARAM_TEST)  ).thenReturn("testValue");
        
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBodyJson)));
        
        
        Method method = this.getClass().getDeclaredMethod("testRequestMethod", Employee.class);
        
        when(handlerMethod.getMethod()).thenReturn(method);
        
        
        // Do Stuff
        helper.logRequest(request, handlerMethod);
        
        
        // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> requestCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);

        verify(spyRedactor).redactLogMessage(requestCaptor.capture());
        
        assertNotNull(requestCaptor.getValue().getBody());
        
        HttpLogMessage redactedMessage = requestCaptor.getValue();
        
        //Assert that headers redacted or not redacted correctly app.props/SensitveDataConfig 
        assertEquals(redactedMessage.getHeaders().get(AUTH_HEADER), REDACTED, "Authorization header is expected to be redacted!");
        assertEquals(redactedMessage.getHeaders().get(CONTENT_TYPE_HEADER), MediaType.APPLICATION_JSON_VALUE);
        assertEquals(redactedMessage.getHeaders().get(CUSTOM_HEADER), "Custom Header Value");
        
       //Assert that query params are redacted or not-redacted correctly per the app.props/SensitveDataConfig
        assertEquals(redactedMessage.getRequestParams().get(QUERY_PARAM_APIKEY), REDACTED);
        assertEquals(redactedMessage.getRequestParams().get(QUERY_PARAM_TEST), "testValue");

        //Assert that the fields in the DTO annotated with Redact are redacted and other fields are not.
        Employee body = objectMapper.readValue(redactedMessage.getBody().toString(), Employee.class);
        
        assertEquals(body.getSsn(), REDACTED);
        assertEquals(body.getAddress().getPhoneNumber(), REDACTED);
        
        assertEquals(body.getFirstName(), "John");
        assertEquals(body.getLastName(), "Doe");
        assertEquals(body.getEmployeeType(), FULL_TIME);
        assertEquals(body.getAddress().getState(), "NY");
        assertEquals(body.getAddress().getCity(), "New York");
        
        
        assertTrue(output.getOut().contains("REQUEST DATA:"),"Expected log output REQUEST DATA");//prefix logged
        assertTrue(output.getOut().contains("127.0.0.1"),"Expected log output shoudl have remote host");
        assertTrue(output.getOut().contains("/test"),"Expected log output shoudl have requested URI");
        assertTrue(output.getOut().contains("John"),"Expected log output not matched");//Employee Details logged
        assertTrue(output.getOut().contains("New York"),"Expected log output not matched");//address logged
        assertTrue(output.getOut().contains(REDACTED),"Expected log output not matched");//Some fields logged as redacted.
        
        
    }

    @Test
    void testLogRequest_UnsupportedContentType(CapturedOutput output) throws Exception {
    	
    	// Setup, Mock
    	
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(CONTENT_TYPE_HEADER,AUTH_HEADER)));
        when(request.getHeader(CONTENT_TYPE_HEADER)).thenReturn(MediaType.TEXT_HTML_VALUE);
        when(request.getHeader(AUTH_HEADER)).thenReturn("Bearer blahblah");
        
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of(QUERY_PARAM_APIKEY)));
        when(request.getParameter(QUERY_PARAM_APIKEY)  ).thenReturn("apiKeyValue");
        
        when(request.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        
        // Act
        helper.logRequest(request, handlerMethod);

        // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> logMessageCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);
         
        verify(spyRedactor).redactLogMessage(logMessageCaptor.capture());
        
        //Assert that the body is not parsed and not-redacted and not-logged.
        assertNull(logMessageCaptor.getValue().getBody());
        //Assert that rest is logged
        assertTrue(output.getOut().contains("REQUEST DATA:"),"Expected log output REQUEST DATA");
        assertTrue(output.getOut().contains("127.0.0.1"),"Expected log output shoudl have remote host");
        assertTrue(output.getOut().contains("/test"),"Expected log output shoudl have requested URI");
        assertTrue(output.getOut().contains("text/html"),"Expected log output REQUEST DATA");
        assertTrue(output.getOut().contains(REDACTED),"Expected log output REQUEST DATA");
        
        
    }

    @Test
    void testLogResponsePojo_SuccessfullyRedactedAndLogged(CapturedOutput output) throws Exception {
        
    	// Arrange
        when(response.getHeaderNames()).thenReturn(List.of(CONTENT_TYPE_HEADER));
        when(response.getHeader(CONTENT_TYPE_HEADER)).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        when(response.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        // Setup, Mock
    	Employee responseBody = new Employee("John", "Doe", "123-45-6789", PART_TIME, new Address("NY", "New York", "555-1234"));
        String responseBodyJson = objectMapper.writeValueAsString(responseBody);
        
        when(response.getContentAsByteArray()).thenReturn(responseBodyJson.getBytes());
        when(response.getCharacterEncoding()).thenReturn(DEFAULT_CHARACTER_ENCODING);
        
        
        // Reflectively get the method to simulate the real handler method
        Method testMethod = this.getClass().getDeclaredMethod("testRequestMethod", Employee.class);
        // Create a MethodParameter for the method's return type
        MethodParameter methodParameter = new MethodParameter(testMethod, -1);

        // Mock the handlerMethod.getReturnType() to return the simulated MethodParameter
        when(handlerMethod.getReturnType()).thenReturn(methodParameter);
        
        // Act
        helper.logResponse(response, handlerMethod);

     // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> responseCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);

        verify(spyRedactor).redactLogMessage(responseCaptor.capture());
        
        assertNotNull(responseCaptor.getValue().getBody());
        
        HttpLogMessage redactedMessage = responseCaptor.getValue();
        
        //Assert that headers redacted or not redacted correctly app.props/SensitveDataConfig 
        assertEquals(redactedMessage.getHeaders().get(CONTENT_TYPE_HEADER), MediaType.APPLICATION_JSON_VALUE);
       
        //Assert that the fields in the DTO annotated with Redact are redacted and other fields are not.
        Employee body = objectMapper.readValue(redactedMessage.getBody().toString(), Employee.class);
        
        assertEquals(body.getSsn(), REDACTED);
        assertEquals(body.getAddress().getPhoneNumber(), REDACTED);
        
        assertEquals(body.getFirstName(), "John");
        assertEquals(body.getLastName(), "Doe");
        assertEquals(body.getEmployeeType(), PART_TIME);
        assertEquals(body.getAddress().getState(), "NY");
        assertEquals(body.getAddress().getCity(), "New York");
        
        
        assertTrue(output.getOut().contains("RESPONSE DATA:"),"Expected log output RESPONSE DATA");//prefix logged
        assertTrue(output.getOut().contains("John"),"Expected log output not matched");//Employee Details logged
        assertTrue(output.getOut().contains("New York"),"Expected log output not matched");//address logged
        assertTrue(output.getOut().contains(REDACTED),"Expected log output not matched");//Some fields logged as redacted.
        
    }
    
    

    @Test
    void testLogResponseEntity_SuccessfullyRedactedAndLogged(CapturedOutput output) throws Exception {
        
    	// Arrange
        when(response.getHeaderNames()).thenReturn(List.of(CONTENT_TYPE_HEADER));
        when(response.getHeader(CONTENT_TYPE_HEADER)).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        when(response.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        
        // Setup, Mock
    	Employee responseBody = new Employee("John", "Doe", "123-45-6789", PART_TIME, new Address("NY", "New York", "555-1234"));
        String responseBodyJson = objectMapper.writeValueAsString(responseBody);
        
        when(response.getContentAsByteArray()).thenReturn(responseBodyJson.getBytes());
        when(response.getCharacterEncoding()).thenReturn(DEFAULT_CHARACTER_ENCODING);
        
        
        // Reflectively get the method to simulate the real handler method
        Method testMethod = this.getClass().getDeclaredMethod("testRequestMethodTwo", Employee.class);
        // Create a MethodParameter for the method's return type
        MethodParameter methodParameter = new MethodParameter(testMethod, -1);

        // Mock the handlerMethod.getReturnType() to return the simulated MethodParameter
        when(handlerMethod.getReturnType()).thenReturn(methodParameter);
        
        // Act
        helper.logResponse(response, handlerMethod);

     // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> responseCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);

        verify(spyRedactor).redactLogMessage(responseCaptor.capture());
        
        assertNotNull(responseCaptor.getValue().getBody());
        
        HttpLogMessage redactedMessage = responseCaptor.getValue();
        
        //Assert that headers redacted or not redacted correctly app.props/SensitveDataConfig 
        assertEquals(redactedMessage.getHeaders().get(CONTENT_TYPE_HEADER), MediaType.APPLICATION_JSON_VALUE);
       
        //Assert that the fields in the DTO annotated with Redact are redacted and other fields are not.
        Employee body = objectMapper.readValue(redactedMessage.getBody().toString(), Employee.class);
        
        assertEquals(body.getSsn(), REDACTED);
        assertEquals(body.getAddress().getPhoneNumber(), REDACTED);
        
        assertEquals(body.getFirstName(), "John");
        assertEquals(body.getLastName(), "Doe");
        assertEquals(body.getEmployeeType(), PART_TIME);
        assertEquals(body.getAddress().getState(), "NY");
        assertEquals(body.getAddress().getCity(), "New York");
        
        
        assertTrue(output.getOut().contains("RESPONSE DATA:"),"Expected log output RESPONSE DATA");//prefix logged
        assertTrue(output.getOut().contains("John"),"Expected log output not matched");//Employee Details logged
        assertTrue(output.getOut().contains("New York"),"Expected log output not matched");//address logged
        assertTrue(output.getOut().contains(REDACTED),"Expected log output not matched");//Some fields logged as redacted.
        
    }
    
    
    @Test
    void testLogResponse_UnsupportedContentType(CapturedOutput output) throws Exception {
        
    	// Arrange
        when(response.getHeaderNames()).thenReturn(List.of(CONTENT_TYPE_HEADER));
        when(response.getHeader(CONTENT_TYPE_HEADER)).thenReturn(MediaType.TEXT_HTML_VALUE);
        
        when(response.getContentType()).thenReturn(MediaType.TEXT_HTML_VALUE);
        
        // Act
        helper.logResponse(response, handlerMethod);

     // Capture arguments passed to filter chain
        ArgumentCaptor<HttpLogMessage> responseCaptor = ArgumentCaptor.forClass(HttpLogMessage.class);

        verify(spyRedactor).redactLogMessage(responseCaptor.capture());
        
        assertNull(responseCaptor.getValue().getBody());
        
        assertTrue(output.getOut().contains("RESPONSE DATA:"),"Expected log output RESPONSE DATA");//prefix logged
        
    }
    
    // Sample Handler Test Methods
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee testRequestMethod(@RequestBody Employee employee) {
    	Employee employeeResp = new Employee("John", "Doe", "123-45-6789", PART_TIME, new Address("NY", "New York", "555-1234"));
    	return employeeResp;
    }
    
    @PostMapping("/test")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Employee> testRequestMethodTwo(@RequestBody Employee employee) {
        return ResponseEntity.ok(employee);
    }
   
}


