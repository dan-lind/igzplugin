package com.danlind.igz;

/*
 * #%L
 * IG API - Sample Client
 * %%
 * Copyright (C) 2014 IG Index
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(value = "application-spring-context.xml")
public class BeanConfiguration {

   @Bean
   public HttpClient httpClient() {
      return HttpClients.createDefault();
   }

   @Bean
   @Qualifier(value = "ig.api.domain.URL")
   public String igApiDomainURL() {
      return PropertiesUtil.getProperty("environment.URL");
   }

}
