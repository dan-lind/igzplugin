package com.danlind.igz.handler;

import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.ig.api.client.StreamingAPI;
import com.danlind.igz.ig.api.client.streaming.HandyTableListenerAdapter;
import com.lightstreamer.ls_client.UpdateInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by danlin on 2017-03-29.
 */
@Component
public class VolumeProvider {

    private final static Logger logger = LoggerFactory.getLogger(VolumeProvider.class);
    private Map<Epic, List<Integer>> rollingVolumeMap = new HashMap<>();

    public void updateRollingVolume(Epic epic, int volume) {
        List<Integer> volumeList = rollingVolumeMap.get(epic);
        if (Objects.isNull(volumeList)) {
            volumeList = new ArrayList<>();
        }
        volumeList.add(volume);
        if (volumeList.size() > 5) {
            volumeList.remove(0);
        }
        rollingVolumeMap.put(epic, volumeList);
    }

    public int getAverageVolume(Epic epic) {
        List<Integer> volumeList = rollingVolumeMap.get(epic);
        int sum = volumeList.stream().mapToInt(Integer::intValue).sum();
        return sum / volumeList.size();
    }
}
