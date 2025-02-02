package net.rptools.maptool.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * @kj-art-dev: Doing some basic testing here.
 */
public class PasswordGeneratorTest {

    private final PasswordGenerator testPasswordGenerator = new PasswordGenerator();

    private static final String ELIGIBLE_CHARACTERS =
            "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWZY23456789+-@#!";

    @Test
    void testGeneratePasswordWithValidRange() {
        String testGeneratedPassword = testPasswordGenerator.getPassword();

        int actualPasswordLength = testGeneratedPassword.length();

        assertTrue(actualPasswordLength >= 15 && testGeneratedPassword.length() <= 30);
    }

    @Test
    void testGeneratePasswordWithValidCharacters() {
        String testGeneratedPassword = testPasswordGenerator.getPassword();

        for (char c : testGeneratedPassword.toCharArray()) {
            assertTrue(ELIGIBLE_CHARACTERS.indexOf(c) >= 0);
        }
    }

    @Test
    void testGeneratePasswordsThatAreUnique() {
        Set<String> generatedPasswords = new HashSet<>();
        int generatedPasswordCount = 100;

        for (int i = 0; i < generatedPasswordCount; i++) {
            generatedPasswords.add(testPasswordGenerator.getPassword());
        }
        assertEquals(generatedPasswordCount, generatedPasswords.size());
    }
}
