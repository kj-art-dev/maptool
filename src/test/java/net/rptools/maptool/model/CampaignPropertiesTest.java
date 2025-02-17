/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.model;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.*;
import net.rptools.lib.MD5Key;
import net.rptools.maptool.model.sheet.stats.StatSheetLocation;
import net.rptools.maptool.model.sheet.stats.StatSheetProperties;
import net.rptools.maptool.server.proto.CampaignPropertiesDto;
import org.junit.jupiter.api.Test;

public class CampaignPropertiesTest {

  @Test
  public void testSetValidDefaultTokenPropertyType() {
    CampaignProperties campaignProperties = new CampaignProperties();
    assertNotNull(campaignProperties.getDefaultTokenPropertyType());
  }

  @Test
  public void testShouldSuccessfullyMergeIntoTwoCampaignProperties() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
    CampaignProperties campaignPropertiesTest2 = new CampaignProperties();

    LookupTable lookupTableTest = new LookupTable();
    lookupTableTest.setName("lookupTableTest");

    String remoteRepoTest1 = "remoteRepoTest1.com";
    String remoteRepoTest2 = "remoteRepoTest2.com";

    campaignPropertiesTest1.getRemoteRepositoryList().add(remoteRepoTest1);

    campaignPropertiesTest2.getRemoteRepositoryList().add(remoteRepoTest1);
    campaignPropertiesTest2.getRemoteRepositoryList().add(remoteRepoTest2);

    Map<String, LookupTable> mapTest = new HashMap<>();

    mapTest.put("1", lookupTableTest);

    campaignPropertiesTest1.setLookupTableMap(mapTest);
    campaignPropertiesTest1.mergeInto(campaignPropertiesTest2);

    assertEquals(campaignPropertiesTest2.getLookupTableMap().get("1"), lookupTableTest);
    assertEquals(
        campaignPropertiesTest2.getRemoteRepositoryList(),
        campaignPropertiesTest1.getRemoteRepositoryList());
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
  public void testShouldSetInitiativeOwnerPermissions() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();

    campaignPropertiesTest1.setInitiativeOwnerPermissions(true);
    assertTrue(campaignPropertiesTest1.isInitiativeOwnerPermissions());

    campaignPropertiesTest1.setInitiativeOwnerPermissions(false);
    assertFalse(campaignPropertiesTest1.isInitiativeOwnerPermissions());
  }

  @Test
  public void testShouldReadResolveCampaignProperties() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
    Object readResolveTest = campaignPropertiesTest1.readResolve();

    assertNotNull(readResolveTest);
  }

  @Test
  public void testShouldSetRemoteRepositoryListFromList() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
    int expectedSize = 5;

    List<String> testRemoteRepoList = new ArrayList<>();

    testRemoteRepoList.add("remoteRepoTest1.com");
    testRemoteRepoList.add("remoteRepoTest2.com");
    testRemoteRepoList.add("remoteRepoTest3.com");
    testRemoteRepoList.add("remoteRepoTest4.com");
    testRemoteRepoList.add("remoteRepoTest5.com");

    campaignPropertiesTest1.setRemoteRepositoryList(testRemoteRepoList);

    assertEquals(testRemoteRepoList, campaignPropertiesTest1.getRemoteRepositoryList());
    assertEquals(expectedSize, campaignPropertiesTest1.getRemoteRepositoryList().size());
  }

  @Test
  public void testShouldSetDefaultStatSheetPropertiesForCampaign() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();
    StatSheetProperties statSheetPropertiesTest1 =
        new StatSheetProperties("test", StatSheetLocation.BOTTOM_RIGHT);
    campaignPropertiesTest1.setTokenTypeDefaultStatSheet("test", statSheetPropertiesTest1);

    campaignPropertiesTest1.getTokenTypeDefaultStatSheet("test");

    assertEquals(
        statSheetPropertiesTest1, campaignPropertiesTest1.getTokenTypeDefaultStatSheet("test"));
  }

  @Test
  public void testShouldConvertCampaignPropertiesToDtoAndBackAgain() {
    CampaignProperties campaignPropertiesTest1 = new CampaignProperties();

    StatSheetProperties statSheetPropertiesTest1 =
        new StatSheetProperties("test", StatSheetLocation.BOTTOM_RIGHT);
    campaignPropertiesTest1.setTokenTypeDefaultStatSheet("test", statSheetPropertiesTest1);

    CampaignPropertiesDto campaignPropertiesToDt0 = campaignPropertiesTest1.toDto();

    assertNotNull(campaignPropertiesToDt0);
    assertInstanceOf(CampaignPropertiesDto.class, campaignPropertiesToDt0);

    CampaignProperties campaignPropertiesFromDto =
        CampaignProperties.fromDto(campaignPropertiesToDt0);

    assertNotNull(campaignPropertiesFromDto);
    assertInstanceOf(CampaignProperties.class, campaignPropertiesFromDto);
  }
}
