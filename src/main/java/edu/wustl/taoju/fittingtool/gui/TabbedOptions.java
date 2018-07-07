package app.gui;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import app.RegistrationTool;

public class TabbedOptions extends JPanel {	
	private static final long serialVersionUID = 1L;

	// ******************************************************************************************************************************
	// Member Variables
	// ******************************************************************************************************************************
	
	private RegistrationTool app;
	private JTabbedPane tabs1;
	private JTabbedPane tabs2;

	// ******************************************************************************************************************************
	// Construction and Initialization
	// ******************************************************************************************************************************

	public TabbedOptions(RegistrationTool app, Viewer3D view) {
		super();
		this.app = app;
		this.setLayout(new GridLayout());
		init();
	}

	private void init() {
		
		tabs1 = new JTabbedPane();
		tabs1.add("Load Atlas and Landmarks", new MeshIOPanel(app));
		tabs1.add("Atlas Visualization", new VisualizationPanel(app));
		tabs2 = new JTabbedPane();
		tabs2.add("Load Image Stack", new ImageStackLoadingPanel(app));
		tabs2.add("Register Atlas to Landmarks", new InteractiveRegistrationPanel(app));

		super.add(tabs1);
		super.add(tabs2);
	}
}
