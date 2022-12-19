package sc.fiji.project;

import net.imglib2.type.numeric.ARGBType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractItem implements Item {
	private String name;
	private List<Action> actions = new ArrayList<>();
	private int color;

	public AbstractItem(String name) {
		setName(name);
		color = ARGBType.rgba(255,255,255,255);
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public List<Action> getActions() {
		return actions;
	}

	@Override
	public int getColor() {
		return color;
	}

	@Override
	public void setColor(int color) {
		this.color = color;
	}

	@Override
	public boolean load() throws IOException {
		return true;
	}

	@Override
	public void unload() {
	}

	public String nameToFileName() {
		return getName().replace(" ", "_").toLowerCase();
	}
}
