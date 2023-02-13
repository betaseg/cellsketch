package de.frauzufall.cellsketch.ui;

import de.frauzufall.cellsketch.command.*;
import net.miginfocom.swing.MigLayout;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import de.frauzufall.cellsketch.BdvProject;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectActionsCard extends JPanel {

	private final JPopupMenu popup = new JPopupMenu();
	private final JLabel fish = new JLabel("");
	private final JTextArea progressText = new JTextArea("");
	List<String> progresses = new ArrayList<>();
	private JPanel fishTank;
	private JPanel buttons;

	public void build(String title, BdvProject project) {
		setLayout(new MigLayout("fill, ins 0, hidemode 1"));
		final JPopupMenu popup = new JPopupMenu();
		addOption(project, popup, "Add mask", AddMask.class);
		addOption(project, popup, "Add labels", AddLabelMap.class);
		addOption(project, popup, "Add boundary (i.e. membrane)", AddBoundary.class);
		addOption(project, popup, "Add filaments from KNOSSOS", AddFilamentsFromKNOSSOS.class);
		final JButton actionsButton = new JButton("Add dataset...");
		actionsButton.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		});
		buttons = new JPanel(new MigLayout("ins 0, fill"));
		buttons.add(actionsButton, "push, grow, span, id actions");
		addBtn(buttons, project, "Analyze", CellSketchAnalyzer.class);
		add(buttons, "span, grow");

		fishTank = new JPanel(new MigLayout("ins -50"));
		fishTank.setBorder(new LineBorder(new Color(0, 0, 20, 20), 50, true));
		fish.setIcon(new ImageIcon(getClass().getResource("/icons/fish2.gif")));
		updateStatus();
		fishTank.add(fish);
		fishTank.setOpaque(false);
		add(fishTank, "pos 0.5al 0.5al");
		this.setComponentZOrder(fishTank, 0);
	}

	void setPanelEnabled(JComponent panel, Boolean isEnabled) {
		panel.setEnabled(isEnabled);
		Component[] components = panel.getComponents();
		for (Component component : components) {
			if (component instanceof JPanel) {
				setPanelEnabled((JPanel) component, isEnabled);
			}
			component.setEnabled(isEnabled);
		}
	}

	public void updateStatus() {
		popup.removeAll();
		setPanelEnabled(buttons, !showProgress());
		fishTank.setVisible(showProgress());
		progressText.setVisible(showProgress());
		StringBuilder text = new StringBuilder();
		for(String title : progresses) {
			text.append(title);
		}
		progressText.setText(text.toString());
		fish.updateUI();
		this.updateUI();
	}

	private boolean showProgress() {
		return progresses.size() > 0;
//		return true;
	}

	public void addProgress(String title) {
		System.out.println("adding progress " + title);
		progresses.add(title);
		updateStatus();
	}

	public void removeProgress(String title) {
		progresses.remove(title);
		updateStatus();
	}

	private void addOption(BdvProject project, JPopupMenu popup, String name, Class<? extends Command> commandClass) {
		popup.add(new JMenuItem(new AbstractAction(name) {
			public void actionPerformed(ActionEvent e) {
				runCommand(project, commandClass);
			}
		}));
	}

	private void addBtn(JPanel panel, BdvProject project, String add_mask, Class<? extends Command> commandClass) {
		JButton addMaskBtn = new JButton(add_mask);
		addMaskBtn.setOpaque(false);
		addMaskBtn.addActionListener(e -> runCommand(project, commandClass));
		panel.add(addMaskBtn, "push, grow, span");
	}

	public void runCommand(BdvProject project, Class<? extends Command> commandClass) {
		Map<String, Object> options = new HashMap<>();
		options.put("project", project.getProjectDir().getAbsolutePath());
		options.put("projectObject", project);
		project.context().service(CommandService.class).run(commandClass, true, options);
	}
}
