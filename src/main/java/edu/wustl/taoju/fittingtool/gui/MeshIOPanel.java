package app.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import app.RegistrationTool;

public class MeshIOPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	private RegistrationTool app;
	
	public MeshIOPanel(RegistrationTool app) {
		super();
		this.app = app;
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		init();
	}
	
	private void init() {
		// IO Interface
		JButton loadExistingMeshButton = new JButton("Load Saved Atlas");
		loadExistingMeshButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							app.loadMesh(sf);
						}
					}
				});
		JButton loadmeshButton = new JButton("Start New Atlas");
		loadmeshButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							app.loadMesh(sf);
						}
					}
				});
		JButton loadlandmarksButton = new JButton("Import Landmarks");
		loadlandmarksButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							try {
								app.loadLandmarks(sf.getCanonicalPath());
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				});

		JButton savelandmarksButton = new JButton("Export Landmarks");
		savelandmarksButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							app.saveLandmarks(sf);
						}
					}
				});
		
		JButton saveMeshButton = new JButton("Save Atlas");
		saveMeshButton.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							try {
								app.writeMesh(sf.getCanonicalPath());
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				});
		
		JPanel iopanel = new JPanel();
		iopanel.setLayout(new BoxLayout(iopanel, BoxLayout.Y_AXIS));
		iopanel.add(new JLabel("Start Here:"));
		iopanel.add(Box.createVerticalStrut(13));
		iopanel.add(loadmeshButton);
		iopanel.add(Box.createVerticalStrut(5));
		iopanel.add(new JLabel(" -- or -- "));
		iopanel.add(Box.createVerticalStrut(5));
		iopanel.add(loadExistingMeshButton);
		
		JPanel finalPanelOuter = new JPanel();
		JPanel finalPanel = new JPanel();
		JPanel finalPanelInner = new JPanel();
		finalPanelOuter.setLayout(new BoxLayout(finalPanelOuter, BoxLayout.Y_AXIS));
		finalPanel.setLayout(new BoxLayout(finalPanel, BoxLayout.Y_AXIS));
		finalPanelInner.setLayout(new BoxLayout(finalPanelInner, BoxLayout.Y_AXIS));
		finalPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		finalPanelInner.setBorder(new EmptyBorder(5, 5, 5, 5));
		finalPanelInner.add(saveMeshButton);
		finalPanel.add(finalPanelInner);
		finalPanelOuter.add(new JLabel("Finished Mesh:"));
		finalPanelOuter.add(Box.createVerticalStrut(13));
		finalPanelOuter.add(finalPanel);
		
		JPanel landmarkpanel = new JPanel();
		landmarkpanel.setLayout(new BoxLayout(landmarkpanel, BoxLayout.Y_AXIS));
		landmarkpanel.add(new JLabel("Save or load landmarks:"));
		landmarkpanel.add(Box.createVerticalStrut(13));
		landmarkpanel.add(loadlandmarksButton);
		landmarkpanel.add(Box.createVerticalStrut(13));
		landmarkpanel.add(savelandmarksButton);
		
		JPanel loadSavePanel = new JPanel();
		loadSavePanel.setLayout(new BoxLayout(loadSavePanel, BoxLayout.Y_AXIS));
		loadSavePanel.add(Box.createVerticalGlue());
		loadSavePanel.add(iopanel);
		loadSavePanel.add(Box.createVerticalGlue());
		
		add(Box.createHorizontalGlue());
		add(loadSavePanel);
		add(Box.createHorizontalStrut(13));
		add(landmarkpanel);
		add(Box.createHorizontalStrut(13));
		add(finalPanelOuter);
		add(Box.createHorizontalGlue());
	}

}
