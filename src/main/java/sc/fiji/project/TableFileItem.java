package sc.fiji.project;

import org.scijava.table.Table;
import org.scijava.table.io.TableIOOptions;
import org.scijava.table.io.TableIOService;
import org.scijava.ui.UIService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TableFileItem extends FileItem {

	private final TableColumnDefinition columns;
	private final TableIOService tableIOService;
	private Table table;

	public TableFileItem(BdvProject app, String name) {
		this(app, name, null);
	}

	public TableFileItem(BdvProject app, String name, TableColumnDefinition columns) {
		super(app, name, "csv");
		tableIOService = project().context().service(TableIOService.class);
		this.columns = columns;
		getActions().add(new DefaultAction(
				"Open table",
				Collections.singletonList(this),
				this::displayTable
		));
	}

	private void displayTable() {
		project().context().getService(UIService.class).show(getTable());
	}

	public boolean load() throws IOException {
		//TODO this is sadly the only way to make the table io plugin save and read both column and row headers
		//TODO https://github.com/scijava/scijava-table/pull/14
		Table _table = tableIOService.open(getFile().getAbsolutePath(), TableIOOptions.options().readColumnHeaders(true).readRowHeaders(true));
		if(columns != null) {
			table = SpecificTableBuilder.build(columns, _table);
		} else {
			table = _table;
		}
		return true;
	}

	@Override
	public boolean save() throws IOException {
		for (int col = 0; col < getTable().getColumnCount(); col++) {
			for (int row = 0; row < getTable().getRowCount(); row++) {
				Object val = getTable().get(col, row);
				if(val == null) getTable().set(col, row, "");
			}
		}
		//TODO this is sadly the only way to make the table io plugin save and read both column and row headers
		//TODO https://github.com/scijava/scijava-table/pull/14
		if(getFile() == null) {
			setFile(new File(project().getProjectDir(), getDefaultFileName()));
		}
		getFile().delete();
		tableIOService.save(getTable(), getFile().getAbsolutePath(), TableIOOptions.options().writeColumnHeaders(true).writeRowHeaders(true));
		return true;
	}

	public Table getTable() {
		if(exists() && table == null) {
			try {
				load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}
}
