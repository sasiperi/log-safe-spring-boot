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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TestControllerIntegrationTest {
	
	@LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    private static final String baseUrl = "http://localhost:{port}/test";

    @Test
    void testGetEmployee() {
        // Arrange
        String url = baseUrl+"?apiKey=testApiKey&aSecret=testSecret";

        // Act
        ResponseEntity<Employee> response = restTemplate.getForEntity(url, Employee.class, port);

        // Assert
        assertEquals(response.getStatusCode(),HttpStatus.OK);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getFirstName(),"John"); // Expected test data
    }

    @Test
    void testCreateEmployee() {
        // Arrange
        Employee newEmployee = Employee.builder()
                .firstName("Jane")
                .lastName("Doe")
                .ssn("123-45-6789")
                .address(Address.builder()
                        .state("NY")
                        .city("New York")
                        .phoneNumber("555-1234")
                        .build())
                .build();
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Employee> request = new HttpEntity<>(newEmployee, headers);

        // Act
        ResponseEntity<Employee> response = restTemplate.postForEntity(baseUrl, request, Employee.class,port);

        // Assert
        assertEquals(response.getStatusCode(),HttpStatus.OK);
        assertNotNull(response.getBody());
        assertEquals(response.getBody().getFirstName(),"Jane"); // Expected test data
    }
	
	
	

}
