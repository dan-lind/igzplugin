package com.danlind.igz.misc;

import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.misc.VolumeProvider;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Created by danlin on 2017-03-29.
 */
public class VolumeProviderTest {

    VolumeProvider provider;
    Epic epic;


    @Before
    public void setUp() {
        provider = new VolumeProvider();
        epic = new Epic("TestEpic");

    }

    @Test
    public void testSimpleVolumeAverage() {
        Volume volume = new Volume(5);
        provider.updateRollingVolume(epic, volume);
        provider.updateRollingVolume(epic, volume);
        assertEquals(5, provider.getAverageVolume(epic));
    }

    @Test
    public void testRolloverVolumeAverage() {
        Volume volumeFive = new Volume(5);
        Volume volumeTen = new Volume(10);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        assertEquals(5, provider.getAverageVolume(epic));
        provider.updateRollingVolume(epic, volumeTen);
        provider.updateRollingVolume(epic, volumeTen);
        provider.updateRollingVolume(epic, volumeTen);
        assertEquals(8, provider.getAverageVolume(epic));
    }

    @Test
    public void testVolumeAverageWithMultipleEpics() {
        Epic otherEpic = new Epic("OtherTestEpic");
        provider.updateRollingVolume(epic, new Volume(5));
        provider.updateRollingVolume(epic, new Volume(10));
        provider.updateRollingVolume(epic, new Volume(15));
        assertEquals(10, provider.getAverageVolume(epic));
        provider.updateRollingVolume(otherEpic, new Volume(10));
        provider.updateRollingVolume(otherEpic, new Volume(20));
        provider.updateRollingVolume(otherEpic, new Volume(30));
        assertEquals(10, provider.getAverageVolume(epic));
        assertEquals(20, provider.getAverageVolume(otherEpic));
    }

}
