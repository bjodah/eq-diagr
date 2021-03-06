package plotPDF;

/** Convert a plt-file to PDF-format.
 * The input file is assumed to be in the system-dependent default
 * character encoding. The PDF file is written in "ISO-8859-1" character
 * encoding (ISO Latin Alphabet No. 1).
 * 
 * If an error occurs, a message is diplayed to the console, and
 * unless the command line argument -nostop is given, a message box
 * displaying the error is also produced.
 * 
 * Copyright (C) 2015-2018 I.Puigdomenech.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * 
 * @author Ignasi Puigdomenech */
public class PlotPDF {
private static final String progName = "PlotPDF";
private static final String VERS = "2018-May-15";
private static boolean started = false;
/** print debug information? */
private boolean dbg = false;
/** if true the program does display dialogs with warnings or errors */
private boolean doNotStop = false;
/** the plot file to be converted */
private java.io.File pltFile;
/** the pdf file name (the converted plt file) */
private String pdfFile_name;
/** the pdf file (the converted plt file) */
private java.io.File pdfFile;
/** has the file conversion finished ? */
private boolean finished = false;
private java.io.BufferedReader bufReader;
private java.io.OutputStreamWriter outputFile;
/** has an error occured? if so, delete the output file */
private boolean delete = false;
/** New-line character(s) to substitute "\n" */
private static final String nl = System.getProperty("line.separator");
private static final String SLASH = java.io.File.separator;
private static final String DASH_LINE = "- - - - - -";
private static final java.util.Locale engl = java.util.Locale.ENGLISH;
private String[] colours = new String[11];
/** line widths: normal (0.282 mm), thick, very-thin, thin */
private final double[] WIDTHS = {2.82, 6.35, 0.07, 1.06}; // units: 100 = 1 cm

// -- PDF things --
/** =(72/2.54)/100 */
private final double SCALE_USER_SPACE = 0.283464566929;
/** MediaBox [0 0 593 792] */
private static final double MediaBox_X = 593;
/** MediaBox [0 0 593 792] */
private static final double MediaBox_Y = 792;
private static final double MARGIN_X = 20;
private static final double MARGIN_Y = 15;
/** The size (bytes) of the first 21 pdf-objects,
 * excluding the end-of-line characters (which are system dependent).
 * Object nbr.0 consists of the two first comment lines in the pdf-file:<pre>
 * %PDF-1.1
 * % see objects 21 and 22 below</pre> */
private int[] objBytesNoNewLine = { 37,
                                    40, 47, 85, 90, 93,
                                    97, 87, 92, 95,100,
                                    70, 90, 90, 92, 96,
                                    77, 60,186, 98, 76,   50};
/** The number of end-of-lines in each of the 21 pdf-objects.
 * Used to calculate the size in bytes of each object.
 * Object nbr.0 consists of the two first comment lines in the pdf-file:<pre>
 * %PDF-1.1
 * % see objects 21 and 22 below</pre> */
private int[] objNbrNewLines = { 2,
                              6,  7,  9,  9,  9,
                              9,  9,  9,  9,  9,
                              8,  9,  9,  9,  9,
                              8,  7, 22,  8,  8,    5};
/** number of characters in a end-of-line */
private static final int nlL = System.getProperty("line.separator").length();

private final int NBR_PDF_OBJECTS = objBytesNoNewLine.length;

/** are we in the middle of printing some text? */
private boolean startedPrintingText = false;
/** text used in the PDF file to change the font */
private String changeFontString;
/** the size of the font as a text string */
private String fontSizeTxt;

/** line widths: 0 = normal, 1 = thick, 2 = very-thin, 3 = thin */
private int penNow =0;
private int colorNow =0;
//TODO: change zx and zy to shiftX and shiftY
private double scaleX, scaleY, zx, zy;
private double xNow=-Double.MAX_VALUE, yNow=-Double.MAX_VALUE;
private double x1st=-Double.MAX_VALUE, y1st=-Double.MAX_VALUE;
/**  -1 = start of program;<br>
 * 0 = no drawing performed yet;<br>
 * 1 = some move performed;<br>
 * 2 = some move/line-draw performed. */
private int newPath = -1;

// -- data from the command-line arguments
  private boolean pdfHeader = true;
  /** Colours: 0=black/white; 1=Standard palette; 2="Spana" palette. */
  private int pdfColors = 1;
  private boolean pdfPortrait = true;
  /** Font type: 0=draw; 1=Times; 2=Helvetica; 3=Courier. */
  private int pdfFont = 2;
  /** 0=Vector graphics; 1=Times-Roman; 2=Helvetica; 3=Courier. */
  private final String[] FONTS = {"Vector graphics", "Times-Roman", "Helvetica", "Courier"};
  private int pdfSizeX = 100;
  private int pdfSizeY = 100;
  private double pdfMarginB = 0;
  private double pdfMarginL = 0;

  //<editor-fold defaultstate="collapsed" desc="main: calls the Constructor">
  /**  @param args the command line arguments   */
  public static void main(String[] args) {
    boolean h = false;
    boolean doNotS = false, debg = false;
    String msg = "GRAPHIC  \"Portable Document Format (PDF)\"  UTILITY               "+VERS+nl+
            "=================================================="+nl;
    System.out.println(msg);
    if(args.length > 0) {
        for(String arg : args) {
            if (arg.equalsIgnoreCase("-dbg") || arg.equalsIgnoreCase("/dbg")) {
                debg =true;
            } else if (arg.equalsIgnoreCase("-nostop") || arg.equalsIgnoreCase("/nostop")) {
                doNotS = true;
            } else if (arg.equals("-?") || arg.equals("/?") || arg.equals("?")) {
                h = true;
                printInstructions(System.out);
            } //if args[] = "?"
        } //for arg : args
        if(h) {return;} // exit after help
    } //if args.length >0
    else {
        String t = "This program will convert a plot file (*.plt) into a PDF file."+nl+
                "Usage:   java -jar PlotPDF.jar  [plot-file-name]  [-command=value]"+nl+
                "For a list of possible commands type:  java -jar PlotPDF.jar  -?";
        System.out.println(t);
        ErrMsgBx mb = new ErrMsgBx(msg + nl + "Note: This is a console application."+nl+t, progName);
        System.exit(0);
    }
    PlotPDF pp;
    try{
        pp = new PlotPDF(debg, doNotS, args);
    } catch (Exception ex) { // this should not happen
        exception(ex, null, doNotS);
        pp = null;
    }
    // ---- if needed: wait for the calculations to finish on a different thread
    if(pp != null) {
        final PlotPDF p = pp;
        Thread t = new Thread() {@Override public void run(){
              p.synchWaitConversion();
              p.end_program();
          }};// Thread t
          t.start();  // Note: t.start() returns inmediately;
                      // statements here are executed inmediately.
          try{t.join();} catch (java.lang.InterruptedException ex) {} // wait for the thread to finish
    }
    System.out.println("All done."+nl+DASH_LINE);
    System.exit(0);
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="end_program, etc">
  private void end_program() {
      finished = true;
      this.notify_All();
  }

  private synchronized void notify_All() {this.notifyAll();}

  private synchronized void synchWaitConversion() {
      while(!finished) {
          try {this.wait();} catch(InterruptedException ex) {}
      }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="Constructor: runs the program itself">
  /** Constructor: runs the program itself
   * @param debg
   * @param doNotS
   * @param args  */
  public PlotPDF(boolean debg, boolean doNotS, final String[] args) {
    if(debg) {dbg = true;}
    doNotStop = doNotS;

    //--- read the command line arguments
    pltFile = null; pdfFile = null;
    String msg = null;
    boolean argErr = false;
    if(args != null && args.length >0){
        if(dbg) {System.out.println("Reading command-line arguments..."+nl);}
        for(int i=0; i<args.length; i++) {
          if(dbg){System.out.println("Command-line argument = \""+args[i]+"\"");}
          if(i == 0 && !args[0].toLowerCase().startsWith("-p")) {
              // Is it a plot file name?
              boolean ok = true;
              String name = args[0];
              if(name.startsWith("\"") && name.endsWith("\"")) { //remove enclosing quotes
                    name = name.substring(1, name.length()-1);
              }
              if(!name.toLowerCase().endsWith(".plt")) {name = name.concat(".plt");}
              java.io.File f = new java.io.File(name);
              if(!f.exists()) {
                if(dbg) {System.out.println("Not a plt-file:  \""+f.getAbsolutePath()+"\"");}
                ok = false;
              } // it is not a file
              if(ok && f.isDirectory()) {
                msg = "Error: \""+f.getAbsolutePath()+"\" is a directory.";
                ok = false;
              }
              if(ok && !f.canRead()) {
                msg = "Error: can not open file for reading:"+nl+"   \""+f.getAbsolutePath()+"\"";
                ok = false;
              }
              // ok = true if it is an existing file that can be read
              if(ok) {
                if(dbg){System.out.println("Plot file: "+f.getPath());}
                pltFile = f;
                continue;
              } else {
                if(msg != null) {
                    if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
                }
              }
          } // if i=0
          if(!parseArg(args[i].trim())) {argErr = true; break;}
        } // for i
    } // if args
    if(dbg) {System.out.println(nl+"Command-line arguments ended."+nl);}
    if(argErr) {end_program(); return;}

    if(pltFile == null) {
        msg = "No plot file name given in the command-line.";
        if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
        end_program(); return;
    }

    if(pdfFile_name != null && pdfFile_name.trim().length() >0) {
        if(pdfFile_name.contains(SLASH)) {
            pdfFile = new java.io.File(pdfFile_name);
        } else {
            pdfFile = new java.io.File(pltFile.getParent(), pdfFile_name);
            if(dbg) {
                System.out.println("PDF-file: \""+pdfFile.getAbsolutePath()+"\""+nl);
            }
        }
    }
    if(pdfFile == null) {
        String name = pltFile.getAbsolutePath();
        int n = name.length();
        name = name.substring(0, n-4)+".pdf";
        pdfFile = new java.io.File(name);
    }
    if(pdfFile.exists() && (!pdfFile.canWrite() || !pdfFile.setWritable(true))) {
        msg = "Error: can not write to file"+nl+"   \""+pdfFile.toString()+"\""+nl+
                "is the file locked?";
        if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
        end_program(); return;
    }

    String o = "Portrait"; if(!pdfPortrait) {o = "Landscape";}
    String c = "black on white";
    if(pdfColors == 1) {c = "colours";} else if(pdfColors == 2) {c = "colours selected in \"Spana\"";}
    System.out.println("plot file: "+maybeInQuotes(pltFile.getName())+nl+
            "output file: "+maybeInQuotes(pdfFile.getName())+nl+
            "options:"+nl+
            "   size "+pdfSizeX+"/"+pdfSizeY+" %;"+
            "    left, bottom margins = "+pdfMarginL+", "+pdfMarginB+" cm"+nl+
            "   orientation = "+o+";   font = "+FONTS[pdfFont]+nl+
            "   "+c);
    System.out.print("converting to ");
    System.out.println("PDF ...");

    try{convert2PDF();}
    catch(Exception ex) {
        exception(ex, null, doNotStop);
        delete = true;
    }

    // --- close streams
    try{if(bufReader != null) {bufReader.close();}}
    catch (java.io.IOException ex) {
        exception(ex, "while closing file:"+nl+"   \""+pltFile+"\"", doNotStop);
    }
    if(outputFile != null) {
        try{
            outputFile.flush();
            outputFile.close();
        } catch (java.io.IOException ex) {
            exception(ex, "while closing file:"+nl+"   \""+pdfFile+"\"", doNotStop);
        }
    }
    if(delete) {pdfFile.delete();}

    end_program();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="parseArg">
  /** Interpret a command-line argument
   * @param arg String containing a command-line argument
   * @return false if there was an error associated with the command argument
   */
  private boolean parseArg(String arg) {
    if(arg == null) {return true;}
    if(arg.length() <=0) {return true;}
    if(arg.equals("-?") || arg.equals("/?") || arg.equals("?")) {
      printInstructions(System.out);
      return true;
    } //if args[] = "?"

    String msg = null;

    while(true) {

      if(arg.equalsIgnoreCase("-dbg") || arg.equalsIgnoreCase("/dbg")) {
            dbg = true;
            System.out.println("Debug printout = true");
            return true;
      } // -dbg
      else if(arg.equalsIgnoreCase("-nostop") || arg.equalsIgnoreCase("/nostop")) {
            doNotStop = true;
            if(dbg) {System.out.println("Do not show message boxes");}
            return true;
      } // -nostop
      else if(arg.equalsIgnoreCase("-bw") || arg.equalsIgnoreCase("/bw")) {
            if(dbg){System.out.println("Black on white");}
            pdfColors = 0;
            return true;
      } // -bw
      else if(arg.equalsIgnoreCase("-noh") || arg.equalsIgnoreCase("/noh")) {
            if(dbg){System.out.println("No header");}
            pdfHeader = false;
            return true;
      } // -noh
      else if(arg.equalsIgnoreCase("-clr") || arg.equalsIgnoreCase("/clr")) {
            if(dbg){System.out.println("Colours (default palette)");}
            pdfColors = 1;
            return true;
      } // -clr
      else if(arg.equalsIgnoreCase("-clr2") || arg.equalsIgnoreCase("/clr2")) {
            if(dbg){System.out.println("Colours (\"Spana\" palette)");}
            pdfColors = 2;
            return true;
      } // -clr2
      else {
        if(arg.length() >3) {
          String arg0 = arg.substring(0, 2).toLowerCase();
          if(arg0.startsWith("-p") || arg0.startsWith("/p")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String name = arg.substring(3);
                if(name.startsWith("\"") && name.endsWith("\"")) { //remove enclosing quotes
                    name = name.substring(1, name.length()-1);
                }
                if(!name.toLowerCase().endsWith(".plt")) {name = name.concat(".plt");}
                java.io.File f = new java.io.File(name);
                name = f.getAbsolutePath();
                f = new java.io.File(name);
                if(!f.exists()) {
                    msg = "Error: the plot file does not exist:"+nl+"   \""+f.getAbsolutePath()+"\"";
                    break;
                } // it is not a file
                if(f.isDirectory()) {
                    msg = "Error: \""+f.getAbsolutePath()+"\" is a directory.";
                    break;
                }
               if(!f.canRead()) {
                    msg = "Error: can not open file for reading:"+nl+"   \""+f.getAbsolutePath()+"\"";
                    break;
               }
              if(dbg){System.out.println("Plot file: "+f.getPath());}
              pltFile = f;
              return true;
            }// = or :
          } // if starts with "-p"
          else if(arg0.startsWith("-b") || arg0.startsWith("/b")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {pdfMarginB = Double.parseDouble(t);
                    pdfMarginB = Math.min(20,Math.max(pdfMarginB,-5));
                    if(dbg) {System.out.println("Bottom margin = "+pdfMarginB);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for bottom margin in text \""+t+"\"";
                  pdfMarginB = Double.NaN;
                  break;
                } //catch
            }// = or :
          } // if starts with "-b"
          else if(arg0.startsWith("-l") || arg0.startsWith("/l")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                try {pdfMarginL = Double.parseDouble(t);
                    pdfMarginL = Math.min(20,Math.max(pdfMarginL,-5));
                    if(dbg) {System.out.println("Left margin = "+pdfMarginL);}
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for left margin in text \""+t+"\"";
                  pdfMarginL = Double.NaN;
                  break;
                } //catch
            }// = or :
          } // if starts with "-l"
          else if(arg0.startsWith("-s") || arg0.startsWith("/s")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                int size;
                try {size = Integer.parseInt(t);
                    size = Math.min(300,Math.max(size,20));
                    if(dbg) {System.out.println("Output size = "+size+" %");}
                    pdfSizeX = size;
                    pdfSizeY = size;
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for output size in text \""+t+"\"";
                  size = 100;
                  pdfSizeX = size;
                  pdfSizeY = size;
                  break;
                } //catch
            }// = or :
          } // if starts with "-s"
          else if(arg0.startsWith("-o") || arg0.startsWith("/o")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3).toLowerCase();
                if(t.equals("p")) {pdfPortrait = true;} else
                    if(t.equals("l")) {pdfPortrait = false;}
                if(t.equals("p") || t.equals("l")) {
                    if(dbg) {
                        t = "Orientation = ";
                        if(pdfPortrait) {t = t + "Portrait";}
                        else {t = t + "Landscape";}
                        System.out.println(t);
                    }
                    return true;
                } else {
                    msg = "Error: Wrong format for orientation in text \""+t+"\"";
                    break;
                }
            }// = or :
          } // if starts with "-o"
          else if(arg0.startsWith("-f") || arg0.startsWith("/f")) {
            if(arg.charAt(2) == '=' || arg.charAt(2) == ':') {
                String t = arg.substring(3);
                int f;
                try {f = Integer.parseInt(t);
                    if(f >= 1 && f <= 4) {
                        pdfFont = f-1;
                        if(dbg) {System.out.println("Output font = "+FONTS[pdfFont]);}
                        return true;
                    } else {
                        msg = "Error: Wrong font type in text \""+t+"\"";
                        pdfFont = 2;
                        break;
                    }
                } catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for output size in text \""+t+"\"";
                  pdfFont = 2;
                  break;
                } //catch
            }// = or :
          } // if starts with "-f"
        } // if length >3

        if(arg.length() >4) {
          String arg0 = arg.substring(0, 3).toLowerCase();
          if(arg0.startsWith("-sx") || arg0.startsWith("/sx")) {
            if(arg.charAt(3) == '=' || arg.charAt(3) == ':') {
                String t = arg.substring(4);
                int size;
                try {size = Integer.parseInt(t);
                    size = Math.min(300,Math.max(size,20));
                    if(dbg) {System.out.println("Output X-size = "+size+" %");}
                    pdfSizeX = size;
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for output X-size in text \""+t+"\"";
                  size = 100;
                  pdfSizeX = size;
                  break;
                } //catch
            }// = or :
          } // if starts with "-sx"
          else if(arg0.startsWith("-sy") || arg0.startsWith("/sy")) {
            if(arg.charAt(3) == '=' || arg.charAt(3) == ':') {
                String t = arg.substring(4);
                int size;
                try {size = Integer.parseInt(t);
                    size = Math.min(300,Math.max(size,20));
                    if(dbg) {System.out.println("Output Y-size = "+size+" %");}
                    pdfSizeY = size;
                    return true;
                    } //try
                catch (NumberFormatException nfe) {
                  msg = "Error: Wrong numeric format for output Y-size in text \""+t+"\"";
                  size = 100;
                  pdfSizeY = size;
                  break;
                } //catch
            }// = or :
          } // if starts with "-sy"
        } // if length >4

        if(arg.length() >5) {
          String arg0 = arg.substring(0, 4).toLowerCase();
          if(arg0.startsWith("-pdf") || arg0.startsWith("/pdf")) {
            if(arg.charAt(4) == '=' || arg.charAt(4) == ':') {
                String name = arg.substring(5);
                if(name.startsWith("\"") && name.endsWith("\"")) { //remove enclosing quotes
                    name = name.substring(1, name.length()-1);
                }
                if(!name.toLowerCase().endsWith(".pdf")) {name = name.concat(".pdf");}
                if(dbg){System.out.println("PDF file: "+name);}
                pdfFile_name = name;
                return true;
            }// = or :
          } // if starts with "-pdf"
        }

      }

      // nothing matches
      break;

    } //while

    if(msg == null) {msg = "Error: can not understand command-line argument:"+nl+
            "  \""+arg+"\"";}
    else {msg = "Command-line argument \""+arg+"\":"+nl+msg;}
    if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
    System.out.println();
    printInstructions(System.out);
    return false;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="convert2PDF">
  private void convert2PDF() {
    // initialize the contents of some variables
    scaleX = (double)pdfSizeX/100d + 1e-10;
    scaleY = (double)pdfSizeY/100d + 1e-10;
    zx = pdfMarginL * 100;
    zy = pdfMarginB * 100;
    newPath = -1;
    x1st = -Double.MAX_VALUE; y1st = -Double.MAX_VALUE;

    //--- The PDF file is written in ISO-LATIN-1 encoding
    // Standard charsets:  Every implementation of the Java platform is
    // required to support the following standard charsets.
    // Charset      Description
    // US-ASCII     Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block
    //              of the Unicode character set
    // ISO-8859-1   ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1
    // UTF-8        Eight-bit UCS Transformation Format
    // UTF-16BE     Sixteen-bit UCS Transformation Format, big-endian byte order
    // UTF-16LE     Sixteen-bit UCS Transformation Format, little-endian byte order
    // UTF-16       Sixteen-bit UCS Transformation Format, byte order identified by
    //              an optional byte-order mark
    //--- make sure the output file can be written
    String msg;
    try{
        outputFile = new java.io.OutputStreamWriter(
                   new java.io.FileOutputStream(pdfFile), "ISO-8859-1");
    } catch (java.io.FileNotFoundException ex) {
        msg = "Error:"+nl+ex.getMessage()+nl+
                "****************************************"+nl+
                "**   Is the output PDF-file locked?   **"+nl+
                "****************************************";
        if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
        return;
    } catch (java.io.UnsupportedEncodingException ex) {
        msg = "while writing the PDF file:"+nl+
              "   \""+pdfFile.getAbsolutePath()+"\"";
        exception(ex, msg, doNotStop);
        return;
    }
    //--- files appear to be ok
    setPalette();

    //--- open the input plt-file
    bufReader = getBufferedReader(pltFile, doNotStop);
    if(bufReader == null) {return;}

    //PDF files have the following parts: a one-line header, a body consisting
    //of pdf-objects, a cross-reference table, and a short trailer

    //Write pdf-objects 1 to 21 (fonts, etc)
    pdf_Init(outputFile);

    //The PDF output is written here into a "stream" in pdf-object nbr.22
    //The length of the stream must preceede the stream itself.
    //This is done by writing the stream into a memory character array,
    //and after ending processing the input plot file, find out the
    //length of the stream, writing the length to the pdf file, followed
    //by the stream itself, and at the end the xref (cross-reference table).

    pdfStreamStart();
    if(pdfHeader) {
        pdfStreamAppendTo("BT"+nl+"/F0 8 Tf"+nl+"1 0 0 1 30 770 Tm"+nl+"0 0 0 rg");
        pdfStreamAppendTo("0 0 (File: "+fixTextSimple(maybeInQuotes(pltFile.getAbsolutePath()))+") \""+nl+
                "1 0 0 1 30 760 Tm");
        pdfStreamAppendTo("0 0 (Converted to pdf: "+getDateTime()+") \""+nl+"ET");
    }
    pdfStreamAppendTo("% - - - PlotPDF options:");
    String or = "portrait"; if(!pdfPortrait) {or = "landscape";}
    String c = "black on white";
    if(pdfColors == 1) {c = "colours";} else if(pdfColors == 2) {c = "colours selected in \"Spana\"";}
    pdfStreamAppendTo("% size "+pdfSizeX+"/"+pdfSizeY+" %;    left, bottom margins = "+pdfMarginL+", "+pdfMarginB+" cm"+nl+
            "% "+c+";   "+or+";   font = "+FONTS[pdfFont]+nl+
            "% - - - -  Plot file Start  - - - -");

    // ----------------------------------------------------
    int i0, i1, i2;
    String s0, s1, s2;
    String line;
    String comment;
    boolean readingText = false;
    /** -1=Left  0=center  +1=right */
    int align = 0, alignDef = 0;
    // ----- read all lines
    while(true) {
        try {line = bufReader.readLine();}
        catch (java.io.IOException ex) {
            msg = "while reading the plot file:"+nl+
                  "   \""+pltFile.getAbsolutePath()+"\"";
            exception(ex, msg, doNotStop);
            break;
        }
        if(line == null) {break;}

        // --- get i0, i1, i2 and any comment
        if(line.length() > 0) {s0= line.substring(0,1).trim();} else {s0 = "";}
        if(line.length() > 4) {s1= line.substring(1,5).trim();} else {s1 = "";}
        if(line.length() > 8) {s2= line.substring(5,9).trim();} else {s2 = "";}
        if(s0.length() > 0) {i0 = readInt(s0);} else {i0 = -1;}
        if(s1.length() > 0) {i1 = readInt(s1);} else {i1 = 0;}
        if(s2.length() > 0) {i2 = readInt(s2);} else {i2 = 0;}
        if(line.length() > 9) {
            comment= line.substring(9).trim();
            if(i0 != 0 || (!comment.startsWith("TextBegin") && !readingText)) {
                pdfStreamAppendTo("% "+comment);
            }
            if(comment.equals("TextEnd")) {readingText = false; continue;}
        } else {comment = "";}
        if(readingText) {continue;}
        if(comment.equals("-- HEADING --")) {alignDef = -1;}

        // --- change pen or colour
        if(i0!=0 && i0!=1) {
            if(i0==5 || i0==8) {setPen(i0,i1);}
            continue;
        }
        // at this point either i0 = 0  or  i0 = 1
        // --- line drawing
        if(pdfFont ==0
                || i0 == 1
                || !comment.startsWith("TextBegin")) {
            moveOrDraw(i0,(double)i1,(double)i2);
            continue;
        }

        // --- is there a text to print?
        //      at this point:  diagrConvertFont >0,
        //          i0 =0,  and  comment.startsWith("TextBegin")
        boolean isFormula = false;
        double txtSize, txtAngle;
        if(i0 == 0 && comment.length()>41 && comment.startsWith("TextBegin")) {
            if (comment.substring(9,10).equals("C")) {isFormula=true;}
            txtSize = readDouble(comment.substring(17,24));
            txtAngle = readDouble(comment.substring(35,42));
            // get alignment
            if(comment.length() > 42) {
                String t = comment.substring(55,56);
                if(t.equalsIgnoreCase("L")) {align = -1;}
                else if(t.equalsIgnoreCase("R")) {align = 1;}
                else if(t.equalsIgnoreCase("C")) {align = 0;}
            } else {align = alignDef;}
            // the text is in next line
            try{line = bufReader.readLine();}
            catch(java.io.IOException ex) {
                msg = "while reading the plot file:"+nl+
                      "   \""+pltFile.getAbsolutePath()+"\"";
                exception(ex, msg, doNotStop);
                break;
            }
            if(line != null) {
                if(line.length() > 9) {
                    comment= rTrim(line.substring(9));
                    if(comment.startsWith(" ")) {comment = comment.substring(1);}
                } else {comment = "";}
                // print the text
                printText(i1,i2,comment,isFormula,align,txtSize,txtAngle);
            } // if line != null
            // ------ read all lines until "TextEnd"
            readingText = true;
            } // if "TextBegin"
    } //while

    // --- finished reading the input plot file: write pdfStream to object 22
    writeObject22(outputFile);
    // --- write cross-reference table
    writeXref(outputFile);

  } //convert2PDF

  //<editor-fold defaultstate="collapsed" desc="getBufferedReader">
  private static java.io.BufferedReader getBufferedReader(java.io.File f, boolean noStop) {
    String msg;
    java.io.BufferedReader bufReader;
    java.io.InputStreamReader isr;
    try{
        isr = new java.io.InputStreamReader(new java.io.FileInputStream(f) , "UTF-8");
        bufReader = new java.io.BufferedReader(isr);
    }
    catch (java.io.FileNotFoundException ex) {
        msg = "File not found:"+nl+"   \""+f.getAbsolutePath()+"\"";
        exception(ex, msg, noStop);
        return null;
    } catch (java.io.UnsupportedEncodingException ex) {
        msg = "while reading the plot file:"+nl+
                "   \""+f.getAbsolutePath()+"\"";
        exception(ex, msg, noStop);
        return null;
    }
    return bufReader;
  }
  //</editor-fold>
  //<editor-fold defaultstate="collapsed" desc="readInt(String)">
    private static int readInt(String t) {
      int i;
      try{i = Integer.parseInt(t);}
      catch (java.lang.NumberFormatException ex) {
          System.out.println(DASH_LINE+nl+"Error: "+ex.toString()+nl+
                  "   while reading an integer from String: \""+t+"\""+nl+DASH_LINE);
          i = 0;}
      return i;
    } //readInt(T)
  //</editor-fold>
  //<editor-fold defaultstate="collapsed" desc="readDouble(String)">
    private static double readDouble(String t) {
      double d;
      try{d = Double.parseDouble(t);}
      catch (java.lang.NullPointerException ex) {
          System.out.println(DASH_LINE+nl+"Error: "+ex.toString()+nl+
                  "   while reading an floating-point number from String: \""+t+"\""+nl+DASH_LINE);
          d = 0;}
      return d;
    } //readDouble(T)
  //</editor-fold>

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="pdf_Init">
  /** write pdf-objects 1 to 21 (fonts, etc)*/
  private void pdf_Init(java.io.OutputStreamWriter o) {
    try {
        o.write("%PDF-1.1"+nl+"% see objects 21 and 22 below"+nl);
        // -- print the first objects
        o.write("1 0 obj"+nl+"<<"+nl+"/Type /Outlines"+nl+"/Count 0"+nl+">>"+nl+"endobj"+nl);
        o.write("2 0 obj"+nl+"<<"+nl+"/Type /Encoding"+nl+"/Differences ["+nl);
        pdfStreamStart();
        pdfStreamAppendTo("128 /euro");
        pdfStreamAppendTo("130 /quotesinglbase");
        pdfStreamAppendTo("131 /florin");
        pdfStreamAppendTo("132 /quotedblbase");
        pdfStreamAppendTo("133 /ellipsis");
        pdfStreamAppendTo("134 /dagger");
        pdfStreamAppendTo("135 /daggerdbl");
        pdfStreamAppendTo("136 /circumflex");
        pdfStreamAppendTo("137 /perthousand");
        pdfStreamAppendTo("138 /Scaron");
        pdfStreamAppendTo("139 /guilsinglleft");
        pdfStreamAppendTo("140 /OE");
        pdfStreamAppendTo("142 /Zcaron");
        pdfStreamAppendTo("145 /quoteleft");
        pdfStreamAppendTo("146 /quoteright");
        pdfStreamAppendTo("147 /quotedblleft");
        pdfStreamAppendTo("148 /quotedblright");
        pdfStreamAppendTo("149 /bullet");
        pdfStreamAppendTo("150 /endash");
        pdfStreamAppendTo("151 /emdash");
        pdfStreamAppendTo("152 /tilde");
        pdfStreamAppendTo("153 /trademark");
        pdfStreamAppendTo("154 /scaron");
        pdfStreamAppendTo("155 /guilsinglright");
        pdfStreamAppendTo("156 /oe");
        pdfStreamAppendTo("158 /zcaron");
        pdfStreamAppendTo("159 /Ydieresis");
        pdfStreamAppendTo("160 /space");
        pdfStreamAppendTo("164 /currency");
        pdfStreamAppendTo("166 /brokenbar");
        pdfStreamAppendTo("168 /dieresis");
        pdfStreamAppendTo("169 /copyright");
        pdfStreamAppendTo("170 /ordfeminine");
        pdfStreamAppendTo("172 /logicalnot");
        pdfStreamAppendTo("173 /hyphen");
        pdfStreamAppendTo("174 /registered");
        pdfStreamAppendTo("175 /macron");
        pdfStreamAppendTo("176 /degree");
        pdfStreamAppendTo("177 /plusminus");
        pdfStreamAppendTo("178 /twosuperior");
        pdfStreamAppendTo("179 /threesuperior");
        pdfStreamAppendTo("180 /acute");
        pdfStreamAppendTo("181 /mu");
        pdfStreamAppendTo("183 /periodcentered");
        pdfStreamAppendTo("184 /cedilla");
        pdfStreamAppendTo("185 /onesuperior");
        pdfStreamAppendTo("186 /ordmasculine");
        pdfStreamAppendTo("188 /onequarter");
        pdfStreamAppendTo("189 /onehalf");
        pdfStreamAppendTo("190 /threequarters");
        pdfStreamAppendTo("192 /Agrave");
        pdfStreamAppendTo("193 /Aacute");
        pdfStreamAppendTo("194 /Acircumflex");
        pdfStreamAppendTo("195 /Atilde");
        pdfStreamAppendTo("196 /Adieresis");
        pdfStreamAppendTo("197 /Aring");
        pdfStreamAppendTo("198 /AE");
        pdfStreamAppendTo("199 /Ccedilla");
        pdfStreamAppendTo("200 /Egrave");
        pdfStreamAppendTo("201 /Eacute");
        pdfStreamAppendTo("202 /Ecircumflex");
        pdfStreamAppendTo("203 /Edieresis");
        pdfStreamAppendTo("204 /Igrave");
        pdfStreamAppendTo("205 /Iacute");
        pdfStreamAppendTo("206 /Icircumflex");
        pdfStreamAppendTo("207 /Idieresis");
        pdfStreamAppendTo("208 /Eth");
        pdfStreamAppendTo("209 /Ntilde");
        pdfStreamAppendTo("210 /Ograve");
        pdfStreamAppendTo("211 /Oacute");
        pdfStreamAppendTo("212 /Ocircumflex");
        pdfStreamAppendTo("213 /Otilde");
        pdfStreamAppendTo("214 /Odieresis");
        pdfStreamAppendTo("215 /multiply");
        pdfStreamAppendTo("216 /Oslash");
        pdfStreamAppendTo("217 /Ugrave");
        pdfStreamAppendTo("218 /Uacute");
        pdfStreamAppendTo("219 /Ucircumflex");
        pdfStreamAppendTo("220 /Udieresis");
        pdfStreamAppendTo("221 /Yacute");
        pdfStreamAppendTo("222 /Thorn");
        pdfStreamAppendTo("223 /germandbls");
        pdfStreamAppendTo("224 /agrave");
        pdfStreamAppendTo("225 /aacute");
        pdfStreamAppendTo("226 /acircumflex");
        pdfStreamAppendTo("227 /atilde");
        pdfStreamAppendTo("228 /adieresis");
        pdfStreamAppendTo("229 /aring");
        pdfStreamAppendTo("230 /ae");
        pdfStreamAppendTo("231 /ccedilla");
        pdfStreamAppendTo("232 /egrave");
        pdfStreamAppendTo("233 /eacute");
        pdfStreamAppendTo("234 /ecircumflex");
        pdfStreamAppendTo("235 /edieresis");
        pdfStreamAppendTo("236 /igrave");
        pdfStreamAppendTo("237 /iacute");
        pdfStreamAppendTo("238 /icircumflex");
        pdfStreamAppendTo("239 /idieresis");
        pdfStreamAppendTo("240 /eth");
        pdfStreamAppendTo("241 /ntilde");
        pdfStreamAppendTo("242 /ograve");
        pdfStreamAppendTo("243 /oacute");
        pdfStreamAppendTo("244 /ocircumflex");
        pdfStreamAppendTo("245 /otilde");
        pdfStreamAppendTo("246 /odieresis");
        pdfStreamAppendTo("247 /divide");
        pdfStreamAppendTo("248 /oslash");
        pdfStreamAppendTo("249 /ugrave");
        pdfStreamAppendTo("250 /uacute");
        pdfStreamAppendTo("251 /ucircumflex");
        pdfStreamAppendTo("252 /udieresis");
        pdfStreamAppendTo("253 /yacute");
        pdfStreamAppendTo("254 /thorn");
        pdfStreamAppendTo("255 /ydieresis");
        pdfStreamPrint(o);
        objBytesNoNewLine[2] = objBytesNoNewLine[2] + (int)pdfStreanSize;
        o.write("]"+nl+">>"+nl+"endobj"+nl);
        // --- fonts:
        o.write("3 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F0"+nl+"/BaseFont /Courier"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("4 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F1"+nl+"/BaseFont /Courier-Bold"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("5 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F2"+nl+"/BaseFont /Courier-Oblique"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("6 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F3"+nl+"/BaseFont /Courier-BoldOblique"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("7 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F4"+nl+"/BaseFont /Helvetica"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("8 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F5"+nl+"/BaseFont /Helvetica-Bold"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("9 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F6"+nl+"/BaseFont /Helvetica-Oblique"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("10 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F7"+nl+"/BaseFont /Helvetica-BoldOblique"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("11 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F8"+nl+"/BaseFont /Symbol"+nl+">>"+nl+"endobj"+nl);
        o.write("12 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F9"+nl+"/BaseFont /Times-Roman"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("13 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F10"+nl+"/BaseFont /Times-Bold"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("14 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F11"+nl+"/BaseFont /Times-Italic"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("15 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F12"+nl+"/BaseFont /Times-BoldItalic"+nl+"/Encoding 2 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("16 0 obj"+nl+"<<"+nl+"/Type /Font"+nl+"/Subtype /Type1"+nl+
            "/Name /F13"+nl+"/BaseFont /ZapfDingbats"+nl+">>"+nl+"endobj"+nl);
        o.write("17 0 obj"+nl+"<<"+nl+"/Type /Catalog"+nl+
            "/Pages 19 0 R"+nl+"/Outlines 1 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("18 0 obj"+nl+"<<"+nl+"/ProcSet [ /PDF /Text ]"+nl+"/Font <<"+nl+
            "/F0 3 0 R"+nl+"/F1 4 0 R"+nl+"/F2 5 0 R"+nl+"/F3 6 0 R"+nl+"/F4 7 0 R"+nl+"/F5 8 0 R"+nl+"/F6 9 0 R"+nl+
            "/F7 10 0 R"+nl+"/F8 11 0 R"+nl+"/F9 12 0 R"+nl+"/F10 13 0 R"+nl+"/F11 14 0 R"+nl+"/F12 15 0 R"+nl+"/F13 16 0 R"+nl+
            ">>"+nl+">>"+nl+"endobj"+nl);
        //MediaBox [0 0 593 792]
        o.write("19 0 obj"+nl+"<<"+nl+"/Type /Pages"+nl+"/Count 1"+nl+
            "/Kids [ 20 0 R  ] "+nl+"/MediaBox [0 0 "+(int)MediaBox_X+" "+(int)MediaBox_Y+"]"+nl+
            "/Resources 18 0 R"+nl+">>"+nl+"endobj"+nl);
        o.write("20 0 obj"+nl+"<<"+nl+"/Type /Page"+nl+"/Parent 19 0 R"+nl+
            "/Contents 22 0 R"+nl+"/Resources 18 0 R"+nl+">>"+nl+"endobj"+nl);
        pdfStreamStart(); // this is not a "stream", but we want to count the bytes
        o.write("21 0 obj"+nl+"<<"+nl+
            "/CreationDate (D:"+pdfDate()+")"+nl);
        pdfStreamAppendTo(
            "/Producer (Plot-pdf [java]  by I.Puigdomenech "+VERS+")"+nl+
            "/Author ("+fixTextSimple(System.getProperty("user.name", "anonymous"))+")"+nl+
            "/Title ("+fixTextSimple(maybeInQuotes(pltFile.getName()))+")");
        pdfStreamPrint(o);
        objBytesNoNewLine[21] = objBytesNoNewLine[21] + (int)pdfStreanSize;
        o.write(">>"+nl+"endobj"+nl);
        o.flush();
    } catch (java.io.IOException ex) {
        exception(ex, null, doNotStop);
        delete = true;
    }
  }

  /** get the date in pdf-format: 20120808115058 (length = 14) */
  private String pdfDate() {
    java.text.DateFormat df = new java.text.SimpleDateFormat("yyyyMMddHHmmss");
    java.util.Date now = new java.util.Date();
    return df.format(now);
  }
  /** get the date and time in default format */
  private String getDateTime() {
    java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance
                (java.text.DateFormat.DEFAULT, java.text.DateFormat.DEFAULT,
                    java.util.Locale.getDefault());
    java.util.Date now = new java.util.Date();
    return df.format(now);
  }

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="writeObject22">
 /** write pdfStream.  */
  private void writeObject22(java.io.OutputStreamWriter o) {
      if(startedPrintingText) { // end printing text if needed
        pdfStreamAppendTo("ET");
        startedPrintingText = false;
      }
      if(newPath == 2) { // stroke the graphic path if needed
        if(Math.abs(x1st-xNow)<0.01 && Math.abs(y1st-yNow)<0.01) {
            pdfStreamAppendTo("s");
        } else {
            pdfStreamAppendTo("S");
        }
        newPath = 0;
        x1st = -Double.MAX_VALUE; y1st = -Double.MAX_VALUE;
    }
    pdfStreamAppendTo("% - - - -  Plot file End  - - - -");
    String pdfStreamSizeTxt = Long.toString(pdfStreanSize).trim();
    pdfStreamSizeTxtLength = pdfStreamSizeTxt.length();
    try{
        o.flush();
        o.write("22 0 obj"+nl+"<<"+nl+"/Length "+pdfStreamSizeTxt+nl+">>"+nl+
        "% If you make changes in the \"stream\" you must also change:"+nl+
        "% - In the the trailer, the line below the keyword \"startxref\":"+nl+
        "%   update the byte offset from the beginning of the file to the"+nl+
        "%   beginning of the \"xref\" keyword."+nl+
        "% - Update above the byte length of the \"stream\", including ends-of-line"+nl+
        "stream"+nl);
        o.write(pdfStream.toString());
        o.write("endstream"+nl+"endobj"+nl);
    } catch (java.io.IOException ex) {exception(ex, null, doNotStop); delete = true;}
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="writeXref">
  /** write the cross-reference table to output. */
  private void writeXref(java.io.OutputStreamWriter o) {
    String msg;
    if(pdfStreanSize <= 0) {
      msg = "Programming error in \"writeXref\": pdfStreanSize <= 0.";
      if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
      return;
    }
    long[] objStart = new long[22];
    long lng = 0;
    for(int i = 0; i < NBR_PDF_OBJECTS; i++) {
        lng = lng + (long)(objBytesNoNewLine[i] + (objNbrNewLines[i] * nlL));
        objStart[i] = lng;
    }
    try{
        o.flush();
        o.write("xref"+nl+"0 23"+nl+"0000000000 65535 f"+nl);
        String objStartTxt;
        for(int i = 0; i < NBR_PDF_OBJECTS; i++) {
            objStartTxt = String.format("%010d", objStart[i]);
            o.write(objStartTxt+" 00000 n");
            if(nlL ==1) {o.write(" ");}
            o.write(nl);
        }
        long xrefStart = objStart[NBR_PDF_OBJECTS-1]
            + 18 +(2*nlL) + pdfStreamSizeTxtLength + nlL
            +302 +(7*nlL) + pdfStreanSize + 15 + (2*nlL);
        o.write("trailer"+nl+"<<"+nl+"/Size 23"+nl+
            "/Info 21 0 R"+nl+"/Root 17 0 R"+nl+">>"+nl+
            "startxref"+nl+xrefStart+nl+"%%EOF"+nl);
        o.flush();
    } catch (java.io.IOException ex) {
        exception(ex, null, doNotStop);
        delete = true;
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="pdfStream">

  private StringBuffer pdfStream = new StringBuffer();
  private long pdfStreanSize = 0;
  private int pdfStreamSizeTxtLength;

  private synchronized void pdfStreamStart() {
    pdfStream.delete(0, pdfStream.length());
    pdfStreanSize = 0;
  }
  private synchronized void pdfStreamAppendTo(String t) {
    if(t == null || t.length() <=0) {return;}
    byte[] encodedBytes;
    try{
        encodedBytes = t.getBytes("ISO-8859-1");
        pdfStreanSize = pdfStreanSize + encodedBytes.length;
        pdfStream.append(t);
    } catch (java.io.UnsupportedEncodingException ex) {
        exception(ex, "while encoding to ISO-Latin-1 the text \""+t+"\"", doNotStop);
        delete = true;
    }
    // add end-of-line
    pdfStreanSize = pdfStreanSize + nlL;
    pdfStream.append(nl);
  }
  private void pdfStreamPrint(java.io.OutputStreamWriter o) {
    try{
        o.write(pdfStream.toString());
        o.flush();
    } catch (java.io.IOException ex) {
        exception(ex, "while writing \""+pdfStream.toString()+"\"", doNotStop);
        delete = true;
    }
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="setPalette">
  /** Set the values in "colours[]" depending on diagrConvertColors.
   * if diagrConvertColors = 2, then the palete selected in "Spana" is used,
   * if found. */
  private void setPalette() {
    if(pdfColors == 2) { // "Spana" palette
        java.io.FileInputStream fis = null;
        java.io.File f = null;
        boolean ok = false;
        while (true) { // if "Spana" palette not found: break
            java.util.Properties propertiesIni = new java.util.Properties();
            f = getSpana_Ini();
            if(f == null) {
                System.out.println("Warning: could not find file \".Spana.ini\""+nl+
                         "default colours will be used.");
                break;
            }
            try {
                fis = new java.io.FileInputStream(f);
                propertiesIni.load(fis);
            } //try
            catch (java.io.FileNotFoundException e) {
                System.out.println("Warning: file Not found: \""+f.getPath()+"\""+nl+
                         "default colours will be used.");
                break;
            } //catch FileNotFoundException
            catch (java.io.IOException e) {
                System.out.println("Error: \""+e.toString()+"\""+nl+
                   "   while loading file:"+nl+
                   "   \""+f.getPath()+"\""+nl+
                   "default colours will be used.");
                break;
            } // catch loading-exception
            if(dbg) {System.out.println("Reading colours from \""+f.getPath()+"\".");}
            int red, green, blue;
            float r, g, b;
            String rt, gt, bt;
            try{
                for(int ii=0; ii < colours.length; ii++) {
                    String[] c = propertiesIni.getProperty("Disp_Colour["+ii+"]").split(",");
                    if(c.length >0) {red =Integer.parseInt(c[0]);} else {red=0;}
                    if(c.length >1) {green =Integer.parseInt(c[1]);} else {green=0;}
                    if(c.length >2) {blue =Integer.parseInt(c[2]);} else {blue=0;}
                    r = Math.max(0f, Math.min(1f, (float)Math.max(0, Math.min(255, red))/255f));
                    g = Math.max(0f, Math.min(1f, (float)Math.max(0, Math.min(255, green))/255f));
                    b = Math.max(0f, Math.min(1f, (float)Math.max(0, Math.min(255, blue))/255f));
                    if(r < 0.000001) {rt ="0";} else if(r > 0.999999) {rt ="1";} else {rt = String.valueOf(r);}
                    if(g < 0.000001) {gt ="0";} else if(g > 0.999999) {gt ="1";} else {gt = String.valueOf(g);}
                    if(b < 0.000001) {bt ="0";} else if(b > 0.999999) {bt ="1";} else {bt = String.valueOf(b);}
                    colours[ii] = (rt+" "+gt+" "+bt).trim();
                } //for ii
            } catch (java.lang.NumberFormatException e) {
                System.out.println("Error: \""+e.toString()+"\""+nl+
                   "   while loading file:"+nl+
                   "   \""+f.getPath()+"\""+nl+
                   "default colours will be used.");
                break;
            }
            ok = true;
            break;
        } //while
        if(!ok) {pdfColors = 1;} // use the standard colours instead
        try{if(fis != null) {fis.close();}}
        catch (java.io.IOException e) {
            String msg = "Error: "+e.toString();
            if(f != null) {msg = msg +nl+"while closing \""+f.getPath()+"\"";}
            else {msg = msg+nl+"while closing \"null\"";}
            System.out.println(msg);
        }
    }
    if(pdfColors <=0 || pdfColors >2) {
        for(int i =0; i <  colours.length; i++) {
            colours[i] = "0 0 0";            //Black
        }
        return;
    }
    if(pdfColors == 1) { // standard palette
        colours[0] = "0 0 0";            //Black
        colours[1] = "1 0 0";            //light Red
        colours[2] = "0.5843 0.2627 0";  //Vermilion
        colours[3] = "0 0 1";            //Blue
        colours[4] = "0 0.502 0";        //Green
        colours[5] = "0.7843 0.549 0";   //Orange
        colours[6] = "1 0 1";            //Magenta
        colours[7] = "0 0 0.502";        //dark Blue
        colours[8] = "0.502 0.502 0.502";//Gray
        colours[9] = "0 0.651 1";        //Sky Blue
        colours[10]= "0.502 0 1";       //Violet
    }
    if(dbg) {
        System.out.println("Colours in use;");
        for(int ii=0; ii < colours.length; ii++) {
            System.out.println("  colour["+ii+"] = \""+colours[ii]+"\"");
        }
    }
  } // setPalette()

  private java.io.File getSpana_Ini() {
    java.io.File p;
    boolean ok;
    java.util.ArrayList<String> dirs = new java.util.ArrayList<String>(6);

    String home = getPathApp();
    if(home != null && home.trim().length() >0) {
        p = new java.io.File(home);
        if(p.exists()) {dirs.add(p.getAbsolutePath());}
    }
    String homeDrv = System.getenv("HOMEDRIVE");
    String homePath = System.getenv("HOMEPATH");
    if(homeDrv != null && homeDrv.trim().length() >0 && homeDrv.endsWith(SLASH)) {
        homeDrv = homeDrv.substring(0, homeDrv.length()-1);
    }
    if(homePath != null && homePath.trim().length() >0 && !homePath.startsWith(SLASH)) {
        homePath = SLASH + homePath;
    }
    if((homeDrv != null && homeDrv.trim().length() >0)
                && (homePath != null && homePath.trim().length() >0)) {
        if(homePath.endsWith(SLASH)) {homePath = homePath.substring(0, homePath.length()-1);}
        p = new java.io.File(homeDrv+homePath+SLASH+".config"+SLASH+"eq-diagr");
        if(p.exists()) {dirs.add(p.getAbsolutePath());}
    }
    home = System.getenv("HOME");
    if(home != null && home.trim().length() >0) {
        if(home.endsWith(SLASH)) {home = home.substring(0, home.length()-1);}
        p = new java.io.File(home+SLASH+".config"+SLASH+"eq-diagr");
        if(p.exists()) {dirs.add(p.getAbsolutePath());}
    }
    home = System.getProperty("user.home");
    if(home != null && home.trim().length() >0) {
        if(home.endsWith(SLASH)) {home = home.substring(0, home.length()-1);}
        p = new java.io.File(home+SLASH+".config"+SLASH+"eq-diagr");
        if(p.exists()) {dirs.add(p.getAbsolutePath());}
    }
    java.io.File f = null;
    for(String t : dirs) {
        if(t.endsWith(SLASH)) {t = t.substring(0, t.length()-1);}
        final String fileINIname = ".Spana.ini";
        p = new java.io.File(t+SLASH+fileINIname);
        if(p.exists() && p.canRead()) {
            if(f != null && p.lastModified() > f.lastModified()) {f = p;}
        }
    } // for(dirs)
    return f;
  } // getSpana_Ini()
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="setPen">
 /** Emulates several pens with line width or colours.
  * In black on white mode: if i=8 then the line thickness is changed,
  * if i=5 nothing happens.
  * In colour mode: if i=5 then the line colour is changed
  * (gray scale in non-colour printers), if i=8 nothing happens
  * @param i either 5 (change screen colour) or 8 (chenage plotter pen number)
  * @param pen0 the colour/pen nbr  */
  private void setPen(int i, int pen0) {
    if(i != 5 && i != 8) {return;}
    int pen = Math.max(1, pen0);
    pen--;

    if(i == 5) {    // Screen Colour
        if(pdfColors <=0) { //black/white
            colorNow = 0;
            return;
        }
        while(pen >= colours.length) {
            pen = pen - colours.length;
        }
        if(pen < 0) {pen = colours.length - pen;}
        colorNow = pen;
    }
    if(i == 8) {    // Pen Thickness
        while(pen >= WIDTHS.length) {
            pen = pen - WIDTHS.length;
        }
        if(pen < 0) {pen = WIDTHS.length - pen;}
        penNow = pen;
    }
  } //setPen
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="moveOrDraw">
  private void moveOrDraw (int i0, double x0, double y0) {
    if(startedPrintingText) { // end printing text if needed
        pdfStreamAppendTo("ET");
        startedPrintingText = false;
    }
    // --- newPath: -1 = start of program;
    // 0 = no drawing performed yet
    // 1 = some move performed
    // 2 = some move/line-draw performed
    if(newPath < 0) {newPath = 0;}
    double x, y, xPdf, yPdf, w;
    x = (x0*scaleX + zx) * SCALE_USER_SPACE;
    y = (y0*scaleY + zy) * SCALE_USER_SPACE;
    //---- move-to: if a drawing has been done: stroke and newpath.
    //              if i0=0   do not output anything, only save position.
    //              if i0=-1  force a move-to (for example, before a text-output)
    if(i0 == 0) {
        // drawing done?
        if(newPath == 2) {
            if(Math.abs(x1st-xNow)<0.01 && Math.abs(y1st-yNow)<0.01) {
                pdfStreamAppendTo("s");
            } else {
                pdfStreamAppendTo("S");
            }
            newPath = 0;
        }
        x1st = -Double.MAX_VALUE; y1st = -Double.MAX_VALUE;
    } //move to
    else if(i0 == 1) {
    //---- draw-to: if necessary do a move-to, then a line-to
        //line-draw after a 'move', 1st go to last point: xNow/yNow
        if(newPath ==0) {
            pdfStreamAppendTo(colours[colorNow]+" RG");
            w = WIDTHS[penNow] * (scaleX+scaleY)/2;
            w = Math.min(100,Math.max(0.05,w)) * SCALE_USER_SPACE;
            pdfStreamAppendTo(String.format(engl,"%20.5f",w).trim()+" w");
            //pdfStream.append("0 J"+nl); // lineCap = square
            //pdfStream.append("1 j"+nl); // lineJoin = round
            //pdfStream.append("[ ] 0 d"+nl); // lineDash = no dash solid pattern
            pdfStreamAppendTo("0 J 1 j [ ] 0 d");
            xPdf = xNow;  yPdf = yNow;
            if(pdfPortrait) {
                xPdf = xPdf + MARGIN_X;
            } else { // landscape
                xPdf = MediaBox_X - yNow;
                yPdf = xNow;
                xPdf = xPdf - MARGIN_X;
            }
            yPdf = yPdf + MARGIN_Y;
            pdfStreamAppendTo(toStr(xPdf)+" "+toStr(yPdf)+" m");
            newPath = 1;
            x1st = xNow;  y1st = yNow;
        } //if newPath =0
        xPdf = x;  yPdf = y;
        if(pdfPortrait) {
            xPdf = xPdf + MARGIN_X;
        } else { //landscape
            xPdf = MediaBox_X - y;
            yPdf = x;
            xPdf = xPdf - MARGIN_X;
        }
        yPdf = yPdf + MARGIN_Y;
        pdfStreamAppendTo(toStr(xPdf)+" "+toStr(yPdf)+" l");
        newPath = 2;
    } //draw to
    xNow = x;
    yNow = y;
  } // moveOrDraw
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="toStr(double)">
 /** Write a double into a string. If possible, write it
  * without the decimal point: 3.1 returns "3.1", while 3.0
  * returns "3"
  * @param d
  * @return  */
  private static String toStr(double d) {
    if(Double.isNaN(d)) {d = 0;}
    d = Math.min(999999999999999.9d,Math.max(-999999999999999.9d,d));
    String dTxt;
    if(Math.abs(d-Math.round(d)) >0.001) {
        dTxt = String.format(engl,"%20.3f", d);
    } else {dTxt = Long.toString(Math.round(d));}
    return dTxt.trim();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="fixTextSimple">
  /** Escape parenthesis and back-slash (that is, change "(" to "\(", etc);
   * and change hyphen, "-", to endash.
   * @param txt
   * @return    */
  private String fixTextSimple(String txt) {
    if(txt == null || txt.trim().length() <=0) {return txt;}
    StringBuilder t = new StringBuilder(txt);
    int i = 0;
    while(i < t.length()) {
      // "\" change to "\\"
      if(t.charAt(i) == '\\') {t.replace(i, i+1, "\\\\"); i++;}
      else if(t.charAt(i) == '(')  {t.replace(i, i+1, "\\("); i++;}
      else if(t.charAt(i) == ')')  {t.replace(i, i+1, "\\)"); i++;}
      else if(t.charAt(i) == '-')  {t.replace(i, i+1, "\\226"); i = i+3;} //emdash in pdf
      i++;
    } //while
    return t.toString();
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="maybeInQuotes">
  private String maybeInQuotes(String t) {
    if(t == null || t.length() <=0) {return t;}
    boolean encloseInQuotes = false;
    final String[] q = {"\"","\""};
    for(int i =0; i < t.length(); i++) {
        if(Character.isWhitespace(t.charAt(i))) {encloseInQuotes = true;}
    }
    if(encloseInQuotes) {return (q[0]+t+q[1]);} else {return t;}
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="printInstructions()">
  private static void printInstructions(java.io.PrintStream out) {
    if(out == null) {out = System.out;}
    out.flush();
    out.println("This program will convert a plot file (*.plt) into a PDF file."+nl+
            "Usage:  java -jar PlotPDF.jar [plot-file-name] [-command=value]");
    out.println("Possible commands are:"+nl+
        "  -b=bottom-margin  (in cm)"+nl+
        "  -bw  (black on white output)"+nl+
        "  -clr  (colours - standard palette)"+nl+
        "  -clr2 (colours selected in \"Spana\")"+nl+
        "  -dbg  (output debug information)"+nl+
        "  -f=font-nbr  (1:Vector_Graphics, 2:Times-Roman, 3:Helvetica, 4:Courier)"+nl+
        "  -l=left-margin  (in cm)"+nl+
        "  -noh   (do not make a header with file name and date)"+nl+
        "  -nostop  (do not stop for warnings)"+nl+
        "  -o=P/L  (P: portrait;  L: landscape)"+nl+
        "  -p=plot-file-name  (input \"plt\"-file in UTF-8 Unicode ecoding)"+nl+
        "  -pdf=output-file-name"+nl+
        "  -s=%  (output size; integer %-value)"+nl+
        "  -sx=% and -sy=% (different vertical and horizontal scaling)"+nl+
        "Enclose file-names with double quotes (\"\") it they contain blank space."+nl+
        "Example:  java -jar PlotPDF.jar \"Fe 53\" -PDF=\"pdf\\Fe 53.pdf\"  -s:60 -clr -F:2");
  } //printInstructions(out)
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="methods to print text">

  //<editor-fold defaultstate="collapsed" desc="printText">
 /** Print a text string.
  * @param i1 x-coordinate
  * @param i2 y-coordinate
  * @param txt the text to be printed
  * @param isFormula if the text is to handled as a chemical formula,
  * with super- and sub-scripts
  * @param align -1=Left  0=center  +1=right
  * @param txtSize size in cm
  * @param txtAngle angle in degrees  */
  private void printText(int i1, int i2, String txt,
          boolean isFormula, int align, double txtSize, double txtAngle) {
    final double[] FontScaleHeight={1.45, 1.35, 1.6};
    final double[] FontScaleWidth ={0.7, 0.71, 0.88};
    final String[] ALIGNMENTS = {"left","centre","right"};
    ChemFormula cf = null;

    if(newPath == 2) { // stroke the graphic path if needed
        if(Math.abs(x1st-xNow)<0.01 && Math.abs(y1st-yNow)<0.01) {
            pdfStreamAppendTo("s");
        } else {
            pdfStreamAppendTo("S");
        }
        newPath = 0;
        x1st = -Double.MAX_VALUE; y1st = -Double.MAX_VALUE;
    }

    if(dbg) {
        System.out.println("Print text \""+txt+"\""+nl+
            "   formula:"+isFormula+", align="+align+" size="+txtSize+" angle="+txtAngle);
    }

    if(txt == null || txt.trim().length() <=0) {return;}

    txt = rTrim(txt);
    // get angles between +180 and -180
    while (txtAngle>360) {txtAngle=txtAngle-360;}
    while (txtAngle<-360) {txtAngle=txtAngle+360;}
    if(txtAngle>180) {txtAngle=txtAngle-360;}
    if(txtAngle<-180) {txtAngle=txtAngle+360;}
    // font size
    double fontSize = 100 * ((scaleX+scaleY)/2)
                        * ((txtSize * FontScaleHeight[pdfFont-1]) + 0.001) * SCALE_USER_SPACE;
    fontSize = Math.min(5000, fontSize);
    fontSizeTxt = String.format(engl, "%15.2f",fontSize).trim();
    align = Math.max(-1,Math.min(1,align));

    String str= "% text at X,Y= "+String.valueOf(i1)+" "+String.valueOf(i2)+
                ";  size = "+toStr(txtSize)+";  angle = "+toStr(txtAngle)+";  is formula: "+isFormula+";  alignment:"+ALIGNMENTS[align+1];
    pdfStreamAppendTo(str);
    str = "%     text is: "+txt;
    pdfStreamAppendTo(str.trim());
    if(fontSize <=0.01) {
        pdfStreamAppendTo(str+nl+"%     (size too small)");
        return;
    }

    if(!startedPrintingText) {pdfStreamAppendTo("BT"); startedPrintingText = true;}

    if(penNow == 1) {
        changeFontString = setFontString(1); // bold
    } else {
        changeFontString = setFontString(0); // normal font
    }
    pdfStreamAppendTo(changeFontString);

    pdfStreamAppendTo(colours[colorNow]+" rg");


    boolean containsLetters = false;
    char c;
    for(int i =0; i < txt.length(); i++) {
        c = txt.charAt(i);
        if(Character.isDigit(c) || c == '-' || c == '+' || c == '.') {continue;}
        containsLetters = true;
        break;
    }
    // if it does not contain letters it can not be a chemical formula
    if(!containsLetters || txt.length()<=1) {
        isFormula = false;
        //align = 0; // center-align?
    }
    // find out the length of the text, and sub- and super-scripts
    int txtLength = txt.length();
    if(isFormula && txtLength > 1) {
        if(txt.equalsIgnoreCase("log (ai/ar")) {txt = "log (a`i'/a`ref'";}
        cf = new ChemFormula(txt, new float[1]);
        chemF(cf);
        txtLength = cf.t.length();
    }

    // --- position ---
    double xpl = (double)i1;
    double ypl = (double)i2;

    // --- recalculate position according to alignment etc ---
    double rads = Math.toRadians(txtAngle);
    double w;
    // for non-chemical formulas:
    //    - left aligned: the start position is given
    //    - right aligned: the start position is
    //      "exactly" calculated from the text length
    //    - centre alignment is aproximately calculated
    //      from the text length, as the width varies
    //      for different characters
    // for chemical formulas:
    //      alignment is always aproximately calculated
    //      from the text length, as the width varies
    //      for different characters
    if(!isFormula) { // not a chemical formula
        // for plain text: if right-aligned, move the origin
        if(align == 1 || align == 0) { //right aligned or centred
            if(align == 1) { //right aligned
                w = txtLength; // advance the whole requested text length
            } else { // if(align == 0) // approximate calculation
                // advance the whole text length minus the "real" text length
                w = txtLength * (1-FontScaleWidth[pdfFont-1]);
                w = w /2; // centre means only half
            }
            w = w * txtSize * 100;
            xpl = xpl + w * Math.cos(rads);
            ypl = ypl + w * Math.sin(rads);
        }
    } else { // it is a chemical formula
        if(Math.abs(txtAngle) < 0.01 && align == 0) {
            // (only for text not in axis: no special texts like 'pH', etc)
            // move the text up slightly (labels on curves look better if they
            // do not touch the lines of the curve): increase y-value 0.2*txtSize.
            // Also, move the text to the right a bit proportional to text
            // length. (only for text not in axis (angle must be zero, or text
            // only be numbers, and no special texts like 'pH', etc))
            if(!(txt.equals("pH") ||txt.equals("pe") ||txt.startsWith("Log") ||txt.startsWith("E / V"))
                        && !txt.contains("`TOT'")) {
                // label on a curve or predominance area
                ypl = ypl + 0.3*txtSize*100;
            }
        } //if angle =0
        // advance the whole text length minus the "real" text length
        w = txt.length() * (1-FontScaleWidth[pdfFont-1]);
        // remove things not printed. For example, in [Fe 3+]`TOT' there are three
        // characters not printed: the space before the electrical charge
        // and the accents surrounding "TOT"
        w = w - (txt.length()-txtLength);
        if(align == 0) { w = w /2; }
        if(align == 1 || align == 0) { //right aligned or centred
            w = w * txtSize * 100;
            xpl = xpl + w * Math.cos(rads);
            ypl = ypl + w * Math.sin(rads);
        }
    } // if is formula

    xpl = (xpl * scaleX + zx) * SCALE_USER_SPACE;
    ypl = (ypl * scaleY + zy) * SCALE_USER_SPACE;

    if(pdfPortrait) {
        xpl = xpl + MARGIN_X;
    } else { //landscape
        double x,y;
        x = xpl;  y = ypl;
        xpl = MediaBox_X - y;
        ypl = x;
        xpl = xpl - MARGIN_X;
        txtAngle = txtAngle + 90;
        rads = Math.toRadians(txtAngle);
    }
    ypl = ypl + MARGIN_Y;

    // start position and angle:
    str = toStr(xpl)+" "+toStr(ypl)+" Tm";
    if(Math.abs(txtAngle) <= 0.05) {
        pdfStreamAppendTo("1 0 0 1 "+str);
    } else { // angle !=0
        double cosine = Math.cos(rads);
        double sine = Math.sin(rads);
        String a1 = toStr(cosine);
        String a2 = toStr(sine);
        String a3 = toStr(-sine);
        pdfStreamAppendTo(a1+" "+a2+" "+a3+" "+a1+" "+str);
    } // angle?

    // --- do the printing itself ---
    if(isFormula && cf != null) {
        String move;
        String l2;
        int n1 = 0;
        int j = 1;
        while (j < cf.t.length()) {
          if(cf.d[j] != cf.d[j-1]) {
              l2 = cf.t.substring(n1, j);
              n1 = j;
              move = toStr(cf.d[j-1]*10) + " Ts ";
              l2 = "("+fixText(l2,isFormula)+") Tj";
              if(move.length() >0) {pdfStreamAppendTo(move+l2);} else {pdfStreamAppendTo(l2);}
          }
          j++;
        } //while
        if(n1 <= cf.t.length()) {
            l2 = cf.t.substring(n1);
            move = toStr(cf.d[j-1]*10) + " Ts ";
            l2 = "("+fixText(l2,isFormula)+") Tj 0 Ts";
            if(move.length() >0) {pdfStreamAppendTo(move+l2);} else {pdfStreamAppendTo(l2);}
        }
    } else { // not a chemical formula
        if(align > 0) { // right adjusted text
            //move to the left first by setting
            //  negative horizontal scale and no text rendering
            pdfStreamAppendTo("-100 Tz 3 Tr ("+fixText(txt,isFormula)+") Tj");
            //then display the text with
            //  normal horizontal scale and text rendering
            pdfStreamAppendTo("100 Tz 0 Tr ("+fixText(txt,isFormula)+") Tj");
        } else {
            pdfStreamAppendTo("0 Ts ("+fixText(txt,isFormula)+") Tj");
        }
    }

  } // printText
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="setFontString">
 /** Set the pdf-text to change a font
  * @param type 0 = normal; 1 = bold; 2 = italics; 3 = symbol  */
  private String setFontString(int type) {
    final String[] FONTN =  {"/F9", "/F4","/F0"}; //normal
    final String[] FONTBN = {"/F10","/F5","/F1"}; //bold
    final String[] FONTIN = {"/F11","/F6","/F2"}; //italics 
    String msg = null;
    if(type <0 || type >3) {
        msg = "type = "+type+" shoud be 0 to 3.";
    }
    if(pdfFont <0 || pdfFont >3) {
        msg = "diagrConvertFont = "+pdfFont+" shoud be 0 to 3.";
    }
    if(fontSizeTxt == null || fontSizeTxt.length() <=0) {
        msg = "fontSizeTxt is empty.";
    }
    if(msg != null) {
        msg = "Programming error in \"setFontString\":"+nl+msg;
        if(doNotStop) {System.out.println(msg);} else {ErrMsgBx mb = new ErrMsgBx(msg,progName);}
        return "";
    }
    String changeFontTxt;
    if(type == 1) {//bold font
        changeFontTxt = FONTBN[pdfFont-1];
    } else if(type == 2) {//italics
        changeFontTxt = FONTIN[pdfFont-1];
    } else if(type == 3) {//symbol
        changeFontTxt = "/F8";
    } else {//normal font
        changeFontTxt = FONTN[pdfFont-1];
    }
    changeFontTxt = changeFontTxt + " "+fontSizeTxt+" Tf";
    return changeFontTxt;
  }
  //</editor-fold>
  //<editor-fold defaultstate="collapsed" desc="fixText">
  /** Escape parenthesis and back-slash (that is, change "(" to "\(", etc);
   * and change hyphen, "-", to endash.
   * @param txt
   * @return    */
  private String fixText(String txt, boolean isFormula) {
    if(txt == null || txt.trim().length() <=0) {return txt;}
    // change "\", "(" and ")" to "\\", "\(" and "\)"
    StringBuilder t = new StringBuilder(fixTextSimple(txt));
    int i = 0;
    while(i < t.length()) {
      if(isFormula){ 
        if(t.charAt(i) == '~')  {t.replace(i, i+1, "\\260"); i = i+4; continue;} //degree
        else if(t.charAt(i) == '$')  {t.replace(i, i+1, "\\265"); i = i+4; continue;} //mu
      }
      // Delta:
      if(t.charAt(i) == 127 || (isFormula && t.charAt(i) == '^')) {
          String str = ") Tj /F8 "+fontSizeTxt+" Tf (D) Tj "+changeFontString+" (";
          t.replace(i, i+1, str);
          i = i+str.length();
          continue;
      }
      // versus:
      if(i <= (t.length()-6) && t.substring(i).startsWith("versus")) {
          changeFontString = setFontString(2); //italics
          String str = ") Tj "+changeFontString+" (versus) Tj ";
          if(penNow != 2) {changeFontString = setFontString(0);} else {changeFontString = setFontString(1);} // bold?
          str = str + changeFontString + " (";
          t.replace(i, i+6, str);
          i = i+str.length();
          continue;
      }
      // log: change to lower case
      if(i <= (t.length()-4)
              && (t.substring(i).startsWith("Log ") || t.substring(i).startsWith("Log[")
                || t.substring(i).startsWith("Log{"))) {
          t.replace(i, i+3, "log");
          i = i+3;
          continue;
      }
      // log P: change to lower case "log" and italics "p"
      if(i <= (t.length()-5)
              && t.substring(i).toLowerCase().startsWith("log p")) {
          changeFontString = setFontString(2); //italics
          String str = "log ) Tj "+changeFontString+" (p) Tj ";
          if(penNow != 2) {changeFontString = setFontString(0);} else {changeFontString = setFontString(1);} // bold?
          str = str + changeFontString + " (";
          t.replace(i, i+5, str);
          i = i+str.length();
          continue;
      }
      i++;
    } //while
    return t.toString();
  }
  //</editor-fold>
  //<editor-fold defaultstate="collapsed" desc="rTrim">
 /** Remove trailing white space. If the argument is null, the return value is null as well.
  * @param text input String.
  * @return text without trailing white space. */
  private static String rTrim(String text) {
    if(text == null) {return text;}
    //another possibility: ("a" + text).trim().substring(1)
    int idx = text.length()-1;
    if (idx >= 0) {
        //while (idx>=0 && text.charAt(idx) == ' ') {idx--;}
        while (idx>=0 && Character.isWhitespace(text.charAt(idx))) {idx--;}
        if (idx < 0) {return "";}
        else {return text.substring(0,idx+1);}
    }
    else {  //if length =0
        return text;
    }
  } // rTrim
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="class ChemFormula">
  /** Class used for input and output from method "chemF" */
  static class ChemFormula {
        /** t is a StringBuffer with the initial chemical formula given
         * to method "chemF", and it will be modified on return
         * from chemF, for example "Fe+2" may be changed to "Fe2+". */
        StringBuffer t;
        /** d float[] returned by method "chemF" with
         * super- subscript data for each character position
         * in the StringBuffer t returned  by chemF. */
        float[] d;
        /** @param txt String
        * @param d_org float[] */
        ChemFormula(String txt, float[] d_org) { // constructor
            t = new StringBuffer();
            //if (t.length()>0) {t.delete(0, t.length());}
            t.append(txt);
            int nChar = txt.length();
            d = new float[nChar];
            System.arraycopy(d_org, 0, d, 0, d_org.length); }
    } // class ChemFormula
  //</editor-fold>

//<editor-fold defaultstate="collapsed" desc="chemF">
/** Purpose: given a "chemical formula" in StringBuffer <b>t</b> it returns
 * an array <b>d</b> indicating which characters are super- or
 * sub-scripted and a new (possibly modified) <b>t</b>.
 * Both <b>t</b> and <b>d</b> are stored for input
 * and output in objects of class ChemFormula. Note that <b>t</b> may
 * be changed, e.g. from "Fe+2" to "Fe2+".
 * <p>
 * Stores in array <b>d</b> the vertical displacement of each
 * character (in units of character height).  In the case of HCO3-, for example,
 * d(1..3)=0, d(4)=-0.4, d(5)=+0.4
 * <p>
 * Not only chemical formulas, but this method also "understands" math
 * formulas: x'2`, v`i', log`10', 1.35'.`10'-3`
 * <p>
 * For aqueous ions, the electric charge may be written as either a space and
 * the charge (Fe 3+, CO3 2-, Al(OH)2 +), or as a sign followed by a number
 * (Fe+3, CO3-2, Al(OH)2+). For charges of +-1: with or without a space
 * sepparating the name from the charge (Na+, Cl-, HCO3- or Na +, Cl -, HCO3 -).
 * <p>
 * Within this method, <b>t</b> may be changed, so that on return:
 * <ul><li>formulas Fe+3, CO3-2 are converted to Fe3+ and CO32-
 *  <li>math formulas: x'2`, v`i', log`10', 1.35'.`10'-3`
 *      are stripped of "`" and "'"
 *  <li>@ is used to force next character into base line, and
 *      the @ is removed (mostly used for names containing digits and
 *      some math formulas)</ul>
 * <p>
 * To test:<br>
 * <pre>String txt = "Fe(CO3)2-";
 * System.out.println("txt = \""+txt+"\","+
 *   " new txt = \""+chemF(new ChemFormula(txt, new float[1])).t.toString()+"\","+
 *   " d="+Arrays.toString(chemF(new ChemFormula(txt, new float[1])).d));</pre></p>
 *
 * <p>Version: 2013-Apr-03.
 * 
 * @param cf ChemFormula
 * @return new instance of ChemFormula with d[] of super- subscript
 * positions for each character and a modified StingBuffer t. */
private static void chemF(ChemFormula cf) {
int act; // list of actions:
    final int REMOVE_CHAR = 0;
    final int DOWN = 1;
    final int NONE = 2;
    final int UP = 3;
    final int CONT = 4;
    final int END = 6;
int nchr; char t1; char t2; int i;
float dm; float df;
final char M1='\u2013'; // Unicode En Dash
final char M2='\u2212'; // Minus Sign = \u2212
// The characters in the StringBuffer are stored in "int" variables
// Last, Next-to-last, Next-to-Next-to-last
//                          ow (present character)
//                                  Next, Next-Next, Next-Next-Next
int L; int nL; int nnL; int now; int n; int n2; int n3;
// ASCII Table:
// -----------------------------------------------------------------------------
// 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57
//     !  "  #  $  %  &  '  (  )  *  +  ,  -  .  /  0  1  2  3  4  5  6  7  8  9
// -----------------------------------------------------------------------------
// 58 59 60 61 62 63 64 65 66 67 68 69 70 71 72 73 74 75 76 77 78 79 80 81 82 83
//  :  ;  <  =  >  ?  @  A  B  C  D  E  F  G  H  I  J  K  L  M  N  O  P  Q  R  S
// -----------------------------------------------------------------------------
// 84 85 86 87 88 89 90 91 92 93 94 95 96 97 98 99 00 01 02 03 04 05 06 07 08 09
//  T  U  V  W  X  Y  Z  [  \  ]  ^  _  `  a  b  c  d  e  f  g  h  i  j  k  n  m
// -----------------------------------------------------------------------------
//110 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26
//  n  o  p  q  r  s  t  u  v  w  x  y  z  {  |  }  ~
// -----------------------------------------------------------------------------
// If the string is too short: do nothing
StringBuffer t = new StringBuffer();
t.append(cf.t);
L = t.toString().trim().length();
if(L <= 1) {return;}
// Remove trailing space from the StringBuffer
L = t.length();  // total length
nchr = rTrim(t.toString()).length(); // length without trailing space
if(L > nchr) {t.delete(nchr, L);}
// create d and initialize it
float[] d = new float[nchr];
for(i = 0; i < d.length; i++) {d[i] = 0f;}
// set the non-existing characters to "space"
//     Last, Next-to-last, Next-to-Next-to-last
L = 32; nL = 32; nnL = 32;
// get the ASCII code
now = t.charAt(0);
// get the following characters
n = 32;  if (nchr >=2) {n  = t.charAt(1);} // Next
n2 = 32; if (nchr >=3) {n2 = t.charAt(2);} // Next-Next
// dm (Displacement-Math) for '` sequences: 1.5'.`10'-12`, log p`CO2'
dm = 0f;
// df (Displacement-Formulas) in chemical formulas: CO3 2-, Fe 3+, Cl-, Na+
df = 0f;
//
// -----------------------------
//    Main Loop
//
i = 0;
main_do_loop:
do {
  n3 = 32;
  if(nchr >=(i + 4)) {n3 = t.charAt(i + 3);} //Next-Next-Next
  // Because a chemical formula only contains supersripts at the end
  // (the electric charge), a superscrip character marks the end of a formula.
  // Set df=0 after superscript for characters that are not 0:9 + or -
  if(df > 0.001f) { //     (if last was supercript, i.e. df>0)
    if (now <43 || now >57 || now ==44 || now ==46 || now ==47) //not +- 0:9
         {df = 0f;}
  } //df > 0.001

  checks: { // named block of statements to be used with "break"
    // ---------------
    //   for @
    // ---------------
    //   if the character is @, do nothing; if last char. was @, treat "now" as a
    //   "normal" char. (and therefore, if last char. was @, and "now" is a space,
    //   leave a blank space);
    if(now ==64 && L !=64) { act =REMOVE_CHAR; break checks;}
    if(L ==64) { // @
      if(now ==32) {act =CONT;} else {act =NONE;}
      break checks;
    } // Last=@
    // ---------------
    //   for Ctrl-B (backspace)
    // ---------------
    if(now ==2) {
      now = n; n = n2; n2 = n3;
      act =END; break checks;
    } // now = backspace
    // ---------------
    //   for ' or `
    // ---------------
    if(now ==39 || now ==96) {
      //     for  '`, `' sequences: change the value of variable dm.
      //     if it is a ' and we are writing a "normal"-line, and next is either
      //     blank or s (like it's or it's.): write it normal
      if(now ==39 && dm ==0
              && ( (n ==32 && L ==115)
              || (n ==115 && (n2==32||n2==46||n2==44||n2==58||n2==59||n2==63
                        ||n2==33||n2==41||n2==93)) ) )
                {act =CONT; break checks;}
      if(now ==39) {dm = dm + 0.5f;}
      if(now ==96) {dm = dm - 0.5f;}
      act =REMOVE_CHAR; break checks;
    } // now = ' or `
    // ---------------
    //   for BLANK (space)
    // ---------------
    // Decide if the blank must be printed or not.
    // In front of an electric charge: do not print (like CO3 2-, etc)
    if(now ==32) {
      // if next char. is not a digit (1:9) and not (+-), make a normal blank
      if((n <49 && (n !=43 && n !=45 &&n!=M1&&n!=M2)) || n >57) {act =NONE; break checks;}
      // Space and next is a digit or +-:
      // if next is (+-) and last (+-) make a normal blank
      if((n ==43 || n ==45 || n==M1||n==M2)
              && (L ==43 || L ==45 ||L==M1||L==M2)) {act =NONE; break checks;}
      // if the 2nd-next is letter (A-Z, a-z), make a normal blank
      if((n2 >=65 && n2 <=90) || (n2 >=97 && n2 <=122)) {act =NONE; break checks;}
      // if next is a digit (1-9)
      if(n >=49 && n <=57) { // 1:9
        // and 2nd-next is also a digit and 3rd-next is +-  " 12+"
        if(n2 >=48 && n2 <=57) { // 0:9
            if (n3 !=43 && n3 !=45 &&n3!=M1&&n3!=M2) {act =NONE; break checks;} // n3=+:-
        } // n2=0:9
        // if next is (1-9) and 2nd-next is not either + or -, make a blank
        else if (n2 !=43 && n2 !=45 &&n2!=M1&&n2!=M2) {act =NONE; break checks;}
      } // n=1:9
      // if last char. was blank,  make a blank
      if(L ==32) {act =NONE; break checks;} // 110 //
      // if last char. was neither a letter (A-Z,a-z) nor a digit (0:9),
      // and was not )]}"'>, make a blank
      if((L <48 || L >122 || (L >=58 && L <=64) || (L >=91 && L<= 96))
        && (L !=41 && L !=93 && L !=125 && L !=62 && L !=34 && L !=39))
                    {act =NONE; break checks;}
      // Blanks followed either by + or -, or by a digit (1-9) and a + or -
      // and preceeded by either )]}"'> or a letter or a digit:
      // do not make a blank space.
      act =REMOVE_CHAR; break checks;
    } //now = 32
    // ---------------
    //   for Numbers (0:9)
    // ---------------
    // In general a digit is a subscript, except for electric charges...etc
    if(now >=48 && now <=57) {
      // if last char. is either ([{+- or an impossible chem.name,
      // then write it "normal"
      if(L !=40 && L !=91 && L !=123 && L !=45 &&L!=M1&&L!=M2&& L !=43) { // Last not ([{+-
        if((L ==106 || L ==74) || (L ==113 || L ==81)) {act =NONE; break checks;} // jJ qQ
        if((L ==120 || L ==88) || (L ==122 || L ==90)) {act =NONE; break checks;} // xX zZ
        //if last char. is )]} or a letter (A-Z,a-z): write it 1/2 line down
        if((L >=65 && L <=90) || (L >=97 && L <=122)) {act =DOWN; break checks;} // A-Z a-z
        if(L ==41 || L ==93 || L ==125) {act =DOWN; break checks;} // )]}
        //if last char. is (0:9 or .) 1/2 line (down or up), keep it
        if(df >0.01f && ((L >=48 && L <=57) || L ==46)) {act =CONT; break checks;}
        if(df <-0.01f && (L ==46 || (L >=48 && L <=57))) {act =CONT; break checks;}
      } // Last not ([{+-
      // it is a digit and last char. is not either of ([{+-)]} or A:Z a:z
      // is it an electric charge?
      df = 0f; //125//
      // if last char is space, and next char. is a digit and 2nd-next is a +-
      // (like: W14O41 10+)  then write it "up"
      if(L ==32 && (n >=48 && n <=57)
            && (n2 ==43 || n2 ==45 ||n2 ==M1||n2 ==M2) && now !=48) {
        //if 3rd-next char. is not (space, )]}), write it "normal"
        if(n3 !=32 && n3 !=41 && n3 !=93 && n3 !=125) {act =CONT; break checks;}  // )]}
        act =UP; break checks;
      }
      // if last is not space or next char. is not one of +-, then write it "normal"
      if(L !=32 || (n !=43 && n !=45 &&n!=M1&&n!=M2)) {act =CONT; break checks;}              // +-
      // Next is +-:
      // if 2nd-next char. is a digit or letter, write it "normal"
      if((n2 >=48 && n2 <=57) || (n2 >=65 && n2 <=90)
            || (n2 >=97 && n2 <=122)) {act =CONT; break checks;}                    // 0:9 A:Z a:z
      // if last char. was a digit or a blank, write it 1/2 line up
      // if ((L >=48 && L <=57) || L ==32) {act =UP; break checks;}
      //     act =CONT; break checks;
      act =UP; break checks;
    } // now = (0:9)
    // ---------------
    //   for Signs (+ or -)
    // ---------------
    // Decide if it is an electric charge...
    //  and convert things like: CO3-2  to: CO3 2-
    if(now ==43 || now ==45 ||now ==M1||now ==M2) {
      // First check for charges like: Fe 2+  and  W16O24 11-
      //  If last char. was a digit (2:9) written 1/2 line up, write it
      //  also 1/2 line up (as an electrical charge in H+ or Fe 3+).
      if(L >=50 && L <=57 && df >0.01f) {act =UP; break checks;}                // 2:9
      // charges like: W16O24 11-
      if((L >=48 && L <=57) && (nL >=49 && nL <=57)
              && (df >0.01f)) {act =UP; break checks;}                            // 0:9 1:9
      //is it a charge like: Fe+3 ?   ------------------------
      if(n >=49 && n <=57) {                                       // 1:9
      // it is a +- and last is not superscript and next is a number:
      // check for impossible cases:
      // if 2nd to last was a digit or a period and next a digit: 1.0E-1, 2E-3, etc
        if(((nL >=48 && nL <=57) || nL ==46) && (L ==69 || L==101)) {act =NONE; break checks;} // 09.E
        if(L ==32) {act =NONE; break checks;}                                      // space
        if((L ==106 || L ==74) || (L ==113 || L ==81)) {act =NONE; break checks;}  // jJ qQ
        if((L ==120 || L ==88) || (L ==122 || L ==90)) {act =NONE; break checks;}  // xX zZ
        // if last was 0 and 2nd to last 1 and dm>0 (for 10'-3` 10'-3.1` or 10'-36`)
        if(dm >0.01f && (L ==39 && nL ==48 && nnL ==49)
                && (n >=49 && n <=57)
                && (n2 ==46 || n2 ==96 || (n2 >=48 && n2 <=57))) {act =NONE; break checks;}
        // allowed in L: numbers, letters and )]}"'
        // 1st Line: !#$%&(*+,-./    2nd line: {|~:;<=?@     3rd Line: [\^_`
        if((L <48 && L !=41 && L !=34 && L !=39)
                || (L >122 && L !=125) || (L >=58 && L <=64 && L !=62)
                || (L >=91 && L <=96 && L !=93)) {act =NONE; break checks;}
        if((L ==41 || L ==93 || L ==125 || L ==34 || L ==39)
                && (nL ==32 || nL <48 || nL >122 || (nL >=58 && nL <=64)
                || (nL >=91 && nL <=96))) {act =NONE; break checks;}                   // )]}"'
        // allowed in n2: numbers and space )']`}
        // 1st Line: !"#$%&(*+,-./   2nd Line: except ]`}
        if((n2 <48 && n2 !=41 && n2 !=32 && n2 !=39)
                || (n2 >57 && n2 !=93 && n2 !=96 && n2 !=125)) {act =NONE; break checks;}
        //it is a +- and last is not superscript and next is a number:
        // is it a charge like:  W14O41-10 ?
        if((n >=49 && n <=57) && (n2 >=48 && n2 <=57)) {    // 1:9 0:9
          // characters allowed after the electric charge:  )]}`'@= space and backsp
            if(n3 !=32 && n3 !=2 && n3 !=41 && n3 !=93 && n3 !=125
                    && n3 !=96 && n3 !=39 && n3 !=64 && n3 !=61) {act =NONE; break checks;}
          // it is a formula like "W12O41-10"  convert to  "W12O41 10-"  and move "up"
          t1 = t.charAt(i+1); t2 = t.charAt(i+2);
          t.setCharAt(i+2, t.charAt(i));
          t.setCharAt(i+1, t2);
          t.setCharAt(i, t1);
          int j1 = n;  int j2 = n2;
          n2 = now;
          n = j2; now = j1;
          act =UP; break checks;
        } // 1:9 0:9
        // is it a charge like Fe+3, CO3-2 ?
        if(n >=50 && n <=57) {                                   // 2:9
          //it is a formula of type Fe+3 or CO3-2
          // convert it to Fe3+ or CO32-  and move "up"
          t1 = t.charAt(i+1);
          t.setCharAt(i+1, t.charAt(i));
          t.setCharAt(i, t1);
          int j = n; n = now; now = j;
          act =UP; break checks;
        } // 2:9
      } // 1:9
      // it is a +- and last is not superscript and:  next is not a number:
      // write it 1/2 line up (H+, Cl-, Na +, HCO3 -), except:
      //    1) if last char. was either JQXZ, or not blank-letter-digit-or-)]}, or
      //    2) if next char. is not blank or )}]
      //    3) if last char. is blank or )]}"' and next-to-last is not letter or digit
      if(L ==106 || L ==74 || L ==113 || L ==81) {act =NONE; break checks;}           // jJ qQ
      if(L ==120 || L ==88 || L ==122 || L ==90) {act =NONE; break checks;}          // xX zZ
      if(L ==32 || L ==41 || L ==93 || L ==125) {                     // space )]}
        //1st Line: !"#$%&'(*+,-./{|}~      2nd Line: :;<=>?@[\^_`
        if(nL ==32 || (nL <48 && nL !=41) || (nL >122)
                || (nL >=58 && nL <=64)
                || (nL >=91 && nL <=96 && nL !=93)) {act =NONE; break checks;}
        act =UP; break checks;
      } // space )]}
      // 1st Line: !#$%&(*+,-./{|~     2nd Line: :;<=>?@[\^_`
      if((L <48 && L !=41 && L !=34 && L !=39)
              || (L >122 && L !=125) || (L >=58 && L <=64)
              || (L >=91 && L <=96 && L !=93)) {act =NONE; break checks;}
      if(n !=32 && n !=41 && n !=93 && n !=125 && n !=61
              && n !=39 && n !=96) {act =NONE; break checks;}                             // )]}=`'
      act =UP; break checks;
    } // (+ or -)
    // ---------------
    //   for Period (.)
    // ---------------
    if (now ==46) {
      // if last char was a digit (0:9) written 1/2 line (down or up)
      // and next char is also a digit, continue 1/2 line (down or up)
      if((L >=48 && L <=57) && (n >=48 && n <=57)) {act =CONT; break checks;} // 0:9
      act =NONE; break checks;
    } // (.)
    act =NONE;
  } // --------------- "checks" (end of block name)

  switch_action:
  switch (act) {
    case REMOVE_CHAR:
        // ---------------
        //   take OUT present character //150//
        if(i <nchr) {t.deleteCharAt(i);}
        nchr = nchr - 1;
        nnL = nL; nL = L; L = now; now = n; n = n2; n2 = n3;
        continue; // main_do_loop;
    case DOWN:
        df = -0.4f;       // 170 //
        break; // switch_action;
    case NONE:
        df = 0.0f;        // 180 //
        break; // switch_action;
    case UP:
        df = 0.4f;        // 190 //
        break; // switch_action;
    case CONT:
    case END:
    default:
  } // switch_action

  if(act != END) {// - - - - - - - - - - - - - - - - - - - // 200 //
    nnL = nL;
    nL = L;
    L = now;
    now = n;
    n = n2;
    n2 = n3;
  }

  d[i] = dm + df; //201//
  i = i + 1;
} while (i < nchr); // main do-loop

// finished
// store results in ChemFormula cf
if(cf.t.length()>0) {cf.t.delete(0, cf.t.length());}
cf.t.append(t);
cf.d = new float[nchr];
System.arraycopy(d, 0, cf.d, 0, nchr);
// return;
} // ChemFormula chemF
//</editor-fold>

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="exception(Exception ex)">
 /** return the message "msg" surrounded by lines, including the calling method. 
  * @param ex
  * @param msg
  * @param doNotStop
  */
  private static void exception(Exception ex, String msg, boolean doNotStop) {
    final String ERR_START = "============================";
    String errMsg = ERR_START;
    String exMsg = ex.toString();
    if(exMsg == null || exMsg.length() <=0) {exMsg = "Unknown error.";} else {exMsg = "Error: "+exMsg;}
    errMsg = errMsg +nl+ exMsg;
    if(msg != null && msg.trim().length() >0) {errMsg = errMsg + nl + msg;}
    errMsg = errMsg + nl +stack2string(ex) + nl + ERR_START;
    if(doNotStop) {System.out.println(errMsg);} else {ErrMsgBx mb = new ErrMsgBx(errMsg,progName);}
  } //error(msg)
  //<editor-fold defaultstate="collapsed" desc="stack2string(Exception e)">
 /** returns a <code>printStackTrace</code> in a String, surrounded by two dash-lines.
  * @param e Exception
  * @return printStackTrace */
  private static String stack2string(Exception e) {
    try{
      java.io.StringWriter sw = new java.io.StringWriter();
      java.io.PrintWriter pw = new java.io.PrintWriter(sw);
      e.printStackTrace(pw);
      String t = sw.toString();
      if(t != null && t.length() >0) {
          int i = t.indexOf("Unknown Source");          
          int j = t.indexOf("\n");
          if(i>0 && i > j) {
              t = t.substring(0,i);
              j = t.lastIndexOf("\n");
              if(j>0) {t = t.substring(0,j)+nl;}
          }
      }
      return "- - - - - -"+nl+
             t +
             "- - - - - -";
    }
    catch(Exception e2) {
      return "Internal error in \"stack2string(Exception e)\"";
    }
  } //stack2string(ex)
  //</editor-fold>
  // </editor-fold>

//<editor-fold defaultstate="collapsed" desc="getPathApp">
/** Get the path where an application is located.
 * @return the directory where the application is located,
 * or "user.dir" if an error occurs
 * @version 2014-Jan-17 */
  private static class C {private static void C(){}}
  public static String getPathApp() {
    C c = new C();
    String path;
    java.net.URI dir;
    try{
        dir = c.getClass().
                getProtectionDomain().
                    getCodeSource().
                        getLocation().
                            toURI();
        if(dir != null) {
            String d = dir.toString();
            if(d.startsWith("jar:") && d.endsWith("!/")) {
                d = d.substring(4, d.length()-2);
                dir = new java.net.URI(d);
            }
            path = (new java.io.File(dir.getPath())).getParent();
        } else {path = System.getProperty("user.dir");}
    }
    catch (java.net.URISyntaxException e) {
      if(!started) {
        ErrMsgBx emb = new ErrMsgBx("Error: "+e.toString()+nl+
                    "   trying to get the application's directory.", progName);
      }
      path = System.getProperty("user.dir");
    } // catch
    started = true;
    return path;
  } //getPathApp()
//</editor-fold>

//<editor-fold defaultstate="collapsed" desc="ErrMsgBx">
  /** Displays a "message box" modal dialog with an "OK" button.<br>
   * Why is this needed? For any java console application: if started using
   * javaw.exe (on Windows) or through a ProcessBuilder, no console will appear.
   * Error messages are then "lost" unless a log-file is generated and the user
   * reads it. This class allows the program to stop running and wait for the user
   * to confirm that the error message has been read.
   * <br>
   * A small frame (window) is first created and made visible. This frame is
   * the parent to the modal "message box" dialog, and it has an icon on the
   * task bar (Windows).  Then the modal dialog is displayed on top of the
   * small parent frame.
   * <br>
   * Copyright (C) 2015  I.Puigdomenech.
   * @author Ignasi Puigdomenech
   * @version 2015-July-14 */
  static class ErrMsgBx {

  /** Displays a "message box" modal dialog with an "OK" button and a title.
   * The message is displayed in a text area (non-editable),
   * which can be copied and pasted elsewhere.
   * @param msg will be displayed in a text area, and line breaks may be
   * included, for example: <code>new MsgBox("Very\nbad!",""); </code>
   * If null or empty nothing is done.
   * @param title for the dialog. If null or empty, "Error:" is used
   * @version 2014-July-14 */
  public ErrMsgBx(String msg, String title) {
    if(msg == null || msg.trim().length() <=0) {
        System.out.println("--- MsgBox: null or empty \"message\".");
        return;
    }
    //--- Title
    if(title == null || title.length() <=0) {title = " Error:";}
    java.awt.Frame frame = new java.awt.Frame(title);
    //--- Icon
    String iconName = "images/ErrMsgBx.gif";
    java.net.URL imgURL = this.getClass().getResource(iconName);
    if(imgURL != null) {frame.setIconImage(new javax.swing.ImageIcon(imgURL).getImage());}
    else {System.out.println("--- Error in MsgBox constructor: Could not load image = \""+iconName+"\"");}
    frame.pack();
    //--- centre Window frame on Screen
    java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    int left; int top;
    left = Math.max(55, (screenSize.width  - frame.getWidth() ) / 2);
    top = Math.max(10, (screenSize.height - frame.getHeight()) / 2);
    frame.setLocation(Math.min(screenSize.width-100, left), Math.min(screenSize.height-100, top));
    //---
    final String msgText = wrapString(msg.trim(),80);
    //System.out.println("--- MsgBox:"+nl+msgText+nl+"---");
    frame.setVisible(true);
    //javax.swing.JOptionPane.showMessageDialog(frame, msg, title, javax.swing.JOptionPane.ERROR_MESSAGE);
    MsgBoxDialog msgBox = new MsgBoxDialog(frame, msgText, title, true);
    msgBox.setVisible(true); // becase the dialog is modal, statements below will wait
    msgBox.dispose();
    frame.setVisible(false);
    frame.dispose();
  }

/** Returns an input string, with lines that are longer than <code>maxLength</code>
 * word-wrapped and indented. * 
 * @param s input string
 * @param maxLength if an input line is longer than this length,
 * the line will be word-wrapped at the first white space after <code>maxLength</code>
 * and indented with 4 spaces
 * @return string with long-lines word-wrapped
 */
    public static String wrapString(String s, int maxLength) {
        String deliminator = "\n";
        StringBuilder result = new StringBuilder();
        StringBuffer wrapLine;
        int lastdelimPos;
        for (String line : s.split(deliminator, -1)) {
            if(line.length()/(maxLength+1) < 1) {
                result.append(line).append(deliminator);
            }
            else { //line too long, try to split it
                wrapLine = new StringBuffer();
                lastdelimPos = 0;
                for (String token : line.trim().split("\\s+", -1)) {
                    if (wrapLine.length() - lastdelimPos + token.length() > maxLength) {
                        if(wrapLine.length()>0) {wrapLine.append(deliminator);}
                        wrapLine.append("    ").append(token);
                        lastdelimPos = wrapLine.length() + 1;
                    } else {
                        if(wrapLine.length() <=0) {wrapLine.append(token);}
                        else {wrapLine.append(" ").append(token);}
                    }
                }
                result.append(wrapLine).append(deliminator);
            }
        }
        return result.toString();
    }

  //<editor-fold defaultstate="collapsed" desc="MsgBoxDialog">
  private static class MsgBoxDialog extends java.awt.Dialog {
    private java.awt.Button ok;
    private java.awt.Panel p;
    private final java.awt.TextArea text;

    /**  Creates new form NewDialog */
    public MsgBoxDialog(java.awt.Frame parent, String msg, String title, boolean modal) {
        super(parent, (" "+title), modal);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent evt) {
                MsgBoxDialog.this.setVisible(false);
            }
        });
        setLayout(new java.awt.BorderLayout());
        p = new java.awt.Panel();
        p.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        ok = new java.awt.Button();

        // find out the size of the message (width and height)
        final int wMax = 85; final int hMax=20;
        final int wMin = 5; final int hMin = 1;
        int w = wMin;
        int h=hMin; int i=0; int j=wMin;
        final String eol = "\n";  char c;
        final String nl = System.getProperty("line.separator");
        while (true) {
            c = msg.charAt(i);
            String s = String.valueOf(c);
            if(s.equals(eol) || s.equals(nl)) {
                h++; j=wMin;
            } else {
                j++; w = Math.max(j,w);
            }
            i++;
            if(i >= msg.length()-1) {break;}
        }

        // create a text area
        int scroll = java.awt.TextArea.SCROLLBARS_NONE;
        if(w > wMax && h <= hMax) {scroll = scroll & java.awt.TextArea.SCROLLBARS_HORIZONTAL_ONLY;}
        if(h > hMax && w <= wMax) {scroll = scroll & java.awt.TextArea.SCROLLBARS_VERTICAL_ONLY;}
        if(w > wMax && h > hMax) {scroll = java.awt.TextArea.SCROLLBARS_BOTH;}
        w = Math.min(Math.max(w,10),wMax);
        h = Math.min(h,hMax);
        text = new java.awt.TextArea(msg, h, w, scroll);
        text.setEditable(false);
        //text.setBackground(java.awt.Color.white);
        text.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent evt) {
                if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER
                    || evt.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {closeDialog();}
                if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_TAB) {ok.requestFocusInWindow();}
            }
        });
        text.setBackground(java.awt.Color.WHITE);
        text.setFont(new java.awt.Font("monospaced", java.awt.Font.PLAIN, 12));
        add(text, java.awt.BorderLayout.CENTER);

        ok.setLabel("OK");
        ok.addActionListener(new java.awt.event.ActionListener() {
            @Override public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeDialog();
            }
        });
        ok.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if(evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER
                        || evt.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {closeDialog();}
            }
        });
        p.add(ok);

        add(p, java.awt.BorderLayout.SOUTH);

        pack();
        ok.requestFocusInWindow();

        //--- centre Window frame on Screen
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int left; int top;
        left = Math.max(55, (screenSize.width  - getWidth() ) / 2);
        top = Math.max(10, (screenSize.height - getHeight()) / 2);
        setLocation(Math.min(screenSize.width-100, left), Math.min(screenSize.height-100, top));

    }

    private void closeDialog() {this.setVisible(false);}

  } // private static class MsgBoxDialog
  //</editor-fold>
}  // static class ErrMsgBx
  //</editor-fold>

}