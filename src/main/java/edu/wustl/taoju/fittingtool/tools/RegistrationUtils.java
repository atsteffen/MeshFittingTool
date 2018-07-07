package edu.wustl.taoju.fittingtool.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import app.tools.FileUtils;
import app.tools.MeshLoader;
import app.tools.OSValidator;
import app.tools.structure.Mesh;
import app.tools.topology.Landmark;

/**
 * Wrapper class for external registration executable. Static methods are used to begin and access a registration session, which passes
 * landmark and mesh data to files which are in turn used to build and solve the linear system of equations to fit mesh to landmarks.
 */
public class RegistrationUtils {
	
	//-------------------------------------------------------------------
	// Static Variables/Files
	//-------------------------------------------------------------------
	
	private static final File MESH_FILE_BACKUP = new File("src/app/data/temp_mesh_backup.pol");
	private static final File MESH_FILE = new File("src/app/data/temp_mesh.pol");
	private static final File MASK_FILE = new File("src/app/data/temp_mask.pol");
	private static final File LANDMARK_FILE = new File("src/app/data/temp_landmark.pol");
	private static final File ATA_FILE_BACKUP = new File("src/app/data/temp_ata_backup.pol");
	private static final File ATB_FILE_BACKUP = new File("src/app/data/temp_atb_backup.pol");
	private static final File ATA_FILE = new File("src/app/data/temp_ata.pol");
	private static final File ATB_FILE = new File("src/app/data/temp_atb.pol");
	
	private static Integer subdivisionLevel;
	private static boolean precomputed;
	
	//-------------------------------------------------------------------
	// Public Methods
	//-------------------------------------------------------------------
	
	/**
	 * Initialize a new global Registration session. All previous session info is lost
	 * @param model Mesh state at the beginning of this session
	 * @param landmarks Initial added to fitting matrices, can be empty but not null
	 * @param subLevel Set the subdivision level for this session
	 * @return True if the session can create and write to the static files, False if any errors occur
	 */
	public static boolean startRegistrationSession(Mesh model, Integer subLevel) {
		try {
			precomputed = false;
			subdivisionLevel = subLevel;
			clearFiles();
			MeshLoader.writeMesh(model, MESH_FILE_BACKUP);
			FileUtils.copyfile(MESH_FILE_BACKUP,MESH_FILE);
			subdivideMask();
		} catch (IOException e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Revert to last save point, resetting the mesh and all matrices.
	 * @return Mesh as last save point, or <b>null</b> if errors occur with file IO
	 */
	public static Mesh resetRegistrationSession() {
		FileUtils.copyfile(MESH_FILE_BACKUP,MESH_FILE);
		if (ATA_FILE_BACKUP.exists()) {
			FileUtils.copyfile(ATA_FILE_BACKUP,ATA_FILE);
		}
		if (ATB_FILE_BACKUP.exists()) {
			FileUtils.copyfile(ATB_FILE_BACKUP,ATB_FILE);
		}
		try {
			return MeshLoader.loadMesh(MESH_FILE_BACKUP);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Progress the save point for iterative registration to the current mesh. After calling this method previous registration
	 * steps cannot be undone.
	 * @return Current mesh save point, or <b>null</b> if there are problems with file IO
	 */
	public static Mesh saveRegistrationSession() {
		FileUtils.copyfile(MESH_FILE,MESH_FILE_BACKUP);
		if (ATA_FILE.exists()) {
			FileUtils.copyfile(ATA_FILE,ATA_FILE_BACKUP);
		}
		if (ATB_FILE.exists()) {
			FileUtils.copyfile(ATB_FILE,ATB_FILE_BACKUP);
		}
		try {
			return MeshLoader.loadMesh(MESH_FILE_BACKUP);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	/**
	 * Add additional landmarks to the fitting matrices and iteratively solve those matrices
	 * @param landmarks New landmarks to add
	 * @param fit Fitting weight
	 * @param deform Deformation weight
	 * @param iters Iterations of matrix solves
	 * @return Resultant mesh is return, or null returned if errors occur in the IO
	 */
	public static Mesh addLandmarksToFit(List<Landmark> landmarks, Double fit, Double deform, Integer iters) {

		if (!precomputed) {
			try {
				if (landmarks.isEmpty()) {
					return MeshLoader.loadMesh(MESH_FILE);
				} else {
					precomputeMatrices(landmarks, fit, deform, iters);
					FileUtils.copyfile(ATA_FILE_BACKUP,ATA_FILE);
					FileUtils.copyfile(ATB_FILE_BACKUP,ATB_FILE);
					solveRegistration();
					iters--;
					precomputed = true;
				}
			} catch (IOException e) {
				return null;
			}
		} else {
			try {
				if (landmarks.isEmpty()) {
					return MeshLoader.loadMesh(MESH_FILE);
				} else {
					MeshLoader.writeLandmarks(landmarks, LANDMARK_FILE);
				}
			} catch (IOException e) {
				return null;
			}
		}
		
		for (int k =0; k < iters; k++) {
			try {
				Runtime rt = Runtime.getRuntime();
				String[] cmd ={"src/app/OSX/Register3D","addfit","-p",MESH_FILE.getCanonicalPath()
						,"-l",LANDMARK_FILE.getCanonicalPath(),"-s",""+subdivisionLevel,"-i",""+1,"-m",MASK_FILE.getCanonicalPath(),
						"-f",""+fit,"-d",""+deform,"-a",ATA_FILE.getCanonicalPath(),"-b",ATB_FILE.getCanonicalPath()};
				if (OSValidator.isWindows()) {
					cmd[0] ="src\\app\\Win32\\registerAll2";
				}
				Process pr = rt.exec(cmd);
				pr.waitFor();
			} catch(Exception e) {
				e.printStackTrace();
			}			
			solveRegistration();
		}	
		try {
			return MeshLoader.loadMesh(MESH_FILE);
		} catch (FileNotFoundException e) {
			return null;
		}
	}
	
	//-------------------------------------------------------------------
	// Helper Methods
	//-------------------------------------------------------------------
	
	/**
	 * Remove all static files
	 */
	private static void clearFiles() {
		precomputed = false;
		MESH_FILE.delete();
		MASK_FILE.delete();
		ATA_FILE.delete();
		ATB_FILE.delete();
		LANDMARK_FILE.delete();		
		MESH_FILE_BACKUP.delete();
		ATA_FILE_BACKUP.delete();
		ATB_FILE_BACKUP.delete();
	}
	
	/**
	 * Create the subdivision mask matrix m
	 */
	private static void subdivideMask() {
		try {
			Runtime rt = Runtime.getRuntime();		
			String[] cmd ={"src/app/OSX/Register3D","submask","-p",MESH_FILE_BACKUP.getCanonicalPath(),
						"-s",""+subdivisionLevel,"-m",MASK_FILE.getCanonicalPath()};
			if (OSValidator.isWindows()) {
				cmd[0] ="src\\app\\Win32\\registerAll2";
			}
			Process pr = rt.exec(cmd);
			pr.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Generate the initial AtA and AtB matrices for the first landmarks added to the fit
	 */
	private static void precomputeMatrices(final List<Landmark> landmarks, Double fit, Double deform, Integer iters) {
		try {
			MeshLoader.writeLandmarks(landmarks, LANDMARK_FILE);		
			Runtime rt = Runtime.getRuntime();
			String[] cmd ={"src/app/OSX/Register3D","precompute","-p",MESH_FILE_BACKUP.getCanonicalPath()
					,"-l",LANDMARK_FILE.getCanonicalPath(),"-s",""+subdivisionLevel,"-i",""+iters,"-m",MASK_FILE.getCanonicalPath(),
					" -f ",""+fit,"-d",""+deform,"-a",ATA_FILE_BACKUP.getCanonicalPath(),"-b",ATB_FILE_BACKUP.getCanonicalPath()};
			if (OSValidator.isWindows()) {
				cmd[0] ="src\\app\\Win32\\registerAll2";
			}			
			Process pr = rt.exec(cmd);
			pr.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Solve the AtA*m = AtB linear system
	 */
	private static void solveRegistration() {	
		try {
			Runtime rt = Runtime.getRuntime();
			String[] cmd ={"src/app/OSX/Register3D","solve","-p",MESH_FILE.getCanonicalPath(),
					"-a",ATA_FILE.getCanonicalPath(),"-b",ATB_FILE.getCanonicalPath(),"-o",MESH_FILE.getCanonicalPath()};
			if (OSValidator.isWindows()) {
				cmd[0] ="src\\app\\Win32\\registerAll2";
			}
			Process pr = rt.exec(cmd);
			pr.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
