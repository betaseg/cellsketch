package sc.fiji.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SynapsesTable implements TableColumnDefinition {

	private static final String size = "size";

	private static final List<String> columns = createColumns();

	private static List<String> createColumns() {
		List<String> result = new ArrayList<>();
		result.add(size);
		return Collections.unmodifiableList(result);
	}

	public static int getSizeColumn() { return columns.indexOf(size); }

	@Override
	public List<String> getColumns() {
		return columns;
	}
}
