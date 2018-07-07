package app.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.IntBuffer;

import javax.media.opengl.DebugGL;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.glu.GLU;

import com.sun.opengl.util.BufferUtil;

import app.RegistrationTool;
import app.tools.Log;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.Mesh;
import app.tools.topology.Landmark;
import app.tools.topology.Polyhedron;

public class Viewer2D extends GLPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

	//******************************************************************************************************************************
	//
	//			CONSTANTS AND MEMBER VARIABLES
	//
	//******************************************************************************************************************************

	/** Default serial version UID  */
	private static final long serialVersionUID = 1L;

	private boolean isDragging			= false;
	private boolean mouseReleased 	= false;
	private boolean streamImages 		= false;
	private boolean showCrossSection = true;
	private boolean showInnerSection = true;
	private boolean showExpression = false;
	private int 		mouseX = 0, mouseOverX = 0, lastMouseOverX = 0;
	private int 		mouseY = 0, mouseOverY = 0, lastMouseOverY = 0;
	private int			screenWidth = 1, screenHeight = 1;
	public float		zoomFactor = 1f, aspectRatio = 1f; // aspectRatio lets us maintain proportions when view is resized.
	private boolean zoomChanged = false;
	private boolean doRefresh		   = true;

	private RegistrationTool app;
	public ImageStreamer streamer;
	public int colorCallList = -1;
	public int blackCallList = -1;

	/** Tells CuttingTool to draw an outline. */ private static final boolean OUTLINE = true;
	/** Tells CuttingTool to draw a filled face*/ private static final boolean FILL = false;

	public static final float LINE_ALPHA = 1f, FILL_ALPHA = 0.5f, VOID_ALPHA = 0.2f;
	public static final float CREASE_LINE_WIDTH = 4f, LINE_WIDTH = 0.2f;						//Note that anti-aliasing affects line width.

	//******************************************************************************************************************************
	//
	//			CONSTRUCTORS AND SUCH
	//
	//******************************************************************************************************************************

	protected Viewer2D(String progName,GLCapabilities glCap) {
		super(progName, glCap);
	}

	public Viewer2D(RegistrationTool app, GLCapabilities glCap) {
		super("Viewer2D",glCap);
		this.app = app;
		this.setBackground(Color.DARK_GRAY);
	}

	/** Load a new mesh and display it.
	 * @param model
	 */
	public void loadMesh(Mesh sector) {
		if (app.getCutter() != null)
			app.getCutter().refreshHard();
	}

//	/** Load a new cuttingplane and start slicing the model. */
//	public void loadCuttingTool(CuttingTool cutter) {
//		this.cutter = cutter;
//	}

	/** Runs when the application launches, AND when toggling full-screen mode.
	 * @param gLDrawable
	 */
	public void init(GLAutoDrawable gLDrawable) {
		GL gl = gLDrawable.getGL();
		gLDrawable.setGL( new DebugGL(gLDrawable.getGL()));

		//	gl.glEnable(GL.GL_CULL_FACE);	
		//gl.glShadeModel(GL.GL_SMOOTH);
		gl.glClearColor(1f,1f,1f,0.5f);
		//gl.glClearColor(0.95f, 0.95f, 0.95f, 0.5f);  			 			// Background Color
		gl.glClearDepth(1.0f);                     				 					// Depth Buffer Setup
		gl.glEnable(GL.GL_DEPTH_TEST);             				 			// Enables Depth Testing
		gl.glDepthFunc(GL.GL_LEQUAL);            					   		// The Type Of Depth Testing To Do
		gLDrawable.addKeyListener(this);           	 					// Listening for key events
		gl.glDisable(GL.GL_LIGHTING);

		glCanvas.addMouseMotionListener(this);
		glCanvas.addMouseWheelListener(this);
		glCanvas.addMouseListener(this);

		if ( colorCallList == -1 || blackCallList == -1) {
			colorCallList = gl.glGenLists(1);
			blackCallList = gl.glGenLists(1);
		}

		gl.glEnable(GL.GL_TEXTURE_2D);
		gl.glTexEnvf(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE, GL.GL_MODULATE);

		//if (streamer == null) loadImageStreamer(GeneLoader.Z_AXIS, app.getCutter().getN());
	}

	/** Generates a new ImageStreamer, given an axis and axisIndex (see below).
	 * 0 => X
	 * 1 => Y
	 * 2 => Z
	 * @param axisIndex
	 */
	public void setImageStreamerAxis(String axis, Vector vec) {
		if (!axis.equals("null") && streamer != null) {
			streamer.setId(axis);
			streamer.setAxis(vec.getNormalized());
		}
	}
	
	public void setStreamer(ImageStreamer streamer) { this.streamer = streamer; }
	public ImageStreamer getStreamer() { return streamer; }
	
//	public void loadImageStreamer(File file) throws IOException {
//		streamer = Loader.loadImageStreamer(file.getParent(),file.getName());
//		if (!GeneLoader.Z_AXIS.equals("null")) {
//			streamer.setId(GeneLoader.Z_AXIS);
//			streamer.setAxis(app.getCutter().getN().getNormalized());
//		}
//		
//		List<Landmark> landmarks = streamer.extractLandmarks(1.0,1);
//		
//		RegistrationTool.instance.setLandmarks(landmarks);	
//		RegistrationTool.instance.pcaAlignToLandmarks();
//		RegistrationTool.instance.subdivideMask();
//		RegistrationTool.instance.getTabbedOptions().precomputeMatrices();
//		RegistrationTool.instance.solveRegistration();
//		//SelectionTool.instance.icpAlignToLandmarks();
//		//SelectionTool.instance.registerToLandmarks();
//	}

	//******************************************************************************************************************************
	//
	//			MUTATORS & ACCESSORS
	//
	//******************************************************************************************************************************

	public boolean getShowCrossSection() { return showCrossSection; }
	public boolean getShowInnerSection() { return showInnerSection; }
	public boolean getShowExpression() { return showExpression; }

	/** Tells application whether or not to stream/show images as backgrounds*/
	public void setStreaming(boolean stream) { 
		this.streamImages = stream; 
	}
	/** Tells application whether or not to show cross-sections of polyhedra. If disabled,
	 * Viewer2D will either display images only, or nothing at all. 
	 * @param show
	 */
	public void setShowCrossSection(boolean show) { this.showCrossSection = show; }
	public void setShowInnerSection(boolean show) { this.showInnerSection = show; }
	public void setShowExpression(boolean show) { this.showExpression = show; }

	public void setPan(int x, int y) {	this.panX = x; this.panY = y;  }
	public void setZoom(float zoom) {	this.zoomFactor = zoom; zoomChanged = true; }

	//******************************************************************************************************************************
	//
	//			GLPANEL-PRESCRIBED METHODS
	//
	//******************************************************************************************************************************



	/** Called by drawable to initiate drawing
	 * @param gLDrawable The GLDrawable Object
	 */
	public void display(GLAutoDrawable gLDrawable) 
	{
		GL gl = gLDrawable.getGL();
		gl.glLoadIdentity();
		
		if (zoomChanged || doRefresh) {
			int viewport[] = new int[4];
			gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
			reshape(gl, (int)(viewport[2]), (int)( viewport[3]));
			zoomChanged = false;
			doRefresh = false;
		} else {	
			pan(gl);
		}

		// Clear Color Buffer, Depth Buffer
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

		// Mouse Released
		if (mouseReleased) app.getLassoTool().mouseClick(gl,mouseX,mouseY);

		// Mouse Dragged
		if (isDragging) app.getLassoTool().mouseDrag(gl,mouseX,mouseY);

		// Mouse Moved
		else mouseOver(gl, mouseOverX, mouseOverY);

		// Change Axis
		app.getCutter().rotateView(gl);

		// Update Background Image
		if (streamImages && streamer != null) {
			streamer.paintImage(gl, app.getCutter().getP());
		}

		// Draw Outlines
		if (showCrossSection) updateTriangles(gl);

		// Draw landmarks
		if (app.getViewer3D().getShowLandmarkPoints()) {
			drawLandmarks(gl);
		}

		mouseReleased = false;

	}

	public void pan(GL gl) {
		float coordWidth = (app.getCutter().getXMax() - app.getCutter().getXMin());
		float coordHeight = (app.getCutter().getYMax() - app.getCutter().getYMin());
		float aspectWidth = (screenWidth/(coordWidth*zoomFactor));
		float aspectHeight = (screenHeight/(coordHeight*zoomFactor));
		float alpha = 0.01f;
		Vector dir = app.getCutter().getN().getNormalized();

		app.getCutter().rotateView(gl);
		if (dir.getX() < alpha && dir.getY() < alpha) {						//Z
			gl.glTranslatef(panX/aspectWidth, panY/aspectHeight,0f);
		} else if (dir.getX() < alpha && dir.getZ() < alpha) {				//Y
			gl.glTranslatef(panX/aspectWidth, 0f,-1f*panY/aspectHeight);
		} else if (dir.getY() < alpha && dir.getZ() < alpha) {				//X
			gl.glTranslatef(0f, panY/aspectHeight,-1f*panX/aspectWidth);
		} else
			Log.p("Unclear which axis this is.");
		app.getCutter().unrotateView(gl);
	}

	public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {}

	/** Called by the drawable during the first repaint after the component has 
	 * been resized. The client can update the viewport and view volume of the 
	 * window appropriately, for example by a call to 
	 * GL.glViewport(int, int, int, int); note that for convenience the component
	 * has already called GL.glViewport(int, int, int, int)(x, y, width, height)
	 * when this method is called, so the client may not have to do anything in
	 * this method.
	 * @param gLDrawable The GLDrawable object.
	 * @param x The X Coordinate of the viewport rectangle.
	 * @param y The Y coordinate of the viewport rectangle.
	 * @param width The new width of the window.
	 * @param height The new height of the window.
	 */
	public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
		reshape(gLDrawable.getGL(), width, height);
	}

	public void reshape (GL gl, int width, int height) {
		if (height <= 0) // avoid a divide by zero error!
			height = 1;
		screenWidth = width; screenHeight = height;
		aspectRatio = (width*1f/height)  *  (app.getCutter().getYMax()-app.getCutter().getYMin())/(app.getCutter().getXMax()-app.getCutter().getXMin()); 
		if (aspectRatio == 0f) aspectRatio = 0.001f;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();

		gl.glOrtho(
				app.getCutter().getXMin() 		*zoomFactor*aspectRatio, 
				app.getCutter().getXMax()  	*zoomFactor*aspectRatio, 
				app.getCutter().getYMin() *zoomFactor, 
				app.getCutter().getYMax() 	 	*zoomFactor, 
				-250.0f, 250.0f);

		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();


		if (!zoomChanged) app.getCutter().refresh();

		pan(gl);
	}

	public void refresh() { doRefresh = true; }

	//******************************************************************************************************************************
	//
	//			PAINTING & SELECTION
	//
	//******************************************************************************************************************************

	public void drawLandmarks(GL gl) {
		float pointSize = 4f;
		float scale = 1.0f;
		for (Landmark lm : app.getNewLandmarks()) {

			// if not currently highlighted boundary continue
			Vector v = app.getCutter().getN();
			// check which axis we are on
			Vertex p = app.getCutter().getP();
			if (Math.abs(p.getZ()-lm.getLocation().getZ()) < 0.1) {

				p = p.plus(v);
				gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_POINT);
				gl.glPointSize(pointSize*2);
				gl.glPushMatrix();
				gl.glBegin(GL.GL_POINTS);

				gl.glColor3f(lm.getDisplayColor().getRed()/256.0f, lm.getDisplayColor().getGreen()/256.0f, lm.getDisplayColor().getBlue()/256.0f);

				gl.glVertex3f(lm.getLocation().getX()*scale, lm.getLocation().getY()*scale, p.getZ()*scale-0.1f);

				gl.glEnd();
				gl.glPointSize(pointSize);
				gl.glPopMatrix();
			}
		}
	}
	
	public void updateTriangles(GL gl) {

		gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(1.0f, 1.0f);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
		colorCallList = app.getCutter().paintCrossSection(gl, FILL, colorCallList);

		gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		blackCallList = app.getCutter().paintCrossSection(gl,OUTLINE, blackCallList);
		gl.glDisable(GL.GL_LINE_SMOOTH);

		app.getCutter().refresh(false);
	}

	private void mouseOver(GL gl, int x, int y) {
		if (x == lastMouseOverX && y == lastMouseOverY) return; 		//If mouse hasn't moved, no point in bothering GPU...

		lastMouseOverX = x;
		lastMouseOverY = y;

		GLU glu = new GLU();
		int bufferSize = 4*app.getMesh().getTopology().getPolyhedra().size();
		IntBuffer buffer = BufferUtil.newIntBuffer(bufferSize);
		gl.glSelectBuffer(bufferSize, buffer);
		gl.glRenderMode(GL.GL_SELECT);
		gl.glInitNames();
		gl.glPushName(-1);

		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		int viewport[] = new int[4];
		gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

		glu.gluPickMatrix((double) x, (double) (viewport[3] - y), 1.0, 1.0, viewport, 0);
		gl.glOrtho(
				app.getCutter().getXMin()*zoomFactor*aspectRatio, 
				app.getCutter().getXMax()*zoomFactor*aspectRatio, 
				app.getCutter().getYMin()*zoomFactor, 
				app.getCutter().getYMax()*zoomFactor, -250.0f, 250.0f
		);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
		pan(gl);

		app.getCutter().rotateView(gl);
		app.getCutter().paintCrossSection(gl, FILL, colorCallList);

		int hits = gl.glRenderMode(GL.GL_RENDER);
		reshape(gl, (int)(viewport[2]), (int)( viewport[3]));
		int [] selected = new int[hits*4];
		buffer.get(selected);

		int target = -2; int index = 0, depth = 0;
		for(int i = 0; i < hits; i++) {
			if(index == 0 || selected[index+2] < depth) {
				//Update: Current record is closer
				index++;									//Skip depth of the name stack (always 1, here...)
				depth = selected[index++];    	//Min depth
				index++;                               		//Skip max depth
				target = selected[index++];
			} else {
				//Skip: Record is farther away
				int names = selected[index++];
				index += 2 + names;
			}
		}

		if (target != -2) {
			Polyhedron p = app.getMesh().getTopology().getPolyhedra().get(target);
			Log.setStatus(Mesh.getName(p.getMaterial()));
		} else {
			Log.setStatus(" ");
		}

	}

	//******************************************************************************************************************************
	//
	//			MOUSEADAPTER IMPLEMENTATION
	//
	//******************************************************************************************************************************

	protected int panX = 0, panY = 0;
	protected boolean zoomTool = false, panTool = false, pickTool = true;
	public void setZoomTool() { 	zoomTool = true; 	panTool = false; 	pickTool = false; 	glCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)); }
	public void setPanTool() { 		zoomTool = false;	panTool = true; 	pickTool = false; 	glCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); 	}
	public void setPickTool() { 		zoomTool = false;	panTool = false;	pickTool = true;	  	glCanvas.setCursor(Cursor.getDefaultCursor()); }

	/** Responds when mouse is clicked, calling selection on the given point */ 	
	public void mouseClicked(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
		mouseOverX = e.getX();
		mouseOverY = e.getY();
		mouseReleased = true;
		isDragging = false;
	}
	/** Responds when mouse is dragged, calling selection on the given point*/	
	public void mouseDragged(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
		mouseOverX = e.getX();
		mouseOverY = e.getY();
		mouseReleased = false;
		isDragging = true;
	}
	/** Responds when mousewheel scrolls, adjusting the zoom factor */			  	
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
			zoomFactor += 0.01f*e.getUnitsToScroll();   // Zoom in or out.
			if (zoomFactor <= 0.001f) zoomFactor = 0.001f;
		}
		zoomChanged = true;
	}
	/** Responds when mouse is released, turning "drag" mode off. */ 					
	public void mouseReleased(MouseEvent e) {	
		isDragging = false;
		mouseReleased = true;
	}
	/** Responds when mouse is moved, updating trackers. */								
	public void mouseMoved(MouseEvent e) 	{
		mouseOverX = e.getX();
		mouseOverY = e.getY();
	}

	public void mouseEntered(MouseEvent e) 	{}
	public void mouseExited(MouseEvent e)	 	{}
	public void mousePressed(MouseEvent e)	{
		mouseX = e.getX();
		mouseY = e.getY();
		isDragging = false;
		mouseReleased = true;
	}



}
