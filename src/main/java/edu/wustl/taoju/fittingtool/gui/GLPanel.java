package app.gui;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JPanel;

import com.sun.opengl.util.Animator;

/** GLPanel.java
 * <br>
 * Generic JPanel extension for displaying a GLCanvas using JOGL. 
 * 
 * @author Don McCurdy
 * 
 */
public abstract class GLPanel extends JPanel implements GLEventListener, KeyListener {
	
	//******************************************************************************************************************************
	//
	//			CONSTANTS AND VARIABLES
	//
	//******************************************************************************************************************************
	
    /**Default */
	private static final long serialVersionUID = 1L;
    /** the number of keys that we want to pay attention to */
    protected final int NUM_KEYS = 250;
    /** the array of keys to store whether certain actions should be taken based on their values  */
    protected boolean[] keys = new boolean[NUM_KEYS];
    /** fullscreen or not, true means yes */
    protected boolean fullscreen = false;
	/** a GLCanvas object */
    protected GLCanvas glCanvas;
    /** an Animator object */
    protected Animator animator;
	
	//******************************************************************************************************************************
	//
	//			INITIALIZATION
	//
	//******************************************************************************************************************************
    
    /** Creates a new instance of Lesson10
     * @param dim The Dimension of the Frame by which to view the canvas.
     * @param fscreen A boolean value to set fullscreen or not
     */ 
    protected GLPanel(String progName, GLCapabilities glCapabilities)
    {
        super();
        fullscreen = false;
               
        glCanvas = new GLCanvas(glCapabilities);		 //create a GLCamvas based on the requirements from above
        glCanvas.addGLEventListener(this); 				// add a GLEventListener, which will get called when the canvas is resized or needs to be repainted
        animator = new Animator(glCanvas);
    }

    /**Start the animator and request focus.   */
	public void start() {
		animator.setRunAsFastAsPossible(true);
		animator.start();
		this.getGLCanvas().requestFocus();
	}

	/**Stop the animator, with anything else that might be helpful */
	public void stop() {
		this.getAnimator().stop();
	}
    
	//******************************************************************************************************************************
	//
	//			ACCESSORS
	//
	//******************************************************************************************************************************
	
    /** Called in the beginning of the application to take grab the focus on the
     * monitor of all other apps.
     * @return glCanvas
     */
    public GLCanvas getGLCanvas()
    {
        return glCanvas;
    }
    
    /** Called in the beginning of the application to grab the animator for the
     * canvas
     * @return animator
     */
    public Animator getAnimator()
    {
        return animator;
    }
    
	//******************************************************************************************************************************
	//
	//			KEYLISTENER IMPLEMENTATION
	//
	//******************************************************************************************************************************

    /** Forced by KeyListener; listens for keypresses and
     * sets a value in an array if they are not of
     * KeyEvent.VK_ESCAPE or KeyEvent.VK_F1
     * @param ke The KeyEvent passed from the KeyListener
     */    
    public void keyPressed(KeyEvent ke)
    {
       switch(ke.getKeyCode())
       {
//            case KeyEvent.VK_F1: // Doesn't work in browser. Fixable?
//            {
//                setVisible(false);
//                if (fullscreen)
//                    setSize(800,600);
//                else
//                    setSize(Toolkit.getDefaultToolkit().getScreenSize().getSize());
//                fullscreen = !fullscreen;
//                //reshape();
//                setVisible(true);
//            }
            default :
               if(ke.getKeyCode()<250) // only interested in first 250 key codes, are there more?
                  keys[ke.getKeyCode()]=true;	
               break;
         }
    }
    
    /** Unsets the value in the array for the key pressed.
     * @param ke The KeyEvent passed from the KeyListener
     */    
    public void keyReleased(KeyEvent ke)
    {
        if (ke.getKeyCode() < 250) { keys[ke.getKeyCode()] = false; }
    }
    
    /** ...has no purpose in this class :)
     * @param ke The KeyEvent passed from the KeyListener
     */    
    public void keyTyped(KeyEvent ke) {}
}
