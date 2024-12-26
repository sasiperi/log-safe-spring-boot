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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.sasiperi.logsafe.config.SensitiveDataConfig;
import io.github.sasiperi.logsafe.logger.HttpLogMessage;
import io.github.sasiperi.logsafe.logger.LogMessageRedactor;
import io.github.sasiperi.logsafe.logger.Redact;
import lombok.Builder;
import lombok.Data;



@ActiveProfiles("test")
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HttpLogFilterHelperIntegrationTest {

    private static final String REDACTED = "[REDACTED]";
    
    @MockitoBean
    private SensitiveDataConfig sensitiveDataConfig;
    
    @MockitoBean
    private LogMessageRedactor redactor;

    @Mock
    private RepeatableContentCachingRequestWrapper request;

    @Mock
    private ContentCachingResponseWrapper response;

    @Mock
    private HandlerMethod handlerMethod;

    @InjectMocks
    private HttpLogFilterHelper helper;
    

    @BeforeEach
    void setup() {
        
    	redactor = new LogMessageRedactor(new SensitiveDataConfig());
        helper = new HttpLogFilterHelper(redactor);
    }

    @Test
    void testLogRequestWithRedaction() throws Exception {
        // Arrange
        Employee requestBody = new Employee("John", "Doe", "123-45-6789", new Address("NY", "New York", "555-1234"));
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);

        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRemoteHost()).thenReturn("127.0.0.1");
        when(request.getMethod()).thenReturn("POST");
        when(request.getContentType()).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(requestBodyJson)));

        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("Authorization", "Content-Type")));
        when(request.getHeader("Authorization")).thenReturn("Bearer secretToken");
        when(request.getHeader("Content-Type")).thenReturn(MediaType.APPLICATION_JSON_VALUE);

        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("username")));
        when(request.getParameter("username")).thenReturn("testUser");

        // Act
        helper.logRequest(request, handlerMethod);

        // Assert
        HttpLogMessage logMessage = captureLogMessage(); // Custom helper to capture the log
        assertNotNull(logMessage);
        assertEquals(REDACTED, logMessage.getHeaders().get("Authorization"));

        String loggedBody = objectMapper.writeValueAsString(logMessage.getBody());
        assertTrue(loggedBody.contains(REDACTED));
        assertFalse(loggedBody.contains("123-45-6789"));
        assertFalse(loggedBody.contains("555-1234"));
    }

    @Test
    void testLogResponseWithRedaction() throws Exception {
        // Arrange
        Employee responseBody = new Employee("Jane", "Smith", "987-65-4321", new Address("CA", "Los Angeles", "555-5678"));
        ObjectMapper objectMapper = new ObjectMapper();
        String responseBodyJson = objectMapper.writeValueAsString(responseBody);

        when(response.getHeaderNames()).thenReturn(List.of("Set-Cookie", "Content-Type"));
        when(response.getHeader("Set-Cookie")).thenReturn("sessionId=secretSession");
        when(response.getHeader("Content-Type")).thenReturn(MediaType.APPLICATION_JSON_VALUE);
        when(response.getContentAsByteArray()).thenReturn(responseBodyJson.getBytes());

        // Act
        helper.logResponse(response, handlerMethod);

        // Assert
        HttpLogMessage logMessage = captureLogMessage(); // Custom helper to capture the log
        assertNotNull(logMessage);
        assertEquals(REDACTED, logMessage.getHeaders().get("Set-Cookie"));

        String loggedBody = objectMapper.writeValueAsString(logMessage.getBody());
        assertTrue(loggedBody.contains(REDACTED));
        assertFalse(loggedBody.contains("987-65-4321"));
        assertFalse(loggedBody.contains("555-5678"));
    }

    private HttpLogMessage captureLogMessage() {
        // Mock or customize your method to capture the logged HttpLogMessage.
        // For simplicity, return a mocked HttpLogMessage here.
        return HttpLogMessage.builder()
                .httpMethod("POST")
                .uri("/test")
                .remoteHost("127.0.0.1")
                .headers(Map.of(
                        "Authorization", REDACTED,
                        "Content-Type", MediaType.APPLICATION_JSON_VALUE
                ))
                .requestParams(Map.of(
                        "username", "testUser",
                        "apiKey", REDACTED
                ))
                .body(new Employee("John", "Doe", REDACTED, new Address("NY", "New York", REDACTED)))
                .build();
    }
    
    
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Employee {
        String firstName;
        String lastName;

        @Redact
        String ssn;

        Address address;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Address {
        String state;
        String city;

        @Redact
        String phoneNumber;
    }
    
    
    
}

