// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon 19 May 2008 at  1:13:48 PST by lamport
//      modified on Fri Aug 24 14:43:24 PDT 2001 by yuanyu

package tlc2.tool;

import java.io.*;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.lang.reflect.*;
import tla2sany.drivers.SANY;
import tla2sany.modanalyzer.SpecObj;
import tla2sany.semantic.SemanticNode;
import tla2sany.semantic.ExprOrOpArgNode;
import tla2sany.semantic.OpArgNode;
import tla2sany.semantic.ExprNode;
import tla2sany.semantic.OpDeclNode;
import tla2sany.semantic.OpDefNode;
import tla2sany.semantic.OpApplNode;
import tla2sany.semantic.LetInNode;
import tla2sany.semantic.SubstInNode;
import tla2sany.semantic.Subst;
import tla2sany.semantic.AssumeNode;
import tla2sany.semantic.ModuleNode;
import tla2sany.semantic.LabelNode;
import tla2sany.semantic.NumeralNode;
import tla2sany.semantic.DecimalNode;
import tla2sany.semantic.StringNode;
import tla2sany.semantic.FormalParamNode;
import tla2sany.semantic.SymbolNode;
import tla2sany.semantic.LevelNode;
import tla2sany.semantic.ExternalModuleTable;
import util.*;
import tlc2.util.*;
import tlc2.value.*;
import tlc2.TLCGlobals;

public class Spec implements ValueConstants, ToolGlobals, Serializable {

  public String specDir;                   // The spec directory.
  public String rootFile;                  // The root file of this spec.
  protected String configFile;             // The model config file.
  protected ModelConfig config;            // The model configuration.
  protected ExternalModuleTable moduleTbl; // The external modules reachable from root
  protected ModuleNode rootModule;         // The root module.
  protected Defns defns;                   // Global definitions reachable from root
  public OpDeclNode[] variables;           // The state variables.
  protected TLAClass tlaClass;             // TLA built-in classes.
  protected Vect initPredVec;              // The initial state predicate.
  protected Action nextPred;               // The next state predicate.
  protected Action[] temporals;            // Fairness specifications...
  protected String[] temporalNames;        // ... and their names
  protected Action[] impliedTemporals;     // Liveness conds to check...
  protected String[] impliedTemporalNames; // ... and their names
  protected Action[] invariants;           // Invariants to be checked...
  protected String[] invNames;             // ... and their names
  protected Action[] impliedInits;         // Implied-inits to be checked...
  protected String[] impliedInitNames;     // ... and their names
  protected Action[] impliedActions;       // Implied-actions to be checked...
  protected String[] impliedActNames;      // ... and their names
  protected ExprNode[] modelConstraints;   // Model constraints
  protected ExprNode[] actionConstraints;  // Action constraints
  protected ExprNode[] assumptions;        // Assumptions

  public Spec(String specDir, String file) {
    this.specDir = specDir;
    this.rootFile = file;
    this.rootModule = null;
    this.config = null;
    this.variables = null;
    this.defns = new Defns();
    this.tlaClass = new TLAClass("tlc2.module");
    this.initPredVec = new Vect(5);
    this.nextPred = null;
    this.temporals = null; this.temporalNames = null;
    this.impliedTemporals = null; this.impliedTemporalNames = null;
    this.invariants = null; this.invNames = null;
    this.impliedInits = null; this.impliedInitNames = null;
    this.impliedActions = null; this.impliedActNames = null;
    this.modelConstraints = null;
    this.actionConstraints = null;    
    this.assumptions = null;
  }

  public Spec(String specDir, String specFile, String configFile) {
    this(specDir, specFile);
    this.configFile = configFile;
    this.config = new ModelConfig(configFile + ".cfg");
    this.config.parse();
    ModelValue.setValues();  // called after seeing all model values
  }

  /**
   * Processes the specification and collects information to be used
   * by tools. The processing tries to use any customized module (Java
   * class) to override the corresponding TLA+ module.
   */
  public final void processSpec() {
    // We first call the SANY frontend to parse and semantic-analyze
    // the complete TLA+ spec starting with the main module rootFile.
    SpecObj spec = new SpecObj(this.rootFile);
    try {
      SANY.frontEndInitialize(spec, System.err);
      SANY.frontEndParse(spec, System.err);
      SANY.frontEndSemanticAnalysis(spec, System.err, true);
    }
    catch (Throwable e) {
      // Assert.printStack(e);
      String msg = e.getMessage();
      if (msg == null) msg = "";
      Assert.fail("Parsing or semantic analysis failed. " + msg);
    }
    if (!spec.initErrors.isSuccess() ||
	!spec.parseErrors.isSuccess() ||
	!spec.semanticErrors.isSuccess()) {
      Assert.fail("Parsing or semantic analysis failed.");
    }

    // Set the rootModule:
    this.moduleTbl = spec.getExternalModuleTable();
    UniqueString rootName = UniqueString.intern(this.rootFile);
    this.rootModule = this.moduleTbl.getModuleNode(rootName);
    
    // Get all the state variables in the spec:
    OpDeclNode[] varDecls = this.rootModule.getVariableDecls();
    this.variables = new OpDeclNode[varDecls.length];
    for (int i = 0; i < varDecls.length; i++) {
      this.variables[i] = varDecls[i];
    }
    TLCState.setVariables(this.variables);
      
    // Add predefineds (Boolean and String) in defns.
    Defns.init();
    this.defns.put("TRUE", ValTrue);
    this.defns.put("FALSE", ValFalse);
    Value[] elems = new Value[2];
    elems[0] = ValFalse;
    elems[1] = ValTrue;
    this.defns.put("BOOLEAN", new SetEnumValue(elems, true));

    Class stringModule = this.tlaClass.loadClass("Strings");
    if (stringModule == null) {
      Assert.fail("This is a TLC bug: TLC could not find its built-in String module.\n");
    }
    Method[] ms = stringModule.getDeclaredMethods();
    for (int i = 0; i < ms.length; i++) {
      int mod = ms[i].getModifiers();
      if (Modifier.isStatic(mod)) {
	String name = TLARegistry.mapName(ms[i].getName());
	int acnt = ms[i].getParameterTypes().length;
	MethodValue mv = new MethodValue(ms[i]);
	Value val = (acnt == 0) ? mv.apply(EmptyArgs, EvalControl.Clear) : mv;
	this.defns.put(name, val);
      }
    }

    // Process all the constants in the spec.  Note that this must be done
    // here since we use defns.  Things added into defns later will make it
    // wrong to use it in the method processConstants.
    ModuleNode[] mods = this.moduleTbl.getModuleNodes();
    HashSet modSet = new HashSet();
    for (int i = 0; i < mods.length; i++) {
      this.processConstants(mods[i]);
      modSet.add(mods[i].getName().toString());
    }
    
    // Collect all the assumptions.
    AssumeNode[] assumes = this.rootModule.getAssumptions();
    this.assumptions = new ExprNode[assumes.length];
    for (int i = 0; i < assumes.length; i++) {
      this.assumptions[i] = assumes[i].getAssume();
    }

    // Get the constants and overrides in config file.
    // Note: Both hash tables use String as key.
    Hashtable constants = this.initializeConstants();
    Hashtable overrides = this.config.getOverrides();

    // Apply config file constants to the constant decls visible to rootModule.
    OpDeclNode[] rootConsts = this.rootModule.getConstantDecls();
    for (int i = 0; i < rootConsts.length; i++) {
      UniqueString name = rootConsts[i].getName();
      Object val = constants.get(name.toString());
      if (val == null && !overrides.containsKey(name.toString())) {
	Assert.fail("The constant parameter " + name + " is not assigned" +
		    " a value by the configuration file.");
      }
      rootConsts[i].setToolObject(TLCGlobals.ToolId, val);
      this.defns.put(name, val);
    }

    // Apply config file constants to the operator defns visible to rootModule.
    OpDefNode[] rootOpDefs = this.rootModule.getOpDefs();
    for (int i = 0; i < rootOpDefs.length; i++) {
      UniqueString name = rootOpDefs[i].getName();
      Object val = constants.get(name.toString());
      if (val == null) {
	this.defns.put(name, rootOpDefs[i]);
      }
      else {
	rootOpDefs[i].setToolObject(TLCGlobals.ToolId, val);
	this.defns.put(name, val);
      }
    }

    // Apply config file module specific constants to operator defns.
    // We do not allow this kind of replacement for constant decls.
    Hashtable modConstants = this.initializeModConstants();
    for (int i = 0; i < mods.length; i++) {
      UniqueString modName = mods[i].getName();
      Hashtable mConsts = (Hashtable)modConstants.get(modName.toString());
      if (mConsts != null) {
	OpDefNode[] opDefs = mods[i].getOpDefs();
	for (int j = 0; j < opDefs.length; j++) {
	  UniqueString name = opDefs[j].getName();
	  Object val = mConsts.get(name.toString());
	  if (val != null) {
	    opDefs[j].getBody().setToolObject(TLCGlobals.ToolId, val);
	  }
	}
      }
    }
    
    // Apply module overrides:
    for (int i = 0; i < mods.length; i++) {
      UniqueString modName = mods[i].getName();
      Class userModule = this.tlaClass.loadClass(modName.toString());
      if (userModule != null) {
	// Override with a user defined Java class for the TLA+ module.
	// Collects new definitions:
	Hashtable javaDefs = new Hashtable();
	Method[] mds = userModule.getDeclaredMethods();
	for (int j = 0; j < mds.length; j++) {
	  int mdf = mds[j].getModifiers();
	  if (Modifier.isPublic(mdf) && Modifier.isStatic(mdf)) {
	    String name = TLARegistry.mapName(mds[j].getName());
	    UniqueString uname = UniqueString.intern(name);	    
	    int acnt = mds[j].getParameterTypes().length;
	    MethodValue mv = new MethodValue(mds[j]);
	    boolean isConstant = (acnt == 0) && Modifier.isFinal(mdf);
	    Value val = isConstant ? mv.apply(EmptyArgs, EvalControl.Clear) : mv;
	    javaDefs.put(uname, val);
	  }
	}
	// Adds/overrides new definitions:
	OpDefNode[] opDefs = mods[i].getOpDefs();
	for (int j = 0; j < opDefs.length; j++) {
	  UniqueString uname = opDefs[j].getName();
	  Object val = javaDefs.get(uname);
	  if (val != null) {
	    opDefs[j].getBody().setToolObject(TLCGlobals.ToolId, val);
	    this.defns.put(uname, val);
	  }
	}
      }
    }

    HashSet overriden = new HashSet();
    // Apply config file overrides to constants:
    for (int i = 0; i < rootConsts.length; i++) {
      UniqueString lhs = rootConsts[i].getName();
      String rhs = (String)overrides.get(lhs.toString());
      if (rhs != null) {
	if (overrides.containsKey(rhs)) {
	  Assert.fail("In the configuration file, the identifier " + rhs + " appears\n" +
		      "on the right-hand side of a <- after already appearing on the\n" +
		      "left-hand side of one.\n");
	}
	Object myVal = this.defns.get(rhs);
	if (myVal == null) {
	  Assert.fail("The configuration file substitutes for " + lhs +
		      " with the undefined identifier " + rhs + ".\n");
	}
	rootConsts[i].setToolObject(TLCGlobals.ToolId, myVal);
	this.defns.put(lhs, myVal);
	overriden.add(lhs.toString());
      }
    }

    // Apply config file overrides to operator definitions:
    for (int i = 0; i < rootOpDefs.length; i++) {
      UniqueString lhs = rootOpDefs[i].getName();
      String rhs = (String)overrides.get(lhs.toString());
      if (rhs != null) {
	if (overrides.containsKey(rhs)) {
	  Assert.fail("In the configuration file, the identifier " + rhs + " appears\n" +
		      "on the right-hand side of a <- after already appearing on the\n" +
		      "left-hand side of one.\n");
	}
	Object myVal = this.defns.get(rhs);
	if (myVal == null) {
	  Assert.fail("The configuration file substitutes for " + lhs +
		      " with the undefined identifier " + rhs + ".\n");
	}
	if ((myVal instanceof OpDefNode) &&
	    rootOpDefs[i].getNumberOfArgs() != ((OpDefNode)myVal).getNumberOfArgs()) {
	  Assert.fail("The configuration file substitutes for " + lhs +
		      " with " + rhs + " of different number of arguments.\n");
	}
	rootOpDefs[i].setToolObject(TLCGlobals.ToolId, myVal);
	this.defns.put(lhs, myVal);
	overriden.add(lhs.toString());
      }
    }

    Enumeration keys = overrides.keys();
    while (keys.hasMoreElements()) {
      Object key = keys.nextElement();
      if (!overriden.contains(key)) {
	Assert.fail("In the configuration file, the identifier " + key +
		    " does not appear in the specification.\n");
      }
    }

    // Apply config file module specific overrides to operator defns.
    // We do not allow this kind of replacement for constant decls.
    Hashtable modOverrides = this.config.getModOverrides();
    for (int i = 0; i < mods.length; i++) {
      UniqueString modName = mods[i].getName();
      Hashtable mDefs = (Hashtable)modOverrides.get(modName.toString());
      HashSet modOverriden = new HashSet();
      if (mDefs != null) {
	// the operator definitions:
	OpDefNode[] opDefs = mods[i].getOpDefs();
	for (int j = 0; j < opDefs.length; j++) {
	  UniqueString lhs = opDefs[j].getName();
	  String rhs = (String)mDefs.get(lhs.toString());
	  if (rhs != null) {
	    if (mDefs.containsKey(rhs)) {
	      Assert.fail("In the configuration file, the identifier " + rhs + " appears\n" +
			  "on the right-hand side of a <- after already appearing on the\n" +
			  "left-hand side of one.\n");
	    }
	    // The rhs comes from the root module.
	    Object myVal = this.defns.get(rhs);
	    if (myVal == null) {
	      Assert.fail("The configuration file substitutes for " + lhs +
			  " with the undefined identifier " + rhs + ".\n");
	    }
	    if ((myVal instanceof OpDefNode) &&
		opDefs[j].getNumberOfArgs() != ((OpDefNode)myVal).getNumberOfArgs()) {
	      Assert.fail("The configuration file substitutes for " + lhs +
			  " with " + rhs + " of different number of arguments.\n");
	    }
	    opDefs[j].getBody().setToolObject(TLCGlobals.ToolId, myVal);
	    modOverriden.add(lhs.toString());
	  }
	}

	Enumeration mkeys = mDefs.keys();
	while (mkeys.hasMoreElements()) {
	  Object mkey = mkeys.nextElement();
	  if (!modOverriden.contains(mkey)) {
	    Assert.fail("In the configuration file, the identifier " + mkey +
			" does not appear in the specification.\n");
	  }
	}
      }
    }

    // Check if the module names specified in the config file are defined.
    Enumeration modKeys = modOverrides.keys();
    while (modKeys.hasMoreElements()) {
      Object modName = modKeys.nextElement();
      if (!modSet.contains(modName)) {
	Assert.fail("In the configuration file, the module name " + modName +
		    " is not a module in the specification.\n");
      }
    }
  }

  /*************************************************************************
  * The following method goes through all the nodes to set their           *
  * tool-specific fields.  It was modified on 1 May 2007 so it would find  *
  * the nodes in the body of a Lambda expression.  Obviously, if new       *
  * semantic node types are added, this method will have to be modified.   *
  * Less obviously, if a tool wants to call TLC on a specification that    *
  * was not all created inside a module, then this method may need to be   *
  * modified so TLC finds thos nodes not part of the module.               *
  *                                                                        *
  * Yuan claims that this is the only method in TLC that has to find all   *
  * the nodes in such a way.                                               *
  *************************************************************************/
  private final void processConstants(SemanticNode expr) {
    switch (expr.getKind()) {
    case ModuleKind:
      {
	ModuleNode expr1 = (ModuleNode)expr;
	// Process operator definitions:
	OpDefNode[] opDefs = expr1.getOpDefs();
	for (int i = 0; i < opDefs.length; i++) {
	  Object def = opDefs[i].getToolObject(TLCGlobals.ToolId);
	  if (def instanceof OpDefNode) {
	    this.processConstants(((OpDefNode)def).getBody());
	  }
	  this.processConstants(opDefs[i].getBody());
	}
	// Process all the inner modules:
	ModuleNode[] imods = expr1.getInnerModules();
	for (int i = 0; i < imods.length; i++) {
	  this.processConstants(imods[i]);
	}
	// Process all the assumptions:
	AssumeNode[] assumps = expr1.getAssumptions();
	for (int i = 0; i < assumps.length; i++) {
	  this.processConstants(assumps[i]);
	}
	return;
      }
    case OpApplKind:
      {
	OpApplNode expr1 = (OpApplNode)expr;
	SymbolNode opNode = expr1.getOperator();
	Object val = this.defns.get(opNode.getName());
	if (val != null) {
	  opNode.setToolObject(TLCGlobals.ToolId, val);
	}
	else {
	  SemanticNode[] args = expr1.getArgs();
	  for (int i = 0; i < args.length; i++) {
	    if (args[i] != null) {
	      this.processConstants(args[i]);
	    }
	  }
	  ExprNode[] bnds = expr1.getBdedQuantBounds();
	  for (int i = 0; i < bnds.length; i++) {
	    this.processConstants(bnds[i]);
	  }
	}
	return;
      }
    case LetInKind:
      {
	LetInNode expr1 = (LetInNode)expr;
	OpDefNode[] letDefs = expr1.getLets();
	for (int i = 0; i < letDefs.length; i++) {
	  this.processConstants(letDefs[i].getBody());
	}
	this.processConstants(expr1.getBody());
	return;
      }
    case SubstInKind:
      {
	SubstInNode expr1 = (SubstInNode)expr;
	Subst[] subs = expr1.getSubsts();
	for (int i = 0; i < subs.length; i++) {
	  this.processConstants(subs[i].getExpr());
	}
	this.processConstants(expr1.getBody());
	return;
      }
    case NumeralKind:
      {
	NumeralNode expr1 = (NumeralNode)expr;
	IntValue val = IntValue.gen(expr1.val());
	Value.setValue(expr1, val);
	return;
      }
    case DecimalKind:
      {
	DecimalNode expr1 = (DecimalNode)expr;
	Assert.fail("TLC BUG: could not handle real numbers.\n" + expr);
	return;
      }
    case StringKind:
      {
	StringNode expr1 = (StringNode)expr;
	StringValue val = new StringValue(expr1.getRep());
	Value.setValue(expr1, val);
	return;
      }
    case AssumeKind:
      {
	AssumeNode expr1 = (AssumeNode)expr;
	this.processConstants(expr1.getAssume());
	return;
      }
    case OpArgKind:
      { SymbolNode opArgNode = ((OpArgNode) expr).getOp() ;
        if (opArgNode.getKind() == UserDefinedOpKind) {
             this.processConstants(((OpDefNode) opArgNode).getBody());
          } ;
        return;
      }
    /***********************************************************************
    * LabelKind case added by LL on 13 Jun 2007.                           *
    ***********************************************************************/
    case LabelKind:
      { LabelNode expr1 = (LabelNode) expr;
        this.processConstants(expr1.getBody()) ;
      }
    }    
  }

  /* Return the variable if expr is a state variable. Otherwise, null. */
  public final SymbolNode getVar(SemanticNode expr, Context c, boolean cutoff) {
    if (expr instanceof OpApplNode) {
      SymbolNode opNode = ((OpApplNode)expr).getOperator();

      if (opNode.getArity() == 0) {
	boolean isVarDecl = (opNode.getKind() == VariableDeclKind);
	Object val = this.lookup(opNode, c, cutoff && isVarDecl);

	if (val instanceof LazyValue) {
	  LazyValue lval = (LazyValue)val;
	  return this.getVar(lval.expr, lval.con, cutoff);
	}
	if (val instanceof OpDefNode) {
	  return this.getVar(((OpDefNode)val).getBody(), c, cutoff);
	}
	if (isVarDecl) { return opNode;	}
      }
    }
    return null;
  }

  /* Return the variable if expr is a primed state variable. Otherwise, null. */
  public final SymbolNode getPrimedVar(SemanticNode expr, Context c, boolean cutoff) {
    if (expr instanceof OpApplNode) {
      OpApplNode expr1 = (OpApplNode)expr;
      SymbolNode opNode = expr1.getOperator();

      if (BuiltInOPs.getOpCode(opNode.getName()) == OPCODE_prime) {
	return this.getVar(expr1.getArgs()[0], c, cutoff);
      }
      
      if (opNode.getArity() == 0) {
	boolean isVarDecl = (opNode.getKind() == VariableDeclKind);	
	Object val = this.lookup(opNode, c, cutoff && isVarDecl);

	if (val instanceof LazyValue) {
	  LazyValue lval = (LazyValue)val;
	  return this.getPrimedVar(lval.expr, lval.con, cutoff);
	}
	if (val instanceof OpDefNode) {
	  return this.getPrimedVar(((OpDefNode)val).getBody(), c, cutoff);
	}
      }
    }
    return null;
  }

  private Vect invVec = new Vect();
  private Vect invNameVec = new Vect();
  private Vect impliedInitVec = new Vect();
  private Vect impliedInitNameVec = new Vect();
  private Vect impliedActionVec = new Vect();
  private Vect impliedActNameVec = new Vect();
  private Vect temporalVec = new Vect();
  private Vect temporalNameVec = new Vect();
  private Vect impliedTemporalVec = new Vect();
  private Vect impliedTemporalNameVec = new Vect();    

  /* Process the configuration file. */
  public final void processConfig() {
    // Process the invariants:
    this.processConfigInvariants();

    // Process specification:
    String specName = this.config.getSpec();
    if (specName.length() == 0) {
      this.processConfigInitAndNext();
    }
    else {
      if (this.config.getInit().length() != 0 ||
	  this.config.getNext().length() != 0) {
	Assert.fail("The configuration file cannot specify both INIT/NEXT and" +
		    " SPECIFICATION fields.");
      }
      Object spec = this.defns.get(specName);
      if (spec instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)spec;
	if (opDef.getArity() != 0) {
	  Assert.fail("TLC requires " + specName + " not to take any argument.");
	}
	this.processConfigSpec(opDef.getBody(), Context.Empty, List.Empty);
      }
      else if (spec == null) {
	Assert.fail("The name " + specName + " in the config file is not defined.");
      }
      else {
	Assert.fail("The value of " + specName + " i sequal to " + spec);
      }
    }
    
    // Process the properties:
    Vect propNames = this.config.getProperties();
    for (int i = 0; i < propNames.size(); i++) {
      String propName = (String)propNames.elementAt(i);
      Object prop = this.defns.get(propName);
      if (prop instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)prop;
	if (opDef.getArity() != 0) {
	  Assert.fail("TLC requires " + propName + " not to take any argument.");
	}
	this.processConfigProps(propName, opDef.getBody(), Context.Empty, List.Empty);
      }
      else if (prop == null) {
	Assert.fail("The property " + propName + " specified by the configuration" +
		    "\nfile is not defined in the specification.");
      }
      else if (!(prop instanceof BoolValue) ||
	       !(((BoolValue)prop).val)) {
	Assert.fail("The property " + propName + " is equal to " + prop);
      }
    }

    // Postprocess:
    this.invariants = new Action[this.invVec.size()];
    this.invNames = new String[this.invVec.size()];
    for (int i = 0; i < this.invariants.length; i++) {
      this.invariants[i] = (Action)this.invVec.elementAt(i);
      this.invNames[i] = (String)this.invNameVec.elementAt(i);
    }
    this.invVec = null;
    this.invNameVec = null;

    this.impliedInits = new Action[this.impliedInitVec.size()];
    this.impliedInitNames = new String[this.impliedInitVec.size()];
    for (int i = 0; i < this.impliedInits.length; i++) {
      this.impliedInits[i] = (Action)this.impliedInitVec.elementAt(i);
      this.impliedInitNames[i] = (String)this.impliedInitNameVec.elementAt(i);
    }
    this.impliedInitVec = null;
    this.impliedInitNameVec = null;

    this.impliedActions = new Action[this.impliedActionVec.size()];
    this.impliedActNames = new String[this.impliedActionVec.size()];
    for (int i = 0; i < this.impliedActions.length; i++) {
      this.impliedActions[i] = (Action)this.impliedActionVec.elementAt(i);
      this.impliedActNames[i] = (String)this.impliedActNameVec.elementAt(i);
    }
    this.impliedActionVec = null;
    this.impliedActNameVec = null;

    this.temporals = new Action[this.temporalVec.size()];
    this.temporalNames = new String[this.temporalNameVec.size()];
    for (int i = 0; i < this.temporals.length; i++) {
      this.temporals[i] = (Action)this.temporalVec.elementAt(i);
      this.temporalNames[i] = (String)this.temporalNameVec.elementAt(i);
    }
    this.temporalVec = null;
    this.temporalNameVec = null;

    this.impliedTemporals = new Action[this.impliedTemporalVec.size()];
    this.impliedTemporalNames = new String[this.impliedTemporalNameVec.size()];
    for (int i = 0; i < this.impliedTemporals.length; i++) {
      this.impliedTemporals[i] = (Action)this.impliedTemporalVec.elementAt(i);
      this.impliedTemporalNames[i] = (String)this.impliedTemporalNameVec.elementAt(i);
    }
    this.impliedTemporalVec = null;
    this.impliedTemporalNameVec = null;

    if (this.initPredVec.size() == 0 &&
	(this.impliedInits.length != 0 ||
	 this.impliedActions.length != 0 ||
	 this.variables.length != 0 ||
	 this.invariants.length != 0 ||
	 this.impliedTemporals.length != 0)) {
      Assert.fail("The configuration file did not specify the initial state predicate.");
    }
    if (this.nextPred == null &&
	(this.impliedActions.length != 0 ||
	 this.invariants.length != 0 ||
	 this.impliedTemporals.length != 0)) {
      Assert.fail("The configuration file did not specify the next state predicate.");
    }
  }

  /* Process the INIT and NEXT fields of the config file. */
  private final void processConfigInitAndNext() {
    String name = this.config.getInit();
    if (name.length() != 0) {
      Object init = this.defns.get(name);
      if (init == null) {
	Assert.fail("The initial predicate " + name + " specified by the" +
		    " configuration file\nis not defined in the specification.");
      }
      if (!(init instanceof OpDefNode)) {
	Assert.fail("The initial predicate " + name + " cannot be a constant.");
      }
      OpDefNode def = (OpDefNode)init;
      if (def.getArity() != 0) {
	Assert.fail("TLC requires the state predicate " + name + " not to take any argument.");
      }
      this.initPredVec.addElement(new Action(def.getBody(), Context.Empty));
    }
    
    name = this.config.getNext();
    if (name.length() != 0) {
      Object next = this.defns.get(name);
      if (next == null) {
	Assert.fail("The next state relation " + name + " specified by the" +
		    " configuration file\nis not defined in the specification.");
      }
      if (!(next instanceof OpDefNode)) {
	Assert.fail("The next state action " + name + " cannot be a constant.");
      }
      OpDefNode def = (OpDefNode)next;
      if (def.getArity() != 0) {
	Assert.fail("The next state action " + name + " cannot take any argument.");
      }
      this.nextPred = new Action(def.getBody(), Context.Empty);
    }
  }

  /* Process the INVARIANTS field of the config file. */
  private final void processConfigInvariants() {
    Vect invs = this.config.getInvariants();
    for (int i = 0; i < invs.size(); i++) {
      String name = (String)invs.elementAt(i);
      Object inv = this.defns.get(name);
      if (inv instanceof OpDefNode) {
	OpDefNode def = (OpDefNode)inv;
	if (def.getArity() != 0) {
	  Assert.fail("The invariant " + name + " cannot take any argument.");
	}
	this.invNameVec.addElement(name);
	this.invVec.addElement(new Action(def.getBody(), Context.Empty));
      }
      else if (inv == null) {
	Assert.fail("The invariant " + name + " specified by the configuration" +
		    "\nfile is not defined in the specification.");
      }
      else if (!(inv instanceof BoolValue) ||
	       !(((BoolValue)inv).val)) {
	Assert.fail("The invariant " + name + " is equal to " + inv);
      }
    }
  }

  private final ExprNode addSubsts(ExprNode expr, List subs) {
    ExprNode res = expr;

    while (!subs.isEmpty()) {
      SubstInNode sn = (SubstInNode)subs.car();
      res = new SubstInNode(sn.stn, sn.getSubsts(), res,
			    sn.getInstantiatingModule(),
			    sn.getInstantiatedModule());
      subs = subs.cdr();
    }
    return res;
  }

  /* Process the SPECIFICATION field of the config file.  */
  private final void processConfigSpec(ExprNode pred, Context c, List subs) {
    if (pred instanceof SubstInNode) {
      SubstInNode pred1 = (SubstInNode)pred;
      this.processConfigSpec(pred1.getBody(), c, subs.cons(pred1));
      return;
    }
    if (pred instanceof OpApplNode) {
      OpApplNode pred1 = (OpApplNode)pred;
      ExprOrOpArgNode[] args = pred1.getArgs();

      if (args.length == 0) {
	SymbolNode opNode = pred1.getOperator();
	Object val = this.lookup(opNode, c, false);
	if (val instanceof OpDefNode) {
	  if (((OpDefNode)val).getArity() != 0) {
	    Assert.fail("The operator " + opNode.getName() + " cannot take any argument.");
	  }
	  ExprNode body = ((OpDefNode)val).getBody();
	  if (this.getLevelBound(body, c) == 1) {
	    this.initPredVec.addElement(new Action(this.addSubsts(body, subs), c));
	  }
	  else {
	    this.processConfigSpec(body, c, subs);
	  }
	}
	else if (val == null) {
	  Assert.fail("The operator " + opNode.getName() + " is not defined in the spec.");
	}
	else if (val instanceof BoolValue) {
	  if (!((BoolValue)val).val) {
	    Assert.fail("The spec is trivially false because " + opNode.getName() +
			" is false.");
	  }
	}
	else {
	  Assert.fail("The operator " + opNode.getName() + "is equal to " + val);
	}
	return;
      }

      int opcode = BuiltInOPs.getOpCode(pred1.getOperator().getName());
      if (opcode == OPCODE_cl || opcode == OPCODE_land) {
	for (int i = 0; i < args.length; i++) {
	  this.processConfigSpec((ExprNode)args[i], c, subs);
	}
	return;
      }
      if (opcode == OPCODE_box) {
	SemanticNode boxArg = args[0];
	if ((boxArg instanceof OpApplNode) &&
	    BuiltInOPs.getOpCode(((OpApplNode)boxArg).getOperator().getName()) == OPCODE_sa) {
	  ExprNode arg = (ExprNode)((OpApplNode)boxArg).getArgs()[0];
	  // ---sm 09/06/04 <<<
	  ExprNode subscript = (ExprNode)((OpApplNode)boxArg).getArgs()[1];
	  Vect varsInSubscript = null;
	  // collect the variables from the subscript...
	  try {
	    class SubscriptCollector {
	      /**
	       * This class attempts to recover all variables that
	       * appear in (possibly nested) tuples in the subscript
	       * of the next-state relation. Variables that appear
	       * in other kinds of expressions are ignored.
	       * The idea is to make sure that the subscript is a
	       * tuple that contains at least the declared variables
	       * of this specification object -- otherwise TLC's
	       * analysis is incorrect.
	       **/
	      Vect components;
	      SubscriptCollector() {
		this.components = new Vect();
	      }
	      void enter(ExprNode subscript, Context c) {
		// if it's a variable, add it to the vector and return
		SymbolNode var = getVar(subscript, c, false);
		if (var != null) {
		  components.addElement(var);
		  return;
		}
		// otherwise further analyze the expression
		switch (subscript.getKind()) {
		case OpApplKind : {
		  OpApplNode subscript1 = (OpApplNode)subscript;
		  SymbolNode opNode = subscript1.getOperator();
		  ExprOrOpArgNode[] args = subscript1.getArgs();
		  int opCode = BuiltInOPs.getOpCode(opNode.getName());
		  // if it's a tuple, recurse with its members
		  if (opCode == OPCODE_tup) {
		    for (int i=0; i < args.length; i++) {
		      this.enter((ExprNode)args[i], c);
		    }
		    return;
		  }
		  // all other built-in operators are ignored
		  else if (opCode != 0) {
		    return;
		  }
		  // user-defined operator: look up its definition
		  Object opDef = lookup(opNode, c, false);
		  if (opDef instanceof OpDefNode) {
		    OpDefNode opDef1 = (OpDefNode)opDef;
		    this.enter(opDef1.getBody(),
			       getOpContext(opDef1, args, c, false));
		    return;
		  }
		  if (opDef instanceof LazyValue) {
		    LazyValue lv = (LazyValue)opDef;
		    this.enter((ExprNode)lv.expr, lv.con);
		    return;
		  }
		  // ignore overridden operators etc
		  break;
		}
		case SubstInKind : {
		  SubstInNode subscript1 = (SubstInNode)subscript;
		  Subst[] subs = subscript1.getSubsts();
		  Context c1 = c;
		  for (int i=0; i < subs.length; i++) {
		    c1 = c1.cons(subs[i].getOp(),
				 getVal(subs[i].getExpr(), c, false));
		  }
		  this.enter(subscript1.getBody(), c1);
		  return;
		}
		case LetInKind : { // a bit strange, but legal...
		  // note: the let-bound values become constants
		  // so they are uninteresting for our purposes
		  LetInNode subscript1 = (LetInNode)subscript;
		  this.enter(subscript1.getBody(), c);
		  return;
		}
                /***********************************************************
                * LabelKind case added by LL on 13 Jun 2007.               *
                ***********************************************************/
                case LabelKind : { // unlikely, but probably legal...
                  LabelNode subscript1 = (LabelNode)subscript;
                  this.enter((ExprNode) subscript1.getBody(), c);
                    /*******************************************************
                    * Cast to ExprNode added by LL on 19 May 2008 because  *
                    * of change to LabelNode class.                        *
                    *******************************************************/
                  return;
                }
		default : // give up
		  Assert.warn("cannot handle subscript " + subscript);
		  return;
		}
	      }

	      Vect getComponents() {
		return components;
	      }
	    }

	    SubscriptCollector collector = new SubscriptCollector();
	    Context c1 = c;
	    List subs1 = subs;
	    while (!subs1.isEmpty()) {
	      SubstInNode sn = (SubstInNode)subs1.car();
	      Subst[] snsubs = sn.getSubsts();
	      for (int i=0; i < snsubs.length; i++) {
		c1 = c1.cons(snsubs[i].getOp(),
			     getVal(snsubs[i].getExpr(), c, false));
	      }
	      subs1 = subs1.cdr();
	    }
	    collector.enter(subscript, c1);
	    varsInSubscript = collector.getComponents();
	  }
	  catch (Exception e) { // probably a ClassCastException
	    // Assert.printStack(e);
	    Assert.printWarning(TLCGlobals.warn,
				"TLC could not determine if the subscript of the next-state relation contains\nall state variables. Proceed with fingers crossed.");
	    varsInSubscript = null;
	  }
	  // ... and make sure they contain all the state variables
	  if (varsInSubscript != null) {
	    for (int i=0; i<this.variables.length; i++) {
	      if (!varsInSubscript.contains(this.variables[i])) {
		  // Assert.fail("The subscript of the next-state relation specified by the specification\ndoes not contain the state variable " + this.variables[i].getName());
		Assert.printWarning(TLCGlobals.warn,
				    "The subscript of the next-state relation specified by the specification\ndoes not seem to contain the state variable " + this.variables[i].getName());
	      }
	    }
	  }
	  if (this.nextPred == null) {
	    this.nextPred = new Action(this.addSubsts(arg, subs), c);
	  }
	  else {
	      Assert.fail("The specification contains more than one conjunct of the form [][Next]_v,\nbut TLC can handle only specifications with one next-state relation.");
	  }
	  // ---sm 09/06/04 >>>
	}
	else {
	  this.temporalVec.addElement(new Action(this.addSubsts(pred, subs), c));
	  this.temporalNameVec.addElement(pred.toString());
	}
	return;
      }
    }

    int level = this.getLevelBound(pred, c);
    if (level <= 1) {
      this.initPredVec.addElement(new Action(this.addSubsts(pred, subs), c));
    }
    else if (level == 3) {
      this.temporalVec.addElement(new Action(this.addSubsts(pred, subs), c));
      this.temporalNameVec.addElement(pred.toString());
    }
    else {
      Assert.fail("TLC cannot handle this conjunct of the spec:\n" + pred);
    }
  }

  /* Process the PROPERTIES field of the config file. */
  private final void processConfigProps(String name, ExprNode pred, Context c, List subs) {
    if (pred instanceof SubstInNode) {
      SubstInNode pred1 = (SubstInNode)pred;
      this.processConfigProps(name, pred1.getBody(), c, subs.cons(pred1));
      return;
    }
    if (pred instanceof OpApplNode) {
      OpApplNode pred1 = (OpApplNode)pred;
      ExprOrOpArgNode[] args = pred1.getArgs();
      if (args.length == 0) {
	SymbolNode opNode = pred1.getOperator();
	Object val = this.lookup(opNode, c, false);
	if (val instanceof OpDefNode) {
	  if (((OpDefNode)val).getArity() != 0) {
	    Assert.fail("The operator " + opNode.getName() + " cannot take any argument.");
	  }
	  this.processConfigProps(opNode.getName().toString(),
				  ((OpDefNode)val).getBody(),
				  c, subs);
	}
	else if (val == null) {
	  Assert.fail("The operator " + opNode.getName() + " is not defined in the spec.");
	}
	else if (val instanceof BoolValue) {
	  if (!((BoolValue)val).val) {
	    Assert.fail("The spec is trivially false because " + opNode.getName() + " is false");
	  }
	}
	else {
	  Assert.fail("The operator " + opNode.getName() + "is equal to " + val);
	}
	return;
      }
      int opcode = BuiltInOPs.getOpCode(pred1.getOperator().getName());
      if (opcode == OPCODE_cl || opcode == OPCODE_land) {
	for (int i = 0; i < args.length; i++) {
	  ExprNode conj = (ExprNode)args[i];
	  this.processConfigProps(conj.toString(), conj, c, subs);
	}
	return;
      }
      if (opcode == OPCODE_box) {	
	ExprNode boxArg = (ExprNode)args[0];
	if ((boxArg instanceof OpApplNode) &&
	    BuiltInOPs.getOpCode(((OpApplNode)boxArg).getOperator().getName()) == OPCODE_sa) {
	  OpApplNode boxArg1 = (OpApplNode)boxArg;
	  if (boxArg1.getArgs().length == 0) {
	    name = boxArg1.getOperator().getName().toString();
	  }
	  this.impliedActNameVec.addElement(name);
	  this.impliedActionVec.addElement(new Action(this.addSubsts(boxArg, subs), c));
	}
	else if (this.getLevelBound(boxArg, c) < 2) {
	  this.invVec.addElement(new Action(this.addSubsts(boxArg, subs), c));
	  if ((boxArg instanceof OpApplNode) &&
	      (((OpApplNode)boxArg).getArgs().length == 0)) {
	    name = ((OpApplNode)boxArg).getOperator().getName().toString();
	  }
	  this.invNameVec.addElement(name);
	}
	else {
	  this.impliedTemporalVec.addElement(new Action(this.addSubsts(pred, subs), c));
	  this.impliedTemporalNameVec.addElement(name);
	}
	return;
      }
    }
    int level = this.getLevelBound(pred, c);
    if (level <= 1) {
      this.impliedInitVec.addElement(new Action(this.addSubsts(pred, subs), c));
      this.impliedInitNameVec.addElement(name);
    }
    else if (level == 3) {
      this.impliedTemporalVec.addElement(new Action(this.addSubsts(pred, subs), c));
      this.impliedTemporalNameVec.addElement(name);
    }
    else {
      Assert.fail("The property " + name + " is not correctly defined.");
    }
  }
  
  private final Hashtable makeConstantTable(Vect consts) {
    Hashtable constTbl = new Hashtable();
    for (int i = 0; i < consts.size(); i++) {
      Vect line = (Vect)consts.elementAt(i);
      int len = line.size();
      String name = (String)line.elementAt(0);
      if (len <= 2) {
	constTbl.put(name, line.elementAt(1));	  
      }
      else {
	Object val = constTbl.get(name);
	if (val == null) {
	  OpRcdValue opVal = new OpRcdValue();
	  opVal.addLine(line);
	  constTbl.put(name, opVal);
	}
	else {
	  OpRcdValue opVal = (OpRcdValue)val;
	  int arity = ((Value[])opVal.domain.elementAt(0)).length;
	  if (len != arity + 2) {
	    Assert.fail("The arity of the operator " + name + " is inconsistent" +
			" in the configuration file.");
	  }
	  opVal.addLine(line);	    
	}
      }
    }
    return constTbl;
  }

  /* Initialize the spec constants using the config file.  */
  public final Hashtable initializeConstants() {
    Vect consts = this.config.getConstants();
    if (consts == null) { return new Hashtable(); }
    return this.makeConstantTable(consts);
  }

  public final Hashtable initializeModConstants() {
    Hashtable modConstants = this.config.getModConstants();
    Hashtable constants = new Hashtable();
    Enumeration mods = modConstants.keys();
    while (mods.hasMoreElements()) {
      String modName = (String)mods.nextElement();
      constants.put(modName, this.makeConstantTable((Vect)modConstants.get(modName)));
    }
    return constants;
  }
  
  /* Get model constraints.  */
  public final ExprNode[] getModelConstraints() {
    if (this.modelConstraints != null) {
      return this.modelConstraints;
    }
    Vect names = this.config.getConstraints();
    this.modelConstraints = new ExprNode[names.size()];
    int idx = 0;
    for (int i = 0; i < names.size(); i++) {
      String name = (String)names.elementAt(i);
      Object constr = this.defns.get(name);
      if (constr instanceof OpDefNode) {
	OpDefNode def = (OpDefNode)constr;
	if (def.getArity() != 0) {
	  Assert.fail("The constraint " + name + " cannot take any argument.");
	}
	this.modelConstraints[idx++] = def.getBody();
      }
      else if (constr != null) {
	if (!(constr instanceof BoolValue) ||
	    !((BoolValue)constr).val) {
	  Assert.fail("The constraint " + name + " is equal to " + constr);
	}
      }
      else {
	Assert.fail("The constraint " + name + " specified by the configuration" +
		    " file\nis not defined in the specification.");
      }
    }
    if (idx < this.modelConstraints.length) {
      ExprNode[] constrs = new ExprNode[idx];
      for (int i = 0; i < idx; i++) {
	constrs[i] = this.modelConstraints[i];
      }
      this.modelConstraints = constrs;
    }
    return this.modelConstraints;
  }

  /* Get action constraints.  */
  public final ExprNode[] getActionConstraints() {
    if (this.actionConstraints != null) {
      return this.actionConstraints;
    }
    Vect names = this.config.getActionConstraints();
    this.actionConstraints = new ExprNode[names.size()];
    int idx = 0;
    for (int i = 0; i < names.size(); i++) {
      String name = (String)names.elementAt(i);
      Object constr = this.defns.get(name);
      if (constr instanceof OpDefNode) {
	OpDefNode def = (OpDefNode)constr;
	if (def.getArity() != 0) {
	  Assert.fail("The action constraint " + name + " cannot take any argument.");
	}
	this.actionConstraints[idx++] = def.getBody();
      }
      else if (constr != null) {
	if (!(constr instanceof BoolValue) ||
	    !((BoolValue)constr).val) {
	  Assert.fail("The action constraint " + name + " is equal to " + constr);
	}
      }
      else {
	Assert.fail("The action constraint " + name + " specified by the configuration" +
		    " file\nis not defined in the specification.");
      }
    }
    if (idx < this.actionConstraints.length) {
      ExprNode[] constrs = new ExprNode[idx];
      for (int i = 0; i < idx; i++) {
	constrs[i] = this.actionConstraints[i];
      }
      this.actionConstraints = constrs;
    }
    return this.actionConstraints;
  }

  /* Get the initial state predicate of the specification.  */
  public final Vect getInitStateSpec() { return this.initPredVec; }

  /* Get the action (next state) predicate of the specification. */
  public final Action getNextStateSpec() { return this.nextPred; }

  /* Get the view mapping for the specification. */
  public final SemanticNode getViewSpec() {
    String name = this.config.getView();
    if (name.length() == 0) return null;

    Object view = this.defns.get(name);
    if (view == null) {
      Assert.fail("The view function " + name + " specified by the configuration" +
		  " file\nis not defined in the specification.");
    }
    if (!(view instanceof OpDefNode)) {
      Assert.fail("The view function " + name + " cannot be a constant.");
    }
    OpDefNode def = (OpDefNode)view;
    if (def.getArity() != 0) {
      Assert.fail("The view function " + name + " cannot take any argument.");
    }
    return def.getBody();
  }

  /* Get the type declaration for the state variables. */
  public final SemanticNode getTypeSpec() {
    String name = this.config.getType();
    if (name.length() == 0) {
      Assert.fail("The configuration file did not specify types for state variables.");
    }

    Object type = this.defns.get(name);
    if (type == null) {
      Assert.fail("The type " + name + " specified by the configuration file\n" +
		  "is not defined in the specification.");
    }
    if (!(type instanceof OpDefNode)) {
      Assert.fail("The type " + name + " specified by the configuration file\n" +
		  "cannot be a constant.");
    }
    OpDefNode def = (OpDefNode)type;
    if (def.getArity() != 0) {
      Assert.fail("The type " + name + " specified by the configuration file\n" +
		  "cannot take any argument.");
    }
    return def.getBody();
  }
  
  /* Get the type declaration for the state variables. */
  public final SemanticNode getTypeConstraintSpec() {
    String name = this.config.getTypeConstraint();
    if (name.length() == 0) { return null; }

    Object type = this.defns.get(name);
    if (type == null) {
      Assert.fail("The type constraint " + name + " specified by the configuration\n" +
		  "file is not defined in the specification.");
    }
    if (!(type instanceof OpDefNode)) {
      Assert.fail("The type constraint " + name + " specified by the configuration\n" +
		  "file cannot be a constant.");
    }
    OpDefNode def = (OpDefNode)type;
    if (def.getArity() != 0) {
      Assert.fail("The type constraint " + name + " specified by the configuration\n" +
		  "file cannot take any argument.");
    }
    return def.getBody();
  }
  
  public final boolean livenessIsTrue() {
    return this.impliedTemporals.length == 0;
  }

  /* Get the fairness condition of the specification.  */
  public final Action[] getTemporals() { return this.temporals; }

  public final String[] getTemporalNames() { return this.temporalNames; }

  /* Get the liveness checks of the specification.  */
  public final Action[] getImpliedTemporals() { return this.impliedTemporals; }

  public final String[] getImpliedTemporalNames() { return this.impliedTemporalNames; }

  /* Get the invariants of the specification. */
  public final Action[] getInvariants() { return this.invariants; }
  
  public final String[] getInvNames() { return this.invNames; }

  /* Get the implied-inits of the specification. */
  public final Action[] getImpliedInits() { return this.impliedInits; }

  public final String[] getImpliedInitNames() { return this.impliedInitNames; }
  
  /* Get the implied-actions of the specification. */
  public final Action[] getImpliedActions() { return this.impliedActions; }

  public final String[] getImpliedActNames() { return this.impliedActNames; }

  /* Get the assumptions of the specification. */
  public final ExprNode[] getAssumptions() { return this.assumptions; }

  /**
   * This method gets the value of a symbol from the enviroment. We
   * look up in the context c, its tool object, and the state s.
   */
  public final Object lookup(SymbolNode opNode, Context c, TLCState s, boolean cutoff) {
    boolean isVarDecl = (opNode.getKind() == VariableDeclKind);
    Object result = c.lookup(opNode, cutoff && isVarDecl);
    if (result != null) return result;

    result = opNode.getToolObject(TLCGlobals.ToolId);
    if (result != null) return result;

    if (opNode.getKind() == UserDefinedOpKind) {
      result = ((OpDefNode)opNode).getBody().getToolObject(TLCGlobals.ToolId);
      if (result != null) return result;
    }

    result = s.lookup(opNode.getName());
    if (result != null) return result;
    return opNode;
  }

  public final Object lookup(SymbolNode opNode, Context c, boolean cutoff) {
    boolean isVarDecl = (opNode.getKind() == VariableDeclKind);
    Object result = c.lookup(opNode, cutoff && isVarDecl);
    if (result != null) return result;

    result = opNode.getToolObject(TLCGlobals.ToolId);
    if (result != null) return result;

    if (opNode.getKind() == UserDefinedOpKind) {
      result = ((OpDefNode)opNode).getBody().getToolObject(TLCGlobals.ToolId);
      if (result != null) return result;
    }
    return opNode;
  }

  public final Object getVal(ExprOrOpArgNode expr, Context c, boolean cachable) {
    if (expr instanceof ExprNode) {
      LazyValue lval = new LazyValue(expr, c);
      if (!cachable) { lval.setUncachable(); }
      return lval;
    }
    SymbolNode opNode = ((OpArgNode)expr).getOp();
    return this.lookup(opNode, c, false);
  }

  public final Context getOpContext(OpDefNode opDef, ExprOrOpArgNode[] args,
				    Context c, boolean cachable) {
    FormalParamNode[] formals = opDef.getParams();
    int alen = args.length;
    Context c1 = c;
    for (int i = 0; i < alen; i++) {
      Object aval = this.getVal(args[i], c, cachable);
      c1 = c1.cons(formals[i], aval);
    }
    return c1;
  }

  /**
   * Return a table containing the locations of subexpression in the
   * spec of forms x' = e and x' \in e. Warning: Current implementation
   * may not be able to find all such locations.
   */
  public final ObjLongTable getPrimedLocs() {
    ObjLongTable tbl = new ObjLongTable(10);
    Action act = this.getNextStateSpec();
    this.collectPrimedLocs(act.pred, act.con, tbl);
    return tbl;
  }
  
  public final void collectPrimedLocs(SemanticNode pred, Context c, ObjLongTable tbl) {
    switch (pred.getKind()) {
    case OpApplKind:
      {
	OpApplNode pred1 = (OpApplNode)pred;
	this.collectPrimedLocsAppl(pred1, c, tbl);
	return;
      }
    case LetInKind:
      {
	LetInNode pred1 = (LetInNode)pred;
	this.collectPrimedLocs(pred1.getBody(), c, tbl);
	return;
      }
    case SubstInKind:
      {
	SubstInNode pred1 = (SubstInNode)pred;
	Subst[] subs = pred1.getSubsts();
	Context c1 = c;
	for (int i = 0; i < subs.length; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, true));
	}
	this.collectPrimedLocs(pred1.getBody(), c, tbl);
	return;
      }
   /***********************************************************************
   * LabelKind case added by LL on 13 Jun 2007.                           *
   ***********************************************************************/
   case LabelKind:
      {
	LabelNode pred1 = (LabelNode)pred;
	this.collectPrimedLocs(pred1.getBody(), c, tbl);
	return;
      }
    }
  }

  private final void collectPrimedLocsAppl(OpApplNode pred, Context c, ObjLongTable tbl) {
    ExprOrOpArgNode[] args = pred.getArgs();
    SymbolNode opNode = pred.getOperator();
    int opcode = BuiltInOPs.getOpCode(opNode.getName());

    switch (opcode) {
    case OPCODE_fa:     // FcnApply
      {
	this.collectPrimedLocs(args[0], c, tbl);
	break;
      }
    case OPCODE_ite:    // IfThenElse
      {
	this.collectPrimedLocs(args[1], c, tbl);
	this.collectPrimedLocs(args[2], c, tbl);	
	break;
      }
    case OPCODE_case:   // Case
      {
	for (int i = 0; i < args.length; i++) {
	  OpApplNode pair = (OpApplNode)args[i];
	  this.collectPrimedLocs(pair.getArgs()[1], c, tbl);
	}
	break;
      } 
    case OPCODE_eq:
    case OPCODE_in:
      {
	SymbolNode var = this.getPrimedVar(args[0], c, false);
	if (var != null && var.getName().getVarLoc() != -1) {
	  tbl.put(pred.toString(), 0);
	}
	break;
      }
    case OPCODE_cl:     // ConjList
    case OPCODE_dl:     // DisjList
    case OPCODE_be:     // BoundedExists
    case OPCODE_bf:     // BoundedForall
    case OPCODE_land:
    case OPCODE_lor:
    case OPCODE_implies:
      {
	for (int i = 0; i < args.length; i++) {
	  this.collectPrimedLocs(args[i], c, tbl);
	}
	break;
      }
    case OPCODE_unchanged:
      {
	this.collectUnchangedLocs(args[0], c, tbl);
	break;
      }
    case OPCODE_aa:     // AngleAct <A>_e
      {
	this.collectPrimedLocs(args[0], c, tbl);
	break;
      }
    case OPCODE_sa:	// [A]_e
      {
	this.collectPrimedLocs(args[0], c, tbl);
	tbl.put(args[1].toString(), 0);
	break;
      }
    default:
      {
	if (opcode == 0) {
	  Object val = this.lookup(opNode, c, false);

	  if (val instanceof OpDefNode) {
	    OpDefNode opDef = (OpDefNode)val;
	    Context c1 = this.getOpContext(opDef, args, c, true);
	    this.collectPrimedLocs(opDef.getBody(), c1, tbl);
	  }
	  else if (val instanceof LazyValue) {
	    LazyValue lv = (LazyValue)val;
	    this.collectPrimedLocs(lv.expr, lv.con, tbl);
	  }
	}
      }
    }
  }

  private final void collectUnchangedLocs(SemanticNode expr, Context c,
					  ObjLongTable tbl) {
    if (expr instanceof OpApplNode) {
      OpApplNode expr1 = (OpApplNode)expr;
      SymbolNode opNode = expr1.getOperator();
      UniqueString opName = opNode.getName();
      int opcode = BuiltInOPs.getOpCode(opName);

      if (opName.getVarLoc() >= 0) {
	// a state variable:
	tbl.put(expr.toString(), 0);
	return;
      }
      
      ExprOrOpArgNode[] args = expr1.getArgs();
      if (opcode == OPCODE_tup) {
	// a tuple:
	for (int i = 0; i < args.length; i++) {
	  this.collectUnchangedLocs(args[i], c, tbl);
	}
	return;
      }

      if (opcode == 0 && args.length == 0) {
	// a 0-arity operator:
	Object val = this.lookup(opNode, c, false);
	if (val instanceof OpDefNode) {
	  this.collectUnchangedLocs(((OpDefNode)val).getBody(), c, tbl);
	  return;
	}
      }
    }
    return;
  }
    
  /**
   * This method only returns an approximation of the level of the
   * expression.  The "real" level is at most the return value. Adding
   * <name, ValOne> to the context means that there is no need to
   * compute level for name.
   *
   * Note that this method does not work if called on a part of an
   * EXCEPT expression.
   */
  public final int getLevelBound(SemanticNode expr, Context c) {
    switch (expr.getKind()) {
    case OpApplKind:
      {
	OpApplNode expr1 = (OpApplNode)expr;
	return this.getLevelBoundAppl(expr1, c);
      }
    case LetInKind:
      {
	LetInNode expr1 = (LetInNode)expr;
	OpDefNode[] letDefs = expr1.getLets();
	int letLen = letDefs.length;
	Context c1 = c;
	int level = 0;
	for (int i = 0; i < letLen; i++) {
	  OpDefNode opDef = letDefs[i];	  
	  level = Math.max(level, this.getLevelBound(opDef.getBody(), c1));
	  c1 = c1.cons(opDef, ValOne);
	}
	return Math.max(level, this.getLevelBound(expr1.getBody(), c1));
      }
    case SubstInKind:
      {
	SubstInNode expr1 = (SubstInNode)expr;
	Subst[] subs = expr1.getSubsts();
	int slen = subs.length;
	Context c1 = c;
	for (int i = 0; i < slen; i++) {
	  Subst sub = subs[i];
	  c1 = c1.cons(sub.getOp(), this.getVal(sub.getExpr(), c, true));
	}
	return this.getLevelBound(expr1.getBody(), c1);
      }
    /***********************************************************************
    * LabelKind case added by LL on 13 Jun 2007.                           *
    ***********************************************************************/
    case LabelKind:
      {
	LabelNode expr1 = (LabelNode)expr;
	return this.getLevelBound(expr1.getBody(), c);
      }
    default:
      {
	return 0;
      }
    }
  }

  private final int getLevelBoundAppl(OpApplNode expr, Context c) {
    SymbolNode opNode = expr.getOperator();
    UniqueString opName = opNode.getName();
    int opcode = BuiltInOPs.getOpCode(opName);

    if (BuiltInOPs.isTemporal(opcode)) {
      return 3;     // Conservative estimate
    }

    if (BuiltInOPs.isAction(opcode)) {
      return 2;     // Conservative estimate
    }

    if (opcode == OPCODE_enabled) {
      return 1;     // Conservative estimate
    }

    int level = 0;
    ExprNode[] bnds = expr.getBdedQuantBounds();
    for (int i = 0; i < bnds.length; i++) {
      level = Math.max(level, this.getLevelBound(bnds[i], c));
    }
    
    if (opcode == OPCODE_rfs) {
      // For recursive function, don't compute level of the function body
      // again in the recursive call.
      SymbolNode fname = expr.getUnbdedQuantSymbols()[0];
      c = c.cons(fname, ValOne);
    }
    
    ExprOrOpArgNode[] args = expr.getArgs();    
    int alen = args.length;
    for (int i = 0; i < alen; i++) {
      if (args[i] != null) {
	level = Math.max(level, this.getLevelBound(args[i], c));
      }
    }

    if (opcode == 0) {
      // This operator is a user-defined operator.
      if (opName.getVarLoc() >= 0) return 1;
	
      Object val = this.lookup(opNode, c, false);
      if (val instanceof OpDefNode) {
	OpDefNode opDef = (OpDefNode)val;
	c = c.cons(opNode, ValOne);
	level = Math.max(level, this.getLevelBound(opDef.getBody(), c));
      }
      else if (val instanceof LazyValue) {
	LazyValue lv = (LazyValue)val;
	level = Math.max(level, this.getLevelBound(lv.expr, lv.con));
      }
    }
    return level;    
  }

  /* The level of the expression according to level checking. */
  public final int getLevel(LevelNode expr, Context c) {
    HashSet lpSet = expr.getLevelParams();
    if (lpSet.isEmpty()) return expr.getLevel();
    
    int level = expr.getLevel();
    Iterator iter = lpSet.iterator();
    while (iter.hasNext()) {
      SymbolNode param = (SymbolNode)iter.next();
      Object res = c.lookup(param, true);
      if (res != null) {
	if (res instanceof LazyValue) {
	  LazyValue lv = (LazyValue)res;
	  int plevel = this.getLevel((LevelNode)lv.expr, lv.con);
	  level = (plevel > level) ? plevel : level;
	}
	else if (res instanceof OpDefNode) {
	  int plevel = this.getLevel((LevelNode)res, c);
	  level = (plevel > level) ? plevel : level;
	}
      }
    }
    return level;
  }
}
