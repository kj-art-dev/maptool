package net.rptools.maptool.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** @kj-art-dev: Doing some basic testing here.*/
public class PasswordGeneratorTest {

    @Test
    void testGenerateValidPasswordDefault() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        String generatedPassword = passwordGenerator.getPassword();

        assertTrue(generatedPassword.length() > 15 && generatedPassword.length() < 30);
    }

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

        int minLength = 15;
        int maxLength = 30;

        String generatedPassword = passwordGenerator.getPassword(minLength, maxLength);

        assertTrue(generatedPassword.length() >= minLength && generatedPassword.length() <= maxLength);
    }

    @Test
    void testGenerateValidPasswordUpperRange() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int minLength = 31;
        int maxLength = 46;

        String generatedPassword = passwordGenerator.getPassword(minLength, maxLength);

        assertFalse(generatedPassword.length() >= 15 && generatedPassword.length() <= 30);
    }

    @Test
    void testGenerateValidPasswordLowerRange() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int minLength = 0;
        int maxLength = 14;

        String generatedPassword = passwordGenerator.getPassword(minLength, maxLength);

        assertFalse(generatedPassword.length() >= 15 && generatedPassword.length() <= 30);
    }

    @Test
    void testAttemptGeneratePasswordWithInvalidLength() {
        PasswordGenerator passwordGenerator = new PasswordGenerator();

        int invalidLength = 0;

        String generatedPassword = passwordGenerator.getPassword(invalidLength, invalidLength);

        assertEquals(invalidLength, generatedPassword.length());
    }
}
