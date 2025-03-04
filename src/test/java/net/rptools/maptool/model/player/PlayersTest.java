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
  void testShouldGetPlayerInfo() throws NoSuchAlgorithmException, InvalidKeySpecException {

    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);

    when(mockPlayerDatabase.playerExists("John Doe")).thenReturn(true);

    assertTrue(mockPlayerDatabase.playerExists("John Doe"));

    verify(mockPlayerDatabase, times(1)).playerExists("John Doe");
  }

  @Test
  void testGetPlayerEmpty() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);
    players = new Players(mockPlayerDatabase);

    assertNotNull(players.getPlayer());
  }

  @Test
  void getConnectedPlayers() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);
    players = new Players(mockPlayerDatabase);

    assertNotNull(players.getConnectedPlayers());
  }

  @Test
  void getDatabasePlayers() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);
    players = new Players(mockPlayerDatabase);

    assertNotNull(players.getDatabasePlayers());

    verify(mockPlayerDatabase, times(1)).getAllPlayers();
  }

  @Test
  void addPlayerWithPassword() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);
    players = new Players(mockPlayerDatabase);

    players.addPlayerWithPassword("John Doe", Player.Role.PLAYER, "password");

    assertNotNull(players.getPlayer("John Doe"));

    verify(mockPlayerDatabase, times(1)).playerExists("John Doe");
  }

  @Test
  void addPlayerWithPublicKey() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);
    players = new Players(mockPlayerDatabase);

    players.addPlayerWithPublicKey("John Doe", Player.Role.PLAYER, "publicKey");

    assertNotNull(players.getPlayer("John Doe"));

    verify(mockPlayerDatabase, times(1)).playerExists("John Doe");
  }

  @Test
  void setPublicKeys() {
    PlayerDatabase mockPlayerDatabase = mock(PlayerDatabase.class);

    players = new Players(mockPlayerDatabase);

    assertEquals(
        Players.ChangePlayerStatus.NOT_SUPPORTED, players.setPublicKeys("John Doe", "publicKey"));
  }

  @Test
  void setPassword()
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
  void setRole() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);

    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(Players.ChangePlayerStatus.OK, players.setRole("John Doe", Player.Role.PLAYER));

    verify(mockPersistedPlayerDatabase, times(1)).setRole("John Doe", Player.Role.PLAYER);
  }

  @Test
  void blockPlayer() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);
    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(
        Players.ChangePlayerStatus.OK, players.blockPlayer("John Doe", "Just been blocked"));

    verify(mockPersistedPlayerDatabase, times(1)).blockPlayer(anyString(), anyString());
  }

  @Test
  void unblockPlayer() {
    PersistedPlayerDatabase mockPersistedPlayerDatabase = mock(PersistedPlayerDatabase.class);
    players = new Players(mockPersistedPlayerDatabase);

    assertEquals(Players.ChangePlayerStatus.OK, players.unblockPlayer("John Doe"));

    verify(mockPersistedPlayerDatabase, times(1)).unblockPlayer(anyString());
  }
}
