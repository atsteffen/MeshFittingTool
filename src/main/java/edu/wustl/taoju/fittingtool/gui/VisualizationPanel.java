package app.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import app.RegistrationTool;
import app.tools.Log;
import app.tools.cross_sections.CuttingTool;
import app.tools.structure.Mesh;

public class VisualizationPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private RegistrationTool app;
	
	public VisualizationPanel(RegistrationTool app) {
		super();
		this.app = app;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		init();
	}
	
	private void init() {
		JPanel selectionPanel = new JPanel();
		selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.X_AXIS));

		/* AXIS SELECTION */
		JPanel axispanel = new JPanel();
		axispanel.setLayout(new BoxLayout(axispanel, BoxLayout.Y_AXIS));
		axispanel.setMaximumSize(new Dimension(200, 150));
		axispanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		axispanel.add(new JLabel("<html><i>Axis</i></html>"));
		
		JRadioButton x = new JRadioButton("X"), y = new JRadioButton("Y");
		JRadioButton z = new JRadioButton("Z");
		x.setActionCommand("" + 0);
		y.setActionCommand("" + 1);
		z.setActionCommand("" + 2);
		x.setToolTipText("Side view");
		y.setToolTipText("Top view");
		z.setToolTipText("Front view");
		AxisListener axLis = new AxisListener();
		x.addActionListener(axLis);
		y.addActionListener(axLis);
		z.addActionListener(axLis);
		ButtonGroup aB = new ButtonGroup();
		aB.add(x);
		aB.add(y);
		aB.add(z);
		z.setSelected(true);
		axispanel.add(Box.createGlue());
		axispanel.add(x);
		axispanel.add(y);
		axispanel.add(z);
		axispanel.add(Box.createGlue());
		axispanel.setPreferredSize(new Dimension(50, 150));
		axispanel.setBorder(null);
		
		/* REGION VISIBILITY */
		int[] ids = app.getViewer3D().getComponentList();
		JCheckBox[] compList = new JCheckBox[ids.length];
		ComponentListListener lis = new ComponentListListener();
		JPanel compListPanel = new JPanel();
		compListPanel.setLayout(new BoxLayout(compListPanel, BoxLayout.Y_AXIS));
		compListPanel.setOpaque(true);
		compListPanel
				.add(new JLabel("<html><i>Visible Partitions:</i></html>"));
		JCheckBox header = new JCheckBox("<html><i>Select All</i></html>");
		header.setActionCommand("all");
		header.setSelected(true);
		header.addActionListener(lis);
		compListPanel.add(header);
		lis.setHeader(header);

		for (int i = 0; i < ids.length; ++i) {
			compList[i] = new JCheckBox(Mesh.getName(ids[i]));
			compList[i].setActionCommand("" + ids[i]); // Set the 'id' value
			compList[i].addActionListener(lis);
			compList[i].setSelected(true);
			compList[i].setOpaque(false);
			lis.addCheckBox(ids[i], compList[i]); // Give the listener access to
													// the checkbox, so we can
													// see if we're disabling or
													// enabling.
			compListPanel.add(compList[i]);
		}
		compListPanel.setBackground(Color.WHITE);
		JScrollPane scrollPane = new JScrollPane(compListPanel);
		scrollPane.setPreferredSize(new Dimension(150, 150));
		scrollPane.setMaximumSize(new Dimension(150, 150));
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED));

		/* CUTTING PLANE VISUALIZATION */
		JPanel visPanel = new JPanel();
		visPanel.setLayout(new BoxLayout(visPanel, BoxLayout.Y_AXIS));
		visPanel.add(new JLabel(
				"<html><i>Cutting-plane <br>Visualization:</i></html> "));
		visPanel.add(Box.createVerticalStrut(20));
		PlaneVisListener vLis = new PlaneVisListener();
		JRadioButton none = new JRadioButton("None");
		none.setActionCommand("" + CuttingTool.NONE);
		none.addActionListener(vLis);
		none.setToolTipText("Cutting plane is hidden in 3D view.");
		JRadioButton plane = new JRadioButton("Plane");
		plane.setActionCommand("" + CuttingTool.PLANE);
		plane.addActionListener(vLis);
		plane.setToolTipText("Cutting plane appears as a transparent plane in 3D view.");
		JRadioButton cutaway = new JRadioButton("Cut-away");
		cutaway.setActionCommand("" + CuttingTool.CUTAWAY);
		cutaway.addActionListener(vLis);
		cutaway.setToolTipText("Model is opened at the cutting plane in 3D view.");
		ButtonGroup planeVis = new ButtonGroup();
		planeVis.add(none);
		planeVis.add(plane);
		planeVis.add(cutaway);
		none.setSelected(true);
		visPanel.add(none);
		visPanel.add(plane);
		visPanel.add(cutaway);
		visPanel.setPreferredSize(new Dimension(130, 150));
		visPanel.setMaximumSize(new Dimension(130, 150));
		visPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		visPanel.add(app.getCutter().getSlider());
		
		/* ___________________ */
		selectionPanel.add(Box.createHorizontalStrut(40));
		selectionPanel.add(axispanel);
		selectionPanel.add(Box.createGlue());
		selectionPanel.add(Box.createHorizontalStrut(20));
		selectionPanel.add(visPanel);
		selectionPanel.add(Box.createHorizontalStrut(20));
		selectionPanel.add(scrollPane);
		
		add(selectionPanel);
	}
	
	class AxisListener implements ActionListener {
		private final static float ALPHA = 0f;

		@Override
		public void actionPerformed(ActionEvent arg0) {
			switch (Integer.parseInt(arg0.getActionCommand())) {
			case 0:
				app.getCutter().setAngleX(0f + ALPHA);
				app.getCutter().setAngleY(-90f + ALPHA);	
				app.getViewer2D().setImageStreamerAxis(ImageStreamer.X_AXIS, app.getCutter().getN());
				break;
			case 1:
				app.getCutter().setAngleX(90f + ALPHA);
				app.getCutter().setAngleY(0f + ALPHA);
				app.getViewer2D().setImageStreamerAxis(ImageStreamer.Y_AXIS, app.getCutter().getN());
				break;
			case 2:
				app.getCutter().setAngleX(0);
				app.getCutter().setAngleY(0);
				app.getViewer2D().setImageStreamerAxis(ImageStreamer.Z_AXIS, app.getCutter().getN());
				break;
			default:
				Log.p("ERROR: Preset axis index out of bounds.");
			}
			app.getViewer2D().setPan(0,0);
			app.getViewer2D().setZoom(1.0f);
			app.getLassoTool().clear();

		}

	}
	
	class PlaneVisListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			app.getCutter().setMode(Integer.parseInt(arg0.getActionCommand()));
		}

	}
	
	class ComponentListListener implements ActionListener {
		private HashMap<Integer, JCheckBox> boxes = new HashMap<Integer, JCheckBox> ();
		private JCheckBox header;

		public void setHeader(JCheckBox header) { this.header = header; }
		public void addCheckBox(int i, JCheckBox box) {
			boxes.put((Integer)i, box);
		}

		public void actionPerformed(ActionEvent e) {
			if (e.getActionCommand().equals("all")) {
				for (JCheckBox b : boxes.values()) {
					b.setSelected(((JCheckBox)e.getSource()).isSelected());
					app.getViewer3D().setRegionVisibility(Integer.parseInt(b.getActionCommand()),b.isSelected());
				}
				return;
			}

			int id = Integer.parseInt(e.getActionCommand());
			app.getViewer3D().setRegionVisibility(id, boxes.get(id).isSelected());

			boolean allSelected = true;
			for (JCheckBox b : boxes.values()) if (!b.isSelected()) allSelected = false;
			if (header != null) header.setSelected(allSelected);
		}
	}

}
