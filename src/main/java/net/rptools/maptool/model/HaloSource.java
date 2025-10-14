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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.StringValue;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.server.proto.HaloSourceDto;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a halo source that can be attached to tokens, a halo source can contain one or more
 * halo segments.
 *
 * <p>This class is immutable.
 */
public final class HaloSource implements Comparable<HaloSource>, Serializable {

  private static final Logger log = LogManager.getLogger(HaloSource.class);

  private final @Nullable String name;
  private final @Nullable GUID id;
  private final boolean scaleWithToken;

  /** The halo segments that make up the halo source. */
  private final @Nonnull List<Halo> haloList;

  public HaloSource(
      @Nullable String name, @Nullable GUID id, boolean scaleWithToken, @Nonnull List<Halo> halos) {
    this.name = name;
    this.id = id;
    this.scaleWithToken = scaleWithToken;
    this.haloList = halos;
  }

  @Serial
  public Object writeReplace() {
    // Make sure XStream keeps the serialization nice. We don't need the XML to contain
    // implementation details of the ImmutableList in use.
    return new HaloSource(name, id, scaleWithToken, new ArrayList<>(haloList));
  }

  @SuppressWarnings("ConstantConditions")
  @Serial
  private @Nonnull Object readResolve() {
    final List<Halo> halos = Objects.requireNonNullElse(haloList, Collections.emptyList());
    // Rather than modifying the current object, we'll create a replacement that is definitely
    // initialized properly.
    return new HaloSource(this.name, this.id, this.scaleWithToken, ImmutableList.copyOf(halos));
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof HaloSource)) {
      return false;
    }
    return Objects.equals(((HaloSource) obj).id, id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  public @Nullable GUID getId() {
    return id;
  }

  public @Nullable String getName() {
    return name;
  }

  /**
   * @return A read-only list of halos belonging to this HaloSource
   */
  public @Nonnull List<Halo> getHaloList() {
    return haloList;
  }

  /**
   * @return The maximum width of all the halo segments listed within this halo source
   */
  public @Nonnull Integer getMaxWidth() {
    Integer maxWidth = null;
    long countNulls = 0L;

    try {
      maxWidth =
          haloList.stream()
              .filter(w -> w.getWidth() != null)
              .max(Comparator.comparing(Halo::getWidth))
              .get()
              .getWidth();
      countNulls = haloList.stream().filter(w -> w.getWidth() == null).count();
    } catch (NoSuchElementException e) {
      maxWidth = AppPreferences.haloLineWidth.get();
    }

    if ((maxWidth < AppPreferences.haloLineWidth.get() && countNulls > 0L) || maxWidth == null) {
      maxWidth = AppPreferences.haloLineWidth.get();
    }

    return maxWidth;
  }

  public boolean isScaleWithToken() {
    return scaleWithToken;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Compares this halo source with another.
   *
   * <p>Halo sources are compared by name. If both names are numeric strings, they will be compared
   * as integers. Otherwise they will be compared lexicographically. *
   *
   * @param o the other HaloSource.
   * @return the comparison value.
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(@Nonnull HaloSource o) {
    if (o != this) {
      int nameLong = NumberUtils.toInt(name, Integer.MIN_VALUE);
      int onameLong = NumberUtils.toInt(o.name, Integer.MIN_VALUE);
      if (nameLong != Integer.MIN_VALUE && onameLong != Integer.MIN_VALUE)
        return nameLong - onameLong;
      return name.compareTo(o.name);
    }
    return 0;
  }

  public static @Nonnull HaloSource fromDto(@Nonnull HaloSourceDto dto) {
    return new HaloSource(
        dto.hasName() ? dto.getName().getValue() : null,
        dto.hasId() ? GUID.valueOf(dto.getId().getValue()) : null,
        dto.getScaleWithToken(),
        dto.getHalosList().stream().map(Halo::fromDto).collect(ImmutableList.toImmutableList()));
  }

  public @Nonnull HaloSourceDto toDto() {
    var dto = HaloSourceDto.newBuilder();
    if (name != null) {
      dto.setName(StringValue.of(name));
    }
    if (id != null) {
      dto.setId(StringValue.of(id.toString()));
    }
    dto.setScaleWithToken(scaleWithToken);
    dto.addAllHalos(haloList.stream().map(l -> l.toDto()).collect(Collectors.toList()));
    return dto.build();
  }
}
