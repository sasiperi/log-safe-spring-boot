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

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
@Slf4j
public class TestController {
	
	public final TestService testService;
	
	
	@GetMapping
	public ResponseEntity<Employee> getEmployee(@RequestParam String apiKey, @RequestParam String aSecret) {
		
		log.info("Employee name is: {} with secrete: {}", apiKey, aSecret );
		return testService.getEmployee();
		
	}
	
	@PostMapping
	public Employee createEmployee(@RequestBody Employee employee) {
		
		log.info("Employee posted {}", employee );
		return testService.createEmployee(employee);
		
	}
	
	@PostMapping("/test-post-reqparam")
	public int postRequestParamMap(@RequestParam Map<String, String> reqParams) {
		
		log.info("Params posted {} - {}", reqParams.keySet(),reqParams.values() );
		return 5;
		
	}
	
	@PostMapping(value = "/test-post-reqparam-urlencoded", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public int posttRequestParamMapWithXFormUrlEnc(@RequestParam Map<String, String> reqParams) {
		
		log.info("Params posted {} - {}", reqParams.keySet(),reqParams.values() );
		return 6;
		
	}
}
