package app.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import app.tools.MeshLoader;
import app.tools.RegistrationUtils;
import app.tools.math.Vertex;
import app.tools.structure.Mesh;
import app.tools.topology.Landmark;

public class RegistrationUtilsTests {
	
	private Mesh testTetrahedron;
	private List<Landmark> testLandmarks;
	private List<Landmark> singlePointDiag;

	@Before
	public void setUp() throws FileNotFoundException, IOException {
		testTetrahedron = MeshLoader.loadMesh(new File("test_data/testTetrahedron.pol"));
		testLandmarks = MeshLoader.loadLandmarks(new File("test_data/box.landmarks"));
		singlePointDiag = new ArrayList<Landmark>();
		Landmark singleDiag = new Landmark(new Vertex(1,1,1));
		singleDiag.addMaterial(-1);
		singleDiag.addMaterial(-2);
		singlePointDiag.add(singleDiag);
	}
	
	@After
	public void tearDown() {
		RegistrationUtils.resetRegistrationSession();
	}
	
	@Test
	public void testStartNewRegistrationSession() {
		boolean success = RegistrationUtils.startRegistrationSession(testTetrahedron, 1);
		assertTrue(success);
		success = RegistrationUtils.startRegistrationSession(testTetrahedron, 1);
		assertTrue(success);
		Mesh result = RegistrationUtils.resetRegistrationSession();
		assertTrue(result.getCentroid().getX() == testTetrahedron.getCentroid().getX());
	}
	
	@Test
	public void testAddFitting() {
		RegistrationUtils.startRegistrationSession(testTetrahedron, 1);
		Mesh result = RegistrationUtils.addLandmarksToFit(new ArrayList<Landmark>(), 1.0, 1.0, 1);
		assertTrue(result != null);
		assertTrue(result.getCentroid().getX() == testTetrahedron.getCentroid().getX());
		result = RegistrationUtils.addLandmarksToFit(testLandmarks, 1.0, 1.0, 1);
		assertTrue(result != null);
		assertTrue(result.getCentroid().getX() != testTetrahedron.getCentroid().getX());
		float centroidX = result.getCentroid().getX();
		result = RegistrationUtils.addLandmarksToFit(singlePointDiag, 1.0, 1.0, 1);
		assertTrue(result.getCentroid().getX() != centroidX);
	}
	
	@Test
	public void testSaveRegistrationSession() {
		RegistrationUtils.startRegistrationSession(testTetrahedron, 1);
		Mesh result = RegistrationUtils.addLandmarksToFit(new ArrayList<Landmark>(), 1.0, 1.0, 1);
		assertTrue(result.getCentroid().getX() == testTetrahedron.getCentroid().getX());
		Mesh result2 = RegistrationUtils.saveRegistrationSession();
		assertTrue(result.getCentroid().getX() == result2.getCentroid().getX());
		result2 = RegistrationUtils.addLandmarksToFit(testLandmarks, 1.0, 1.0, 1);
		float centroidX = result2.getCentroid().getX();
		assertTrue(result.getCentroid().getX() != result2.getCentroid().getX());
		result2 = RegistrationUtils.resetRegistrationSession();
		assertTrue(result.getCentroid().getX() == result2.getCentroid().getX());
		result2 = RegistrationUtils.addLandmarksToFit(testLandmarks, 1.0, 1.0, 1);
		assertTrue(result != null);
		assertTrue(result.getCentroid().getX() != result2.getCentroid().getX());
		assertTrue(result2.getCentroid().getX() == centroidX);
	}

}
