package com.danlind.igz.config;

import com.danlind.igz.domain.ContractDetails;
import com.danlind.igz.domain.OrderDetails;
import com.danlind.igz.domain.types.DealId;
import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.rest.AuthenticationResponseAndConversationContext;
import com.danlind.igz.ig.api.client.rest.dto.positions.otc.createOTCPositionV2.Direction;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import net.openhft.chronicle.map.ChronicleMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class PluginBeanConfig {

    private final OrderDetails sampleOrderDetails = new OrderDetails(new Epic("IX.D.OMX.IFD.IP"), 10000, Direction.BUY, 20, new DealId("DIAAAAA9QN6L4AU"));

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(){
        ThreadPoolTaskScheduler threadPoolTaskScheduler
                = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix(
                "ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    @Bean
    public ChronicleMap<Integer, OrderDetails> createOrderReferenceMap() {
        File file = new File("./Plugin/ig/orderChronoMap.dat");
        try {
            file.createNewFile();
            return ChronicleMap
                    .of(Integer.class, OrderDetails.class)
                    .averageValue(sampleOrderDetails)
                    .entries(50)
                    .createOrRecoverPersistedTo(file, false);
        } catch (IOException e) {
            throw new RuntimeException("Exception when creating order reference map", e);
        }
    }

    @Bean
    @DependsOn(value = "createOrderReferenceMap")
    public AtomicInteger createAtomicInteger(ChronicleMap<Integer, OrderDetails> orderReferenceMap) {
        int max = orderReferenceMap.keySet().stream().max(Integer::compareTo).orElse(1000);
        return new AtomicInteger(max + 1);
    }

}
