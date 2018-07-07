package app.tools.cross_sections;

import app.tools.math.Vertex;
import app.tools.topology.Polyhedron;

/** CrossSection.java
 * <br>
 *  Represents an arbitrary cross-section of a single polyhedron. 
 * Generally, this will be a convex polygon. However, cases where the
 * cross-section is concave (or has a hole) must also be handled, by 
 * storing a second (optional) contour.
 * 
 * @author Don McCurdy
 * @date August 5, 2010
 *
 */
public class CrossSection {
	private Vertex[] contour1, contour2;
	private Boolean[] tags1, tags2;
	/** Identifies the polyhedron that generated this cross-section. Not needed once debugging is complete.*/
	public Polyhedron tag; //TODO If all cross-sections are being generated correctly, this tag may be removed.
	
	/** Instantiates a cross-section with two contours (lists of vertices)
	 * @param contourA
	 * @param contourB
	 */
	public CrossSection(Vertex[] contourA, Boolean[] tagsA, Vertex[] contourB, Boolean[] tagsB) {
		contour1 = contourA;
		contour2 = contourB;
		tags1 = tagsA;
		tags2 = tagsB;
	}
	
	/** Access either of the two (indexed by 0 or 1 ) contours.
	 * @param index
	 * @return contour
	 */
	public Vertex[] getContour(int index) {
		if (index == 0)
			return contour1;
		else if (index == 1)
			return contour2;
		else
			throw new ArrayIndexOutOfBoundsException("There are only two contours in any given cross section.");
	}
	
	/** Access the tag array (crease/non-crease) corresponding to either contour, indexed 0 or 1.
	 * @param index
	 * @return creaseTags
	 */
	public Boolean[] getCreaseTags(int index) {
		if (index == 0)
			return tags1;
		else if (index == 1)
			return tags2;
		else
			throw new ArrayIndexOutOfBoundsException("There are only two contours in any given cross section.");
	}
	
}
