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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.rptools.maptool.server.proto.AttachedHaloSourceDto;

public final class AttachedHaloSource {

  private final @Nonnull GUID haloSourceId;

  public AttachedHaloSource(@Nonnull GUID haloSourceId) {
    this.haloSourceId = haloSourceId;
  }

  /**
   * Get the ID of the attached halo source.
   *
   * <p>If you're trying to use this to look up a {@link net.rptools.maptool.model.HaloSource},
   * consider using {@link #resolve(Token, Campaign)} instead. If you're trying to compare to
   * another {@code GUID}, consider using {@link #matches(GUID)}.
   *
   * @return The ID of the attached halo source.
   */
  public GUID getId() {
    return haloSourceId;
  }

  /**
   * Obtain the attached {@code HaloSource} from the token or campaign.
   *
   * @param token The token in which to look up halo source IDs.
   * @param campaign The campaign in which to look up halo source IDs.
   * @return The {@code HaloSource} referenced by this {@code AttachedHaloSource}, or {@code null}
   *     if no such halo source exists.
   */
  public @Nullable HaloSource resolve(Token token, Campaign campaign) {

    for (CategorizedHalos.Category category : campaign.getHaloSources().getCategories()) {
      var source = category.halos().get(haloSourceId);
      if (source.isPresent()) {
        return source.get();
      }
    }

    return null;
  }

  /**
   * Check if this {@code AttachedHaloSource} references a {@code HaloSource} with a matching ID.
   *
   * @param haloSourceId The ID of the halo source to match against.
   * @return {@code true} If {@code haloSourceId} is the same as the ID of the attached halo source.
   */
  public boolean matches(@Nonnull GUID haloSourceId) {
    return haloSourceId.equals(this.haloSourceId);
  }

  public static AttachedHaloSource fromDto(AttachedHaloSourceDto dto) {
    return new AttachedHaloSource(GUID.valueOf(dto.getHaloSourceId()));
  }

  public AttachedHaloSourceDto toDto() {
    var dto = AttachedHaloSourceDto.newBuilder();
    dto.setHaloSourceId(haloSourceId.toString());
    return dto.build();
  }
}
