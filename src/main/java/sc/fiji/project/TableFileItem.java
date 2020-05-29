package sc.fiji.project;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;
import org.scijava.table.DefaultTableIOPlugin;
import org.scijava.table.GenericTable;
import org.scijava.ui.UIService;
import org.scijava.util.ClassUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class TableFileItem extends FileItem {

	private final TableColumnDefinition columns;
	private final PluginService pluginService;
	private GenericTable table;

	public TableFileItem(BdvProject app, String name) {
		this(app, name, null);
	}

	public TableFileItem(BdvProject app, String name, TableColumnDefinition columns) {
		super(app, name, "csv");
		pluginService = project().context().service(PluginService.class);
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
		PluginInfo<SciJavaPlugin> pluginInfo = pluginService.getPlugin(DefaultTableIOPlugin.class);
		DefaultTableIOPlugin plugin = (DefaultTableIOPlugin) pluginService.createInstance(pluginInfo);
		setValues(plugin, new String[]{"readRowHeaders", "readColHeaders"}, new Object[]{true, true});
		GenericTable _table = plugin.open(getFile().getAbsolutePath());
		if(columns != null) {
			table = SpecificTableBuilder.build(columns, _table);
		} else {
			table = _table;
		}
		return true;
	}

	private static void setValues(final Object instance, final String[] fieldNames,
	                             final Object[] values) throws SecurityException
	{
		final Class<?> cls = instance.getClass();
		final List<Field> fields = ClassUtils.getAnnotatedFields(cls,
				Parameter.class);
		final HashMap<String, Field> fieldMap = new HashMap<>();
		for (final Field field : fields) {
			fieldMap.put(field.getName(), field);
		}
		for (int i = 0; i < fieldNames.length; i++) {
			ClassUtils.setValue(fieldMap.get(fieldNames[i]), instance, values[i]);
		}
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
		PluginInfo<SciJavaPlugin> pluginInfo = pluginService.getPlugin(DefaultTableIOPlugin.class);
		DefaultTableIOPlugin plugin = (DefaultTableIOPlugin) pluginService.createInstance(pluginInfo);
		setValues(plugin, new String[]{"writeRowHeaders", "writeColHeaders"}, new Object[]{true, true});
		if(getFile() == null) {
			setFile(new File(project().getProjectDir(), getDefaultFileName()));
		}
		plugin.save(getTable(), getFile().getAbsolutePath());
		return true;
	}

	public GenericTable getTable() {
		if(exists() && table == null) {
			try {
				load();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return table;
	}

	public void setTable(GenericTable table) {
		this.table = table;
	}
}
