package sc.fiji.project;

import bdv.util.Bdv;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

public interface BdvProject extends Project {

	Bdv getBdv();
	BdvInterface labelEditorInterface();

}
