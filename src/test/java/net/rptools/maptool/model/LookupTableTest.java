package net.rptools.maptool.model;

import net.rptools.lib.MD5Key;
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
    public void ShouldAddMultipleValidTableEntries() {
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

        lookupTableTest.addEntry(1, 5, "entry1", imageKeyTest);
        lookupTableTest.addEntry(6, 10, "entry2", imageKeyTest);
        lookupTableTest.addEntry(11, 20, "entry3", imageKeyTest);

        String expectedEntry1 = "entry1";
        String expectedEntry2 = "entry2";
        String expectedEntry3 = "entry3";

        int randomRoll = ThreadLocalRandom.current().nextInt(1, 20);

        if (randomRoll >= 1 && randomRoll < 6) {
            assertEquals(expectedEntry1, lookupTableTest.getLookup(Integer.toString(randomRoll)).getValue());
        }

        if (randomRoll >= 6 && randomRoll < 11) {
            assertEquals(expectedEntry2, lookupTableTest.getLookup(Integer.toString(randomRoll)).getValue());
        }

        if (randomRoll >= 11 && randomRoll < 21) {
            assertEquals(expectedEntry3, lookupTableTest.getLookup(Integer.toString(randomRoll)).getValue());
        }
    }

}
