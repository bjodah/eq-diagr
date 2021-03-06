package predominanceAreaDiagrams;

import lib.common.Util;
import lib.kemi.chem.Chem;
import lib.kemi.graph_lib.DiagrPaintUtility;
import lib.kemi.graph_lib.GraphLib;
import lib.kemi.haltaFall.Factor;
import lib.kemi.haltaFall.HaltaFall;
import lib.kemi.readDataLib.ReadDataLib;
import lib.kemi.readWriteDataFiles.ReadChemSyst;

/** Creates a Predominance Area diagram. <br>
 * This program will read a data file, make some calculations, and
 * create a diagram. The diagram is displayed and stored in a plot file. <br>
 * The data file contains the description of a chemical system (usually
 * an aqueous system) and a description on how the diagram should be drawn:
 * what should be in the axes, concentrations for each chemical
 * component, etc. <br>
 * If the command-line option "-nostop" is given, then no option dialogs will
 * be shown. <br>
 * Output messages and errors are written to <code>System.out</code> and
 * <code>.err</code> (the console) and output is directed to a JTextArea as well.
 * <br>
 * Copyright (C) 2014-2018  I.Puigdomenech
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 *
 * @author Ignasi Puigdomenech */
public class Predom extends javax.swing.JFrame {
    static final String VERS = "2018-Aug-17";
    static final String progName = "PREDOM";
/** variable needed in "main" method */
    private static Predom predomFrame;
/** print debug information? */
    private static final boolean DBG_DEFAULT = false;
/** print debug information? */
    boolean dbg = false;
/** if true the program does not exit after the diagram is drawn (and saved
 * as a plot file), that is, the diagram remains visible for the user.
 * If false, and if the data file is given as a command-line argument,
 * then the program saves the diagram and exits without displaying it */
    private boolean doNotExit = false;
/** if true the program does display dialogs with warnings or errors */
    private boolean doNotStop = false;
/** if true the concentration range in the x-axis may have a reversed "order",
 * that is, minimum value to the right and maximum value to the left, if so
 * it is given in the input data file. */
    private boolean reversedConcs = false;
/** the directory where the program files are located */
    private static String pathApp;
/** the directory where the last input data file is located */
    StringBuffer pathDef = new StringBuffer();
    private java.io.File inputDataFile = null;
    private java.io.File outputPltFile = null;
/** true if an input data file is given in the command line.
 * If so, then the calculations are performed without waiting for user actions,
 * a diagram is generated and saved, and the program exits unless doNotExit is true. */
    private boolean inputDataFileInCommandLine;
/** true if the calculations have finished, or if the user wishes to finish
 * them (and to end the program) */
    private boolean finishedCalculations = true;
/** true if the graphic user interface (GUI) has been displayed and then closed by the user */
    private boolean programEnded = true;
/** An instance of SwingWorker to perform the HaltaFall calculations */
    private HaltaTask tsk = null;
/** used to calculate execution time */
    private long calculationStart = 0;
/** the execution time */
    private long calculationTime = 0;
/** size of the user's computer screen */
    static java.awt.Dimension screenSize;
/** original size of the program window */
    private final java.awt.Dimension originalSize;

/** a class used to store and retrieve data, used by HaltaFall */
    private Chem ch = null;
/** a class to store data about a chemical system */
    private Chem.ChemSystem cs = null;
/** a class to store data about a the concentrations to calculate an
 * equilibrium composition, used by HaltaFall */
    private Chem.ChemSystem.ChemConcs csC = null;
/** a class to store diverse information on a chemical system: names of species, etc */
    private Chem.ChemSystem.NamesEtc namn = null;
/** a class to store array data about a diagram */
    private Chem.DiagrConcs dgrC = null;
/** a class to store non-array data about a diagram */
    private Chem.Diagr diag = null;
/** a class to calculate activity coefficients */
    private Factor factor;
/** the calculations class */
    private HaltaFall h;

/** if true only aquous species will be shown in the diagram, that is,
 * predominance areas for solids will not appear. */
    boolean aqu = false;
/** if true a dashed line will be plotted in the diagram showing the pH of
 * neutral water, which is temperature dependent. At 25 C the neutral pH is 7. */
    boolean neutral_pH = false;
/** the species predominating in the diagram at the point being calculated.
 * -2 if two solid species have practically the same amount of "main component" */
    private int topSpecies = -1;

/** a class to read text files where data is separated by commas */
    private ReadDataLib rd;
    private Plot_Predom plot = null;
/** data from a plot-file needed by the paint methods */
    GraphLib.PltData dd; // instantiated in "Plot_Predom.drawPlot"
                                   // it containts the info in the plot file

    private HelpAboutF helpAboutFrame = null;
    private final javax.swing.JFileChooser fc;
    private final javax.swing.filechooser.FileNameExtensionFilter filterDat;
/** true if the message text area should be erased before a new datafile is
 * read (after the user has selected a new data file) */
    private boolean eraseTextArea = false;
/** Where errors will be printed. It may be <code>System.err</code>.
 * If null, <code>System.err</code> is used. */
    private java.io.PrintStream err;
/** Where messages will be printed. It may be <code>System.out</code>.
 * If null, <code>System.out</code> is used. */
    private java.io.PrintStream out;

    /** The maximum number of calculation steps along an axis */
    private final static int NSTP_MAX = 1000;
    /** The minimum number of calculation steps along the X-axis */
    private final static int NSTP_MIN = 4;
    private final static int NSTP_DEF = 50;
    /** The number of calculation steps along an axis.
     * The number of calculated points in the plot is <code>nSteps+1</code>
     * along each axis. Note that to the outside world, the number of
     * calculation points are reported.  */
    int nSteps = NSTP_DEF;
    /** The calculation step being performed along the outer loop (out of nSteps).
     * The first point corresponds to no step, <code>nStepOuter = 0</code> */
    private int nStepOuter;
    /** The calculation step being performed along the inner loop (out of nSteps)
     * The first point corresponds to no step, <code>nStepInner = 0</code> */
    private int nStepInner;
    private final double ln10 = Math.log(10d);
    /** true if activity coeeficients have to be calculated */
    boolean calcActCoeffs = false;
    private final int actCoeffsModelDefault =2;
    /** the ionic strength, or -1 if it has to be calculated at each calculation step */
    private double ionicStrength = Double.NaN;
    double temperature_InCommandLine = Double.NaN;
    private int actCoeffsModel_InCommandLine = -1;
    private double tolHalta = Chem.TOL_HALTA_DEF;
    double peEh = Double.NaN;
    double tHeight;

    /** output debug reporting in HaltaFall. Default = Chem.DBGHALTA_DEF = 1 (report errors only)
     * @see Chem.ChemSystem.ChemConcs#dbg Chem.ChemSystem.ChemConcs.dbg */
    private int dbgHalta = Chem.DBGHALTA_DEF;
    /** true if the component has either <code>noll</code> = false or it has positive
     * values for the stoichiometric coefficients (a[ix][ia]-values)
     * @see chem.Chem.ChemSystem#a a
     * @see chem.Chem.ChemSystem#noll noll */
    private boolean[] pos;
    /** true if the component has some negative
     * values for the stoichiometric coefficients (a[ix][ia]-values)
     * @see chem.Chem.ChemSystem#a a */
    private boolean[] neg;
    //
    private final DiagrPaintUtility diagrPaintUtil;
    //
    private final java.io.PrintStream errPrintStream  =
       new java.io.PrintStream(
         new errFilteredStreamPredom(
           new java.io.ByteArrayOutputStream()),true);
    private final java.io.PrintStream outPrintStream  =
       new java.io.PrintStream(
         new outFilteredStreamPredom(
           new java.io.ByteArrayOutputStream()),true);
    /** true if a minumum of information is sent to the console (System.out)
     * when inputDataFileInCommandLine and not doNotExit.
     * False if all output is sent only to the JTextArea panel. */
    boolean consoleOutput = true;
    /** New-line character(s) to substitute "\n".<br>
     * It is needed when a String is created first, including new-lines,
     * and the String is then printed. For example
     * <pre>String t = "1st line\n2nd line";
     *System.out.println(t);</pre>will add a carriage return character
     * between the two lines, which on Windows system might be
     * unsatisfactory. Use instead:
     * <pre>String t = "1st line" + nl + "2nd line";
     *System.out.println(t);</pre> */
    private static final String nl = System.getProperty("line.separator");
    private static final java.util.Locale engl = java.util.Locale.ENGLISH;
    static final String LINE = "-------------------------------------";
    private static final String SLASH = java.io.File.separator;
    //
    //todo: no names can start with "*" after reading the chemical system

  //<editor-fold defaultstate="collapsed" desc="Constructor">
    /** Creates new form Predom
     * @param doNotExit0
     * @param doNotStop0
     * @param dbg0  */
    public Predom(
            final boolean doNotExit0,
            final boolean doNotStop0,
            final boolean dbg0) {
        initComponents();
        dbg = dbg0;
        doNotStop = doNotStop0;
        doNotExit = doNotExit0;
        // ---- redirect all output to the tabbed pane
        out = outPrintStream;
        err = errPrintStream;
        // ---- get the current working directory
        setPathDef();
        if(DBG_DEFAULT) {System.out.println("default path: \""+pathDef.toString()+"\"");}
        // ---- Define open/save file filters
        fc = new javax.swing.JFileChooser("."); //the "user" path
        filterDat =new javax.swing.filechooser.FileNameExtensionFilter("*.dat", new String[] { "DAT"});
        // ----
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        //--- F1 for help
        javax.swing.KeyStroke f1KeyStroke = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1,0, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(f1KeyStroke,"F1");
        javax.swing.Action f1Action = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jMenuHelpAbout.isEnabled()) {jMenuHelpAbout.doClick();}
            }};
        getRootPane().getActionMap().put("F1", f1Action);
        //--- ctrl-C: stop calculations
        javax.swing.KeyStroke ctrlCKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_DOWN_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlCKeyStroke,"CTRL_C");
        javax.swing.Action ctrlCAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 1) {
                      if(h != null) {h.haltaCancel();}
                      if(tsk != null) {tsk.cancel(true);}
                      finishedCalculations = true;
                      Predom.this.notify_All();
                }
            }};
        getRootPane().getActionMap().put("CTRL_C", ctrlCAction);
        //--- define Alt-key actions
        //--- alt-X: eXit
        javax.swing.KeyStroke altXKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altXKeyStroke,"ALT_X");
        javax.swing.Action altXAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jMenuFileXit.isEnabled()) {jMenuFileXit.doClick();}
            }};
        getRootPane().getActionMap().put("ALT_X", altXAction);
        //--- alt-Q: quit
        javax.swing.KeyStroke altQKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altQKeyStroke,"ALT_Q");
        getRootPane().getActionMap().put("ALT_Q", altXAction);
        //--- alt-Enter: make diagram
        javax.swing.KeyStroke altEnterKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altEnterKeyStroke,"ALT_Enter");
        javax.swing.Action altEnterAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jMenuFileMakeD.isEnabled()) {jMenuFileMakeD.doClick();}
            }};
        getRootPane().getActionMap().put("ALT_Enter", altEnterAction);
        //--- alt-C: method for activity coefficients
        javax.swing.KeyStroke altCKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altCKeyStroke,"ALT_C");
        javax.swing.Action altCAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0 &&
                        jComboBoxModel.isEnabled()) {jComboBoxModel.requestFocusInWindow();}
            }};
        getRootPane().getActionMap().put("ALT_C", altCAction);
        //--- alt-D: diagram
        javax.swing.KeyStroke altDKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altDKeyStroke,"ALT_D");
        javax.swing.Action altDAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0 &&
                        jMenuFileMakeD.isEnabled() &&
                        jButtonDoIt.isEnabled()) {jButtonDoIt.doClick();
                } else if(jTabbedPane.getSelectedIndex() != 2 &&
                        jTabbedPane.isEnabledAt(2)) {jTabbedPane.setSelectedIndex(2);
                }
            }};
        getRootPane().getActionMap().put("ALT_D", altDAction);
        //--- alt-E: hEight
        javax.swing.KeyStroke altEKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.ALT_MASK, false);
        javax.swing.Action altEAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0){jScrollBarHeight.requestFocusInWindow();}
            }};
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altEKeyStroke,"ALT_E");
        getRootPane().getActionMap().put("ALT_E", altEAction);
        //--- alt-L: pLot file name
        javax.swing.KeyStroke altLKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altLKeyStroke,"ALT_L");
        javax.swing.Action altLAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0){jTextFieldPltFile.requestFocusInWindow();}
            }};
        getRootPane().getActionMap().put("ALT_L", altLAction);
        //--- alt-M: messages pane
        javax.swing.KeyStroke altMKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altMKeyStroke,"ALT_M");
        javax.swing.Action altMAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() != 1 &&
                        jTabbedPane.isEnabledAt(1)) {jTabbedPane.setSelectedComponent(jScrollPaneMessg);}
            }};
        getRootPane().getActionMap().put("ALT_M", altMAction);
        //--- alt-N:  nbr of points
        javax.swing.KeyStroke altNKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altNKeyStroke,"ALT_N");
        javax.swing.Action altNAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0){jScrollBarNbrPoints.requestFocusInWindow();}
            }};
        getRootPane().getActionMap().put("ALT_N", altNAction);
        //--- alt-P: parameters
        javax.swing.KeyStroke altPKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altPKeyStroke,"ALT_P");
        javax.swing.Action altPAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() != 0) {jTabbedPane.setSelectedIndex(0);}
            }};
        getRootPane().getActionMap().put("ALT_P", altPAction);
        //--- alt-S:  ionic strength / stop calcúlations
        javax.swing.KeyStroke altSKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altSKeyStroke,"ALT_S");
        javax.swing.Action altSAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0 &&
                        jTextFieldIonicStgr.isEnabled()) {
                    jTextFieldIonicStgr.requestFocusInWindow();
                } else if(jTabbedPane.getSelectedIndex() == 1) {
                      if(h != null) {h.haltaCancel();}
                      if(tsk != null) {tsk.cancel(true);}
                      finishedCalculations = true;
                      Predom.this.notify_All();
                }
            }};
        getRootPane().getActionMap().put("ALT_S", altSAction);
        //--- alt-R:  temperature
        javax.swing.KeyStroke altRKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altRKeyStroke,"ALT_R");
        javax.swing.Action altRAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0 &&
                        jTextFieldT.isEnabled()) {jTextFieldT.requestFocusInWindow();}
            }};
        getRootPane().getActionMap().put("ALT_R", altRAction);
        //--- alt-T:  tolerance in Haltafall
        javax.swing.KeyStroke altTKeyStroke = javax.swing.KeyStroke.getKeyStroke(
                java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.ALT_MASK, false);
        getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).put(altTKeyStroke,"ALT_T");
        javax.swing.Action altTAction = new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if(jTabbedPane.getSelectedIndex() == 0 &&
                        jComboBoxTol.isEnabled()) {jComboBoxTol.requestFocusInWindow();}
            }};
        getRootPane().getActionMap().put("ALT_T", altTAction);
        // -------

        //--- Title
        this.setTitle("Predom:  Predominance Area Diagrams");
        jMenuBar.add(javax.swing.Box.createHorizontalGlue(),2); //move "Help" menu to the right

        //--- center Window on Screen
        originalSize = this.getSize();
        screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int left; int top;
        left = Math.max(55, (screenSize.width  - originalSize.width ) / 2);
        top = Math.max(10, (screenSize.height - originalSize.height) / 2);
        this.setLocation(Math.min(screenSize.width-100, left),
                         Math.min(screenSize.height-100, top));
        //---- Icon
        String iconName = "images/Predom256_32x32_blackBckgr.gif";
        java.net.URL imgURL = this.getClass().getResource(iconName);
        java.awt.Image icon;
        if (imgURL != null) {
            icon = new javax.swing.ImageIcon(imgURL).getImage();
            this.setIconImage(icon);
            //com.apple.eawt.Application.getApplication().setDockIconImage(new javax.swing.ImageIcon("Football.png").getImage());
            if(System.getProperty("os.name").startsWith("Mac OS")) {
                try {
                    Class<?> c = Class.forName("com.apple.eawt.Application");
                    //Class params[] = new Class[] {java.awt.Image.class};
                    java.lang.reflect.Method m =
                        c.getDeclaredMethod("setDockIconImage",new Class[] { java.awt.Image.class });
                    Object i = c.newInstance();
                    Object paramsObj[] = new Object[]{icon};
                    m.invoke(i, paramsObj);
                } catch (Exception e) {System.out.println("Error: "+e.getMessage());}
            }
        } else {
            System.out.println("Error: Could not load image = \""+iconName+"\"");
        }

        //--- set up the Form
        jMenuFileMakeD.setEnabled(false);
        jButtonDoIt.setText("make the Diagram");
        jButtonDoIt.setEnabled(false);
        java.awt.Font f = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12);
        jTextAreaA.setFont(f);
        jTextAreaA.setText(null);
        jTabbedPane.setEnabledAt(1, false);
        jTabbedPane.setEnabledAt(2, false);

        jScrollBarNbrPoints.setFocusable(true);
        jScrollBarNbrPoints.setValue(nSteps);
        jScrollBarHeight.setFocusable(true);
        tHeight = 1;
        jScrollBarHeight.setValue(Math.round((float)(10*tHeight)));

        jLabelPltFile.setText("plot file name");
        jLabelPltFile.setEnabled(false);
        jTextFieldPltFile.setText(null);
        jTextFieldPltFile.setEnabled(false);
        jLabelStatus.setText("waiting...");
        jLabelProgress.setText(" ");
        jTextFieldT.setText("NaN"); // no temperature given
        jComboBoxModel.setSelectedIndex(actCoeffsModelDefault);

        //--- the methods in DiagrPaintUtility are used to paint the diagram
        diagrPaintUtil = new DiagrPaintUtility();

    } // Predom constructor
    //</editor-fold>

//<editor-fold defaultstate="collapsed" desc="start">
 /** Sets this window frame visible, and deals with the command-line arguments
  * @param reversedConcs0 needed when reading the input data file.
  * The comman-line argument may be given <i>after</i> the data file name...
  * @param help0 if help is displayed, request focus for the text pane,
  * where the help is shown
  * @param args the command-line arguments */
  private void start(
          final boolean reversedConcs0,
          final boolean help0,
          final String[] args) {
    this.setVisible(true);
    //----
    out.println(LINE+nl+progName+" (Predominance Area Diagrams),  version: "+VERS);
    java.util.Date today = new java.util.Date();
    java.text.DateFormat dateFormatter =
            java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.DEFAULT,
                 java.text.DateFormat.DEFAULT, java.util.Locale.getDefault());
    out.println("PREDOM started: "+dateFormatter.format(today));

    //--- deal with any command line arguments
    inputDataFileInCommandLine = false;
    reversedConcs = reversedConcs0; //this is needed when reading the data file
    boolean argErr = false;
    if(args != null && args.length >0){
        for (String arg : args) {
            if (!dispatchArg(arg)) {argErr = true;}
        } // for arg
    } // if argsList != null
    if(argErr && !doNotExit) {end_program(); return;}
    out.println("Finished reading command-line arguments.");
    if(dbg) {
      out.println("--------"+nl+
                  "Application path: \""+pathApp+"\""+nl+
                  "Default path: \""+pathDef.toString()+"\""+nl+
                  "--------");
    }
    consoleOutput = inputDataFileInCommandLine & !doNotExit;

    // is the temperture missing even if a ionic strength is given?
    if((!Double.isNaN(ionicStrength) && Math.abs(ionicStrength) >1e-10)
            && Double.isNaN(temperature_InCommandLine)) {
        String msg = "Warning: ionic strength given as command line argument, I="
                +(float)ionicStrength+nl+
                "    but no temperature is given on the command line.";
        out.println(msg);
    }
    // if the command-line "-i" is not given, set the ionic strength to zero
    if(!calcActCoeffs) {ionicStrength = 0;}

    jCheckReverse.setSelected(reversedConcs);
    jCheckActCoeff.setSelected(calcActCoeffs);
    jTextFieldIonicStgr.setText(String.valueOf(ionicStrength));
    showActivityCoefficientControls(calcActCoeffs);
    set_tol_inComboBox();
    showTemperature();

    //--- if help is requested on the command line and the
    //    program's window stays on screen: show the message pane
    if(help0) {
        jTextAreaA.setCaretPosition(0);
        jTabbedPane.setSelectedComponent(jScrollPaneMessg);
    }

    // Make a diagram if an input file is given in the command-line
    if(inputDataFileInCommandLine == true) {
        if(outputPltFile == null) {
            String txt = inputDataFile.getAbsolutePath();
            String plotFileN = txt.substring(0,txt.length()-3).concat("plt");
            outputPltFile = new java.io.File(plotFileN);
        }
        // note: as the calculations are done on a worker thread, this returns pretty quickly
        try {doCalculations();}
        catch (Exception ex) {showErrMsgBx(ex);}
    }
    programEnded = false;
  } //start
//</editor-fold>

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupDebug = new javax.swing.ButtonGroup();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanelParameters = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanelFiles = new javax.swing.JPanel();
        jLabelData = new javax.swing.JLabel();
        jTextFieldDataFile = new javax.swing.JTextField();
        jLabelPltFile = new javax.swing.JLabel();
        jTextFieldPltFile = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jButtonDoIt = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabelNbrPText = new javax.swing.JLabel();
        jLabelPointsNbr = new javax.swing.JLabel();
        jScrollBarNbrPoints = new javax.swing.JScrollBar();
        jLabelHeight = new javax.swing.JLabel();
        jLabelHD = new javax.swing.JLabel();
        jScrollBarHeight = new javax.swing.JScrollBar();
        jPanel3 = new javax.swing.JPanel();
        jCheckReverse = new javax.swing.JCheckBox();
        jPanelActC = new javax.swing.JPanel();
        jCheckActCoeff = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        jLabelIonicStr = new javax.swing.JLabel();
        jTextFieldIonicStgr = new javax.swing.JTextField();
        jLabelIonicStrM = new javax.swing.JLabel();
        jPanelT = new javax.swing.JPanel();
        jLabelT = new javax.swing.JLabel();
        jTextFieldT = new javax.swing.JTextField();
        jLabelTC = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabelModel = new javax.swing.JLabel();
        jComboBoxModel = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jLabelTol = new javax.swing.JLabel();
        jComboBoxTol = new javax.swing.JComboBox();
        jScrollPaneMessg = new javax.swing.JScrollPane();
        jTextAreaA = new javax.swing.JTextArea();
        jPanelDiagram = new javax.swing.JPanel()   
        {   
            @Override
            public void paint(java.awt.Graphics g)   
            {   
                super.paint(g);   
                paintDiagrPanel(g);   
            }   
        };
        jPanelStatusBar = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        jLabelProgress = new javax.swing.JLabel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuFileOpen = new javax.swing.JMenuItem();
        jMenuFileMakeD = new javax.swing.JMenuItem();
        jMenuFileXit = new javax.swing.JMenuItem();
        jMenuDebug = new javax.swing.JMenu();
        jCheckBoxMenuPredomDebug = new javax.swing.JCheckBoxMenuItem();
        jMenuSave = new javax.swing.JMenuItem();
        jMenuCancel = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuHelpAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                formWindowGainedFocus(evt);
            }
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        jTabbedPane.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane.setAlignmentX(0.0F);

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/predominanceAreaDiagrams/images/Predom256_32x32_whiteBckgr.gif"))); // NOI18N

        jLabelData.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelData.setLabelFor(jTextFieldDataFile);
        jLabelData.setText("input data file:");

        jTextFieldDataFile.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldDataFile.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextFieldDataFileMouseClicked(evt);
            }
        });
        jTextFieldDataFile.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldDataFileKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextFieldDataFileKeyTyped(evt);
            }
        });

        jLabelPltFile.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelPltFile.setLabelFor(jTextFieldPltFile);
        jLabelPltFile.setText("<html>p<u>l</u>ot file name:</html>");

        jTextFieldPltFile.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextFieldPltFileFocusGained(evt);
            }
        });

        javax.swing.GroupLayout jPanelFilesLayout = new javax.swing.GroupLayout(jPanelFiles);
        jPanelFiles.setLayout(jPanelFilesLayout);
        jPanelFilesLayout.setHorizontalGroup(
            jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilesLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabelData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelPltFile))
                .addGap(5, 5, 5)
                .addGroup(jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextFieldDataFile, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE)
                    .addComponent(jTextFieldPltFile, javax.swing.GroupLayout.DEFAULT_SIZE, 460, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanelFilesLayout.setVerticalGroup(
            jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilesLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabelData)
                    .addComponent(jTextFieldDataFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelPltFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextFieldPltFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButtonDoIt.setText("make the Diagram");
        jButtonDoIt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDoItActionPerformed(evt);
            }
        });

        jLabelNbrPText.setLabelFor(jLabelPointsNbr);
        jLabelNbrPText.setText("<html><u>N</u>br of calc. steps:</html>");

        jLabelPointsNbr.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelPointsNbr.setText(" 50");
        jLabelPointsNbr.setToolTipText("double-click to reset to default");
        jLabelPointsNbr.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabelPointsNbrMouseClicked(evt);
            }
        });

        jScrollBarNbrPoints.setMaximum(NSTP_MAX+1);
        jScrollBarNbrPoints.setMinimum(NSTP_MIN);
        jScrollBarNbrPoints.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        jScrollBarNbrPoints.setVisibleAmount(1);
        jScrollBarNbrPoints.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBarNbrPointsAdjustmentValueChanged(evt);
            }
        });
        jScrollBarNbrPoints.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jScrollBarNbrPointsFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jScrollBarNbrPointsFocusLost(evt);
            }
        });

        jLabelHeight.setText("<html>h<u>e</u>ight of text in diagram:</html>");

        jLabelHD.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelHD.setText("1.0");
        jLabelHD.setToolTipText("double-click to reset to default");
        jLabelHD.setMaximumSize(new java.awt.Dimension(15, 14));
        jLabelHD.setMinimumSize(new java.awt.Dimension(15, 14));
        jLabelHD.setPreferredSize(new java.awt.Dimension(15, 14));
        jLabelHD.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabelHDMouseClicked(evt);
            }
        });

        jScrollBarHeight.setMaximum(101);
        jScrollBarHeight.setMinimum(3);
        jScrollBarHeight.setOrientation(javax.swing.JScrollBar.HORIZONTAL);
        jScrollBarHeight.setValue(10);
        jScrollBarHeight.setVisibleAmount(1);
        jScrollBarHeight.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jScrollBarHeightFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jScrollBarHeightFocusLost(evt);
            }
        });
        jScrollBarHeight.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
            public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
                jScrollBarHeightAdjustmentValueChanged(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabelNbrPText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelPointsNbr, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelHD, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollBarHeight, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE)
                    .addComponent(jScrollBarNbrPoints, javax.swing.GroupLayout.DEFAULT_SIZE, 75, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelNbrPText, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelPointsNbr))
                    .addComponent(jScrollBarNbrPoints, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabelHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelHD, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollBarHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonDoIt, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(jButtonDoIt)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jCheckReverse.setMnemonic(java.awt.event.KeyEvent.VK_R);
        jCheckReverse.setText("<html>allow <u>R</u>eversed min. and max. axes limits</html>");
        jCheckReverse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckReverseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jCheckReverse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jCheckReverse, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jCheckActCoeff.setMnemonic(java.awt.event.KeyEvent.VK_A);
        jCheckActCoeff.setText("<html><u>A</u>ctivity coefficient calculations</html>");
        jCheckActCoeff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckActCoeffActionPerformed(evt);
            }
        });

        jLabelIonicStr.setLabelFor(jTextFieldIonicStgr);
        jLabelIonicStr.setText("<html>ionic <u>S</u>trength</html>");
        jLabelIonicStr.setEnabled(false);

        jTextFieldIonicStgr.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldIonicStgr.setEnabled(false);
        jTextFieldIonicStgr.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldIonicStgrActionPerformed(evt);
            }
        });
        jTextFieldIonicStgr.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextFieldIonicStgrFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldIonicStgrFocusLost(evt);
            }
        });
        jTextFieldIonicStgr.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldIonicStgrKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextFieldIonicStgrKeyTyped(evt);
            }
        });

        jLabelIonicStrM.setText("<html>mol/(kg H<sub>2</sub>O)</html>");
        jLabelIonicStrM.setEnabled(false);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelIonicStr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldIonicStgr, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelIonicStrM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabelIonicStr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jTextFieldIonicStgr, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabelIonicStrM, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jLabelT.setLabelFor(jTextFieldT);
        jLabelT.setText("<html>tempe<u>r</u>ature</html>");
        jLabelT.setEnabled(false);

        jTextFieldT.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        jTextFieldT.setToolTipText("<html>\nThe temperature is needed either to calculate Debye-Hückel constants<br>\nin activity coefficient models, or to calculate Eh values<br>\nfrom pe: Eh = pe*(ln(10)*R*T)/F<br>\nValue must be between 0 and 300 (in Celsius)</html>");
        jTextFieldT.setEnabled(false);
        jTextFieldT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldTActionPerformed(evt);
            }
        });
        jTextFieldT.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                jTextFieldTFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                jTextFieldTFocusLost(evt);
            }
        });
        jTextFieldT.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldTKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextFieldTKeyTyped(evt);
            }
        });

        jLabelTC.setText("°C");
        jLabelTC.setEnabled(false);

        javax.swing.GroupLayout jPanelTLayout = new javax.swing.GroupLayout(jPanelT);
        jPanelT.setLayout(jPanelTLayout);
        jPanelTLayout.setHorizontalGroup(
            jPanelTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTLayout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldT, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabelTC)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelTLayout.setVerticalGroup(
            jPanelTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelTLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jTextFieldT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jLabelTC))
        );

        jLabelModel.setLabelFor(jComboBoxModel);
        jLabelModel.setText("<html>activity <u>C</u>officient model:<html>");

        jComboBoxModel.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Davies eqn.", "SIT (Specific Ion interaction 'Theory')", "simplified HKF (Helgeson, Kirkham & Flowers)" }));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jComboBoxModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addComponent(jLabelModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxModel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanelActCLayout = new javax.swing.GroupLayout(jPanelActC);
        jPanelActC.setLayout(jPanelActCLayout);
        jPanelActCLayout.setHorizontalGroup(
            jPanelActCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActCLayout.createSequentialGroup()
                .addGroup(jPanelActCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelActCLayout.createSequentialGroup()
                        .addGroup(jPanelActCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelActCLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jPanelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCheckActCoeff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelActCLayout.setVerticalGroup(
            jPanelActCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelActCLayout.createSequentialGroup()
                .addGroup(jPanelActCLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelActCLayout.createSequentialGroup()
                        .addComponent(jCheckActCoeff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jPanelT, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 6, Short.MAX_VALUE))
                    .addGroup(jPanelActCLayout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jLabelTol.setLabelFor(jComboBoxTol);
        jLabelTol.setText("tolerance (for calculations in HaltaFall):");

        jComboBoxTol.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1E-2", "1E-3", "1E-4", "1E-5", "1E-6", "1E-7", "1E-8", "1E-9", " " }));
        jComboBoxTol.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxTolActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jLabelTol)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jComboBoxTol, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabelTol)
                .addComponent(jComboBoxTol, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout jPanelParametersLayout = new javax.swing.GroupLayout(jPanelParameters);
        jPanelParameters.setLayout(jPanelParametersLayout);
        jPanelParametersLayout.setHorizontalGroup(
            jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelParametersLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanelFiles, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanelActC, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanelParametersLayout.setVerticalGroup(
            jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelParametersLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelParametersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jPanelFiles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelActC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(73, Short.MAX_VALUE))
        );

        jTabbedPane.addTab("<html><u>P</u>arameters</html>", jPanelParameters);

        jScrollPaneMessg.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        jScrollPaneMessg.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        jScrollPaneMessg.setFont(new java.awt.Font("Monospaced", 0, 13)); // NOI18N

        jTextAreaA.setBackground(new java.awt.Color(255, 255, 204));
        jTextAreaA.setText("Use the PrintStreams \"err\" and \"out\" to\nsend messages to this pane, for example;\n   out.println(\"message\");\n   err.println(\"Error\");\netc.\nSystem.out and System.err will send\noutput to the console, which might\nnot be available to the user.");
        jTextAreaA.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextAreaAKeyPressed(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTextAreaAKeyTyped(evt);
            }
        });
        jScrollPaneMessg.setViewportView(jTextAreaA);

        jTabbedPane.addTab("Messages", jScrollPaneMessg);

        jPanelDiagram.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout jPanelDiagramLayout = new javax.swing.GroupLayout(jPanelDiagram);
        jPanelDiagram.setLayout(jPanelDiagramLayout);
        jPanelDiagramLayout.setHorizontalGroup(
            jPanelDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 611, Short.MAX_VALUE)
        );
        jPanelDiagramLayout.setVerticalGroup(
            jPanelDiagramLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );

        jTabbedPane.addTab("Diagram", jPanelDiagram);

        jPanelStatusBar.setBackground(new java.awt.Color(204, 255, 255));
        jPanelStatusBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jLabelStatus.setText("# # # #");

        jLabelProgress.setText("text");

        javax.swing.GroupLayout jPanelStatusBarLayout = new javax.swing.GroupLayout(jPanelStatusBar);
        jPanelStatusBar.setLayout(jPanelStatusBarLayout);
        jPanelStatusBarLayout.setHorizontalGroup(
            jPanelStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanelStatusBarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelStatus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelStatusBarLayout.setVerticalGroup(
            jPanelStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelStatusBarLayout.createSequentialGroup()
                .addGroup(jPanelStatusBarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabelProgress)
                    .addComponent(jLabelStatus))
                .addContainerGap())
        );

        jMenuFile.setMnemonic('F');
        jMenuFile.setText("File");

        jMenuFileOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.ALT_MASK));
        jMenuFileOpen.setMnemonic('o');
        jMenuFileOpen.setText("Open input file");
        jMenuFileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFileOpenActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuFileOpen);

        jMenuFileMakeD.setMnemonic('D');
        jMenuFileMakeD.setText("make the Diagram");
        jMenuFileMakeD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFileMakeDActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuFileMakeD);

        jMenuFileXit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.ALT_MASK));
        jMenuFileXit.setMnemonic('x');
        jMenuFileXit.setText("Exit");
        jMenuFileXit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuFileXitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuFileXit);

        jMenuBar.add(jMenuFile);

        jMenuDebug.setMnemonic('b');
        jMenuDebug.setText("debug");

        jCheckBoxMenuPredomDebug.setMnemonic('V');
        jCheckBoxMenuPredomDebug.setText("Verbose");
        jCheckBoxMenuPredomDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuPredomDebugActionPerformed(evt);
            }
        });
        jMenuDebug.add(jCheckBoxMenuPredomDebug);

        jMenuSave.setMnemonic('v');
        jMenuSave.setText("save messages to file");
        jMenuSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuSaveActionPerformed(evt);
            }
        });
        jMenuDebug.add(jMenuSave);

        jMenuCancel.setMnemonic('S');
        jMenuCancel.setText("STOP the Calculations (Alt-S)");
        jMenuCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuCancelActionPerformed(evt);
            }
        });
        jMenuDebug.add(jMenuCancel);

        jMenuBar.add(jMenuDebug);

        jMenuHelp.setText("Help");

        jMenuHelpAbout.setMnemonic('a');
        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuHelpAboutActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuHelpAbout);

        jMenuBar.add(jMenuHelp);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane)
            .addComponent(jPanelStatusBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanelStatusBar, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

//<editor-fold defaultstate="collapsed" desc="Events">
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        end_program();
    }//GEN-LAST:event_formWindowClosing

    private void jMenuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuHelpAboutActionPerformed
        jMenuHelpAbout.setEnabled(false);
        Thread hlp = new Thread() {@Override public void run(){
            helpAboutFrame = new HelpAboutF(VERS, pathApp, out);
            helpAboutFrame.setVisible(true);
            helpAboutFrame.waitFor();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
                helpAboutFrame = null;
                jMenuHelpAbout.setEnabled(true);
            }}); //invokeLater(Runnable)
        }};//new Thread
        hlp.start();
    }//GEN-LAST:event_jMenuHelpAboutActionPerformed

    private void jMenuFileXitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFileXitActionPerformed
        end_program();
    }//GEN-LAST:event_jMenuFileXitActionPerformed

    private void jMenuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFileOpenActionPerformed
        eraseTextArea = true;
        getTheInputFileName();
        jTabbedPane.requestFocusInWindow();
    }//GEN-LAST:event_jMenuFileOpenActionPerformed

    private void jMenuFileMakeDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuFileMakeDActionPerformed
        jButtonDoIt.doClick();
    }//GEN-LAST:event_jMenuFileMakeDActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        int width = Math.round((float)originalSize.getWidth());
        int height = Math.round((float)originalSize.getHeight());
        if(this.getHeight()<height){this.setSize(this.getWidth(), height);}
        if(this.getWidth()<width){this.setSize(width,this.getHeight());}
        if(jTabbedPane.getWidth()>this.getWidth()) {
            jTabbedPane.setSize(this.getWidth(), jTabbedPane.getWidth());
        }
    }//GEN-LAST:event_formComponentResized

    private void jTextAreaAKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextAreaAKeyTyped
        evt.consume();
    }//GEN-LAST:event_jTextAreaAKeyTyped

    private void jTextAreaAKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextAreaAKeyPressed
        if(!Util.isKeyPressedOK(evt)) {evt.consume();}
    }//GEN-LAST:event_jTextAreaAKeyPressed

    private void jCheckBoxMenuPredomDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuPredomDebugActionPerformed
        dbg = jCheckBoxMenuPredomDebug.isSelected();
    }//GEN-LAST:event_jCheckBoxMenuPredomDebugActionPerformed

    private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
        if(helpAboutFrame != null) {helpAboutFrame.bringToFront();}

    }//GEN-LAST:event_formWindowGainedFocus

private void jComboBoxTolActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxTolActionPerformed
        int i = jComboBoxTol.getSelectedIndex();
        tolHalta = 0.01/Math.pow(10,i);
}//GEN-LAST:event_jComboBoxTolActionPerformed

private void jButtonDoItActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDoItActionPerformed
        // If no plot-file name is given in the command line but
        //      a data-file name is given: set a default plot-file name
        if(outputPltFile == null) {
            String dir = pathDef.toString();
            if(dir.endsWith(SLASH)) {dir = dir.substring(0,dir.length()-1);}
            outputPltFile = new java.io.File(dir + SLASH + jTextFieldPltFile.getText());
        }
        // note: as the calculations are done on a worker thread, this returns pretty quickly
        doCalculations();
        // statements here are performed almost inmediately
}//GEN-LAST:event_jButtonDoItActionPerformed

private void jTextFieldTKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldTKeyTyped
        char key = evt.getKeyChar();
        if(!isCharOKforNumberInput(key)) {evt.consume();}
}//GEN-LAST:event_jTextFieldTKeyTyped

private void jTextFieldTKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldTKeyPressed
        if(evt.getKeyChar() == java.awt.event.KeyEvent.VK_ENTER) {
            validateTemperature();
        }
}//GEN-LAST:event_jTextFieldTKeyPressed

private void jTextFieldTFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldTFocusLost
        validateTemperature();
}//GEN-LAST:event_jTextFieldTFocusLost

private void jTextFieldTFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldTFocusGained
        jTextFieldT.selectAll();
}//GEN-LAST:event_jTextFieldTFocusGained

private void jTextFieldTActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldTActionPerformed
        validateTemperature();
}//GEN-LAST:event_jTextFieldTActionPerformed

private void jTextFieldIonicStgrKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldIonicStgrKeyTyped
        char key = evt.getKeyChar();
        if(!isCharOKforNumberInput(key)) {evt.consume();}
}//GEN-LAST:event_jTextFieldIonicStgrKeyTyped

private void jTextFieldIonicStgrKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldIonicStgrKeyPressed
        if(evt.getKeyChar() == java.awt.event.KeyEvent.VK_ENTER) {
            validateIonicStrength();
        }
}//GEN-LAST:event_jTextFieldIonicStgrKeyPressed

private void jTextFieldIonicStgrFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldIonicStgrFocusLost
        validateIonicStrength();
}//GEN-LAST:event_jTextFieldIonicStgrFocusLost

private void jTextFieldIonicStgrFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldIonicStgrFocusGained
        jTextFieldIonicStgr.selectAll();
}//GEN-LAST:event_jTextFieldIonicStgrFocusGained

private void jTextFieldIonicStgrActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldIonicStgrActionPerformed
        validateIonicStrength();
}//GEN-LAST:event_jTextFieldIonicStgrActionPerformed

private void jCheckActCoeffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckActCoeffActionPerformed
        if(jCheckActCoeff.isSelected()) {
            calcActCoeffs = true;
            showActivityCoefficientControls(true);
            ionicStrength = readIonStrength();
            showTemperature();
        }
        else {
            calcActCoeffs = false;
            showActivityCoefficientControls(false);
            showTemperature();
        }
}//GEN-LAST:event_jCheckActCoeffActionPerformed

private void jCheckReverseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckReverseActionPerformed
        reversedConcs = jCheckReverse.isSelected();
}//GEN-LAST:event_jCheckReverseActionPerformed

private void jScrollBarHeightAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBarHeightAdjustmentValueChanged
        jLabelHD.setText(String.valueOf((float)jScrollBarHeight.getValue()/10f).trim());
}//GEN-LAST:event_jScrollBarHeightAdjustmentValueChanged

private void jScrollBarHeightFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jScrollBarHeightFocusLost
        jScrollBarHeight.setBorder(null);
}//GEN-LAST:event_jScrollBarHeightFocusLost

private void jScrollBarHeightFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jScrollBarHeightFocusGained
        jScrollBarHeight.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0,0,0)));
}//GEN-LAST:event_jScrollBarHeightFocusGained

private void jScrollBarNbrPointsAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_jScrollBarNbrPointsAdjustmentValueChanged
        jLabelPointsNbr.setText(String.valueOf(jScrollBarNbrPoints.getValue()).trim());
}//GEN-LAST:event_jScrollBarNbrPointsAdjustmentValueChanged

private void jScrollBarNbrPointsFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jScrollBarNbrPointsFocusLost
        jScrollBarNbrPoints.setBorder(null);
}//GEN-LAST:event_jScrollBarNbrPointsFocusLost

private void jScrollBarNbrPointsFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jScrollBarNbrPointsFocusGained
        jScrollBarNbrPoints.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0,0,0)));
}//GEN-LAST:event_jScrollBarNbrPointsFocusGained

private void jLabelPointsNbrMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelPointsNbrMouseClicked
    if(evt.getClickCount() >1) {  // double-click
        jScrollBarNbrPoints.setValue(NSTP_DEF);
    }
}//GEN-LAST:event_jLabelPointsNbrMouseClicked

private void jTextFieldPltFileFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_jTextFieldPltFileFocusGained
    jTextFieldPltFile.selectAll();
}//GEN-LAST:event_jTextFieldPltFileFocusGained

private void jTextFieldDataFileKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldDataFileKeyTyped
        char c = Character.toUpperCase(evt.getKeyChar());
        if(evt.getKeyChar() != java.awt.event.KeyEvent.VK_ESCAPE &&
           !(evt.isAltDown() &&
                ((c == 'A') ||
                 (c == 'B') || (c == 'C') ||
                 (c == 'D') || (c == 'E') ||
                 (c == 'F') || (c == 'H') ||
                 (c == 'L') || (c == 'M') ||
                 (c == 'N') ||
                 (c == 'P') || (c == 'R') ||
                 (c == 'S') || (c == 'T') ||
                 (c == 'X') )
                 ) //isAltDown
                 )
            {
                evt.consume(); // remove the typed key
                getTheInputFileName();
            }
}//GEN-LAST:event_jTextFieldDataFileKeyTyped

private void jTextFieldDataFileKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldDataFileKeyPressed
        if(!Util.isKeyPressedOK(evt)) {evt.consume();}
}//GEN-LAST:event_jTextFieldDataFileKeyPressed

private void jTextFieldDataFileMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextFieldDataFileMouseClicked
        eraseTextArea = true;
        getTheInputFileName();
}//GEN-LAST:event_jTextFieldDataFileMouseClicked

private void jLabelHDMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelHDMouseClicked
    if(evt.getClickCount() >1) {  // double-click
        jScrollBarHeight.setValue(10);
    }
}//GEN-LAST:event_jLabelHDMouseClicked

    private void jMenuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuSaveActionPerformed
        jTextFieldDataFile.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        setCursorWait();
        if(pathDef == null) {setPathDef();}
        String txtfn = Util.getSaveFileName(this, progName, "Choose an output file name:", 7, "messages.txt", pathDef.toString());
        jTextFieldDataFile.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setCursorDef();
        if(txtfn == null || txtfn.trim().equals("")) {return;}
        java.io.File outputTxtFile = new java.io.File(txtfn);
        java.io.PrintWriter pw = null;
        try {
            pw = new java.io.PrintWriter(new java.io.BufferedWriter(new java.io.FileWriter(outputTxtFile.toString())));
            pw.print(jTextAreaA.getText());
            pw.flush();
            pw.close();
            javax.swing.JOptionPane.showMessageDialog(this, "File:"+nl+"    "+outputTxtFile.toString()+nl+"has been written.",
                    progName,javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception ex) {showErrMsgBx(ex.toString(),1);}
        finally {if(pw != null) {pw.flush(); pw.close();}}
        jTabbedPane.setSelectedComponent(jScrollPaneMessg);
        jTabbedPane.requestFocusInWindow();
    }//GEN-LAST:event_jMenuSaveActionPerformed

    private void jMenuCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuCancelActionPerformed
        quitConfirm(this);
    }//GEN-LAST:event_jMenuCancelActionPerformed
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="checkInput">
/** Make checks and make changes to the data stored in the Chem classes.
 * @return true if checks are OK  */
  private boolean checkInput() {
    if(cs == null) {err.println("? Programming error in \"PREDOM.checkInput\": cs=null."); return false;}
    if(dbg) {out.println("--- checkInput()");}
    // -------------------
    //   CHEMICAL SYSTEM
    // -------------------
    // mg = total number of soluble species in the aqueous solution:
    //      all components + soluble complexes
    // Note that in HaltaFall the solid components are fictitious soluble
    // components with "zero" concentration (with noll = true)
    int mg = cs.Ms - cs.mSol; // = na + nx;
    int i;

    // ---- Remove asterisk "*" from the name of components
    for(i=0; i<cs.Na; i++) {
        if(namn.identC[i].startsWith("*")) {
            namn.identC[i] = namn.identC[i].substring(1);
            namn.ident[i]  = namn.identC[i];
            cs.noll[i] = true;
        }
        //if(chem_syst.Chem.isWater(csNamn.identC[i]))
        //            {water = i;}
    } // for i=0...Na-1

    // ---- Remove reaction products (soluble or solid) with
    //      name starting with "*".
    // Note that the solids corresponding to the components will
    // not have a name starting with "*". This is already removed when
    // reading the input file.
    double w; int j; int js;
    i = cs.Na;
    while (i < cs.Ms) {
        if(namn.ident[i].startsWith("*")) {
            if(i < mg) {mg--; cs.nx = cs.nx-1;} else {cs.mSol = cs.mSol -1;}
            cs.Ms = cs.Ms -1;
            if(i >= cs.Ms) {break;}
            for(j=i; j<cs.Ms; j++) {
                js = j - cs.Na;
                cs.lBeta[js] = cs.lBeta[js+1];
                // for(int ia=0; ia<cs.Na; ia++) {cs.a[js][ia] = cs.a[js+1][ia];}
                System.arraycopy(cs.a[js+1], 0, cs.a[js], 0, cs.Na);
                namn.ident[j] = namn.ident[j+1];
                cs.noll[j] = cs.noll[j+1];
            }
        }
        else {i++;}
    } //while (true)

    // ---- get electric charge, length of name, etc
    diag.aquSystem = false;
    for(i=0; i<cs.Ms; i++) {
        namn.nameLength[i] = getNameLength(namn.ident[i]);
        // Species for which the concentration is not to be
        // included in the Mass-Balance (for ex. the concentration
        // of "free" electrons is excluded)
        if(Util.isElectron(namn.ident[i]) ||
           Util.isWater(namn.ident[i])) {
                cs.noll[i] = true;
                diag.aquSystem = true;} // e- or H2O
        if(i < mg) { //aqueous species
            namn.z[i]=0;
            csC.logf[i] = 0;
            if(namn.ident[i].length() >4 &&
                namn.ident[i].toUpperCase().endsWith("(AQ)")) {
                    diag.aquSystem = true;}
                else { //does not end with "(aq)"
                    namn.z[i] = Util.chargeOf(namn.ident[i]);
                    if(namn.z[i] != 0) {diag.aquSystem = true;}
                } // ends with "(aq)"?
        }// if i < mg
    } // for i=0...Ms-1
    // The electric charge of two fictive species (Na+ and Cl-)
    // that are used to ensure electrically neutral aqueous solutions
    // when calculating the ionic strength and activity coefficients
    namn.z[mg] = 1;     //electroneutrality "Na+"
    namn.z[mg+1] =-1;   //electroneutrality "Na+"

    // ---- set Gaseous species to have zero conc
    //      if it is an aqueous system
    if(diag.aquSystem) {
        for(i =0; i < mg; i++) {
            if(Util.isGas(namn.ident[i]) ||
               Util.isLiquid(namn.ident[i]) ||
               Util.isWater(namn.ident[i])) {
                        cs.noll[i] = true;}
        } //for i
        } //if aquSystem

    // ---- Remove H2O among the complexes, if found
    if(diag.aquSystem) {
      for(i=cs.Na; i<cs.Ms; i++) {
        if(Util.isWater(namn.ident[i])) {
            if(i < mg) {mg--; cs.nx = cs.nx-1;} else {cs.mSol = cs.mSol -1;}
            cs.Ms = cs.Ms -1;
            if(i >= cs.Ms) {break;}
            for(j=i; j<cs.Ms; j++) {
                js = j - cs.Na;
                cs.lBeta[js] = cs.lBeta[js+1];
                //for(int ia=0; ia<cs.Na; ia++) {cs.a[js][ia] = cs.a[js+1][ia];}
                System.arraycopy(cs.a[js+1], 0, cs.a[js], 0, cs.Na);
                namn.ident[j] = namn.ident[j+1];
                cs.noll[j] = cs.noll[j+1];
            }
        } //ident[i]="H2O"
      } //for i
    } //if aquSystem

    if(dbg) {cs.printChemSystem(out);}

    // ---- Check that all reactions are charge balanced
    if(calcActCoeffs) {
        double zSum;
        boolean ok = true;
        for(i=cs.Na; i < mg; i++) {
            int ix = i - cs.Na;
            zSum = (double)(-namn.z[i]);
            for(j=0; j < cs.Na; j++) {
                zSum = zSum + cs.a[ix][j]*(double)namn.z[j];
            } //for j
            if(Math.abs(zSum) > 0.0005) {
                ok = false;
                err.format(engl,"--- Warning: %s, z=%3d, charge imbalance:%9.4f",
                            namn.ident[i],namn.z[i],zSum);
            }
        } //for i
        if(!ok) {
            if(!showErrMsgBxCancel("There are charge imbalanced reactions in the input file.",1)) {
                return false;
            }
        }
    } // if calcActCoeffs

    // ---- Check that at least there is one fuid species active
    boolean foundOne = false;
    for(i =0; i < mg; i++) {
        if(!cs.noll[i]) {foundOne = true; break;}
    } //for i
    if(!foundOne) {
        String t = "Error: There are no fluid species active ";
        if(cs.mSol > 0) {t = t.concat("(Only solids)");}
        t = t+nl+"This program can not handle such chemical systems.";
        showErrMsgBx(t,1);
        return false;
    }

    // --------------------
    //   PLOT INFORMATION
    // --------------------
    diag.pInX =0; diag.pInY = 0;
    //  pInX=0 "normal" X-axis
    //  pInX=1 pH in X-axis
    //  pInX=2 pe in X-axis
    //  pInX=3 Eh in X-axis
    if(Util.isElectron(namn.identC[diag.compX])) {
        if(diag.Eh) {diag.pInX = 3;} else {diag.pInX = 2;}
    } else if(Util.isProton(namn.identC[diag.compX])) {diag.pInX = 1;}
    if(Util.isElectron(namn.identC[diag.compY])) {
        if(diag.Eh) {diag.pInY = 3;} else {diag.pInY = 2;}
    } else if(Util.isProton(namn.identC[diag.compY])) {diag.pInY = 1;}


    // ----------------------------------------------
    //   CHECK THE CONCENTRATION FOR EACH COMPONENT
    // ----------------------------------------------

    for(int ia =0; ia < cs.Na; ia++) {
        if(dgrC.hur[ia]==1 && // T
            ia==diag.compX) {
                showErrMsgBx("Error: the concentration for component \""+namn.identC[ia]+"\" "+
                    "must vary, as it belongs to the X-axis!",1);
                return false;
                }
        if(ia==diag.compMain && dgrC.hur[ia]==1 && // T
            Math.abs(dgrC.cLow[ia]) <= 0) {
                if(!showErrMsgBxCancel("The concentration for "+nl
                        +"the main component \""+namn.identC[ia]+"\" is zero!",1)) {return false;}
                }
        if(dgrC.hur[ia] >0 && dgrC.hur[ia]<=3) { //T, TV or LTV
            if(Util.isWater(namn.identC[ia])) {
                showErrMsgBx("Error: The calculations are made for 1kg H2O"+nl+
                           "Give log(H2O-activity) instead of a total conc. for water.",1);
                return false;
           } // if water
        } //if T, TV or LTV
        if(dgrC.hur[ia] ==2 || dgrC.hur[ia] ==3 || dgrC.hur[ia] ==5) { //TV, LTV or LAV
            if(ia != diag.compX && ia != diag.compY) {
                String t;
                if(dgrC.hur[ia] ==5) {t="log(activity)";} else {t="total conc.";}
                String msg = "Error: The "+t+" is varied for \""+namn.identC[ia]+"\""+nl+
                           "   but this component is neither the component in the Y-axis ("+namn.identC[diag.compY]+"),"+nl+
                           "   nor the component in the X-axis ("+namn.identC[diag.compX]+")!";
                showErrMsgBx(msg,2);
                return false;
            }
        } //if TV, LTV or LAV
        if((dgrC.hur[ia] ==1 || dgrC.hur[ia] ==4) && //T or LA
           (ia == diag.compX || ia == diag.compY)) {
                String t, ax;
                if(dgrC.hur[ia] ==4) {t="log(activity)";} else {t="total conc.";}
                if(ia == diag.compX) {ax="X";} else {ax="Y";}
                showErrMsgBx("Error: The "+t+" of \""+namn.identC[ia]+"\""+nl+
                        "can NOT be a fixed value because this component belongs to the "+ax+"-axis !",1);
                return false;
            }
        if(dgrC.hur[ia] ==1 && // T
           Math.abs(dgrC.cLow[ia]) >100) {
                String t = String.format(engl,"Error:  For component: "+namn.identC[ia]+nl+
                        "   Tot.Conc.=%12.4g mol/kg.  This is not a reasonable value!",dgrC.cLow[ia]);
                showErrMsgBx(t,1);
                return false;
            }
        if(dgrC.hur[ia] ==4 &&  // LA
           Util.isProton(namn.identC[ia]) &&
           (dgrC.cLow[ia] <-14 || dgrC.cLow[ia] >2)) {
                String msg = String.format(engl,"Warning: In the input, you give  pH =%8.2f%n"+
                        "This value could be due to an input error.",(-dgrC.cLow[ia]));
                if(!showErrMsgBxCancel(msg,2)) {return false;}
        }
        if(dgrC.hur[ia] !=1 && dgrC.hur[ia] !=4) {//if TV, LTV or LAV
            if(dgrC.cLow[ia] == dgrC.cHigh[ia] ||
                    Math.max(Math.abs(dgrC.cLow[ia]), Math.abs(dgrC.cHigh[ia])) < 1e-15) {
                showErrMsgBx("Error:  Min-value = Max-value for component \""+namn.identC[ia]+"\"",1);
                return false;
            }
            if(dgrC.cLow[ia] > dgrC.cHigh[ia] && !reversedConcs) {
                w = dgrC.cLow[ia];
                dgrC.cLow[ia] = dgrC.cHigh[ia];
                dgrC.cHigh[ia] = w;
            }
            if(!reversedConcs && dgrC.hur[ia] ==5 && // pH/pe/EH varied - LAV
                   (Util.isProton(namn.identC[ia]) ||
                    Util.isElectron(namn.identC[ia]))) {
                w = dgrC.cLow[ia];
                dgrC.cLow[ia] = dgrC.cHigh[ia];
                dgrC.cHigh[ia] = w;
            }
            if(dgrC.hur[ia] ==5 && // LAV
                    (Util.isProton(namn.identC[ia])) &&
                     (dgrC.cLow[ia] <-14.00001 || dgrC.cLow[ia] >2.00001 ||
                      dgrC.cHigh[ia] <-14.00001 || dgrC.cHigh[ia] >2.00001)) {
                String msg = String.format(engl,"Warning: In the input, you give  pH =%8.2f to %7.2f%n"+
                        "These values could be due to an input error.",(-dgrC.cLow[ia]),(-dgrC.cHigh[ia]));
                if(!showErrMsgBxCancel(msg,2)) {return false;}
            }
            if(dgrC.hur[ia] ==2 && // TV
                  (Math.max(Math.abs(dgrC.cHigh[ia]),Math.abs(dgrC.cLow[ia]))>100)) {
                showErrMsgBx("Error:  You give  ABS(TOT.CONC.) > 100  for component: "+namn.identC[ia]+nl+
                    "This value is too high and perhaps an input error."+nl+
                    "Set the maximum ABS(TOT.CONC.) value to 100.",1);
                return false;
            }
            if(dgrC.hur[ia] ==3) { // LTV
                if((Math.min(dgrC.cLow[ia], dgrC.cHigh[ia]) < -7.0001) &&
                      (Util.isProton(namn.identC[ia]))) {
                    String msg = "Warning: You give a  LOG (TOT.CONC.) < -7  for component: "+namn.identC[ia]+nl+
                        "This value is rather low and could be due to an input error."+nl+
                        "Maybe you meant to set  LOG (ACTIVITY) < -7 ??";
                    if(!showErrMsgBxCancel(msg,2)) {return false;}
                    }
                if(Math.max(dgrC.cLow[ia], dgrC.cHigh[ia]) > 2.0001) {
                    showErrMsgBx("Error: You give a  LOG (TOT.CONC.) > 2  for component: "+namn.identC[ia]+nl+
                        "This value is too high and it could be due to an input error."+nl+
                        "Please set the LOG (TOT.CONC.) value to <=2.",1);
                    return false;
                }
            } //if LTV
        } //if TV, LTV or LAV

    } // for ia = 0... Na-1

    // ----------------
    //   OTHER CHECKS
    // ----------------

    if(cs.nx == 0) {
      for(i =0; i < cs.Na; i++) {
        if(csC.kh[i] == 2) {continue;} //only it Tot.conc. is given
        if(i == diag.compX || i == diag.compY) { //component in an axis:
          if(i >= (cs.Na-cs.solidC)) { //is it a solid?
              if(!showErrMsgBxCancel("Warning: with no complexes in the fluid phase you should not have"+nl+
              "a solid component ("+namn.identC[i]+") in an axis.",2)) {return false;}
          }
      }
      } //for i
    } //if nx=0

    // ---- Check that a total concentration is given for the main component
    //   is it a solid?                        and log(activity) given?
    if(diag.compMain >= (cs.Na - cs.solidC) && csC.kh[diag.compMain] == 2) {
        String v;
        if(dgrC.hur[diag.compMain] == 5) {v = " varied";} else {v = "";}
        String t = "Error: For the main component \""+namn.identC[diag.compMain]+"\""+nl+
                "   please give its total concentration"+v+nl+
                "   instead of its log(activity)"+v;
        showErrMsgBx(t+".",1);
        return false;
    }

    // ---- See which components have positive or negative (or both)
    //      values for the stoichiometric coefficients (a[ix][ia]-values)
    pos = new boolean[cs.Na];
    neg = new boolean[cs.Na];
    for(i =0; i < cs.Na; i++) {
        pos[i] = false; neg[i] = false;
        if(csC.kh[i] == 2) {continue;} //only it Tot.conc. is given
        if(!cs.noll[i]) { // if not "e-" and not solid component
                    pos[i] = true;}
        for(j = cs.Na; j < cs.Ms; j++) {
            if(!cs.noll[j]) {
                if(cs.a[j-cs.Na][i] >0) {pos[i] = true;}
                if(cs.a[j-cs.Na][i] <0) {neg[i] = true;}
            } // !noll
        } //for j
    } //for i
    // check POS and NEG with the Tot.Conc. given in the input
    for(i =0; i < cs.Na; i++) {
        if(csC.kh[i] == 2) {continue;} //only it Tot.conc. is given
        if((!pos[i] && !neg[i]) ) { // || cs.nx ==0
            String msg = "Error: for component \""+namn.identC[i]+"\" give Log(Activity)";
            if(dgrC.hur[i] !=1) { // not "T", that is: "TV" or "LTV"
                msg = msg+" to vary";}
            showErrMsgBx(msg,1);
            return false;
        } //if Nx =0 or (!pos[] & !neg[])
        if((pos[i] && neg[i]) ||
           (pos[i] && (dgrC.hur[i] ==3 ||  //LTV
                (dgrC.cLow[i]>0 && (Double.isNaN(dgrC.cHigh[i]) || dgrC.cHigh[i]>0)))) ||
           (neg[i] && (
                dgrC.hur[i] !=3 && //LTV
                (dgrC.cLow[i]<0 && (Double.isNaN(dgrC.cHigh[i]) || dgrC.cHigh[i]<0))))) {
            continue;
        }
        if(pos[i] || neg[i]) {
            String msg = "Error: Component \"%s\" may not have %s Tot.Conc. values.%s"+
                    "Give either  Tot.Conc. %s=0.0  or  Log(Activity)%s";
            if(!pos[i] && (dgrC.cLow[i]>0 || (!Double.isNaN(dgrC.cHigh[i]) && dgrC.cHigh[i]>0))) {
                showErrMsgBx(String.format(msg, namn.identC[i], "positive", nl,"<",nl),1);
                return false;}
            if(!neg[i] && (dgrC.cLow[i]<0 || (!Double.isNaN(dgrC.cHigh[i]) && dgrC.cHigh[i]<0))) {
                showErrMsgBx(String.format(msg, namn.identC[i], "negative", nl,">",nl),1);
                return false;}
        } //if pos or neg
    } //for i

    // OK so far. Update "nx" (=nbr of soluble complexes)
    cs.nx = mg - cs.Na;
    return true;

  } // checkInput()

//<editor-fold defaultstate="collapsed" desc="getNameLength(species)">
    private static int getNameLength(String species) {
        int nameL = Math.max(1, Util.rTrim(species).length());
        if(nameL < 3) {return nameL;}
        // Correct name length if there is a space between name and charge
        // "H +",  "S 2-",  "Q 23+"
        int sign; int ik;
        sign =-1;
        for(ik =nameL-1; ik >= 2; ik--) {
            char c = species.charAt(ik);
            if(c == '+' || c == '-' ||
               // unicode en dash or unicode minus
               c =='\u2013' || c =='\u2212') {sign = ik; break;}
            } //for ik
        if(sign <2) {return nameL;}
        if(sign < nameL-1 &&
                (Character.isLetterOrDigit(species.charAt(sign+1)))) {return nameL;}
        if(species.charAt(sign-1) == ' ')
                        {nameL = nameL-1; return nameL;}
        if(nameL >=4) {
                if(species.charAt(sign-1) >= '2' && species.charAt(sign-1) <= '9' &&
                   species.charAt(sign-2) == ' ')
                        {nameL = nameL-1; return nameL;}
        } //if nameL >=4
        if(nameL >=5) {
                if((species.charAt(sign-1) >= '0' && species.charAt(sign-1) <= '9') &&
                   (species.charAt(sign-2) >= '1' && species.charAt(sign-2) <= '9') &&
                   species.charAt(sign-3) == ' ')
                        {nameL = nameL-1;}
            } //if nameL >=5
        return nameL;
    } // getNameLength(species)
//</editor-fold>

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="dispatchArg">
/**
 * @param arg String containing a command-line argument
 * @return false if there was an error associated with the command argument
 */
private boolean dispatchArg(String arg) {
  if(arg == null) {return true;}
  out.println("Command-line argument = \""+arg+"\"");
  if(arg.equals("-?") || arg.equals("/?") || arg.equals("?")) {
      out.println("Usage:   PREDOM  [-command=value]");
      printInstructions(out);
      if(this.isVisible()) {jTabbedPane.setSelectedComponent(jScrollPaneMessg);}
      return true;} //if args[] = "?"

  String msg = null;
  while(true) {
    if(arg.length() >3) {
        String arg0 = arg.substring(0, 2).toLowerCase();
        if(arg0.startsWith("-d") || arg0.startsWith("/d")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String f = arg.substring(3);
                if(f.startsWith("\"") && f.endsWith("\"")) { //remove enclosing quotes
                    f = f.substring(1, f.length()-1);
                }
                if(!f.toLowerCase().endsWith(".dat")) {f = f.concat(".dat");}
                inputDataFile = new java.io.File(f);
                setPathDef(inputDataFile);
                //get the complete file name
                String fil;
                try {fil = inputDataFile.getCanonicalPath();}
                catch (java.io.IOException e) {
                    try{fil = inputDataFile.getAbsolutePath();}
                    catch (Exception e1) {fil = inputDataFile.getPath();}
                }
                inputDataFile = new java.io.File(fil);
                if(dbg){out.println("Data file: "+inputDataFile.getAbsolutePath());}
                if(!doNotExit) {consoleOutput = true;}
                if(!readDataFile(inputDataFile)) {
                    //-- error reading data file
                    if(this.isVisible()) {jTabbedPane.setSelectedComponent(jScrollPaneMessg);}
                    inputDataFileInCommandLine = false;
                    return false;
                }
                else {showTheInputFileName(inputDataFile);}
                inputDataFileInCommandLine = true;
                return true; // no error
            }// = or :
        } else if(arg0.startsWith("-p") || arg0.startsWith("/p")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String f = arg.substring(3);
                if(f.startsWith("\"") && f.endsWith("\"")) { //remove enclosing quotes
                    f = f.substring(1, f.length()-1);
                }
                if(!f.toLowerCase().endsWith(".plt")) {f = f.concat(".plt");}
                outputPltFile = new java.io.File(f);
                if(dbg){out.println("Plot file: "+outputPltFile.getAbsolutePath());}
                jTextFieldPltFile.setText(outputPltFile.getName());
                return true;
            }// = or :
        } else if(arg0.startsWith("-i") || arg0.startsWith("/i")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
            try {ionicStrength = Double.parseDouble(t);
                ionicStrength = Math.max(-1,Math.min(1000,ionicStrength));
                if(ionicStrength < 0) {ionicStrength = -1;}
                jTextFieldIonicStgr.setText(String.valueOf(ionicStrength));
                if(Math.abs(ionicStrength) > 1e-10) {calcActCoeffs = true;}
                if(dbg) {out.println("Ionic strength = "+ionicStrength);}
                return true;
            } //try
            catch (NumberFormatException nfe) {
              msg = "Wrong numeric format for ionic strength in text \""+t+"\"";
              ionicStrength = 0;
              jTextFieldIonicStgr.setText(String.valueOf(ionicStrength));
              break;
            } //catch
            }// = or :
        } else if(arg0.startsWith("-t") || arg0.startsWith("/t")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {temperature_InCommandLine = Double.parseDouble(t);
                    temperature_InCommandLine = Math.min(1000,Math.max(temperature_InCommandLine,-10));
                    jTextFieldT.setText(String.valueOf(temperature_InCommandLine));
                    if(dbg) {out.println("Temperature = "+temperature_InCommandLine);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for temperature in text \""+t+"\"";
                  temperature_InCommandLine = Double.NaN;
                  break;
                } //catch
            }// = or :
        } else if(arg0.startsWith("-h") || arg0.startsWith("/h")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {tHeight = Double.parseDouble(t);
                    tHeight = Math.min(10,Math.max(tHeight, 0.3));
                    jScrollBarHeight.setValue(Math.round((float)(10*tHeight)));
                    if(dbg) {out.println("Height factor for texts in diagrams = "+tHeight);}
                    return true;
                } //try
                catch (NumberFormatException nfe) {
                    msg = "Wrong numeric format for text height in \""+t+"\"";
                    tHeight =1;
                    jScrollBarHeight.setValue(Math.round((float)(10*tHeight)));
                    break;
                } //catch
            }// = or :
        } // if starts with "-h"

        if(arg0.startsWith("-m") || arg0.startsWith("/m")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {actCoeffsModel_InCommandLine = Integer.parseInt(t);
                    actCoeffsModel_InCommandLine = Math.min(jComboBoxModel.getItemCount()-1,
                            Math.max(0,actCoeffsModel_InCommandLine));
                    jComboBoxModel.setSelectedIndex(actCoeffsModel_InCommandLine);
                    if(dbg) {out.println("Activity coeffs. method = "+actCoeffsModel_InCommandLine);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                msg = "Wrong numeric format for activity coeff. model in \""+t+"\"";
                actCoeffsModel_InCommandLine = actCoeffsModelDefault;
                jComboBoxModel.setSelectedIndex(actCoeffsModelDefault);
                break;
                } //catch
            }// = or :
        } else if(arg0.startsWith("-n") || arg0.startsWith("/n")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {nSteps = Integer.parseInt(t);
                    nSteps = Math.min(NSTP_MAX, Math.max(nSteps, NSTP_MIN));
                    jScrollBarNbrPoints.setValue(nSteps);
                    if(dbg) {out.println("Nbr calc. steps in diagram = "+(nSteps)+" (nbr. points = "+(nSteps+1)+")");}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                msg = "Wrong numeric format for number of calculation steps in \""+t+"\"";
                nSteps =NSTP_DEF;
                jScrollBarNbrPoints.setValue(nSteps);
                break;
                } //catch
            }// = or :
        } // if starts with "-n"
    } // if length >3

    if(arg.length() >5) {
        String arg0 = arg.substring(0, 5).toLowerCase();
        if(arg0.startsWith("-tol") || arg0.startsWith("/tol")) {
            if(arg.charAt(4) == '=' || arg.charAt(4) == ':') {
                String t = arg.substring(5);
                double w;
                try {w = Double.parseDouble(t);
                    tolHalta = Math.min(1e-2,Math.max(w, 1e-9));
                    set_tol_inComboBox();
                    if(dbg) {out.println("Max tolerance in HaltaFall = "+tolHalta);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                msg = "Wrong numeric format for tolerance in \""+t+"\"";
                tolHalta = Chem.TOL_HALTA_DEF;
                set_tol_inComboBox();
                break;
                } //catch
            }// = or :
        } // if starts with "-tol"
    }
    if(arg.length() >6) {
        String arg0 = arg.substring(0, 5).toLowerCase();
        if(arg0.startsWith("-dbgh") || arg0.startsWith("/dbgh")) {
            if(arg.charAt(5) == '=' || arg.charAt(5) == ':') {
                String t = arg.substring(6);
                try {dbgHalta = Integer.parseInt(t);
                    dbgHalta = Math.min(6, Math.max(dbgHalta, 0));
                    if(dbg) {out.println("Debug printout level in HaltaFall = "+dbgHalta);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                msg = "Wrong numeric format for HaltaFall debug level \""+t+"\" (setting default:"+Chem.DBGHALTA_DEF+")";
                dbgHalta = Chem.DBGHALTA_DEF;
                break;
                } //catch
            }// = or :
        } // if starts with "-dbgh"
    } //if length >6

    if(arg.equalsIgnoreCase("-ph") || arg.equalsIgnoreCase("/ph")) {
            neutral_pH = true;
            jCheckBoxMenuPredomDebug.setSelected(dbg);
            out.println("Add neutral pH dash-line to plot.");
            return true;
    } else if(arg.equalsIgnoreCase("-aqu") || arg.equalsIgnoreCase("/aqu")) {
            aqu = true;
            jCheckBoxMenuPredomDebug.setSelected(dbg);
            out.println("Plot only aqueous species; areas for solids not shown.");
            return true;
    } else if(arg.equalsIgnoreCase("-dbg") || arg.equalsIgnoreCase("/dbg")) {
            dbg = true;
            jCheckBoxMenuPredomDebug.setSelected(dbg);
            out.println("Debug printout = true");
            return true;
    } else if(arg.equalsIgnoreCase("-rev") || arg.equalsIgnoreCase("/rev")) {
            reversedConcs = true;
            if(dbg) {out.println("Allow reversed ranges in axes");}
            return true;
    } else if(arg.equalsIgnoreCase("-keep") || arg.equalsIgnoreCase("/keep")) {
            out = outPrintStream; // direct messages to tabbed pane
            err = errPrintStream; // direct error messages to tabbed pane
            doNotExit = true;
            if(dbg) {out.println("Do not close window after calculations");}
            return true;
    } else if(arg.equalsIgnoreCase("-nostop") || arg.equalsIgnoreCase("/nostop")) {
            doNotStop = true;
            if(dbg) {out.println("Do not show message boxes");}
            return true;
    }
    break;
  } //while

  if(msg == null) {msg = "Error: can not understand command-line argument:"+nl+
            "  \""+arg+"\""+nl+"For a list of possible commands type:  PREDOM  -?";}
  else {msg = "Command-line argument \""+arg+"\":"+nl+msg;}
  out.flush();
  err.println(msg);
  err.flush();
  printInstructions(out);
  if(!doNotStop) {
      if(!this.isVisible()) {this.setVisible(true);}
      javax.swing.JOptionPane.showMessageDialog(this,msg,progName,
            javax.swing.JOptionPane.ERROR_MESSAGE);
  }
  if(this.isVisible()) {jTabbedPane.setSelectedComponent(jScrollPaneMessg);}
  return false;
} // dispatchArg(arg)
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="diverse Methods">

  //<editor-fold defaultstate="collapsed" desc="showActivityCoefficientControls">
/** show/hide the activity coefficient controls in the window */
  private void showActivityCoefficientControls(boolean show) {
    if(show) {
        jTextFieldIonicStgr.setEnabled(true);
        jLabelIonicStr.setEnabled(true);
        jLabelIonicStr.setText("<html>ionic <u>S</u>trength</html>");
        jLabelIonicStrM.setEnabled(true);
        jLabelModel.setEnabled(true);
        jLabelModel.setText("<html>activity <u>C</u>officient model:<html>");
        jComboBoxModel.setEnabled(true);
    } else {
        jLabelIonicStr.setEnabled(false);
        jLabelIonicStr.setText("ionic Strength");
        jLabelIonicStrM.setEnabled(false);
        jTextFieldIonicStgr.setEnabled(false);
        jLabelModel.setText("activity cofficient model:");
        jLabelModel.setEnabled(false);
        jComboBoxModel.setEnabled(false);
    }
  } //showActivityCoefficientControls(show)
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="disable/restore Menus">
  /** disable menus and buttons during calculations */
  private void disableMenus() {
    //if(this.isVisible()) {
      jMenuFileOpen.setEnabled(false);
      jMenuFileMakeD.setEnabled(false);
      jMenuDebug.setEnabled(true);
      jCheckBoxMenuPredomDebug.setEnabled(false);
      jMenuSave.setEnabled(false);
      jMenuCancel.setEnabled(true);
      jTabbedPane.setSelectedComponent(jScrollPaneMessg);
      jLabelData.setText("input data file:");
      jLabelData.setEnabled(false);
      jTextFieldDataFile.setEnabled(false);
      jLabelPltFile.setText("plot file name:");
      jLabelPltFile.setEnabled(false);
      jTextFieldPltFile.setEnabled(false);

      jLabelNbrPText.setText("Nbr of calcs. steps:");
      jLabelNbrPText.setEnabled(false);
      jLabelPointsNbr.setEnabled(false);
      jScrollBarNbrPoints.setEnabled(false);

      jLabelHeight.setText("height of text in diagram:");
      jLabelHeight.setEnabled(false);
      jLabelHD.setEnabled(false);
      jScrollBarHeight.setEnabled(false);

      jCheckActCoeff.setText("Activity coefficient calculations");
      jCheckActCoeff.setEnabled(false);
      showActivityCoefficientControls(false);
      showTemperature();
      jLabelTol.setEnabled(false);
      jComboBoxTol.setEnabled(false);

      jCheckReverse.setText("allow Reversed min. and max. axes limits");
      jCheckReverse.setEnabled(false);
      jButtonDoIt.setText("make the Diagram");
      jButtonDoIt.setEnabled(false);
    //} //if visible
  } //disableMenus()
/** enable menus and buttons after the calculations are finished
 * and the diagram is displayed
 * @param allowMakeDiagram if true then the button and menu to make diagrams are enabled,
 * they are disabled otherwise */
  private void restoreMenus(boolean allowMakeDiagram) {
    //if(this.isVisible()) {
      jMenuFileOpen.setEnabled(true);
      if(allowMakeDiagram) {
        jButtonDoIt.setText("<html>make the <u>D</u>iagram</html>");
      } else {
        jButtonDoIt.setText("make the Diagram");
      }
      jButtonDoIt.setEnabled(allowMakeDiagram);
      jMenuFileMakeD.setEnabled(allowMakeDiagram);
      jMenuDebug.setEnabled(true);
      jCheckBoxMenuPredomDebug.setEnabled(true);
      jMenuSave.setEnabled(true);
      jMenuCancel.setEnabled(false);
      jLabelData.setEnabled(true);
      jTextFieldDataFile.setEnabled(true);
      jLabelPltFile.setText("<html>p<u>l</u>ot file name:</html>");
      jLabelPltFile.setEnabled(true);
      jTextFieldPltFile.setEnabled(true);

      jLabelNbrPText.setText("<html><u>N</u>br of calc. steps:</html>");
      jLabelNbrPText.setEnabled(true);
      jLabelPointsNbr.setEnabled(true);
      jScrollBarNbrPoints.setEnabled(true);

      jLabelHeight.setText("<html>h<u>e</u>ight of text in diagram:</html>");
      jLabelHeight.setEnabled(true);
      jLabelHD.setEnabled(true);
      jScrollBarHeight.setEnabled(true);

      jCheckActCoeff.setText("<html><u>A</u>ctivity coefficient calculations</html>");
      jCheckActCoeff.setEnabled(true);
      showActivityCoefficientControls(jCheckActCoeff.isSelected());
      showTemperature();

      jLabelPltFile.setText("plot file name");
      jLabelPltFile.setEnabled(false);
      jTextFieldPltFile.setText(null);
      jTextFieldPltFile.setEnabled(false);
      //jTextFieldDataFile.setText("");

      jLabelTol.setEnabled(true);
      jComboBoxTol.setEnabled(true);
      jCheckReverse.setText("<html>allow <u>R</u>eversed min. and max. axes limits</html>");
      jCheckReverse.setEnabled(true);
      jLabelStatus.setText("waiting...");
      jLabelProgress.setText(" ");
    //} //if visible
  } //restoreMenus()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="readDataFile_hadError">
/** enable controls and disable diagram if an error is found when
 * reading the input file */
  private void readDataFile_hadError() {
    out.println("--- Error(s) reading the input file ---");
    if(this.isVisible()) {
        jTabbedPane.setTitleAt(2, "Diagram");
        jTabbedPane.setEnabledAt(2, false); //disable the diagram
        restoreMenus(false);
        jTabbedPane.setSelectedComponent(jScrollPaneMessg);
    }
  } // readDataFile_hadError()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="set_tol_inComboBox">
/** find the closest item in the tolerances combo box and select it */
  private void set_tol_inComboBox() {
    double w0, w1;
    int listItem =0;
    int listCount = jComboBoxTol.getItemCount();
    for(int i =1; i < listCount; i++) {
      w0 = Double.parseDouble(jComboBoxTol.getItemAt(i-1).toString());
      w1 = Double.parseDouble(jComboBoxTol.getItemAt(i).toString());
      if(tolHalta >= w0 && i==1) {listItem = 0; break;}
      if(tolHalta <= w1 && i==(listCount-1)) {listItem = (listCount-1); break;}
      if(tolHalta < w0 && tolHalta >=w1) {
        if(Math.abs(tolHalta-w0) < Math.abs(tolHalta-w1)) {
            listItem = i-1;
        } else {
            listItem = i;
        }
        break;
      }
    } //for i
    jComboBoxTol.setSelectedIndex(listItem);
  } //set_tol_inComboBox()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="end_program">
  private void end_program() {
      if(dbg) {out.println("--- end_program()");}
      if(!finishedCalculations && !quitConfirm(this)) {return;}
      programEnded = true;
      this.notify_All();
      this.dispose();
      if(helpAboutFrame != null) {
          helpAboutFrame.closeWindow();
          helpAboutFrame = null;
      }
  } // end_program()
//</editor-fold>

  private synchronized void notify_All() {this.notifyAll();}

  private synchronized void synchWaitCalcs() {
      while(!finishedCalculations) {
          try {wait();} catch(InterruptedException ex) {}
      }
  }
  private synchronized void synchWaitProgramEnded() {
      while(!programEnded) {
          try {wait();} catch(InterruptedException ex) {}
      }
  }

//<editor-fold defaultstate="collapsed" desc="isCharOKforNumberInput">
  /** @param key a character
   * @return true if the character is ok, that is, it is either a number,
   * or a dot, or a minus sign, or an "E" (such as in "2.5e-6") */
  private boolean isCharOKforNumberInput(char key) {
        return Character.isDigit(key)
                || key == '-' || key == '+' || key == '.' || key == 'E' || key == 'e';
  } // isCharOKforNumberInput(char)
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="quitConfirm">
  private boolean quitConfirm(javax.swing.JFrame c) {
    boolean q = true;
    if(!doNotStop) {
      Object[] options = {"Cancel", "STOP"};
      int n = javax.swing.JOptionPane.showOptionDialog (c,
                "Do you really want to stop the calculations?",
                progName, javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.ERROR_MESSAGE, null, options, null);
      q = n == javax.swing.JOptionPane.NO_OPTION;
    } //not "do not stop":
    if(q) {
      if(h != null) {h.haltaCancel();}
      finishedCalculations = true;
      this.notify_All();
    }
    return q;
  } // quitConfirm(JFrame)
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="showTheInputFileName">
  /** Show the input data file name in the JFrame (window) */
  private void showTheInputFileName(java.io.File dataFile) {
    showTemperature();
    jTextFieldDataFile.setText(dataFile.getAbsolutePath());
    jLabelPltFile.setEnabled(true);
    jLabelPltFile.setText("<html>p<u>l</u>ot file name:</html>");
    jTextFieldPltFile.setEnabled(true);
    jMenuFileMakeD.setEnabled(true);
    jButtonDoIt.setEnabled(true);
    jButtonDoIt.setText("<html>make the <u>D</u>iagram</html>");
    jButtonDoIt.requestFocusInWindow();
  } // showTheInputFileName()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="setPathDef">

  /** Sets the variable "pathDef" to the path of a file.
   * Note that "pathDef" may end with the file separator character, e.g. "D:\"
   * @param f File */
  private void setPathDef(java.io.File f) {
    if(pathDef == null) {pathDef = new StringBuffer();}
    java.net.URI uri;
    if(f != null) {
        if(!f.getAbsolutePath().contains(SLASH)) {
            // it is a bare file name, without a path
            if(pathDef.length()>0) {return;}
        }
        try{uri = f.toURI();}
        catch (Exception ex) {uri = null;}
    } else {uri = null;}
    if(pathDef.length()>0) {pathDef.delete(0, pathDef.length());}
    if(uri != null) {
        if(f != null && f.isDirectory()) {
          pathDef.append((new java.io.File(uri.getPath())).toString());
        } else {
          pathDef.append((new java.io.File(uri.getPath())).getParent().toString());
        } //directory?
    } else { //uri = null:  set Default Path = Start Directory
        java.io.File currDir = new java.io.File("");
        try {pathDef.append(currDir.getCanonicalPath());}
        catch (java.io.IOException e) {
          try{pathDef.append(System.getProperty("user.dir"));}
          catch (Exception e1) {pathDef.append(".");}
        }
    } //uri = null
  } // setPathDef(File)

  /** Set the variable "pathDef" to the path of a file name.
   * Note that "pathDef" may end with the file separator character, e.g. "D:\"
   * @param fName String with the file name */
  private void setPathDef(String fName) {
    java.io.File f = new java.io.File(fName);
    setPathDef(f);
  }

  /** Set the variable "pathDef" to the user directory ("user.home", system dependent) */
  private void setPathDef() {
    String t = System.getProperty("user.home");
    setPathDef(t);
  }

// </editor-fold>

/*
    static void pause() {
    // Defines the standard input stream
    java.io.BufferedReader stdin =
        new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        System.out.print ("Press Enter");
        System.out.flush();
        try{String txt = stdin.readLine();}
        catch (java.io.IOException ex) {System.err.println(ex.toString());}
    }
*/
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="millisToShortDHMS">
/** converts time (in milliseconds) to human-readable format "&lt;dd&gt;hh:mm:ss"
 * @param duration (in milliseconds)
 * @return  */
  public static String millisToShortDHMS(long duration) {
    //adapted from http://www.rgagnon.com
    String res;
    int millis = (int)(duration % 1000);
    duration /= 1000;
    int seconds = (int) (duration % 60);
    duration /= 60;
    int minutes = (int) (duration % 60);
    duration /= 60;
    int hours = (int) (duration % 24);
    int days = (int) (duration / 24);
    if (days == 0) {
      res = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds,millis);
    } else {
      res = String.format("%dd%02d:%02d:%02d", days, hours, minutes, seconds);
    }
    return res;
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="setCursor">
  private void setCursorWait() {
    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
    jTextAreaA.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
  }
  private void setCursorDef() {
    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    jTextAreaA.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
  }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="FilteredStreams">
private class errFilteredStreamPredom extends java.io.FilterOutputStream {
    private int n=0;
    public errFilteredStreamPredom(java.io.OutputStream aStream) {
        super(aStream);
      } // constructor
    @Override
    public void write(byte b[]) throws java.io.IOException {
        String aString = new String(b);
        jTabbedPane.setTitleAt(1, "<html><u>M</u>essages</html>");
        jTabbedPane.setEnabledAt(1, true);
        jTabbedPane.setSelectedComponent(jScrollPaneMessg);
        jTextAreaA.append(aString);
        jTextAreaA.setSelectionStart(Integer.MAX_VALUE);
        if(n%1000 == 0) {
            try{Thread.sleep(1);} catch (InterruptedException ex) {}
            n=0;
        } else {n++;}
    }
    @Override
    public void write(byte b[], int off, int len) throws java.io.IOException {
        String aString = new String(b , off , len);
        jTabbedPane.setTitleAt(1, "<html><u>M</u>essages</html>");
        jTabbedPane.setEnabledAt(1, true);
        jTabbedPane.setSelectedComponent(jScrollPaneMessg);
        jTextAreaA.append(aString);
        jTextAreaA.setSelectionStart(Integer.MAX_VALUE);
        if(n%1000 == 0) {
            try{Thread.sleep(1);} catch (InterruptedException ex) {}
            n=0;
        } else {n++;}
    } // write
    } // class errFilteredStreamPredom

private class outFilteredStreamPredom extends java.io.FilterOutputStream {
    public outFilteredStreamPredom(java.io.OutputStream aStream) {
      super(aStream);
    } // constructor
    @Override
    public void write(byte b[]) throws java.io.IOException {
        String aString = new String(b);
        jTabbedPane.setTitleAt(1, "<html><u>M</u>essages</html>");
        jTabbedPane.setEnabledAt(1, true);
        jTextAreaA.append(aString);
        jTextAreaA.setSelectionStart(Integer.MAX_VALUE);
    }
    @Override
    public void write(byte b[], int off, int len) throws java.io.IOException {
        String aString = new String(b , off , len);
        jTabbedPane.setTitleAt(1, "<html><u>M</u>essages</html>");
        jTabbedPane.setEnabledAt(1, true);
        jTextAreaA.append(aString);
        jTextAreaA.setSelectionStart(Integer.MAX_VALUE);
    } // write
    } // class outFilteredStreamPredom

//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="findTopSpecies()">
/** get the value of <code>topSpecies</code> */
private void findTopSpecies() {
  //if(dbg) {out.println("--- findTopSpecies()");}
  double topConc;
  double topConcSolid;
  if(diag.compMain < (cs.Na - cs.solidC) && !cs.noll[diag.compMain]) {
    topConc = csC.C[diag.compMain];
    topSpecies = diag.compMain;
  } else {topConc = 0; topSpecies = -1;}
  double w;
  int nIons = cs.Na + cs.nx;
  for (int i = cs.Na; i < nIons; i++) {
    w = cs.a[i-cs.Na][diag.compMain];
    if(Math.abs(w) < 0.00001) {continue;}
    w = w * csC.C[i];
    if(w <= topConc) {continue;}
    topConc = w;
    topSpecies = i;
  } //for i
  if(aqu) {return;}
  //plot also predominance areas for solids
  topConcSolid = Double.MIN_VALUE;
  if(diag.oneArea >= (cs.Na+cs.nx)) { //if only one area is plotted
      w = cs.a[diag.oneArea][diag.compMain]*csC.C[diag.oneArea];
      if(w > topConcSolid) {topSpecies = diag.oneArea;}
      return;
  } //if only one area
  for (int i = nIons; i < cs.Ms; i++) {
    w = cs.a[i-cs.Na][diag.compMain]*csC.C[i];
    if(w < topConcSolid) {continue;}
    topSpecies = i;
    //che if the amount of "compMain" is almost the same for two solids (<0.001% diff)
    if(Math.abs(w/topConcSolid)-1 <= 0.00001) {topSpecies = -2;}
    topConcSolid = w;
  } //for i
} //findTopSpecies()
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="getTheInputFileName">
    /** Get an input data file name from the user
     * using an Open File dialog */
    private void getTheInputFileName() {
        jTextFieldDataFile.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        setCursorWait();
        if(pathDef == null) {setPathDef();}
        fc.setMultiSelectionEnabled(false);
        fc.setCurrentDirectory(new java.io.File(pathDef.toString()));
        fc.setDialogTitle("Select a data file:");
        fc.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
        fc.setAcceptAllFileFilterUsed(true);
        javax.swing.LookAndFeel defLaF = javax.swing.UIManager.getLookAndFeel();
        try {javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());}
        catch (Exception ex) {}
        fc.updateUI();
        jTextFieldDataFile.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setCursorDef();
        fc.setFileFilter(filterDat);
        int returnVal = fc.showOpenDialog(this);
        // reset the look and feel
        try {javax.swing.UIManager.setLookAndFeel(defLaF);}
        catch (javax.swing.UnsupportedLookAndFeelException ex) {}
        if(returnVal == javax.swing.JFileChooser.APPROVE_OPTION) {
            if(eraseTextArea) {
                jTextAreaA.selectAll();
                jTextAreaA.replaceRange("", 0, jTextAreaA.getSelectionEnd());
            }
            inputDataFile = fc.getSelectedFile();
            setPathDef(fc.getCurrentDirectory());
            if(readDataFile(inputDataFile)) {
                showTheInputFileName(inputDataFile);
                outputPltFile = null;
                String txt = inputDataFile.getName();
                String plotFileN = txt.substring(0,txt.length()-3).concat("plt");
                jTextFieldPltFile.setText(plotFileN);
                jTabbedPane.setSelectedComponent(jPanelParameters);
                jTabbedPane.setTitleAt(2, "Diagram");
                jTabbedPane.setEnabledAt(2, false);
                jMenuFileOpen.setEnabled(true);
                jMenuFileMakeD.setEnabled(true);
                jButtonDoIt.setEnabled(true);
                jButtonDoIt.setText("<html>make the <u>D</u>iagram</html>");
                jButtonDoIt.requestFocusInWindow();
            } // if readDataFile
            else {return;}
        } // if returnVal = JFileChooser.APPROVE_OPTION
        jTabbedPane.setSelectedComponent(jPanelParameters);
        jTabbedPane.requestFocusInWindow();
        jButtonDoIt.requestFocusInWindow();
        jCheckReverse.setText("allow Reversed min. and max. axes limits");
        jCheckReverse.setEnabled(false);
    } // getTheInputFileName()
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="paintDiagrPanel">
/** used when constructing the jPanelDiagram:
 * <pre>jPanelDiagram = new javax.swing.JPanel() {
 *    public void paint(java.awt.Graphics g)
 *         {
 *             super.paint(g);
 *             paintDiagrPanel(g);
 *         }
 *     };</pre>
 */
    private void paintDiagrPanel(java.awt.Graphics g) {
        java.awt.Graphics2D g2D = (java.awt.Graphics2D)g;
        if(dd != null) {
            diagrPaintUtil.paintDiagram(g2D, jPanelDiagram.getSize(), dd, false);
        }
    }
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="printInstructions(out)">
  private static void printInstructions(java.io.PrintStream out) {
    if(out == null) {out = System.out;}
    out.flush();
    out.println("Possible commands are:"+nl+
    "  -aqu     (plot only aqueous species; areas for solids not shown)"+nl+
    "  -d=data-file-name  (input file name)"+nl+
    "  -dbg     (output debug information)"+nl+
    "  -dbgH=n  (level for debug output from HaltaFall"+nl+
    "            in the first calculation point; default ="+Chem.DBGHALTA_DEF+")"+nl+
    "  -d=data-file-name  (input file name)"+nl+
    "  -h=nbr   (height factor for labels in the plot)"+nl+
    "  -i=nbr   (ionic strength (the equil. constants are"+nl+
    "            assumed for I=0). Requires a temperature."+nl+
    "            Enter \"-i=-1\" to calculate I at each point)"+nl+
    "  -keep    (window open at the end)"+nl+
    "  -m=nbr   (model to calculate activity coefficients:"+nl+
    "            0 = Davies eqn; 1 = SIT; 2 = simplified HKF; default =2)"+nl+
    "  -n=nbr   (calculation steps along"+nl+ //nStep
    "            each axes; "+(NSTP_MIN)+" to "+(NSTP_MAX)+")"+nl+
    "  -nostop  (do not stop for warnings)"+nl+
    "  -p=output-plot-file-name"+nl+
    "           (note: diagram not displayed after the calculation)"+nl+
    "  -pH      (show neutral pH as a dash line)"+nl+
    "  -rev     (do not reverse the input"+nl+
    "            min. and max. limits in x-axis)"+nl+
    "  -t=nbr   (temperature in °C, ignored if not needed)"+nl+
    "  -tol=nbr (tolerance when solving mass-balance equations in Haltafall,"+nl+
    "            0.01 >= nbr >= 1e-9; default ="+Chem.TOL_HALTA_DEF+")"+nl+
    "Enclose file names with double quotes (\"\") it they contain blank space."+nl+
    "Example:   PREDOM \"/d=Fe 25\" -t:25 -i=-1 \"-p:plt\\Fe 25\" -n=200");
  } //printInstructions(out)
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="readDataFile">
private boolean readDataFile(java.io.File dataFile) {
    if(dbg) {out.println("--- readDataFile("+dataFile.getAbsolutePath()+")");}
    String msg;
    //--- check the name
    if(!dataFile.getName().toLowerCase().endsWith(".dat")) {
        msg = "File: \""+dataFile.getName()+"\""+nl+
                "Error: data file name must end with \".dat\"";
        showErrMsgBx(msg,1);
        return false;
    }
    if(dataFile.getName().length() <= 4) {
        msg = "File: \""+dataFile.getName()+"\""+nl+
                "Error: file name must have at least one character";
        showErrMsgBx(msg,1);
        return false;
    }
    String dataFileN = null;
    try {dataFileN = dataFile.getCanonicalPath();} catch (java.io.IOException ex) {}
    if(dataFileN == null) {
        try {dataFileN = dataFile.getAbsolutePath();}
        catch (Exception ex) {dataFileN = dataFile.getPath();}
    }
    dataFile = new java.io.File(dataFileN);
    //
    //--- create a ReadDataLib instance
    try {rd = new ReadDataLib(dataFile);}
    catch (ReadDataLib.DataFileException ex) {
        showErrMsgBx(ex.getMessage(),1);
        if(rd != null) {
            try {rd.close();}
            catch (ReadDataLib.ReadDataLibException ex2) {showErrMsgBx(ex2);}
        }
        return false;
    }
    msg = "Reading input data file \""+dataFile+"\"";
    out.println(msg);
    if(consoleOutput) {System.out.println(msg);}
    //--- read the chemical system (names, equilibrium constants, stoichiometry)
    boolean warn = false; // throw an exception for missing plot data
    try {ch = null;
        ch = ReadChemSyst.readChemSystAndPlotInfo(rd, dbg, warn, out);
    }
    catch (ReadChemSyst.ConcDataException ex) {
        ch = null; showMsg(ex);
    }
    catch (ReadChemSyst.DataLimitsException ex) {
        ch = null; showMsg(ex);
    }
    catch (ReadChemSyst.PlotDataException ex) {
        ch = null; showMsg(ex);
    }
    catch (ReadChemSyst.ReadDataFileException ex) {
        ch = null; showMsg(ex);
    }
    if(ch == null) {
        msg = "Error while reading data file \""+dataFile.getName()+"\"";
        showErrMsgBx(msg,1);
        try {rd.close();}
        catch (ReadDataLib.ReadDataLibException ex) {showMsg(ex);}
        readDataFile_hadError();
        return false;
    }
    if(ch.diag.plotType != 0) {
        msg = "Error: data file \""+dataFile.getName()+"\""+nl;
        if(ch.diag.plotType >= 1 && ch.diag.plotType <=8) {msg = msg +
                "does NOT contain information for a Predominance Area Diagram."+nl+
                "Run program SED instead.";}
        else {msg = msg + "contains erroneous plot information.";}
        showErrMsgBx(msg,1);
        try {rd.close();}
        catch (ReadDataLib.ReadDataLibException ex) {showMsg(ex);}
        readDataFile_hadError();
        return false;
    }
    //
    //--- get a temperature:
    double t_d, w;
    try {w = rd.getTemperature();} // temperature written as a comment in the data file?
    catch (ReadDataLib.DataReadException ex) {showErrMsgBx(ex); w = Double.NaN;}
    if(!Double.isNaN(w)) {
      t_d = w;
      if(!Double.isNaN(temperature_InCommandLine)) {
        // temperature also given in command line
        if(Math.abs(t_d - temperature_InCommandLine)>0.001) { // difference?
          msg = String.format(engl,"Warning: temperature in data file =%6.2f,%s",t_d,nl);
          msg = msg + String.format(engl,
                  "   but in the command line t=%6.2f!%s",
                  temperature_InCommandLine,nl);
          msg = msg + String.format(engl,"t=%6.2f will be used.",temperature_InCommandLine);
          showErrMsgBx(msg,2);
          t_d = temperature_InCommandLine;
        } // temperatures differ
      }  // temperature also given
      jTextFieldT.setText(String.valueOf(t_d));
    } // temperature written in data file

    try {rd.close();}
    catch (ReadDataLib.ReadDataLibException ex) {showMsg(ex);}
    msg = "Finished reading the input data file.";
    out.println(msg);
    if(consoleOutput) {System.out.println(msg);}
    //--- set the references pointing to the instances of the storage classes
    cs = ch.chemSystem;
    csC = cs.chemConcs;
    namn = cs.namn;
    dgrC = ch.diagrConcs;
    diag = ch.diag;

    //---- Set the calculation instructions for HaltaFall
    // Concentration types for each component:
    //    hur =1 for "T" (fixed Total conc.)
    //    hur =2 for "TV" (Tot. conc. Varied)
    //    hur =3 for "LTV" (Log(Tot.conc.) Varied)
    //    hur =4 for "LA" (fixed Log(Activity) value)
    //    hur =5 for "LAV" (Log(Activity) Varied)</pre>
    for(int j =0; j < cs.Na; j++) {
        if(dgrC.hur[j] >3)
            {csC.kh[j]=2;} //kh=2 log(activity) is given
                            //The Mass Balance eqn. has to be solved
        else {csC.kh[j]=1;} //kh=1 Tot.Conc. is given
                            //The Tot.Conc. will be calculated
    }

    // --------------------------
    // ---  Check concs. etc  ---
    // --------------------------
    if(!checkInput()) {
        ch = null; cs = null; csC = null; namn = null; dgrC = null; diag = null;
        readDataFile_hadError();
        return false;}

    if(cs.jWater >=0 && dbg) {
        out.println("Water (H2O) is included: all concentrations are in \"mol/(1 kg H2O)\".");
    }

    return true;
} //readDataFile()
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="Temperature & Ionic Strength">
    private void showTemperature() {
    if(!jCheckActCoeff.isEnabled() ||
            ((!calcActCoeffs || ionicStrength ==0) &&
             (diag == null || (diag !=null && diag.pInX !=3 && diag.pInY !=3 && !diag.Eh)))
            ) {
        jLabelT.setText("temperature");
        jLabelT.setEnabled(false);
        jLabelTC.setEnabled(false);
        jTextFieldT.setEnabled(false);}
    else {
        jLabelT.setText("<html>tempe<u>r</u>ature</html>");
        jLabelT.setEnabled(true);
        jLabelTC.setEnabled(true);
        jTextFieldT.setEnabled(true);}
    } //showTemperature()

    private void validateIonicStrength() {
        if(jTextFieldIonicStgr.getText().length() <=0) {return;}
        try{
            ionicStrength = readIonStrength();
            ionicStrength = Math.min(1000,Math.max(ionicStrength, -1000));
            if(ionicStrength < 0) {ionicStrength = -1;}
            jTextFieldIonicStgr.setText(String.valueOf(ionicStrength));
            showTemperature();
        } //try
        catch (NumberFormatException nfe) {
            String msg = "Wrong numeric format"+nl+nl+"Please enter a floating point number.";
            showErrMsgBx(msg,1);
            jTextFieldIonicStgr.setText(String.valueOf(ionicStrength));
            jTextFieldIonicStgr.requestFocusInWindow();
        } //catch
    } // validateIonicStrength()

    private void validateTemperature() {
        if(jTextFieldT.getText().length() <=0) {return;}
        double t = readTemperature();
        jTextFieldT.setText(String.valueOf(t));
    } // validateTemperature()

  /** Reads the value in <code>jTextFieldIonicStgr</code>.
   * Check that it is within -1 to 1000
   * @return the ionic strength */
  private double readIonStrength() {
    if(jTextFieldIonicStgr.getText().length() <=0) {return 0;}
    double w;
    try{w = Double.parseDouble(jTextFieldIonicStgr.getText());
        w = Math.min(1000,Math.max(w, -1));
        if(w < 0) {w = -1;}
        } //try
    catch (NumberFormatException nfe) {
        out.println("Error reading Ionic Strength:"+nl+"   "+nfe.toString());
        w = 0;
        } //catch
    return w;
  } //readIonStrength()

  /** Reads the value in <code>jTextFieldT</code>.
   * Checks that it is within -50 to 1000 Celsius. It returns "NaN"
   * (not_a_number) if there is no temperature to read.
   * @return the temperature in Celsius */
  private double readTemperature() {
    if(jTextFieldT.getText().length() <=0) {return Double.NaN;}
    double w;
    try{w = Double.parseDouble(jTextFieldT.getText());
        w = Math.min(1000,Math.max(w, -50));
        } //try
    catch (NumberFormatException nfe) {
        if(!jTextFieldT.getText().equals("NaN")) {
            out.println("Error reading Temperature:"+nl+"   "+nfe.toString());
        }
        w = Double.NaN;
    } //catch
    return w;
  } //readTemperature()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="showErrMsgBx">

/** Outputs a message (through a call to <code>showMsg(msg,type)</code>
 * and shows it in a [OK, Cancel] message box (if doNotStop = false)
 * @param msg the message
 * @param type =1 exception error; =2 warning; =3 information
 * @return it return <code>true</code> if the user chooses "OK", returns <code>false</code> otherwise
 * @see #showMsg(java.lang.String, int) showMsg */
  boolean showErrMsgBxCancel(String msg, int type) {
    if(msg == null || msg.trim().length() <=0) {return true;}
    //if(type == 1) {type = 0;}
    showMsg(msg,type);
    if(!doNotStop && predomFrame != null) {
        int j;
        if(type<=1) {j=javax.swing.JOptionPane.ERROR_MESSAGE;}
        else if(type==2) {j=javax.swing.JOptionPane.INFORMATION_MESSAGE;}
        else {j=javax.swing.JOptionPane.WARNING_MESSAGE;}
        if(!predomFrame.isVisible()) {predomFrame.setVisible(true);}
        Object[] opt = {"OK", "Cancel"};
        int n= javax.swing.JOptionPane.showOptionDialog(predomFrame,msg,
                        progName,javax.swing.JOptionPane.OK_CANCEL_OPTION,j, null, opt, opt[0]);
        if(n != javax.swing.JOptionPane.OK_OPTION) {return false;}
    }
    return true;
}

/** Outputs a message (through a call to <code>showMsg(msg,0)</code> if type=1,
 * or to <code>showMsg(msg,type)</code> if type is =2 or 3),
 * and shows it in a message box (if doNotStop = false)
 * @param msg the message
 * @param type =1 exception error; =2 warning; =3 information
 * @see #showMsg(java.lang.String, int) showMsg
 */
void showErrMsgBx(String msg, int type) {
    if(msg == null || msg.trim().length() <=0) {return;}
    //if(type == 1) {type = 0;}
    showMsg(msg,type);
    if(!doNotStop) {
        if(predomFrame == null) {
            ErrMsgBox mb = new ErrMsgBox(msg, progName);
        } else {
            int j;
            if(type<=1) {j=javax.swing.JOptionPane.ERROR_MESSAGE;}
            else if(type==2) {j=javax.swing.JOptionPane.INFORMATION_MESSAGE;}
            else {j=javax.swing.JOptionPane.WARNING_MESSAGE;}
            if(!this.isVisible()) {this.setVisible(true);}
            javax.swing.JOptionPane.showMessageDialog(this, msg, progName,j);
        }
    }
}
/** Outputs the exception message and the stack trace,
 * through a call to <code>showMsg(ex)</code>,
 * and shows a message box (if doNotStop = false)
 * @param ex the exception
 * @see #showMsg(java.lang.Exception) showMsg */
void showErrMsgBx(Exception ex) {
    if(ex == null) {return;}
    showMsg(ex);
    String msg = ex.toString();
    if(!doNotStop) {
        if(predomFrame == null) {
            ErrMsgBox mb = new ErrMsgBox(msg, progName);
        } else {
            int j = javax.swing.JOptionPane.ERROR_MESSAGE;
            if(!this.isVisible()) {this.setVisible(true);}
            javax.swing.JOptionPane.showMessageDialog(this, msg, progName,j);
        }
    }
}
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="showMsg">
/** Outputs a message either to System.out (if type is 1, 2 or 3) or
 * to System.err otherwise (type=0).
 * @param msg the message
 * @param type =0 error (outputs message to System.err); =1 error; =2 warning; =3 information
 * @see #showErrMsgBx(java.lang.String, int) showErrMsgBx */
void showMsg(String msg, int type) {
    if(msg == null || msg.trim().length() <=0) {return;}
    final String flag;
    if(type == 2) {flag = "Warning";} else if(type == 3) {flag = "Message";} else {flag = "Error";}
    if(type == 1 || type == 2 || type == 3) {
        out.println("- - - - "+flag+":"+nl+msg+nl+"- - - -");
        System.out.println("- - - - "+flag+":"+nl+msg+nl+"- - - -");
        out.flush(); System.out.flush();
    } else {
        err.println("- - - - "+flag+":"+nl+msg+nl+"- - - -");
        System.err.println("- - - - "+flag+":"+nl+msg+nl+"- - - -");
        err.flush(); System.err.flush();
    }
}
/** Outputs the exception message and the stack trace to System.err and to err
 * @param ex the exception
 * @see #showErrMsgBx(java.lang.Exception) showErrMsgBx */
void showMsg(Exception ex) {
    if(ex == null) {return;}
    String msg = "- - - - Error:"+nl+ex.toString()+nl+nl+Util.stack2string(ex)+nl+"- - - -";
    err.println(msg);
    System.err.println(msg);
    err.flush();
    System.err.flush();
}
  //</editor-fold>

//<editor-fold defaultstate="collapsed" desc="doCalculations">
  private void doCalculations() {
    setCursorWait();
    if(dbg) {out.println("--- doCalculations()");}
    jLabelStatus.setText("Starting the calculations");
    jLabelProgress.setText(" ");
    disableMenus();
    //---- Check if "EH" is needed -----------
    if(diag.Eh && diag.pInX != 3 && diag.pInY != 3) {
        boolean peGiven = false;
        for(int i =0; i < cs.Na; i++) {
            if(Util.isElectron(namn.identC[i]) &&
                csC.kh[i] ==2) { // kh=2  logA given
                    peGiven = true; break;}
        } //for i
        if(!peGiven) {diag.Eh = false;}
    }
    //--- temperature ------------------------
    diag.temperature = readTemperature();
    if(Double.isNaN(diag.temperature) && diag.Eh) { // Eh plotted in one axis
        String msg = "\"Error: Need to plot Eh values, but no temperature is given.";
        showErrMsgBx(msg,1);
        setCursorDef();
        restoreMenus(true);
        return;
    }
    if(diag.Eh) {peEh = (ln10*8.3144126d*(diag.temperature+273.15d)/96484.56d);} else {peEh = Double.NaN;}
    // decide if the temperature should be displayed in the diagram
    if(!diag.Eh && !calcActCoeffs
            && !(neutral_pH && (diag.pInX ==1 || diag.pInY ==1))) {
        if(dbg) {out.println(" (Note: temperature not needed in the diagram)");}
    }
    // nbr of calculation steps
    nSteps = jScrollBarNbrPoints.getValue();
    nSteps = Math.max(NSTP_MIN,nSteps);
    /** Max. number of points to be plotted. In a predominance area diagram
     * these are the points delimiting the areas. Should be at least = 15 * nSteps. */
    final int mxPNT = nSteps * 17;
    /** a class to store data about a Predom diagram */
    final PredomData predData = new PredomData(cs.Ms, mxPNT); // create a new instance
    jLabelNbrPText.setText("Nbr of calc. steps:");
    jScrollBarNbrPoints.setEnabled(false);
    jLabelNbrPText.setEnabled(false);
    if(dbg) {out.println(" "+(nSteps+1)+" caculation points"+nl+
            "ionic strength = "+ionicStrength+nl+
            "temperature = "+(float)diag.temperature+nl+
            "max relative mass-balance tolerance = "+(float)tolHalta);}

    // ---------------------------------------
    // get an instance of Plot
    plot = new Plot_Predom(this, err, out);
    // ---------------------------------------

    String msg;
    // ionic strength
    diag.ionicStrength = ionicStrength;
    if(!diag.aquSystem && diag.ionicStrength != 0) {
        msg = "File: "+inputDataFile.getName()+nl+
                "Warning: This does not appear to be an aqueous system,"+nl+
                "and yet you give a value for the ionic strength?";
        if(!showErrMsgBxCancel(msg, 1)) {
            out.println("--- Cancelled by the user ---");
            setCursorDef();
            restoreMenus(true);
            return;
        }
    }

    // model to calculate ion activity coefficients
    if(calcActCoeffs) {
        diag.activityCoeffsModel = jComboBoxModel.getSelectedIndex();
        diag.activityCoeffsModel = Math.min(jComboBoxModel.getItemCount()-1,
                                        Math.max(0,diag.activityCoeffsModel));
        csC.actCoefCalc = true;
    } else {
        diag.activityCoeffsModel = -1;
        csC.actCoefCalc = false;
    }
    // height scale for texts in the diagram
    tHeight = jScrollBarHeight.getValue()/10;
    tHeight = Math.min(10.d,Math.max(tHeight, 0.3));
    // keep track of elapsed time
    calculationStart = System.nanoTime();

    //-- a simple check...
    if(dgrC.hur[diag.compX] <=1 || dgrC.hur[diag.compX] ==4 || dgrC.hur[diag.compX] >=6) { //error?
        err.println("Programming error found at \"doCalculations()\":"+nl+
                    "   diag.compX = "+diag.compX+" hur = "+dgrC.hur[diag.compX]+" (should be 2,3 or 5)");
    }
    if(dgrC.hur[diag.compY] <=1 || dgrC.hur[diag.compY] ==4 || dgrC.hur[diag.compY] >=6) { //error?
        err.println("Programming error found at \"doCalculations()\":"+nl+
                    "   diag.compY = "+diag.compY+" hur = "+dgrC.hur[diag.compY]+" (should be 2,3 or 5)");
    }

    // check POS and NEG with the Tot.Conc. given in the input
    int j;
    for(j =0; j < cs.Na; j++) {
        if(csC.kh[j] == 2) {continue;} //only it Tot.conc. is given
        if(!(pos[j] && neg[j]) && (pos[j] || neg[j])) {
            if(dgrC.hur[j] !=3 &&  //not LTV
               dgrC.cLow[j]==0 && (Double.isNaN(dgrC.cHigh[j]) || dgrC.cHigh[j]==0)) {
                    //it is only POS or NEG and Tot.Conc =0
                    csC.logA[j] = -9999.;
                    dgrC.cLow[j] = csC.logA[j];
                    if(j == diag.compX || j == diag.compY) {dgrC.cHigh[j] = dgrC.cLow[j]+10;}
                    csC.kh[j] =2; //kh=2 means logA given
                    if(dbg) {out.println("Can not calculate mass-balance for for component \""+namn.identC[j]+"\""+nl+
                        "   its log(activity) is now set to -9999.");}
            }
        }
    } //for j

    // ---- Make an instance of Factor
    String userHome = System.getProperty("user.home");
    factor = new Factor(ch, pathApp, userHome, pathDef.toString(), out);
    out.flush();
    // ---- print information on the model used for activity coefficients
    try {factor.factorPrint(dbg);}
    catch (Exception ex) {
        showErrMsgBx(ex.getMessage(),1);
        factor = null;
    }
    if(factor == null) {
        restoreMenus(false);
        setCursorDef();
        return;
    }
    // ---- Initialize variables
    csC.dbg = dbgHalta;
    csC.cont = false;
    csC.tol = tolHalta;
    for(j =0; j < cs.Na; j++) {
        if(csC.kh[j] == 1) {
            csC.tot[j]=dgrC.cLow[j];
            csC.logA[j]=-10;
            if(csC.tot[j]>0) {csC.logA[j] = Math.log10(csC.tot[j]) -3;}
        }
        else {csC.logA[j]=dgrC.cLow[j];}
    } // for j

    predData.xLeft = dgrC.cLow[diag.compX];
    predData.xRight = dgrC.cHigh[diag.compX];
    predData.stepX =(dgrC.cHigh[diag.compX] - dgrC.cLow[diag.compX]) / nSteps;

    predData.yBottom = dgrC.cLow[diag.compY];
    predData.yTop = dgrC.cHigh[diag.compY];
    predData.stepY =(dgrC.cHigh[diag.compY] - dgrC.cLow[diag.compY]) / nSteps;

    // ---- Run the calculations on another Thread ----
    jLabelStatus.setText("Please wait --");
    finishedCalculations = false;

    msg = "Starting the calculations...";
    out.println(msg);
    if(consoleOutput) {System.out.println(msg);}

    tsk = new HaltaTask();
    tsk.setPredData(predData);
    tsk.execute();

  } //doCalculations()
//</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="private class HaltaTask">
/** A SwingWorker to perform tasks in the background.
 * @see HaltaTask#doInBackground() doInBackground() */
public class HaltaTask extends javax.swing.SwingWorker<Boolean, Integer> {
    private boolean getHaltaInstanceOK = true;
    private boolean haltaError = false;
    private int nbrTooLargeConcs = 0;
    private int nbrHaltaErrors = 0;
    private int nbrHaltaUncertain = 0;
    private final StringBuilder failuresMsg = new StringBuilder();
    private final java.util.Locale engl = java.util.Locale.ENGLISH;
    private boolean tooManyAreaPoints = false;
    private char[][] lineMap = null;
    private PredomData predData = null;
  /** Sets a local pointer to an instance of PredomData, where results will be stored.
   * @param pd an instance of PredomData */
    protected void setPredData(PredomData pd) {predData = pd;}

  /** The instructions to be executed are defined here
   * @return true if no error occurs, false otherwise
   * @throws Exception */
  @Override protected Boolean doInBackground() throws Exception {
    if(predData == null) {
        showMsg("Programming error in \"HaltaTask\" (SwingWorker):"+nl+"PredomData = null.", 0);
        this.cancel(true);
        return false;
    }
    //--- do the HaltaFall calculations
    // create an instance of class HaltaFall
    h = null;
    try {h = new HaltaFall(cs,factor, out);}
    catch (Chem.ChemicalParameterException ex) { // this should not occur, but you never know
        showErrMsgBx(ex);
        getHaltaInstanceOK = false; // skip the rest of the thread
    }
    if(!getHaltaInstanceOK) {this.cancel(true); return false;}

   /** <code>line[n][1]</code> is the predominating species at point "n" (along the
    * Y-axis) for the present calculation.<br>
    * <code>line[n][0]</code> is the predominating species at point "n" (along the
    * Y-axis) for the <i>previous</i> calculation.<br>
    * The plot area is divided in "nSteps" which means nSteps+1 calculated
    * positions, plus one over the top and one under the bottom, this gives
    * (nSteps+3) for the size of line[][2].<br>
    * The bottom and top  values (or left and right if calculations are row-wise),
    * that is, line[0][] and line[nSteps+2][], are always "-1",
    * that is: no predominating species outside the plot area. */
    int[][] line = new int[nSteps+3][2];

    int iL;
    int iL_1; // = iL -1
    final int NOW = 1;  final int PREVIOUS = 0;
    for(iL=0; iL<line.length; iL++) {line[iL][PREVIOUS]=-1; line[iL][NOW]=-1;}

    int i,j, n;
    if(dbg) {
        lineMap = new char[nSteps+1][nSteps+1];
        for(j=0; j<lineMap.length; j++) {for(i=0; i<lineMap.length; i++) {lineMap[j][i] = ' ';}}
    }
    double xVal, yVal; // these are the position of the calculated point
    predData.nPoint = -1;
    boolean frontier;
    double tolHalta0 = csC.tol;
    final String f = "Calculation failed in \"HaltaFall.haltaCalc\" at point (%d,%d), x=%7.5f y=%7.5f"+nl+"%s";
    final String d;
    if(cs.Ms<=98) {d="%2d";} else if(cs.Ms<=998) {d="%3d";} else if(cs.Ms<=9998) {d="%4d";} else {d="%5d";}

    // ----- The calculations are performed column-wise:
    //          all Y-values are calcualted for each X-value,
    //          starting from the left/bottom corner and going
    //          to the right and upwards
    //       The outer loop is along the X-axis and the inner loop along the Y-axis
    nStepOuter = -1;
    xVal = predData.xLeft - predData.stepX;
    do_loopOuter:
    do {  // -------------------------------------- Outer Loop for X-axis
        nStepOuter++;
        publish((nStepOuter+1));
        //if needed for debugging: sleep some milliseconds (wait) at each calculation point
        //try {Thread.sleep(1);} catch(Exception ex) {}

        xVal = xVal + predData.stepX;
        j = diag.compX;
        // --- input data for this calculation point
        if(csC.kh[j] == 1) {
            if(dgrC.hur[j] ==3) { // LTV
                csC.tot[j] = Math.exp(ln10*xVal);
            } else { // TV
                csC.tot[j]=xVal;
            }
        } else { // kh[j] = 2
              csC.logA[j]=xVal;
        }
        // For the 1st point at the bottom of the diagram, starting the calcs. using
        // the last equilibrium composition (at the top of the diagram) might be a bad idea
        csC.cont = false;

        // csC.cont = false;
        nStepInner = -1;
        yVal = predData.yBottom - predData.stepY;
        do {  // -------------------------------------- Inner Loop for Y-axis
            nStepInner++;
            iL = nStepInner+1;
            iL_1 = nStepInner; // iL_1 = iL-1
            yVal = yVal + predData.stepY;
            j = diag.compY;
            // --- input data for this calculation point
            if(csC.kh[j] == 1) {
                if(dgrC.hur[j] ==3) { // LTV
                    csC.tot[j] = Math.exp(ln10*yVal);
                } else { // TV
                    csC.tot[j]=yVal;
                }
            } else { // kh[j] = 2
                csC.logA[j]=yVal;
            }

            if(finishedCalculations) {break do_loopOuter;} //user requests exit?

            // print debug output from halta for the first point
            if(nStepOuter == 0 && nStepInner == 0) {
                if(dbg || csC.dbg > Chem.DBGHALTA_DEF) {
                    out.println("Starting calculation 1 (of "+(nSteps+1)+"x"+(nSteps+1)+")");
                }
            } else {
                csC.dbg = Chem.DBGHALTA_DEF;
                // ########## ---------- ##########  ---------- ########## ##?##
                if(Math.abs(xVal+9.03)<0.005 && (Math.abs(yVal+2.51)<0.001)) {
                    csC.dbg = 6;
                    out.println("---- Note: x="+(float)xVal+", y="+(float)yVal+"  (debug) ---- nStepInner="+nStepInner+", nStepOuter="+nStepOuter);
                }
                // ########## ---------- ##########  ---------- ########## ##?##
            }
            // --- HaltaFall: do the calculations
            //     calculate the equilibrium composition of the system
            try {
                csC.tol = tolHalta0;
                h.haltaCalc();
                if(csC.isErrFlagsSet(2)) { // too many iterations when solving the mass balance equations
                    do {
                        csC.tol = csC.tol * 0.1; // decrease tolerance and try again
                        if(dbg || csC.dbg > Chem.DBGHALTA_DEF) {
                            out.println("Too many iterations when solving the mass balance equations"+nl+
                                    "  decreasing tolerance to: "+(float)csC.tol+" and trying again.");
                        }
                        h.haltaCalc();
                    } while (csC.isErrFlagsSet(2) && csC.tol >= 1e-9);
                    csC.tol = tolHalta0;
                    if(dbg || csC.dbg > Chem.DBGHALTA_DEF) {
                        out.println("Restoring tolerance to: "+(float)tolHalta0+" for next calculations.");
                    }
                }
                if(csC.isErrFlagsSet(3)) { // failed to find a satisfactory combination of solids
                    if(dbg || csC.dbg > Chem.DBGHALTA_DEF) {
                        out.println("Failed to find a satisfactory combination of solids. Trying again...");
                    }
                    csC.cont = false;      // try again
                    h.haltaCalc();
                }
            } catch (Chem.ChemicalParameterException ex) {
                String ms = "Error in \"HaltaFall.haltaCalc\", errFlags="+nl+csC.errFlagsGetMessages()+nl+
                        "   at point: ("+(nStepInner+1)+","+(nStepOuter+1)+")  at x="+(float)xVal+" y="+(float)yVal;
                showMsg(ex);
                showErrMsgBx(ms+nl+ex.getMessage(),1);
                haltaError = true;
                break do_loopOuter;
            }
            // ---
            if(finishedCalculations) {break do_loopOuter;} //user request exit?

            if(csC.isErrFlagsSet(5)) {nbrTooLargeConcs++;}
            else if(csC.isErrFlagsSet(1)) {nbrHaltaUncertain++;}
            if(csC.isErrFlagsSet(2) || csC.isErrFlagsSet(3) || csC.isErrFlagsSet(4)
                            || csC.isErrFlagsSet(6)) {
                nbrHaltaErrors++;
                frontier = false;
                if(failuresMsg.length() >0) {failuresMsg.append(nl);}
                failuresMsg.append(String.format(engl,f,(nStepInner+1),(nStepOuter+1),(float)xVal,(float)yVal,csC.errFlagsGetMessages()));
            } else {
                //--------------------------------------------------
                findTopSpecies();

                line[iL][NOW] = topSpecies;
                //--------------------------------------------------
                //  Is this point a frontier between two areas ?
                if(line[iL][NOW] != line[iL][PREVIOUS]) {
                    frontier = line[iL][NOW] != line[iL_1][NOW] ||
                        line[iL][NOW] != line[iL_1][PREVIOUS];
                } else {
                    frontier = line[iL][NOW] != line[iL_1][NOW];
                }
            }
            if(frontier) {
                predData.nPoint++;
                // store what species are involved
                predData.pair[predData.nPoint][0] = line[iL][PREVIOUS];
                if(nStepOuter ==0 && line[iL][NOW] != line[iL_1][NOW]) {
                                            predData.pair[predData.nPoint][0] = line[iL_1][NOW];
                }
                predData.pair[predData.nPoint][1] = line[iL][NOW];
                predData.pair[predData.nPoint][2] = line[iL_1][NOW];
                // the position of the line separating the predominance areas
                predData.xPl[predData.nPoint] = xVal;
                    predData.yPl[predData.nPoint] = yVal;
                if(dbg) {lineMap[nStepInner][nStepOuter] = '+';}
            } else { // not frontier
                if(iL == (nSteps+1) || nStepOuter == nSteps) {
                    //Point at the margin of the diagram
                    //  these are used only to determine the centre of each predominance area
                    predData.nPoint++;
                    predData.pair[predData.nPoint][0]=line[iL][PREVIOUS];
                    predData.pair[predData.nPoint][1]=-1; //the predominating species at this point
                    predData.pair[predData.nPoint][2]=line[iL][NOW];
                    predData.xPl[predData.nPoint] = xVal;
                    predData.yPl[predData.nPoint] = yVal;
                    if(dbg) {lineMap[nStepInner][nStepOuter] = '+';}
                }
            }//frontier?
            // --------------------------------------------------

            if(dbg && nStepOuter == 0 && nStepInner == 0 && diag.activityCoeffsModel >=0) {
                    err.flush(); out.flush();
                    out.println("First calculation step finished.");
                    factor.printActivityCoeffs(out);
            }

          } while(nStepInner < nSteps); // ------------ Inner Loop for 2nd-axis

          if(dbg) { //-- print the predominance species map
              if(nStepOuter==0) {out.println("---- Map of predominating species (from 1 \""+
                      namn.ident[0]+"\" to "+(cs.Ms-1)+" \""+namn.ident[cs.Ms-1]+"\") and X-variable."+nl+
                      "     The leftmost column corresponds to Y="+(float)predData.yBottom+" and the rightmost column to Y="+(float)predData.yTop);}
              for(i=0; i<(line.length-1); i++) {
                  if(line[i][NOW]>=0) {n=line[i][NOW]+1;} else {n=-1;}
                  out.print(String.format(d,n));
              }
              out.print(" "+(float)xVal);
              out.println();
          }

          if(predData.nPoint >= predData.mxPNT) {
            tooManyAreaPoints = true;
            break; // do_loopOuter;
          }
          for(i =0; i<line.length; i++) {line[i][PREVIOUS] = line[i][NOW]; line[i][NOW] = -1;}

        } while(nStepOuter < nSteps); // ------------ External Loop for 1st-axis
    return true;
  }
  /** Performs some tasks after the calculations have been finished */
  @Override protected void done() {
    if(isCancelled()) {
        if(dbg) {System.out.println("SwingWorker cancelled.");}
    } else {
        int i,j;
        String msg;
        if(!haltaError) {
            if(tooManyAreaPoints) {
              showErrMsgBx("Problem: Too many area-delimiting points were found."+nl+
                  "The diagram will be incomplete.", 1);
            } // tooManyAreaPoints?
            calculationTime = (System.nanoTime() - calculationStart)
                /1000000; //convert nano seconds to milli seconds
            msg = "--- Calculated "+(nSteps+1)+" x "+(nStepOuter+1)+" points, time="+millisToShortDHMS(calculationTime);
            out.println(nl+msg);
            System.out.println(msg);
            System.out.flush();
            if(nbrTooLargeConcs > 0) {
                int percent = nbrTooLargeConcs*100 /((nSteps+1) * (nSteps+1));
                if(percent >0) {
                    msg = percent+" % of the calculated points had some"+nl+
                          "concentrations > "+(int)Factor.MAX_CONC+" (molal); impossible in reality."+nl+nl;
                    if(calcActCoeffs) {msg = msg + "The activity coefficients are then WRONG"+nl+
                          "and the results unrealistic.";}
                    else {msg = msg + "These results are unrealistic.";}
                    showErrMsgBx(msg, 1);
                }
            }
            if(nbrHaltaUncertain >1) {
                out.println(LINE);
                out.println(String.format("%d",nbrHaltaUncertain).trim()+" uncertain point(s)");
                out.println(LINE);
            }
            if(failuresMsg != null && failuresMsg.length() >0) {
                out.println(LINE);
                out.println(failuresMsg);
                out.println(LINE);
            }
            if(nbrHaltaErrors >0) {
                msg = String.format("%d",nbrHaltaErrors).trim()+" calculation failure(s)";
                showErrMsgBx(msg, 1);
            }
            if(dbg && lineMap != null) { //-- print the are limits map
                out.println("---- Map of area limits (points to plot):");
                for(i=0; i<lineMap.length; i++) {
                    for(j=0; j<lineMap.length; j++) {out.print(lineMap[j][i]);} out.println();
                }
            } //dbg

            // --------------------------------------------------
            //  Determine the center of each area
            //    (where labels will be plotted)
            plot.minMax(ch, predData);
            // --------------------------------------------------
            if(dbg) {
              out.println("---- List of points to plot (including margins):"+nl+"point_nbr, pair[0,1,2], x-value, y-value");
              for(i=0; i<predData.nPoint; i++) {
                out.println(
                        String.format("%3d,  %3d,%3d,%3d",i,predData.pair[i][0],predData.pair[i][1],predData.pair[i][2])+
                        ",   "+(float)predData.xPl[i]+", "+(float)predData.yPl[i]
                        );
              }
              out.println("----");
            }

            // --------------------------------------------------
            // Take away plot margins (about: 4*nStep  points)
            // (the margins are needed to determine the center of each area)
            int nPoints2 = -1;
            for(i=0; i < predData.nPoint; i++) {
                if(predData.pair[i][0] == -1 &&
                    (predData.pair[i][1] == predData.pair[i][2] || predData.pair[i][2] == -1
                    )) {continue;}
                if(predData.pair[i][1] == -1) {continue;}
                if(predData.pair[i][2] == -1 && predData.pair[i][0] == predData.pair[i][1]) {continue;}
                nPoints2++;
                predData.xPl[nPoints2] = predData.xPl[i];
                predData.yPl[nPoints2] = predData.yPl[i];
                //
                // pair[][2] is not used when plotting, if pair[][0] = pair[][1] use pair[][2]
                if(predData.pair[i][0] == predData.pair[i][1] &&
                    predData.pair[i][2] != -1) {predData.pair[i][0] = predData.pair[i][2];}
                predData.pair[nPoints2][0] = predData.pair[i][0];
                predData.pair[nPoints2][1] = predData.pair[i][1];
                predData.pair[nPoints2][2] = predData.pair[i][2]; //not really neaded becaise pair[][2] is not used when plotting
            } // for i
            predData.nPoint = nPoints2;
            // --------------------------------------------------
            //  Move the points halv step to the left and down
            //  to try to compensate for the column-wise
            //  calculation procedure
            for(i=0; i<predData.nPoint; i++) {
              if(Math.abs(predData.xPl[i]-predData.xLeft) > 1e-5 &&
                 Math.abs(predData.xPl[i]-predData.xRight) > 1e-5) {
                                predData.xPl[i] = predData.xPl[i]-0.5*predData.stepX;
              }
              if(Math.abs(predData.yPl[i]-predData.yBottom) > 1e-5 &&
                 Math.abs(predData.yPl[i]-predData.yTop) > 1e-5) {
                                predData.yPl[i] = predData.yPl[i]-0.5*predData.stepY;
              }
            }

            // --------------------------------------------------
            if(dbg) {
                out.println("---- List of points to plot:"+nl+"point_nbr, pair[0,1,2], x-value, y-value");
                for(i=0; i<=predData.nPoint; i++) {
                    out.println(
                        String.format("%3d,  %3d,%3d,%3d",i,predData.pair[i][0],predData.pair[i][1],predData.pair[i][2])+
                        ",   "+(float)predData.xPl[i]+", "+(float)predData.yPl[i]
                        );
                }
                out.println("----");
            }

            out.println("Number of points to draw = "+(predData.nPoint+1));

            // -------------------------------------------
            out.println("Saving plot file \""+outputPltFile.getAbsolutePath()+"\"...");
            plot.drawPlot(outputPltFile, ch, predData);
            if(outputPltFile != null && outputPltFile.getName().length()>0) {
                String msg3 = "Saved plot file: \""+outputPltFile.getAbsolutePath()+"\"";
                out.println(msg3);
                System.out.println(msg3);
            }
            // -------------------------------------------
        } //haltaError?

        // execute the following actions on the event-dispatching Thread
        // after the "calculations" and the plotting are finished
        if(getHaltaInstanceOK && !haltaError) {
          javax.swing.SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
            jTabbedPane.setTitleAt(2, "<html><u>D</u>iagram</html>");
            jTabbedPane.setEnabledAt(2, true);
            jTabbedPane.setSelectedComponent(jPanelDiagram);
            jTabbedPane.requestFocusInWindow();
            restoreMenus(true);
          }}); // invokeLater
        }//if getHaltaInstanceOK
        else {
          javax.swing.SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
            jTabbedPane.setSelectedComponent(jScrollPaneMessg);
            jTabbedPane.requestFocusInWindow();
            restoreMenus(true);
          }}); // invokeLater
        }//if !getHaltaInstanceOK
        if(dbg) {System.out.println("SwingWorker done.");}
    }
    out.println(LINE);
    System.out.println(LINE);
    finishedCalculations = true;
    predomFrame.notify_All();
    setCursorDef();
  }
  @Override protected void process(java.util.List<Integer> chunks) {
    // Here we receive the values that we publish(). They may come grouped in chunks.
    final int i = chunks.get(chunks.size()-1);
    int nn = (int)Math.floor(Math.log10(nSteps+1))+1;
    final String f = "now calculating loop: %"+String.format("%3d",nn).trim()
            +"d (out of %"+String.format("%3d",nn).trim()+"d)";
    javax.swing.SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
        jLabelProgress.setText(String.format(f,i,(nSteps+1)));
    }}); // invokeLater
  }
    }
  // </editor-fold>

  //<editor-fold defaultstate="collapsed" desc="main">
  /** The "main" method. Creates a new frame if needed.
   * Errors and messages are sent to System.out and System.err.
   * @param args the command line arguments */
  public static void main(final String args[]) {
    // ----
    System.out.println(LINE+nl+progName+" (Predominance Area Diagrams),  version: "+VERS);
    // set LookAndFeel
    //try {javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());}
    //try {javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");}
    try {javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());}
    catch (Exception ex) {}
    //---- for JOptionPanes set the default button to the one with the focus
    //     so that pressing "enter" behaves as expected:
    javax.swing.UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
    //     and make the arrow keys work:
    Util.configureOptionPane();

    if(args.length <=0) {System.out.println("Usage:   PREDOM  [data-file-name]  [-command=value]"+nl+
                "For a list of possible commands type:  PREDOM  -?");}
    else {
        if(DBG_DEFAULT) {System.out.println("PREDOM "+java.util.Arrays.toString(args));}
    }

    //---- get Application Path
    pathApp = Main.getPathApp();
    if(DBG_DEFAULT) {System.out.println("Application path: \""+pathApp+"\"");}
    //---- "invokeAndWait": Wait for either:
    //     - the main window is shown, or
    //     - perform the calculations and save the diagram
    boolean ok = true;
    String errMsg = "PREDOM construction did not complete successfully"+nl;
    try {
        java.awt.EventQueue.invokeAndWait(new Runnable() {@Override public void run() {
            // deal with some special command-line arguments
            boolean doNotExit0 = false;
            boolean doNotStop0 = false;
            boolean dbg0 = DBG_DEFAULT;
            boolean rev0 = false;
            boolean h = false;
            if(args.length > 0) {
                for (String arg : args) {
                    if (arg.equalsIgnoreCase("-dbg") || arg.equalsIgnoreCase("/dbg")) {
                        dbg0 =true;
                    } else if (arg.equalsIgnoreCase("-keep") || arg.equalsIgnoreCase("/keep")) {
                        doNotExit0 =true;
                    } else if (arg.equalsIgnoreCase("-nostop") || arg.equalsIgnoreCase("/nostop")) {
                        doNotStop0 = true;
                    } else if (arg.equalsIgnoreCase("-rev") || arg.equalsIgnoreCase("/rev")) {
                        rev0 =true;
                    } else if (arg.equals("-?") || arg.equals("/?") || arg.equals("?")) {
                        h = true;
                        printInstructions(System.out);
                    } //if args[] = "?"
                } //for arg
                if(h && !doNotExit0) {return;} // exit after help if OK to exit
            } //if args.length >0
            predomFrame = new Predom(doNotExit0, doNotStop0, dbg0);//.setVisible(true);
            predomFrame.start(rev0, h, args);
        }}); //invokeAndWait
    } catch (InterruptedException ex) {
        ok = false;  errMsg = errMsg + Util.stack2string(ex);
    }
    catch (java.lang.reflect.InvocationTargetException ex) {
        ok = false;  errMsg = errMsg + Util.stack2string(ex);
    } 
    if(!ok) {
        System.err.println(errMsg);
        ErrMsgBox mb = new ErrMsgBox(errMsg, progName);
    }

    //-- wait, either for the calculations to finish, or
    //   for the window to be closed by the user
    if(predomFrame != null) {
        Thread t = new Thread() {@Override public void run(){
            if(predomFrame.inputDataFileInCommandLine) {
                predomFrame.synchWaitCalcs();
                if(!predomFrame.doNotExit) {predomFrame.end_program();}
                else{predomFrame.synchWaitProgramEnded();}
            } else {
                predomFrame.synchWaitProgramEnded();
            }
        }};// Thread t
        t.start();  // Note: t.start() returns inmediately;
                    // statements here are executed inmediately.
        try {t.join();} catch (InterruptedException ex) {} // wait for the thread to finish
        if(predomFrame.dbg) {System.out.println(progName+" - finished.");}
    }
    //javax.swing.JOptionPane.showMessageDialog(null, "ready?", progName, javax.swing.JOptionPane.INFORMATION_MESSAGE);
  } // main(args[])
  //</editor-fold>

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupDebug;
    private javax.swing.JButton jButtonDoIt;
    private javax.swing.JCheckBox jCheckActCoeff;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuPredomDebug;
    private javax.swing.JCheckBox jCheckReverse;
    private javax.swing.JComboBox jComboBoxModel;
    private javax.swing.JComboBox jComboBoxTol;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelData;
    private javax.swing.JLabel jLabelHD;
    private javax.swing.JLabel jLabelHeight;
    private javax.swing.JLabel jLabelIonicStr;
    private javax.swing.JLabel jLabelIonicStrM;
    private javax.swing.JLabel jLabelModel;
    private javax.swing.JLabel jLabelNbrPText;
    private javax.swing.JLabel jLabelPltFile;
    private javax.swing.JLabel jLabelPointsNbr;
    private javax.swing.JLabel jLabelProgress;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JLabel jLabelT;
    private javax.swing.JLabel jLabelTC;
    private javax.swing.JLabel jLabelTol;
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenuItem jMenuCancel;
    private javax.swing.JMenu jMenuDebug;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuFileMakeD;
    private javax.swing.JMenuItem jMenuFileOpen;
    private javax.swing.JMenuItem jMenuFileXit;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuHelpAbout;
    private javax.swing.JMenuItem jMenuSave;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanelActC;
    private javax.swing.JPanel jPanelDiagram;
    private javax.swing.JPanel jPanelFiles;
    private javax.swing.JPanel jPanelParameters;
    private javax.swing.JPanel jPanelStatusBar;
    private javax.swing.JPanel jPanelT;
    private javax.swing.JScrollBar jScrollBarHeight;
    private javax.swing.JScrollBar jScrollBarNbrPoints;
    private javax.swing.JScrollPane jScrollPaneMessg;
    private javax.swing.JTabbedPane jTabbedPane;
    private javax.swing.JTextArea jTextAreaA;
    private javax.swing.JTextField jTextFieldDataFile;
    private javax.swing.JTextField jTextFieldIonicStgr;
    private javax.swing.JTextField jTextFieldPltFile;
    private javax.swing.JTextField jTextFieldT;
    // End of variables declaration//GEN-END:variables

} // class Predom
