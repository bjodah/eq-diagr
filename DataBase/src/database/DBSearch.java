package database;

import lib.common.MsgExceptn;
import lib.common.Util;
import lib.database.Complex;
import lib.database.LibDB;
import lib.database.ProgramDataDB;
import lib.huvud.ProgramConf;

/** Search reactions in the databases .
 * <br>
 * Copyright (C) 2016 I.Puigdomenech.
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
public class DBSearch {
  /** the search results: complexes and solids found in the database search */
  java.util.ArrayList<Complex> dat = new java.util.ArrayList<Complex>();
  /** temperature in degrees Celsius */
  double temperature = 25;
  /** pressure in bar */
  double pressure = 1;
  /** number of components (soluble and solid) */
  int na;
  /** number of soluble complexes */
  int nx;
  /** number of solid reaction products */
  int nf;
  /** number of solid components */
  int solidC;

  //<editor-fold defaultstate="collapsed" desc="private fields">
  private ProgramConf pc;
  private ProgramDataDB pd;
  private FrameDBmain dbF;
  /** counter: the database being read */
  private int db;
  /** name of the database being read */
  private String complxFileName;
  /** the size in bytes of the file being read */
  private long complxFileNameSize;
  /** a counter indicating how many reactions have been read so far */
  private long cmplxNbr = 0;
  private final double SIZE_FACTOR_TXT = 54.675;
  private final double SIZE_FACTOR_BIN = 124.929;
  /** the binary database being read */
  private java.io.DataInputStream dis;
  /** the text database being read */
  private java.io.BufferedReader br;
  /** is "e-" among the components selected by the user? */
  private boolean redox;
  /** the data bases are searched again when new redoc components are found */
  private int nLoops;
  /** if <code>binaryOrText</code> = 2 reading a binary database<br>
   * if <code>binaryOrText</code> = 1 reading text database<br>
   * if <code>binaryOrText</code> = 0 then all files are closed (because they have been read) */
  private int binaryOrText;
  /** Contains the selected components, both the original,
   * selected by the user, and new redox components
   * (if the user selects Fe+2 and e-, then selectedComps[] contains Fe+2 and Fe+3) */
  private java.util.ArrayList<String> selectedComps = new java.util.ArrayList<String>();
  /** In advanced mode the user may select to exclude some redox couples,
   * for example HS-/SO4-2, or NH3/NO3-. In such a case the complex SO4-2
   * has to be excluded if HS- and e- are selected.
   * The list with these components/complexes is kept in comps_X[] */
  private java.util.ArrayList<String> comps_X = new java.util.ArrayList<String>();
  /** List of all other possible components for elements of selected-components.
   * For example: if CN- is selected, and it is listed under the elements C and N,
   * then <code>comps[]</code> will contain CO3-2, EDTA-4, NO3-, NH3, etc. */
  private java.util.ArrayList<String> comps = new java.util.ArrayList<String>();
  /** The new redox components. For example, if the user selects H+, e- and Fe+2,
   * then <code>selectedComps[]</code> contains H+, e-, Fe+2 and Fe+3 and
   * <code>rRedox[]</code> will contain the new redox components (Fe+3).
   * @see SearchData#selectedComps selectedComps */
  private java.util.ArrayList<Complex> rRedox = new java.util.ArrayList<Complex>();
  /** true if the current database has been searched to the end and therefore the next
   * database must be opened (if there are any databases left to be searched) */
  private boolean openNextFile;
  /** true if no databases could be openend and searched */
  private boolean noFilesFound;
  
  /** New-line character(s) to substitute "\n" */
  private static final String nl = System.getProperty("line.separator");
  //</editor-fold>

 /** Constructor of a DBSearch instance
  * @param programConf configuration data about the "calling" program
  * @param programData data about the "calling" program, including the list of databases
  * @throws DBSearch.SearchException   */
  public DBSearch(ProgramConf programConf, ProgramDataDB programData)
          throws DBSearch.SearchException {
    binaryOrText = 0;
    noFilesFound = true;
    openNextFile = true;
    if(programConf == null) {throw new SearchException("Error: programConf = null in \"DBSearch\" constructor");}
    this.pc = programConf;
    if(programData == null) {throw new SearchException("Error: programDataDB = null in \"DBSearch\" constructor");}
    this.pd = programData;
    this.temperature = pd.temperature;
  }

  //<editor-fold defaultstate="collapsed" desc="searchComplexes">
 /** Searches the databases for reactions fitting the components selected by the user
  * and specified in the selectedComps[] list. If the electron "e-" is selected,
  * the databases may have to be scanned repeated times if new redox components
  * are found. For example, if {Fe+2, e-} are selected, after the first database
  * scan Fe+3 is found, and the databases must be scanned again for {Fe+2, Fe+3, e-}.
  * 
  * The reactions found in the search are stored in ArrayList "dat".
  * The progress bars in the lower half of the FrameDBmain show the search progress.
  * @param mainFrame
  * @throws DBSearch.SearchException */
  void searchComplexes (FrameDBmain mainFrame) throws DBSearch.SearchException {
    if(mainFrame == null) {MsgExceptn.exception("Error: mainFrame = null in \"searchComplexes\""); throw new SearchException();}
    this.dbF = mainFrame;
    if(dbF.modelSelectedComps.size() <=0) {
        MsgExceptn.exception("Error: modelSelectedComps.size() <=0 in \"searchComplexes\"");
        throw new SearchException();
    }
    if(pd.dataBasesList.size() <=0) {
        MsgExceptn.exception("Error: dataBasesList.size() <=0 in \"searchComplexes\"");
        throw new SearchException();
    }
    boolean found;

    if(pc.dbg) {System.out.println(FrameDBmain.LINE+nl+"--- Searching reactions");}


    nx =0;
    nf =0;
    binaryOrText = 0;
    nLoops = 1;

    // --- What components has the user selected?
    //  For redox systems (the user selected "e-" as a component)
    //  selectedComps[] contains the selected components, both the original,
    //  selected by the user, and new redox components. For example, if the user
    //  selects e- and Fe+2, then selectedComps[] contains Fe+2 and Fe+3)
    for(int i = 0; i < dbF.modelSelectedComps.size(); i++) {
        selectedComps.add(dbF.modelSelectedComps.get(i).toString());
    }

    redox = isComponentSelected("e-");
    na = selectedComps.size();
    solidC = 0;
    for(int i =0; i < na; i++) {
        if(Util.isSolid(selectedComps.get(i))) {solidC++;}
    }
    if(dbF.solidSelectedComps != solidC) {
        MsgExceptn.exception("Error in \"searchComplexes\":"+nl+
                "the number of solid components does not match!");
    }

// todo ?
/* advanced option: exclude some redox reactions?
If redox And RedoxAsk Then  */

    if(!redoxChecks()) {
        System.out.println("--- Search cancelled.");
        return;
    }

    // -------- For redox systems (the user selected "e-" as a component) ----------
    // Make a list of all other possible components for elements of selected-components
    // for example: if CN- is selected, and it is listed under the elements C and N,
    //              the list will contain CO3-2, EDTA-4, NO3-, NH3, etc
    // The list is stored in:   comps[]

    // selectedComps[nSelectedComps] contains the selected components, both the original,
    //          selected by the user, and new redox components
    //          (if the user selects Fe+2, then selectedComps[] contains Fe+2 and Fe+3)

    // rRedox[] will contain the new redox components (Fe+3)

    // The user may select to exclude some redox couples,
    //    for example HS-/SO4-2, or NH3/NO3-
    // In such a case the complex SO4-2 might have to be excluded
    //    if HS- and e- are selected
    // The list with these components/complexes is kept in comps_X[]
    // --------------- Redox loop: new components ----------------------------------
    if(redox) {
      boolean excluded;
      String[] elemComp; String selCompName; String el;
      for(int i =0; i< selectedComps.size(); i++) {
        selCompName = selectedComps.get(i).toString();
        for(int k0 =0; k0< pd.elemComp.size(); k0++) { //loop through all components in the database (CO3-2,SO4-2,HS-,etc)
          elemComp = pd.elemComp.get(k0);
          if(Util.nameCompare(elemComp[1],selCompName)) { //got the component selected by the user
            //Note: array elemComp[0] contains: the name-of-the-element (e.g. "C"),
            //  the formula-of-the-component ("CN-"), and
            //  the name-of-the-component ("cyanide")
              el = elemComp[0]; //get the element corresponding to the component: e.g. "S" for SO4-2
              for(int k1 =0; k1< pd.elemComp.size(); k1++) { //loop through all components in the database (CO3-2,SO4-2,HS-,etc)
                elemComp = pd.elemComp.get(k1);
                if(elemComp[0].equals(el)) { //got the right element
                  if(k1 != k0) {
                    excluded = false;
                    if((el.equals("N") && !pd.redoxN) || (el.equals("S") && !pd.redoxS)
                            || (el.equals("P") && !pd.redoxP)) {
                            excluded = true;
                    }
                    if(!excluded) {
                        // check if this component is already in the list,
                        // if it is not, it must be considered as a possible redox component
                        found = false;
                        if(comps.size()>0) {
                            for(int j=0; j < comps.size(); j++) {
                              if(Util.nameCompare(elemComp[1],comps.get(j))) {found = true; break;}
                            } //for j
                        }
                        if(!found) {comps.add(elemComp[1]);}
                    } else { //excluded:
                        //check if this component is already in the list,
                        //if it is not, it must be considered as a possible redox component
                        found = false;
                        if(comps_X.size() >0) {
                            for(int j=0; j < comps_X.size(); j++) {
                              if(Util.nameCompare(elemComp[1],comps_X.get(j))) {found = true; break;}
                            } //for j
                        }
                        if(!found) {comps_X.add(elemComp[1]);}
                    } //excluded?
                  } //if k1 != k0
                } //elemComp[0] = el
              } //for k1;  list of all available components
          } //if elemComp[1] = selCompName
        } //for k0;  list of all available components
      } //for i;   all selected components
      if(pc.dbg) {
        int n = comps.size();
        System.out.println("--- Possible new redox components:"+nl+"  comps[] size:"+n);
        for(int j=0; j<n; j++) {System.out.println("    "+comps.get(j));}
        n = comps_X.size();
        System.out.println("  comps_X[] size:"+n);
        for(int j=0; j<n; j++) {System.out.println("    "+comps_X.get(j));}
        if(comps.size() > 0 || n > 0) {System.out.println("---");}
      }
    } //if redox
    //-------- end of make lists for redox systems ----------

    // --------------------------------------------------
    // -------- Redox loop: new species -----------------
    //   loop searching database for redox systems
    //   (for non-redox systems the loop is run only once)
    // --------------------------------------------------
    while(true) {

        // ---------------------------------------------
        //  Search databases for all reactions
        // ---------------------------------------------
        try {scanDataBases();}
        catch (SearchInternalException ex) {
            if(!(ex instanceof SearchInternalException)) {
                String msg = "Error in \"searchComplexes\":"+nl+Util.stack2string(ex);
                throw new SearchException(msg);
            } else {throw new SearchException(ex.getMessage());}
        }
        // ---------------------------------------------

        if(!redox) {
            if(pc.dbg) {System.out.println("--- Search reactions ended.");}
            return;
        }

        //--------- For redox systems:
        //  If components appear as reaction products it will be needed to search again.
        //  For example, if the components selected by the user are Fe+2 and e-,
        //  the database search will find Fe+3 as a complex,
        //  which must be regarded as a new component
        //
        //  Update: rRedox[] - contains a list with the new components.
        //   selectedComps[] - contains a list with all components:
        //                     those selected originally by the user and
        //                     the new ones found in the database search.
        int n_selectedComps_0 = selectedComps.size();
        for(int i=0; i < dat.size(); i++) {
            if(Complex.isRedox(dat.get(i))) {
                found = false;
                String t1 = dat.get(i).name;
                for(int j=0; j < comps.size(); j++) {
                    if(Util.nameCompare(t1,comps.get(j))) {
                        //found a complex (e g Fe+3) which is a component
                        //   check that it is not already selected
                        found = true;
                        for(int k=0; k < selectedComps.size(); k++) {
                            if(Util.nameCompare(t1,selectedComps.get(k))) {found = false; break;}
                        } //for k
                        break;
                    }
                } //for j
                if(found) {
                    selectedComps.add(t1);
                    rRedox.add(dat.get(i));
                } //if found
            } //if ePresent
        } //for i

        // --------------------------------
        // If new redox components have been found,
        // one must search the database again.
        // For example, if the components selected
        // are Fe+2 and e-,  the database search will
        // find Fe+3 as a new component and another
        // database earch is needed including Fe+3
        // --------------------------------
        if(n_selectedComps_0 != selectedComps.size()) {
            nLoops++;
            continue; //while
        }
        break;
    } //while
    // --------------------------------
    //   end loops searching database
    //   for redox systems
    // --------------------------------

    // --------------------------------------------------
    // -------- Redox loop: correct reactions -----------
    // If rRedox() is not empty:
    // Perform corrections for the new redox components.
    //
    // Example: if H+, e- and Fe+2 are choosen,
    // a new redox component Fe+3 is found, and
    // to the reaction: Fe+3 - H+ = FeOH+2
    // one must add:    Fe+2 - e- = Fe+3    etc
    if(rRedox.size() > 0) {
        double n1, n2, np, rdx_dH, rdx_dCp;
        int rdxC, fnd;
                Complex cplx;
        boolean hPresent;
        String lComp, lComp2;
        boolean needsMore;
        for(int i=0; i < dat.size(); i++) { // loop through all Complexes
            // because cplx is a reference to an object of type Complex in the ArrayList,
            // any change to cplx changes the object in the ArrayList (dat)
            cplx = dat.get(i);
            while (true) {
                needsMore = false;
                for(int ic=0; ic < Complex.NDIM; ic++) {
                    lComp = cplx.component[ic];
                    if(lComp != null && lComp.length() >0) {
                      n1 = cplx.numcomp[ic];
                      np = cplx.proton;
                      rdxC = -1; //is this component a new redox component ?
                      for(int ir=0; ir < rRedox.size(); ir++) {
                          if(Util.nameCompare(rRedox.get(ir).name, lComp)) {rdxC = ir; break;}
                      }//for ir
                      if(rdxC > -1) { //it has a redox component: make corrections
                          needsMore = true;
                          //add the equilibrium constant of rRedox(rDxC)
                          cplx.constant = cplx.constant + n1 * rRedox.get(rdxC).constant;
                          rdx_dH = rRedox.get(rdxC).deltH;
                          rdx_dCp = rRedox.get(rdxC).deltCp;
                          if(!Double.isNaN(cplx.deltH) && cplx.deltH != Complex.EMPTY
                                  && !Double.isNaN(rdx_dH) && rdx_dH != Complex.EMPTY) {
                             cplx.deltH = cplx.deltH + n1 * rdx_dH;}
                          if(!Double.isNaN(cplx.deltCp) && cplx.deltCp != Complex.EMPTY
                                  && !Double.isNaN(rdx_dCp) && rdx_dCp != Complex.EMPTY) {
                             cplx.deltCp = cplx.deltCp + n1 * rdx_dCp;}
                          //add all stoichiometric coefficients of rRedox(rDxC)
                          cplx.component[ic] = "";
                          cplx.numcomp[ic] = 0;
                          hPresent = false;
                          for(int irr=0; irr < Complex.NDIM; irr++) {
                              lComp2 = rRedox.get(rdxC).component[irr];
                              if(lComp2 != null && lComp2.length() > 0) {
                                  n2 = rRedox.get(rdxC).numcomp[irr];                                  
                                  fnd = -1;
                                  for(int ic2=0; ic2<Complex.NDIM; ic2++) {
                                      if(Util.nameCompare(cplx.component[ic2], lComp2)) {
                                          fnd = ic2;
                                          cplx.numcomp[ic2] = cplx.numcomp[ic2] + n1 * n2;
                                          break;
                                      }
                                  }//for ic2
                                  if(fnd < 0) {
                                      for(int ic2=0; ic2<Complex.NDIM; ic2++) {
                                          if(cplx.component[ic2].length() <=0) {
                                              fnd = ic2; cplx.component[ic2] = lComp2;
                                              if(Util.isProton(lComp2)) {
                                                  cplx.numcomp[ic2] = np + n1 * n2;
                                              } else {
                                                  cplx.numcomp[ic2] = n1 * n2;
                                              }//if "H+"
                                              break;
                                          }//if component[ic2] = ""
                                      }//for ic2
                                  }//if fnd <0
                                  if(Util.isProton(lComp2)) {
                                      hPresent = true; cplx.proton = np + n1 * n2;
                                  } else {
                                      if(fnd < 0) {
                                          
                                          String msg = "Internal program error."+nl+nl+
                                                "Species \""+cplx.name+"\", component \""+lComp+"\""+nl+
                                                "correcting for \""+lComp2+"\""+nl+nl+
                                                "Please report this to the author of this software.";
                                          throw new SearchException(msg);
                                      }//if fnd<0
                                  }//if "H+"
                              }//if lComp2
                          }//for irr
                          n2 = rRedox.get(rdxC).proton;
                          if(!hPresent && Math.abs(n2) >0.001) { //make correction for "protons"
                              lComp2 = "H+";
                              fnd = -1;
                              for(int ic2=0; ic2 < Complex.NDIM; ic2++) {
                                  if(cplx.component[ic2].equals(lComp2)) {
                                      fnd = ic2;
                                      cplx.numcomp[ic2] = cplx.numcomp[ic2] + n1 * n2;
                                      break;
                                  }
                              }//for ic2
                              cplx.proton = cplx.proton + n1 * n2;
                              if(fnd < 0) {
                                  for(int ic2=0; ic2 < Complex.NDIM; ic2++) {
                                      if(cplx.component[ic2].length() <= 0) {
                                          cplx.component[ic2] = lComp2;
                                          cplx.numcomp[ic2] = cplx.proton;
                                          break;
                                      }
                                  }//for ic2
                              }//if fnd <0
                          }// if !hPresent && abs(rRedox.get(rdxC).proton) >0.001

                      }//if rDxC>-1
                    }//if lComp
                } //for ic
                if(!needsMore) {break;}
            } //while true
        }//for i (loop through all Complexes)
    } //iv sd.rRedox.size() > 0

    // ------- End of: Perform corretions for
    //         new redox components.
    // ------------------------------------------


    // ---------------------------------------------
    //   end of database search
    // ---------------------------------------------

    if(pc.dbg) {System.out.println("--- Search reactions ended.");}
  } //searchComplexes

  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="private methods">

  //<editor-fold defaultstate="collapsed" desc="scanDataBases">
 /** Reads all databases looking for all reaction products formed by the components
  * in the selectedComps[] list. The reactions found are stored in ArrayList "dat".
  * @throws DBSearch.SearchInternalException */
  private void scanDataBases() throws DBSearch.SearchInternalException {
    if(pc.dbg) {
        System.out.println("--- \"scanDataBases\", nLoops = "+nLoops+",  Selected components:");
        for (String selectedComp : selectedComps) {System.out.println("    " + selectedComp);}
        System.out.println("---");
    }
    int answer; String msg, t; boolean found; int i,j;
    boolean firstComplex = true;
        Complex rr;
    complxFileName = "";
    // the number of "components" will vary for redox systems,
    // for example, if Fe+2 is selected by the user, we have to search Fe+3 as well
    int nSelectedComps = selectedComps.size();

    // ---------------------------------------------
    //  Search all databases for reactions
    // ---------------------------------------------
    while(true) {
        rr = getOneComplex(firstComplex); //throws SearchInternalException()
        if(rr == null) {break;} //no more reactions
        if(rr.name.length() >0) {

            //---- make some primitive consistency checks, in case of a text file
            if(binaryOrText == 1 && nLoops ==1) {
                msg = Complex.checkComplex(rr);
                for(i=0; i < Complex.NDIM; i++) {
                    //check if the component is in the list of possible components
                    if(rr.component[i] != null && rr.component[i].length() >0) {
                        found = false;
                        String[] elemComp;
                        for(j=0; j < pd.elemComp.size(); j++) {
                            elemComp = pd.elemComp.get(j);
                            if(Util.nameCompare(rr.component[i],elemComp[1])) {found = true; break;}
                        } //for j
                        if(!found) {
                            t = "Component \""+rr.component[i]+"\" in complex \""+rr.name+"\""+nl+"not found in the element-files.";
                            if(msg.length() > 0) {msg = msg +nl+ t;} else {msg = t;}
                        }//not found
                    }
                }//for i
                if(msg != null) {
                    System.out.println("---- Error \""+msg+"\""+nl+"      for complex \""+rr.name+"\", logK="+rr.constant+", ref.=\""+rr.reference+"\"");
                    for(i=0; i< Complex.NDIM; i++) {System.out.print(" "+rr.component[i]+" "+rr.numcomp[i]+";");}
                    System.out.println();
                        Object[] opt = {"OK", "Cancel"};
                        answer = javax.swing.JOptionPane.showOptionDialog(dbF,
                                "Error in file \""+complxFileName+"\""+nl+"\""+msg+"\"",
                                pc.progName, javax.swing.JOptionPane.YES_NO_OPTION,
                                javax.swing.JOptionPane.WARNING_MESSAGE, null, opt, opt[1]);
                        if(answer != javax.swing.JOptionPane.YES_OPTION) {
                            try{
                                if(binaryOrText ==2 && dis != null) {dis.close();}
                                else if(binaryOrText ==1 && br != null) {br.close();}
                            } catch (java.io.IOException e) {}
                            throw new SearchInternalException();
                        }
                } //if msg !=null
            } //if text file and nLoops =1
            //---- end of consistency checks

            if(!rr.name.startsWith("@")) {
            // ----- Name does not begin with "@" (the normal case)
            //       Add the complex
                //check if we have already this complex. If found: replace it
                boolean include = true;
                if(redox) { //does this complex involve "e-" ?
                    boolean ePresent = false;
                    for(int ic=0; ic < Complex.NDIM; ic++) {
                        if(Util.isElectron(rr.component[ic])) {ePresent = true; break;}
                    }
                    if(ePresent) {
                      //does the user want to exclude this complex/component from redox equilibria?
                      for(j=0; j < comps_X.size(); j++) {
                          if(Util.nameCompare(rr.name,comps_X.get(j))) { //the complex (e g Fe+3) is a component to be excluded
                            include = false;  break;
                          }
                      } //for j
                      if(include) {
                        //It could be a new redox component (like Fe+2 - e- = Fe+3)
                        //if so: replace RRedox if already found, otherwise add it
                        found = false; //check if it is already selected
                        for(int k=0; k < selectedComps.size(); k++) {
                            if(Util.nameCompare(rr.name,selectedComps.get(k))) {found = true; break;}
                        } //for k
                        if(found) { //already selected: replace?
                            if(pc.dbg) {System.out.println("Complex already there (it was one of the components): "+rr.name);}
                            include = false; //the complex was already there
                            //-- Replace rRedox if same name + same stoichiometry
                            for(i=0; i < rRedox.size(); i++) {
                                if(Complex.sameNameAndStoichiometry(rr, rRedox.get(i))) {
                                    if(pc.dbg) {System.out.println("Complex already there and same stoichiometry: "+rr.name);}
                                    rRedox.set(i, rr); break;
                                }
                            }//for i
                        } //if found

                        ////The complex was not in the RRedox-list:
                        ////Should it be excluded because the user does not want
                        ////    redox equilibria for this element?
                        ////This is done with the function  "isRedox" (= true if the component
                        ////    only contains one element and H/O.
                        ////    For example: SO4-2 contains only "S" isRedox=true;
                        ////    but for Fe(SO4)2- isRedox=false)
                        //boolean exclude = false;
                        //if(isRedox("C", rr.name) && !pd.redoxC) {exclude = true;}
                        //if(isRedox("N", rr.name) && !pd.redoxN) {exclude = true;}
                        //if(isRedox("S", rr.name) && !pd.redoxS) {exclude = true;}
                        //if(isRedox("P", rr.name) && !pd.redoxP) {exclude = true;}
                        //if(exclude) {include = false;}

                      } //if include
                    } //if ePresent
                } //if redox

                //If  "include":  add the complex (replace otherwise)
                if(include) {
                    found = false;
                    for(i = 0; i < dat.size(); i++) {
                        if(Util.nameCompare(rr.name,dat.get(i).name)) { //replace!
                            dat.set(i, rr); found = true; break;
                        }
                    }//for i
                    if(!found) { //add!
                        //allSolids:  0=include all solids; 1=exclude (cr); 2=exclude (c); 3=exclude (cr)&(c)
                        boolean excludeSolid = true;
                        if(!Util.is_cr_or_c_solid(rr.name) ||
                           pd.allSolids == 0 ||
                           (Util.is_cr_solid(rr.name) && pd.allSolids !=1 && pd.allSolids !=3) ||
                           (Util.is_c_solid(rr.name) && pd.allSolids <2)) {excludeSolid = false;}
                        if(excludeSolid) {
                            final String s; if(pd.allSolids == 1) {s="(cr) solids excluded!";}
                            else if(pd.allSolids == 2) {s="(c) solids excluded!";}
                            else if(pd.allSolids == 3) {s="(cr) & (c) solids excluded!";} else {s = " ";}
                            javax.swing.SwingUtilities.invokeLater(new Runnable() {@Override public void run() {
                              dbF.jLabel_cr_solids.setText(s);
                            }});
                        } else { //include solid or not a solid
                            if(Util.isSolid(rr.name)) {
                                dat.add(rr);
                                nf++;
                            } //if solid
                            else { //soluble
                                dat.add(nx, rr);
                                nx++;
                            } //solid/soluble?
                        } //excludeSolid?
                    }//if !found
                }//if include
            // ----- end of: name does Not begin with a "@"
            } else {
            // ----- Complex name begins with "@"
            //       Withdraw the complex if it already has been read from the database
                rr.name = rr.name.substring(1);
                // first see if it is a "normal" complex
                if(nx+nf >0) {
                    i = 0;
                    do{ //while (i < (nx+nf))    --- loop through all reactions
                        if(Util.nameCompare(dat.get(i).name,rr.name)) {
                            dat.remove(i);
                            if(i < nx) {nx--;} else {nf--;} 
                        }
                        else {i++;}
                    } while (i < (nx+nf));
                } //if nx+nf >0
                // if redox: remove any components that are equivalent to the @-complex
                if(redox) {
                    int fnd = -1;
                    for(j = dbF.modelSelectedComps.size(); //search only new redox comps.
                            j < nSelectedComps; j++) {
                        if(Util.nameCompare(rr.name,selectedComps.get(j))) {fnd = j; break;}
                    }
                    if(fnd > -1) {
                        //First remove from components list
                        selectedComps.remove(fnd);
                        nSelectedComps--;
                        //2nd remove from "rRedox"
                        for(j =0; j<rRedox.size(); j++) {
                            if(Util.nameCompare(rr.name,rRedox.get(j).toString())) {rRedox.remove(j); break;}
                        }//for j
                        //remove from "dat" all reaction products formed by this redox component:
                        if(nx+nf >0) {
                            i = 0;
                            do{ //while (i < (nx+nf))    --- loop through all reactions
                                found = false;
                                for(int ic=0; ic < Complex.NDIM; ic++) { //formed by the component to delete?
                                  if(Util.nameCompare(dat.get(i).component[ic],rr.name)) {found = true; break;}
                                }
                                if(found) {
                                    dat.remove(i);
                                    if(i < nx) {nx--;} else {nf--;} 
                                } //if found
                                else {i++;}
                            } while (i < (nx+nf));
                        } //if nx+nf >0
                    } //if fnd >-1
                } //if redox
            } //----- end of: the name begins with a "@"                
        } //if rr.name.length() >0
        if(binaryOrText == 0) {break;}
        firstComplex = false;
    } //while
    // -----------------------------------------------
    //  end of database search
    // -----------------------------------------------
    try{
        if(binaryOrText ==2 && dis != null) {dis.close();}
        else if(binaryOrText ==1 && br != null) {br.close();}
    } catch (java.io.IOException e) {}
    dbF.updateProgressBarLabel(" ", 0);
    dbF.updateProgressBar(0);

  } //scanDataBases()
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="getOneComplex">
  /** This routine will get the next "complex" with the condition that
   * all its components must be in the "selected-components" list.
   * Both binary and text files will be read.
   * This routine is NOT called to convert database files (Text <=> Binary).
   * <p> On output:<br>
   * if <code>binaryOrText</code> = 2 reading binary database<br>
   * if <code>binaryOrText</code> = 1 reading text database<br>
   * if <code>binaryOrText</code> = 0 then all files are closed
   *   (because they have been read)
   * 
   * @param firstComplex if true the first file is opened and the first complex
   * is searched for; if false then find the next complex from the list of
   * database files
   * @return
   * @see lib.database.LibSearch#getComplex(boolean) getComplex
   * @throws DBSearch.SearchInternalException */
  private Complex getOneComplex(boolean firstComplex) throws DBSearch.SearchInternalException {
    boolean protonPresent;
    if(firstComplex) {
        //open the first file
        openNextFile = true;
        db = 0;
    }

    while (db < pd.dataBasesList.size()) {
      if(openNextFile) {
        try{
            if(dis != null) {dis.close();} else if(br != null) {br.close();}
        } catch (java.io.IOException ioe) {MsgExceptn.msg(ioe.getMessage());}
        complxFileName = pd.dataBasesList.get(db);
        if(complxFileName == null || complxFileName.length() <=0) {continue;}
        java.io.File dbf = new java.io.File(complxFileName);
        if(!dbf.exists() || !dbf.canRead()) {
            String msg = "Error: can not open file"+nl+
                         "    \""+complxFileName+"\".";
            if(!dbf.exists()) {msg = msg +nl+ "(the file does not exist)."+nl+
                                              "Search terminated";}
            throw new SearchInternalException(msg);
        }
        complxFileNameSize = dbf.length();
        cmplxNbr = 0;
        this.dbF.updateProgressBarLabel("Searching \""+complxFileName+"\"", nLoops);
        this.dbF.updateProgressBar(0);
        //--- text or binary?
        try{
            if(binaryOrText ==2 && dis != null) {dis.close();}
            else if(binaryOrText ==1 && br != null) {br.close();}
        } catch (java.io.IOException e) {}
        try{
            if(complxFileName.toLowerCase().endsWith("db")) { //--- binary file
                binaryOrText = 2;
                dis = new java.io.DataInputStream(new java.io.FileInputStream(dbf));
            } else { //--- text file
                binaryOrText = 1;
                br = new java.io.BufferedReader(new java.io.FileReader(dbf));
                // -- comments at the top of the file
                // topComments = rd.dataLineComment.toString();
            } //--- text or binary?
        }
        catch (java.io.FileNotFoundException ex) {
            try{
                if(dis != null) {dis.close();} else if(br != null) {br.close();}
            } catch (java.io.IOException ioe) {MsgExceptn.msg(ioe.getMessage());}
            String msg = "Error: "+ex.toString()+nl+
                    "while trying to open file: \""+complxFileName+"\"."+nl+"search terminated";
            throw new SearchInternalException(msg);
        }
        noFilesFound = false;
        openNextFile = false;
        if(pc.dbg) {System.out.println("Scanning database \""+complxFileName+"\"");}
      } //if sd.openNextFile

      Complex complex = null;
      loopComplex:
      while (true) {
        cmplxNbr++;
        if(binaryOrText ==2) { //Binary complex database
            try {
                complex = LibDB.getBinComplex(dis);
                dbF.updateProgressBar((int)(100*(double)cmplxNbr*SIZE_FACTOR_BIN
                        /(double)complxFileNameSize));
            }
            catch (LibDB.ReadBinCmplxException ex) {
                String msg = "Error: in \"getOneComplex\", cmplxNbr = "+cmplxNbr+nl+
                    "ReadBinCmplxException: "+ex.getMessage()+nl+
                    "in file: \""+complxFileName+"\"";
                throw new SearchInternalException(msg);
            }
        } //binaryOrText =2 (Binary file)
        else if(binaryOrText ==1) { // Text  complex database
            try {
                try{
                    complex = LibDB.getTxtComplex(br);
                    dbF.updateProgressBar((int)(100*(double)cmplxNbr*SIZE_FACTOR_TXT
                            /(double)complxFileNameSize));
                }
                catch (LibDB.EndOfFileException ex) {complex = null;}                    
            }
            catch (LibDB.ReadTxtCmplxException ex) {
                String msg = "Error: in \"getOneComplex\", cmplxNbr = "+cmplxNbr+nl+
                    ex.getMessage()+nl+
                    "in file: \""+complxFileName+"\"";
                throw new SearchInternalException(msg);
            }
        } //binaryOrText =1 (Text file)

        if(complex == null || complex.name == null) {break;} //  loopComplex // end-of-file, open next database
        if(complex.name.startsWith("@")) {return complex;}
        // --- is this a species formed from the selected components?
        protonPresent = false;
        for(int i=0; i < Complex.NDIM; i++) {
            if(complex.component[i].length() <=0 ||
               Util.isWater(complex.component[i])) {continue;} //H2O
            if(!isComponentSelected(complex.component[i]) &&
               Math.abs(complex.numcomp[i]) >0.001) {continue loopComplex;}
            if(Util.isProton(complex.component[i])) {protonPresent = true;}
        } //for i
        if(!protonPresent && Math.abs(complex.proton) > 0.001 &&
           !isComponentSelected("H+") &&
           !isComponentSelected("H +")) {continue;} // loopComplex
        // all components are selected: select complex
        return complex;
      } //while (true)  --- loopComplex:

      // ----- no complex found: end-of-file, or error. Get next file
      db++;
      openNextFile = true;
    } //while sd.db < pd.dataBasesList.size()

    if(noFilesFound) { // this should not happen...
        MsgExceptn.exception("Error: none of the databases could be found.");
        throw new SearchInternalException();
    }
    try{
        if(dis != null) {dis.close();} else if(br != null) {br.close();}
    } catch (java.io.IOException ioe) {MsgExceptn.msg(ioe.getMessage());}
    binaryOrText = 0;
    return null; //return null if no more reactions
  } //getOneComplex
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="isComponentSelected">
  /** find out if "component" is in the list of selected components
   * (in ArrayList selectedComps) */
  private boolean isComponentSelected(String component) {
      for(int i=0; i< selectedComps.size(); i++) {
          if(Util.nameCompare(component,selectedComps.get(i))) {return true;}
      }
      return false;
  } //isComponentSelected
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="isRedox(element, component)">
  /** For a given component, and one of its elements:
   * is this a component which might be involved in redox equilibria?
   * This is used to warn the user of a possible problem in the choice of components.
   * For example, if the user selects both V+2 and VO2+, it might be better to choose
   * instead V+2 and e-.<br>
   * Rule: if the component is formed by only the element, oxygen and hydrogen,
   * then return <code>true</code>. Examples: Fe+2, CrO4-2, H2PO4-
   * (return <code>true</code> for the elements Fe, Cr and P),
   * while for CN- return <code>false</code>. */
  static boolean isRedox(String element, String component) {
      StringBuilder comp = new StringBuilder(component);
      //---If it does not contain the element in the name, it is not a redox couple
      int i = component.indexOf(element);
      if(i<0) {return false;}
      //---Take away *, @, at the beginning of the name
      if(comp.length() >=2) {
          if(comp.charAt(0) == '*' || comp.charAt(0) == '@') {
              comp.delete(0, 1);
          }
      }
      //--- Take away diverse endings:
      // --- Take away (c), (s), (l), (g)
      if(comp.length() >=4) {
          String end = comp.substring(comp.length()-3, comp.length());
          if(end.equalsIgnoreCase("(c)") || end.equalsIgnoreCase("(s)") ||
             end.equalsIgnoreCase("(l)") || end.equalsIgnoreCase("(g)")) {
              comp.delete(comp.length()-3, comp.length());
          }
      }
      // --- Take away (cr), (am), (aq)
      if(comp.length() >=5) {
          String end = comp.substring(comp.length()-4, comp.length());
          if(end.equalsIgnoreCase("(cr)") || end.equalsIgnoreCase("(am)") ||
             end.equalsIgnoreCase("(aq)")) {comp.delete(comp.length()-3, comp.length());}
      }
      // --- Take away (vit)
      if(comp.length() >=6) {
          String end = comp.substring(comp.length()-5, comp.length());
          if(end.equalsIgnoreCase("(vit)")) {comp.delete(comp.length()-3, comp.length());}
      }
      //--- Take away numbers charge (+/-) and space
      i = 0;
      while(i<comp.length()) {
          if((comp.charAt(i)>='0' && comp.charAt(i)<='9') || comp.charAt(i) == '+' ||
             comp.charAt(i) == '-' ||
             comp.charAt(i) == '\u2013' || comp.charAt(i) == '\u2212' || // unicode en dash or minus
             comp.charAt(i) == '.' || comp.charAt(i) == ';' || comp.charAt(i) == ',' ||
             comp.charAt(i) == '(' || comp.charAt(i) == ')' ||
             comp.charAt(i) == '[' || comp.charAt(i) == ']' ||
             comp.charAt(i) == '{' || comp.charAt(i) == '}') {
                comp.delete(i, i+1);
          } else {i++;}
      } //while
      //--- Take away the element name
      while(true) {
          i = comp.indexOf(element);
          if(i<0) {break;}
          comp.delete(i, i+element.length());
      } //while
      //--- Take away oxygen
      while(true) {
          i = comp.indexOf("O");
          if(i<0) {break;}
          comp.delete(i, i+1);
      } //while
      //--- Take away hydrogen
      while(true) {
          i = comp.indexOf("H");
          if(i<0) {break;}
          comp.delete(i, i+1);
      } //while
      //--- Take away white space
      i = 0;
      while(i<comp.length()) {
          if(Character.isWhitespace(comp.charAt(i))) {comp.delete(i, i+1);}
          else {i++;}
      } //while
      return comp.length() <= 0;
  }
  //</editor-fold>

  //<editor-fold defaultstate="collapsed" desc="redoxChecks">
  /** Checks that the user does not select both Fe+2 and Fe+3 as components...
   * @return  true (go on with the search) if there was no problem (two similar components were not selected)
   * or if the user clicks to go ahead anyway. Returns false (cancel the search) if there was a
   * problem and the user selected to cancel the search
   */
  private boolean redoxChecks() {
    if(pc.dbg) {System.out.println("--- \"redoxChecks\".  redox = "+redox);}
    //----------
    // Check that the user does not select both Fe+2 and Fe+3 as components...
    //----------
    // This is done with the function  "is_redox" that =True if the component
    //    only contains one element and H/O. For example: SO4-2 contains only "S" is_redox%=True; but for CN- is_redox%=False
    String[] elemComp; String selCompName, el; boolean problem = false;
    for(int i =0; i< selectedComps.size(); i++) { //loop through all components selected by the user (e.g.: H+,CO3-,Fe+3)
      selCompName = selectedComps.get(i).toString();
      for(int k0 =0; k0< pd.elemComp.size(); k0++) { //loop through all components in the database (CO3-2,SO4-2,HS-,etc)
        elemComp = pd.elemComp.get(k0);
        if(Util.nameCompare(elemComp[1],selCompName)) { //this component has been selected by the user
            //Note: array ElemComp[0] contains: the name-of-the-element (e.g. "C"),
            //  the formula-of-the-component ("CN-"), and
            //  the name-of-the-component ("cyanide")
            el = elemComp[0]; //get the element corresponding to the component: e.g. "S" for SO4-2
            if(!isRedox(el, selCompName)) {break;} // k0
            if(!pd.redoxAsk || redox) {
                if(el.equals("N") && !pd.redoxN) {break;} // k0
                else if(el.equals("P") && !pd.redoxP) {break;} // k0
                else if(el.equals("S") && !pd.redoxS) {break;} // k0
            }
            //the component selected by the user "is redox": it only contains one element in addition to H/O
            //  such as H+, SO4-2 or Fe+3.
            //Look and see if there are other components for the same element
            for(int k1 =0; k1< pd.elemComp.size(); k1++) { //loop through all components in the database (CO3-2,SO4-2,HS-,etc)
              elemComp = pd.elemComp.get(k1);
              if(elemComp[0].equals(el)) { //got the right element
                  if(!Util.nameCompare(elemComp[1],selCompName)) { //look at all other components with the same element
                      if(isRedox(el, elemComp[1])) {
                        // this component also "is redox".
                        // For example, the user selected SO4-2 and this component is HS-
                        // check if this redox component has also been selected by the user
                        for(int k2 =i; k2< selectedComps.size(); k2++) { //search the rest of the selected components
                          if(Util.nameCompare(elemComp[1],selectedComps.get(k2).toString())) {
                            problem = true;
                            String msg = "Warning!"+nl+"You selected two components for the same element:"+
                                  "  \""+selCompName+"\" and \""+elemComp[1]+"\""+nl+
                                  "They are probably related by redox reactions."+nl;
                            if(redox) {msg = msg + "You should remove one of these components.";}
                            else {
                                msg = msg + "You should instead select \"e-\" as a component, and remove either "+
                                        "\""+selCompName+"\" or \""+elemComp[1]+"\".";
                            } //if redox
                            msg = msg + nl + "The calculations will then determine their respective concentrations."+nl+nl+
                                    "Are you sure that you want to continue ?";
                            System.out.println("- - - - - -"+nl+msg+nl+"- - - - - -");
                            Object[] opt = {"Continue", "Cancel"};
                            int answer = javax.swing.JOptionPane.showOptionDialog(
                                    dbF, msg, pc.progName, javax.swing.JOptionPane.YES_NO_OPTION,
                                    javax.swing.JOptionPane.WARNING_MESSAGE, null, opt, opt[1]);
                            if(answer != javax.swing.JOptionPane.YES_OPTION) {
                              System.out.println("Answer: Cancel");
                              return false;
                            }
                            System.out.println("Answer: Continue anyway");
                          } //if elemComp[1] = dbF.modelSelectedComps[k2]
                        } //for k2
                      }//isRedox(el, elemComp[1])
                  }//elemComp[1] != selCompName
              }//elemComp[0] = el
            } //for k1
        } //if elemComp[1] = selCompName
      } //for k0
    } //for i
    //---------- check ends
    if(pc.dbg && !problem) {System.out.println("Checks ok.");}
    return true;
  } //redoxChecks
  //</editor-fold>

  //</editor-fold>

class SearchException extends Exception {
    public SearchException() {super();}
    public SearchException(String txt) {super(txt);}
} // SearchException
private class SearchInternalException extends Exception {
    public SearchInternalException() {super();}
    public SearchInternalException(String txt) {super(txt);}
} //SearchInternalException

}
