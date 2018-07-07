package edu.wustl.taoju.fittingtool.tools;

import java.io.BufferedReader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import app.gui.ImageStreamer;
import app.tools.math.Vector;
import app.tools.math.Vertex;
import app.tools.structure.*;
import app.tools.topology.*;

/**
 * Loader.java
 * <br><br>
 * Provides static methods to load meshes and to parse crease topology.
 * <br><br>
 * Based on the port of NeHe's Lesson 10 to JOGL, done by Nicholas Campbell (campbelln@hartwick.edu).
 * I had access to version 1.00, written 20 December, 2003.
 * 
 * @author Don McCurdy
 * @date June 2010
 *
 */
public abstract class MeshLoader {

	//******************************************************************************************************************************
	//
	//			LOADING MESHES
	//
	//******************************************************************************************************************************
	
	/** Load the mesh with the given fileName. Default path is "app/data/" (this path need not be specified)
	 * @param fileName
	 * @return mesh
	 * @throws FileNotFoundException 
	 */
	
	public static Mesh loadMesh(File stream) throws FileNotFoundException {
		InputStream istream = new FileInputStream(stream);
		BufferedReader bR = new BufferedReader(new InputStreamReader(istream));
		try { while(!bR.ready()) {	try { Thread.sleep(200); } catch (InterruptedException ie) {} }	} catch (IOException ie) {} 

		ArrayList<Vertex> proGeometry = new ArrayList<Vertex>();
		ArrayList<Polyhedron> proPolys = new ArrayList<Polyhedron>();

		readFile(bR, proGeometry, proPolys);											//Parse the file for the vertices and polyhedra
		try {  bR.close();  } catch (IOException e) {e.printStackTrace();	}

		Geometry g = new Geometry(proGeometry);

		ArrayList<Face> proFaces 	= loadCreaseFaces(g, proPolys);		//Identify crease structures
		ArrayList<Edge>proEdges	= loadCreaseEdges(proFaces);
		ArrayList<Integer>proPoints	= loadCreasePoints(proEdges);

		Topology t = new Topology(proPoints, proEdges, proFaces, proPolys);

		Log.p("Loaded: " + proGeometry.size() + " points, " + proPolys.size() + " polyhedra, " + proFaces.size() + " crease faces, " + proEdges.size() + " crease edges, and " + proPoints.size() + " crease points. ");

		return new Mesh (t, g);
	}
	
	public static List<Landmark> loadLandmarks(File file) throws FileNotFoundException, IOException {
		InputStream istream = new FileInputStream(file.getCanonicalPath());
		BufferedReader bR = new BufferedReader(new InputStreamReader(istream));
		
		String local = bR.readLine();
		if (!local.equals("landmarks")) {
			throw new IOException();
		}
		
		List<Landmark> result = new ArrayList<Landmark>();
		
		float xcoord, ycoord, zcoord;
		for (String line = bR.readLine(); line != null; line = bR.readLine()) {
			String[] parts = line.trim().split("\\s+");
			if (parts.length < 3) {
				continue;
			}
			xcoord = Float.parseFloat(parts[0]);
			ycoord = Float.parseFloat(parts[1]);
			zcoord = Float.parseFloat(parts[2]);
			Landmark lm = new Landmark(new Vertex(xcoord,ycoord,zcoord));
			
			for (int i = 3; i < parts.length; ++i){
				lm.addMaterial(Integer.parseInt(parts[i]));
			}
			
			result.add(lm);
		}
		
		return result;
	}
	
	public static void writeLandmarks(List<Landmark> landmarks, File file) throws IOException {
		if (file.exists()) {
			file.delete();
		}
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		bw.write("landmarks");
		bw.newLine();
		for (Landmark lm : landmarks){
			String loc = lm.getLocation().getX() + " " + lm.getLocation().getY() + " " + lm.getLocation().getZ();
			String mat = "";
			for (int i = 0; i < lm.getMaterials().size(); ++i){
				mat += " " + lm.getMaterials().get(i);
			}
			bw.write(loc + mat);
			bw.newLine();
		}
		bw.close();
	}
	
	public static void appendLandmarks(List<Landmark> landmarks, File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (Landmark lm : landmarks){
			String loc = lm.getLocation().getX() + " " + lm.getLocation().getY() + " " + lm.getLocation().getZ();
			String mat = "";
			for (int i = 0; i < lm.getMaterials().size(); ++i){
				mat += " " + lm.getMaterials().get(i);
			}
			bw.write(loc + mat);
			bw.newLine();
		}
		bw.close();
	}
	
	public static void writeMesh(Mesh savemesh, File file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(file.getCanonicalPath())));
		bw.write("poly");
		bw.newLine();
		String num = savemesh.getGeometry().size() + " " + savemesh.getTopology().getPolyhedra().size();
		bw.write(num);
		bw.newLine();
		for (Vertex v : savemesh.getGeometry().getPoints()){
			String loc = v.getX() + " " + v.getY() + " " + v.getZ();
			bw.write(loc);
			bw.newLine();
		}
		for (Polyhedron p : savemesh.getTopology().getPolyhedra()){
			String loc = "4 ";
			for (Integer i : p.getVertices()) {
				loc += i + " ";
			}
			loc += p.getMaterial();
			bw.write(loc);
			bw.newLine();
		}
		bw.close();
	}
	
	public static ImageStreamer loadImageStreamer(String directory, String filename) throws IOException {
		InputStream istream = new FileInputStream(directory + "/" + filename);
		BufferedReader bR = new BufferedReader(new InputStreamReader(istream));
		
		String local = bR.readLine();
		if (!local.equals("imagestack")) {
			throw new IOException();
		}
		
		int sizestart = 0;
		int sizex = 0;
		int sizey = 0;
		int sizez = 0;
		String ext = "png";
		String base = "default";
		int scale = 100;
		
		for (String line = bR.readLine(); line != null; line = bR.readLine()) {
			if (line.startsWith("#")) {
				if (line.contains("start")){
					sizestart = Integer.parseInt(line.split("=")[1].trim());
				}
				else if (line.contains("sizex")){
					sizex = Integer.parseInt(line.split("=")[1].trim());
				}
				else if (line.contains("sizey")){
					sizey = Integer.parseInt(line.split("=")[1].trim());
				}
				else if (line.contains("sizez")){
					sizez = Integer.parseInt(line.split("=")[1].trim());
				}
				else if (line.contains("base")){
					base = line.split("=")[1].trim();
				}
				else if (line.contains("scale")){
					scale = Integer.parseInt(line.split("=")[1].trim());
				}
				else if (line.contains("ext")){
					ext = line.split("=")[1].trim();
				}
			}
			else {
				return new ImageStreamer(sizestart,sizex,sizey,sizez,base,ext,scale,directory);
			}
		}
		
		return new ImageStreamer(sizestart,sizex,sizey,sizez,base,ext,scale,directory);
	}
	
	/** Parses a .POL file for vertices and polyhedra, feeding the results into the provided ArrayLists.
	 * @param bufferedReader
	 * @param arrayForVertices
	 * @param arrayForPolyhedra
	 */
	private static void readFile(BufferedReader bR, ArrayList<Vertex> vertices, ArrayList<Polyhedron> polyhedra) {
		int numMaterials;

		try {	
			ArrayList<String> strings = new ArrayList<String>();
			for (String line = bR.readLine(); line != null; line = bR.readLine())
				if (line.length()> 2) 
					strings.add(line.trim());

			StringTokenizer sT = new StringTokenizer(strings.get(1));			//Skip first line, which should just read 'poly.' Second line tells how many points/polyhedra				
			int numVert = Integer.parseInt(sT.nextToken());
			int numPoly = Integer.parseInt(sT.nextToken());

			float scale = 1.0f;		//No touchey.
			for (int i = 0; i < numVert; ++i) {
				sT = new StringTokenizer(strings.get(i+2)); //Offset by 2, skipping first two lines.
				vertices.add(new Vertex(scale*Float.parseFloat(sT.nextToken()), scale*Float.parseFloat(sT.nextToken()),scale*Float.parseFloat(sT.nextToken())));
			}

			HashSet<Integer> knownMaterials = new HashSet<Integer>();

			for(int i = 0; i < numPoly; ++i) {
				sT = new StringTokenizer(strings.get(2+numVert + i)); //Offset to where polyhedra begin.
				switch (Integer.parseInt(sT.nextToken()))	{			//Number of vertices to expect

				case 4 :
					Integer fa = Integer.parseInt(sT.nextToken());	//vertex 1
					Integer fb = Integer.parseInt(sT.nextToken());	//vertex 2
					Integer fc = Integer.parseInt(sT.nextToken());	//vertex 3
					Integer fd = Integer.parseInt(sT.nextToken());	//vertex 4
					int[] fourVerts = new int[] {fa,fb,fc,fd};
					int tMat = Integer.parseInt(sT.nextToken());	//material	
					knownMaterials.add(tMat);
					polyhedra.add(new Tetrahedron(fourVerts, tMat));
					break;

				case 6 :
					Integer sa = Integer.parseInt(sT.nextToken());	//vertex 1
					Integer sb = Integer.parseInt(sT.nextToken());	//vertex 2
					Integer sc = Integer.parseInt(sT.nextToken());	//vertex 3
					Integer sd = Integer.parseInt(sT.nextToken());	//vertex 4
					Integer se = Integer.parseInt(sT.nextToken());	//vertex 5
					Integer sf = Integer.parseInt(sT.nextToken());	//vertex 6
					int[] sixVerts = new int[] {sa,sb,sc,sd,se,sf};
					int oMat = Integer.parseInt(sT.nextToken())	;		//material
					knownMaterials.add(oMat);
					polyhedra.add(new Octahedron(sixVerts, oMat));
					break;

				default :
					throw new InvalidParameterException("This application only supports tetrahedral and octahedral meshes.");
				}
			}
			numMaterials = knownMaterials.size();
			/*** In case memory management isn't quite doing it ... */
			strings.clear();
			knownMaterials.clear();
			/**************/
			Log.p("Finished reading: " + vertices.size() + " vertices, " + polyhedra.size() + " polyhedra, and " + numMaterials + " materials found.");
		} catch (IOException iOE) { Log.p("Error loading file: " + iOE); }
	}
	
	//******************************************************************************************************************************
	//
	//			IDENTIFYING CREASE TOPOLOGY
	//
	//******************************************************************************************************************************
	
	/** Detects all crease faces and returns them in an array. 
	 * @param geometry
	 * @param polyhedra
	 * @return crease_faces
	 */
	private static ArrayList<Face> loadCreaseFaces(Geometry g, ArrayList<Polyhedron> polys) {
		ArrayList<Face> creaseFaces = new ArrayList<Face>();
		HashMap<Long, CreaseFaceStruct> hash = new HashMap<Long, CreaseFaceStruct>();

		for (Polyhedron p : polys) {
			int[][] rawFaces = new int[p.getNumFaces()][3];
			p.getFaceData(g, rawFaces, new Vector[p.getNumFaces()]);

			for (int i = 0; i < rawFaces.length; ++i) {
				Face f = new Face (rawFaces[i], p.getMaterial());
				if (hash.containsKey(f.longHashCode())) {
					hash.get(f.longHashCode()).put(p,i).f.setMaterial(p.getMaterial(), false);
				} else {
					hash.put(f.longHashCode(), new CreaseFaceStruct().put(f).put(p,i));
				}
			}	
		}

		for (Long l : hash.keySet()) {
			CreaseFaceStruct s = hash.get(l);
			if (s.f.getMaterial(true) != s.f.getMaterial(false)) {
				creaseFaces.add(s.f);
				s.p1.addCreaseFace(s.i1);
				if (s.p2 != null) s.p2.addCreaseFace(s.i2);
			}
		}

		return creaseFaces;
	}

	/** Detects all crease edges and returns them in an array. 
	 * 
	 * @param crease_faces
	 * @return crease_edges
	 */
	private static ArrayList<Edge> loadCreaseEdges(ArrayList<Face> faces) {
		ArrayList<Edge> creaseEdges = new ArrayList<Edge>();
		HashMap<Long, Edge> hash = new HashMap<Long, Edge>();

		for (Face f : faces) {
			int[][] rawEdges = f.getEdges();

			for (int i = 0; i < rawEdges.length; ++i) {
				Edge e = new Edge ( new int[] {rawEdges[i][0], rawEdges[i][1]});
				if(!hash.containsKey(e.longHashCode()))
					hash.put(e.longHashCode(), e);

				hash.get(e.longHashCode()).addMaterial(f.getMaterial(true));
				hash.get(e.longHashCode()).addMaterial(f.getMaterial(false));
			}
		}

		for (Long l : hash.keySet()) {
			Edge e = hash.get(l);
			if (e.getMaterials().size() > 2) 
				creaseEdges.add(e);
		}

		return creaseEdges;
	}

	/** Detects all crease vertices and returns them in an array. 
	 * 
	 * @param geometry
	 * @param crease_edges
	 * @return crease_points
	 */
	private static ArrayList<Integer> loadCreasePoints(ArrayList<Edge> edges) {
		ArrayList<Integer> creasePoints = new ArrayList<Integer> ();
		HashMap<Integer, Short> hash = new HashMap<Integer, Short>();	//Read:   <index, # edges containing index>

		for (Edge e : edges)
			for (int i = 0; i < e.getVertices().length; ++i) {
				int key = e.getVertices()[i];
				if (!hash.containsKey(key))
					hash.put(key, (short)1);
				else
					hash.put(key, 		(short)		(hash.get(key) 	+		1		));
			}

		for (Integer i : hash.keySet()) {
			if (hash.get(i) > 2 )
				creasePoints.add(i);
		}

		return creasePoints;
	}

}

class CreaseFaceStruct {
	protected Polyhedron p1, p2;
	protected int i1, i2;
	protected Face f;
	
	public CreaseFaceStruct put(Polyhedron p, int i) {
		if (p1 == null) {
			p1 = p;
			i1 = i;
		}
		else if (p2 == null) {
			p2 = p;
			i2 = i;
		}
		else
			throw new ArrayIndexOutOfBoundsException ("No room for polyhedron in this struct.");
		return this;
	}
	
	public CreaseFaceStruct put(Face f) {
		if (this.f == null)
			this.f = f;
		else
			throw new ArrayIndexOutOfBoundsException("No room for face in this struct.");
		return this;
	}
}