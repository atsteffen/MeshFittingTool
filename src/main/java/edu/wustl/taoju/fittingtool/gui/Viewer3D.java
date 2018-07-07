package app.gui;

import java.awt.*;
import java.awt.event.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;

import javax.media.opengl.*;   //JoGL Lib Imports
import javax.media.opengl.glu.GLU;

import app.RegistrationTool;
import app.tools.Log;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.Mesh;
import app.tools.topology.*;

public class Viewer3D extends GLPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

	//******************************************************************************************************************************
	//
	//			CONSTANTS AND VARIABLES
	//
	//******************************************************************************************************************************

	//************************VARIABLES

	private RegistrationTool app;
	private final float lineWidth = 1f;				//Default 1.0f.
	private final float thinLineWidth = 0.05f;		
	private final float creaseLineWidth = 6.0f;	//Default 5.0f;
	private final float pointSize = 4f;			//Default 3.0f. // May apparently be affected by lasso tool, which also draws in this GL context.
	private final float walkingSpeed = 0.8f; 	//Default 8. Max 100.
	private final float scale = RegistrationTool.SCALE;

	//*************************************

	/** Default */
	private static final long serialVersionUID = 1L;
	private final float PI_OVER_180 = (float)(Math.PI/180.0);

	private FloatBuffer orientation = FloatBuffer.wrap(new float[16]);
	private float xpos, ypos, yrot, heading = 0.0f;
	private float zpos = 12.5f;
	private float lookupdown = 0.0f;
	private float spinX = 0.0f;
	private float spinY = -25.0f;
	private float explodeFactor = 0.0f;		//Default 0. Max 100.

	protected HashMap<Integer, Boolean> partitionStatus;		// T or F for visibility. Partitions, when not listed, are always shown.

	//Set up lighting
	private float[] specular = { 0.5f, 0.5f, 0.5f, 0.5f };
	private float[] shininess =  { 20.0f };
	private float[] lightPosition = { 0.0f, 0.0f, 40.0f, 1.0f };
	private boolean enableLighting = true;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTORS AND INITIALIZATION
	//
	//******************************************************************************************************************************

	/** Creates a new instance of Lesson10
	 * @param dim The Dimension of the Frame by which to view the canvas.
	 * @param fscreen A boolean value to set fullscreen or not
	 */
	public Viewer3D(RegistrationTool app, GLCapabilities glCap)
	{
		super("Viewer3D", glCap);
		this.app = app;
		this.setShadingEnabled(enableLighting);
	}

	/** Load a new mesh and display it.
	 * @param model
	 */
	public void loadMesh(Mesh sector) {
		partitionStatus = new HashMap<Integer, Boolean>();
		refresh();
	}
	
	/** Load a new mesh and display it.
	 * @param model
	 */
	public void reloadMesh(Mesh sector) {
		refresh();
	}


	/**  Called by the drawable immediately after the OpenGL context is 
	 * initialized for the first time. Can be used to perform one-time OpenGL 
	 * initialization such as setup of lights and display lists.
	 * @param gLDrawable The GLDrawable object.
	 */
	public void init(GLAutoDrawable gLDrawable)
	{
		//GLU glu = new GLU();
		GL gl = gLDrawable.getGL();
		gLDrawable.setGL( new DebugGL(gLDrawable.getGL()));
		if (Log.glVersion == null) Log.glVersion = gl.glGetString(GL.GL_VERSION);


		gl.glShadeModel(GL.GL_SMOOTH);
		gl.glClearColor(1.0f,1.0f,1.0f,0.5f);
		//gl.glClearColor(0.95f, 0.95f, 0.95f, 0.5f);  			 							// Background Color
		gl.glClearDepth(1.0f);                     				 									// Depth Buffer Setup
		gl.glEnable(GL.GL_DEPTH_TEST);             				 							// Enables Depth Testing
		gl.glDepthFunc(GL.GL_LEQUAL);            					 					  	// The Type Of Depth Testing To Do
		gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);	// Really Nice Perspective Calculations
		gl.glEnable(GL.GL_TEXTURE_2D);
		gLDrawable.addKeyListener(this);           	 									// Listening for key events

		gl.glLineWidth(lineWidth);
		gl.glPointSize(pointSize);
		//gl.glEnable(GL.GL_LINE_SMOOTH);	//Anti-aliased lines are too thick...

		gl.glFrontFace(GL.GL_CW);
		gl.glEnable(GL.GL_CULL_FACE);	

		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, FloatBuffer.wrap(specular));
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, FloatBuffer.wrap(shininess));
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, FloatBuffer.wrap(lightPosition));
		gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE);
		gl.glEnable(GL.GL_COLOR_MATERIAL);
		gl.glEnable(GL.GL_LIGHT0);

		gl.glMatrixMode( GL.GL_MODELVIEW ) ;
		gl.glPushMatrix( ) ;
		gl.glLoadIdentity( ) ;
		gl.glGetFloatv(GL.GL_MODELVIEW_MATRIX, orientation);
		gl.glPopMatrix( ) ;

		glCanvas.addMouseMotionListener(this);
		glCanvas.addMouseWheelListener(this);
		glCanvas.addMouseListener(this);

		initCallLists(gl);
	}


	//******************************************************************************************************************************
	//
	//			GUI FIELDS
	//
	//******************************************************************************************************************************
	private boolean invertIntersections = false;

	private boolean showPoints 	= false;
	private boolean showEdges 	= true;
	private boolean showFaces 	= false;

	private boolean showLandmarkPoints = false;
	private boolean showCreasePoints = false;
	private boolean showCreaseFaces = false;
	private boolean showCreaseEdges = false;
	private boolean showExteriorCreaseFaces= true;

	/**Select to affect all elements */ 		public static final short ELEMENTS_ALL 				= 0;
	/**Select to affect points */ 				public static final short ELEMENTS_POINTS 			= 1;	
	/**Select to affect edges */ 				public static final short ELEMENTS_EDGES			= 2;	
	/**Select to affect faces */ 				public static final short ELEMENTS_FACES 			= 3;

	/**Specify crease edges*/					public static final short CREASE_EDGES					= 102;
	/**Specify crease points*/					public static final short CREASE_POINTS					= 103;
	/**Exterior crease faces*/					public static final short CREASE_FACES_EXTERIOR 		= 100;
	/**Interior crease faces */					public static final short CREASE_FACES_INTERIOR		= 101;
	/**Invert intersections 	*/					public static final short INVERT_INTERSECTIONS 		= 110;

	/**Color elements by material/partition.*/ public static final short COLORMODEL_MATERIAL	= 0;	
	/**Color elements by their relative importance.*/ public static final short COLORMODEL_DEGREE		= 1;	
	/**Color elements a single solid color. (black)*/ public static final short COLORMODEL_SOLID		= 2;		

	//******************************************************************************************************************************
	//
	//			GUI ACCESSORS
	//
	//******************************************************************************************************************************

	public boolean isShown(int id) {
		if (id == Mesh.NULL_BOUNDARY)
			return false;
		else if (partitionStatus.get(id) == null)
			return true;
		else
			return partitionStatus.get(id);
	}
	/** Returns true if a face should be inverted, false if not, and null if the face should not be shown.
	 * @param face-in-question
	 * @return shouldBeFlipped
	 */
	public Boolean isFaceInverted(Face f) {
		boolean nullSided = (f.getMaterial(true) == Mesh.NULL_BOUNDARY || f.getMaterial(false) == Mesh.NULL_BOUNDARY);
		boolean bothSidesHidden = (!isShown(f.getMaterial(true)) && !isShown(f.getMaterial(false)));
		boolean oneSideHidden = !nullSided && (isShown(f.getMaterial(true)) ^ isShown(f.getMaterial(false)));
		boolean show = (
				(showExteriorCreaseFaces && nullSided )	
				||
				(!nullSided && showCreaseFaces) 				
				||
				(oneSideHidden)
		)&&(!bothSidesHidden);

		if (!show)
			return null;

		boolean flip = (!nullSided) && invertIntersections && (showCreaseFaces || showExteriorCreaseFaces || showFaces);

		return flip;
	}
	/**Returns the ID numbers for each partition in the model.
	 *  Handy way to get the regions needed to call setRegionVisibility(...).
	 * @return array of ID numbers
	 */
	public int[] getComponentList () {
		return app.getMesh().getPartitionIDs();
	}

	//******************************************************************************************************************************
	//
	//			GUI MUTATORS
	//
	//******************************************************************************************************************************

	public void setVisible(short elementType, boolean visible) {
		switch (elementType) {
		case ELEMENTS_ALL :
			showPoints = showEdges = showFaces = visible;
			break;
		case ELEMENTS_POINTS :
			showLandmarkPoints = visible;
			break;
		case ELEMENTS_EDGES :
			showEdges = visible;
			break;
		case ELEMENTS_FACES :
			showFaces = visible;
			break;
		default:
			throw new NoSuchElementException("No type of element matching the input was found. " +
					"Please check provided identifier, elementType=" + elementType + ".");
		}
		refresh();
	}
	public void setShadingEnabled(boolean enable) {
		if (enable)
			enableLighting = true;
		else
			enableLighting = false;
	}

	public void setRegionVisibility (int regionID, boolean isVisible) {
		partitionStatus.put(regionID, isVisible);
		refresh();
	}
	
	public void setCreaseVisibility (short creaseType, boolean alwaysVisible) {
		switch (creaseType) {
		case CREASE_FACES_INTERIOR :
			showCreaseFaces = alwaysVisible;
			break;
		case CREASE_EDGES : 
			showCreaseEdges = alwaysVisible;
			break;
		case CREASE_POINTS :
			showCreasePoints = alwaysVisible;
			break;
		case CREASE_FACES_EXTERIOR :
			showExteriorCreaseFaces = alwaysVisible;
			break;
		case INVERT_INTERSECTIONS :
			invertIntersections = alwaysVisible;
			break;
		default:
			throw new InvalidParameterException("Unrecognized crease-type identifier.");
		}
		refresh();
	}
	
	//******************************************************************************************************************************
	//
	//			RUNTIME OPERATION
	//
	//******************************************************************************************************************************
	/** Offset for polyhedral call-list 		*/ 	private int offsetPolyhedra;
	/** Offset for crease points				*/ private int offsetCreasePoints;
	/** Offset for crease edges					*/	private int offsetCreaseEdges;
	/** Offset for crease faces					*/ private int offsetCreaseFaces;
	/** Offset for selected polyhedra 		*/ 	private int offsetSelected;
	/** Offset for expression point cloud	*/ private int offsetCloud;

	private boolean doRefresh = true; 

	public  void refresh() { refresh(true); }
	public  void refresh(boolean doUpdates) { doRefresh = doUpdates; /* onlyRefreshSelection = false; */ }
	private void refreshCallLists(GL gl) {
		if (! app.getMesh().hasNewSelections(Mesh.VIEW_ID_1)) { 
			gl.glNewList(offsetPolyhedra, GL.GL_COMPILE);
			displayPolyhedra(gl);
			gl.glEndList();

			gl.glNewList(offsetCreasePoints, GL.GL_COMPILE);
			displayCreasePoints(gl);
			gl.glEndList();

			gl.glNewList(offsetCreaseEdges, GL.GL_COMPILE);
			displayCreaseEdges(gl);
			gl.glEndList();

			gl.glNewList(offsetCreaseFaces, GL.GL_COMPILE);
			displayCreaseFaces(gl);
			gl.glEndList();
		} 		
		gl.glNewList(offsetSelected, GL.GL_COMPILE);
		displayPolyhedra(gl,true);
		gl.glEndList();
	}

	private void initCallLists(GL gl) {
		offsetPolyhedra = gl.glGenLists(1);
		offsetCreasePoints = gl.glGenLists(1);
		offsetCreaseEdges = gl.glGenLists(1);
		offsetCreaseFaces = gl.glGenLists(1);
		offsetSelected = gl.glGenLists(1);
		offsetCloud = gl.glGenLists(1);
	}

	/** Called by drawable to initiate drawing
	 * @param gLDrawable The GLDrawable Object
	 */
	public  void display(GLAutoDrawable gLDrawable) 
	{
		GL gl = gLDrawable.getGL();
		gl.glPushMatrix();

		displayScene(gl);

		if (doRefresh) {
			refreshCallLists(gl);
			doRefresh = false;
		}

		displayCallLists(gl);
		displayCuttingPlane(gl);
		displayLandmarkPoints(gl);

		gl.glPopMatrix();

		parseKeys(gl);
	}
	private void displayScene(GL gl) {
		gl.glMatrixMode( GL.GL_MODELVIEW ) ;
		gl.glPushMatrix( ) ;
		gl.glLoadIdentity( ) ;
		gl.glRotatef(spinY, 0, 1, 0); spinY = 0;
		gl.glRotatef(spinX, 1, 0, 0); spinX = 0;
		gl.glMultMatrixf ( orientation );
		gl.glGetFloatv( GL.GL_MODELVIEW_MATRIX, orientation ) ;
		gl.glPopMatrix( ) ;

		// Clear Color Buffer, Depth Buffer
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );
		gl.glLoadIdentity();

		float sceneroty = 360.0f - yrot;

		gl.glRotatef(lookupdown, 1.0f, 0.0f, 0.0f);	//Orient the view-frame
		gl.glRotatef(sceneroty, 0.0f, 1.0f, 0.0f);

		gl.glTranslatef(-xpos, -ypos, -zpos); 			//Move the camera away from the model
		gl.glMultMatrixf(orientation);						//Rotate the model

		if (enableLighting) {
			gl.glEnable(GL.GL_LIGHTING);
		} else {
			gl.glDisable(GL.GL_LIGHTING);
		}
	}
	private void displayCuttingPlane(GL gl) {
		if (app.getCutter() != null)		{ app.getCutter() .paintGLClipPlane(gl); }
	}
	@SuppressWarnings("unused")
	private void displayCallLists(GL gl) {

		gl.glPushMatrix(); 
		gl.glDisable(GL.GL_LIGHTING);
		//gl.glLineWidth( !selectionMode ? lineWidth : thinLineWidth ); TODO
		gl.glLineWidth(thinLineWidth);

		if (showPoints) {																				////////////Paint points
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_POINT);
			gl.glCallList(offsetPolyhedra);
		}
		if (showEdges && false /* TODO */ ) {												////////////Paint edges
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
			gl.glCallList(offsetPolyhedra);
		}

		gl.glPopMatrix();	
		if (enableLighting) gl.glEnable(GL.GL_LIGHTING);// ...only allow shading for faces...

		if (showFaces && false /* TODO */ ) {													////////////Paint faces
			if (showEdges || showPoints) { 
				gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
			}
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
			gl.glCallList(offsetPolyhedra);
			if (showEdges || showPoints) 
				gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		}

		if ( !  app.getViewer2D().getShowExpression()) {
			ByteBuffer buffer = ByteBuffer.wrap(new byte[1]);
			gl.glGetBooleanv(GL.GL_CLIP_PLANE0, buffer);
			byte clipping = buffer.get(0);
			if (clipping == 1)
				gl.glDisable(GL.GL_CLIP_PLANE0);

			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
			gl.glCallList(offsetSelected);

			if (clipping == 1)
				gl.glEnable(GL.GL_CLIP_PLANE0);
		} else {
			gl.glCallList(offsetCloud);
		}

		////// Show topology. //////

		displayCreaseTopology(gl, (short)0);

	}



	/*****************Main painting routine********************/
	private void displayPolyhedra(GL gl, boolean selectionMode) {
		if (app.getMesh() != null) {
			Vertex meshCenter = app.getMesh().getCentroid();

			ArrayList<Polyhedron> polys = app.getMesh().getTopology().getPolyhedra();
			for (int j = 0; j < polys.size(); ++j) {									//...and for each polygon in that region...
				if (!isShown(polys.get(j).getMaterial())) 
					continue;

				int[][] faces = new int[polys.get(j).getNumFaces()][3];
				Vector[] normals = new Vector[polys.get(j).getNumFaces()];

				polys.get(j).getFaceData(app.getMesh().getGeometry(), faces, normals);

				Vertex polyCenter = polys.get(j).getCentroid(app.getMesh().getGeometry()); 
				Vector explodeOffset = new Vector (meshCenter, polyCenter).getScaled(explodeFactor);

				if (selectionMode) 
					if (polys.get(j).isSelected()) {
						gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
					}
					else
						continue;

				for (int k = 0; k < faces.length; ++k) {						//...draw all faces.

					Vertex [] v = new Vertex[3];
					for (int i = 0; i < 3; ++i)
						v[i] = app.getMesh().getGeometry().get(faces[k][i]);

					gl.glBegin(GL.GL_TRIANGLES);

					Color c = Mesh.getColor(polys.get(j).getMaterial());
					gl.glColor3f(c.getRed()/256.0f, c.getGreen()/256.0f, c.getBlue()/256.0f);

					Vector n = normals[k];					//normal vector
					gl.glNormal3f(n.getX(), n.getY(), n.getZ());				//Colors can also store alphas?			

					gl.glVertex3f(v[0].getX()*scale+explodeOffset.getX(), v[0].getY()*scale+explodeOffset.getY(), v[0].getZ()*scale+explodeOffset.getZ());
					gl.glVertex3f(v[1].getX()*scale+explodeOffset.getX(), v[1].getY()*scale+explodeOffset.getY(), v[1].getZ()*scale+explodeOffset.getZ());
					gl.glVertex3f(v[2].getX()*scale+explodeOffset.getX(), v[2].getY()*scale+explodeOffset.getY(), v[2].getZ()*scale+explodeOffset.getZ());


					gl.glEnd();

				}

			}
		}
	}
	private void displayPolyhedra(GL gl) {
		displayPolyhedra(gl, false); 
	}
	/*_****************Crease feature painting*****************_*/
	private void displayCreaseTopology(GL gl, short colorModel) {
		gl.glDisable(GL.GL_LIGHTING);

		gl.glCallList(offsetCreasePoints); 
		gl.glCallList(offsetCreaseEdges); 

		if (enableLighting) gl.glEnable(GL.GL_LIGHTING);

		gl.glCallList(offsetCreaseFaces); 

	}
	private void displayCreasePoints(GL gl) {
		if (!showCreasePoints) return;

		for (Integer p : app.getMesh().getTopology().getPoints()) {

			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_POINT);
			gl.glPointSize(pointSize*2);
			gl.glPushMatrix();
			gl.glBegin(GL.GL_POINTS);

			gl.glColor3f(1.0f, 0.0f, 0.0f);

			Vector explVec = new Vector (app.getMesh().getCentroid(), app.getMesh().getGeometry().get(p)).getScaled(explodeFactor);
			gl.glVertex3f(app.getMesh().getGeometry().get(p).getX()*scale+explVec.getX(), app.getMesh().getGeometry().get(p).getY()*scale+explVec.getY(), app.getMesh().getGeometry().get(p).getZ()*scale+explVec.getZ());

			gl.glEnd();
			gl.glPointSize(pointSize);
			gl.glPopMatrix();
		}
	}
	private void displayLandmarkPoints(GL gl) {
		if (!showLandmarkPoints) return;

		for (Landmark lm : app.getNewLandmarks()) {

			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_POINT);
			gl.glPointSize(pointSize*2);
			gl.glPushMatrix();
			gl.glBegin(GL.GL_POINTS);

			gl.glColor3f(lm.getDisplayColor().getRed()/256.0f, lm.getDisplayColor().getGreen()/256.0f, lm.getDisplayColor().getBlue()/256.0f);

			gl.glVertex3f(lm.getLocation().getX()*scale, lm.getLocation().getY()*scale, lm.getLocation().getZ()*scale);

			gl.glEnd();
			gl.glPointSize(pointSize);
			gl.glPopMatrix();
		}
	}
	private void displayCreaseEdges(GL gl) {
		if (!showCreaseEdges) return;

		for (Edge e : app.getMesh().getTopology().getEdges()) {
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
			gl.glLineWidth(creaseLineWidth);
			gl.glPushMatrix();
			gl.glBegin(GL.GL_LINES);

			gl.glColor3f(0.0f, 0.0f, 0.0f);

			Vertex v1 = app.getMesh().getGeometry().get(e.getVertices()[0]);
			Vertex v2 = app.getMesh().getGeometry().get(e.getVertices()[1]);
			Vertex mid = Vertex.midpoint(v1,v2);
			Vector explVec = new Vector(app.getMesh().getCentroid(), new Vertex(mid.getX(), mid.getY(), mid.getZ())).getScaled(explodeFactor);

			gl.glVertex3f(v1.getX()*scale + explVec.getX(), v1.getY()*scale + explVec.getY(), v1.getZ()*scale + explVec.getZ());
			gl.glVertex3f(v2.getX()*scale + explVec.getX(), v2.getY()*scale + explVec.getY(), v2.getZ()*scale + explVec.getZ());

			gl.glEnd();
			gl.glLineWidth(lineWidth);
			gl.glPopMatrix();
		}
	}
	private void displayCreaseFaces(GL gl) {	
		if (!showCreaseFaces && !showExteriorCreaseFaces && !invertIntersections) 	return;		//Break if there's nothing to do

		for (Face f : app.getMesh().getTopology().getFaces()) {				
			Boolean flip = isFaceInverted(f);
			if (flip == null)
				continue;		

			if (showFaces && (showEdges || showPoints)) { 		//Offset faces to allow points & lines to display properly, if there are any.
				gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				gl.glLineWidth(3f);
			}

			for (int i= 0; i < 2; ++i) {
				if (i == 0) 
					if (showFaces) {
						gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
					}
					else continue;
				else 
					if (showEdges) {
						if (showFaces) gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
						gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);
					}
					else continue;

				//			if (!selectionMode)
				//				gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
				//			else
				//				gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_LINE);

				gl.glPushMatrix();
				gl.glBegin(GL.GL_TRIANGLES);

				Vector xVec = new Vector(app.getMesh().getCentroid(), f.getCentroid(app.getMesh().getGeometry())).getScaled(explodeFactor);

				for (short run = 1; run < 3; ++run) {	//Display both sides of the face.
					boolean side = (run ==1);
					boolean flip2 = false;
					if (!isShown(f.getMaterial(side))) {
						if (isShown(f.getMaterial(!side))) {
							flip2 = true;
						}
					}

					Vector n = f.getNormal(app.getMesh().getGeometry(), side);
					gl.glNormal3f(n.getX(), n.getY(), n.getZ());
					Color c = f.getColor(side^flip^flip2);
					gl.glColor3f(c.getRed()/256.0f, c.getGreen()/256.0f, c.getBlue()/256.0f);
					float[][] fD = f.getFaceData(app.getMesh().getGeometry(), side);
					gl.glVertex3f(		fD[0][0]*scale + xVec.getX(),		fD[0][1]*scale + xVec.getY(),		fD[0][2]*scale + xVec.getZ());
					gl.glVertex3f(		fD[1][0]*scale + xVec.getX(),		fD[1][1]*scale + xVec.getY(),		fD[1][2]*scale + xVec.getZ());
					gl.glVertex3f(		fD[2][0]*scale + xVec.getX(),		fD[2][1]*scale + xVec.getY(),		fD[2][2]*scale + xVec.getZ());

				}

				gl.glEnd();
				gl.glPopMatrix();

				break;
			}

//			if (showEdges || showPoints) 
//				gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
		}
	}
//	private void displayExpression (GL gl) {
//		//gl.glDisable(GL.GL_LIGHTING);  //...It's debatable whether or not lighting helps.
////		Vertex origin = new Vertex(0f,0f,0f);
////		
////		for (Polyhedron p : model.getTopology().getPolyhedra()) {
////			if (   !   p.isSelected()) continue;
////			Vertex centroid = p.getCentroid(model.getGeometry());
////			Vector normal = new Vector(origin,centroid).getNormalized();
////			Color c = Polyhedron.getExpressionColor(p.getExpression());
////			gl.glColor3f(c.getRed()/255f,c.getGreen()/255f,c.getBlue()/255f);
////			gl.glNormal3f(normal.getX(),normal.getY(),normal.getZ());
////			gl.glPointSize(pointSize*2);
////			gl.glBegin(GL.GL_POINTS);
////			gl.glVertex3f(
////					centroid.getX(),
////					centroid.getY(),
////					centroid.getZ()
////			);
////			gl.glEnd();
////			gl.glPointSize(pointSize);
////		}
//		if (RegistrationTool.instance.getMesh() != null) {
//			boolean showDensity = true;
//			
//			float scalePoly = 1; float minVol = 0;
//			boolean getVol=false;
//			ArrayList<Polyhedron> polys = RegistrationTool.instance.getMesh().getTopology().getPolyhedra();
//			for (Polyhedron p : polys)
//				if (p.getExprData() != null) getVol=true;
//			if (getVol) minVol = getMinVol(RegistrationTool.instance.getMesh());
//			for (int j = 0; j < polys.size(); ++j) {			//...and for each polygon in that region...
//				if (!isShown(polys.get(j).getMaterial())) 
//					continue;
//
//				int[][] faces = new int[polys.get(j).getNumFaces()][3];
//				Vector[] normals = new Vector[polys.get(j).getNumFaces()];
//
//				polys.get(j).getFaceData(RegistrationTool.instance.getMesh().getGeometry(), faces, normals);
//
//				Vertex polyCenter = polys.get(j).getCentroid(RegistrationTool.instance.getMesh().getGeometry()); 
//				//Vector explodeOffset = new Vector (meshCenter, polyCenter).getScaled(explodeFactor);
//				try {scalePoly = polys.get(j).getExprData().getPointSize(minVol,showColor);}
//				catch (NullPointerException e) {scalePoly=1;}
//				//TODO optionally disable transparency for efficiency's sake
//				boolean trans = true;
//				if (trans && showDensity) {
//					gl.glEnable(GL.GL_BLEND);	//Enable blending
//					gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//				}
//				
//				boolean selectionMode = true, showSelected = true;
//				if (selectionMode || showSelected) 
//					if (polys.get(j).isSelected() || Mesh.showUnselected)
//						gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL.GL_FILL);
//					else
//						continue;
//				
//				if (polys.get(j).getExprData() != null && ! Mesh.getShown(polys.get(j).getExprData().getStrength())) continue;	
//				
//				Color c = polys.get(j).getColor(); //polys.get(j).getExpressionColor(); //TODO getPColor(polys.get(j),selectionMode);
//				
//				for (int k = 0; k < faces.length; ++k) {						//...draw all faces.
//
//					Vertex [] v = new Vertex[3];
//					for (int i = 0; i < 3; ++i)
//						v[i] = RegistrationTool.instance.getMesh().getGeometry().get(faces[k][i]);
//
//					gl.glBegin(GL.GL_TRIANGLES);
//
//					gl.glColor4f(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f, (trans) ? Viewer2D.FILL_ALPHA : c.getAlpha()/255f);//
//
//					Vector n = normals[k];					//normal vector
//					gl.glNormal3f(n.getX(), n.getY(), n.getZ());				//Colors can also store alphas?			
//					gl.glVertex3f(	polyCenter.getX() + (v[0].getX() - polyCenter.getX())/scalePoly,
//									polyCenter.getY() + (v[0].getY() - polyCenter.getY())/scalePoly,
//									polyCenter.getZ() + (v[0].getZ() - polyCenter.getZ())/scalePoly);
//					gl.glVertex3f(	polyCenter.getX() + (v[1].getX() - polyCenter.getX())/scalePoly,
//									polyCenter.getY() + (v[1].getY() - polyCenter.getY())/scalePoly,
//									polyCenter.getZ() + (v[1].getZ() - polyCenter.getZ())/scalePoly);
//					gl.glVertex3f(	polyCenter.getX() + (v[2].getX() - polyCenter.getX())/scalePoly,
//									polyCenter.getY() + (v[2].getY() - polyCenter.getY())/scalePoly,
//									polyCenter.getZ() + (v[2].getZ() - polyCenter.getZ())/scalePoly);
//					gl.glEnd();
//				}
//				if (trans && showDensity) gl.glDisable(GL.GL_BLEND);
//			}
//		}
//	}
//	
//	public float getMinVol(Mesh mesh) {
//		float vol = Float.MAX_VALUE;
//		for (Polyhedron p : mesh.getTopology().getPolyhedra()) {
//			int[] hmln;
////			try {hmln = p.getExprData().getHMLN();}
////			catch (NullPointerException e) {hmln = new int[] {0,0,0,0};}
////			float total = hmln[0]+hmln[1]+hmln[2]+hmln[3];
//			if (total > 0)
//				if ((p.getVolume(mesh.getGeometry())/(total)) < vol)
//					vol = p.getVolume(mesh.getGeometry())/(total);
//		}
//		return vol;
//	}
	
	/** Called by drawable to show that a mode or device has changed <br>
	 * <B>!! CURRENTLY NON-Functional IN JoGL !!</B>
	 * @param gLDrawable The GLDrawable object.
	 * @param modeChanged Indicates if the video mode has changed.
	 * @param deviceChanged Indicates if the video device has changed.
	 */
	public void displayChanged(GLAutoDrawable gLDrawable,  boolean modeChanged, boolean deviceChanged)  {}
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
	public void reshape(GLAutoDrawable gLDrawable, 
			int x, 
			int y, 
			int width, 
			int height)
	{
		GLU glu = new GLU();
		GL gl = gLDrawable.getGL();

		if (height <= 0) // avoid a divide by zero error!
			height = 1;
		float h = (float)width / (float)height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, h, 1, 1000);
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	//******************************************************************************************************************************
	//
	//			USER INTERFACE (KEYBOARD AND MOUSE)
	//
	//******************************************************************************************************************************

	protected boolean isDragging = false;
	protected int originX;
	protected int originY;
	protected final float rotConst = -0.2f; // Default -0.2f

	public void mouseDragged(MouseEvent e) {
		spinY += (originX - e.getX())*rotConst;
		originX = e.getX();
		spinX += (originY - e.getY())*rotConst;
		originY = e.getY();



		//drag(e.getPoint());

	}
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
			zpos += (float)Math.cos(heading*PI_OVER_180) * 0.1f*walkingSpeed*e.getUnitsToScroll();   // Move On The X-Plane Based On Player Direction
			xpos += (float)Math.sin(heading*PI_OVER_180) * 0.1f*walkingSpeed*e.getUnitsToScroll();   // Move On The X-Plane Based On Player Direction
			ypos += (float)Math.sin(lookupdown*PI_OVER_180)*0.1f*walkingSpeed*e.getUnitsToScroll();		// Move On The Y-Plane Based On Player Direction
		}
	}
	public void mousePressed(MouseEvent e) {
		originX = e.getX();
		originY = e.getY();
		mapToSphere(e.getPoint(), this.StVec);
	}

	/**
	 * Register and act on any keys that have been pressed.
	 * @param gl
	 */
	private void parseKeys(GL gl) {
		if (keys[KeyEvent.VK_RIGHT])
		{
			heading -= 0.1f*walkingSpeed;
			yrot = heading;
		}
		else if (keys[KeyEvent.VK_LEFT])
		{
			heading += 0.1f*walkingSpeed;
			yrot = heading;
		}
		if (keys[KeyEvent.VK_PAGE_UP])
		{
			xpos -= (float)Math.sin(heading*PI_OVER_180) * 0.1f*walkingSpeed;   // Move On The X-Plane Based On Player Direction
			zpos -= (float)Math.cos(heading*PI_OVER_180) * 0.1f*walkingSpeed;   // Move On The Z-Plane Based On Player Direction
			ypos -= (float)Math.sin(lookupdown*PI_OVER_180)*0.1f*walkingSpeed;		// Move On The Y-Plane Based On Player Direction
		}
		else if (keys[KeyEvent.VK_PAGE_DOWN])
		{
			xpos += (float)Math.sin(heading*PI_OVER_180) * 0.1f*walkingSpeed;    // Move On The X-Plane Based On Player Direction
			zpos += (float)Math.cos(heading*PI_OVER_180) * 0.1f*walkingSpeed;    // Move On The Z-Plane Based On Player Direction
			ypos += (float)Math.sin(lookupdown*PI_OVER_180)*0.1f*walkingSpeed;		// Move On The Y-Plane Based On Player Direction

		}
		if (keys[KeyEvent.VK_UP])
		{
			lookupdown -= 0.1f*walkingSpeed;
		}
		else if (keys[KeyEvent.VK_DOWN])
		{
			lookupdown += 0.1f*walkingSpeed;
		}
	}

	public void mouseClicked(MouseEvent e) 	{} //meh.
	public void mouseEntered(MouseEvent e)	{} 
	public void mouseExited(MouseEvent e)	 	{}
	public void mouseMoved(MouseEvent e) 	{} 
	public void mouseReleased(MouseEvent e){}

	//****************************************************************************************************************************** Rotation (kinda experimental)
	private static final float Epsilon = 1.0e-5f;
	Vector3f StVec = new Vector3f();
	Vector3f EnVec = new Vector3f();
	Quat4f NewRot = new Quat4f();

	public void mapToSphere(Point point, Vector3f vector) {
		//Copy parameter into temp point
		Point2f tempPoint = new Point2f(point.x, point.y);

		//Adjust point coords and scale down to range of [-1 ... 1]
		tempPoint.x = (tempPoint.x * 1.0f /*this.adjustWidth*/) - 1.0f;
		tempPoint.y = 1.0f - (tempPoint.y * 1.0f /*this.adjustHeight*/);

		//Compute the square of the length of the vector to the point from the center
		float length = (tempPoint.x * tempPoint.x) + (tempPoint.y * tempPoint.y);

		//If the point is mapped outside of the sphere... (length > radius squared)
		if (length > 1.0f) {
			//Compute a normalizing factor (radius / sqrt(length))
			float norm = (float) (1.0 / Math.sqrt(length));

			//Return the "normalized" vector, a point on the sphere
			vector.x = tempPoint.x * norm;
			vector.y = tempPoint.y * norm;
			vector.z = 0.0f;
		} else    //Else it's on the inside
		{
			//Return a vector to a point mapped inside the sphere sqrt(radius squared - length)
			vector.x = tempPoint.x;
			vector.y = tempPoint.y;
			vector.z = (float) Math.sqrt(1.0f - length);
		}

	}

	//Mouse drag, calculate rotation
	public void drag(Point NewPt) {
		//Map the point to the sphere
		this.mapToSphere(NewPt, EnVec);

		//Return the quaternion equivalent to the rotation
		if (NewRot != null) {
			Vector3f Perp = new Vector3f();

			//Compute the vector perpendicular to the begin and end vectors
			Vector3f.cross(Perp, StVec, EnVec);

			//Compute the length of the perpendicular vector
			if (Perp.length() > Epsilon)    //if its non-zero
			{
				//We're ok, so return the perpendicular vector as the transform after all
				NewRot.x = Perp.x;
				NewRot.y = Perp.y;
				NewRot.z = Perp.z;
				//In the quaternion values, w is cosine (theta / 2), where theta is rotation angle
				NewRot.w = Vector3f.dot(StVec, EnVec);
			} else                                    //if its zero
			{
				//The begin and end vectors coincide, so return an identity transform
				NewRot.x = NewRot.y = NewRot.z = NewRot.w = 0.0f;
			}
		}
	}

	public boolean getShowLandmarkPoints() {
		return showLandmarkPoints;
	}

}

class Point2f {
	public float x, y;

	public Point2f(float x, float y) {
		this.x = x;
		this.y = y;
	}
}

class Vector3f {
	public float x, y, z;

	public static void cross(Vector3f Result, Vector3f v1, Vector3f v2) {
		Result.x = (v1.y * v2.z) - (v1.z * v2.y);
		Result.y = (v1.z * v2.x) - (v1.x * v2.z);
		Result.z = (v1.x * v2.y) - (v1.y * v2.x);
	}

	public static float dot(Vector3f v1, Vector3f v2) {
		return (v1.x * v2.x) + (v1.y * v2.y) + (v1.z + v2.z);
	}

	public float length() {
		return (float)Math.sqrt(x * x + y * y + z * z);
	}
}

class Quat4f {
	public float x, y, z, w;
}

