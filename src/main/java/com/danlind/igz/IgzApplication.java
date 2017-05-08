package com.danlind.igz;

import com.danlind.igz.config.HttpClientProperties;
import com.danlind.igz.config.PluginProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Created by danlin on 2017-03-07.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties( { PluginProperties.class, HttpClientProperties.class } )
public class IgzApplication {

    static Logger logger = LoggerFactory.getLogger(IgzApplication.class);

    public static void main(String[] args) {
        logger.info("Starting IG-Zorro bridge");
    }


}
