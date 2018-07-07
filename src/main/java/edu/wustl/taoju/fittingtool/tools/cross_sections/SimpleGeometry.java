package app.tools.cross_sections;

import java.security.InvalidParameterException;
import java.util.Arrays;

import app.tools.math.Vertex;
import app.tools.structure.Geometry;
import app.tools.topology.Octahedron;
import app.tools.topology.Tetrahedron;

/** SimpleGeometry.java
 *
 * Stores wrapper objects for polyhedra/planes/edges, used to calculate cross sections.
 *
 * @author Don McCurdy
 *
 * Note: Global indexing refers to a vertex's index in the Mesh, local indexing to the vertex's index in the Polyhedron.
 * So, an edge might have local indexes between 0 and 5 for an octahedron, or between 0 and 3 for a tetrahedron.
 * The same edge could have global indexes upwards of 10,000, depending on the number of vertices in the Mesh.
 */
public class SimpleGeometry {
	/** Geometry */ private Geometry g;

	public SimpleGeometry (Geometry g2) { g = g2; }

	class SimplePolyhedron {
		public SimpleFace[] faces;

		public SimplePolyhedron (int [] global, short [] creaseFaces) {
			boolean[] crease = new boolean[(global.length == 4) ? 4 : 8];
			for (short i : creaseFaces)
				crease[i] = true;
			
			switch (global.length) {
			case 4:
				faces = new SimpleFace[] {
						new SimpleFace(new int[] {global[0], global[1], global[2]}, new int[]{0,1,2}, crease[0]),
						new SimpleFace(new int[] {global[0], global[2], global[3]}, new int[]{0,2,3}, crease[1]),
						new SimpleFace(new int[] {global[0], global[3], global[1]}, new int[]{0,3,1}, crease[2]),
						new SimpleFace(new int[] {global[1], global[3], global[2]}, new int[]{1,3,2}, crease[3]),
				};				
				break;
			case 6:
				faces = new SimpleFace[] {
						new SimpleFace(new int[] {global[0], global[1], global[2]}, new int[]{0,1,2}, crease[0]),
						new SimpleFace(new int[] {global[0], global[3], global[1]}, new int[]{0,3,1}, crease[1]),
						new SimpleFace(new int[] {global[1], global[5], global[2]}, new int[]{1,5,2}, crease[2]),
						new SimpleFace(new int[] {global[2], global[4], global[0]}, new int[]{2,4,0}, crease[3]),

						new SimpleFace(new int[] {global[3], global[4], global[5]}, new int[]{3,4,5}, crease[4]),
						new SimpleFace(new int[] {global[3], global[0], global[4]}, new int[]{3,0,4}, crease[5]),
						new SimpleFace(new int[] {global[4], global[2], global[5]}, new int[]{4,2,5}, crease[6]),
						new SimpleFace(new int[] {global[5], global[1], global[3]}, new int[]{5,1,3}, crease[7]),
				};
				break;
			default:
				throw new InvalidParameterException("Not a tetrahedron or octahedron in SimplePolyhedron. ?");
			}
		}

		public SimpleFace[] getAdjacentFaces(SimpleEdge edge) {
			switch (faces.length) {
			case 4:		// Find adjacent faces using TETRAHEDRAL adjacency.
				short[] faceIndexesT = Tetrahedron.getAdjacentFaces((short)edge.localIndexes[0], (short)edge.localIndexes[1]);
				return new SimpleFace[] {faces[faceIndexesT[0]], faces[faceIndexesT[1]]};
				
			case 8:		// Find adjacent faces using OCTAHEDRAL adjacency.
				short[] faceIndexesO = Octahedron.getAdjacentFaces((short)edge.localIndexes[0], (short)edge.localIndexes[1]);
				return new SimpleFace[] {faces[faceIndexesO[0]], faces[faceIndexesO[1]]};
				
			default:
				throw new InvalidParameterException("Not a tetrahedron or octahedron in SimplePolyhedron. ?");
			}
		}
	}
	class SimpleFace {
		public SimpleEdge[] edges;
		public boolean isCrease;

		//public SimpleFace (SimpleEdge[] newEdges) { edges  = newEdges; }

		public SimpleFace (int[] global, int [] local, boolean isCrease) {
			edges = new SimpleEdge[] {
					new SimpleEdge (new int[] {global[0],global[1]}, new int[] {local[0],local[1]}),
					new SimpleEdge (new int[] {global[1],global[2]}, new int[] {local[1],local[2]}),
					new SimpleEdge (new int[] {global[2],global[0]}, new int[] {local[2],local[0]})
			};
			this.isCrease = isCrease;
		}
		
		@Override
		public boolean equals(Object o) {
			if ( ! (o instanceof SimpleFace)) 
				return false;
			SimpleFace other = (SimpleFace) o;
			return (this.edges[0].equals(other.edges[0])) 
			&& (this.edges[1].equals(other.edges[1])) 
			&& (this.edges[2].equals(other.edges[2]));
		}
	}
	class SimpleEdge {
		public int[] globalIndexes;
		public int[] localIndexes;

		public SimpleEdge(int[] global, int[] local) {
			globalIndexes = global;
			localIndexes = local;
		}

		public Vertex getVertex(int index) {
			return g.get(globalIndexes[index]);
		}

		@Override
		public boolean equals(Object obj) {
			if (! (obj instanceof SimpleEdge))
				return false;
			else {
				SimpleEdge other = (SimpleEdge) obj;
				int[] thisArray = new int [] {globalIndexes[0], globalIndexes[1]};
				int[] otherArray= new int[] {other.globalIndexes[0], other.globalIndexes[1]};
				Arrays.sort(thisArray);
				Arrays.sort(otherArray);
				return Arrays.equals(thisArray, otherArray);
			}
		}
	}
}


