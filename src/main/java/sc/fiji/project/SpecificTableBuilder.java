package sc.fiji.project;

import org.scijava.table.DefaultGenericTable;
import org.scijava.table.GenericTable;
import org.scijava.table.Table;

import java.util.ArrayList;
import java.util.List;

public class SpecificTableBuilder {
	public static <T extends TableColumnDefinition> GenericTable build(T columnDefinition) {
		GenericTable table = new DefaultGenericTable();
		columnDefinition.getColumns().forEach(table::appendColumn);
		return table;
	}
	public static <T extends TableColumnDefinition> Table build(T columnDefinition, Table table) {
		GenericTable newtable = new DefaultGenericTable();
		List<String> columns = new ArrayList<>(columnDefinition.getColumns());
		for (int i = 0; i < table.getColumnCount(); i++) {
			if(columns.indexOf(table.getColumnHeader(i)) < 0) columns.add(table.getColumnHeader(i));
		}
		columns.forEach(newtable::appendColumn);
		for (int row = 0; row < table.getRowCount(); row++) {
			newtable.appendRow(table.getRowHeader(row));
			for (int col = 0; col < table.getColumnCount(); col++) {
				newtable.set(columns.indexOf(table.getColumnHeader(col)), row, table.get(col, row));
			}
		}
		return newtable;
	}
}
