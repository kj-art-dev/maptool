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
package net.rptools.maptool.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

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
