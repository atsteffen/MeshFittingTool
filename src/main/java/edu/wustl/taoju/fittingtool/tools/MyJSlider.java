package edu.wustl.taoju.fittingtool.tools;

import javax.swing.JSlider;

/** MyJSlider.java
 * <br>
 * Extends the JSlider class, providing some extra functionality. Ideally, this class could be 
 * extended to allow the sensitivity of the slider to be adjusted, allowing more precise 
 * control of the cutting plane.
 * 
 * @author Don McCurdy
 *
 */
public class MyJSlider extends JSlider {
	/** Default */ private static final long serialVersionUID = -6402371571087613848L;
	
	/** Initializes the slider, defaulting to the Horizontal orientation. 
	 * 
	 * @param min
	 * @param max
	 * @param init
	 */
	public MyJSlider (int min, int max, int init) {
		this(JSlider.HORIZONTAL, min, max, init);
	}
	/** Initializes the slider
	 * 
	 * @param orientation
	 * @param min
	 * @param max
	 * @param value
	 */
	public MyJSlider (int orientation, int min, int max, int value) {
		super(orientation, min, max, value);
		this.setAutoscrolls(true);
	}

	@SuppressWarnings("unused") private MyJSlider() {}
	@SuppressWarnings("unused") private MyJSlider (int min, int max) {}
	@Override @Deprecated public int getValue() { return super.getValue(); }
	
	/** Returns the value on the slider as a fraction of its maximum value 
	 * 
	 * @return float
	 */
	public float getValuef() {
		return ((float)super.getValue())/this.getMaximum();
	}
	
	/** Sets the value on the slider to the given fraction of its maximum value 
	 * 
	 * @param value
	 */
	public void setValue(float value) {
		super.setValue((int)(value*this.getMaximum()));
	}
	
}
