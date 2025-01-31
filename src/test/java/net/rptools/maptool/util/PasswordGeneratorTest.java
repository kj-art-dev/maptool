package net.rptools.maptool.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** @kj-art-dev: Doing some basic testing here.*/
public class PasswordGeneratorTest {

    @Test
    void testGenerateValidPasswordWithFixedLength() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int testLength = 15;

        String generatedPassword = passwordGenerator.getPassword(testLength, testLength);

        assertEquals(testLength, generatedPassword.length());
    }

    @Test
    void testGenerateValidPasswordWithDefaultLengthRange() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int minLength = 10;
        int maxLength = 30;

        String generatedPassword = passwordGenerator.getPassword(minLength, maxLength);

        assertTrue(generatedPassword.length() >= minLength && generatedPassword.length() <= maxLength);
    }

    @Test
    void testAttemptGeneratePasswordWithInvalidLength() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int invalidLength = 0;

        String generatedPassword = passwordGenerator.getPassword(invalidLength, invalidLength);

        assertEquals(invalidLength, generatedPassword.length());
    }

    @Test
    void testGenerateValidPasswordWithInvalidRange() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int minLength = -100;
        int maxLength = 30;

        assertThrows(IllegalArgumentException.class, () -> passwordGenerator.getPassword(minLength, maxLength));
    }
}
