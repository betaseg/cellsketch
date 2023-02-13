package de.frauzufall.cellsketch.model;

import java.io.IOException;
import java.util.List;

public interface Item {
	boolean exists();
	String getName();
	List<Action> getActions();
	int getColor();
	void setColor(int color);
	void loadConfig() throws IOException;
	void saveConfig() throws IOException;
    void display();
    boolean load() throws IOException;
    void unload();
    void delete() throws IOException;
}
