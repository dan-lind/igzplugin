package com.danlind.igz.handler;

import com.danlind.igz.domain.types.Epic;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by danlin on 2017-03-29.
 */
public class VolumeProviderTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testSimpleVolumeAverage() {
        VolumeProvider provider = new VolumeProvider();
        Epic epic = new Epic("TestEpic");
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 5);
        Assert.assertEquals(5, provider.getAverageVolume(epic));
    }

    @Test
    public void testRolloverVolumeAverage() {
        VolumeProvider provider = new VolumeProvider();
        Epic epic = new Epic("TestEpic");
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 5);
        Assert.assertEquals(5, provider.getAverageVolume(epic));
        provider.updateRollingVolume(epic, 10);
        provider.updateRollingVolume(epic, 10);
        provider.updateRollingVolume(epic, 10);
        Assert.assertEquals(8, provider.getAverageVolume(epic));
    }

    @Test
    public void testVolumeAverageWithMultipleEpics() {
        VolumeProvider provider = new VolumeProvider();
        Epic epic = new Epic("TestEpic");
        Epic otherEpic = new Epic("OtherTestEpic");
        provider.updateRollingVolume(epic, 5);
        provider.updateRollingVolume(epic, 10);
        provider.updateRollingVolume(epic, 15);
        Assert.assertEquals(10, provider.getAverageVolume(epic));
        provider.updateRollingVolume(otherEpic, 10);
        provider.updateRollingVolume(otherEpic, 20);
        provider.updateRollingVolume(otherEpic, 30);
        Assert.assertEquals(10, provider.getAverageVolume(epic));
        Assert.assertEquals(20, provider.getAverageVolume(otherEpic));
    }

}
