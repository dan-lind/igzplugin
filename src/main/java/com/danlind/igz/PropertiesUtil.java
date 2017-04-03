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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {

   private static final Logger LOG = LoggerFactory.getLogger(PropertiesUtil.class);

   private static final String PROPERTY_FILENAME = "environment.properties";

   private static Properties theProperties;

   public static Properties getProperties() throws RuntimeException {
      if (theProperties == null) {
         theProperties = new Properties();

         String filename = PROPERTY_FILENAME;
         LOG.debug("filename: " + filename);

         try {
            theProperties.load(PropertiesUtil.class.getClassLoader().getResourceAsStream(filename));
         } catch (IOException e) {
            throw new RuntimeException("Unable to load properties file: " + filename);
         }

         // additional local property file
         filename = "local.properties";
         InputStream resourceAsStream = PropertiesUtil.class.getClassLoader().getResourceAsStream(filename);

         if (resourceAsStream != null) {
            LOG.debug("Properties file found");
            try {
               theProperties.load(resourceAsStream);
            } catch (IOException e) {
               throw new RuntimeException("Unable to load properties file: " + filename);
            }
         }
      }

      return theProperties;
   }

   public static String getProperty(String key) {
      return getProperties().getProperty(key);
   }

}
