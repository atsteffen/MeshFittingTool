package app.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import app.RegistrationTool;
import app.data.Bin;
import app.tools.Log;

/** PermanentOptions.java
 * <br>
 * Toolbar displayed on the lower edge of the GUI. Offers options that are used regardless of which tab is open, 
 * such as toggling full screen, writing to a file, and (eventually) submitting selections to the server.
 * 
 * @author Don McCurdy
 *
 */
public class PermanentOptions extends JPanel {
	/** Default serial version UID  */
	private static final long serialVersionUID = 1L;
	private Color backgroundColor 		= Color.LIGHT_GRAY;
	private Image backgroundImage		= null;
	private String backgroundImageString = "wood_panel.jpg";
	private static final int ICON_SIZE = 20;

	private RegistrationTool app;
	private JToggleButton fullscreen;

	/** Initializes the panel. 
	 * 
	 * @param application
	 */
	public PermanentOptions (RegistrationTool mom) {
		this.app = mom;
		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		init();
	}

	/** Sets up all relevant components */
	private void init() {		
		FullScreenListener pLis = new FullScreenListener();
		fullscreen = new JToggleButton();
		fullscreen.setIcon(getIcon("expand.png"));
		fullscreen.setSelectedIcon(getIcon("collapse.png"));
		fullscreen.addActionListener(pLis);
		fullscreen.setToolTipText("Toggle fullscreen mode.");
		add(fullscreen);
		add(Box.createGlue());

		JToggleButton points = new JToggleButton(getIcon("points.png")), edges = new JToggleButton(getIcon("edges.png"),true), faces = new JToggleButton(getIcon("faces.png"));
		VisibilityListener viewLis = new VisibilityListener();
		points.setActionCommand(""+Viewer3D.ELEMENTS_POINTS);
		points.setToolTipText("Show/hide atlas points (Disabled by default)");
		edges.setActionCommand(""+Viewer3D.ELEMENTS_EDGES);
		edges.setToolTipText("Show/hide atlas edges (Enabled by default)");
		faces.setActionCommand(""+Viewer3D.ELEMENTS_FACES);
		faces.setToolTipText("Show/hide atlas faces (Disabled by default)");
		points.addActionListener(viewLis);
		edges.addActionListener(viewLis);
		faces.addActionListener(viewLis);
		add(points);
		add(edges);
		add(faces);

		add(Box.createGlue());
		add(Log.getStatusPanel());
		add(Box.createGlue());
		
		app.getViewer2D().setShowInnerSection(false);

		ImageIcon str = getIcon("photo.png");
		JToggleButton stream = new JToggleButton(str,false);
		stream.setToolTipText("Enable/disable image overlay on 2D view.");
		stream.addActionListener(new ShowImageStackListener() );
		add(stream);

		setBackground();
	}

	private ImageIcon getIcon(String name) {
		return getJarIcon(name);
	}

	public static ImageIcon getJarIcon(String name) {
		return getJarIcon(name, ICON_SIZE);  
	}
	
	public static ImageIcon getJarIcon (String name, int size) {
		String path = "icons/" + name;
		int MAX_IMAGE_SIZE = 5000;  //Change this to the size of your biggest image, in bytes.
		int count = 0;
		BufferedInputStream imgStream = new BufferedInputStream(
				Bin.getResourceClass().getResourceAsStream(path));
		byte buf[] = new byte[MAX_IMAGE_SIZE];
		try {
			count = imgStream.read(buf);
			imgStream.close();
		} catch (java.io.IOException ioe) {
			System.err.println("Couldn't read stream from file: " + path);
			return null;
		}
		if (count <= 0) {
			System.err.println("Empty file: " + path);
			return null;
		}
		Image img = Toolkit.getDefaultToolkit().createImage(buf);
		img = img.getScaledInstance(size,size, Image.SCALE_SMOOTH);
		return new ImageIcon(img);	 
	}

	/** Sets the background image. */
	private void setBackground() {
		try { 
			URL imageURL = Bin.getResourceClass().getResource(backgroundImageString);
			backgroundImage = ImageIO.read(imageURL);	
			this.setOpaque(false);
		}
		catch (Exception e) { Log.p("Background image not loaded: " + e); this.setBackground(backgroundColor); }
	}

	/** Toggles application to/from full-screen mode. 
	 * 
	 * @param setFullscreen
	 */
	public void setFullscreen(boolean fscreen) {
		if (fscreen) {
			fullscreen.setSelected(true);
			if (!app.setFullscreen(fscreen)) 
				fullscreen.setSelected(false);
		} else {
			app.setFullscreen(false);
			fullscreen.setSelected(false);
		}
	}

	/** Used to update/disable buttons when full-screen mode is toggled by keystroke. 
	 * 
	 * @param fullscreen
	 */
	public void didSetFullscreen(boolean full) {
		fullscreen.setSelected(full);
	}

	@Override
	/** Overrides the default paint method, allowing the background image to be painted first. 
	 * @param g
	 */
	public void paint(Graphics g) {
		if (backgroundImage != null) 
			g.drawImage(backgroundImage, 0,0, this.getWidth(), this.getHeight(), this);
		super.paint(g);
	}
	
	/** Listener that toggles the visibility of the Image stack behind the 2D view **/
	class ShowImageStackListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			app.getViewer2D().setStreaming(((JToggleButton)e.getSource()).isSelected());
		}
	}

	/** Listener that switches toggles visibility of points, edges and faces **/
	class VisibilityListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			app.getViewer3D().setVisible(Short.parseShort(e.getActionCommand()), ((JToggleButton)e.getSource()).isSelected());
		}

	}

	/** Listener that, when activated, toggles fullscreen mode */
	class FullScreenListener implements ActionListener {
		@Override
		public void actionPerformed (ActionEvent e) {
			Log.p("Toggling fullscreen mode.");
			setFullscreen(	(	(JToggleButton)(e.getSource()	)	).isSelected());
		}
	}
}
