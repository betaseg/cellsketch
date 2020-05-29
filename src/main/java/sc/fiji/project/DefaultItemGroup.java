package sc.fiji.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultItemGroup implements ItemGroup {
	private final String name;
	private final List<Item> items;

	public DefaultItemGroup(String name) {
		this.name = name;
		items = new ArrayList<>();
	}

	@Override
	public List<Item> getItems() {
		return items;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<Action> getActions() {
		return null;
	}

	@Override
	public int getColor() {
		return 0;
	}

	@Override
	public void setColor(int color) {
	}

	@Override
	public void loadConfigFrom(Map<String, Object> data) {
		items.forEach(item -> {
			loadItemConfig(data, item);
		});
	}

	protected void loadItemConfig(Map<String, Object> data, Item item) {
		Map<String, Object> config = (Map<String, Object>) data.getOrDefault(item.getName(), new LinkedHashMap<>());
		item.loadConfigFrom(config);
	}

	@Override
	public void saveConfigTo(Map<String, Object> data) {
		for (Item item : getItems()) {
			Map<String, Object> config = new LinkedHashMap<>();
			item.saveConfigTo(config);
			data.put(item.getName(), config);
		}
	}
}
