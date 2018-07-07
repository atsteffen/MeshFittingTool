package app.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

import app.RegistrationTool;
import app.tools.structure.Mesh;

public class InteractiveRegistrationPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private RegistrationTool app;
	
	private JSpinner fitWeight;
	private JSpinner deformWeight;
	private JSpinner iters;
	
	private JComboBox boundary1;
	private JComboBox boundary2;
	
	public InteractiveRegistrationPanel(RegistrationTool app) {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.app = app;
		init();
	}
	
	private void init() {
		JPanel regpanel = new JPanel();
		regpanel.setLayout(new BoxLayout(regpanel, BoxLayout.Y_AXIS));
	
		SpinnerNumberModel fitModel = new SpinnerNumberModel(1.0, 0, 100, 0.01);
		SpinnerNumberModel deformModel = new SpinnerNumberModel(1.0, 0, 100, 0.01);
		SpinnerNumberModel iterModel = new SpinnerNumberModel(1, 1, 50, 1);
		
		fitWeight = new JSpinner(fitModel);
		deformWeight = new JSpinner(deformModel);
		iters = new JSpinner(iterModel);
		
		JButton drawregButton = new JButton("Draw");
		drawregButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						app.toggleDrawLandmarks();
					} 
				}
		);
		JButton clearregButton = new JButton("Clear");
		clearregButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						app.clearActiveLandmarks();
					} 
				}
		);
		JButton fitregButton = new JButton("Fit");
		fitregButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						Double fit =1.0;
						Double def =1.0;
						Integer it =1;
						SpinnerModel dataModel = fitWeight.getModel();
						//fit = (Double) fitWeight.getValue();
						if (dataModel instanceof SpinnerNumberModel) {
							fit = (Double) (  (SpinnerNumberModel)dataModel ).getValue();
						}
						dataModel = deformWeight.getModel();
						if (dataModel instanceof SpinnerNumberModel) {
							def = (Double) (  (SpinnerNumberModel)dataModel ).getValue();
						}
						dataModel = iters.getModel();
						if (dataModel instanceof SpinnerNumberModel) {
							it = (Integer) (  (SpinnerNumberModel)dataModel ).getValue();
						}
						try {
							app.addLandmarksToFit(fit, def, it);
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					} 
				}
		);
		JButton saveregButton = new JButton("Save");
		saveregButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						app.saveActiveLandmarksAndFit();
					} 
				}
		);
		
		regpanel.add(new JLabel("Fit Weight"));
		regpanel.add(fitWeight);
		regpanel.add(new JLabel("Deform Weight"));
		regpanel.add(deformWeight);
		regpanel.add(new JLabel("iterations"));
		regpanel.add(iters);
		
		JPanel regsubpanel = new JPanel();
		regsubpanel.setLayout(new BoxLayout(regsubpanel, BoxLayout.X_AXIS));
		regsubpanel.add(clearregButton);
		regsubpanel.add(fitregButton);
		regsubpanel.add(saveregButton);
		
		regpanel.add(regsubpanel);
		
		// Boundary Picker interface
		JPanel boundaryPicker = new JPanel();
		boundaryPicker.setLayout(new BoxLayout(boundaryPicker, BoxLayout.Y_AXIS));

		int [] ids2 = app.getMesh().getPartitionIDs();
		String[] boundaries = new String[ids2.length+2];
		boundaries[0] = "Unknown Region";
		boundaries[1] = "Empty Region";
		for (int i = 2; i < ids2.length+2; ++i) {
			boundaries[i] = Mesh.getName(ids2[i-2]);
		}
		boundary1 = new JComboBox(boundaries);
		boundary1.setSelectedIndex(0);
		RegistrationTool.instance.setActiveMaterials(0,-2);
		boundary1.addActionListener(new ActionListener() { 
			@Override	
			public void actionPerformed(ActionEvent e) {
				int [] ids2 = app.getMesh().getPartitionIDs();
				JComboBox cb = (JComboBox)e.getSource();
				int index = cb.getSelectedIndex();
				if (index > 1) {
					index = ids2[index-2];
				}
				else {
					index = index - 2;
				}
				RegistrationTool.instance.setActiveMaterials(0,index);
			}
		});
		boundary1.setMaximumRowCount(5);

		boundary2 = new JComboBox(boundaries);
		boundary2.setSelectedIndex(1);
		RegistrationTool.instance.setActiveMaterials(1,-1);
		boundary2.addActionListener(new ActionListener() { 
			@Override	
			public void actionPerformed(ActionEvent e) {
				int [] ids2 = app.getMesh().getPartitionIDs();
				JComboBox cb = (JComboBox)e.getSource();
				int index = cb.getSelectedIndex();
				if (index > 1) {
					index = ids2[index-2];
				}
				else {
					index = index - 2;
				}
				RegistrationTool.instance.setActiveMaterials(1,index);
			}
		});
		boundary2.setMaximumRowCount(4);

		boundaryPicker.add(new JLabel("Boundary 1:"));
		boundaryPicker.add(boundary1);
		boundaryPicker.add(new JLabel("Boundary 2:"));
		boundaryPicker.add(boundary2);
		boundaryPicker.add(Box.createVerticalStrut(70));

		add(Box.createHorizontalGlue());
		add(regpanel);
		add(Box.createHorizontalStrut(20));
		add(boundaryPicker);
		add(Box.createHorizontalGlue());
	}

}
