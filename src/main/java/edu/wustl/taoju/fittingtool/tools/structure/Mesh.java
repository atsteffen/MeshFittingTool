package app.tools.structure;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Jama.Matrix;
import app.tools.Log;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.topology.Edge;
import app.tools.topology.Face;
import app.tools.topology.Landmark;
import app.tools.topology.Octahedron;
import app.tools.topology.Polyhedron;
import app.tools.topology.Tetrahedron;
import app.tools.topology.Topology;

public class Mesh {
	private Topology topology;
	private Geometry geometry;

	private Vertex centroid;
	private boolean[] newSelections;
	private int subDivLevel;

	public static final int VIEW_ID_1 = 0, VIEW_ID_2 = 1;

	//******************************************************************************************************************************
	//
	//			CONSTRUCTION & INITIALIZATION
	//
	//******************************************************************************************************************************

	public Mesh (Topology t, Geometry g) {
		topology = t;
		geometry = g;
		subDivLevel = 0;
		init();
	}

	private void init() {
		//Initialize centroid
		centroid = new Vertex(0,0,0);
		for (int i = 0; i < geometry.size(); ++i) {
			centroid.setX(geometry.get(i).getX() + centroid.getX());
			centroid.setY(geometry.get(i).getY() + centroid.getY());
			centroid.setZ(geometry.get(i).getZ() + centroid.getZ());
		}
		centroid.setX(centroid.getX()/geometry.size());
		centroid.setY(centroid.getY()/geometry.size());
		centroid.setZ(centroid.getZ()/geometry.size());

		newSelections = new boolean [] {false, false};
	}
	
	//******************************************************************************************************************************
	//
	//			ACCESSORS & MUTATORS
	//
	//******************************************************************************************************************************

	public int getSubDivLevel() { return subDivLevel; }

	public Topology getTopology() {
		return topology;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public Vertex getCentroid() {
		return centroid;
	}

	public int[] getPartitionIDs() {
		HashSet<Integer> partitionIDs = new HashSet<Integer>();
		for (Polyhedron p : topology.getPolyhedra()) 
			partitionIDs.add(p.getMaterial());

		int[] partitionIDsArray = new int[partitionIDs.size()];
		int i = 0; for (Integer mat : partitionIDs)
			partitionIDsArray[i++] = mat;

		return partitionIDsArray;
	}

	public boolean hasNewSelections(int viewIndex) { return newSelections[viewIndex]; }

	public void setNewSelections(int viewIndex, boolean newSelections) { 
		this.newSelections[viewIndex] = newSelections;
	}

	/** Method to alert BOTH views that there are new selections. */
	public void pushNewSelections() {
		setNewSelections(0, true);							
		setNewSelections(1, true);
	}

	//******************************************************************************************************************************
	//
	//			STATIC FUNCTIONALITY (e.g. component coloring)
	//
	//******************************************************************************************************************************

	/** Colors and values used to represent partitions. 
	 * NOTE: Color.DARK_GRAY is reserved for faces bounding "the void." 
	 */
	public final static int NULL_BOUNDARY = -99;
	/** Should represent the MAXIMUM number of vertices possible in any instance of the Mesh class. */
	public static final int DEFAULT_BUFFER = 1000000;

	public static boolean showHigh = true, showMod = true, showLow = true, showNone = true, showUnselected = false;;

	public static Color getColor(int material) { 
		if (material == NULL_BOUNDARY)
			return Regions.getColor(Regions.getDefaultIndex());
		//return submeshColors[material%submeshColors.length]; 
		return Regions.getColor(material);
	}

	public static String getName(int material) {
		if (material == NULL_BOUNDARY) 
			return "Null Region";
		else 
			return Regions.getName(material);
	}

	/** Sets visibility of unselected polyhedra.
	 * @param visible
	 */
	public static void setShown(boolean visible) {
			showUnselected = visible;
	}

	/** Tells whether or not polyhedra having a particular property should be shown.
	 * Display of unselected polyhedra is (or should be) subject to the other four rules.
	 * 
	 * @param property
	 * @return shown
	 */
	public static boolean getShown(int property) {
		switch (property) {
		case 3:
			return showHigh;
		case 2:
			return showMod;
		case 1:
			return showLow;
		case 0:
			return showNone;
		case -1:
			return showUnselected;
		default:
			Log.p("Unrecognized property type in Mesh's getShown() method.");
			return false;
		}
	}

	//******************************************************************************************************************************
	//
	//			SUBDIVISION
	//
	//******************************************************************************************************************************
	/** Original vertex weight */ private static float TET_WEIGHT_1 = -1.0f/16.0f;
	/** Adjacent vertex weight*/ private static float TET_WEIGHT_2 = 17.0f/48.0f;
	/** Original vertex weight */ private static float OCT_WEIGHT_1 = 3.0f/8.0f;
	/** Adjacent vertex weight*/ private static float OCT_WEIGHT_2 = 1.0f/12.0f;
	/** Opposite vertex weight*/ private static float OCT_WEIGHT_3 = 7.0f/24.0f;

	public void subdivide() {
		linearSubdivision();
		smoothing();
		subDivLevel++;
	}

	/** Splits each element of the mesh:
	 * 1 Tetrahedron -> 4 Tetrahedron + 1 Octahedron
	 * 1 Octahedron -> 8 Tetrahedron + 6 Octahedron
	 * 1 Crease Edge -> 2 Crease Edges
	 * 1 Crease Face -> 4 Crease Faces
	 * 1 Crease Vertex -> 1 Crease Vertex
	 */
	private void linearSubdivision() {
		GeometryMap hash = new GeometryMap(geometry);

		Topology oldTopology = topology;
		topology = new Topology(oldTopology.getPoints(), new ArrayList<Edge>(), new ArrayList<Face>(), new ArrayList<Polyhedron>());

		for (Polyhedron p : oldTopology.getPolyhedra()) 
			topology.getPolyhedra().addAll(p.subdivide(geometry, hash));
		for (Face f : oldTopology.getFaces())
			topology.getFaces().addAll(f.subdivide(geometry, hash));
		for (Edge e : oldTopology.getEdges()) 
			topology.getEdges().addAll(e.subdivide(geometry, hash));
	}
	/** Smooths all elements */ 
	private void smoothing() {
		short[] degree = initDegrees();
		int[] valence = initValences();

		Vertex[] refVertices = new Vertex[geometry.getPoints().size()];
		for (int i = 0; i < geometry.getPoints().size(); ++i)  {
			refVertices[i] = new Vertex(geometry.get(i).getX(),geometry.get(i).getY(),geometry.get(i).getZ() );
			geometry.get(i).setXYZ(new float[] {0f,0f,0f});	
		}

		smoothPoints(valence, degree, refVertices);
		smoothEdges(valence, degree, refVertices);
		smoothFaces(valence, degree, refVertices);
		smoothPolyhedra(valence, degree, refVertices);

		for (int i = 0; i < geometry.size(); ++i) {
			Vertex v = geometry.get(i);	
			v.setXYZ(new float[]{	v.getX()/valence[i],		v.getY()/valence[i],			v.getZ()/valence[i]			});
		}
	}

	/** Smooth crease-points */ 			private void smoothPoints(int[] valence, short[] degree, Vertex[] ref) {
		for (Integer i : topology.getPoints())  {
			if (degree[i] == Vertex.CREASE_POINT) {
				geometry.get(i).setXYZ(ref[i].getXYZ());
				valence[i] = 1;
			} else { Log.p("Error: Invalid crease-point in smoothing step."); }
		}
	}
	/** Smooth crease-edge points*/ 	private void smoothEdges(int[] valence, short[] degree, Vertex[] ref) {
		for (Edge e : topology.getEdges()) {
			Vertex a = ref[e.getVertices()[0]];			
			Vertex b = ref[e.getVertices()[1]];
			Vertex mid = Vertex.midpoint(a, b);

			for (int i = 0; i < e.getVertices().length; ++i) {
				if ( degree[e.getVertices()[i]] == Vertex.CREASE_EDGE){
					Vertex cur = geometry.get(e.getVertices()[i]);
					cur.setXYZ(new float[]{	cur.getX()+mid.getX(),		 cur.getY()+mid.getY(), 		cur.getZ()+mid.getZ()});
					valence[e.getVertices()[i]]++;
				}
			}
		}
	}
	/** Smooth crease-face points */	 	private void smoothFaces(int[] valence, short[] degree, Vertex[] ref) {
		for (Face f : topology.getFaces()) 	//Pre-compute valences for crease-face vertices
			for (int i : f.getPoints()) 
				if (degree[i] == Vertex.CREASE_FACE) 
					valence[i]++;

		for (Face f : topology.getFaces()) {
			for (int i = 0; i < f.getPoints().length; ++i) 
				if (degree[f.getPoints()[i]] == Vertex.CREASE_FACE) {	
					int j = f.getPoints()[i];

					float w = (5f/8f) - (float)Math.pow((3f/8f) + (1f/4f)*
							Math.cos(2*Math.PI/valence[j]), 2);

					Vertex a = ref[f.getPoints()[i]				].getScaled(1-2*w);
					Vertex b = ref[f.getPoints()[(i+1)%3]	].getScaled(w);
					Vertex c = ref[f.getPoints()[(i+2)%3]	].getScaled(w);

					Vertex loopCentroid = new Vertex(		
							(a.getX()+b.getX()+c.getX()), 
							(a.getY()+b.getY()+c.getY()),
							(a.getZ()+b.getZ()+c.getZ())
					);

					geometry.get(j).setX(geometry.get(j).getX() + loopCentroid.getX());
					geometry.get(j).setY(geometry.get(j).getY() + loopCentroid.getY());
					geometry.get(j).setZ(geometry.get(j).getZ() + loopCentroid.getZ());

				} else if (degree[f.getPoints()[i]] > Vertex.CREASE_FACE) {
					Log.p("ERROR: Vertex of degree " + degree[i] + " found in a crease face.");
				}
		}
	}
	/** Smooth all other points */			private void smoothPolyhedra(int[] valence, short[] degree, Vertex[] ref) {
		for (Polyhedron p : topology.getPolyhedra()) {
			if (p instanceof Tetrahedron) 
				smoothTetrahedron((Tetrahedron)p, valence, degree, ref);
			else
				smoothOctahedron((Octahedron)p, valence, degree, ref);
		}
	}

	private void smoothTetrahedron(Tetrahedron poly, int[] valence, short[] degree, Vertex[] ref) {
		for (int index = 0; index < poly.getVertices().length; ++index) {
			if (degree[poly.getVertices()[index]] != Vertex.NORMAL)
				continue;

			Vertex target = geometry.get(poly.getVertices()[index]);

			for (int i = 0; i < poly.getVertices().length; ++i) {
				Vertex current = ref[poly.getVertices()[i]];
				if (i == index) {
					target.setX(		target.getX() + TET_WEIGHT_1*current.getX() 		);
					target.setY(		target.getY() + TET_WEIGHT_1*current.getY() 		);
					target.setZ(		target.getZ() + TET_WEIGHT_1*current.getZ() 		);
				}
				else {
					target.setX(		target.getX() + TET_WEIGHT_2*current.getX() 		);
					target.setY(		target.getY() + TET_WEIGHT_2*current.getY() 		);
					target.setZ(		target.getZ() + TET_WEIGHT_2*current.getZ() 		);
				}	
			}

			valence[poly.getVertices()[index]]++;
		}
	}
	private void smoothOctahedron(Octahedron poly, int[] valence, short[] degree, Vertex[] ref) {
		for (int index = 0; index < poly.getVertices().length; ++index) {
			if (degree[poly.getVertices()[index]] != Vertex.NORMAL)
				continue;

			int offset = 1;
			if (index%2 == 1)
				offset = -1;

			Vertex target = geometry.get(poly.getVertices()[index]);

			for (int i = 0; i < poly.getVertices().length; ++i) {
				Vertex current = ref[poly.getVertices()[i]];

				if (i == index) {
					target.setX(		target.getX() + OCT_WEIGHT_1*current.getX() 		);
					target.setY(		target.getY() + OCT_WEIGHT_1*current.getY() 		);
					target.setZ(		target.getZ() + OCT_WEIGHT_1*current.getZ() 			);
				}
				else if (i == index + offset) {
					target.setX(		target.getX() + OCT_WEIGHT_3*current.getX() 		);
					target.setY(		target.getY() + OCT_WEIGHT_3*current.getY() 		);
					target.setZ(		target.getZ() + OCT_WEIGHT_3*current.getZ() 			);
				}
				else {
					target.setX(		target.getX() + OCT_WEIGHT_2*current.getX() 		);
					target.setY(		target.getY() + OCT_WEIGHT_2*current.getY() 		);
					target.setZ(		target.getZ() + OCT_WEIGHT_2*current.getZ() 			);
				}	
			}

			valence[poly.getVertices()[index]]++;

		}
	}

	private short[] initDegrees() {
		short[] degrees = new short[geometry.size()];

		for (int i = 0; i < geometry.size(); ++i)
			degrees[i] = Vertex.NORMAL;

		for (Face f : topology.getFaces()) 
			for (int index : f.getPoints()) 
				degrees[index] = Vertex.CREASE_FACE;

		for (Edge e : topology.getEdges()) 
			for (int index : e.getVertices()) 
				degrees[index] = Vertex.CREASE_EDGE;

		for (Integer index : topology.getPoints())
			degrees[index] = Vertex.CREASE_POINT;

		return degrees;
	}
	private int[] initValences() {
		int[] valence = new int[geometry.getPoints().size()];
		for (int i = 0; i < valence.length; ++i)
			valence[i] = 0;
		return valence;
	}

	public void pcaAlignToLandmarks(List<Landmark> landmarks) {
		
//		// TEST
//		Matrix testRot = new Matrix(3,3);
//		testRot.set(0, 0, 1.0); testRot.set(0, 1, 0.0); testRot.set(0, 2, 0.0);
//		testRot.set(1, 0, 0.0); testRot.set(1, 1, Math.cos(0.2)); testRot.set(1, 2, -Math.sin(0.2));
//		testRot.set(2, 0, 0.0); testRot.set(2, 1, Math.sin(0.2)); testRot.set(2, 2, Math.cos(0.2));
//		
//		// TEST rotate
//		for (int i = 0; i < geometry.size(); ++i) {
//			double[][] d = {geometry.get(i).getXYZDouble()};
//			Matrix temp = new Matrix(d);
//			temp = temp.transpose();
//			Matrix rot = testRot.times(temp);
//			Vertex v = new Vertex( (float)rot.get(0, 0), (float)rot.get(1, 0), (float)rot.get(2, 0));
//			geometry.set(i, v);
//		}
//		
//		// TEST translate
//		for (int i = 0; i < geometry.size(); ++i) {
//			Vector v = new Vector( 1.2f, 1.4f, 1.3f);
//			Vertex result = geometry.get(i).plus(v);
//			geometry.set(i,result);
//		}
		
		Vertex centerM = new Vertex(0.0f,0.0f,0.0f);
		Set<Integer> hashSet = new HashSet<Integer>();
		for (int i = 0; i < topology.getFaces().size(); ++i){
			if (topology.getFaces().get(i).isSurface()) {
				for (int j = 0; j < topology.getFaces().get(i).getPoints().length; ++j) {
					int index = topology.getFaces().get(i).getPoints()[j];
					if (hashSet.add(index)) {
						centerM.setX(centerM.getX()+geometry.get(index).getX());
						centerM.setY(centerM.getY()+geometry.get(index).getY());
						centerM.setZ(centerM.getZ()+geometry.get(index).getZ());
					}
				}
			}
		}
		centerM.setX(centerM.getX()/hashSet.size());
		centerM.setY(centerM.getY()/hashSet.size());
		centerM.setZ(centerM.getZ()/hashSet.size());
		
		
		Matrix mMesh = new Matrix(3,hashSet.size());
		int count = 0;
		for (int i : hashSet) {
			mMesh.set(0, count, geometry.get(i).getX()-centerM.getX());
			mMesh.set(1, count, geometry.get(i).getY()-centerM.getY());
			mMesh.set(2, count, geometry.get(i).getZ()-centerM.getZ());
			count++;
		}
		
		Vertex centerL = new Vertex(0.0f,0.0f,0.0f);
		for (int i = 0; i < landmarks.size(); ++i){
			centerL.setX(centerL.getX()+landmarks.get(i).getLocation().getX());
			centerL.setY(centerL.getY()+landmarks.get(i).getLocation().getY());
			centerL.setZ(centerL.getZ()+landmarks.get(i).getLocation().getZ());
		}
		centerL.setX(centerL.getX()/landmarks.size());
		centerL.setY(centerL.getY()/landmarks.size());
		centerL.setZ(centerL.getZ()/landmarks.size());
		
		Matrix mLandmarks = new Matrix(3,landmarks.size());
		for (int i = 0; i < landmarks.size(); ++i){
			mLandmarks.set(0, i, landmarks.get(i).getLocation().getX()-centerL.getX());
			mLandmarks.set(1, i, landmarks.get(i).getLocation().getY()-centerL.getY());
			mLandmarks.set(2, i, landmarks.get(i).getLocation().getZ()-centerL.getZ());
		}
		
		// align centroids
		for (int i = 0; i < geometry.size(); ++i) {
			geometry.set(i,geometry.get(i).plus(centerL.minus(centerM)));
		}
		
		// TODO: for now just align centroids
		if (true) return;
		
		// align PCA
		@SuppressWarnings("unused")
		Matrix covarienceMesh = mMesh.times(mMesh.transpose());
		Matrix covarienceLandmarks = mLandmarks.times(mLandmarks.transpose());
		
		// get 4 rotation choices
		double[][] inv1 = {{-1.0,0.0,0.0},{0.0, 1.0,0.0},{0.0,0.0, 1.0}};
		double[][] inv2 = {{ 1.0,0.0,0.0},{0.0,-1.0,0.0},{0.0,0.0, 1.0}};
		double[][] inv3 = {{ 1.0,0.0,0.0},{0.0, 1.0,0.0},{0.0,0.0,-1.0}};
		Matrix invert1 = new Matrix(inv1);
		Matrix invert2 = new Matrix(inv2);
		Matrix invert3 = new Matrix(inv3);

		Matrix eigensLandmarks = covarienceLandmarks.eig().getV();
		
		double[] orthogal = {
				eigensLandmarks.get(1,2)*eigensLandmarks.get(2, 1) - eigensLandmarks.get(2, 2)*eigensLandmarks.get(1, 1),
				eigensLandmarks.get(2,2)*eigensLandmarks.get(0, 1) - eigensLandmarks.get(0, 2)*eigensLandmarks.get(2, 1),
				eigensLandmarks.get(0,2)*eigensLandmarks.get(1, 1) - eigensLandmarks.get(1, 2)*eigensLandmarks.get(0, 1)
				};
		
		// pick the rotation causing the smallest angle change
		Matrix eigensMesh = covarienceMesh.eig().getV();
		
		// TEST Make sure right handed
		double[] orthogal2 = {
				eigensMesh.get(1,2)*eigensMesh.get(2, 1) - eigensMesh.get(2, 2)*eigensMesh.get(1, 1),
				eigensMesh.get(2,2)*eigensMesh.get(0, 1) - eigensMesh.get(0, 2)*eigensMesh.get(2, 1),
				eigensMesh.get(0,2)*eigensMesh.get(1, 1) - eigensMesh.get(1, 2)*eigensMesh.get(0, 1)
				};
		
		Matrix Rotation = eigensLandmarks.times(eigensMesh.transpose());
		float trace = getTrace(Rotation);
		float angle = (float) java.lang.Math.abs(java.lang.Math.acos((trace-1.0)/2.0));

		
		Matrix eigensMesh2 = eigensMesh.times(invert1).times(invert2);
		Matrix temp = eigensLandmarks.times(eigensMesh2.transpose());
		trace = getTrace(temp);
		float angle2 = (float) java.lang.Math.abs(java.lang.Math.acos((trace-1.0)/2.0));
		if (angle2 < angle) {
			Rotation = temp;
		}
		
		Matrix eigensMesh3 = eigensMesh.times(invert2).times(invert3);
		temp = eigensLandmarks.times(eigensMesh3.transpose());
		trace = getTrace(temp);
		float angle3 = (float) java.lang.Math.abs(java.lang.Math.acos((trace-1.0)/2.0));
		if (angle3 < angle) {
			Rotation = temp;
		}
		
		Matrix eigensMesh4 = eigensMesh.times(invert1).times(invert3);
		temp = eigensLandmarks.times(eigensMesh4.transpose());
		trace = getTrace(temp);
		float angle4 = (float) java.lang.Math.abs(java.lang.Math.acos((trace-1.0)/2.0));
		if (angle4 < angle) {
			Rotation = temp;
		}
				
		for (int i = 0; i < geometry.size(); ++i) {
			double[][] d = {geometry.get(i).minus(centerL).getXYZDouble()};
			Matrix rot = Rotation.times((new Matrix(d)).transpose());
			Vector v = new Vector( (float)rot.get(0, 0), (float)rot.get(1, 0), (float)rot.get(2, 0));
			Vertex result = centerL.plus(v);
			geometry.set(i,result);
		}
		
	}
	
//	public void icpAlignToLandmarks(List<Landmark> landmarks) {
//		
//		HashMap<Integer,Integer> svd = new HashMap<Integer,Integer>();
//		for (int i = 0; i < topology.getFaces().size(); ++i){
//			if (topology.getFaces().get(i).isSurface()) {
//				for (int j = 0; j < topology.getFaces().get(i).getPoints().length; ++j) {
//					
//					int index = topology.getFaces().get(i).getPoints()[j];
//
//					if (!svd.containsKey(index)) {
//						
//						boolean matchfound = false;
//						float mindistance = Float.MAX_VALUE;
//						Integer minindex = -1;
//						
//						for (int k = 0; k < landmarks.size(); ++k) {
//							
//							if (landmarks.get(k).equalMaterials(topology.getFaces().get(i))){
//								
//							}
//							
//							
//						}
//						
//						
//						
//					}
//					
//					
//				}
//			}
//		}
//		
//	}

	private float getTrace(Matrix rotation) {
		return (float) (rotation.get(0, 0) + rotation.get(1, 1) + rotation.get(2, 2));
	}

}
