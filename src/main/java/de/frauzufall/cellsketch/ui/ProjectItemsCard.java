package de.frauzufall.cellsketch.ui;

import net.imglib2.type.numeric.ARGBType;
import net.miginfocom.swing.MigLayout;
import de.frauzufall.cellsketch.model.Action;
import de.frauzufall.cellsketch.model.DisplayableInBdv;
import de.frauzufall.cellsketch.model.Item;
import de.frauzufall.cellsketch.model.ItemGroup;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectItemsCard extends JPanel {

	private final ItemTableModel model = new ItemTableModel();

	private static int VISIBILITY_COL = 0;
	private static int TITLE_COL = 1;
	private static int COLOR_COL = 2;
	private static int ACTIONS_COL = 3;

	private static int squareColWidth = 25;

	public void build(String title) {
		setLayout(new MigLayout("fill, ins 0"));
		JTable table = makeItemsTable(model);
		add(table, "push, grow, span");
	}

	private JTable makeItemsTable(ItemTableModel model) {
		JTable table = new JTable(model);
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);
		table.setCellSelectionEnabled(false);
		TableColumn visibilityColumn = table.getColumnModel().getColumn(VISIBILITY_COL);
		visibilityColumn.setMaxWidth(squareColWidth);
		visibilityColumn.setCellEditor(new VisibilityButtonEditor());
		visibilityColumn.setCellRenderer(new VisibilityButtonRenderer());
		TableColumn titleColumn = table.getColumnModel().getColumn(TITLE_COL);
		titleColumn.setCellRenderer(new TitleRenderer());
		TableColumn colorColumn = table.getColumnModel().getColumn(COLOR_COL);
		colorColumn.setCellRenderer(new ColorRenderer());
		colorColumn.setCellEditor(new ColorEditor());
		colorColumn.setMaxWidth(squareColWidth);
		TableColumn actionsColumn = table.getColumnModel().getColumn(ACTIONS_COL);
		actionsColumn.setMaxWidth(squareColWidth);
		actionsColumn.setCellEditor(new ActionsButtonEditor());
		actionsColumn.setCellRenderer(new ActionsButtonRenderer());
		table.getTableHeader().setVisible(false);
		table.setRowHeight(22);
		table.setRowMargin(2);
		return table;
	}

	public void clear() {
		model.getItems().clear();
	}

	public void addItem(Item item) {
		if(item.exists()) {
			model.getItems().add(item);
		}
		if(ItemGroup.class.isAssignableFrom(item.getClass())) {
			((ItemGroup)item).getItems().forEach(child -> model.getItems().add(child));
		}
	}

	private void openActionMenu(Component source, Item item) {
		JPopupMenu menu = new JPopupMenu();
		for (Action action : item.getActions()) {
			JMenuItem menuItem = menu.add(new AbstractAction(action.getTitle()) {
				@Override
				public void actionPerformed(ActionEvent e) {
					new Thread(action).start();
				}
			});
			menuItem.setEnabled(action.isExecutable());
		}
		menu.show(source,0, 0);
	}

	public AbstractTableModel getItemsModel() {
		return model;
	}

	static class VisibilityButtonRenderer extends JToggleButton implements TableCellRenderer {

		private final ImageIcon selectedIcon;
		private final ImageIcon defaultIcon;

		VisibilityButtonRenderer() {
			selectedIcon = new ImageIcon(getClass().getResource("/icons/visible.png"));
			defaultIcon = new ImageIcon(getClass().getResource("/icons/invisible.png"));
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
		                                               boolean isSelected, boolean hasFocus, int row, int column) {
			if(value == null) return null;
			if(DisplayableInBdv.class.isAssignableFrom(value.getClass())) {
				setSelectedIcon(selectedIcon);
				setIcon(defaultIcon);
				DisplayableInBdv item = (DisplayableInBdv) value;
				setSelected(item.isVisible());
				setEnabled(item.exists());
			} else {
				setIcon(null);
			}
			return this;
		}
	}

	static class ActionsButtonRenderer extends JToggleButton implements TableCellRenderer {

		ActionsButtonRenderer() {
			setText("...");
			setOpaque(false);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
		                                               boolean isSelected, boolean hasFocus, int row, int column) {
			Item item = (Item) value;
			if(item == null || item.getActions() == null || item.getActions().size() == 0){
				return null;
			}
			return this;
		}
	}


	static class TitleRenderer extends DefaultTableCellRenderer {
		public Component getTableCellRendererComponent(JTable table, Object value,
		                                               boolean isSelected, boolean hasFocus, int row, int column) {
			Component label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value == null) return label;
			if(ItemGroup.class.isAssignableFrom(value.getClass())){
				this.setFont(this.getFont().deriveFont(Font.BOLD));
			}
			((JLabel)label).setText(((Item)value).getName());

			return label;
		}
	}

	static class ColorRenderer extends JLabel
			implements TableCellRenderer {

		private final Border lineBorder = BorderFactory.createLineBorder(Color.darkGray);

		public Component getTableCellRendererComponent(
				JTable table, Object item,
				boolean isSelected, boolean hasFocus,
				int row, int column) {
			if(item == null) return this;
			if(DisplayableInBdv.class.isAssignableFrom(item.getClass())) {
				Color newColor = new Color(((Item)item).getColor());
				setBackground(newColor);
				setOpaque(true);
				setBorder(lineBorder);
			} else {
				setBackground(null);
				setOpaque(false);
				setBorder(null);
			}
			return this;
		}
	}


	static class VisibilityButtonEditor extends AbstractCellEditor
			implements TableCellEditor {
		private JToggleButton button;

		private DisplayableInBdv item;

		private boolean isPushed;

		VisibilityButtonEditor() {
			button = new JToggleButton();
			button.setOpaque(false);
			button.addActionListener(e -> fireEditingStopped());
			button.setText("");
		}

		public Component getTableCellEditorComponent(JTable table, Object value,
		                                             boolean isSelected, int row, int column) {
			if(DisplayableInBdv.class.isAssignableFrom(value.getClass())) {
				item = (DisplayableInBdv) value;
			}
			isPushed = true;
			return button;
		}

		public Object getCellEditorValue() {
			if (isPushed && item != null) {
				if(item.isVisible()) item.removeFromBdv();
				else item.addToBdv();
			}
			isPushed = false;
			return item;
		}

		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}

	class ActionsButtonEditor extends AbstractCellEditor
			implements TableCellEditor {
		private JButton button;

		private Item item;

		private boolean isPushed;

		ActionsButtonEditor() {
			button = new JButton("...");
			button.setOpaque(false);
			button.addActionListener(e -> fireEditingStopped());
		}

		public Component getTableCellEditorComponent(JTable table, Object value,
		                                             boolean isSelected, int row, int column) {
			item = (Item) value;
			isPushed = true;
			return button;
		}

		public Object getCellEditorValue() {
			if (isPushed && item != null) {
				openActionMenu(button, item);
			}
			isPushed = false;
			return item;
		}

		public boolean stopCellEditing() {
			isPushed = false;
			return super.stopCellEditing();
		}
	}

	private static class ItemTableModel extends AbstractTableModel {
		private final java.util.List<Item> items = new ArrayList<>();

		List<Item> getItems() {
			return items;
		}

		@Override
		public String getColumnName(int column) {
			if(column == VISIBILITY_COL) return "visibility";
			if(column == TITLE_COL) return "name";
			if(column == COLOR_COL) return "color";
			if(column == ACTIONS_COL) return "actions";
			return super.getColumnName(column);
		}

		@Override
		public int getRowCount() {
			return items.size();
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return Item.class;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if(rowIndex < items.size()) return items.get(rowIndex);
			else return null;
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			Item item = items.get(rowIndex);
			if(columnIndex == VISIBILITY_COL || columnIndex == COLOR_COL) {
				if(DisplayableInBdv.class.isAssignableFrom(item.getClass())) {
					return item.exists();
				}
			}
			else if(columnIndex == ACTIONS_COL) {
				return item.getActions() != null && item.getActions().size() > 0;
			}
			return false;
		}
	}
	static class ColorEditor extends AbstractCellEditor
			implements TableCellEditor,
			ActionListener {
		private Color currentColor;
		private JButton button;
		private JColorChooser colorChooser;
		private static final String EDIT = "edit";
		private DisplayableInBdv item;

		ColorEditor() {
			button = new JButton();
			button.setActionCommand(EDIT);
			button.addActionListener(this);
			button.setBorderPainted(false);
			colorChooser = new JColorChooser();
		}

		public void actionPerformed(ActionEvent e) {
			DisplayableInBdv item = null;
			if(DisplayableInBdv.class.isAssignableFrom(getCellEditorValue().getClass())) {
				item = (DisplayableInBdv) getCellEditorValue();
			}
			if (EDIT.equals(e.getActionCommand())) {
				button.setBackground(currentColor);
				colorChooser.setColor(currentColor);
				JDialog dialog = JColorChooser.createDialog(button, "Pick a Color", true, colorChooser, this, null);
				dialog.setVisible(true);
				fireEditingStopped();
			} else {
				currentColor = colorChooser.getColor();
				if(item != null) {
					item.setColor(ARGBType.rgba(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentColor.getAlpha()));
					item.updateBdvColor();
					try {
						item.saveConfig();
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
		}

		public Object getCellEditorValue() {
			return item;
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
			item = (DisplayableInBdv) table.getModel().getValueAt(row, column);
			int color = item.getColor();
			currentColor = new Color(color);
			return button;
		}
	}

}
