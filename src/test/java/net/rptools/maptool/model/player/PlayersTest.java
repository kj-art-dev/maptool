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
package net.rptools.maptool.model.player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.NoSuchPaddingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PlayersTest {

  @Mock private PlayerDatabase mockPlayerDatabase;

  @InjectMocks private Players players;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testShouldAddPlayerWithPassword() {
    players = new Players(mockPlayerDatabase);

    players.addPlayerWithPassword("John Doe", Player.Role.PLAYER, "password");

    assertNotNull(players.getPlayer("John Doe"));

    verify(mockPlayerDatabase, times(1)).playerExists("John Doe");
  }

  @Test
  void testShouldAddPlayerWithPublicKey() {
    players = new Players(mockPlayerDatabase);

    players.addPlayerWithPublicKey("John Doe", Player.Role.PLAYER, "publicKey");

    assertNotNull(players.getPlayer("John Doe"));

    verify(mockPlayerDatabase, times(1)).playerExists("John Doe");
  }

  @Test
  void testShouldSetPublicKeys()
      throws InvalidAlgorithmParameterException,
          NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidKeySpecException,
          PasswordDatabaseException,
          InvalidKeyException {

    String testPublicKeyString = "mockPublicKey";

    when(mockPlayerDatabase.supportsAsymmetricalKeys()).thenReturn(true);

    Players.ChangePlayerStatus testPlayerStatus =
        players.setPublicKeys("John Doe", testPublicKeyString);

    assertEquals(Players.ChangePlayerStatus.NOT_SUPPORTED, testPlayerStatus);
    verify(mockPlayerDatabase).supportsAsymmetricalKeys();
  }

  @Test
  void testShouldSetPasswordAndVerify()
      throws NoSuchPaddingException,
          NoSuchAlgorithmException,
          InvalidKeySpecException,
          PasswordDatabaseException,
          InvalidKeyException {

    PersistedPlayerDatabase persistedPlayerDatabase = mock(PersistedPlayerDatabase.class);

    players = new Players(persistedPlayerDatabase);

    Players.ChangePlayerStatus result = players.setPassword("testUser", "testPassword");

    assertEquals(Players.ChangePlayerStatus.OK, result);

    verify(persistedPlayerDatabase, times(1)).setSharedPassword("testUser", "testPassword");
  }

  @Test
  void testShouldSetRoleAndReturnOKPlayerStatus() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);

    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(Players.ChangePlayerStatus.OK, players.setRole("John Doe", Player.Role.PLAYER));

    verify(mockPersistedPlayerDatabase, times(1)).setRole("John Doe", Player.Role.PLAYER);
  }

  @Test
  void testShouldBlockPlayerThenVerifyDatabaseCall() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);
    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(
        Players.ChangePlayerStatus.OK, players.blockPlayer("John Doe", "Just been blocked"));

    verify(mockPersistedPlayerDatabase, times(1)).blockPlayer(anyString(), anyString());
  }

  @Test
  void testShouldUnblockPlayerThenVerifyDatabaseCall() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);
    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(Players.ChangePlayerStatus.OK, players.unblockPlayer("John Doe"));

    verify(mockPersistedPlayerDatabase, times(1)).unblockPlayer(anyString());
  }
}
