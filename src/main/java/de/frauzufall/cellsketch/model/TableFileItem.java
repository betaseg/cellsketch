/*-
 * #%L
 * cellsketch
 * %%
 * Copyright (C) 2020 - 2023 Deborah Schmidt
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
