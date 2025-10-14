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

import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** A collection of {@link HaloSource} grouped into categories. */
public class CategorizedHalos {
  public record Category(String name, Halos halos) {}

  public static CategorizedHalos copyOf(Map<String, ? extends Map<GUID, HaloSource>> sources) {
    var categorized = new CategorizedHalos();
    for (var entry : sources.entrySet()) {
      categorized.addAllToCategory(entry.getKey(), entry.getValue().values());
    }
    return categorized;
  }

  private final TreeMap<String, Halos> allHalos;

  public CategorizedHalos() {
    allHalos = new TreeMap<>();
  }

  public CategorizedHalos(CategorizedHalos other) {
    this();
    for (var entry : other.allHalos.entrySet()) {
      allHalos.put(entry.getKey(), new Halos(entry.getValue()));
    }
  }

  public boolean isEmpty() {
    return allHalos.isEmpty();
  }

  public void clear() {
    allHalos.clear();
  }

  public Iterable<Category> getCategories() {
    return Iterables.transform(allHalos.entrySet(), e -> new Category(e.getKey(), e.getValue()));
  }

  /**
   * Looks up a category, adding it if it doesn't already exist.
   *
   * @param name The name of the category to find.
   * @return The existing category if present, otherwise the new category.
   */
  public Optional<Category> getCategory(String name) {
    var halos = this.allHalos.get(name);
    if (halos == null) {
      return Optional.empty();
    }

    return Optional.of(new Category(name, halos));
  }

  public void addToCategory(String categoryName, HaloSource halo) {
    addAllToCategory(categoryName, List.of(halo));
  }

  public void addAllToCategory(String categoryName, Collection<HaloSource> halos) {
    if (halos.isEmpty()) {
      // Don't create a category if we don't have to.
      return;
    }
    allHalos.computeIfAbsent(categoryName, c -> new Halos()).addAll(halos);
  }

  public void addAll(CategorizedHalos other) {
    for (var entry : other.allHalos.entrySet()) {
      addAllToCategory(entry.getKey(), entry.getValue());
    }
  }
}
