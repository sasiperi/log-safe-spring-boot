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

import static io.github.sasiperi.logsafe.EmployeeType.FULL_TIME;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class TestService {

	public ResponseEntity<Employee> getEmployee() {
		Employee employee = Employee.builder()
								.firstName("John")
								.lastName("Doe")
								.ssn("123-45-6789")
								.employeeType(FULL_TIME)
								.address(new Address("NY", "New York", "555-1234"))
								.build();
				
		return ResponseEntity.ok(employee);
	}

	public Employee createEmployee(Employee employee) {
		
		employee.setId(5);
		return employee;
	}

}
