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
package net.rptools.maptool.client.ui.zone.renderer;

import com.google.common.eventbus.Subscribe;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import net.rptools.lib.CodeTimer;
import net.rptools.lib.MD5Key;
import net.rptools.maptool.client.AppPreferences;
import net.rptools.maptool.client.AppUtil;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ui.zone.PlayerView;
import net.rptools.maptool.client.ui.zone.ZoneViewModel;
import net.rptools.maptool.client.ui.zone.vbl.TokenVBL;
import net.rptools.maptool.events.MapToolEventBus;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.drawing.DrawableColorPaint;
import net.rptools.maptool.model.zones.GridChanged;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HaloRenderer {
  private final RenderHelper renderHelper;
  private final Zone zone;

  private static final Logger log = LogManager.getLogger(HaloRenderer.class);

  private final Map<CompositeUnitPolygonShapeKey, Shape> haloUnitPolygonShapeMap = new HashMap<>();
  private final Map<CompositeUnitShapeKey, Shape> haloUnitShapeMap = new HashMap<>();
  private final Map<Halo, Double> haloScaleFactorMap = new HashMap<>();
  private int haloLineWidthPreference;

  // used for clipping arc shapes to avoid dangly bits a the end
  private Shape haloClipArcByCircleShape;

  // region These fields need to be recalculated whenever the grid changes.
  private Shape haloGridShapeCache;
  private final Map<CompositeHaloMiniShapeKey, Area> haloMiniShapeMap = new HashMap<>();
  private final Map<MD5Key, Area> haloTopologyAreaMap = new HashMap<>();

  // endregion

  public HaloRenderer(RenderHelper renderHelper, Zone zone) {
    this.renderHelper = renderHelper;
    this.zone = zone;

    new MapToolEventBus().getMainEventBus().register(this);
  }

  @Subscribe
  private void gridChanged(GridChanged event) {
    if (event.zone() != this.zone) {
      return;
    }

    haloTopologyAreaMap.clear();
    haloMiniShapeMap.clear();
    haloGridShapeCache = null;
  }

  /**
   * Loop through each {@link AttachedHaloSource} on the token.
   *
   * @param g2d where to paint
   * @param token the token which may have halos
   * @param position the token's position
   * @param view used for GMs viewing as a player
   */
  public void renderHalos(
      Graphics2D g2d, Token token, ZoneViewModel.TokenPosition position, PlayerView view) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-renderHalos");

    var attachedHaloSources = token.getHaloSources();
    if (token.getHaloColor() == null && token.getHaloSources().isEmpty()) {
      return;
    }

    var grid = zone.getGrid();
    if (grid == null) {
      return;
    }

    if (MapTool.getPlayer().isGM()) {
      // as we have not actually determined halo visibility yet, only provide this for GMs!
      timer.increment("HaloRenderer-renderHalos:tokensWithHalos");
    }

    // used to help determine concentric offsets for multiple halos
    ArrayList<Integer> renderableHaloSourceMaxWidths = new ArrayList<>();

    haloLineWidthPreference =
        AppPreferences.haloLineWidth.get() < 0 ? 0 : AppPreferences.haloLineWidth.get();

    timer.start("HaloRenderer-renderHalos:prepareToRender");
    // Loop through the token's attached halo sources, then associated halos, and determine whether
    // they need to be rendered.  If so, establish where concentrically they need to be rendered and
    // store them for later as we need to render these halo sources in reverse order.
    Map<HaloSource, Map<Halo, Double>> renderableHaloSources = new LinkedHashMap<>();
    if (!attachedHaloSources.isEmpty()) {
      // loop through the halo sources attached to the token
      for (AttachedHaloSource ahs : attachedHaloSources) {
        HaloSource hs = ahs.resolve(token, MapTool.getCampaign());
        if (hs != null) {
          var haloList = hs.getHaloList();
          int maxWidth = 0;

          // do we need to render the halo?
          if (MapTool.getPlayer().isGM() && view.isGMView()
              || ((!MapTool.getPlayer().isGM() || (MapTool.getPlayer().isGM() && !view.isGMView()))
                  && haloList.stream().noneMatch(Halo::isGM))
              || (AppUtil.playerOwns(token) && haloList.stream().anyMatch(Halo::isOwnerOnly))
              || (!AppUtil.playerOwns(token) && haloList.stream().noneMatch(Halo::isOwnerOnly))) {

            timer.increment("HaloRenderer-renderHalos:renderableHaloSources");
            double haloSourceScaleFactor;
            if (hs.isScaleWithToken()) {
              haloSourceScaleFactor =
                  Math.min(
                          position.footprintBounds().getBounds().getHeight(),
                          position.footprintBounds().getBounds().getWidth())
                      / grid.getSize();
            } else {
              haloSourceScaleFactor = 1d;
            }

            Map<Halo, Double> renderableHalos = new LinkedHashMap<>();

            // loop through the individual halo segments in each halo source
            for (Halo h : haloList) {

              /*
              Concentrically offset each halo around the token based on:
              1. the supplied halo offset scaled to the token
              2. the total maximum widths thus far
              3. the number of gaps between halo sources
               */
              double concentricOffset =
                  2d
                      * (h.getOffset() * haloSourceScaleFactor
                          + renderableHaloSourceMaxWidths.stream().reduce(0, Integer::sum)
                          + renderableHaloSourceMaxWidths.size() * haloLineWidthPreference);

              // store the halo its offset for rendering (in order) later...
              renderableHalos.put(h, concentricOffset);

              // store the scale factor for the halo later...
              haloScaleFactorMap.put(h, haloSourceScaleFactor);

              // determine the maximum width we have rendered so far for the halo source
              int width = h.getWidth() == null ? haloLineWidthPreference : h.getWidth();
              maxWidth =
                  Math.max(
                      maxWidth,
                      (int)
                          Math.ceil(
                              (width * h.getScaleY() + h.getOffset()) * haloSourceScaleFactor));
            }

            // store the halo source for rendering (in reverse order) later...
            renderableHaloSources.put(hs, renderableHalos);
          }

          // add the maxWidth to the width accumulator
          if (maxWidth != 0) {
            renderableHaloSourceMaxWidths.add(maxWidth);
          }
        }
      }
    }
    timer.stop("HaloRenderer-renderHalos:prepareToRender");

    // Render the halo sources in reverse order, but their respective halos in order.  This is so
    // any filled outer-concentric halo sources do not graffiti over inner-concentric halos sources.
    timer.start("HaloRenderer-renderHalos:orderedRendering");
    List<HaloSource> haloSourcesToRender = new ArrayList<>(renderableHaloSources.keySet());
    ListIterator<HaloSource> iterator =
        haloSourcesToRender.listIterator(haloSourcesToRender.size());
    while (iterator.hasPrevious()) {
      HaloSource previousHaloSource = iterator.previous();
      for (Map.Entry<Halo, Double> entry :
          renderableHaloSources.get(previousHaloSource).entrySet()) {
        renderHalo(g2d, token, position, grid, entry.getKey(), entry.getValue());
      }
    }
    timer.stop("HaloRenderer-renderHalos:orderedRendering");

    // finally, render any legacy halo in the innermost position
    if (token.getHaloColor() != null) {
      timer.start("HaloRenderer-renderHalos:renderLegacyHalo");
      DrawableColorPaint dcp = new DrawableColorPaint(token.getHaloColor());
      Halo simpleHalo =
          new Halo(
              dcp,
              Halo.HaloShapeType.GRID,
              haloLineWidthPreference,
              null,
              false,
              false,
              false,
              false,
              false,
              0,
              0,
              0,
              1d,
              1d,
              0,
              0,
              0,
              0,
              0,
              0);
      double concentricOffset =
          2d
              * (renderableHaloSourceMaxWidths.stream().reduce(0, Integer::sum)
                  + renderableHaloSourceMaxWidths.size() * haloLineWidthPreference);
      haloScaleFactorMap.put(simpleHalo, 1d);
      renderHalo(g2d, token, position, grid, simpleHalo, concentricOffset);
      timer.stop("HaloRenderer-renderHalos:renderLegacyHalo");
    }

    timer.stop("HaloRenderer-renderHalos");
  }

  /**
   * Render the individual halo.
   *
   * @param g2d where to paint
   * @param token the token which has this halo
   * @param position the token's position
   * @param grid the map's grid
   * @param halo the halo itself
   * @param concentricOffset the distance to offset the next halo
   */
  private void renderHalo(
      Graphics2D g2d,
      Token token,
      ZoneViewModel.TokenPosition position,
      Grid grid,
      Halo halo,
      double concentricOffset) {

    var timer = CodeTimer.get();
    timer.increment("HaloRenderer-renderHalo");
    timer.start("HaloRenderer-renderHalo");

    Halo.HaloShapeType haloShapeType =
        halo.getHaloShapeType() == null ? Halo.DEFAULT_HALO_SHAPE_TYPE : halo.getHaloShapeType();
    ;

    // for TOKEN halo shapes, override the halo shape according to the token's shape
    if (haloShapeType.equals(Halo.HaloShapeType.TOKEN)) {
      if (token.getShape().equals(Token.TokenShape.CIRCLE)
          || token.getShape().equals(Token.TokenShape.TOP_DOWN)) {
        haloShapeType = Halo.HaloShapeType.CIRCLE;
      } else {
        haloShapeType = Halo.HaloShapeType.GRID;
      }
    }

    // count how many of each type of halo we are rendering
    timer.increment(
        String.format("HaloRenderer-renderHalo:%s", haloShapeType.name().toLowerCase()));
    Shape haloShape = null;

    // get the shape of the halo
    switch (haloShapeType) {
      case Halo.HaloShapeType.FOOTPRINT -> {
        if (!GridFactory.getGridType(grid).equals(GridFactory.NONE)) {
          // tokens on gridless maps do not have footprints
          haloShape = getHaloFootprintShape(token, grid, concentricOffset);
        }
      }
      case Halo.HaloShapeType.TOPOLOGY -> {
        haloShape = getHaloTopolopyShape(token);
      }
      case Halo.HaloShapeType.MBL -> {
        if (token.getMaskTopologyTypes().contains(Zone.TopologyType.MBL)) {
          // only render a MBL halo shape if the token has MBL!
          haloShape = token.getTransformedMaskTopology(zone, Zone.TopologyType.MBL);
        }
      }
      case Halo.HaloShapeType.GRID -> {
        haloShape = getHaloGridShape(position, grid, halo, concentricOffset);
      }
      default -> {
        if (halo.getMini() > 0) {
          haloShape = getHaloMiniShape(token, position, grid, halo, concentricOffset);
        } else {
          haloShape = getHaloGeometricShape(position, grid, halo, haloShapeType, concentricOffset);
        }
      }
    }

    final Shape finalHaloShape = haloShape;
    if (finalHaloShape != null) {
      renderHelper.render(
          g2d,
          worldG -> {
            paintHalo(worldG, token, grid, finalHaloShape, halo);
          });
    }

    timer.stop("HaloRenderer-renderHalo");
  }

  /**
   * Get a scaled and positioned geometric shape of the halo. Where possible we path these shapes in
   * a clockwise direction starting from the top or top-left. This is so any dash pattern, for
   * consistency, will also originate from the same location across different basic shapes.
   *
   * <p>Not that for TOKEN shaped halos, the actual shape will have been previously overridden so we
   * pass <code>haloShapeType</code> as a argument rather than calling <code>halo.getHaloShapeType
   * </code> here.
   *
   * @param grid the grid
   * @param halo the halo itself
   * @return the halo shape
   */
  private Shape getHaloGeometricShape(
      ZoneViewModel.TokenPosition position,
      Grid grid,
      Halo halo,
      Halo.HaloShapeType haloShapeType,
      double concentricAdjustment) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getHaloGeometricShape");

    // get the unit shape, polygon and star shapes have vertices, angle base shapes have an angle.
    Shape unitShape = getUnitShape(haloShapeType, halo.getVertices(), halo.getAngle());

    // transform the shape
    double scaleX;
    double scaleY;
    double footprintScaleX = position.footprintBounds().getWidth() + concentricAdjustment;
    double footprintScaleY = position.footprintBounds().getHeight() + concentricAdjustment;
    if (GridFactory.getGridType(grid).equals(GridFactory.ISOMETRIC)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_HORI)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_VERT)) {
      scaleX = halo.getScaleX() * Math.min(footprintScaleX, footprintScaleY);
      scaleY = halo.getScaleY() * Math.min(footprintScaleX, footprintScaleY);
    } else {
      scaleX = halo.getScaleX() * footprintScaleX;
      scaleY = halo.getScaleY() * footprintScaleY;
    }
    double translateX = position.transformedBounds().getBounds2D().getCenterX();
    double translateY = position.transformedBounds().getBounds2D().getCenterY();

    Shape transformedHaloShape =
        transformHaloShape(
            unitShape,
            scaleX,
            scaleY,
            halo.getFlipHorizontal(),
            halo.getFlipVertical(),
            halo.getRotate(),
            translateX,
            translateY);

    // To clip an arc based shape we use the circle on which the arc lies
    if (halo.isAngleBasedShape(haloShapeType)) {
      haloClipArcByCircleShape =
          transformHaloShape(
              getUnitShape(Halo.HaloShapeType.CIRCLE, 0, 0),
              scaleX,
              scaleY,
              halo.getFlipHorizontal(),
              halo.getFlipVertical(),
              halo.getRotate(),
              translateX,
              translateY);
    }

    timer.stop("HaloRenderer-getHaloGeometricShape");
    return transformedHaloShape;
  }

  /**
   * Generate a shape, with a number of vertices lying on a circumscribed circle (circumcircle) for
   * a given polygon inscribed circle (incircle) of radius (inradius), which can be scaled and
   * positioned later as needed.
   *
   * <ul>
   *   <li>regular polygon
   *   <li>star polygon with a polygon density of 2
   *
   * @param haloShapeType the type of the polygon halo shape
   * @param vertices the number of polygon vertices or star outer-vertices
   * @return the unit polygon shape
   */
  private Shape getUnitPolygonShape(Halo.HaloShapeType haloShapeType, Integer vertices) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getUnitPolygonShape");
    CompositeUnitPolygonShapeKey key = new CompositeUnitPolygonShapeKey(haloShapeType, vertices);

    Shape polygonShape =
        haloUnitPolygonShapeMap.computeIfAbsent(
            key,
            key2 -> {
              timer.increment("HaloRenderer-getUnitPolygonShape:computeIfAbsent");
              double inRadius = 1 / 2d;
              // a circumscribed circle passes through all polygon outer-vertices
              double circumRadius = inRadius / Math.cos(Math.PI / vertices);
              double radius = circumRadius;

              double orientationAngle = 0d;
              // orientate regular polygons to have a flat top, otherwise a pointy top for stars
              if (haloShapeType.equals(Halo.HaloShapeType.POLYGON)) {
                orientationAngle = Math.PI / vertices;
              }

              // create the shape by iterating around the number of points
              Path2D.Double shapePath = new Path2D.Double();
              for (int i = 0; i < vertices; i++) {
                double theta; // the angle required move round to the next point
                if (haloShapeType.equals(Halo.HaloShapeType.STAR)) {
                  // with polygon density of 2 the next point will be the one after next, but...
                  // ...even numbered stars need to be rotated when halfway to avoid repetition
                  if (vertices % 2 == 0 && i == vertices / 2) {
                    orientationAngle = orientationAngle + 2 * Math.PI / vertices;
                  }
                  theta = (4 * Math.PI * i / vertices) - orientationAngle;
                } else {
                  // for regular polygons the next point is adjacent clockwise
                  theta = (2 * Math.PI * i / vertices) - orientationAngle;
                }
                double x = radius * Math.sin(theta);
                double y = radius * -Math.cos(theta);

                if (i == 0) {
                  shapePath.moveTo(x, y);
                } else if (haloShapeType.equals(Halo.HaloShapeType.STAR)
                    && (vertices % 2 == 0 && i == vertices / 2)) {
                  shapePath.closePath();
                  shapePath.moveTo(x, y);
                } else {
                  shapePath.lineTo(x, y);
                }
              }
              shapePath.closePath();

              return shapePath;
            });

    timer.stop("HaloRenderer-getUnitPolygonShape");
    return polygonShape;
  }

  /**
   * Gey a scaled and positioned halo minishape, composed of a number of miniature shapes rotated
   * and spread equidistant around a circle.
   *
   * @return the polygon shape
   * @see net.rptools.maptool.model.Halo.HaloShapeType
   */
  private Shape getHaloMiniShape(
      Token token,
      ZoneViewModel.TokenPosition position,
      Grid grid,
      Halo halo,
      double concentricAdjustment) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getHaloMiniShape");

    timer.start("HaloRenderer-getHaloMiniShape:unitShapePrep");
    Shape unitShape = getUnitShape(halo.getHaloShapeType(), halo.getVertices(), halo.getAngle());

    // scale unit shape to the required miniature size
    double haloSourceScaleFactor = haloScaleFactorMap.get(halo);
    double scaleUnitX =
        (halo.getWidth() == null ? haloLineWidthPreference : halo.getWidth())
            * haloSourceScaleFactor
            * halo.getScaleX();
    double scaleUnitY =
        (halo.getWidth() == null ? haloLineWidthPreference : halo.getWidth())
            * haloSourceScaleFactor
            * halo.getScaleY();
    Shape scaledUnitShape =
        AffineTransform.getScaleInstance(scaleUnitX, scaleUnitY).createTransformedShape(unitShape);

    // get some other attributes we need
    int mini = halo.getMini(); // the number of miniature shapes
    int miniStart = halo.getMiniStart() == 0 ? 0 : halo.getMiniStart() - 1; // when to start
    int miniStop = halo.getMiniStop() == 0 ? mini : halo.getMiniStop(); // when to stop
    double miniRotate = halo.getMiniRotate();
    double miniSpin =
        halo.getMiniSpin(); // rotation multiplier as mini it revolves (0 is static, 1 is default)
    TokenFootprint footprint = token.getFootprint(grid);
    double radius = grid.getSize() / 2d;

    double scaleX;
    double scaleY;
    double footprintScaleX =
        (position.footprintBounds().getWidth() + concentricAdjustment) / grid.getSize();
    double footprintScaleY =
        (position.footprintBounds().getHeight() + concentricAdjustment) / grid.getSize();
    if (GridFactory.getGridType(grid).equals(GridFactory.ISOMETRIC)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_HORI)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_VERT)) {
      scaleX = Math.min(footprintScaleX, footprintScaleY);
      scaleY = Math.min(footprintScaleX, footprintScaleY);
    } else {
      scaleX = footprintScaleX;
      scaleY = footprintScaleY;
    }
    timer.stop("HaloRenderer-getHaloMiniShape:unitShapePrep");

    timer.start("HaloRenderer-getHaloMiniShape:generateKey");
    CompositeHaloMiniShapeKey key =
        new CompositeHaloMiniShapeKey(
            footprint, halo, scaleUnitX, scaleUnitY, radius, concentricAdjustment);
    timer.stop("HaloRenderer-getHaloMiniShape:generateKey");

    timer.start("HaloRenderer-getHaloMiniShape:compositeArea");
    // revolve the mini shape around the radius, rotate , and then and combine
    Area compositeArea =
        haloMiniShapeMap.computeIfAbsent(
            key,
            key2 -> {
              timer.increment("HaloRenderer-getHaloMiniShape:haloMiniShapeMap");

              Area area = new Area();
              // iterate through the mini halo shapes
              for (int i = miniStart; i < miniStop; i++) {
                double theta = (2 * Math.PI * i / mini);
                double x = radius * Math.sin(theta);
                double y = radius * -Math.cos(theta);
                double x1 = x * scaleX;
                double y1 = y * scaleY;

                Shape revolvedShape =
                    AffineTransform.getTranslateInstance(x1, y1)
                        .createTransformedShape(scaledUnitShape);
                Shape rotatedShape =
                    AffineTransform.getRotateInstance(
                            theta * miniSpin + miniRotate * Math.PI / 180, x1, y1)
                        .createTransformedShape(revolvedShape);
                Area singleArea = new Area(rotatedShape);
                area.add(singleArea);
              }
              return area;
            });
    timer.stop("HaloRenderer-getHaloMiniShape:compositeArea");

    // position the composite area to the token and rotate as required
    timer.start("HaloRenderer-getHaloMiniShape:transform");
    double translateX = position.transformedBounds().getBounds2D().getCenterX();
    double translateY = position.transformedBounds().getBounds2D().getCenterY();
    Shape translatedCompositeArea =
        AffineTransform.getTranslateInstance(translateX, translateY)
            .createTransformedShape(compositeArea);
    Shape finalHaloMiniShape =
        AffineTransform.getRotateInstance(halo.getRotate() * Math.PI / 180d, translateX, translateY)
            .createTransformedShape(translatedCompositeArea);
    timer.stop("HaloRenderer-getHaloMiniShape:transform");

    timer.stop("HaloRenderer-getHaloMiniShape");

    return finalHaloMiniShape;
  }

  /**
   * Generate a shape of unit size, which can be scaled and positioned later as needed.
   *
   * @param haloShapeType the halo shape type
   * @param vertices the number of vertices (if applicable)
   * @param angle the angle (if applicable)
   * @return the unit shape
   */
  private Shape getUnitShape(Halo.HaloShapeType haloShapeType, int vertices, double angle) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getUnitShape");

    CompositeUnitShapeKey key = new CompositeUnitShapeKey(haloShapeType, vertices, angle);

    Shape returnUnitShape =
        haloUnitShapeMap.computeIfAbsent(
            key,
            key2 -> {
              Shape unitShape;
              double r = 1d;
              switch (haloShapeType) {
                case Halo.HaloShapeType.CIRCLE -> {
                  // rotated so the circle path starts at the 12 o'clock position
                  unitShape =
                      AffineTransform.getRotateInstance(-Math.PI / 2, 0, 0)
                          .createTransformedShape(new Ellipse2D.Double(-r / 2, -r / 2, r, r));
                }

                // start angles = 90 so path starts from 12 o'clock position
                // extent angles is negative to force clockwise direction
                case Halo.HaloShapeType.ARC -> {
                  unitShape = new Arc2D.Double(-r / 2, -r / 2, r, r, 90, -angle, Arc2D.OPEN);
                }
                case Halo.HaloShapeType.CHORD -> {
                  unitShape = new Arc2D.Double(-r / 2, -r / 2, r, r, 90, -angle, Arc2D.CHORD);
                }
                case Halo.HaloShapeType.PIE -> {
                  unitShape = new Arc2D.Double(-r / 2, -r / 2, r, r, 90, -angle, Arc2D.PIE);
                }
                case Halo.HaloShapeType.TRIANGLE -> {
                  unitShape =
                      AffineTransform.getRotateInstance(Math.PI, 0, 0)
                          .createTransformedShape(
                              getUnitPolygonShape(Halo.HaloShapeType.POLYGON, 3));
                }
                case Halo.HaloShapeType.SQUARE -> {
                  // this could also have been done as getUnitPolygonShape with 4 vertices
                  unitShape = new Rectangle2D.Double(-r / 2, -r / 2, r, r);
                }
                case Halo.HaloShapeType.POLYGON -> {
                  unitShape = getUnitPolygonShape(Halo.HaloShapeType.POLYGON, vertices);
                }
                case Halo.HaloShapeType.STAR -> {
                  unitShape = getUnitPolygonShape(Halo.HaloShapeType.STAR, vertices);
                }
                default -> {
                  // should not get here as should always have a halo shape type
                  unitShape = null;
                }
              }
              return unitShape;
            });

    timer.stop("HaloRenderer-getUnitShape");
    return returnUnitShape;
  }

  /**
   * Get a scaled and positioned halo shape based on the token footprint's occupied cells.
   *
   * @param token the token that needs the halo
   * @param grid the grid
   * @param concentricAdjustment offset from the centre
   * @return the halo shape
   */
  private Shape getHaloFootprintShape(Token token, Grid grid, double concentricAdjustment) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getHaloFootprintShape");

    timer.start("HaloRenderer-getHaloFootprintShape:gridShape");
    Shape gridShape = grid.getCellShape();
    Area compositeArea = new Area();
    double scaleX = 1d + concentricAdjustment / grid.getCellWidth();
    double scaleY = 1d + concentricAdjustment / grid.getCellHeight();
    Shape scaledShape =
        AffineTransform.getScaleInstance(scaleX, scaleY).createTransformedShape(gridShape);
    timer.stop("HaloRenderer-getHaloFootprintShape:gridShape");

    timer.start("HaloRenderer-getHaloFootprintShape:compositeArea");
    Set<CellPoint> cps = token.getOccupiedCells(grid);
    for (CellPoint cp : cps) {
      ZonePoint zp = grid.convert(cp);
      Shape translatedShape =
          AffineTransform.getTranslateInstance(zp.x, zp.y).createTransformedShape(scaledShape);
      Area areaCell = new Area(translatedShape);
      compositeArea.add(areaCell);
    }
    timer.stop("HaloRenderer-getHaloFootprintShape:compositeArea");

    timer.start("HaloRenderer-getHaloFootprintShape:transform");
    double gridCellOffsetX = grid.getCellOffset().getWidth();
    double gridCellOffsetY = grid.getCellOffset().getHeight();
    Shape translatedCompositeArea =
        AffineTransform.getTranslateInstance(
                gridCellOffsetX - concentricAdjustment / 2d,
                gridCellOffsetY - concentricAdjustment / 2d)
            .createTransformedShape(compositeArea);
    timer.stop("HaloRenderer-getHaloFootprintShape:transform");

    timer.stop("HaloRenderer-getHaloFootprintShape");

    return translatedCompositeArea;
  }

  /**
   * Get a a scaled and positioned halo shape based on the grid.
   *
   * @param position the token's position
   * @param grid the grid
   * @param halo the halo
   * @param concentricAdjustment offset from the centre
   * @return the grid shape, or a circle for gridless
   */
  private Shape getHaloGridShape(
      ZoneViewModel.TokenPosition position, Grid grid, Halo halo, double concentricAdjustment) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getHaloGridShape");

    if (haloGridShapeCache == null) {
      if (GridFactory.getGridType(grid).equals(GridFactory.NONE)) {
        double r = grid.getSize() / 2d;
        haloGridShapeCache = new Ellipse2D.Double(-r, -r, 2 * r, 2 * r);
      } else {
        haloGridShapeCache = grid.getCellShape();
        haloGridShapeCache =
            AffineTransform.getTranslateInstance(
                    -haloGridShapeCache.getBounds2D().getCenterX(),
                    -haloGridShapeCache.getBounds2D().getCenterY())
                .createTransformedShape(haloGridShapeCache);
      }
    }

    double scaleX = (position.footprintBounds().getWidth() + concentricAdjustment) / grid.getSize();
    double scaleY =
        (position.footprintBounds().getHeight() + concentricAdjustment) / grid.getSize();
    double scale;
    if (GridFactory.getGridType(grid).equals(GridFactory.ISOMETRIC)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_HORI)
        || GridFactory.getGridType(grid).equals(GridFactory.HEX_VERT)) {
      scale = Math.min(scaleX, scaleY);
    } else {
      scale = Math.max(scaleX, scaleY);
    }
    double translateX = position.transformedBounds().getBounds2D().getCenterX();
    double translateY = position.transformedBounds().getBounds2D().getCenterY();

    Shape transformedHaloShape =
        transformHaloShape(
            haloGridShapeCache,
            scale,
            scale,
            halo.getFlipHorizontal(),
            halo.getFlipVertical(),
            halo.getRotate(),
            translateX,
            translateY);

    timer.stop("HaloRenderer-getHaloGridShape");
    return transformedHaloShape;
  }

  /**
   * Get a halo shape based on the transformed topology of the token's image asset.
   *
   * @param token the token which needs a halo
   * @return Area as a Shape
   */
  private Shape getHaloTopolopyShape(Token token) {
    // no concentricAdjustment for Halo Topolopy shapes currently...

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-getHaloTopologyShape");

    Area tokenTopology =
        haloTopologyAreaMap.computeIfAbsent(
            token.getImageAssetId(),
            id2 -> {
              return TokenVBL.createOptimizedTopologyArea(
                  token,
                  10,
                  true,
                  new Color(0, 0, 0, 0),
                  2,
                  TokenVBL.JTS_SimplifyMethodType.DOUGLAS_PEUCKER_SIMPLIFIER.name());
            });

    timer.stop("HaloRenderer-getHaloTopologyShape");
    return token.getTransformedMaskTopology(zone, tokenTopology);
  }

  /**
   * Apply any required scale, position, rotation, & flipping sequentially to a halo shape using
   * {@link AffineTransform}.
   *
   * <p>Note that this method is not used to transform all halo shapes. For example, TOPOLOGY and
   * MBL instead use {@link Token}{@code .getTransformedMaskTopology()}.
   *
   * @param haloShape the halo shape itself
   * @param scaleX if required, scale the halo shape in the x-dimension
   * @param scaleY if required, scale the halo shape in the y-dimension
   * @param translateX if required, move the halo shape in the x-dimension
   * @param translateY if required, move the halo shape in the y-dimension
   * @return the transformed halo shape
   */
  private Shape transformHaloShape(
      Shape haloShape,
      double scaleX,
      double scaleY,
      boolean flipHorizontal,
      boolean flipVertical,
      double rotateAngle,
      double translateX,
      double translateY) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-transformHaloShape");

    // build and apply all the transforms we need for basic shapes
    // * note that transforms are applied in the reverse order to which they are added below
    AffineTransform at = new AffineTransform();
    // * the transform below this comment will be applied last
    if (translateX != 0 || translateY != 0) {
      at.translate(translateX, translateY);
    }
    if (flipHorizontal) {
      at.scale(1, -1);
    }
    if (flipVertical) {
      at.scale(-1, 1);
    }
    if (rotateAngle % 360 != 0) {
      at.rotate(rotateAngle * Math.PI / 180d);
    }
    if (scaleX != 0 || scaleY != 0) {
      at.scale(scaleX, scaleY);
    }
    // * the transform above this comment will be applied first

    timer.stop("HaloRenderer-transformHaloShape");
    return at.createTransformedShape(haloShape);
  }

  /**
   * Paint the halo using the width, color, and dashed pattern where supplied.
   *
   * @param g2d where to paint
   * @param token the token with the halos
   * @param grid the map's grid
   * @param paintShape the shape to paint which has been scaled and positioned
   * @param halo the halo itself
   */
  private void paintHalo(Graphics2D g2d, Token token, Grid grid, Shape paintShape, Halo halo) {

    var timer = CodeTimer.get();
    timer.start("HaloRenderer-paintHalo");

    // region 1. determine the halo colors including alpha
    timer.start("HaloRenderer-paintHalo:color");
    Color color;
    if (halo.getPaint() instanceof DrawableColorPaint dcp) {
      boolean hasAlpha = (dcp.getColor() & 0xFF000000) != 0xFF000000;
      color = new Color(dcp.getColor(), hasAlpha);
    } else {
      color = new Color(255, 255, 255, 125);
    }
    Color bgColor = color;
    timer.stop("HaloRenderer-paintHalo:color");
    // endregion

    Halo.HaloShapeType haloShapeType =
        halo.getHaloShapeType() == null ? Halo.DEFAULT_HALO_SHAPE_TYPE : halo.getHaloShapeType();
    String haloShapeTypeName = haloShapeType.name();

    boolean doStroke = true;
    boolean doFill = halo.getFill();
    boolean doClip = true;
    double haloWidth = halo.getWidth() == null ? haloLineWidthPreference : halo.getWidth();

    if (halo.getMini() > 0) {
      doStroke = false;
      doFill = true;
      haloWidth = 1d;
    }

    if (doFill) {
      // no need for clippin' if we are a fillin'
      doClip = false;
    }

    Stroke haloStroke = null;
    if (doStroke) {
      // region 2. determine the halo stroke
      timer.start("HaloRenderer-paintHalo:basicStroke");

      double haloSourceScaleFactor = haloScaleFactorMap.get(halo);
      // double width stroke thickness because we will clip the inside half
      float strokeWidth = (float) (2f * haloWidth * Math.max(1d, haloSourceScaleFactor));

      haloStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
      timer.stop("HaloRenderer-paintHalo:basicStroke");

      timer.start("HaloRenderer-paintHalo:dashedPattern");
      ArrayList<Float> dashedPattern = halo.getDashedPattern();
      if (dashedPattern != null) {
        // Could be dashing
        if (!dashedPattern.isEmpty()) {
          // Definitely dashing
          int arraySize = dashedPattern.size();
          float dashPhase = 0f;

          // The dash array takes pairs of float values, but if an odd number provided
          // shrink the array size and use the last number for the dash phase
          if (Math.floorMod(arraySize, 2) == 1) {
            arraySize = arraySize - 1;
            dashPhase = (float) (dashedPattern.get(arraySize) * haloSourceScaleFactor);
          }
          float[] dash = new float[arraySize];
          for (int i = 0; i < arraySize; i++) {
            dash[i] = (float) (dashedPattern.get(i) * haloSourceScaleFactor);
          }
          haloStroke =
              new BasicStroke(
                  strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, dashPhase);
        }
      }
      timer.stop("HaloRenderer-paintHalo:dashedPattern");
      // endregion

      Shape originalClip = null;
      // region 3. clipping and drawing
      if (doClip) {
        timer.start(String.format("HaloRenderer-paintHalo:clip-%s", haloShapeTypeName));
        originalClip = g2d.getClip();
        Area bounds = new Area(g2d.getClipBounds());
        if (haloShapeType.equals(Halo.HaloShapeType.ARC)) {
          // clip an arc by the parent circle to avoid any danglers at the end of the arcs
          bounds.subtract(new Area(haloClipArcByCircleShape));
        } else {
          bounds.subtract(new Area(paintShape));
        }
        g2d.setClip(bounds);
        timer.stop(String.format("HaloRenderer-paintHalo:clip-%s", haloShapeTypeName));
      }

      timer.start(String.format("HaloRenderer-paintHalo:draw-%s", haloShapeTypeName));
      g2d.setColor(color);
      g2d.setStroke(haloStroke);
      g2d.draw(paintShape);
      timer.stop(String.format("HaloRenderer-paintHalo:draw-%s", haloShapeTypeName));

      if (doClip) {
        timer.start(String.format("HaloRenderer-paintHalo:resetClip-%s", haloShapeTypeName));
        g2d.setClip(originalClip);
        timer.stop(String.format("HaloRenderer-paintHalo:resetClip-%s", haloShapeTypeName));
      }
      // endregion
    }

    // region 4. filling
    if (doFill) {
      timer.start(String.format("HaloRenderer-paintHalo:fill-%s", haloShapeTypeName));
      g2d.setColor(bgColor);
      g2d.fill(paintShape);
      timer.stop(String.format("HaloRenderer-paintHalo:fill-%s", haloShapeTypeName));
    }
    // endregion

    timer.stop("HaloRenderer-paintHalo");
  }

  /**
   * Keys for the mini halo shape multikey map cache.
   *
   * @param key1 the token footprint
   * @param key2 the halo
   * @param key3 the scaleUnitX
   * @param key4 the scaleUnitY
   * @param key5 the radius
   * @param key6 the concentric offset adjustment
   */
  private record CompositeHaloMiniShapeKey(
      TokenFootprint key1, Halo key2, Double key3, Double key4, Double key5, Double key6) {

    @Override
    public boolean equals(Object obj) {
      if (!(obj
          instanceof
          CompositeHaloMiniShapeKey(
              TokenFootprint akey1,
              Halo akey2,
              Double akey3,
              Double akey4,
              Double akey5,
              Double akey6))) {
        return false;
      }
      return this.key1.equals(akey1)
          && this.key2.equals(akey2)
          && this.key3.equals(akey3)
          && this.key4.equals(akey4)
          && this.key3.equals(akey5)
          && this.key4.equals(akey6);
    }

    @Override
    public int hashCode() {
      return key1.hashCode()
          ^ key2.hashCode()
          ^ key3.hashCode()
          ^ key4.hashCode()
          ^ key5.hashCode()
          ^ key6.hashCode();
    }
  }

  /**
   * Keys for the polygon unit shape multikey map cache.
   *
   * @param key1 the halo shape type
   * @param key2 the number of halo vertices
   */
  private record CompositeUnitPolygonShapeKey(Halo.HaloShapeType key1, Integer key2) {

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof CompositeUnitPolygonShapeKey(Halo.HaloShapeType akey1, Integer akey2))) {
        return false;
      }
      return this.key1.equals(akey1) && this.key2.equals(akey2);
    }

    @Override
    public int hashCode() {
      return key1.hashCode() ^ key2.hashCode();
    }
  }

  /**
   * Keys for the unit shape multikey map cache.
   *
   * @param key1 the halo shape type
   * @param key2 the number of halo vertices (will be 0 for non-polygonal shapes)
   * @param key3 the angle (will be 0 for non-angle based shapes)
   */
  private record CompositeUnitShapeKey(Halo.HaloShapeType key1, Integer key2, Double key3) {

    @Override
    public boolean equals(Object obj) {
      if (!(obj
          instanceof
          CompositeUnitShapeKey(Halo.HaloShapeType akey1, Integer akey2, Double akey3))) {
        return false;
      }
      return this.key1.equals(akey1) && this.key2.equals(akey2) && this.key3.equals(akey3);
    }

    @Override
    public int hashCode() {
      return key1.hashCode() ^ key2.hashCode() ^ key3.hashCode();
    }
  }
}
