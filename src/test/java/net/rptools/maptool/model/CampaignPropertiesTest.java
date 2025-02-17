package net.rptools.maptool.model;

import net.rptools.lib.MD5Key;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CampaignPropertiesTest {

    @Test
    public void testSetValidDefaultTokenPropertyType() {
        CampaignProperties campaignProperties = new CampaignProperties();
        assertNotNull(campaignProperties.getDefaultTokenPropertyType());
    }

    @Test
    public void testShouldSuccessfullyMergeInto() {
        CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
        CampaignProperties campaignPropertiesTest2 = new CampaignProperties();

        LookupTable lookupTableTest = new LookupTable();
        lookupTableTest.setName("lookupTableTest");

        Map<String, LookupTable> mapTest = new HashMap<>();

        mapTest.put("1", lookupTableTest);

        campaignPropertiesTest1.setLookupTableMap(mapTest);

        campaignPropertiesTest1.mergeInto(campaignPropertiesTest2);

        assertEquals(campaignPropertiesTest2.getLookupTableMap().get("1"), lookupTableTest);
    }

    @Test
    public void testShouldGetAllImageAssets() {
        CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
        MD5Key imageKeyTest =
                new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());

        LookupTable lookupTableTest = new LookupTable();
        lookupTableTest.setTableImage(imageKeyTest);

        Map<String, LookupTable> mapTest = new HashMap<>();

        mapTest.put("1", lookupTableTest);

        campaignPropertiesTest1.setLookupTableMap(mapTest);

        Set<MD5Key> md5KeySet = campaignPropertiesTest1.getAllImageAssets();

        assertTrue(md5KeySet.contains(imageKeyTest));
    }

    @Test
    public void testShouldSetInitiativeOwnerPermissions()
    {
        CampaignProperties campaignPropertiesTest1 = new CampaignProperties();

        campaignPropertiesTest1.setInitiativeOwnerPermissions(true);
        assertTrue(campaignPropertiesTest1.isInitiativeOwnerPermissions());

        campaignPropertiesTest1.setInitiativeOwnerPermissions(false);
        assertFalse(campaignPropertiesTest1.isInitiativeOwnerPermissions());
    }

    @Test
    public void testShouldReadResolveCampaignProperties()
    {
        CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
        Object readResolveTest = campaignPropertiesTest1.readResolve();

        assertNotNull(readResolveTest);
    }
}
