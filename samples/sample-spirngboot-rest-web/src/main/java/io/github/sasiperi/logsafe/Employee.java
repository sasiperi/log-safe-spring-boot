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

import com.fasterxml.jackson.annotation.JsonInclude;

import io.github.sasiperi.logsafe.logger.Redact;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
	
	private int id;
    private String firstName;
    private String lastName;

    @Redact
    private String ssn;
    
    //to test no other java or other types are recursively processed (reflection) and only base-packe configured is searched for DTOs
    private EmployeeType employeeType;

    // to test nested elements redacted.
    private Address address;

	
}
