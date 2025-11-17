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
package net.rptools.maptool.client.ui.lookuptable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import net.rptools.lib.MD5Key;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.MapToolUtil;
import net.rptools.maptool.client.swing.AbeillePanel;
import net.rptools.maptool.client.swing.ButtonKind;
import net.rptools.maptool.client.swing.GenericDialog;
import net.rptools.maptool.client.swing.GenericDialogFactory;
import net.rptools.maptool.client.swing.ImageChooserDialog;
import net.rptools.maptool.client.ui.ImageAssetPanel;
import net.rptools.maptool.language.I18N;
import net.rptools.maptool.model.AssetManager;
import net.rptools.maptool.model.LookupTable;
import net.rptools.maptool.model.LookupTable.LookupEntry;

public class EditLookupTablePanel extends AbeillePanel<LookupTable> {
  private static final int RANGE_COLUMN_INDEX = 0;
  private static final int VALUE_COLUMN_INDEX = 1;
  private static final int IMAGE_COLUMN_INDEX = 2;

  private final GenericDialogFactory dialogFactory;

  private ImageAssetPanel tableImageAssetPanel;
  private int defaultRowHeight;

  public EditLookupTablePanel() {
    super(new EditLookupTablePanelView().getRootComponent());
    panelInit();

    dialogFactory =
        GenericDialog.getFactory()
            .setContent(this)
            .makeModal(true)
            .addButton(ButtonKind.ACCEPT)
            .addButton(ButtonKind.CANCEL)
            .setDefaultButton(ButtonKind.ACCEPT)
            .setCloseOperation(WindowConstants.HIDE_ON_CLOSE)
            .onBeforeClose(
                e -> {
                  unbind();
                });
  }

  public void showDialog(@Nullable LookupTable lookupTable, boolean isNew) {
    var title =
        isNew || lookupTable == null
            ? I18N.getString("LookupTablePanel.msg.titleNew")
            : I18N.getString("LookupTablePanel.msg.titleEdit");
    dialogFactory.setDialogTitle(title);

    bind(lookupTable);

    dialogFactory.display();
  }

  public void initTableDefinitionTable() {
    defaultRowHeight = getTableDefinitionTable().getRowHeight();

    getTableDefinitionTable().setDefaultRenderer(ImageAssetPanel.class, new ImageCellRenderer());
    getTableDefinitionTable().setModel(createLookupTableModel(new LookupTable()));
    getTableDefinitionTable()
        .addMouseListener(
            new MouseAdapter() {
              @Override
              public void mousePressed(MouseEvent e) {
                int column = getTableDefinitionTable().columnAtPoint(e.getPoint());
                if (column != IMAGE_COLUMN_INDEX) {
                  return;
                }
                int row = getTableDefinitionTable().rowAtPoint(e.getPoint());
                String imageIdStr =
                    (String) getTableDefinitionTable().getModel().getValueAt(row, column);

                // HACK: this is a hacky way to figure out if the button was pushed :P
                if (e.getPoint().x > getTableDefinitionTable().getSize().width - 15) {
                  if (imageIdStr == null || imageIdStr.isEmpty()) {
                    // Add
                    ImageChooserDialog chooserDialog = MapTool.getFrame().getImageChooserDialog();
                    chooserDialog.setVisible(true);

                    MD5Key imageId = chooserDialog.getImageId();
                    if (imageId == null) {
                      return;
                    }
                    imageIdStr = imageId.toString();
                  } else {
                    // Cancel
                    imageIdStr = null;
                  }
                } else if (e.getPoint().x > getTableDefinitionTable().getSize().width - 30) {
                  // Add
                  ImageChooserDialog chooserDialog = MapTool.getFrame().getImageChooserDialog();

                  chooserDialog.setVisible(true);

                  MD5Key imageId = chooserDialog.getImageId();
                  if (imageId == null) {
                    return;
                  }
                  imageIdStr = imageId.toString();
                }
                getTableDefinitionTable().getModel().setValueAt(imageIdStr, row, column);
                updateDefinitionTableRowHeights();
                getTableDefinitionTable().repaint();
              }
            });
  }

  public void initTableImage() {
    tableImageAssetPanel = new ImageAssetPanel();
    tableImageAssetPanel.setPreferredSize(new Dimension(150, 150));
    tableImageAssetPanel.setBorder(BorderFactory.createLineBorder(Color.black));
    replaceComponent("tableImagePanel", "tableImage", tableImageAssetPanel);
  }

  @Override
  public void bind(LookupTable lookupTable) {
    super.bind(lookupTable);

    getTableNameTextField().setText(lookupTable.getName());
    getTableRollTextField().setText(lookupTable.calculateRoll());
    tableImageAssetPanel.setImageId(lookupTable.getTableImage());
    getVisibleCheckbox().setSelected(lookupTable.getVisible());
    getAllowLookupCheckbox().setSelected(lookupTable.getAllowLookup());

    getTableNameTextField().requestFocusInWindow();

    getTableDefinitionTable().setModel(createLookupTableModel(lookupTable));
    updateDefinitionTableRowHeights();
  }

  @Override
  public boolean commit() {
    if (!super.commit()) {
      return false;
    }

    var lookupTable = getModel();

    // Commit any in-process edits
    var tableDefinitionTable = getTableDefinitionTable();
    if (tableDefinitionTable.isEditing()) {
      tableDefinitionTable.getCellEditor().stopCellEditing();
    }
    String name = getTableNameTextField().getText().trim();
    if (name.isEmpty()) {
      MapTool.showError("EditLookupTablePanel.error.noName");
      return false;
    }
    LookupTable existingTable = MapTool.getCampaign().getLookupTableMap().get(name);
    if (existingTable != null && existingTable != lookupTable) {
      MapTool.showError(I18N.getText("EditLookupTablePanel.error.sameName", name));
      return false;
    }
    var tableModel = (LookupTableTableModel) tableDefinitionTable.getModel();
    if (tableModel.getRowCount() < 1) {
      MapTool.showError(I18N.getText("EditLookupTablePanel.error.invalidSize", name));
      return false;
    }

    // save existing name for later removal from LookupTableMap
    String origname = lookupTable.getName();
    lookupTable.setName(name);
    lookupTable.setRoll(getTableRollTextField().getText());
    lookupTable.setTableImage(tableImageAssetPanel.getImageId());
    lookupTable.setVisible(getVisibleCheckbox().isSelected());
    lookupTable.setAllowLookup(getAllowLookupCheckbox().isSelected());
    lookupTable.clearEntries();
    for (int i = 0; i < tableModel.getRowCount(); i++) {
      var row = tableModel.getRowAt(i);

      String range = row.range;
      if (range.isEmpty()) {
        continue;
      }
      String value = row.value;
      String imageId = row.imageId;

      int min;
      int max;
      int split = range.indexOf('-', range.charAt(0) == '-' ? 1 : 0); // Allow negative numbers
      try {
        if (split < 0) {
          min = Integer.parseInt(range);
          max = min;
        } else {
          min = Integer.parseInt(range.substring(0, split).trim());
          max = Integer.parseInt(range.substring(split + 1).trim());
        }
      } catch (NumberFormatException nfe) {
        MapTool.showError(I18N.getText("EditLookupTablePanel.error.badRange", name, range, i));
        return false;
      }
      MD5Key image = null;
      if (imageId != null && !imageId.isEmpty()) {
        image = new MD5Key(imageId);
        MapToolUtil.uploadAsset(AssetManager.getAsset(image));
      }
      lookupTable.addEntry(min, max, value, image);
    }
    if (!name.equals(origname)) {
      // New name is not the same as the existing name
      MapTool.getCampaign().getLookupTableMap().remove(origname);
    }
    // This will add it if it is new
    MapToolUtil.uploadAsset(AssetManager.getAsset(tableImageAssetPanel.getImageId()));
    MapTool.serverCommand().putLookupTable(lookupTable);

    return true;
  }

  public JTextField getTableNameTextField() {
    return (JTextField) getComponent("tableName");
  }

  public JTextField getTableRollTextField() {
    return (JTextField) getComponent("defaultTableRoll");
  }

  public JTable getTableDefinitionTable() {
    return (JTable) getComponent("definitionTable");
  }

  public JCheckBox getVisibleCheckbox() {
    return (JCheckBox) getComponent("visibleCheckbox");
  }

  public JCheckBox getAllowLookupCheckbox() {
    return (JCheckBox) getComponent("allowLookupCheckbox");
  }

  private void updateDefinitionTableRowHeights() {
    JTable table = getTableDefinitionTable();
    for (int row = 0; row < table.getRowCount(); row++) {
      String imageId = (String) table.getModel().getValueAt(row, IMAGE_COLUMN_INDEX);
      table.setRowHeight(row, imageId != null && !imageId.isEmpty() ? 100 : defaultRowHeight);
    }
  }

  private LookupTableTableModel createLookupTableModel(LookupTable lookupTable) {
    var rows = new ArrayList<LookupTableRow>();
    for (LookupEntry entry : lookupTable.getEntryList()) {
      String range =
          entry.getMax() != entry.getMin()
              ? entry.getMin() + "-" + entry.getMax()
              : "" + entry.getMin();
      String value = entry.getValue();
      MD5Key imageId = entry.getImageId();

      rows.add(new LookupTableRow(range, value, imageId != null ? imageId.toString() : null));
    }
    return new LookupTableTableModel(rows);
  }

  private class ImageCellRenderer extends ImageAssetPanel implements TableCellRenderer {
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      String s = value == null ? "" : Objects.requireNonNullElse(value.toString(), "");
      setImageId(!s.isEmpty() ? new MD5Key(s) : null, EditLookupTablePanel.this);
      return this;
    }
  }

  private static final class LookupTableRow {
    public String range;
    public String value;
    public @Nullable String imageId;

    public LookupTableRow() {
      this("", "", null);
    }

    public LookupTableRow(String range, String value, @Nullable String imageId) {
      this.range = range;
      this.value = value;
      this.imageId = imageId;
    }
  }

  private static final class LookupTableTableModel extends AbstractTableModel {
    private LookupTableRow newRow = new LookupTableRow();
    private final List<LookupTableRow> rowList;

    private final List<String> columnNames =
        List.of(
            I18N.getText("Label.range"), I18N.getText("Label.value"), I18N.getText("Label.image"));

    public LookupTableTableModel(List<LookupTableRow> rowList) {
      this.rowList = rowList;
    }

    public LookupTableRow getRowAt(int rowIndex) {
      if (rowIndex < 0 || rowIndex > rowList.size()) {
        throw new IndexOutOfBoundsException(rowIndex);
      }
      if (rowIndex == rowList.size()) {
        return newRow;
      }
      return rowList.get(rowIndex);
    }

    public int getColumnCount() {
      return columnNames.size();
    }

    public int getRowCount() {
      return rowList.size() + 1;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      LookupTableRow row = rowIndex < rowList.size() ? rowList.get(rowIndex) : newRow;

      return switch (columnIndex) {
        case RANGE_COLUMN_INDEX -> row.range;
        case VALUE_COLUMN_INDEX -> row.value;
        case IMAGE_COLUMN_INDEX -> row.imageId;
        default -> "";
      };
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      LookupTableRow row = rowIndex < rowList.size() ? rowList.get(rowIndex) : newRow;

      switch (columnIndex) {
        case RANGE_COLUMN_INDEX -> row.range = (String) aValue;
        case VALUE_COLUMN_INDEX -> row.value = (String) aValue;
        case IMAGE_COLUMN_INDEX -> row.imageId = (String) aValue;
      }

      if (row == newRow) {
        // Need to make the row permanent, and add a new placeholder.
        rowList.add(newRow);
        newRow = new LookupTableRow();
        fireTableRowsInserted(rowList.size(), rowList.size());
      }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      if (columnIndex == IMAGE_COLUMN_INDEX) {
        return ImageAssetPanel.class;
      } else {
        return String.class;
      }
    }

    @Override
    public String getColumnName(int column) {
      return columnNames.get(column);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex != IMAGE_COLUMN_INDEX;
    }
  }
}
