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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * A collection of {@link HaloSource} with unique IDs.
 *
 * <p>When adding a {@code HaloSource}, if the collection already has one with the same ID it will
 * be replaced.
 */
public class Halos extends AbstractSet<HaloSource> {
  public static Halos copyOf(Iterable<HaloSource> sources) {
    var halos = new Halos();
    for (var source : sources) {
      halos.add(source);
    }
    return halos;
  }

  private final LinkedHashMap<GUID, HaloSource> sources;

  public Halos() {
    this.sources = new LinkedHashMap<>();
  }

  public Halos(Halos other) {
    this();
    addAll(other);
  }

  @Override
  public int size() {
    return sources.size();
  }

  @Override
  public void clear() {
    sources.clear();
  }

  @Override
  public boolean contains(Object o) {
    return o instanceof HaloSource haloSource && this.sources.containsKey(haloSource.getId());
  }

  @Override
  public boolean add(HaloSource haloSource) {
    sources.put(haloSource.getId(), haloSource);
    return true;
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof HaloSource haloSource) {
      var previous = sources.remove(haloSource.getId());
      return previous != null;
    }
    return false;
  }

  @Override
  public @Nonnull Iterator<HaloSource> iterator() {
    return sources.values().iterator();
  }

  public Optional<HaloSource> get(GUID haloSourceId) {
    return Optional.ofNullable(sources.get(haloSourceId));
  }
}
