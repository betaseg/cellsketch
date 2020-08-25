package sc.fiji.project;

import bdv.util.Bdv;
import org.scijava.Disposable;
import sc.fiji.labeleditor.plugin.interfaces.bdv.BdvInterface;

public interface BdvProject extends Project, Disposable {

	Bdv getBdv();
	BdvInterface labelEditorInterface();

}
