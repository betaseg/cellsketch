package sc.fiji.project;

import bdv.util.BdvSource;

import java.util.List;

public interface DisplayableInBdv extends Item {

	void addToBdv();
	void updateBdvColor();

	void setVisible(boolean visible);
	boolean isVisible();

	List<BdvSource> getSources();
	void setSources(List<BdvSource> sources);

	default void removeFromBdv() {
		if(getSources() == null) return;
		getSources().forEach(BdvSource::removeFromBdv);
		getSources().clear();
	}

	default void displayIfExists() {
		if(exists()) addToBdv();
	}

	@Override
	default void display() {
		if(!isVisible()) addToBdv();
	}

}
