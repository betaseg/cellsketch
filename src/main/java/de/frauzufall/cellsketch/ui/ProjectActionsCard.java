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
	}

	public void setEnabled(boolean enabled) {
		this.setPanelEnabled(buttons, enabled);
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
