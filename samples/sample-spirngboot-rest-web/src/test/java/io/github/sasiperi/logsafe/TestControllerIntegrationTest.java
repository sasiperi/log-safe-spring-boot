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
package io.github.sasiperi.logsafe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
public class TestControllerIntegrationTest {
	
	@LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    private static final String baseUrl = "http://localhost:{port}/test";
    private static final String REDACTED = "[REDACTED]";

    @Test
    void testGetEmployee(CapturedOutput output) {
        // Arrange
        String url = baseUrl+"?apiKey=testApiKey&aSecret=testSecret";
        
        // Act
        ResponseEntity<Employee> response = restTemplate.getForEntity(url, Employee.class, port);

        // Assert
        assertEquals(response.getStatusCode(),HttpStatus.OK);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getFirstName(),"John"); // Expected test data
        
        assertTrue(output.getOut().contains("REQUEST DATA:"),"Expected log output REQUEST DATA");
        assertTrue(output.getOut().contains("\"host\":\"localhost:"),"Expected log output shoudl have remote host");
        assertTrue(output.getOut().contains("/test"),"Expected log output shoudl have requested URI");
        assertTrue(output.getOut().contains("application/json"),"Expected log output content type");
        assertTrue(output.getOut().contains("\"aSecret\":\"[REDACTED]\""),"Expected secret query param to be redacted");
        assertTrue(output.getOut().contains("\\\"firstName\\\":\\\"John\\\""),"Expected firstname John");
        assertTrue(output.getOut().contains("\\\"employeeType\\\":\\\"FULL_TIME\\\""),"Expected employee type to be FULL_TIME");
        assertTrue(output.getOut().contains("\\\"phoneNumber\\\":\\\"[REDACTED]\\\""),"Expected phone number to be redacted");
        
    }

    @Test
    void testCreateEmployee(CapturedOutput output) {
        // Arrange
        Employee newEmployee = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .ssn("123-45-6789")
                .employeeType(EmployeeType.PART_TIME)
                .address(Address.builder()
                        .state("NY")
                        .city("New York")
                        .phoneNumber("555-1234")
                        .build())
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer tokenValue");
        HttpEntity<Employee> request = new HttpEntity<>(newEmployee, headers);

        // Act
        ResponseEntity<Employee> response = restTemplate.postForEntity(baseUrl, request, Employee.class,port);

        // Assert
        assertEquals(response.getStatusCode(),HttpStatus.OK);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getFirstName(),"Jane"); // Expected test data
        
        
        assertTrue(output.getOut().contains("RESPONSE DATA:"),"Expected log output REQUEST DATA");
        assertTrue(output.getOut().contains("\"host\":\"localhost:"),"Expected log output shoudl have remote host");
        assertTrue(output.getOut().contains("/test"),"Expected log output shoudl have requested URI");
        assertTrue(output.getOut().contains("application/json"),"Expected log output REQUEST DATA");
        assertTrue(output.getOut().contains("\"authorization\":\"[REDACTED]\""),"Expected Auth header to be redacted");
        assertTrue(output.getOut().contains("\\\"firstName\\\":\\\"Jane\\\""),"Expected firstname John");
        assertTrue(output.getOut().contains("\\\"employeeType\\\":\\\"PART_TIME\\\""),"Expected employee type to be FULL_TIME");
        assertTrue(output.getOut().contains("\\\"phoneNumber\\\":\\\"[REDACTED]\\\""),"Expected phone number to be redacted");
    }
	
	
    @Test
    void testNodHandlerFound() {
        // Arrange
        String url = baseUrl+"/blah/?apiKey=testApiKey&aSecret=testSecret";
        
        // Act
        ResponseEntity<Employee> response = restTemplate.getForEntity(url, Employee.class, port);

        // Assert
        assertEquals(response.getStatusCode(),HttpStatus.NOT_FOUND);
        
    }

    @Test
    void testPostRequestParamMap(CapturedOutput output) {
        // Arrange
        String url = baseUrl+"/test-post-reqparam?apiKey=testApiKey&aSecret=testSecret";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer tokenValue");
        //HttpEntity<Employee> request = new HttpEntity<>(newEmployee, headers);

        // Act
        Integer response = restTemplate.postForObject(url, null, Integer.class, port);
        
       // Assert
        assertEquals(response,5);
        assertTrue(output.getOut().contains("Params posted [apiKey, aSecret] - [testApiKey, testSecret]"));
        assertTrue(output.getOut().contains("\"aSecret\":\"[REDACTED]\""),"Expected secret query param to be redacted");

        
    }
    
    @Test
    void tesPosttRequestParamMapWithXFormUrlEnc(CapturedOutput output) {
        // Arrange
        String url = baseUrl+"/test-post-reqparam-urlencoded";
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("apiKey", "testApiKey");
        body.add("aSecret", "testSecret");
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

        // Act
        ResponseEntity<Integer> response = restTemplate.postForEntity(url, requestEntity, Integer.class, port);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(6, response.getBody()); // Assuming the method returns the size of the map
        assertTrue(output.getOut().contains("Params posted [apiKey, aSecret] - [testApiKey, testSecret]"));
        assertTrue(output.getOut().contains("\"aSecret\":\"[REDACTED]\""),"Expected secret query param to be redacted");
    }
}
