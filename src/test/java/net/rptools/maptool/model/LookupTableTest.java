package net.rptools.maptool.model;

import net.rptools.lib.MD5Key;
import net.rptools.maptool.client.MapTool;
import net.rptools.parser.ParserException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.*;

import static org.junit.jupiter.api.Assertions.*;

public class LookupTableTest {


    @Test
    public void testShouldCreateLookupTableBaseLine() {
        LookupTable lookupTableTest = new LookupTable();
        assertNotNull(lookupTableTest);
    }

    @Test
    public void testShouldSetTableImage() {
        LookupTable lookupTableTest = new LookupTable();
        MD5Key imageKeyTest = new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());

        lookupTableTest.setTableImage(imageKeyTest);
        MD5Key actualImageKey = lookupTableTest.getTableImage();

        assertEquals(imageKeyTest, actualImageKey);
    }

    @Test
    public void testShouldAddValidTableEntry() {
        LookupTable lookupTableTest = new LookupTable();
        MD5Key imageKeyTest = new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());

        lookupTableTest.addEntry(1, 20, "test", imageKeyTest);

        int expectedEntryCount = 1;
        int actualEntryCount = lookupTableTest.getEntryList().size();

        assertEquals(expectedEntryCount, actualEntryCount);
    }

    @Test
    public void testShouldAddMultipleValidTableEntries() {
        LookupTable lookupTableTest = new LookupTable();
        MD5Key imageKeyTest = new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());

        lookupTableTest.addEntry(1, 2, "entry1", imageKeyTest);
        lookupTableTest.addEntry(3, 4, "entry2", imageKeyTest);
        lookupTableTest.addEntry(5, 6, "entry3", imageKeyTest);
        lookupTableTest.addEntry(7, 8, "entry4", imageKeyTest);
        lookupTableTest.addEntry(9, 10, "entry5", imageKeyTest);
        lookupTableTest.addEntry(11, 12, "entry6", imageKeyTest);
        lookupTableTest.addEntry(13, 14, "entry7", imageKeyTest);
        lookupTableTest.addEntry(15, 16, "entry8", imageKeyTest);
        lookupTableTest.addEntry(17, 18, "entry9", imageKeyTest);
        lookupTableTest.addEntry(19, 20, "entry10", imageKeyTest);

        int expectedEntryCount = 10;
        int actualEntryCount = lookupTableTest.getEntryList().size();

        assertEquals(expectedEntryCount, actualEntryCount);
    }

    @Test
    public void testShouldGetCorrectTableEntryFromRandomRollsWithinRange() throws ParserException {
        LookupTable lookupTableTest = new LookupTable();
        MD5Key imageKeyTest = new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());
        int rollCount = 50;

        lookupTableTest.addEntry(1, 5, "entry1", imageKeyTest);
        lookupTableTest.addEntry(6, 10, "entry2", imageKeyTest);
        lookupTableTest.addEntry(11, 20, "entry3", imageKeyTest);

        String expectedEntry1 = "entry1";
        String expectedEntry2 = "entry2";
        String expectedEntry3 = "entry3";

        for (int i = 0; i < rollCount; i++) {

            int randomRoll = ThreadLocalRandom.current().nextInt(1, 20);
            String actualEntry = lookupTableTest.getLookup(Integer.toString(randomRoll)).getValue();

            if (randomRoll >= 1 && randomRoll < 6) {
                assertEquals(expectedEntry1, actualEntry);
            }

            if (randomRoll >= 6 && randomRoll < 11) {
                assertEquals(expectedEntry2, actualEntry);
            }

            if (randomRoll >= 11 && randomRoll < 21) {
                assertEquals(expectedEntry3, actualEntry);
            }
        }
    }

    @Test
    public void testLookupTableNameLengthValidation() {
        LookupTable lookupTableTest1 = new LookupTable();
        lookupTableTest1.setName("");

        LookupTable lookupTableTest2 = new LookupTable();
        lookupTableTest2.setName(" ");

        LookupTable lookupTableTest3 = new LookupTable();
        lookupTableTest3.setName("test123");

        LookupTable lookupTableTest4 = new LookupTable();
        lookupTableTest4.setName("abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWZY23456789+-@#!");

        assertFalse(lookupTableTest1.getName().trim().length() > 0);
        assertFalse(lookupTableTest2.getName().trim().length() > 0);
        assertTrue(lookupTableTest3.getName().trim().length() > 0);
        assertTrue(lookupTableTest4.getName().trim().length() > 0);
    }

    @Test
    public void testLookupTableShouldClearAllEntries() {
        LookupTable lookupTableTest = new LookupTable();
        MD5Key imageKeyTest = new MD5Key(new File("src/test/resources/circleToken.png").getAbsolutePath().getBytes());

        lookupTableTest.addEntry(1, 2, "entry1", imageKeyTest);
        lookupTableTest.addEntry(3, 4, "entry2", imageKeyTest);
        lookupTableTest.addEntry(5, 6, "entry3", imageKeyTest);
        lookupTableTest.addEntry(7, 8, "entry4", imageKeyTest);
        lookupTableTest.addEntry(9, 10, "entry5", imageKeyTest);

        assertEquals(5, lookupTableTest.getEntryList().size());

        lookupTableTest.clearEntries();

        assertEquals(0, lookupTableTest.getEntryList().size());

    }
}
