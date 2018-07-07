package app.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import app.RegistrationTool;
import app.tools.MeshLoader;
import app.tools.RegistrationUtils;
import app.tools.topology.Landmark;

public class ImageStackLoadingPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private RegistrationTool app;
	
	private JSpinner fitWeight;
	private JSpinner deformWeight;
	private JSpinner iters;
	
	public ImageStackLoadingPanel(RegistrationTool app) {
		super();
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.app = app;
		init();
	}
	
	private void init() {
		
		// loading
		JButton loadNewImageStack = new JButton("Load New Image Stack");
		loadNewImageStack.addActionListener(
				new ActionListener() { 
					@Override	
					public void actionPerformed(ActionEvent e) {
						JFileChooser chooser = new JFileChooser();
						int option = chooser.showOpenDialog(app);
						if (option == JFileChooser.APPROVE_OPTION) {
							File sf = chooser.getSelectedFile();
							try {
								loadImageStreamer(sf);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					} 
				});
		
		// extract boundary	
		JButton extractLandmarks = new JButton("Extract Boundary Landmarks");
		extractLandmarks.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						extractLandmarksFromStack();
						RegistrationUtils.startRegistrationSession(app.getMesh(), app.getSubLevel());
					}
				});
		
		// fitting	
		JPanel fitpanel = new JPanel();
		fitpanel.setLayout(new BoxLayout(fitpanel, BoxLayout.Y_AXIS));
		
		SpinnerNumberModel fitModel = new SpinnerNumberModel(1.0, 0, 100, 0.01);
		SpinnerNumberModel deformModel = new SpinnerNumberModel(1.0, 0, 100, 0.01);
		SpinnerNumberModel iterModel = new SpinnerNumberModel(1, 1, 50, 1);
		
		fitWeight = new JSpinner(fitModel);
		deformWeight = new JSpinner(deformModel);
		iters = new JSpinner(iterModel);
		
		JButton fitToBoundary = new JButton("Fit Mesh");
		fitToBoundary.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						RegistrationUtils.addLandmarksToFit(app.getLandmarks(), (Double) fitWeight.getValue(), (Double) deformWeight.getValue(), (Integer) iters.getValue());
					}
				});
		JPanel fitAndIterations = new JPanel();
		fitAndIterations.setLayout(new BoxLayout(fitAndIterations, BoxLayout.X_AXIS));
		
		fitAndIterations.add(fitToBoundary);
		fitAndIterations.add(Box.createHorizontalStrut(13));
		fitAndIterations.add(iters);
		
		JPanel fitpanelLabel = new JPanel();
		fitpanelLabel.setLayout(new BoxLayout(fitpanelLabel, BoxLayout.X_AXIS));
		fitpanelLabel.add(new JLabel("(step 3)"));
		fitpanelLabel.add(Box.createHorizontalGlue());
		
		fitpanel.add(Box.createVerticalGlue());
		fitpanel.add(fitpanelLabel);
		fitpanel.add(new JLabel("Fit Weight"));
		fitpanel.add(fitWeight);
		fitpanel.add(Box.createVerticalStrut(13));
		fitpanel.add(new JLabel("Iterations"));
		fitpanel.add(fitAndIterations);
		fitpanel.add(Box.createVerticalGlue());
		
		// saving		
		JPanel buttonpanel = new JPanel();
		buttonpanel.setLayout(new BoxLayout(buttonpanel, BoxLayout.Y_AXIS));
		
		JButton saveMeshBoundary = new JButton("Fix Atlas Boundary");
		saveMeshBoundary.addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						RegistrationUtils.saveRegistrationSession();
					}
				});
		
		buttonpanel.add(new JLabel("(step 1)"));
		buttonpanel.add(loadNewImageStack);
		buttonpanel.add(Box.createVerticalStrut(13));
		buttonpanel.add(new JLabel("(step 2)"));
		buttonpanel.add(extractLandmarks);
		buttonpanel.add(Box.createVerticalStrut(13));
		buttonpanel.add(new JLabel("(step 4)"));
		buttonpanel.add(saveMeshBoundary);
		
		add(Box.createHorizontalStrut(20));
		add(buttonpanel);
		add(Box.createHorizontalStrut(20));
		add(fitpanel);
		add(Box.createHorizontalStrut(20));
	}
	
	private void loadImageStreamer(File file) throws IOException {
		ImageStreamer streamer = MeshLoader.loadImageStreamer(file.getParent(),file.getName());
		if (!ImageStreamer.Z_AXIS.equals("null")) {
			streamer.setId(ImageStreamer.Z_AXIS);
			streamer.setAxis(app.getCutter().getN().getNormalized());
		}
		
		app.getViewer2D().setStreamer(streamer);
	}
	
	private void extractLandmarksFromStack() {
		ImageStreamer streamer = app.getViewer2D().getStreamer();		
		List<Landmark> landmarks = streamer.extractLandmarks(1.0,1);
		app.setLandmarks(landmarks);
	}

}
