package com.danlind.igz.misc;

import com.danlind.igz.domain.types.Epic;
import com.danlind.igz.domain.types.Volume;
import com.danlind.igz.misc.VolumeProvider;
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
        Volume volume = new Volume(5);
        provider.updateRollingVolume(epic, volume);
        provider.updateRollingVolume(epic, volume);
        Assert.assertEquals(5, provider.getAverageVolume(epic));
    }

    @Test
    public void testRolloverVolumeAverage() {
        VolumeProvider provider = new VolumeProvider();
        Epic epic = new Epic("TestEpic");
        Volume volumeFive = new Volume(5);
        Volume volumeTen = new Volume(10);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        provider.updateRollingVolume(epic, volumeFive);
        Assert.assertEquals(5, provider.getAverageVolume(epic));
        provider.updateRollingVolume(epic, volumeTen);
        provider.updateRollingVolume(epic, volumeTen);
        provider.updateRollingVolume(epic, volumeTen);
        Assert.assertEquals(8, provider.getAverageVolume(epic));
    }

    @Test
    public void testVolumeAverageWithMultipleEpics() {
        VolumeProvider provider = new VolumeProvider();
        Epic epic = new Epic("TestEpic");
        Epic otherEpic = new Epic("OtherTestEpic");
        provider.updateRollingVolume(epic, new Volume(5));
        provider.updateRollingVolume(epic, new Volume(10));
        provider.updateRollingVolume(epic, new Volume(15));
        Assert.assertEquals(10, provider.getAverageVolume(epic));
        provider.updateRollingVolume(otherEpic, new Volume(10));
        provider.updateRollingVolume(otherEpic, new Volume(20));
        provider.updateRollingVolume(otherEpic, new Volume(30));
        Assert.assertEquals(10, provider.getAverageVolume(epic));
        Assert.assertEquals(20, provider.getAverageVolume(otherEpic));
    }

}
