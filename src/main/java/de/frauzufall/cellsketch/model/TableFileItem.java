package de.frauzufall.cellsketch.model;

import org.scijava.table.Table;
import org.scijava.table.io.TableIOOptions;
import org.scijava.table.io.TableIOService;
import org.scijava.ui.UIService;
import de.frauzufall.cellsketch.BdvProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class TableFileItem extends FileItem {

	private final TableIOService tableIOService;
	private Table table;

	public TableFileItem(BdvProject app, boolean deletable) {
		this(app, null, deletable);
	}

	public TableFileItem(BdvProject app, String defaultFileName, boolean deletable) {
		super(app, defaultFileName, deletable);
		tableIOService = project().context().service(TableIOService.class);
		getActions().add(new DefaultAction(
				"Open table",
				Collections.singletonList(this),
				this::displayTable
		));
	}

	private void displayTable() {
		project().context().getService(UIService.class).show(getTable());
	}

	@Override
	public void display() {
		displayTable();
	}

	@Override
	public boolean load() throws IOException {
		if(getFile() == null || !getFile().exists()) return false;
		//TODO this is sadly the only way to make the table io plugin save and read both column and row headers
		//TODO https://github.com/scijava/scijava-table/pull/14
		table = tableIOService.open(getFile().getAbsolutePath(), TableIOOptions.options().readColumnHeaders(true).readRowHeaders(true));
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

	@Override
	public void loadConfig() throws IOException {
	}

	@Override
	public void saveConfig() throws IOException {
	}
}
