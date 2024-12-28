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
package io.github.sasiperi.logsafe.logger;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class HttpLogMessage
{
   String httpMethod;
   String uri;
   String remoteHost;
   Map<String,String> headers;
   Map<String,String> requestParams;
   Map<String,Object> requestAttributes;
   
   Object body;
   
   @Override
   public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
       }catch(Exception e) {
           return "Could not serialize HttpLogMessage: "+ e.getMessage();
       }
       
   }
   
}

