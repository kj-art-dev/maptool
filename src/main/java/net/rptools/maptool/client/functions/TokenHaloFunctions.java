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
package net.rptools.maptool.client.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.rptools.lib.StringUtil;
import net.rptools.maptool.client.*;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.CategorizedHalos;
import net.rptools.maptool.model.HaloSource;
import net.rptools.maptool.model.Halos;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.util.FunctionUtil;
import net.rptools.parser.Parser;
import net.rptools.parser.ParserException;
import net.rptools.parser.VariableResolver;
import net.rptools.parser.function.AbstractFunction;

public class TokenHaloFunctions extends AbstractFunction {
  private static final TokenHaloFunctions instance = new TokenHaloFunctions();

  private TokenHaloFunctions() {
    super(
        0, 3, "getHalo", "setHalo", "hasHalos", "clearHalos", "getHalos", "setHalos", "showHalos");
  }

  /**
   * Gets the singleton Halo instance.
   *
   * @return the Halo instance.
   */
  public static TokenHaloFunctions getInstance() {
    return instance;
  }

  @Override
  public Object childEvaluate(
      Parser parser, VariableResolver resolver, String functionName, List<Object> parameters)
      throws ParserException {

    if (functionName.equalsIgnoreCase("getHalo")) {
      return getHalo((MapToolVariableResolver) resolver, parameters);
    } else if ("setHalo".equalsIgnoreCase(functionName)) {
      return setHalo((MapToolVariableResolver) resolver, parameters);
    } else if (functionName.equalsIgnoreCase("hasHalos")) {
      FunctionUtil.checkNumberParam(functionName, parameters, 0, 4);

      String type = (!parameters.isEmpty()) ? parameters.get(0).toString() : "*";
      String name = (parameters.size() > 1) ? parameters.get(1).toString() : "*";
      Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 2, 3);
      return hasHalos(token, type, name) ? BigDecimal.ONE : BigDecimal.ZERO;
    } else if (functionName.equalsIgnoreCase("clearHalos")) {
      FunctionUtil.checkNumberParam(functionName, parameters, 0, 2);

      Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 0, 1);
      MapTool.serverCommand().updateTokenProperty(token, Token.Update.clearHaloSources);
      return "";
    } else if (functionName.equalsIgnoreCase("getHalos")) {
      FunctionUtil.checkNumberParam(functionName, parameters, 0, 4);

      String type = !parameters.isEmpty() ? parameters.get(0).toString() : "*";
      String delim = parameters.size() > 1 ? parameters.get(1).toString() : ",";
      Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 2, 3);
      return getHalos(token, type, delim);
    } else if (functionName.equalsIgnoreCase("setHalos")) {
      FunctionUtil.checkNumberParam(functionName, parameters, 3, 5);

      String type = parameters.get(0).toString();
      String name = parameters.get(1).toString();
      BigDecimal value = FunctionUtil.paramAsBigDecimal(functionName, parameters, 2, false);
      Token token = FunctionUtil.getTokenFromParam(resolver, functionName, parameters, 3, 4);
      return setHalos(token, type, name, value);
    } else if (functionName.equalsIgnoreCase("showHalos")) {
      FunctionUtil.checkNumberParam(functionName, parameters, 0, 1);

      if (parameters.isEmpty()) {
        // toggle
        AppState.setShowTokenHalos(!AppState.isShowTokenHalos());
      } else {
        String show = parameters.getFirst().toString();
        AppState.setShowTokenHalos(!Objects.equals(show, "0"));
      }
      if (MapTool.getFrame().getCurrentZoneRenderer() != null) {
        MapTool.getFrame().getCurrentZoneRenderer().repaint();
      }
      return AppState.isShowTokenHalos();
    }
    throw new ParserException(I18N.getText("macro.function.general.unknownFunction", functionName));
  }

  /**
   * Gets the halo for the token.
   *
   * @param token the token to get the halo for.
   * @return the halo.
   */
  public static Object getHalo(Token token) {
    var haloColor = token.getHaloColor();
    if (haloColor != null) {
      return "#" + Integer.toHexString(haloColor.getRGB()).substring(2);
    } else {
      return "None";
    }
  }

  /**
   * Sets the halo color of the token.
   *
   * @param token the token to set halo of.
   * @param value the value to set.
   */
  public static void setHalo(Token token, Object value) {
    Color haloColor;
    if (value instanceof Color) {
      haloColor = (Color) value;
    } else if (value instanceof BigDecimal) {
      haloColor = new Color(((BigDecimal) value).intValue());
    } else {
      String col = value.toString();
      if (StringUtil.isEmpty(col)
          || col.equalsIgnoreCase("none")
          || col.equalsIgnoreCase("default")) {
        haloColor = null;
      } else {
        String hex = col;
        Color color = MapToolUtil.getColor(hex);
        haloColor = color;
      }
    }
    var cmd = MapTool.serverCommand();

    if (haloColor != null) {
      cmd.updateTokenProperty(token, Token.Update.setHaloColor, haloColor.getRGB());
    } else {
      cmd.updateTokenProperty(token, Token.Update.setHaloColor);
    }
  }

  /**
   * Gets the halo of the token.
   *
   * @param args The arguments.
   * @return the halo color.
   * @throws ParserException if an error occurs.
   */
  private Object getHalo(MapToolVariableResolver resolver, List<Object> args)
      throws ParserException {
    Token token;

    if (args.size() == 1) {
      if (!MapTool.getParser().isMacroTrusted()) {
        throw new ParserException(I18N.getText("macro.function.general.noPermOther", "getHalo"));
      }
      token = FindTokenFunctions.findToken(args.get(0).toString(), null);
      if (token == null) {
        throw new ParserException(
            I18N.getText("macro.function.general.unknownToken", "getHalo", args.get(0).toString()));
      }
    } else if (args.size() == 0) {
      token = resolver.getTokenInContext();
      if (token == null) {
        throw new ParserException(I18N.getText("macro.function.general.noImpersonated", "getHalo"));
      }
    } else {
      throw new ParserException(
          I18N.getText("macro.function.general.tooManyParam", "getHalo", 1, args.size()));
    }
    return getHalo(token);
  }

  /**
   * Sets the halo of the token.
   *
   * @param args The arguments.
   * @return the halo color.
   * @throws ParserException if an error occurs.
   */
  private Object setHalo(MapToolVariableResolver resolver, List<Object> args)
      throws ParserException {

    Token token;
    Object value = args.get(0);

    switch (args.size()) {
      case 0:
        throw new ParserException(
            I18N.getText("macro.function.general.notEnoughParam", "setHalo", 1, args.size()));
      default:
        throw new ParserException(
            I18N.getText("macro.function.general.tooManyParam", "setHalo", 2, args.size()));
      case 1:
        token = resolver.getTokenInContext();
        if (token == null) {
          throw new ParserException(
              I18N.getText("macro.function.general.noImpersonated", "setHalo"));
        }
        break;
      case 2:
        if (!MapTool.getParser().isMacroTrusted()) {
          throw new ParserException(I18N.getText("macro.function.general.noPermOther", "setHalo"));
        }
        token = FindTokenFunctions.findToken(args.get(1).toString(), null);
        if (token == null) {
          throw new ParserException(
              I18N.getText(
                  "macro.function.general.unknownToken", "setHalo", args.get(1).toString()));
        }
    }
    setHalo(token, value);
    return value;
  }

  /**
   * Gets the names of the halo sources that are on.
   *
   * @param token The token to get the halo sources for.
   * @param categoryName The category to get the halo sources for. If "*" then the halo sources for
   *     all categories will be returned.
   * @param delim the delimiter for the list.
   * @return a string list containing the halo that are on.
   * @throws ParserException if the halo type can't be found.
   */
  private static String getHalos(Token token, String categoryName, String delim)
      throws ParserException {
    ArrayList<String> haloList = new ArrayList<String>();
    CategorizedHalos haloSources = MapTool.getCampaign().getHaloSources();

    if (categoryName.equals("*")) {
      for (CategorizedHalos.Category category : haloSources.getCategories()) {
        for (HaloSource hs : category.halos()) {
          if (token.hasHaloSource(hs)) {
            haloList.add(hs.getName());
          }
        }
      }
    } else {
      var category = haloSources.getCategory(categoryName);
      if (category.isPresent()) {
        for (HaloSource hs : category.get().halos()) {
          if (token.hasHaloSource(hs)) {
            haloList.add(hs.getName());
          }
        }
      } else {
        throw new ParserException(
            I18N.getText("macro.function.tokenHalo.unknownHaloCategory", "getHalos", categoryName));
      }
    }

    if ("json".equals(delim)) {
      JsonArray jarr = new JsonArray();
      haloList.forEach(l -> jarr.add(new JsonPrimitive(l)));
      return jarr.toString();
    } else {
      return StringFunctions.getInstance().join(haloList, delim);
    }
  }

  /**
   * Sets the halo value for a token.
   *
   * @param token the token to set the halo for.
   * @param categoryName the category of the halo source.
   * @param name the name of the halo source.
   * @param val the value to set for the halo source, 0 for off non 0 for on.
   * @return 0 if the halo was not found, otherwise 1;
   * @throws ParserException if the halo type can't be found.
   */
  private static BigDecimal setHalos(Token token, String categoryName, String name, BigDecimal val)
      throws ParserException {
    boolean found = false;
    CategorizedHalos categorizedHalos = MapTool.getCampaign().getHaloSources();

    Halos halos;

    var category = categorizedHalos.getCategory(categoryName);
    if (category.isPresent()) {
      halos = category.get().halos();
    } else {
      throw new ParserException(
          I18N.getText("macro.function.tokenHalo.unknownHaloCategory", "setHalos", categoryName));
    }

    final var add = !BigDecimal.ZERO.equals(val);
    for (HaloSource hs : halos) {
      if (name.equals(hs.getName())) {
        found = true;
        MapTool.serverCommand().toggleHaloSourceOnToken(token, add, hs);
      }
    }

    return found ? BigDecimal.ONE : BigDecimal.ZERO;
  }

  /**
   * Checks to see if the token has a halo source. The token is checked to see if it has a halo
   * source with the name in the second parameter from the category in the first parameter. A "*"
   * for category indicates all categories are checked; a "*" for name indicates all names are
   * checked. The "$token" category indicates that only halo sources defined on the token are
   * checked.
   *
   * @param token the token to check.
   * @param categoryName the type of halo to check.
   * @param name the name of the halo to check.
   * @return true if the token has the halo source.
   * @throws ParserException if the halo type can't be found.
   */
  public static boolean hasHalos(Token token, String categoryName, String name)
      throws ParserException {
    if ("*".equals(categoryName) && "*".equals(name)) {
      return token.hasHaloSources();
    }

    CategorizedHalos categorizedHalos = MapTool.getCampaign().getHaloSources();

    if ("*".equals(categoryName)) {
      for (CategorizedHalos.Category category : categorizedHalos.getCategories()) {
        for (HaloSource hs : category.halos()) {
          if (name.equals(hs.getName()) && token.hasHaloSource(hs)) {
            return true;
          }
        }
        return false;
      }
    } else {
      var category = categorizedHalos.getCategory(categoryName);
      if (category.isPresent()) {
        for (HaloSource hs : category.get().halos()) {
          if ((name.equals(hs.getName()) || name.equals("*")) && token.hasHaloSource(hs)) {
            return true;
          }
        }
      } else {
        throw new ParserException(
            I18N.getText("macro.function.tokenHalo.unknownHaloCategory", "hasHalos", categoryName));
      }
    }

    return false;
  }
}
