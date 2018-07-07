package edu.wustl.taoju.fittingtool.tools;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;

/**
 * Log.java
 * <br>
 * Displays messages from various parts of the program. At present, messages are printed to console and the instance's JPanel.
 * <br>
 * Implements Singleton design pattern.
 * 
 * @author Don McCurdy
 * @date June 2010
 *
 */
public class Log implements ActionListener {
	/**Singleton instance*/
	private static Log log = new Log();

	private JPanel logPanel, statusPanel;
	private JTextArea logField;
	private JLabel statusLabel;
	public static String glVersion = null;

	private Timer t = new Timer(3000, log);

	/** Initializes an instance of the Log class and its JPanel interface. */
	public Log() {
		t.addActionListener(getInstance());

		//LOG PANEL

		logPanel = new JPanel();
		logField = new JTextArea(5,0);

		logField.setEditable(false);

		JScrollPane scrollPane = new JScrollPane(logField);

		JButton printInfo = new JButton("Debugging Info");	printInfo.addActionListener(this);
		JPanel banner = new JPanel(); banner.setLayout(new BoxLayout(banner, BoxLayout.X_AXIS));	banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
		banner.add(new JLabel("<html><i>Applet Log:</i></html>")); 		banner.add(Box.createGlue());		banner.add(printInfo);

		logPanel.setLayout(new BoxLayout(logPanel, BoxLayout.Y_AXIS));
		logPanel.add(banner);
		logPanel.add(scrollPane);
		logPanel.setVisible(true);

		scrollPane.setAutoscrolls(true);

		//STATUS PANEL
		statusPanel = new JPanel();
		statusPanel.setBorder(new BevelBorder(BevelBorder.LOWERED));
		statusPanel.setPreferredSize(new Dimension(150,20));
		statusPanel.setMaximumSize(new Dimension(150,20));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));

		statusLabel = new JLabel(" ");
		statusPanel.add(Box.createGlue());
		statusPanel.add(statusLabel);
		statusPanel.add(Box.createGlue());

	}

	public static void setStatus(String s) {
		if (s.equals("")) s = " "; //status box gets deformed if there's literally nothing in it.
		getInstance().statusLabel.setText(s);
	}

	public static void setStatusLoading(float progress) {
		if (progress > 1f || progress < 0f)
			Log.p("Invalid request.");
		else {
			String s = " ";
			for (float i = 0f; i < progress; i += 1f/10f) 
				s+="|";
			if (! s.equals(getStatus()))
				setStatus(s.trim());
		}
	}

	public static String getStatus() {
		return getInstance().statusLabel.getText();
	}

	/** If passed "true," starts a clock and prints memory usage on each tick. 
	 * @param verbose
	 */
	public static void printMem(boolean verbose) {
		if (verbose)  {
			getInstance().t.start();
		}
		else {
			getInstance().t.stop();
		}
	}

	/** Prints the given string to the Java console, as well as the instance's JPanel display.
	 * 
	 * @param string
	 */
	public static void p(String s) {
		System.out.println(":: " + s);
		getInstance().logField.append("\n"+s);
		getInstance().logField.setCaretPosition(getInstance().logField.getText().length()-1);
	}

	/** Prints a divider with the given string as a label.
	 * 
	 * @param string
	 */
	public static void div(String s) {
		Log.p(":::::::::::::::::::::::::::" + s + ":::::::::::::::::::::::::::::::::");
	}

	/** Prints a divider. */
	public static void div() {
		div("");
	}

	/** Returns the (single) instance of the Log class. 
	 * 
	 * @return log
	 */
	private static Log getInstance() {
		return log;
	}

	/** Prints the available system info for debugging, etc.  */
	public static void printSystemInfo() {
		String keys [] =
		{
				"java.vendor", "java.vendor.url",
				"java.version", "java.class.version",
				"os.name", "os.arch", "os.version",
				/*"file.separator", "path.separator",
				"line.separator",*/ "browser",
				"browser.vendor", "browser.version"
		};
		for (int i = 0; i < keys.length; i++)
		{
			try 
			{
				String key = keys[i];
				String value = System.getProperty (key);
				Log.p(key + ": " + value);
			}
			catch (SecurityException see) { 
				see.printStackTrace();
				Log.p("A security exception has been encountered while trying to access debugging information. Aborting.");
			}
		}
		printJOGLInfo();
		Log.p("Applet's current RAM use is: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000000) + "MB of " + (Runtime.getRuntime().maxMemory()/1000000)+ "MB available.");	

	}

	public static void printJOGLInfo() {
		try
		{
			Package p = Class.forName("javax.media.opengl.GL").getPackage();
			if (p == null)
				System.out.println("Cannot find JOGL package.");
			else {
				Log.p("JOGL version: " + p.getImplementationVersion());
				if (glVersion != null)  Log.p("GL Version: " + glVersion);
				else Log.p("GL Version: indeterminate");
			}
		}
		catch(Exception ex)	{	ex.printStackTrace();   }
	}

	/** Listens for Log events */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof JButton) 
			printSystemInfo();
		else
			Log.p("Applet's current RAM use is: " + ((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1000000) + "MB of " + (Runtime.getRuntime().maxMemory()/1000000)+ "MB available.");	
	}

	/** Returns the panel to which log messages are printed.
	 * 
	 * @return logPanel
	 */
	public static JPanel getLogPanel() {
		return getInstance().logPanel;
	}

	public static JPanel getStatusPanel() {
		return getInstance().statusPanel;
	}


}
