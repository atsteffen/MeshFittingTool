package app.tools.cross_sections;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import app.RegistrationTool;
import app.data.Bin;
import app.gui.Viewer2D;
import app.tools.Log;
import app.tools.MyJSlider;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.topology.Polyhedron;

/**CuttingTool.java
 * 
 * Represents a bounding box fitted to a Mesh, along with the cutting plane that can slide along the major axis of that box.
 * Contains methods used to clip the model against the cutting plane.
 * 
 * @author Don McCurdy
 *
 */
public class CuttingTool implements ChangeListener {
	//******************************************************************************************************************************
	//
	//			FIELDS AND VARIABLES
	//
	//******************************************************************************************************************************
	public RegistrationTool app;
	private CrossSectionMaker crossSM;
	/** Axis along which the cutting-plane slides */ private Vector axis;
	/** Offset of the plane along its axis */				private float offset = 0.0f;
	/** Rotational parameters	*/								private float yRot, xRot;

	/** Polyhedra intersecting the cutting plane */ 					private ArrayList<Integer> clippedPolyhedra = new ArrayList<Integer>();
	/** Maps indexes in the cutting plane to global indexes */ 	private HashMap<Integer, Integer> listMap = new HashMap<Integer, Integer>();

	boolean enabled = false, clipOnPaint = false, doRefresh = true, doIntersectRefresh = true, doPlaneHandle = false, lassoMode = false, regionMode = false;

	float xMax, xMin, yMax, yMin, zMax, zMin;
	float radius;
	//******************************************************************************************************************************
	//
	//			GUI ELEMENTS
	//
	//******************************************************************************************************************************
	private Color fillColor = Color.BLACK;
	/** Scaling factor. Must match the value in Viewer3D.java to look right. */
	float scalar = RegistrationTool.SCALE;

	JPanel controlPanel;
	JLabel label;
	JButton minUp, minDown;
	JToggleButton enable, cut, deselect, planeHandle;
	private MyJSlider slider;
	//MyDKnob yAxis, xAxis;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTION
	//
	//******************************************************************************************************************************

	/** Sets up a new CuttingTool, tailoring the dimensions of the cutting plane
	 * to match the model's size/orientation. 
	 * @param app
	 * @param m
	 */
	public CuttingTool (RegistrationTool app) {
		// find bounds, size appropriately, and orient in some default way.
		this.app = app;
		crossSM = new CrossSectionMaker(app);

		init();
		initControlPanel();
		setEnabled(false);

	}
	
	/** Should be called after loading a new mesh. 
	 * Finds bounds, sizes panning area appropriately, and goes to a default orientation.
	 */
	public void init() {
		xMax 	= yMax 	= zMax 	= Integer.MIN_VALUE;
		xMin  	= yMin 	= zMin 	= Integer.MAX_VALUE;
		xRot = yRot = 0.0f;
		radius = 0.0f;

		for (int i = 0; i < app.getMesh().getGeometry().size(); ++i) {
			Vertex v = app.getMesh().getGeometry().get(i);

			if (v.getX() > xMax)
				xMax = v.getX();
			if (v.getX() < xMin)
				xMin = v.getX();

			if (v.getY() > yMax)
				yMax = v.getY();
			if (v.getY() < yMin)
				yMin= v.getY();

			if (v.getZ() > zMax)
				zMax = v.getZ();
			if (v.getZ() < zMin)
				zMin = v.getZ();

			if (Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ()) > radius )
				radius = (float) Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
		}
//
//						Log.p("Zmax = " + zMax);
//						Log.p("Zmin = " + zMin);
//						Log.p("Xmax = " + xMax);
//						Log.p("Xmin = " + xMin);
//						Log.p("Ymax = " + yMax);
//						Log.p("Ymin = " + yMin);
//		
//						Log.p("Zspan = " + (zMax - zMin));
//						Log.p("Yspan = " + (yMax - yMin));
//						Log.p("Xspan = " + (xMax - xMin));

		//		float yOff = ((yMax - yMin) / 2f) - yMax;				//Trying an offset of data...
		//		Log.p("yOff: " + yOff);
		//		for (Vertex v : m.getGeometry().getPoints())
		//			v.setY(v.getY() + yOff);


		axis = new Vector (0.0f, 0.0f, 1.0f).getScaled(radius*2*scalar);
	}

	/** Initializes the GUI components controlling the cutting-plane */
	public void initControlPanel() {
		//minUp = new JButton("+");					minUp.setSize(40,20);		minUp.addChangeListener(this);
		//minDown = new JButton ("-");		minDown.setSize(40,20);	minDown.addChangeListener(this);
		label = new JLabel("Cutting Plane: ");
		enable = new JToggleButton("Selection Mode");			enable.addChangeListener(this);			
		cut = new JToggleButton("Cutaway View");						cut.addChangeListener(this);
		deselect = new JToggleButton("Drag -> Deselection"); deselect.addChangeListener(this);
		planeHandle = new JToggleButton("Drag Plane"); planeHandle.addChangeListener(this);

//		JPanel sliderPanel = new JPanel();	sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS)); sliderPanel.setOpaque(false);
//		JPanel gaugePanel = new JPanel();	gaugePanel.setLayout(new BoxLayout(gaugePanel, BoxLayout.X_AXIS)); gaugePanel.setOpaque(false);
		slider = new MyJSlider(0,1000,0);										slider.addChangeListener(this);				slider.setName("Pan Cutting-Plane");	//slider.setDragType(DKnob.SIMPLE);
//		yAxis = new MyDKnob();											yAxis.addChangeListener(this);						yAxis.setDragType(DKnob.SIMPLE);		//yAxis.setName("Y-Axis"); yAxis.add(new JLabel("Y-Axis")); yAxis.
//		xAxis = new MyDKnob();												xAxis.addChangeListener(this);						xAxis.setDragType(DKnob.SIMPLE);

//		gaugePanel.add(Box.createGlue());
//		gaugePanel.add(yAxis);
//		gaugePanel.add(Box.createGlue());
//		gaugePanel.add(xAxis);
//		sliderPanel.add(new JLabel("Rotate cutting-plane around X-Axis/Y-Axis:"));
//		sliderPanel.add(gaugePanel);
//		sliderPanel.add(Box.createGlue());
//		sliderPanel.add(new JLabel("Pan through cutting-plane:"));
//		sliderPanel.add(slider);

		controlPanel = new JPanel();
		controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));
		JPanel temp = new JPanel();
		temp.setLayout(new BoxLayout(temp, BoxLayout.Y_AXIS));

		temp.add(Box.createVerticalStrut(5));
		temp.add(label);
		temp.add(enable);
		temp.add(Box.createGlue());
		temp.add(cut);
		temp.add(planeHandle);
		temp.add(deselect);
		temp.add(Box.createGlue());
		//		temp.add(new JLabel("Sensitivity:"));
		//		temp.add(minUp);
		//		temp.add(minDown);

		temp.setOpaque(false);

		///////////////////////////////Tooltips
		//minUp.setToolTipText("Lower the sensitivity of the panning-wheel (For finer adjustments).");
		//minDown.setToolTipText("Increase the sensitivity of the panning-wheel (For coarser adjustments)." );
		enable.setToolTipText("Enable slicing mode.");
		cut.setToolTipText("Hides the cutting-plane visualization in the left panel and, instead, shows the sliced 3D model.");
		slider.setToolTipText("Adjust the position of the cutting-plane. (Click, then drag mouse left/right)");
//		xAxis.setToolTipText("Click and drag mouse left/right to rotate the cutting-plane around the X axis.");
//		yAxis.setToolTipText("Click and drag mouse left/right to rotate the cutting-plane around the Y axis.");
		///////////////////////////////

		controlPanel.add(Box.createGlue());
		controlPanel.add(temp);
		controlPanel.add(Box.createGlue());
//		controlPanel.add(sliderPanel);
		controlPanel.setOpaque(true);

		slider.setValue(0.5f);//		xAxis.setValue(0.5f); 		yAxis.setValue(0.5f);
	}
	
	//******************************************************************************************************************************
	//
	//			RUN-TIME OPERATION
	//
	//******************************************************************************************************************************
	public final static int CUTAWAY = 0, PLANE = 1, NONE = 3;
	private int mode = NONE;

	/** Translates from the origin to a point in the plane.
	 * @param gl
	 */
	public void translateView(GL gl) {
		gl.glTranslatef(getP().getX(), getP().getY(), getP().getZ());
	}

	public void untranslateView(GL gl) {
		gl.glTranslatef(-1f*getP().getX(), -1f*getP().getY(), -1f*getP().getZ());
	}

	/** Rotates to the correct orientation of the cutting plane
	 * @param gl
	 */
	public void rotateView(GL gl) {
		gl.glRotatef(xRot, 1.0f, 0.0f, 0.0f);
		gl.glRotatef(yRot, 0.0f, 1.0f, 0.0f);
	}

	public void unrotateView(GL gl) {
		gl.glRotatef(-1f*yRot, 0f, 1f, 0f);
		gl.glRotatef(-1f*xRot, 1f, 0f, 0f);
	}

	/** Sets up a new GLClipPlane, allowing OpenGL to
	 * clip the model against the cutting plane.
	 * @param gl
	 */
	public void applyGLClipPlane(GL gl){
		//if (enabled) {
		gl.glPushMatrix();
		gl.glScalef(scalar, scalar, scalar);

		Vector normal = axis.getNormalized().getInverse();		//A little arbitrary, but yeaah.
		Vertex v = new Vertex(0.0f, 0.0f, 0.0f);//source.plus(axis.getScaled((offset-1.0f)/2.0f));

		double a = normal.getX();
		double b = normal.getY();
		double c = normal.getZ();
		double d = (-1)*(v.getX()*normal.getX() 		+ 		v.getY()*normal.getY()		+				v.getZ()*normal.getZ());

		double [] equation = new double[] {a,	b, c, d};

		gl.glTranslatef(getP().getX(),getP().getY(),getP().getZ());
		gl.glRotatef(yRot, 0.0f, -1.0f, 0.0f);
		gl.glRotatef(xRot, -1.0f, 0.0f, 0.0f);

		gl.glClipPlane(GL.GL_CLIP_PLANE0, DoubleBuffer.wrap(equation));
		gl.glEnable(GL.GL_CLIP_PLANE0);

		gl.glPopMatrix();
		//}
	}

	/** Paints the clipping plane. A single quad is rendered - no 
	 * clipping takes place.
	 * @param gl
	 */
	public void paintGLClipPlane(GL gl) {
		switch (mode) {
		case NONE: 
			gl.glDisable(GL.GL_CLIP_PLANE0);
			return;
		case CUTAWAY:
			applyGLClipPlane(gl);
			return;
		case PLANE:
			gl.glDisable(GL.GL_CLIP_PLANE0);
			clipBounds(gl,true);

			gl.glPushMatrix();
			gl.glScalef(scalar, scalar, scalar);
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
			gl.glEnable(GL.GL_POLYGON_STIPPLE);
			gl.glPolygonStipple(ByteBuffer.wrap(Bin.HALF_TONE));

			gl.glTranslatef(getP().getX(), getP().getY(), getP().getZ());
			gl.glRotatef(yRot, 0.0f, -1.0f, 0.0f);
			gl.glRotatef(xRot, -1.0f, 0.0f, 0.0f);

			gl.glBegin(GL.GL_QUADS);

			gl.glNormal3f(getN().getX(), getN().getY(), getN().getZ());
			gl.glColor3f(fillColor.getRed()/255.0f, fillColor.getGreen()/255.0f, fillColor.getBlue()/255.0f);

			gl.glVertex4f(0,1,0,0.001f);
			gl.glVertex4f(-1, 0, 0, 0.001f);
			gl.glVertex4f(0,-1.0f,0,0.001f);
			gl.glVertex4f(1, 0, 0, 0.001f);

			gl.glVertex4f(1, 0, 0, 0.001f);
			gl.glVertex4f(0,-1.0f,0,0.001f);
			gl.glVertex4f(-1, 0, 0, 0.001f);
			gl.glVertex4f(0,1,0,0.001f);

			gl.glEnd();
			gl.glDisable(GL.GL_POLYGON_STIPPLE);

			gl.glPopMatrix();

			clipBounds(gl, false);
			return;
		default:
			Log.p("UNKNOWN CUTTING-PLANE VISUALIZATION STATE.");
		}
	}

	/** Renders the bounding box used to clip the cutting plane. If this method is
	 * not called, the cutting-quad will appear to extend infinitely along its plane.
	 * @param gl
	 * @param enable
	 */
	private void clipBounds(GL gl, boolean enable) {
		if (enable) {

			gl.glPushMatrix();
			gl.glScalef(scalar, scalar, scalar);

			Vector[] normals = new Vector[] {
					new Vector(-1,0,0), 
					new Vector(1,0,0), 
					new Vector(0,-1,0), 
					new Vector(0,1,0), 
					new Vector(0,0,-1), 
					new Vector(0,0,1) 
			};
			Vertex[] points = new Vertex[] { 
					new Vertex(radius+0.001f, 0,0), 
					new Vertex(-radius-0.001f,0,0), 
					new Vertex(0,radius+0.001f,0), 
					new Vertex(0,-radius-0.001f,0), 
					new Vertex(0,0,radius+0.001f), 
					new Vertex(0,0,-radius-0.001f) 
			};

			for (int i = 0; i < normals.length; ++i) {
				double d = (-1)*(points[i].getX()*normals[i].getX() 		+ 		points[i].getY()*normals[i].getY()		+				points[i].getZ()*normals[i].getZ());
				double[] equation = new double[] {normals[i].getX(), normals[i].getY(), normals[i].getZ(), d};
				gl.glClipPlane(GL.GL_CLIP_PLANE0+i, DoubleBuffer.wrap(equation));
				gl.glEnable(GL.GL_CLIP_PLANE0+i);
			}


			gl.glPopMatrix();

		} else {
			gl.glDisable(GL.GL_CLIP_PLANE0);
			gl.glDisable(GL.GL_CLIP_PLANE1);
			gl.glDisable(GL.GL_CLIP_PLANE2);
			gl.glDisable(GL.GL_CLIP_PLANE3);
			gl.glDisable(GL.GL_CLIP_PLANE4);
			gl.glDisable(GL.GL_CLIP_PLANE5);
			if (enabled) return; 

		}
	}

	/** Paints the cross-sections of all polyhedra intersecting the cutting plane.
	 * If a refresh needs to be done, this list of cross-sections is regenerated.
	 * @param gl
	 * @param doOutline
	 * @param listNum
	 * @return
	 */
	public int paintCrossSection(GL gl, boolean doOutline, int listNum) {
		if (doRefresh /*|| currentSubDivLevel != app.getMesh().getSubDivLevel() */ ) {
			listNum = fillList(gl, doOutline, listNum);
		}

		for (int i = 0; i < listMap.size(); ++i)
			gl.glCallList(listNum + i);

		return listNum;
	}

	/** Variable needed to efficiently handle call lists */ private int oldSize = 0;
	/** Fills the display-lists with the cross-sections of intersecting polyhedra. 
	 * 
	 * @param gl
	 * @param doOutline
	 * @param listNum
	 * @return
	 */
	public int fillList(GL gl, boolean doOutline, int listNum) {
		//if (readDensity) {DensityFile.readDensities(app,gl);readDensity=false;}
		if (!doOutline) {
			oldSize = listMap.size();
			listMap.clear();
		} 
		if (oldSize != 0 && 			//Avoids having call-lists overwritten when toggling fullscreen.
				! ( doOutline && 
						(listNum >= app.getViewer2D().colorCallList 
								&& listNum <= app.getViewer2D().colorCallList+oldSize))) {
			gl.glDeleteLists(listNum, oldSize);
			//Log.p("Clearing " + oldSize + " from " + ( doOutline ? " OUTLINE " : " FILL " ) + " list " + listNum);
		}


		GLU glu = new GLU();																		//Initialize GLU and tessellators
		GLUtessellator tess = initTessellator(glu, gl);

		ArrayList<Integer> intersections = getIntersections();
		crossSM = new CrossSectionMaker(app);

		listNum = gl.glGenLists(intersections.size());
		//Log.p("Generated " +intersections.size() +"  for " + (doOutline ? "OUTLINE" : "FILL") + " at " + listNum );

		for (int index = 0; index < intersections.size(); ++index) {
//			Polyhedron p = app.getMesh().getTopology().getPolyhedra().get(intersections.get(index));	

			gl.glNewList(listNum+index, GL.GL_COMPILE);								//Name the call list

			if (!doOutline) {
				listMap.put(intersections.get(index), index);
				gl.glLoadName(intersections.get(index));									//Load name
			}
			
//			int total;
//			try{
//				//int[] hmln = p.getExprData().getHMLN();
//				switch(showColor) {
//				case 0: total = hmln[0]+hmln[1]+hmln[2]+hmln[3]; break;
//				case 1: total = hmln[0]+hmln[1]+hmln[2]; break;
//				case 2: total = hmln[0]+hmln[1]; break;
//				case 3: total = hmln[0]; break;
//				default: total = 0; break;
//				}
//			} catch (NullPointerException e) {total = 1;}

			Color c;
			if (doOutline) c = glSetColor(gl, Color.BLACK);									//Load color/alpha values
//			else if (p.isSelected() && total > 0) c = glSetColor(gl, (app.getViewer2D().getShowExpression()) ? p.getColor() : Mesh.getColor(p.getMaterial()));
			else c = glSetColor(gl, Color.WHITE);

			//Paint the cross section
			CrossSection crossSection = crossSM.getCrossSection(app.getMesh().getTopology().getPolyhedra().get(intersections.get(index)));
			paintOneCrossSection(gl, glu, tess, crossSection, c, doOutline);

			gl.glDisable(GL.GL_BLEND);
			gl.glEndList();

		}
		return listNum;
	}

	/** Creates a new tessellator, allowing non-triangular 
	 * cross sections to be tessellated before rendering.
	 * @param glu
	 * @param gl
	 * @return
	 */
	public GLUtessellator initTessellator(GLU glu, GL gl) {
		GLUtessellator tess = glu.gluNewTess();
		TessellatorCallBack tCB = new TessellatorCallBack(gl, new GLU());

		glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, tCB);						//Provide callback methods
		glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, tCB);
		glu.gluTessCallback(tess, GLU.GLU_TESS_END, tCB);
		glu.gluTessCallback(tess, GLU.GLU_TESS_ERROR, tCB);
		glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, tCB);

		return tess;
	}


	/** Given a GL context and color, sets the drawing color and associated alpha values.
	 * Uses alpha values defined by Viewer2D to make lines opaque, whites more transparent, etc.
	 * 
	 * METHOD ENABLES BLENDING - User should call gl.glDisable(GL.GL_BLEND) after doing any drawing.
	 * 
	 * @param gl
	 * @param c
	 */
	private Color glSetColor(GL gl, Color c) {
		if (c.equals(Color.BLACK)) 
			c = new Color(0f,0f,0f,Viewer2D.LINE_ALPHA);
		else if (c.equals(Color.WHITE)) 
			c = new Color (1f,1f,1f,Viewer2D.VOID_ALPHA);
		else 
			c = new Color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, Viewer2D.FILL_ALPHA);

		gl.glEnable(GL.GL_BLEND);															//Enable blending
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f,c.getAlpha()/255f);
		return c;

	}

	/** Regenerates the call list for a single polyhedron.
	 * @param gl
	 * @param meshIndex
	 * @param listNum
	 */
	public void reGen(GL gl, int meshIndex, int listNum) {
		gl.glNewList(listNum+listMap.get(meshIndex), GL.GL_COMPILE);

		gl.glLoadName(meshIndex);

		gl.glDisable(GL.GL_BLEND);
		gl.glEndList();

	}

	/** Paints a single cross section, given a CrossSection object. 
	 * The cross section will be rendered differently based on whether 
	 * a filled polygon or an outline is needed.
	 * @param gl
	 * @param glu
	 * @param tess
	 * @param crossSection
	 * @param doOutline
	 */
	private void paintOneCrossSection(GL gl, GLU glu, GLUtessellator tess, CrossSection crossSection, Color c, boolean doOutline) {	
		if (!doOutline) {																	//Case 1: To draw a filled cross-section, we tessellate.

			glu.gluTessBeginPolygon(tess, null);

			for (short contour = 0; contour <= 1; ++contour) {
				if (crossSection.getContour(contour).length > 2) {
					glu.gluTessBeginContour(tess);
					for (int i = 0; i < crossSection.getContour(contour).length; ++i ) {
						Vertex v = crossSection.getContour(contour)[i];
						double[] data = new double[] { v.getX(),v.getY(),v.getZ(),c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f,c.getAlpha()/255f};
						glu.gluTessVertex(tess, data, 0, data);
					}
					glu.gluTessEndContour(tess);
				}
			}
			try { 	glu.gluTessEndPolygon(tess);		
			} catch (Throwable e) { //Where are self-intersecting polyhedra coming from?
				Log.p("ERROR: A polygon could not be displayed, due to an array-out-of-bounds error from OpenGL.");	
				Log.p("This cross-section was responsible: " + crossSection);
				Log.p("Error was: " + e);
				Log.p("Length of contour 1: " + crossSection.getContour(0).length);
				Log.p("Length of contour 2: " + crossSection.getContour(1).length);
				Log.p("Contour 1:");
				for (int i = 0; i < crossSection.getContour(0).length; ++i) 
					Log.p("---->" + crossSection.getContour(0)[i]);
				//crossSection.tag.setSelected(true); 
				//Re-enable the above line to select problematic polygons when they're detected. (Allowing them to be exported)
			} 

		} else {																					//Case 2: To draw the outline, we just use GL_POLYGON.

			//gl.glBegin(GL.GL_LINE_LOOP);
			for (int i = 0; i < crossSection.getContour(0).length; ++i) {
				Vertex v1 = crossSection.getContour(0)[i];
				Vertex v2 = crossSection.getContour(0)[(i+1)%crossSection.getContour(0).length];
				
				if (crossSection.getCreaseTags(0)[(i+1)%crossSection.getContour(0).length]) 
					gl.glLineWidth(Viewer2D.CREASE_LINE_WIDTH);
				else if (app.getViewer2D().getShowInnerSection()) 
					gl.glLineWidth(Viewer2D.LINE_WIDTH);
				else continue;
				
				//gl.glLineWidth(  (crossSection.getCreaseTags(0)[(i+1)%crossSection.getContour(0).length]) ? Viewer2D.CREASE_LINE_WIDTH : Viewer2D.LINE_WIDTH );
				gl.glBegin(GL.GL_LINES);
				gl.glVertex3f(v1.getX()*scalar, v1.getY()*scalar, v1.getZ()*scalar);
				gl.glVertex3f(v2.getX()*scalar, v2.getY()*scalar, v2.getZ()*scalar);
				gl.glEnd();
			}
			//gl.glEnd();
			//gl.glBegin(GL.GL_LINE_LOOP);
			for (int i = 0; i < crossSection.getContour(1).length; ++i) {
				Vertex v = crossSection.getContour(1)[i];
				Vertex v2 = crossSection.getContour(1)[(i+1)%crossSection.getContour(1).length];
				
				if (crossSection.getCreaseTags(0)[(i+1)%crossSection.getContour(0).length]) 
					gl.glLineWidth(Viewer2D.CREASE_LINE_WIDTH);
				else if (app.getViewer2D().getShowInnerSection())  		
					gl.glLineWidth(Viewer2D.LINE_WIDTH);
				else continue;
				
				//gl.glLineWidth(  (crossSection.getCreaseTags(1)[(i+1)%crossSection.getContour(1).length]) ? Viewer2D.CREASE_LINE_WIDTH : Viewer2D.LINE_WIDTH );
				gl.glBegin(GL.GL_LINES);
				gl.glVertex3f(v.getX()*scalar, v.getY()*scalar,v.getZ()*scalar);
				gl.glVertex3f(v2.getX()*scalar, v2.getY()*scalar, v2.getZ()*scalar);
				gl.glEnd();
				
				gl.glPointSize(Viewer2D.CREASE_LINE_WIDTH/2);
				gl.glEnable( GL.GL_POINT_SMOOTH );
				gl.glBegin(GL.GL_POINTS);
				gl.glVertex3f(v.getX()*scalar, v.getY()*scalar,v.getZ()*scalar);
				gl.glVertex3f(v2.getX()*scalar, v2.getY()*scalar, v2.getZ()*scalar);
				gl.glEnd();
			}
			//gl.glEnd();

		}
	}

	//******************************************************************************************************************************
	//
	//			INTERSECTION HANDLING
	//
	//******************************************************************************************************************************

	/** Finds indexes of all polyhedra in the mesh that intersect the cutting plane. */
	private ArrayList<Integer> getIntersections () {
		if (doIntersectRefresh /* || currentSubDivLevel != app.getMesh().getSubDivLevel() */ ) {
			clippedPolyhedra = new ArrayList<Integer>();
			for (int i = 0; i < app.getMesh().getTopology().getPolyhedra().size(); ++i)
				if (checkPolyhedron(app.getMesh().getTopology().getPolyhedra().get(i)))
					clippedPolyhedra.add(i);
			doIntersectRefresh = false;
		}

		return clippedPolyhedra;
	}

	/** Checks to see if the given polyhedron intersects with the cutting plane.
	 * @param poly
	 * @return
	 */
	private boolean checkPolyhedron(Polyhedron poly) {
		boolean foundBehind = false;
		boolean foundAhead = false;

		for (int i = 0; i < poly.getVertices().length; ++i) {
			Vertex q = app.getMesh().getGeometry().get(poly.getVertices()[i]);
			Vertex p = getP();

			Vector traj = new Vector(p,q);
			float dot = traj.dotProduct(getN());

			if (dot > 0)
				foundAhead = true;
			else if (dot < 0)
				foundBehind = true;
			else {
				foundAhead = true;
				foundBehind = true;
			}

		}
		return foundBehind		&&		foundAhead;
	}

	//******************************************************************************************************************************
	//
	//			ACCESSORS
	//
	//******************************************************************************************************************************

	public int getMode() { return mode; }

	/* Getters for the six bounds used to clip the cutting plane and to size the 2D viewing plane. */
	public float getZMin() { return zMin*scalar; }
	public float getZMax() {	return zMax*scalar; }
	public float getXMin() { return xMin*scalar; }
	public float getXMax() {return xMax*scalar; }
	public float getYMax() { return yMax*scalar; }
	public float getYMin() {return yMin*scalar; }
	public float getRadius() { return radius*scalar; }

	/** Returns a point on the cutting plane.
	 * @return pointOnCuttingPlane
	 */
	public Vertex getP() {
		return new Vertex(0,0,0).plus(getN().getScaled(offset*radius));
	}
	/** Returns the normal vector of the cutting plane. 
	 * @return normalVector
	 */
	public Vector getN() {
		return axis
		.getNormalized()
		.getMultiplied(Vector.getRotationMatrix(Vector.X_AXIS, xRot))
		.getMultiplied(Vector.getRotationMatrix(Vector.Y_AXIS,yRot))
		;
	}

	/** Gets the GUI handle for the cutting-tool's controls 
	 * @return controlPanel 
	 */
	public JPanel getControlPanel() { return controlPanel; }
	/** Returns the JSlider that pans the cutting-plane back and forth. */
	public JSlider getSlider() { return slider;	}
	/** Returns the lasso tool. */
	public boolean getLassoMode() {		return lassoMode;     	}
	public boolean getRegionMode() { 		return regionMode; 	}
	public CrossSectionMaker getCrossSM() { return crossSM; }

	//******************************************************************************************************************************
	//
	//			MUTATORS
	//
	//******************************************************************************************************************************

	public void setMode(int mode) { this.mode = mode; }
	public void setRegionSelecting(boolean regionSelecting) { this.regionMode = regionSelecting; }

	/** Enables the cutting-tool
	 * @param enabled
	 */
	public void setEnabled (boolean enabled) {
		this.enabled = enabled;

		//slider.setEnabled(enabled); //NOTE this change made June 3 2011
		//minUp.setEnabled(enabled);
		//minDown.setEnabled(enabled);
		//deselect.setEnabled(enabled);
		planeHandle.setEnabled(enabled);
		cut.setEnabled(enabled);

	}
	/** Turns lassoing mode on/off. Disables single selection while on.
	 * @param lassoMode
	 */
	public void setLassoMode ( boolean lassoMode) {
		this.lassoMode = lassoMode;
	}
	/** Tells the cutting tool to refresh before the next rendering cycle. */
	public void refresh() { doRefresh = true; }
	/** Tells the cutting tool whether or not to refresh before the next rendering cycle. */
	public void refresh(boolean ref) { doRefresh = ref;}
	/** Tells cutting tool to refresh the cross section list AND the list of polyhedra from which they are generated. */
	public void refreshHard() { doRefresh = true; doIntersectRefresh = true; }
	/** Sets the y-axis rotation of the cutting plane. 
	 * @param angle
	 */
	public void setAngleY(float angle) {
		yRot = angle;
		refresh();
		doIntersectRefresh = true;
	}
	/** Sets the x-axis rotation of the cutting plane.
	 * @param angle
	 */
	public void setAngleX(float angle) {
		xRot = angle;
		refresh();
		doIntersectRefresh = true;
	}
	/** Set the position of the cutting plane along its axis.
	 * Cutting plane may be clipped if the value passed here
	 * is not on the interval [0.0f, 1.0f]
	 * @param pan
	 */
	public void setPan(float pan) {
		offset = (2.0f)*pan - 1.0f;
		slider.setValue(pan);
		if (app.getLassoTool() != null) app.getLassoTool().setSliderVisual(pan);
		refreshHard();
	}

	//******************************************************************************************************************************
	//
	//			LISTENER IMPLEMENTATION
	//
	//******************************************************************************************************************************

	/** Listens to state changes in the CuttingTool 
	 * @param e
	 */
	public void stateChanged(ChangeEvent e) {
		if(e.getSource() == this.enable) {
			setEnabled(enable.isSelected());
//			app.setSelectionMode(enable.isSelected());
			if (enable.isSelected()) { //Why this works, I have no idea...
				clipOnPaint = false;
				cut.setSelected(false);
			}
		} else if (e.getSource() instanceof MyJSlider /*e.getSource() == this.slider*/) {
			setPan(( (MyJSlider) e.getSource()).getValuef());
			//setPan(slider.getValuef());
		} else if (e.getSource() == this.minUp) {
			//slider.incSensitivity();
		} else if (e.getSource() == this.minDown) {
			//slider.decSensitivity();
		} else if (e.getSource() == this.cut) {
			clipOnPaint = cut.isSelected();
		} else if	(e.getSource() == this.deselect){
			//doDeselect = deselect.isSelected();
		} else if (e.getSource() == this.planeHandle) {
			doPlaneHandle = planeHandle.isSelected();
		}

	}
}
