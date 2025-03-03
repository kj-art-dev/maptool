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

import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import javax.crypto.NoSuchPaddingException;
import net.rptools.lib.MD5Key;
import net.rptools.maptool.util.cipher.CipherUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito.*;

class PlayersTest {

  class PlayerDatabaseStub implements PlayerDatabase {

    @Override
    public boolean playerExists(String playerName) {
      return false;
    }

    @Override
    public Player getPlayer(String playerName)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
      return mock(Player.class);
    }

    @Override
    public boolean supportsDisabling() {
      return false;
    }

    @Override
    public boolean supportsAsymmetricalKeys() {
      return false;
    }

    @Override
    public boolean supportsRolePasswords() {
      return false;
    }

    @Override
    public boolean isBlocked(Player player) {
      return false;
    }

    @Override
    public String getBlockedReason(Player player) {
      return "Just been blocked";
    }

    @Override
    public Set<Player> getOnlinePlayers() {
      return Set.of();
    }

    @Override
    public AuthMethod getAuthMethod(Player player) {
      return null;
    }

    @Override
    public CompletableFuture<CipherUtil.Key> getPublicKey(Player player, MD5Key md5key)
        throws ExecutionException, InterruptedException {
      return null;
    }

    @Override
    public Set<String> getEncodedPublicKeys(String name) {
      return Set.of();
    }

    @Override
    public CompletableFuture<Boolean> hasPublicKey(Player player, MD5Key md5key) {
      return null;
    }

    @Override
    public boolean isPlayerRegistered(String name)
        throws InterruptedException, InvocationTargetException {
      return false;
    }

    @Override
    public void playerSignedIn(Player player) {}

    @Override
    public void playerSignedOut(Player player) {}

    @Override
    public boolean isPlayerConnected(String name) {
      return false;
    }
  }

  class PersistedPlayerDatabaseStub implements PersistedPlayerDatabase {

    @Override
    public void disablePlayer(String player, String reason) throws PasswordDatabaseException {}

    @Override
    public void addPlayerSharedPassword(String name, Player.Role role, String password)
        throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            NoSuchPaddingException,
            InvalidKeyException {}

    @Override
    public void addPlayerAsymmetricKey(String name, Player.Role role, Set<String> publicKeyStrings)
        throws NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {}

    @Override
    public void setSharedPassword(String name, String password)
        throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            InvalidKeyException {}

    @Override
    public void setAsymmetricKeys(String name, Set<String> keys)
        throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {}

    @Override
    public void addAsymmetricKeys(String name, Set<String> keys)
        throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            InvalidKeyException,
            InvalidAlgorithmParameterException {}

    @Override
    public boolean isPersisted(String name) {
      return false;
    }

    @Override
    public void deletePlayer(String name) {}

    @Override
    public void blockPlayer(String name, String reason) {}

    @Override
    public void unblockPlayer(String name) {}

    @Override
    public void setRole(String name, Player.Role role) {}

    @Override
    public void commitChanges()
        throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            InvalidKeyException {}

    @Override
    public void rollbackChanges()
        throws NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidKeySpecException,
            PasswordDatabaseException,
            InvalidKeyException {}

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {}

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {}

    @Override
    public boolean playerExists(String playerName) {
      return false;
    }

    @Override
    public Player getPlayer(String playerName)
        throws NoSuchAlgorithmException, InvalidKeySpecException {
      return null;
    }

    @Override
    public boolean supportsDisabling() {
      return false;
    }

    @Override
    public boolean supportsAsymmetricalKeys() {
      return false;
    }

    @Override
    public boolean supportsRolePasswords() {
      return false;
    }

    @Override
    public boolean isBlocked(Player player) {
      return false;
    }

    @Override
    public String getBlockedReason(Player player) {
      return "";
    }

    @Override
    public Set<Player> getOnlinePlayers() {
      return Set.of();
    }

    @Override
    public AuthMethod getAuthMethod(Player player) {
      return null;
    }

    @Override
    public CompletableFuture<CipherUtil.Key> getPublicKey(Player player, MD5Key md5key)
        throws ExecutionException, InterruptedException {
      return null;
    }

    @Override
    public Set<String> getEncodedPublicKeys(String name) {
      return Set.of();
    }

    @Override
    public CompletableFuture<Boolean> hasPublicKey(Player player, MD5Key md5key) {
      return null;
    }

    @Override
    public boolean isPlayerRegistered(String name)
        throws InterruptedException, InvocationTargetException {
      return false;
    }

    @Override
    public void playerSignedIn(Player player) {}

    @Override
    public void playerSignedOut(Player player) {}

    @Override
    public boolean isPlayerConnected(String name) {
      return false;
    }
  }

  @Test
  void testShouldCreateValidPlayersObject() {
    PlayerDatabaseStub mockPlayerDatabase = new PlayerDatabaseStub();
    Players testPlayers = new Players(mockPlayerDatabase);

    assertNotNull(testPlayers);
  }

  //  @Test
  //  void testShouldGetPlayer() {
  //    PlayerDatabaseStub playerDatabase = new PlayerDatabaseStub();
  //    Players testPlayers = new Players(playerDatabase);
  //
  //    PlayerDatabase mockPlayerDatabase = Mockito.mock(PlayerDatabase.class);
  //    Players mockPlayers = Mockito.mock(Players.class);
  //
  //    CompletableFuture<PlayerInfo> mockPlayerInfoFuture = Mockito.mock(CompletableFuture.class);
  //
  //    when(mockPlayers.getPlayer("John Doe").thenAccept(mockPlayerInfoFuture::complete));
  //
  //    assertEquals(mockPlayers, testPlayers);
  //    //        assertEquals(mockPlayers, testPlayers.getPlayer("John Doe"));
  //
  //  }

  @Test
  void testGetPlayerEmpty() {
    PlayerDatabaseStub playerDatabase = new PlayerDatabaseStub();
    Players testPlayers = new Players(playerDatabase);

    assertNotNull(testPlayers.getPlayer());
  }

  @Test
  void getConnectedPlayers() {
    PlayerDatabaseStub playerDatabase = new PlayerDatabaseStub();
    Players testPlayers = new Players(playerDatabase);

    assertNotNull(testPlayers.getConnectedPlayers());
  }

  @Test
  void getDatabasePlayers() {
    PlayerDatabaseStub playerDatabase = new PlayerDatabaseStub();
    Players testPlayers = new Players(playerDatabase);

    assertNotNull(testPlayers.getDatabasePlayers());
  }

  @Test
  void addPlayerWithPassword() {}

  @Test
  void addPlayerWithPublicKey() {}

  @Test
  void setPublicKeys() {}

  @Test
  void setPassword() {
    PersistedPlayerDatabaseStub persistedPlayerDatabaseStub = new PersistedPlayerDatabaseStub();
    Players testPlayers = new Players(persistedPlayerDatabaseStub);

    assertEquals(
        Players.ChangePlayerStatus.OK, testPlayers.setPassword("John Doe", "test_password"));
  }

  @Test
  void setRole() {
    PersistedPlayerDatabaseStub persistedPlayerDatabaseStub = new PersistedPlayerDatabaseStub();
    Players testPlayers = new Players(persistedPlayerDatabaseStub);

    assertEquals(
        Players.ChangePlayerStatus.OK, testPlayers.setRole("John Doe", Player.Role.PLAYER));
  }

  @Test
  void blockPlayer() {
    PersistedPlayerDatabaseStub persistedPlayerDatabaseStub = new PersistedPlayerDatabaseStub();
    Players testPlayers = new Players(persistedPlayerDatabaseStub);

    assertEquals(
        Players.ChangePlayerStatus.OK, testPlayers.blockPlayer("John Doe", "Just been blocked"));
  }

  @Test
  void unblockPlayer() {
    PersistedPlayerDatabaseStub persistedPlayerDatabaseStub = new PersistedPlayerDatabaseStub();
    Players testPlayers = new Players(persistedPlayerDatabaseStub);

    assertEquals(Players.ChangePlayerStatus.OK, testPlayers.unblockPlayer("John Doe"));
  }
}
