package app.tools.cross_sections;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


import app.RegistrationTool;
import app.gui.Viewer2D;
import app.tools.Log;
import app.tools.MyJSlider;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.Mesh;
import app.tools.topology.Landmark;
import app.tools.topology.Polyhedron;

/** LassoStackingTool.java
 * 
 * Manages the "stacked lasso" advanced selection operation.
 *
 * @author Don McCurdy
 *
 */
public class LassoStackingTool implements ChangeListener,ActionListener {
	/* Class References */
	/** Application */ 							private RegistrationTool app;
	/** GUI Panel */								private JPanel controlPanel;
	/** Lasso Stack */							private LassoList stack;

	/* Status Info */
	/** Axis */ 									private Vector n;
	/** Current point on axis */ 			private Vertex p;
	/** Needs to run selection */			private boolean awaitingSelection = false;
	/** Tracks enabled regions */		private HashMap<Integer,Boolean> enabledRegions = new HashMap<Integer,Boolean>();
	/** Corresponding checkboxes */ 	private JCheckBox [] compList;
	/** Select/deselect all */				private JCheckBox		header;


	//******************************************************************************************************************************
	//
	//			INITIALIZATION
	//
	//******************************************************************************************************************************

	/** Constructor, which takes in an application, model, and cutting tool. */
	public LassoStackingTool (RegistrationTool app) {
		this.app = app;
		this.stack = new LassoList();

		if (app.getMesh() != null) {
		initListeners();
		initControlPanel();
		setEnabled(false);
		}
	}

	protected CuttingTool getCutter() {
		return app.getCutter();
	}

	//******************************************************************************************************************************
	//
	//			GUI ELEMENTS & CHANGE LISTENER IMPLEMENTATION 
	//
	//******************************************************************************************************************************
	private JButton selectVolume = new JButton("Select Volume"), resetVolume = new JButton("Reset");
	private JButton newLasso = new JButton("+"), closeLasso = new JButton("Close"), deleteLasso = new JButton("Ð");
	private JButton nextLasso = new JButton(">>"), previousLasso = new JButton("<<");
	private JLabel currentLasso = new JLabel("0 / 0");
	private MyJSlider slider = new MyJSlider(0,1000,500);

	/** Initialize the UI components to pass all events on to this LassoStackingTool object */
	private void initListeners() {
		this.selectVolume.addActionListener(this);
		this.resetVolume.addActionListener(this);
		this.newLasso.addActionListener(this);
		this.deleteLasso.addActionListener(this);
		this.closeLasso.addActionListener(this);
		this.nextLasso.addActionListener(this);
		this.previousLasso.addActionListener(this);	
		this.slider.addChangeListener(this);
	}

	/**  Enables all (appropriate) UI elements and calls updateNav() when done. 
	 * @param isOn
	 * */
	public void setEnabled(boolean isOn) {
		selectVolume.setEnabled(isOn);
		resetVolume.setEnabled(isOn);
		newLasso.setEnabled(isOn);
		deleteLasso.setEnabled(isOn);
		closeLasso.setEnabled(isOn);
		nextLasso.setEnabled(isOn);
		previousLasso.setEnabled(isOn);
		slider.setEnabled(isOn);
		updateNav();
	}

	/** Initializes the JPanel that represents this tool in the GUI */
	private void initControlPanel() {
		controlPanel = new JPanel(); 		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));		controlPanel.setOpaque(true);	//Initialize panels
		controlPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		JPanel leftPanel = new JPanel(); 	leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS)); 				leftPanel.setOpaque(false);
		JPanel rowPanel = new JPanel(); rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));				rowPanel.setOpaque(false);
		JPanel topPanel = new JPanel(); topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));					topPanel.setOpaque(false);

		previousLasso.setToolTipText("Moves cutting plane to position of previous contour.");
		nextLasso.setToolTipText("Moves cutting plane to position of next contour.");
		currentLasso.setToolTipText("Whole numbers correspond to positions of user-defined contours. Decimal values indicate relative distance to existings contours.");
		rowPanel.add(Box.createGlue());
		rowPanel.add(previousLasso);// bottom (row) panel
		rowPanel.add(currentLasso);
		rowPanel.add(nextLasso);
		rowPanel.add(Box.createGlue());

		selectVolume.setToolTipText("If two or more (2+) contours have been defined, selects the region of the atlas inside these contours.");
		resetVolume.setToolTipText("Deletes all defined contours.");
		topPanel.add(selectVolume);
		topPanel.add(resetVolume);

		leftPanel.add(Box.createGlue());	
		leftPanel.add(topPanel);			
		leftPanel.add(Box.createGlue());// left panel
		JPanel miniRow = new JPanel(); miniRow.setLayout(new BoxLayout(miniRow, BoxLayout.X_AXIS)); miniRow.setOpaque(false);
		//miniRow.add(new JLabel("Contour:"));
		newLasso.setToolTipText("Start adding a new contour at current cutting-plane position. Must be clicked before points can be put onto 2D view.");
		deleteLasso.setToolTipText("Remove lasso at current cutting-plane position.");
		closeLasso.setToolTipText("Finish adding a new contour at current cutting-plane position. Must be clicked to connect first and last user-defined points.");
		miniRow.add(newLasso);
		miniRow.add(deleteLasso);
		miniRow.add(closeLasso);
		leftPanel.add(miniRow);
		leftPanel.add(Box.createGlue());	
		leftPanel.add(rowPanel);
		//leftPanel.add(slider);

		int [] ids = app.getMesh().getPartitionIDs();
		for (int i : ids) enabledRegions.put(i, true);
		compList = new JCheckBox[ids.length];
		JPanel compListPanel = new JPanel();
		compListPanel.setLayout(new BoxLayout(compListPanel, BoxLayout.Y_AXIS));
		compListPanel.setOpaque(true);
		compListPanel.add(new JLabel("<html><i>Selected Partitions:</i></html>" ));
		header = new JCheckBox("<html><i>Select All</i></html>");
		header.setActionCommand("all");
		header.setSelected(true);
		header.addActionListener(this);
		header.setToolTipText("Enable/disable all regions for selection by lasso tool.");
		compListPanel.add(header);

		for (int i = 0; i < ids.length; ++i) {
			compList[i] = new JCheckBox(Mesh.getName(ids[i]));	
			compList[i].setActionCommand("" + ids[i]);					//Set the 'id' value
			compList[i].addActionListener(this);
			compList[i].setSelected(true);
			compList[i].setOpaque(false);
			compList[i].setToolTipText("When disabled, " + Mesh.getName(ids[i]) + " region is ignored (never selected) by lasso tool.");
			//lis.addCheckBox(ids[i], compList[i]);								//Give the listener access to the checkbox, so we can see if we're disabling or enabling.
			compListPanel.add(compList[i]);
		}
		compListPanel.setBackground(Color.WHITE);
		JScrollPane scrollPane = new JScrollPane(compListPanel);
		scrollPane.setPreferredSize(new Dimension(200, 130));
		scrollPane.setMaximumSize(new Dimension(200,130));
		scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED));

		JToggleButton edges = new JToggleButton("E"); edges.addActionListener(this); edges.setActionCommand("edge");edges.setSelected(true);
		JToggleButton faces = new JToggleButton("F"); faces.addActionListener(this); faces.setActionCommand("face");	faces.setSelected(true);
		JToggleButton points = new JToggleButton("V"); points.addActionListener(this); points.setActionCommand("point"); points.setSelected(true);
		JPanel test = new JPanel(); test.setLayout(new BoxLayout(test, BoxLayout.X_AXIS));
		test.add(edges); test.add(faces); test.add(points);
		//leftPanel.add(test); //Re-enable to toggle options related to how the tool determines which polyhedra are inside the contoured region.

		controlPanel.add(leftPanel);	// main panel
		controlPanel.add(Box.createGlue());
		controlPanel.add(scrollPane);

	}
	public JPanel getControlPanel() { return controlPanel; }

	/** Moves the slider to a given position, and updates P with the cutting tool's value. */
	public void setSliderVisual(float value) {
		slider.setValue(value);
		this.p = getCutter().getP();
		updateNav();
	}

	/** Listens for slider events */
	public void stateChanged(ChangeEvent arg0) {
		if (arg0.getSource() == this.slider) {
//			Log.p("Lasso slider is doing stuff. Eh?"); //TODO LassoTool's slider isn't ever displayed, no?
//			getCutter().setPan(slider.getValuef());
//			this.p = getCutter().getP();
//			updateNav();
			//this.currentIndex = stack.getIndexNice(p);
		}
	}

	public void clear() {
		this.stack.clear();
		getCutter().refresh();
		this.n = getCutter().getN();
		this.p = getCutter().getP();
		updateNav();
	}

	/** Listens for JButton events */
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getActionCommand().equals("edge")) {
			testEdges = !testEdges;
		}else if (arg0.getActionCommand().equals("face")) {
			testFaces = ! testFaces;
		}else if (arg0.getActionCommand().equals("point")) {
			testVertices = ! testVertices;
		}else if (arg0.getSource() == selectVolume) {															//***** Select contents
			awaitingSelection = !stack.isEmpty();
		} else if (arg0.getSource() == resetVolume) {															//***** Reset everything
			clear();
		} else if (arg0.getSource() == newLasso) {																//***** Create a new lasso ...
			if(stack.size() == 0) {
				this.n = getCutter().getN();
				this.p = getCutter().getP();
				stack.add(new Lasso(p, n, slider.getValuef()));
				updateNav();
				slider.setEnabled(false);
			} else {
				this.p = getCutter().getP();
				stack.add(stack.get(p,true).setSliderValue(slider.getValuef()));
				updateNav();
			}
		} else if (arg0.getSource() == deleteLasso) {															//***** Delete lasso
			if (stack.size() != 0) {
				float current = stack.getIndex(p);
				if (current%1.0f == 0.0f) {
					stack.remove((int)current);
					updateNav();
				}
			}
		} else if (arg0.getSource() == closeLasso) {																//***** Close off first lasso
			if (!stack.getFirst().getClosed() && stack.getFirst().size() > 2) {
				this.stack.getFirst().setClosed(true);
				this.closeLasso.setEnabled(false);
				slider.setEnabled(true);
			}
		} else if (arg0.getSource() == nextLasso) {																//***** Next
			float current = stack.getIndexNice(p);
			float next = current;
			setSliderVisual(stack.get((int)next).getSliderValue());
		} else if (arg0.getSource() == previousLasso) {														//***** Previous
			float current = stack.getIndexNice(p)-1.0f;
			float previous = (current%1.0f == 0) ? current - 1.0f : current;
			setSliderVisual(stack.get((int)previous).getSliderValue());
		} else if (arg0.getActionCommand().equals("all")) {
			for (JCheckBox b : compList) {
				b.setSelected(((JCheckBox)arg0.getSource()).isSelected());
				enabledRegions.put(Integer.parseInt(b.getActionCommand()),b.isSelected());
			}
		} else if (arg0.getSource() instanceof JCheckBox){
			JCheckBox region = ((JCheckBox)arg0.getSource());
			enabledRegions.put(Integer.parseInt(region.getActionCommand()), region.isSelected());
			boolean allSelected = true;
			for (JCheckBox b : compList) if (!b.isSelected()) allSelected = false;
			header.setSelected(allSelected);
		}

	}

	//******************************************************************************************************************************
	//
	//			AUTOMATIC NAVIGATION
	//
	//******************************************************************************************************************************

	/** Updates the interface, disabling and enabling buttons when appropriate. Operates separately from the setEnabled() method, which just disables everything.*/
	private void updateNav() {
		//if (!enable.isSelected()) return;

		if (stack.size() != 0) {
			float currentIndex = stack.getIndexNice(p);
			currentLasso.setText(prettyFloat(currentIndex) + " / " + stack.size());
			previousLasso.setEnabled( currentIndex > 1 );
			deleteLasso.setEnabled(currentIndex%1.0f == 0.0f);
			newLasso.setEnabled(currentIndex%1.0f != 0.0f);
			nextLasso.setEnabled(currentIndex < stack.size());
			closeLasso.setEnabled(stack.size() == 1 && !stack.getFirst().getClosed() && stack.getFirst().size() > 2);
		} else {
			currentLasso.setText("0 / 0");
			previousLasso.setEnabled(false);
			nextLasso.setEnabled(false);
			deleteLasso.setEnabled(false);
			newLasso.setEnabled(true);
			closeLasso.setEnabled(false);
		}
	}

	private String prettyFloat(float f) {
		return String.format("%.3f", f);
	}


	//******************************************************************************************************************************
	//
	//			MOUSE HANDLING
	//
	//******************************************************************************************************************************
	private boolean nextIsReleased = false;
	private boolean isDraggingVertex = false;
	private int selectedVertex = -1;
	private float alpha = 0.2f;

	/** Notifies the tool that the next mouseevent has been followed by a mouse-release event.
	 *  (Meaning, it's either an isolated click or the end of a mouse-drag) */
	public void mouseRelease() {
		//if (isDraggingVertex) 
		nextIsReleased = true;
		selectedVertex = -1;
		isDraggingVertex = false;
	}
	
	private static int lastLocX;
	private static int lastLocY;
	
	public void mouseDrag(GL gl, int mouseX, int mouseY) {

		Vertex location = get3DPoint(gl,mouseX,mouseY);
		Landmark lm = new Landmark(location);
		lm.addMaterial(app.getActiveMaterials().get(0));
		lm.addMaterial(app.getActiveMaterials().get(1));
				
		int index = (int) (app.getActiveMaterials().size()*java.lang.Math.random());
		Integer mat = app.getActiveMaterials().get(index);
		if (mat.equals(-2)) {
			if (app.getActiveMaterials().get((index+1)%2).equals(-2)){
				lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
			}
			else if (app.getActiveMaterials().get((index+1)%2).equals(-1)) {
				lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
			}
			else {
				Color c = Mesh.getColor(app.getActiveMaterials().get((index+1)%2));
				lm.setDisplayColor(c);
			}
		}
		else if (mat.equals(-1)) {
			lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
		}
		else {
			Color c = Mesh.getColor(mat);
			lm.setDisplayColor(c);
		}
		
		for (int i : lm.getMaterials()) {
			System.out.print("color :" + i);
		}
		
		app.addLandmark(lm);
		
		lastLocX = mouseX;
		lastLocY = mouseY;
	}

	/** Receives a mouse event, and figures out whether it's a click, drag, release, etc., and then calls the appropriate response method */
	public void mouseClick(GL gl, int mouseX, int mouseY) {
		if (!isDraggingVertex && !nextIsReleased) {								// Case: Beginning a drag
			if ( /* !enable.isSelected() || */ stack.size() == 0) return;
			isDraggingVertex = true;
			//Log.p("Beginning drag.");

			//selectPoint(get3DPoint(gl,mouseX,mouseY));
			
			Vertex location = get3DPoint(gl,mouseX,mouseY);
			Landmark lm = new Landmark(location);
			lm.addMaterial(app.getActiveMaterials().get(0));
			lm.addMaterial(app.getActiveMaterials().get(1));
			
			app.addLandmark(lm);
			
			lastLocX = mouseX;
			lastLocY = mouseY;

			return;
		} else if (isDraggingVertex) {						// Case: Continuing a drag
			//Log.p("...");

			double distance = Math.sqrt((mouseX-lastLocX)*(mouseX-lastLocX) + (mouseY-lastLocY)*(mouseY-lastLocY));
			if (distance > 5){
				Vertex location = get3DPoint(gl,mouseX,mouseY);
				Landmark lm = new Landmark(location);
				lm.addMaterial(app.getActiveMaterials().get(0));
				lm.addMaterial(app.getActiveMaterials().get(1));
				
				app.addLandmark(lm);
				
				lastLocX = mouseX;
				lastLocY = mouseY;
			}
			//if (selectedVertex != -1) { stack.get(p,true).set(selectedVertex,get3DPoint(gl,mouseX,mouseY)); }

			return;
		} else if (isDraggingVertex) {						// Case: Ending a drag
			isDraggingVertex = false;
			nextIsReleased = false;
			//Log.p("Ended drag.");

			//if (selectedVertex != -1) { stack.get(p,true).set(selectedVertex,get3DPoint(gl,mouseX,mouseY)); }
			//selectedVertex = -1;

			return;
		} else if (nextIsReleased && !isDraggingVertex) {						// Case: General click
			nextIsReleased = false;
			//Log.p("Click.");
			
			Vertex location = get3DPoint(gl,mouseX,mouseY);
			Landmark lm = new Landmark(location);
			lm.addMaterial(app.getActiveMaterials().get(0));
			lm.addMaterial(app.getActiveMaterials().get(1));
			
			int index = (int) (app.getActiveMaterials().size()*java.lang.Math.random());
			Integer mat = app.getActiveMaterials().get(index);
			if (mat.equals(-2)) {
				if (app.getActiveMaterials().get((index+1)%2).equals(-2)){
					lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
				}
				else if (app.getActiveMaterials().get((index+1)%2).equals(-1)) {
					lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
				}
				else {
					Color c = Mesh.getColor(app.getActiveMaterials().get((index+1)%2));
					lm.setDisplayColor(c);
				}
			}
			else if (mat.equals(-1)) {
				lm.setDisplayColor(new Color(1.0f, 1.0f, 0.0f));
			}
			else {
				Color c = Mesh.getColor(mat);
				lm.setDisplayColor(c);
			}
			
			app.addLandmark(lm);
			
			lastLocX = mouseX;
			lastLocY = mouseY;

			if(stack.size() != 1 || stack.getFirst().getClosed() ) {
				//Log.p("Nothing to do with this mouse click.");
				return;
			}
			stack.getFirst().add(get3DPoint(gl, mouseX, mouseY)); 
			updateNav();
			return;
		}

		Log.p("Click-type not recognized by stacking tool.");

	}

	/** Given a mouse's coordinates (in three-space), sets the nearest contour vertex as "selected."
	 * If no vertices are within distance "alpha," no vertex is selected.
	 * @param mouse
	 */
	private void selectPoint(Vertex mouse) {
		Lasso l = stack.get(p,true);
		int closestIndex = -1;
		float closestDistance = alpha;
		for (int i = 0; i < l.size(); ++i) {
			float distToI = (new Vector(mouse,l.get(i))).getMagnitude();
			if (distToI < closestDistance) {
				closestIndex = i;
				closestDistance = distToI;
			}
		} 
		selectedVertex = closestIndex;
	}

	/** Given the coordinates of the cursor on the the 2D viewport, unprojects the coordinates into 3D space and returns a Vertex object. */
	public Vertex get3DPoint(GL gl, int mouseX, int mouseY) {
		GLU glu = new GLU();

		/* Computing X & Z coordinates */
		double wcoordNear[] = new double[4];// wx, wy, wz;// returned xyz coords
		double wcoordFar[] = new double[4];
		int viewport[] = new int[4]; 					gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
		double mvmatrix[] = new double[16];		gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, mvmatrix, 0);
		double projmatrix[] = new double[16];	gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, projmatrix, 0);
		int realy = viewport[3] - (int) mouseY;// ... - 1;  /* note viewport[3] is height of window in pixels */
		glu.gluUnProject((double) mouseX, (double) realy, 0.0,mvmatrix, 0,projmatrix, 0, viewport, 0, wcoordNear, 0);
		glu.gluUnProject((double) mouseX, (double) realy, 1.0,mvmatrix, 0,projmatrix, 0, viewport, 0, wcoordFar, 0);

		/* Computing Z coordinate */
		Vertex linePt1 = new Vertex((float)wcoordNear[0],(float)wcoordNear[1],(float)wcoordNear[2]);
		Vertex linePt2 = new Vertex((float)wcoordFar[0],(float)wcoordFar[1],(float)wcoordFar[2]);
		Vertex planeP = getCutter().getP();
		Vector planeN = getCutter().getN();
		Vector u = new Vector(linePt1,linePt2);
		Vector w = new Vector(planeP,linePt1);
		float D = planeN.dotProduct(u);
		float N = -planeN.dotProduct(w);
		float sI = N / D;
		Vertex point = linePt1.plus(u.getScaled(sI));
		return point;
	}

	//******************************************************************************************************************************
	//
	//			RENDERING
	//
	//******************************************************************************************************************************

	/** Renders the lasso stack in the preferred format for a 2D viewport. */
	public void render2D(GL gl) {
		if (stack.size() == 0) return;
		//if (awaitingSelection) {doSelection(gl); awaitingSelection = false;}

		switch (stack.get(this.p,true).size()) {
		case 0:
			break;
		case 1:
			Lasso l1 = stack.get(this.p,true);
			gl.glColor3f(1.0f,0.0f, 0.0f);
			gl.glPointSize(6.0f);
			gl.glBegin(GL.GL_POINTS);
			gl.glVertex3f(l1.get(0).getX(), l1.get(0).getY(),l1.get(0).getZ());
			gl.glEnd();
			break;
		default:
			Lasso l2 = stack.get(this.p,true);
			gl.glColor3f(0.0f,0.0f, 0.0f);
			gl.glLineWidth(3.0f);
			gl.glBegin(GL.GL_LINES);
			for (int i = 0; i < l2.size(); ++i) {
				gl.glVertex3f(l2.get(i).getX(), l2.get(i).getY(),l2.get(i).getZ());
				if (i+1 == l2.size() && !l2.getClosed()) break;
				gl.glVertex3f(l2.get((i+1)%l2.size()).getX(), l2.get((i+1)%l2.size()).getY(),l2.get((i+1)%l2.size()).getZ());		
			}
			gl.glEnd();
			gl.glLineWidth(1.0f);

			if (stack.contains(l2)) {
				gl.glPointSize(6.0f);
				gl.glBegin(GL.GL_POINTS);
				for (int i = 0; i < l2.size(); ++i) {
					if (i == selectedVertex) gl.glColor3f(0.0f,1.0f, 0.0f); else gl.glColor3f(1.0f,0.0f, 0.0f);
					gl.glVertex3f(l2.get(i).getX(), l2.get(i).getY(),l2.get(i).getZ());
				}
				gl.glEnd();
			}

		}
	} 

	/** Renders the lasso stack in the preferred format for a 3D viewport. */
	public void render3D(GL gl) {		//TODO Technically, all the dragging and stuff could completely work in 3D

		if (stack.size() == 0) return;

		switch (stack.get(this.p,true).size()) {
		case 0:
			break;
		case 1:
			Lasso l1 = stack.get(this.p,true);
			gl.glBegin(GL.GL_POINTS);
			gl.glVertex3f(l1.get(0).getX(), l1.get(0).getY(),l1.get(0).getZ());
			gl.glEnd();
			break;
		default:
			for (Lasso l2 : stack) {
				if (p == l2.getP()) continue;
				gl.glLineWidth(2.0f);
				gl.glColor3f(0,0,0);
				gl.glBegin(GL.GL_LINES);
				for (int i = 0; i < l2.size(); ++i) {
					gl.glVertex3f(l2.get(i).getX(), l2.get(i).getY(),l2.get(i).getZ());
					gl.glVertex3f(l2.get((i+1)%l2.size()).getX(), l2.get((i+1)%l2.size()).getY(),l2.get((i+1)%l2.size()).getZ());		
				}
				gl.glEnd();
			}

			Lasso l2 = stack.get(this.p,true);
			gl.glColor3f(0.3f,0.0f, 0.0f);
			gl.glLineWidth(5.0f);
			gl.glBegin(GL.GL_LINES);
			for (int i = 0; i < l2.size(); ++i) {
				gl.glVertex3f(l2.get(i).getX(), l2.get(i).getY(),l2.get(i).getZ());
				gl.glVertex3f(l2.get((i+1)%l2.size()).getX(), l2.get((i+1)%l2.size()).getY(),l2.get((i+1)%l2.size()).getZ());		
			}
			gl.glEnd();
			gl.glLineWidth(1.0f);

			if (stack.contains(l2)) {
				gl.glColor3f(1.0f,0.0f, 0.0f);
				gl.glPointSize(6.0f);
				gl.glBegin(GL.GL_POINTS);
				for (int i = 0; i < l2.size(); ++i) {
					gl.glVertex3f(l2.get(i).getX(), l2.get(i).getY(),l2.get(i).getZ());
				}
				gl.glEnd();
				gl.glPointSize(4.0f);
			}

		}
	}

	@Deprecated
	/** Renders a given lasso as a FILLED polygon. Used for hit detection. */
	public void renderFilled(GL gl, Lasso lasso) {
		renderFilled(gl,new GLU(),lasso);
	}

	@Deprecated
	/** Renders a filled lasso as a FILLED polygon. Used for hit detection */
	public void renderFilled (GL gl, GLU glu, Lasso lasso) { 		
		GLUtessellator tess = getCutter().initTessellator(glu, gl);
		gl.glColor3f(0.0f,0.0f,1.0f);
		Vector normal = n.getNormalized();
		gl.glNormal3f(normal.getX(), normal.getY(), normal.getZ());
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);

		glu.gluTessBeginPolygon(tess, null);
		glu.gluTessBeginContour(tess);
		for (int i = 0; i < lasso.size(); ++i ) {
			Vertex v = lasso.get(i);
			double[] data = new double[] { v.getX(), v.getY(), v.getZ() };
			glu.gluTessVertex(tess, data, 0, data);
		}
		glu.gluTessEndContour(tess);
		try { 	glu.gluTessEndPolygon(tess);	} catch (Throwable e) { Log.p("Error closing polygon! " + e); } 
	}


	private boolean testVertices = true, testEdges = true, testFaces= true;

	/** Iterates over all polyhedra, checking to see if any vertices are inside the corresponding interpolated contour. If so, the polyhedron is selected. */
//	public void doSelection(GL gl,Viewer2D view) {
//		if (!awaitingSelection) return;
//
//		try {
//			app.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//
//			ArrayList<Polyhedron> polys = app.getMesh().getTopology().getPolyhedra();
//			for (int p = 0; p < polys.size(); ++p) {
//				if (testVertices) 
//					for (int i : polys.get(p).getVertices()) 
//						if ((enabledRegions.get(polys.get(p).getMaterial()) && stack.contains(app.getMesh().getGeometry().get(i), gl, this, view)))
//							polys.get(p).setSelected(app.isSelecting(), app.getExpression(p));
//				if (testEdges && ((enabledRegions.get(polys.get(p).getMaterial()) && testEdges(polys.get(p)))))
//					polys.get(p).setSelected(app.isSelecting(), app.getExpression(p));
//
//				if (testFaces && ((enabledRegions.get(polys.get(p).getMaterial()) && testFaces(polys.get(p)))))
//					polys.get(p).setSelected(app.isSelecting()		,app.getExpression(p));
//
//			}
//
//			//			if (testVertices) 
//			//				for(Polyhedron p : mesh.getTopology().getPolyhedra()) 
//			//					for (int i : p.getVertices()) 
//			//						p.setSelected((enabledRegions.get(p.getMaterial()) && stack.contains(mesh.getGeometry().get(i), gl, this, view))	?	true	:	p.isSelected(), app.getExpression());
//			//
//			//			if (testEdges) 
//			//				for (Polyhedron p : mesh.getTopology().getPolyhedra())
//			//					p.setSelected(	((enabledRegions.get(p.getMaterial()) && testEdges(p)) || p.isSelected())	, app.getExpression());
//			//
//			//			if (testFaces) 
//			//				for (Polyhedron p : mesh.getTopology().getPolyhedra())
//			//					p.setSelected(	((enabledRegions.get(p.getMaterial()) && testFaces(p)) || p.isSelected())		,app.getExpression());
//
//		} finally { 
//			app.setCursor(Cursor.getDefaultCursor()); 
//		}
//
//		Log.p("Done running selection.");
//		app.getMesh().pushNewSelections();
//		awaitingSelection = false;
//		clear();
//	}

	private boolean testEdges (Polyhedron p) {
		ArrayList<Vertex> v = p.getEdges(app.getMesh().getGeometry());
		for (int i = 0; i < v.size()/2; ++i ) {
			Vertex [] segment = new Vertex[] {v.get(2*i), v.get(2*i+1)};
			for (Vertex [] face : stack.getFaces(segment))									//Check outer surface of prism
				if (intersectSegmentAndFace(segment, face) != null) return true;

			Lasso base = stack.getFirst(), cap = stack.getLast();						//Check top and bottom of prism
			Vertex vBase = intersectSegmentAndPlane(segment, base.get());
			Vertex vCap = intersectSegmentAndPlane(segment,cap.get());
			if ((vBase != null && stack.contains( vBase,  base,   this)) || (vCap != null && stack.contains( vCap,  cap,     this))) return true;
		}
		return false;
	}

	private boolean testFaces (Polyhedron p) {
		int[][] faces = new int[p.getNumFaces()][3];
		p.getFaceIndexes(faces);

		for (int i = 0; i < faces.length; ++i) {
			Vertex[] face = new Vertex[] {
					app.getMesh().getGeometry().get(faces[i][0]),
					app.getMesh().getGeometry().get(faces[i][1]),
					app.getMesh().getGeometry().get(faces[i][2])
			};
			for (Vertex[] segment : stack.getEdges(face))
				if (intersectSegmentAndFace(segment, face) != null) return true;
		}

		return false;
	}

	public static void main (String[] args) {
		Log.p("Simple test run.");
		Vertex[] segment = new Vertex[] {new Vertex(1.8f,-1f,1.1f), new Vertex(1.8f,1f,1.1f)};
		Vertex[] plane = new Vertex[] { new Vertex(2f,0f,0f),new Vertex(2f,0f,1f),new Vertex(0f,0f,1f)};
		Log.p("Intersection: " + intersectSegmentAndFace(segment,plane));
	}

	public static Vertex intersectSegmentAndFace(Vertex [] segment, Vertex [] face) {
		//Get planar intersection
		Vertex intersection = intersectSegmentAndPlane(segment, face);
		if (intersection == null) return null;

		//Do right turn tests to see if intersection is within face
		Vector refVec = new Vector(face[0],face[1]).crossProduct(new Vector(face[1],face[2]));
		int sign = 0;
		for (int i = 0; i < face.length; ++i) {
			Vector testVec = new Vector(face[i],intersection).crossProduct(new Vector(intersection,face[(i+1)%face.length]));
			switch (sign) {
			case 1:
				if (testVec.dotProduct(refVec) > 0f) return null; else break;
			case -1:
				if (testVec.dotProduct(refVec) < 0f) return null; else break;
			default:
				sign =  (testVec.dotProduct(refVec) > 0) ? -1 : 1;
			}
		}

		//If so, return intersection
		return intersection;
	}

	public static Vertex intersectSegmentAndPlane(Vertex[] segment, Vertex[] plane) {
		Vector pNorm = new Vector(plane[0],plane[1]).crossProduct(new Vector(plane[1],plane[2]));
		float pD = pNorm.dotProduct(new Vector(plane[0].getX(), plane[0].getY(),plane[0].getZ()));
		Vector segVec = new Vector(segment[0], segment[1]);
		float planeSegProj = pNorm.dotProduct(segVec);

		if (planeSegProj == 0f) return null;
		float dist = (pD -  pNorm.dotProduct(new Vector(segment[0].getXYZ()))) / planeSegProj;
		if (dist < 0f ||  dist > 1f) return null;

		return new Vertex (segment[0].getX() + segVec.getX()*dist,segment[0].getY() + segVec.getY()*dist,	segment[0].getZ() + segVec.getZ()*dist);
	}

	public void mouseDrag() {
		isDraggingVertex = true;
	}

	public RegistrationTool getRegistrationTool() {
		return app;
	}


}

//******************************************************************************************************************************
//
//			LASSO STORAGE & INTERPOLATION
//
//******************************************************************************************************************************

@SuppressWarnings("serial")
/** Represents a LinkedList of Lasso objects. Provides basic accessors and mutators, as well as generating intermediate contours
 * (by interpolating the vertices of the nearest existing contours)  given a point along the cutting axis.
 */
class LassoList extends LinkedList<Lasso> {

	//** Generates a new LassoList **/
	/*public LassoList () {
			Vertex v0 = new Vertex(3,3,3);
			Vertex v1 = new Vertex(0,0,0);
			Vector n = new Vector(1,0,0);
			float t = 1*(new Vector(v1,v0)).dotProduct(n) / (n.getMagnitude() * n.getMagnitude());
			Log.p("T: " + t + "    vOut: " + v1.plus(n.getScaled(t)));
		} */

	/** Given a point along the cutting axis, generates the float-valued index of the corresponding contour. 
	 * Indexing starts at 1, not 0, whereas indexing for getIndex starts at 0.*/
	public float getIndexNice(Vertex p) { 
		//If lasso already exists
		for (int i = 0; i < this.size(); ++i)
			if (this.get(i).getP().equals(p)) return i+1;

		float targetIndex = -1;
		for (int i = 0; i < this.size(); ++i) {
			if (this.get(i).compare(this.get(i).getP(), p) < 0.0f) {
				targetIndex = (float)i;
				break;
			}
		}

		if (targetIndex == -1) targetIndex = this.size();

		if(targetIndex != 0 && targetIndex != this.size()) {
			Vertex source = get((int)targetIndex-1).getP();
			Vertex dest = get((int)targetIndex).getP();
			float distFrom = (new Vector(source,p)).getMagnitude();
			float distTo = (new Vector(p,dest)).getMagnitude();
			targetIndex += distFrom / (distFrom + distTo);
			return targetIndex;
		}

		return targetIndex+0.5f;
	}

	/** Given a point along the cutting axis, generates the float-valued index of the corresponding contour.
	 * Indexing starts at 0, whereas indexing for getIndexNice starts at 1. 
	 * @param p
	 * @return floatValuedIndex
	 */
	public float getIndex(Vertex p) {return getIndexNice(p) - 1.0f;}

	/** Given an arbitary point in 3-space, generates the nearest point along the cutting plane's axis.
	 * @param v0
	 * @return nearestVertex
	 */
	public Vertex getProjectedPoint(Vertex v0) {
		if (this.isEmpty()) { Log.p("EMPTY STACK!"); return v0;}

		Lasso l = this.getFirst();
		Vertex v1 = l.getP();
		float t = /*-*/1*(new Vector(v1,v0)).dotProduct(l.getN())/(l.getN().getMagnitude()*l.getN().getMagnitude());
		return v1.plus(l.getN().getScaled(t));
	}

	/** Interpolates a lasso (contour) around the cutting-plane's axis, intersecting a given point on that axis. 
	 * 	Use getProjectedPoint(Vertex) to project an arbitrary point onto the axis. If allowEnds == false, returns
	 * null if the provided point is beyond the current boundaries of the surface.
	 * 
	 * @param p
	 * @param allowEnds
	 */
	public Lasso get(Vertex p, boolean allowEnds) {
		float index = getIndex(p);
		if (index%1.0f == 0.0f) {																	// Case 1: Existing lasso
			return this.get((int)index);

		} else if (index > this.size()-1) {														// Case 2: Off back end
			if (!allowEnds) return null;
			Lasso result = new Lasso(p,this.getLast().getN());
			Vector offsetVector = new Vector(this.getLast().getP(),p);
			for (Vertex oldV : this.getLast()) 
				result.add(oldV.plus(offsetVector));
			result.setClosed(true);
			return result;

		} else if (index < 0) {																		// Case 3: In front
			if (!allowEnds) return null;
			Lasso result = new Lasso(p,this.getFirst().getN());
			Vector offsetVector = new Vector(this.getFirst().getP(),p);
			for (Vertex oldV : this.getFirst()) 
				result.add(oldV.plus(offsetVector));
			result.setClosed(true);
			return result;

		} else {																							// Case 4: Between two existing lassos
			Lasso result = new Lasso(p,this.get((int)index).getN());
			float srcK = 1.0f - index%1.0f;
			float destK = 1.0f - srcK;
			for (int i = 0; i < this.get((int)index).size(); ++i) {
				result.add(get((int)index).get(i).getScaled(srcK).plus(get((int)index+1).get(i).getScaled(destK)));
			}
			result.setClosed(true);
			return result;
		}
	}

	/**Returns faces on this surface that may (or may not) intersect the given edge.
	 * TODO Currently, all edges are returned. Clever elimination of contours could win some efficiency.
	 * @param edge
	 * @return possiblyIntersectingFaces
	 */
	public ArrayList< Vertex[] > getFaces(Vertex[] edge) {
		ArrayList< Vertex [] > faces = new ArrayList< Vertex[] >();
		ListIterator<Lasso> iter = listIterator();
		Lasso last = iter.next();
		while (iter.hasNext()) {
			Lasso current = iter.next();
			faces.addAll(getFaces(last, current));			//This would be the place to see if faces are likely to be useful (?)
		}
		return faces;
	}

	/** Returns edges on this surface that may (or may not) intersect the given face.
	 * TODO Clever elimination could speed this up. Also, only contour edges are considered. (Irrelevant?)
	 * @param face
	 * @return possiblyIntersectingEdges
	 */
	public ArrayList< Vertex[] > getEdges(Vertex[] face) {
		ArrayList<Vertex[] > edges = new ArrayList< Vertex[] > ();
		for (Lasso l : this) 
			for (int i = 0; i < l.size(); ++i) edges.add(new Vertex[] {l.get(i), l.get((i+1)%l.size())});
		return edges;
	}

	/** Returns the triangular faces connecting a pair of given lassos.
	 * @param a
	 * @param b
	 * @return faces
	 */
	private ArrayList< Vertex[] > getFaces(Lasso a, Lasso b) {
		ArrayList < Vertex[] > faces = new ArrayList< Vertex[] >();
		for (int i = 0; i < a.size(); ++i) {
			faces.add(new Vertex[] {a.get(i),b.get(i),b.get((i+1)%b.size())});
			faces.add(new Vertex[] {a.get((i+1)%a.size()),a.get(i),b.get(i)});
		}
		return faces;
	}

	/** Check to see whether the given point is incident on the given lasso object. 
	 * @param vertex
	 * @param lasso
	 * @param tool
	 * @return isOnLasso
	 */
	public boolean contains (Vertex v, Lasso lasso, LassoStackingTool tool) {
		Vector perp = tool.getRegistrationTool().getCutter().getN().getNormalized();
		Vector ray = new Vector(perp.getZ(), perp.getX(), perp.getY());	

		if (lasso == null) return false;

		int intersectionCount = 0;
		for (int i = 0; i < lasso.size(); ++i ) {
			intersectionCount += (Vector.intersectSegmentAndRay(lasso.get(i),lasso.get((i+1)%lasso.size()),v, ray, new float[1]))  ? 1 : 0 ;
		}
		return (intersectionCount%2 == 1);
	}

	/** Checks to see whether the given point is inside of the region defined by the given lasso stack.
	 * 
	 * TODO This method computes perpendiculars (used for ray-segment intersection) lazily, meaning some
	 * re-writing will be required for generality if axes other than the x/y/z axes are used.
	 * 
	 * TODO Also, there's no reason this method should need to wait around for an openGL call.
	 * 
	 * @param v
	 * @param gl
	 * @param tool
	 * @param view
	 * @return
	 */
	public boolean contains (Vertex v, GL gl, LassoStackingTool tool, Viewer2D view) {
		return contains( v, get(getProjectedPoint(v), false),tool);
	}

	/** Adds a lasso to the stack. */
	@Override
	public boolean add(Lasso lasso) {
		if (this.size() != 0) {
			int targetIndex = -1;
			for (int i = 0; i < this.size(); ++i) {
				if (this.get(i).compare(this.get(i).getP(), lasso.getP()) < 0 ) {
					targetIndex = i;
					break;
				}
			}

			if (targetIndex != -1) {
				super.add(targetIndex, lasso);
				Log.p("Added lasso at index: " + (targetIndex+1) + " of " + this.size());
				return true;
			}
		}
		Log.p("Added lasso to end.");
		super.add(lasso);
		return true;
	}

}

//******************************************************************************************************************************
//
//			LASSO REPRESENTATION
//
//******************************************************************************************************************************

class Lasso implements Iterable<Vertex>, Comparable<Lasso> {
	private ArrayList<Vertex> vertices = new ArrayList<Vertex>();
	private boolean isClosed = false;
	private Vertex p;
	private Vector n;
	private float sliderValue = -1.0f;
	//private float  ... do i want to have depth along axis? or just use one of the points?

	public Lasso (Vertex p, Vector n) { this.p = p; this.n = n;  }
	public Lasso (Vertex p, Vector n, float f) { this.p = p; this.n = n; this.sliderValue = f; }

	public void add(Vertex v) { vertices.add(v); }	
	public int size() {return vertices.size();}
	public Vertex get(int i) { return vertices.get(i); };
	public Vertex[] get() { return this.vertices.toArray(new Vertex[this.size()]); }
	public Vertex set(int i, Vertex v) { return vertices.set(i, v); }
	public boolean getClosed() { return isClosed; }
	public void setClosed(boolean closed) { isClosed = closed; }
	public Vertex getP() { return p; }
	public Vector getN() { return n; }
	public float getSliderValue() { if (sliderValue == -1.0f) Log.p("RETURNING INVALID SLIDER LOC FOR LASSO"); return sliderValue; }
	public Lasso setSliderValue(float f) { this.sliderValue = f; return this; }

	public Iterator<Vertex> iterator() {
		return vertices.iterator();
	}

	public int compare(Vertex p1, Vertex p2) {
		Vector dirVect = new Vector(p1, p2);
		float dot = dirVect.dotProduct(this.n);
		if (dot > 0.0f) return 1;
		else if (dot == 0.0f) return 0;
		else return -1;
	}

	public int compareTo(Lasso other) {
		if (this == other || this.vertices.equals(other.vertices)) return 0;
		return compare(this.p, other.p);
	}

}
