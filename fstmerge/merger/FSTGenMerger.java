package merger;

import jargs.gnu.CmdLineParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import modification.traversalLanguageParser.addressManagement.DuplicateFreeLinkedList;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import printer.PrintVisitorException;
import printer.PrintVisitorInterface;
import printer.csharp.CSharpPrintVisitor;
import printer.csharpm.CSharpMergePrintVisitor;
import printer.java.JavaPrintVisitor;
import printer.javam.JavaMergePrintVisitor;
import printer.pythonm.PythonMergePrintVisitor;
import printer.textm.TextMergePrintVisitor;
import util.DiffMerged;
import util.FPFNCandidates;
import util.MergeConflict;
import util.MergeResult;
import util.Util;
import builder.ArtifactBuilderInterface;
import builder.csharp.CSharpBuilder;
import builder.csharpm.CSharpMergeBuilder;
import builder.java.JavaBuilder;
import builder.javam.JavaMergeBuilder;
import builder.pythonm.PythonMergeBuilder;
import builder.textm.TextMergeBuilder;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import composer.FSTGenProcessor;
import de.ovgu.cide.fstgen.ast.AbstractFSTParser;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import de.ovgu.cide.fstgen.parsers.generated_java15_merge.SimplePrintVisitor;

public class FSTGenMerger extends FSTGenProcessor {

	/*
	 * MERGING
	 */
	public static final String MERGE_SEPARATOR 		 = "##FSTMerge##";
	public static final String SEMANTIC_MERGE_MARKER = "~~FSTMerge~~";
	private static LinkedList<FSTNode> baseNodes 	 = new LinkedList<FSTNode>();

	private MergeVisitor mergeVisitor = new MergeVisitor();

	public void initFields(){
		importsFromBase  = new LinkedList<String>();
		importsFromRight = new LinkedList<String>();
		importsFromLeft  = new LinkedList<String>();

		currentMergedClass = "";
		currentMergedRevisionFilePath = "";
		mapMergeConflicts = new HashMap<String, ArrayList<MergeConflict>>();

		methodsFromBase  	  	= new LinkedList<LinkedList<String>>();
		newMethodsFromRight 	= new LinkedList<LinkedList<String>>();
		newMethodsFromLeft  	= new LinkedList<LinkedList<String>>();
		editedMethodsFromRight 	= new LinkedList<LinkedList<String>>();
		editedMethodsFromLeft  	= new LinkedList<LinkedList<String>>();

		editedNodesFromRight=  ArrayListMultimap.create();
		editedNodesFromLeft	=  ArrayListMultimap.create();
		newNodesFromRight	=  ArrayListMultimap.create();
		newNodesFromLeft	=  ArrayListMultimap.create();
		nodesFromBase		=  ArrayListMultimap.create();

		possibleDuplicationsOrAdditions = ArrayListMultimap.create(); //FSTNode's foundCompatibleNode differentiate 

		nonJavaMergedFiles= 0;
		nonJavaEqualFiles = 0;
		nonJavaFilesConfs = 0;
		nonJavaFiles      = 0;

		javaMergedFiles		= 0;
		javaEqualFiles		= 0;
		javaFilesConfsSS 	= 0;
		javaFilesConfsUN 	= 0;
		javaFiles			= 0;

		badParsedFiles  = 0;
		totalFiles    	= 0;

		Util.JDIME_CONFS = 0;
		Util.JDIME_FILES = 0;
		Util.JDIME_LOCS  = 0;
	}

	public void resetFields(){
		try{
			//FPFN IMPORT ISSUE NEW
			importsFromBase.clear();  
			importsFromRight.clear();
			importsFromLeft.clear(); 

			currentMergedClass="";
			currentMergedRevisionFilePath="";
			currentMergeResult=null;
			mapMergeConflicts.clear();

			methodsFromBase.clear(); 
			newMethodsFromRight.clear();
			newMethodsFromLeft.clear(); 
			editedMethodsFromRight.clear();
			editedMethodsFromLeft.clear();

			editedNodesFromRight.clear();
			editedNodesFromLeft.clear();
			newNodesFromRight.clear();	
			newNodesFromLeft.clear();
			nodesFromBase.clear();	


			//FPFN DUPLICATIONS
			//file;FSTNode
			possibleDuplicationsOrAdditions.clear();

			//FPFN UTIL STATISTICS
			nonJavaMergedFiles= 0;
			nonJavaEqualFiles = 0;
			nonJavaFilesConfs = 0;
			nonJavaFiles      = 0;

			javaMergedFiles	= 0;
			javaEqualFiles	= 0;
			javaFilesConfsSS = 0;
			javaFilesConfsUN = 0;
			javaFiles		= 0;

			badParsedFiles  = 0;
			totalFiles    	= 0;

			Util.JDIME_CONFS = 0;
			Util.JDIME_FILES = 0;
			Util.JDIME_LOCS = 0;
			LineBasedMerger.errorFiles.clear();
			LineBasedMerger.currentFile = "";
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public FSTGenMerger() {
		super();
		mergeVisitor.registerMerger(new LineBasedMerger());
		ArtifactBuilderInterface stdJavaBuilder = null;
		ArtifactBuilderInterface stdCSharpBuilder = null;
		for (ArtifactBuilderInterface builder : this.getArtifactBuilders()) {
			if (builder instanceof JavaBuilder)
				stdJavaBuilder = builder;
			if (builder instanceof CSharpBuilder)
				stdCSharpBuilder = builder;
		}

		unregisterArtifactBuilder(stdJavaBuilder);
		unregisterArtifactBuilder(stdCSharpBuilder);

		registerArtifactBuilder(new JavaMergeBuilder());
		registerArtifactBuilder(new CSharpMergeBuilder());
		registerArtifactBuilder(new PythonMergeBuilder());
		registerArtifactBuilder(new TextMergeBuilder(".java"));
		registerArtifactBuilder(new TextMergeBuilder(".cs"));
		registerArtifactBuilder(new TextMergeBuilder(".py"));

		PrintVisitorInterface stdJavaPrinter = null;
		PrintVisitorInterface stdCSharpPrinter = null;
		for (PrintVisitorInterface printer : this.getPrintVisitors()) {
			if (printer instanceof JavaPrintVisitor)
				stdJavaPrinter = printer;
			if (printer instanceof CSharpPrintVisitor)
				stdCSharpPrinter = printer;
		}

		unregisterPrintVisitor(stdJavaPrinter);
		unregisterPrintVisitor(stdCSharpPrinter);

		registerPrintVisitor(new JavaMergePrintVisitor());
		registerPrintVisitor(new CSharpMergePrintVisitor());
		registerPrintVisitor(new PythonMergePrintVisitor());
		registerPrintVisitor(new TextMergePrintVisitor(".java"));
		registerPrintVisitor(new TextMergePrintVisitor(".cs"));
		registerPrintVisitor(new TextMergePrintVisitor(".py"));

		//FPFN
		initFields();

	}

	public void printUsage() {
		System.err
		.println("Usage: FSTGenMerger [-h, --help] [-o, --output-directory] \n"
				+ "                    [-b, --base-directory] [-p, --preprocess-files] \n"
				+ "                    <-e, --expression>|<-f, --filemerge> myfile parentfile yourfile \n");
	}

	public FPFNCandidates runMerger(String revisionFile) {
		try {
			FSTGenMerger merger = new FSTGenMerger();
			currentMergedRevisionFilePath = revisionFile;
			String files[] 		= {"--expression",revisionFile};

			long t0 = System.currentTimeMillis();

			merger.ignoreEqualFiles(revisionFile);

			FPFNCandidates candidates = merger.run(files);

			Util.countConflicts(revisionFile);

			merger.restoreEqualFiles(revisionFile);

			long tf = System.currentTimeMillis();
			System.out.println("merge time: " + ((tf-t0)/60000) + " minutes");

			return candidates;

		} catch (RuntimeException ru){
			ru.printStackTrace();
			System.err.println(ru.toString());
			deleteRevision();

		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}

	public FPFNCandidates runMerger(MergeResult mergeResult) {
		try {
			FSTGenMerger merger = new FSTGenMerger();
			currentMergeResult	= mergeResult;
			currentMergedRevisionFilePath = currentMergeResult.revision;
			String files[] 		= {"--expression",currentMergedRevisionFilePath};

			long t0 = System.currentTimeMillis();

			merger.ignoreEqualFiles(currentMergedRevisionFilePath);

			merger.ignoreNonJavaFiles(currentMergedRevisionFilePath);

			FPFNCandidates candidates = merger.run(files);

			Util.countConflicts(currentMergeResult);

			merger.restoreEqualFiles(currentMergedRevisionFilePath);

			Util.unMergeNonJavaFiles(currentMergedRevisionFilePath);

			long tf = System.currentTimeMillis();
			long mergeTime =  ((tf-t0)/60000);
			System.out.println("merge time: " + mergeTime + " minutes");

			merger.logFilesStatistics(currentMergedRevisionFilePath,mergeTime);
			merger.resetFields();

			return candidates;

		} catch (RuntimeException ru){
			ru.printStackTrace();
			System.err.println(ru.toString());
			deleteRevision();

		} catch (Exception e) {
			e.printStackTrace();
		} 
		return null;
	}

	public FPFNCandidates run(String[] args) throws RuntimeException {
		// configuration options
		CmdLineParser cmdparser = new CmdLineParser();
		CmdLineParser.Option outputdir = cmdparser.addStringOption('o',
				"output-directory");
		CmdLineParser.Option expression = cmdparser.addStringOption('e',
				"expression");
		CmdLineParser.Option basedir = cmdparser.addStringOption('b',
				"base-directory");
		@SuppressWarnings("unused")
		CmdLineParser.Option help = cmdparser.addBooleanOption('h', "help");
		CmdLineParser.Option preprocessfiles = cmdparser.addBooleanOption('p',
				"preprocess-files");
		CmdLineParser.Option quiet = cmdparser.addBooleanOption('q', "quiet");
		CmdLineParser.Option filemerge = cmdparser.addBooleanOption('f',
				"filemerge");

		try {
			cmdparser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.out.println(e.getMessage());
			printUsage();
			System.exit(2);
		}

		Boolean preprocessfilesval = (Boolean) cmdparser.getOptionValue(
				preprocessfiles, Boolean.FALSE);
		fileLoader.setPreprocessFiles(preprocessfilesval);
		Boolean filemergeval = (Boolean) cmdparser.getOptionValue(filemerge);
		String expressionval = (String) cmdparser.getOptionValue(expression);
		if (null == expressionval && null == filemergeval) {
			printUsage();
			System.exit(2);
		}
		String basedirval = (String) cmdparser.getOptionValue(basedir);
		if (null == basedirval) {
			basedirval = (new File(expressionval)).getAbsoluteFile()
					.getParentFile().getPath();
		}
		String outputdirval = (String) cmdparser.getOptionValue(outputdir);
		Boolean quietval = (Boolean) cmdparser.getOptionValue(quiet,
				Boolean.FALSE);

		try {

			List<ArtifactBuilderInterface> buildersAccepted = new ArrayList<ArtifactBuilderInterface>();

			try {
				fileLoader.loadFiles(expressionval, basedirval, false,buildersAccepted);
			} catch (cide.gparser.ParseException e1) {
				fireParseErrorOccured(e1);
				e1.printStackTrace();
			}

			if (null != outputdirval)
				featureVisitor.setWorkingDir(outputdirval);
			else
				featureVisitor.setWorkingDir(basedirval);
			featureVisitor.setExpressionName(expressionval);

			for (ArtifactBuilderInterface builder : buildersAccepted) {
				LinkedList<FSTNonTerminal> features = builder.getFeatures();

				if (!quietval)
					for (FSTNonTerminal feature : features) {
						String ftos = feature.toString();
						if (!ftos.isEmpty())
							System.out.println(feature.toString());
					}

				FSTNode merged;

				if (features.size() != 0) {
					merged = merge(features);

					//FPFN RENAMING ISSUE
					mergeVisitor.setErrorFiles(fileLoader.getComposer().getErrorFiles());
					mergeVisitor.setCurrentRevision(expressionval);

					mergeVisitor.visit(merged);

					if (!quietval) {
						String mtos = merged.toString();
						if (!mtos.isEmpty())
							System.err.println(merged.toString());
					}

					try {
						featureVisitor.visit((FSTNonTerminal) merged);
					} catch (PrintVisitorException e) {
						e.printStackTrace();
					}
				}
			}

			//FPFN
			FPFNCandidates candidates = new FPFNCandidates();

			//FPFN CONSECUTIVE LINES
			printConsecutiveLineNumbers(expressionval);

			//FPFN SPACING
			printSpacingNumbers(expressionval);

			//FPFN CONSECUTIVE LINES AND SPACING
			printConsectutiveLinesAndSpacingIntersectionNumbers(expressionval);

			//printEditionsToDifferentPartsOfSameStmtNumbers(expressionval);
			//countAndPrintFalseNegativesNewMethodsReferencingEditedOnes(expressionval);
			countAndPrintFalseNegativesNewArtefactReferencingEditedOnes(expressionval);

			//FPFN IMPORT ISSUE NEW
			candidates.importCandidates 	= countAndPrintFalseNegativeImportsUnMergeBased(expressionval);

			//FPFN RENAMING ISSUE
			printRenamingNumbers(expressionval);
			candidates.renamingCandidates 	= printListOfRenamings();

			//			//FPFN DUPLICATED ISSUE
			//			printDuplicatedMethodsNumbers(expressionval);
			//			candidates.duplicatedCandidates	= printListOfDuplications();	

			//FPFN DUPLICATIONS ISSUE NEW
			candidates.duplicatedCandidates = countAndPrintFalseNegativeDuplications(expressionval);

			setFstnodes(AbstractFSTParser.fstnodes);

			return candidates;

		} catch (MergeException me) {
			System.err.println(me.toString());
			me.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//removeBadParsedFiles();
		}
		return null;
	}

	public static void main(String[] args) {

		try {
			//			FileOutputStream f = new FileOutputStream("console.log");
			//			LoggerPrintStream tee = new LoggerPrintStream(f, System.out);
			//			System.setOut(tee);
			//

			FileInputStream stream 	 = new FileInputStream("/home/ines/gjcc/fpfnanalysis/samplerpl/java.revisions");
			InputStreamReader reader = new InputStreamReader(stream);
			BufferedReader br 	= new BufferedReader(reader);
			String line 	 	= br.readLine();
			while(line != null){
				FSTGenMerger merger 			= new FSTGenMerger();
				String file 					= line;
				currentMergedRevisionFilePath 	= line;
				String files[] 	= {"--expression",file};

				if(new File(currentMergedRevisionFilePath).exists()){

					long t0 = System.currentTimeMillis();
					merger.ignoreEqualFiles(currentMergedRevisionFilePath);
					merger.ignoreNonJavaFiles(currentMergedRevisionFilePath);

					merger.run(files);

					Util.countConflicts(currentMergedRevisionFilePath);

					merger.restoreEqualFiles(currentMergedRevisionFilePath);

					long tf = System.currentTimeMillis();
					long mergeTime =  ((tf-t0)/60000);
					System.out.println("merge time: " + mergeTime + " minutes");

					Util.unMergeNonJavaFiles(currentMergedRevisionFilePath);
					merger.logFilesStatistics(currentMergedRevisionFilePath,mergeTime);
					merger.resetFields();
				}

				line = br.readLine();
			}
			br.close();
			System.out.println("finished!!!");


			//			FSTGenMerger merger = new FSTGenMerger();
			//
			//			//String revision 	= "C:\\GGTS\\ggts-bundle\\workspace\\others\\allrevisionstemp\\java_cassandra\\revisions\\rev_4f3a9_bd889\\rev_4f3a9-bd889.revisions";;
			//			//String revision 	= "C:\\GGTS\\workspace\\GitCE\\test\\testinfra\\rev1\\rev1.revisions";
			//			//String revision 	= "C:\\Users\\Guilherme\\Desktop\\Itens Recentes\\mh\\rev.revisions";
			//			String revision 	= "/home/ines/Downloads/ggts-bundle/workspace_gjcc/GitCE/test/testinfra/rev1/rev1.revisions";
			//			//String revision 	= "C:\\fpfnsample\\java_cassandra\\revisions\\rev_2ce7b_e863c\\rev_2ce7b-e863c.revisions";
			//
			//
			//			currentMergedRevisionFilePath = revision;
			//			String files[] 		= {"--expression",revision};
			//
			//			long t0 = System.currentTimeMillis();
			//			merger.ignoreEqualFiles(revision);
			//
			//			merger.ignoreNonJavaFiles(revision);
			//
			//			merger.run(files);
			//
			//			Util.countConflicts(revision);
			//
			//			merger.restoreEqualFiles(revision);	
			//
			//			long tf = System.currentTimeMillis();
			//			long mergeTime =  ((tf-t0)/60000);
			//			System.out.println("merge time: " + mergeTime + " minutes");
			//
			//			Util.unMergeNonJavaFiles(revision);
			//			merger.logFilesStatistics(revision,mergeTime);
			//			merger.resetFields();


		} catch (RuntimeException ru){
			ru.printStackTrace();
			System.err.println(ru.toString());
			deleteRevision();

		} catch (Exception e) {
			e.printStackTrace();
		} 
	}

	private static FSTNode merge(List<FSTNonTerminal> tl) throws MergeException {

		if (tl.size() != 3)
			throw new MergeException(tl);

		tl.get(0).index = 0;
		tl.get(1).index = 1;
		tl.get(2).index = 2;

		//FPFN
		manageEmptyBase(tl.get(0), tl.get(1));

		FSTNode mergeLeftBase = merge(tl.get(0), tl.get(1), true);
		FSTNode mergeLeftBaseRight = merge(mergeLeftBase, tl.get(2), false);
		removeLoneBaseNodes(mergeLeftBaseRight);
		return mergeLeftBaseRight;
	}

	public static FSTNode merge(FSTNode nodeA, FSTNode nodeB, boolean firstPass) {
		return merge(nodeA, nodeB, null, firstPass);
	}

	public static FSTNode merge(FSTNode nodeA, FSTNode nodeB,FSTNonTerminal compParent, boolean firstPass) {//left, base

		// System.err.println("nodeA: " + nodeA.getName() + " index: " +
		// nodeA.index);
		// System.err.println("nodeB: " + nodeB.getName() + " index: " +
		// nodeB.index);


		//FPFN
		//UPDATING THE REFERECENCE OF THE CLASS BEING ANALYSED
		if("Java-File".equals(nodeA.getType())){
			String filePath = getFilePath(nodeA);
			currentMergedClass = filePath +File.separator+ nodeA.getName();
		}

		if (nodeA.compatibleWith(nodeB)) {

			FSTNode compNode = nodeA.getShallowClone();
			compNode.index = nodeB.index;
			compNode.setParent(compParent);

			if (nodeA instanceof FSTNonTerminal	&& nodeB instanceof FSTNonTerminal) {
				FSTNonTerminal nonterminalA = (FSTNonTerminal) nodeA;
				FSTNonTerminal nonterminalB = (FSTNonTerminal) nodeB;
				FSTNonTerminal nonterminalComp = (FSTNonTerminal) compNode;

				for (FSTNode childB : nonterminalB.getChildren()) {
					FSTNode childA = nonterminalA.getCompatibleChild(childB);
					if (childA == null) {
						//DIFFMERGED
						//tagFSTNodeFromRight(childB);

						FSTNode cloneB = childB.getDeepClone();
						if (childB.index == -1)
							childB.index = nodeB.index;
						cloneB.index = childB.index;
						nonterminalComp.addChild(cloneB);
						// System.err.println("cloneB: " + cloneB.getName() +
						// " index: " + cloneB.index);
						if (firstPass) { //means that the node came from the base revision
							baseNodes.add(cloneB);
						}

						//FPFN
						identifyFPFNCandidateRight(firstPass, childB);

					} else {
						if (childA.index == -1)
							childA.index = nodeA.index;
						if (childB.index == -1)
							childB.index = nodeB.index;


						// there is no need ??
						//						//FPFN
						//						if(isTypeValidAddedNode(childA) && !firstPass){
						//							setFoundCompatibleNode(FSTGenMerger.currentMergedClass,childA);
						//						}

						nonterminalComp.addChild(merge(childA, childB, nonterminalComp, firstPass));
					}
				}

				//				//FPFN
				//				if(firstPass){ //workaround for empty bases
				//					if(!isTypeValidAddedNode(nodeA)){
				//						fillLeftNodes(nodeA);
				//					}
				//				}


				for (FSTNode childA : nonterminalA.getChildren()) {
					FSTNode childB = nonterminalB.getCompatibleChild(childA);
					if (childB == null) {
						//DIFFMERGED
						//tagFSTNodeFromLeft(childA);

						FSTNode cloneA = childA.getDeepClone();
						if (childA.index == -1)
							childA.index = nodeA.index;
						cloneA.index = childA.index;
						nonterminalComp.addChild(cloneA);

						// System.err.println("cloneA: " + cloneA.getName() +
						// " index: " + cloneA.index);
						if (baseNodes.contains(childA)) {
							baseNodes.remove(childA);
							baseNodes.add(cloneA);
						}

						//FPFN
						identifyFPFNCandidateLeft(firstPass, childA);

					} else {
						if (!firstPass) {
							baseNodes.remove(childA);
						}

						//FPFN
						if(isTypeValidAddedNode(childB)){
							setFoundCompatibleNode(FSTGenMerger.currentMergedClass,childB);
						}
					}
				}
				return nonterminalComp;
			} else if (nodeA instanceof FSTTerminal	&& nodeB instanceof FSTTerminal	&& compParent instanceof FSTNonTerminal) {
				FSTTerminal terminalA = (FSTTerminal) nodeA;
				FSTTerminal terminalB = (FSTTerminal) nodeB;
				FSTTerminal terminalComp = (FSTTerminal) compNode;

				// SPECIAL CONFLICT HANDLER
				if (!terminalA.getMergingMechanism().equals("Default")) {
					terminalComp.setBody(mergeBody(terminalA.getBody(),	terminalB.getBody(), firstPass, terminalA.index,terminalB.index));
				}
				return terminalComp;
			}
			return null;
		} else
			return null;
	}

	private static String mergeBody(String bodyA, String bodyB,
			boolean firstPass, int indexA, int indexB) {

		// System.err.println(firstPass);
		// System.err.println("#" + bodyA + "#");
		// System.err.println("#" + bodyB + "#");

		if (bodyA.contains(SEMANTIC_MERGE_MARKER)) {
			return bodyA + " " + bodyB;
		} else {
			if (firstPass) {
				return SEMANTIC_MERGE_MARKER + " " + bodyA + " "
						+ MERGE_SEPARATOR + " " + bodyB + " " + MERGE_SEPARATOR;
			} else {
				if (indexA == 0)
					return SEMANTIC_MERGE_MARKER + " " + bodyA + " "
					+ MERGE_SEPARATOR + " " + MERGE_SEPARATOR + " "
					+ bodyB;
				else
					return SEMANTIC_MERGE_MARKER + " " + MERGE_SEPARATOR + " "
					+ bodyA + " " + MERGE_SEPARATOR + " " + bodyB;
			}
		}
	}

	private static void removeLoneBaseNodes(FSTNode mergeLeftBaseRight) {
		boolean removed = false;
		for (FSTNode loneBaseNode : baseNodes) {
			if (mergeLeftBaseRight == loneBaseNode) {
				FSTNonTerminal parent = (FSTNonTerminal) mergeLeftBaseRight
						.getParent();
				if (parent != null) {
					parent.removeChild(mergeLeftBaseRight);
					removed = true;
				}
			}
		}
		if (!removed && mergeLeftBaseRight instanceof FSTNonTerminal) {
			Object[] children = ((FSTNonTerminal) mergeLeftBaseRight)
					.getChildren().toArray();
			for (Object child : children) {
				removeLoneBaseNodes((FSTNode) child);
			}
		}
	}


	//	//DIFFMERGED 
	//	private static void tagFSTNodeFromLeft(FSTNode childA) {
	//		if(childA instanceof FSTTerminal){
	//			if(!((FSTTerminal)childA).getPrefix().contains("CONTRIB::") && !childA.getType().equals("EmptyDecl"))
	//				((FSTTerminal)childA).setPrefix(((FSTTerminal)childA).getPrefix() + "CONTRIB::LEFT::");
	//		}
	//		if(childA instanceof FSTNonTerminal){
	//			for (FSTNode childc : ((FSTNonTerminal) childA).getChildren()) {
	//				if(childc.getType().equals("Id")){
	//					if(!((FSTTerminal)childc).getBody().contains("CONTRIB::") && !childc.getType().equals("EmptyDecl")){
	//						((FSTTerminal)childc).setBody("CONTRIB::LEFT::" + ((FSTTerminal)childc).getBody());
	//						break;
	//					}
	//				}
	//			}
	//		}
	//	}
	//
	//	//DIFFMERGED 
	//	private static void tagFSTNodeFromRight(FSTNode childB) {
	//		if(childB instanceof FSTTerminal){
	//			if(!((FSTTerminal)childB).getPrefix().contains("CONTRIB::") && !childB.getType().equals("EmptyDecl"))
	//				((FSTTerminal)childB).setPrefix(((FSTTerminal)childB).getPrefix() + "CONTRIB::RIGHT::");
	//		}
	//		if(childB instanceof FSTNonTerminal){
	//			for (FSTNode childc : ((FSTNonTerminal) childB).getChildren()) {
	//				if(childc.getType().equals("Id")){
	//					if(!((FSTTerminal)childc).getBody().contains("CONTRIB::") && !childc.getType().equals("EmptyDecl")){
	//						((FSTTerminal)childc).setBody("CONTRIB::RIGHT::" + ((FSTTerminal)childc).getBody());
	//						break;
	//					}
	//				}
	//			}
	//		}
	//	}



	/*
	 * FPFN ANALYSIS
	 */
	//FPFN IMPORT ISSUE NEW
	private static LinkedList<String> importsFromBase  = new LinkedList<String>();
	private static LinkedList<String> importsFromRight = new LinkedList<String>();
	private static LinkedList<String> importsFromLeft  = new LinkedList<String>();

	//FPFN
	private static String currentMergedClass;
	private static String currentMergedRevisionFilePath;
	public static MergeResult currentMergeResult;
	public static Map<String, ArrayList<MergeConflict>> mapMergeConflicts = new HashMap<String, ArrayList<MergeConflict>>();


	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private static LinkedList<LinkedList<String>> methodsFromBase  	  = new LinkedList<LinkedList<String>>();
	private static LinkedList<LinkedList<String>> newMethodsFromRight = new LinkedList<LinkedList<String>>();
	private static LinkedList<LinkedList<String>> newMethodsFromLeft  = new LinkedList<LinkedList<String>>();
	public static LinkedList<LinkedList<String>> editedMethodsFromRight = new LinkedList<LinkedList<String>>();
	public static LinkedList<LinkedList<String>> editedMethodsFromLeft  = new LinkedList<LinkedList<String>>();

	public static Multimap<String, FSTNode> editedNodesFromRight=  ArrayListMultimap.create();
	public static Multimap<String, FSTNode> editedNodesFromLeft	=  ArrayListMultimap.create();
	public static Multimap<String, FSTNode> newNodesFromRight	=  ArrayListMultimap.create();
	public static Multimap<String, FSTNode> newNodesFromLeft	=  ArrayListMultimap.create();
	public static Multimap<String, FSTNode> nodesFromBase		=  ArrayListMultimap.create();

	//FPFN DUPLICATIONS
	//file;FSTNode
	public static Multimap<String, FSTNode> possibleDuplicationsOrAdditions = ArrayListMultimap.create(); //FSTNode's foundCompatibleNode differentiate 

	//FPFN UTIL STATISTICS
	public static int nonJavaMergedFiles= 0;
	public static int nonJavaEqualFiles = 0;
	public static int nonJavaFilesConfs = 0;
	public static int nonJavaFiles      = 0;

	public static int javaMergedFiles	= 0;
	public static int javaEqualFiles	= 0;
	public static int javaFilesConfsSS 	= 0;
	public static int javaFilesConfsUN 	= 0;
	public static int javaFiles			= 0;

	public static int badParsedFiles    = 0;
	public static int totalFiles    	= 0;

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private void countAndPrintFalseNegativesNewArtefactReferencingEditedOnes(String expressionval) throws IOException{
		List<String> logEntries = new ArrayList<String>();
		int newArtefactReferencingOldOne = 0;
		for(String unmergedFile : FSTGenMerger.newNodesFromLeft.keySet()){
			if(isClassAllowed(unmergedFile)){
				for(FSTNode newLeftNode : newNodesFromLeft.get(unmergedFile)){
					String newLeftNodeContet;
					try{
						newLeftNodeContet = (newLeftNode instanceof FSTTerminal)?(((FSTTerminal)newLeftNode).getBody()):(prettyPrint((FSTNonTerminal)newLeftNode));
					}catch(Exception e){
						newLeftNodeContet= "";
					}
					for(FSTNode editedRightNode : editedNodesFromRight.get(unmergedFile)){
						String editedRightNodeContent = (editedRightNode instanceof FSTTerminal)?(((FSTTerminal)editedRightNode).getBody()):(prettyPrint((FSTNonTerminal)editedRightNode));
						String editedRightNodeIdentif = editedRightNode.getName();
						if(referGeneralNAREO(newLeftNodeContet,editedRightNodeIdentif)){
							ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);
							if(null != mergeConflicts){
								for(MergeConflict mc : mergeConflicts){
									if(conflictContainsNAREO(mc,newLeftNodeContet,editedRightNodeContent, true)){
										newArtefactReferencingOldOne++;

										String logEntry = expressionval+";"+unmergedFile+";"+newLeftNodeContet+";"+editedRightNodeContent +";"+mc.getBody();
										logEntries.add(logEntry);
									}
								}
							}
						}
					}
				}
			}
		}
		for(String unmergedFile : FSTGenMerger.newNodesFromRight.keySet()){
			if(isClassAllowed(unmergedFile)){
				for(FSTNode newRightNode : newNodesFromRight.get(unmergedFile)){
					String newRightNodeContet;
					try{
						newRightNodeContet = (newRightNode instanceof FSTTerminal)?(((FSTTerminal)newRightNode).getBody()):(prettyPrint((FSTNonTerminal)newRightNode));
					} catch(Exception e){
						newRightNodeContet = "";
					}
					for(FSTNode editedLeftNode : editedNodesFromLeft.get(unmergedFile)){
						String editedLeftNodeContent = (editedLeftNode instanceof FSTTerminal)?(((FSTTerminal)editedLeftNode).getBody()):(prettyPrint((FSTNonTerminal)editedLeftNode));
						String editedLeftNodeIdentif = editedLeftNode.getName();
						if(referGeneralNAREO(newRightNodeContet,editedLeftNodeIdentif)){
							ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);
							if(null != mergeConflicts){
								for(MergeConflict mc : mergeConflicts){
									if(conflictContainsNAREO(mc,newRightNodeContet,editedLeftNodeContent, false)){
										newArtefactReferencingOldOne++;

										String logEntry = expressionval+";"+unmergedFile+";"+newRightNodeContet+";"+editedLeftNodeContent+";"+mc.getBody();
										logEntries.add(logEntry);
									}
								}
							}
						}
					}
				}
			}
		}
		editedNodesFromRight.clear();
		editedNodesFromLeft.clear();	
		newNodesFromRight.clear();	
		newNodesFromLeft.clear();	
		nodesFromBase.clear();

		newArtefactReferencingOldOne += countAndPrintFalseNegativesNewMethodsReferencingEditedOnes(expressionval,logEntries);

		printFalseNegativesNewMethodsReferencingEditedOnes(expressionval,logEntries, newArtefactReferencingOldOne);
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private int countAndPrintFalseNegativesNewMethodsReferencingEditedOnes(String expressionval, List<String> logEntries)   {
		int newMethodsReferencingEditedOnes = 0;
		for(LinkedList<String> newLeftMethod : newMethodsFromLeft){
			String leftMethodFile = newLeftMethod.get(0);
			String leftMethodDeclaration = newLeftMethod.get(1);
			for(LinkedList<String> editedRightMethod : editedMethodsFromRight){
				String rightMethodFile = editedRightMethod.get(0);
				String rightMethodDeclaration = editedRightMethod.get(1);
				String rightMethodSignature = editedRightMethod.get(2);
				if(leftMethodFile.equals(rightMethodFile)){
					if(referMethodNAREO(leftMethodDeclaration,rightMethodSignature)){
						String unmergedFile = leftMethodFile; //or rightMethodFile
						if(isClassAllowed(unmergedFile)){
							ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);
							if(null != mergeConflicts){
								for(MergeConflict mc : mergeConflicts){
									if(conflictContainsNAREO(mc,leftMethodDeclaration,rightMethodDeclaration, true)){
										newMethodsReferencingEditedOnes++;

										String logEntry = expressionval+";"+unmergedFile+";"+leftMethodDeclaration+";"+rightMethodDeclaration+";"+mc.getBody();
										logEntries.add(logEntry);
									}
								}
							}
						}
					}
				}
			}
		}
		for(LinkedList<String> newRightMethod : newMethodsFromRight){
			String rightMethodFile = newRightMethod.get(0);
			String rightMethodDeclaration = newRightMethod.get(1);
			for(LinkedList<String> editedLeftMethod : editedMethodsFromLeft){
				String leftMethodFile = editedLeftMethod.get(0);
				String leftMethodDeclaration = editedLeftMethod.get(1);
				String leftMethodSignature = editedLeftMethod.get(2);
				if(rightMethodFile.equals(leftMethodFile)){
					if(referMethodNAREO(rightMethodDeclaration,leftMethodSignature)){
						String unmergedFile = rightMethodFile; //or leftMethodFile
						if(isClassAllowed(unmergedFile)){
							ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);
							if(null != mergeConflicts){
								for(MergeConflict mc : mergeConflicts){
									if(conflictContainsNAREO(mc,rightMethodDeclaration,leftMethodDeclaration,false)){
										newMethodsReferencingEditedOnes++;

										String logEntry = expressionval+";"+unmergedFile+";"+leftMethodDeclaration+";"+rightMethodDeclaration+";"+mc.getBody();
										logEntries.add(logEntry);
									}
								}
							}
						}
					}
				}
			}
		}

		FSTGenMerger.editedMethodsFromLeft.clear();
		FSTGenMerger.editedMethodsFromRight.clear();
		FSTGenMerger.newMethodsFromLeft.clear();
		FSTGenMerger.newMethodsFromRight.clear();
		FSTGenMerger.methodsFromBase.clear();

		return newMethodsReferencingEditedOnes;
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private void printFalseNegativesNewMethodsReferencingEditedOnes(String expressionval, List<String> logEntries,int newArtefactsReferencingEditedOnes) throws IOException {
		//printing reports
		String header = "";
		File file = new File("results/ssmerge_newArtefactsReferencingEditedOnes_numbers.csv");
		if(!file.exists()){
			file.createNewFile();
			header = "revision;newArtefactsReferencingEditedOnes\n";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header);
			}
			bw.write(expressionval+";"+newArtefactsReferencingEditedOnes);
			if(null!=currentMergeResult)
				currentMergeResult.newArtefactsReferencingEditedOnes = newArtefactsReferencingEditedOnes;
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}

		header = "";
		file = new File("results/log_ssmerge_newArtefactsReferencingEditedOnes.csv");
		if(!file.exists()){
			file.createNewFile();
			header = "revision;file;leftContent;rightContent;conflict\n";
		}
		fw = new FileWriter(file, true);
		bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header);
			}
			for(String entry : logEntries){
				bw.write(entry);
			}
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private boolean referMethodNAREO(String addedMethod, String editedMethodSSMergeSignature) {
		if(!addedMethod.isEmpty() && !editedMethodSSMergeSignature.isEmpty()){
			editedMethodSSMergeSignature = editedMethodSSMergeSignature.split("\\(")[0];
			//return addedMethod.contains(editedMethodSSMergeSignature);
			return addedMethod.matches("(?s).*\\b"+editedMethodSSMergeSignature+"\\b.*");
		}else{
			return false;
		}
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private boolean referGeneralNAREO(String addedContent, String editedContent) {
		if(!addedContent.isEmpty() && !editedContent.isEmpty()){
			editedContent = editedContent.split("\\(")[0];
			return addedContent.matches("(?s).*\\b"+editedContent+"\\b.*");
		} else {
			return false;
		}
	}

	//FPFN NEW ARTEFACT REFERENCING EDITED ONE
	private boolean conflictContainsNAREO(MergeConflict mergeConflict, String addedContent, String editedContent, boolean addedByLeft) {
		if(!addedContent.isEmpty() && !editedContent.isEmpty() && !mergeConflict.getLeft().isEmpty() && !mergeConflict.getRight().isEmpty()){
			try {
				addedContent  = Util.getSingleLineContentNoSpacing(addedContent);
				editedContent = Util.getSingleLineContentNoSpacing(editedContent);
				String leftPart  = Util.getSingleLineContentNoSpacing(mergeConflict.getLeft());
				String rightPart = Util.getSingleLineContentNoSpacing(mergeConflict.getRight());
				if(addedByLeft){
					boolean conditiona = editedContent.contains(rightPart) && leftPart.contains(addedContent);
					boolean conditionb = rightPart.contains(editedContent) && leftPart.contains(addedContent);
					return (conditiona || conditionb);
				} else{
					boolean conditiona = editedContent.contains(leftPart) && rightPart.contains(addedContent);
					boolean conditionb = leftPart.contains(editedContent) && rightPart.contains(addedContent);
					return (conditiona || conditionb);
				}
			} catch (Exception e){
				e.printStackTrace();
				return false;
			}
		} else {
			return false;
		}
	}

	//FPFN IMPORT ISSUE NEW
	private ArrayList<String> countAndPrintFalseNegativeImportsParserBased(String expressionval) {
		try{

			int pairOfImportedPackages 	 			= 0;
			int pairOfImportedPackageAndMember 		= 0;
			int pairOfImportedPackageAndMemberUsed 	= 0;
			int pairOfImportedSameMember 			= 0;
			int totalInsertedImports				= importsFromLeft.size() + importsFromRight.size();

			ArrayList<String> membersAnalysed 	= new ArrayList<String>();
			ArrayList<String> issuedFiles		= new ArrayList<String>();

			while(!importsFromLeft.isEmpty()){
				String fullLeftImportStatement = importsFromLeft.poll();
				String leftImportsClass = fullLeftImportStatement.split(":")[0];
				String leftImportStmt	= fullLeftImportStatement.split(":")[1];
				if(isClassAllowed(leftImportsClass)){
					for(String fullRightIportStatement : importsFromRight){
						String rightImportsClass = fullRightIportStatement.split(":")[0];
						String rightImportStmt	= fullRightIportStatement.split(":")[1];
						if(isClassAllowed(rightImportsClass)){
							if(leftImportsClass.equals(rightImportsClass)){
								if(!rightImportStmt.equals(leftImportStmt)){

									File revFile 		= new File(currentMergedRevisionFilePath);
									String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
									String issuedFile	= expressionval+";"+mergedFolder+leftImportsClass;

									rightImportStmt = Util.getSingleLineContentNoSpacing(rightImportStmt);
									String[] aux = rightImportStmt.split("\\.");
									String rightImportedMember = aux[aux.length-1];
									leftImportStmt = Util.getSingleLineContentNoSpacing(leftImportStmt);
									aux = leftImportStmt.split("\\.");
									String leftImportedMember = aux[aux.length-1];

									if(rightImportedMember.equals("*;") && leftImportedMember.equals("*;")){ 			//p.* vs q.*
										pairOfImportedPackages++;
										if(!issuedFiles.contains(issuedFile))issuedFiles.add(issuedFile);
									} else if(rightImportedMember.equals("*;") || leftImportedMember.equals("*;")) {	//p.Z vs. q.*
										pairOfImportedPackageAndMember++;

										int rightIndex = rightImportStmt.lastIndexOf(".");
										String rightPackage = rightImportStmt.substring(0, rightIndex);
										int leftIndex = leftImportStmt.lastIndexOf(".");
										String leftPackage = leftImportStmt.substring(0, leftIndex);
										if(!rightPackage.equals(leftPackage)){
											String memberBeingAnalysed;
											if(rightImportedMember.equals("*;"))
												memberBeingAnalysed  = leftImportsClass+":"+leftImportedMember;
											else
												memberBeingAnalysed  = rightImportsClass+":"+rightImportedMember;
											//AVOIDING DUPLICATE ANALYSIS
											if(!membersAnalysed.contains(memberBeingAnalysed)){ 
												if(isMemberUsed(rightImportedMember.substring(0,rightImportedMember.length() - 1),leftImportedMember.substring(0,leftImportedMember.length() - 1),leftImportsClass))
													pairOfImportedPackageAndMemberUsed++;
												membersAnalysed.add(memberBeingAnalysed);
											}
										}
									} else if(rightImportedMember.equals(leftImportedMember)) {							//p.Z vs. q.Z
										pairOfImportedSameMember++;
										if(!issuedFiles.contains(issuedFile))issuedFiles.add(issuedFile);
									}
								}
							}
						}
					}
				}
			}

			//revision;packages;packages and members; same members
			String header = "";
			File file = new File("results/ssmerge_import_numbers.csv");
			if(!file.exists()){
				file.createNewFile();
				header = "revision;pairOfImportedPackages;pairOfImportedPackageAndMember;pairOfImportedPackageAndMemberUsed;pairOfImportedSameMember;totalInsertedImports\n";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			if(!header.isEmpty()){
				bw.write(header);
			}
			bw.write(expressionval+";"+pairOfImportedPackages+";"+pairOfImportedPackageAndMember+";"+pairOfImportedPackageAndMemberUsed+";"+pairOfImportedSameMember+";"+totalInsertedImports);

			if(currentMergeResult!=null){
				currentMergeResult.importIssuesFromSsmergeMemberMember 	 = pairOfImportedSameMember;
				currentMergeResult.importIssuesFromSsmergePackagePackage = pairOfImportedPackages;
				currentMergeResult.importIssuesFromSsmergePackageMember  = pairOfImportedPackageAndMemberUsed;
				currentMergeResult.importsInserted  					 = totalInsertedImports;
			}

			bw.newLine();
			bw.close();
			fw.close();

			importsFromBase.clear();
			importsFromLeft.clear();
			importsFromRight.clear();

			return issuedFiles;
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	//FPFN IMPORT ISSUE NEW
	private ArrayList<String> countAndPrintFalseNegativeImportsUnMergeBased(String expressionval) {
		try{

			int pairOfImportedPackages 	 			= 0;
			int pairOfImportedPackageAndMember 		= 0;
			int pairOfImportedPackageAndMemberUsed 	= 0;
			int pairOfImportedSameMember 			= 0;
			int totalInsertedImports				= importsFromLeft.size() + importsFromRight.size();

			ArrayList<String> membersAnalysed 					= new ArrayList<String>();
			ArrayList<String> typeAmbiguityCandidateFiles		= new ArrayList<String>();
			ArrayList<String> logentries 						= new ArrayList<String>();

			while(!importsFromLeft.isEmpty()){
				String fullLeftImportStatement = importsFromLeft.poll();
				String leftImportsClass = fullLeftImportStatement.split(":")[0];
				String leftImportStmt	= fullLeftImportStatement.split(":")[1];
				leftImportStmt = Util.getSingleLineContentNoSpacing(leftImportStmt);

				if(isClassAllowed(leftImportsClass)){
					for(String fullRightIportStatement : importsFromRight){
						String rightImportsClass = fullRightIportStatement.split(":")[0];
						String rightImportStmt	= fullRightIportStatement.split(":")[1];
						rightImportStmt = Util.getSingleLineContentNoSpacing(rightImportStmt);

						if(isClassAllowed(rightImportsClass)){
							if(leftImportsClass.equals(rightImportsClass)){

								//ONLY IF UNMERGE HAVE DETECTED A CONFLICT
								String unmergedFile = leftImportsClass; //or rightImportsClass
								ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);

								if(null != mergeConflicts){
									File revFile 		= new File(currentMergedRevisionFilePath);
									String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
									String typeAmbiguityCandidateFile	= expressionval+";"+mergedFolder+unmergedFile;

									if(!rightImportStmt.equals(leftImportStmt)){
										String[] aux = rightImportStmt.split("\\.");
										String rightImportedMember = aux[aux.length-1];
										aux = leftImportStmt.split("\\.");
										String leftImportedMember = aux[aux.length-1];


										//FIRST CASE: p.* vs q.*
										if(rightImportedMember.equals("*;") && leftImportedMember.equals("*;")){ 			
											for(MergeConflict mc : mergeConflicts){
												if(mc.contains(leftImportStmt, rightImportStmt)){
													pairOfImportedPackages++;
													if(!typeAmbiguityCandidateFiles.contains(typeAmbiguityCandidateFile))typeAmbiguityCandidateFiles.add(typeAmbiguityCandidateFile);

													String logentry = expressionval+";"+(mergedFolder+unmergedFile)+";"+leftImportStmt+";"+rightImportStmt+";"+mc.getBody();
													logentries.add(logentry);
													break;
												}
											}


										//SECOND CASE: p.Z vs. q.*
										} else if(rightImportedMember.equals("*;") || leftImportedMember.equals("*;")) {	
											for(MergeConflict mc : mergeConflicts){
												if(mc.contains(leftImportStmt, rightImportStmt)){
													pairOfImportedPackageAndMember++;

													int rightIndex = rightImportStmt.lastIndexOf(".");
													int leftIndex  = leftImportStmt.lastIndexOf(".");
													String rightPackage = rightImportStmt.substring(0, rightIndex);
													String leftPackage  = leftImportStmt.substring(0, leftIndex);
													if(!rightPackage.equals(leftPackage)){
														String memberBeingAnalysed;
														if(rightImportedMember.equals("*;"))
															memberBeingAnalysed  = leftImportsClass+":"+leftImportedMember;
														else
															memberBeingAnalysed  = rightImportsClass+":"+rightImportedMember;
														//AVOIDING DUPLICATED ANALYSIS
														if(!membersAnalysed.contains(memberBeingAnalysed)){ 
															if(isMemberUsed(rightImportedMember.substring(0,rightImportedMember.length() - 1),leftImportedMember.substring(0,leftImportedMember.length() - 1),leftImportsClass)){
																pairOfImportedPackageAndMemberUsed++;
															}
															membersAnalysed.add(memberBeingAnalysed);
														}
													}
													break;
												}
											}


										//THIRD CASE: p.Z vs. q.Z	
										} else if(rightImportedMember.equals(leftImportedMember)) {							
											for(MergeConflict mc : mergeConflicts){
												if(mc.contains(leftImportStmt, rightImportStmt)){
													pairOfImportedSameMember++;
													if(!typeAmbiguityCandidateFiles.contains(typeAmbiguityCandidateFile))typeAmbiguityCandidateFiles.add(typeAmbiguityCandidateFile);

													String logentry = expressionval+";"+(mergedFolder+unmergedFile)+";"+leftImportStmt+";"+rightImportStmt+";"+mc.getBody();
													logentries.add(logentry);
													break;
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}

			printLogPairOfImportedIssuesCandidateList(logentries);

			printFalseNegativeImportsUnMergeBasedNumbers(expressionval,
					pairOfImportedPackages, pairOfImportedPackageAndMember,
					pairOfImportedPackageAndMemberUsed,
					pairOfImportedSameMember, totalInsertedImports);

			return typeAmbiguityCandidateFiles;
		}catch(Exception e){
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}

	//FPFN IMPORT ISSUE NEW
	private void printFalseNegativeImportsUnMergeBasedNumbers(String expressionval,
			int pairOfImportedPackages, int pairOfImportedPackageAndMember,
			int pairOfImportedPackageAndMemberUsed,
			int pairOfImportedSameMember, int totalInsertedImports)
					throws IOException {
		String header = "";
		File file = new File("results/ssmerge_import_numbers.csv");
		if(!file.exists()){
			file.createNewFile();
			header = "revision;pairOfImportedPackages;pairOfImportedPackageAndMember;pairOfImportedPackageAndMemberUsed;pairOfImportedSameMember;totalInsertedImports\n";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		if(!header.isEmpty()){
			bw.write(header);
		}
		bw.write(expressionval+";"+pairOfImportedPackages+";"+pairOfImportedPackageAndMember+";"+pairOfImportedPackageAndMemberUsed+";"+pairOfImportedSameMember+";"+totalInsertedImports);

		if(currentMergeResult!=null){
			currentMergeResult.importIssuesFromSsmergePackagePackage = pairOfImportedPackages;
			currentMergeResult.importIssuesFromSsmergePackageMember  = pairOfImportedPackageAndMemberUsed;
			currentMergeResult.importIssuesFromSsmergeMemberMember 	 = pairOfImportedSameMember;
			currentMergeResult.importsInserted  					 = totalInsertedImports;
		}

		bw.newLine();
		bw.close();
		fw.close();

		importsFromBase.clear();
		importsFromLeft.clear();
		importsFromRight.clear();
	}

	//FPFN IMPORT ISSUE NEW
	private boolean isMemberUsed(String rightImportedMember,String leftImportedMember, String targetClass) {
		boolean result = false;
		try {
			//GETTING THE PATH OF THE FILES TO BE ANALYSED
			File revFile = new File(currentMergedRevisionFilePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String leftRevName 	= bufferedReader.readLine();
			String baseRevName 	= bufferedReader.readLine();
			String rightRevName = bufferedReader.readLine();
			String baseFolder 	= revFile.getParentFile() + File.separator + baseRevName;
			String leftFolder 	= revFile.getParentFile() + File.separator + leftRevName;
			String rightFolder 	= revFile.getParentFile() + File.separator + rightRevName;
			bufferedReader.close();

			String logEntry = currentMergedRevisionFilePath+";"+targetClass+";"+leftImportedMember+";"+rightImportedMember+";";

			//VERIFYING IF ONE FILE USES A SAME NAMED MEMBER IMPORTED BY THE OTHER FILE
			try {
				String memberName;
				File base = new File(baseFolder+targetClass);
				if(rightImportedMember.equals("*")){
					memberName = leftImportedMember;
					File right = new File(rightFolder+targetClass);
					ArrayList<String> rightContribs = (new DiffMerged().findLinesContributionsNonNumeric(right, base, base)).get(0);
					for(String contrib : rightContribs){
						//if(contrib.contains(memberName) && !contrib.contains("import")){
						if(contrib.matches("(?s).*\\b"+memberName+"\\b.*") && !contrib.contains("import ")){
							result = true;
							logEntry = logEntry + contrib;
							printpairOfImportedPackageAndMemberUsedList(logEntry);
							break;
						}
					}
				} else {
					memberName = rightImportedMember;
					File left = new File(leftFolder+targetClass);
					ArrayList<String> leftContribs;
					leftContribs = (new DiffMerged().findLinesContributionsNonNumeric(left, base, base)).get(0);
					for(String contrib : leftContribs){
						if(contrib.matches("(?s).*\\b"+memberName+"\\b.*") && !contrib.contains("import ")){
							result = true;
							logEntry = logEntry + contrib;
							printpairOfImportedPackageAndMemberUsedList(logEntry);
							break;
						}
					}
				}
				return result;

				//TO HAPPEN WHEN THE BASE FILE IS EMPTY
			} catch (IOException e) { 
				try {
					String memberName;
					File toBeAnalised;
					if(rightImportedMember.equals("*")){
						memberName = leftImportedMember;
						toBeAnalised = null;
					} else {
						memberName = rightImportedMember;
						toBeAnalised = null;
					}
					FileInputStream stream = new FileInputStream(toBeAnalised);
					InputStreamReader reader = new InputStreamReader(stream);
					BufferedReader br = new BufferedReader(reader);
					String line = br.readLine();
					while(line != null){
						if(line.matches("(?s).*\\b"+memberName+"\\b.*") && !line.contains("import ")){
							result = true;
							logEntry = logEntry + line;
							printpairOfImportedPackageAndMemberUsedList(logEntry);
							break;
						}
						line = br.readLine();
					}
					br.close();
					return result;
				} catch (IOException e1) {
					return false;
				}
			}		
		} catch (IOException e) {
			return false;
		}
	}

	//FPFN IMPORT ISSUE NEW
	private static String getFilePath(FSTNode node){
		String dir = "";
		if(node == null){
		} else 	if(node.getType().equals("Folder")){
			dir = getFilePath(node.getParent()) + File.separator + node.getName();
		} else {
			dir = getFilePath(node.getParent()) + dir;
		}
		return dir;
	}

	//FPFN IMPORT ISSUE NEW
	private boolean isClassAllowed(String className) {
		DuplicateFreeLinkedList<File> parsedErrors = fileLoader.getComposer().getErrorFiles();
		for(File errorFile : parsedErrors){
			if(errorFile.getAbsolutePath().contains(className)){
				return false;
			}
		}
		return true;
	}

	//FPFN RENAMING ISSUE
	private void printRenamingNumbers(String expressionval) throws IOException {
		String header = "";
		File file = new File( "results/ssmerge_renaming_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;possibleRenamings;renamingsDueToIdentation";
		}

		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			//bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames());
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames()+";"+this.mergeVisitor.getLineBasedMerger().getCountOfRenamesDueToIdentation());
			if(null!=currentMergeResult){
				currentMergeResult.renamingConflictsFromSsmerge = this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames();
				currentMergeResult.renamingConflictsFromSsmergeDueToIdentation = this.mergeVisitor.getLineBasedMerger().getCountOfRenamesDueToIdentation();
			}
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			//bw.write(expressionval+";"+0);
			bw.write(expressionval+";"+0+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN RENAMING ISSUE
	private List<String> printListOfRenamings() throws IOException {
		try{
			String header = "";
			List<String> listOfRenames = new ArrayList<String>((this.mergeVisitor.getLineBasedMerger()).mapRenamingConflicts.values());
			File file = new File( "results/log_ssmerge_renaming.csv" );
			if(!file.exists()){
				file.createNewFile();
				header="revision;file;methodSignature";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			for(String e : listOfRenames){
				bw.write(e);
				bw.newLine();
			}
			bw.close();
			fw.close();
			return listOfRenames;
		}catch(Exception e){
			e.printStackTrace();
			return new ArrayList<String>();
		}
	}

	//FPFN DUPLICATED METHOD ISSUE
	private void printDuplicatedMethodsNumbers(String expressionval) throws IOException {
		File file = new File( "results/ssmerge_duplicated_numbers.csv" );
		String header = "";
		if(!file.exists()){
			file.createNewFile();
			header = "revision;possibleDuplications";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleDuplications());
			if(null!=currentMergeResult)
				currentMergeResult.duplicationIssuesFromSsmerge = this.mergeVisitor.getLineBasedMerger().getCountOfPossibleDuplications();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN DUPLICATED METHOD ISSUE
	private List<String> printListOfDuplications() throws IOException {
		try{
			String header = "";
			List<String> listOfDuplications = this.mergeVisitor.getLineBasedMerger().listDuplicatedMethods;
			File file = new File( "results/log_ssmerge_duplicated.csv" );
			if(!file.exists()){
				file.createNewFile();
				header=  "revision;file;methodSignature";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			for(String e : listOfDuplications){
				bw.write(e);
				bw.newLine();
			}
			bw.close();
			fw.close();

			List<String> duplicateCandidates = new ArrayList<String>();
			for(String entry: this.mergeVisitor.getLineBasedMerger().listDuplicatedMethods){
				String[] columns = entry.split(";"); 
				String duplicateCandidate = columns[0]+";"+columns[1];
				if(!duplicateCandidates.contains(duplicateCandidate)){
					duplicateCandidates.add(duplicateCandidate);
				}
			}

			return duplicateCandidates;
		}catch(Exception e){e.printStackTrace();}
		return new ArrayList<String>();
	}

	//FPFN CONSECUTIVE LINES ISSUE
	private void printConsecutiveLineNumbers(String expressionval) throws IOException {
		String header = "";
		File file = new File( "results/ssmerge_consecutiveLines_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;consecutiveLinesConflicts";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfConsecutiveLinesConfs());
			if(null!=currentMergeResult)
				currentMergeResult.consecutiveLinesConflicts = this.mergeVisitor.getLineBasedMerger().getCountOfConsecutiveLinesConfs();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			//e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN SPACING ISSUE
	private void printSpacingNumbers(String expressionval) throws IOException {
		String header = "";
		File file = new File( "results/ssmerge_spacing_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;spacingConflicts";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			int spacings = this.mergeVisitor.getLineBasedMerger().getCountOfSpacingConfs() + this.mergeVisitor.getLineBasedMerger().getCountOfRenamesDueToIdentation();
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+spacings);
			if(null!=currentMergeResult)
				currentMergeResult.spacingConflicts = spacings;
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			//e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN CONSECUTIVE LINES AND SPACING INTERSECTION ISSUE
	private void printConsectutiveLinesAndSpacingIntersectionNumbers(String expressionval) throws IOException {
		String header = "";
		File file = new File( "results/ssmerge_consecutiveLinesAndSpacingIntersection_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;consecutiveAndSpacingConflicts";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfConsecutiveLinesAndSpacingConfs());
			if(null!=currentMergeResult)
				currentMergeResult.consecutiveLinesAndSpacingConflicts = this.mergeVisitor.getLineBasedMerger().getCountOfConsecutiveLinesAndSpacingConfs();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			//e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN EDITIONS TO DIFFERENT PARTS OF SAME STMT ISSUE
	private void printEditionsToDifferentPartsOfSameStmtNumbers(String expressionval) throws IOException {
		String header = "";
		File file = new File( "results/jdime_editionsToDifferentPartsOfSameStmt_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;editionsToDifferentPartsOfSameStmt";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfEditionsToDifferentPartsOfSameStmt());
			if(null!=currentMergeResult)
				currentMergeResult.editionsToDifferentPartsOfSameStmt = this.mergeVisitor.getLineBasedMerger().getCountOfEditionsToDifferentPartsOfSameStmt();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN IMPORT ISSUE NEW
	private void printpairOfImportedPackageAndMemberUsedList(String logEntry)  throws IOException{
		String header = "";
		File file = new File( "results/log_ssmerge_pairOfImportedPackageAndMemberUsed.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;file;leftImport;rightImport;reference";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(logEntry);
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.close();
			fw.close();
		}

	}

	//FPFN IMPORT ISSUE NEW
	private void printLogPairOfImportedIssuesCandidateList(ArrayList<String> logentries)  throws IOException{
		String header = "";
		File file = new File( "results/log_ssmerge_pairOfImportedIssuesCandidate.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;file;leftImport;rightImport;conflictUnmerge";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			for(String entry : logentries){
				bw.write(entry);
				bw.newLine();
			}
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.close();
			fw.close();
		}

	}

	//FPFN DUPLICATIONS ISSUE NEW
	private List<String> countAndPrintFalseNegativeDuplications(String expressionval) throws IOException{
		ArrayList<String> duplicationFilesCandidates = new ArrayList<String>();
		int duplications = 0;
		for(String unmergedFile : FSTGenMerger.possibleDuplicationsOrAdditions.keySet()){
			if(isClassAllowed(unmergedFile)){
				for(FSTNode possibleDuplicationNode : FSTGenMerger.possibleDuplicationsOrAdditions.get(unmergedFile)){
					if(possibleDuplicationNode.isFoundCompatibleNode()){
						boolean existsconflictWithNode = false;
						ArrayList<MergeConflict> mergeConflicts = mapMergeConflicts.get(unmergedFile);	
						String nodeIdentifier1  = getNodeIdentifier(possibleDuplicationNode);
						String nodeIdentifier2 = getNodeIdentifier(possibleDuplicationNode.getCompatibleNode());

						if(null != mergeConflicts){
							for(MergeConflict mc : mergeConflicts){
								if(mc.contains(nodeIdentifier1, nodeIdentifier2) || mc.contains(nodeIdentifier2, nodeIdentifier1)){
									existsconflictWithNode = true;
									printConflictingDuplication(expressionval, unmergedFile, nodeIdentifier1, nodeIdentifier2,mc.getBody());
									break;
								} 
							}
						}
						if(!existsconflictWithNode && !nodeIdentifier1.isEmpty() && !nodeIdentifier2.isEmpty()){//nodeIdentifier isEmpty when not relevant node
							duplications++;

							File revFile 		= new File(currentMergedRevisionFilePath);
							String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
							String duplicationFileCandidate = expressionval+";"+mergedFolder+unmergedFile;
							if(!duplicationFilesCandidates.contains(duplicationFileCandidate))duplicationFilesCandidates.add(duplicationFileCandidate);

							printNonConflictingDuplication(expressionval, unmergedFile, nodeIdentifier1, nodeIdentifier2);
						}
					}
				}
			}
		}

		printDuplicationsNumbers(expressionval, duplications);
		return duplicationFilesCandidates;
	}

	//FPFN DUPLICATIONS ISSUE NEW
	private void printDuplicationsNumbers(String expressionval, int duplications)
			throws IOException {
		String header = "";
		File file = new File( "results/ssmerge_duplications_numbers.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;duplications";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			if(null!=currentMergeResult){
				//currentMergeResult.duplicationIssuesFromParser  = duplications;
				currentMergeResult.duplicationIssuesFromSsmerge = duplications;
			}
			bw.write(expressionval+";"+duplications);
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			//e.printStackTrace();
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//FPFN DUPLICATIONS ISSUE NEW
	private void printConflictingDuplication(String expressionval,String filename, String nodeIdentifier1, String nodeIdentifier2, String conflictBody) throws IOException {
		String header = "";
		File file = new File( "results/log_conflictingduplications.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;file;nodeIdentifier1;;nodeIdentifier2;conflict";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+ filename + ";" + nodeIdentifier1+ ";" + nodeIdentifier2+";"+conflictBody);
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.close();
			fw.close();
		}
	}

	//FPFN DUPLICATIONS ISSUE NEW
	private void printNonConflictingDuplication(String expressionval, String filename, String nodeIdentifier1, String nodeIdentifier2) throws IOException {
		String header = "";
		File file = new File( "results/log_nonconflictingduplications.csv" );
		if(!file.exists()){
			file.createNewFile();
			header = "revision;file;nodeIdentifier1;nodeIdentifier2";
		}
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			if(!header.isEmpty()){
				bw.write(header+"\n");
			}
			bw.write(expressionval+";"+ filename + ";" +nodeIdentifier1+ ";" +nodeIdentifier2);
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			e.printStackTrace();
			bw.close();
			fw.close();
		}
	}

	//FPFN DUPLICATIONS ISSUE NEW
	private String getNodeIdentifier(FSTNode node){
		String identifier = "";
		try{
			String noderepresentation = "";
			if(node instanceof FSTNonTerminal){
				noderepresentation = prettyPrint((FSTNonTerminal)node);
			} else if (node instanceof FSTTerminal) {
				noderepresentation = ((FSTTerminal) node).getBody();
			}

			noderepresentation = Util.getSingleLineContent(noderepresentation);

			if(noderepresentation.contains("{"))
				identifier = noderepresentation.split("\\{")[0];
			else if(noderepresentation.contains("="))
				identifier = noderepresentation.split("=")[0];
			else if(noderepresentation.contains(";"))
				identifier = noderepresentation.split(";")[0];
		} catch(Exception e){
			identifier = String.valueOf(System.currentTimeMillis()); //RANDOM VALUE TO AVOID MISRESULTS
		}
		return identifier;
	}

	//FPFN
	private static void identifyFPFNCandidateLeft(boolean firstPass,FSTNode childA) {
		//FPFN
		if(firstPass){//indicates left nodes
			if(isTypeValidAddedNode(childA) && isContentValidNode(childA)){
				if(!Util.multimapContains(possibleDuplicationsOrAdditions,currentMergedClass,childA) && !Util.multimapContains(nodesFromBase,currentMergedClass,childA)){
					FSTGenMerger.possibleDuplicationsOrAdditions.put(FSTGenMerger.currentMergedClass,childA);
				}
			}
		}
		//IMPORT ISSUE NEW
		//DOES THE NEW IMPORT DECLARATION CAME FROM LEFT REVISION?
		if(childA.getType().contains("ImportDeclaration")){
			String importStatement = currentMergedClass+":"+((FSTTerminal)childA).getBody();
			if(!importsFromBase.contains(importStatement) && !importsFromLeft.contains(importStatement))
				importsFromLeft.add(importStatement);
		}

		//FPFN NEW ARTEFACT REFERENCING EDITED ONE
		//DOES THE NEW METHOD DECLARATION CAME FROM LEFT REVISION?
		else if(childA.getType().contains("MethodDecl")){
			LinkedList<String> entry = new LinkedList<String>();
			entry.add(currentMergedClass);
			entry.add(((FSTTerminal)childA).getBody());
			if(!Util.contains(newMethodsFromLeft,entry) && !Util.contains(methodsFromBase,entry)){
				newMethodsFromLeft.add(entry);
				newNodesFromLeft.put(currentMergedClass,childA);
			}
		}
		//FPFN NEW METHOD REFERENCING EDITED METHOD
		//DOES THE NEW NODE CAME FROM LEFT REVISION?
		else if (isTypeValidAddedNode(childA) && isContentValidNode(childA)) {
			if(!Util.multimapContains(newNodesFromLeft,currentMergedClass,childA) && !Util.multimapContains(nodesFromBase,currentMergedClass,childA)){
				newNodesFromLeft.put(currentMergedClass,childA);
			}

		}
	}

	//FPFN
	private static void identifyFPFNCandidateRight(boolean firstPass,FSTNode childB) {
		if(!firstPass){//indicates right node
			if(isTypeValidAddedNode(childB) && isContentValidNode(childB)){
				if(!Util.multimapContains(possibleDuplicationsOrAdditions,currentMergedClass,childB) && !Util.multimapContains(nodesFromBase,currentMergedClass,childB)){
					FSTGenMerger.possibleDuplicationsOrAdditions.put(FSTGenMerger.currentMergedClass,childB);
				}
			}
		}
		//FPFN IMPORT ISSUE NEW
		//DOES THE NEW IMPORT DECLARATION CAME FROM RIGHT REVISION?
		if(childB.getType().contains("ImportDeclaration")){
			String importStatement = currentMergedClass+":"+((FSTTerminal)childB).getBody();
			if (!firstPass) {
				if(!importsFromRight.contains(importStatement))
					importsFromRight.add(importStatement);
			} else {
				if(!importsFromBase.contains(importStatement))
					importsFromBase.add(importStatement);
			}
		}
		//FPFN NEW ARTEFACT REFERENCING EDITED ONE
		//DOES THE NEW METHOD DECLARATION CAME FROM RIGHT REVISION?
		else if(childB.getType().contains("MethodDecl")){
			LinkedList<String> entry = new LinkedList<String>();
			entry.add(currentMergedClass);
			entry.add(((FSTTerminal)childB).getBody());
			if (!firstPass) {
				if(!Util.contains(newMethodsFromRight,entry)){
					newMethodsFromRight.add(entry);
					newNodesFromRight.put(currentMergedClass, childB);
				}
			} else {
				if(!Util.contains(methodsFromBase,entry)){
					methodsFromBase.add(entry);
					nodesFromBase.put(currentMergedClass, childB);
				}
			}
		}
		//FPFN NEW ARTEFACT REFERENCING EDITED ONE
		//DOES THE NEW NODE CAME FROM RIGHT REVISION?
		else if(isTypeValidAddedNode(childB) && isContentValidNode(childB)) {
			if (!firstPass) {
				if(!Util.multimapContains(newNodesFromRight,currentMergedClass,childB)){
					newNodesFromRight.put(currentMergedClass, childB);
				}
			} else {
				if(!Util.multimapContains(nodesFromBase,currentMergedClass,childB)){
					nodesFromBase.put(currentMergedClass, childB);
				}
			}
		}
	}

	//FPFN
	private static void fillLeftNodes(FSTNode node) {
		if(node instanceof FSTNonTerminal){
			FSTNonTerminal nontnode = (FSTNonTerminal) node;
			for (FSTNode child : nontnode.getChildren()) {
				if("Java-File".equals(child.getType())){
					String filePath = getFilePath(child);
					currentMergedClass = filePath +File.separator+ child.getName();
				}
				identifyFPFNCandidateLeft(true, child);
				fillLeftNodes(child);
			}
		}
	}

	//FPFN
	private static boolean isTypeValidAddedNode(FSTNode node) {
		return 	!node.getType().contains("Folder") 				&&
				!node.getType().contains("Feature") 			&&
				!node.getType().contains("File") 				&&
				!node.getType().contains("Content")				&&
				!node.getType().contains("ClassDeclaration") 	&&
				!node.getType().contains("CompilationUnit")  	&&
				!node.getType().contains("PackageDeclaration") 	&&
				!node.getType().contains("ImportDeclaration");
	}

	//FPFN
	private static void setFoundCompatibleNode(String file, FSTNode candidate) {
		for (FSTNode node : FSTGenMerger.possibleDuplicationsOrAdditions.get(file)) {
			if (node.compatibleWith(candidate)){
				node.setFoundCompatibleNode(true);
				node.setCompatibleNode(candidate);
			}
		}		
	}

	//FPFN
	private static boolean isContentValidNode(FSTNode childA) {
		if(childA instanceof FSTTerminal){
			String ctnt = ((FSTTerminal) childA).getBody();
			String oneLineNodeContent = ctnt.replaceAll("\\r\\n|\\r|\\n","");
			int numberOfWordsOfNodeContent = (oneLineNodeContent.split("\\s+")).length;
			return (numberOfWordsOfNodeContent > 1);		
		} else {
			return true;
		}
	}

	//FPFN
	private static void manageEmptyBase(FSTNode left,FSTNode base) {
		fillBaseNodes(base);
		fillLeftNodes(left);	
	}

	//FPFN
	private static void fillBaseNodes(FSTNode node) {
		if(null!=node){
			if("Java-File".equals(node.getType())){
				String filePath = getFilePath(node);
				currentMergedClass = filePath +File.separator+ node.getName();
			}

			if(isTypeValidAddedNode(node) && isContentValidNode(node)){
				nodesFromBase.put(currentMergedClass, node);
			}

			if(node instanceof FSTNonTerminal){
				FSTNonTerminal nontnode = (FSTNonTerminal) node;
				for (FSTNode child : nontnode.getChildren()) {
					fillBaseNodes(child);
				}
			}
			return;
		}
	}



	/*
	 * GENERAL
	 */
	private void removeBadParsedFiles() {
		try {
			//Identifying folders names
			File revFile 	= new File(currentMergedRevisionFilePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath())));   
			String leftRevName 	= bufferedReader.readLine();
			String baseRevName 	= bufferedReader.readLine();
			String rightRevName = bufferedReader.readLine();
			String mergedName 	= revFile.getName().split("\\.")[0];
			String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
			bufferedReader.close();

			//first, search and delete bad parsed files
			FSTGenProcessor composer = fileLoader.getComposer();
			DuplicateFreeLinkedList<File> parsedErrors = composer.getErrorFiles();
			for(File f : parsedErrors){
				String filePath = f.getAbsolutePath();
				String fileToDelete = ((filePath.replaceFirst(baseRevName, mergedName)).replaceFirst(leftRevName, mergedName)).replaceFirst(rightRevName, mergedName);
				String ssmergeout 	= fileToDelete;
				String mergout 		= ssmergeout + ".merge";
				File file = new File(ssmergeout);
				if(file.exists())
					FileDeleteStrategy.FORCE.delete(file);
				file = new File(mergout);
				if(file.exists())
					FileDeleteStrategy.FORCE.delete(file);
			}

			//second, delete the output folder if it gets empty
			File folder = new File(mergedFolder);
			if(isDirectoryEmpty(folder)){
				FileUtils.deleteDirectory(new File(mergedFolder));
				FileDeleteStrategy.FORCE.delete(new File(currentMergedRevisionFilePath));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void deleteRevision() {
		try {
			File revFile 	= new File(currentMergedRevisionFilePath);
			FileUtils.deleteDirectory(new File(revFile.getParent()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isDirectoryEmpty(File directory){
		boolean result = true;
		if(directory.isDirectory()){
			String files[] = directory.list();
			for (String temp : files) {
				File fileDelete = new File(directory, temp);
				result = result && isDirectoryEmpty(fileDelete);
			}
		}else{
			result = false;
		}
		return result;
	}

	private void ignoreEqualFiles(String revisionFilePath){
		try {
			File revFile = new File(revisionFilePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String leftRevName 	= bufferedReader.readLine();
			String baseRevName 	= bufferedReader.readLine();
			String rightRevName = bufferedReader.readLine();
			String baseFolder 	= revFile.getParentFile() + File.separator + baseRevName;
			bufferedReader.close();
			moveEqualFiles(leftRevName,baseRevName,baseFolder,rightRevName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void ignoreNonJavaFiles(String revisionFilePath){
		try {
			File revFile = new File(revisionFilePath);
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(revFile.getAbsolutePath()))); 
			String leftRevName 	= bufferedReader.readLine();
			String baseRevName 	= bufferedReader.readLine();
			String rightRevName = bufferedReader.readLine();
			String baseFolder 	= revFile.getParentFile() + File.separator + baseRevName;
			String leftFolder 	= revFile.getParentFile() + File.separator + leftRevName;
			String rightFolder 	= revFile.getParentFile() + File.separator + rightRevName;
			bufferedReader.close();


			moveNonJavaFiles(leftRevName,baseRevName,baseFolder,rightRevName);
			moveNonJavaFiles(null,leftRevName,leftFolder,rightRevName);
			moveNonJavaFiles(null,rightRevName,rightFolder,leftRevName);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void restoreEqualFiles(String revisionFilePath) throws IOException{
		//StringBuffer sb = new StringBuffer(revisionFilePath);
		//sb.setLength(sb.lastIndexOf("."));
		//sb.delete(0, sb.lastIndexOf(File.separator) + 1);
		String tempFolder = revisionFilePath.replaceFirst((new File(revisionFilePath).getName()),"_equalFiles");
		//String resultAlias = sb.toString();
		String resultAlias 	= (new File(revisionFilePath)).getName().split("\\.")[0];
		if(new File(tempFolder).exists()){
			moveEqualFilesBack("_equalFiles", tempFolder, resultAlias);
			FileUtils.deleteDirectory(new File(tempFolder));
		}
	}

	private void moveEqualFiles(String leftRevName, String baseRevName, String baseFolder, String rightRevName) throws IOException{
		File directory = new File(baseFolder);
		if(directory.exists()){
			File[] fList = directory.listFiles();
			for (File file : fList){
				if (file.isDirectory()){
					moveEqualFiles(leftRevName, baseRevName, file.getAbsolutePath(), rightRevName);
				} else {
					//String leftFilePath   = file.getAbsolutePath().replaceFirst("rev_[\\w]+_[\\w]+", leftAlias);
					String leftFilePath   = file.getAbsolutePath().replaceFirst(baseRevName, leftRevName);
					String rightFilePath  = file.getAbsolutePath().replaceFirst(baseRevName, rightRevName);
					if(Util.isFilesContentEqual(leftFilePath, file.getAbsolutePath(), rightFilePath)){
						String temp = file.getAbsolutePath().replaceFirst(baseRevName, "_equalFiles");
						FileUtils.moveFile(file, new File(temp));

						File l = new File(leftFilePath);
						File r = new File(rightFilePath);
						l.setWritable(true);
						r.setWritable(true);
						l.delete();
						r.delete();
						//						FileUtils.forceDelete(new File(leftFilePath));
						//						FileUtils.forceDelete(new File(rightFilePath));
						System.out.println("ignoring equal file: " + file.getAbsolutePath());


						if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
							++FSTGenMerger.javaEqualFiles;
						}else{
							++FSTGenMerger.nonJavaEqualFiles;
						}

					} else if(Util.isFilesContentEqual(leftFilePath, file.getAbsolutePath())){
						File rightFile = new File(rightFilePath);
						if(rightFile.exists()){
							String temp = rightFile.getAbsolutePath().replaceFirst(rightRevName, "_equalFiles");
							FileUtils.moveFile(rightFile, new File(temp));
						}

						File l = new File(leftFilePath);
						File r = new File(rightFilePath);
						l.setWritable(true);
						r.setWritable(true);
						l.delete();
						r.delete();
						//						
						//						FileUtils.forceDelete(new File(leftFilePath));
						//						FileUtils.forceDelete(new File(file.getAbsolutePath()));
						System.out.println("ignoring equal file: " + file.getAbsolutePath());



						if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
							++FSTGenMerger.javaEqualFiles;
						}else{
							++FSTGenMerger.nonJavaEqualFiles;
						}

					} else if(Util.isFilesContentEqual(rightFilePath, file.getAbsolutePath())){
						File leftFile = new File(leftFilePath);
						if(leftFile.exists()){
							String temp = leftFile.getAbsolutePath().replaceFirst(leftRevName, "_equalFiles");
							FileUtils.moveFile(leftFile, new File(temp));
						}
						File l = new File(leftFilePath);
						File r = new File(rightFilePath);
						l.setWritable(true);
						r.setWritable(true);
						l.delete();
						r.delete();
						//						FileUtils.forceDelete(new File(rightFilePath));
						//						FileUtils.forceDelete(new File(file.getAbsolutePath()));
						System.out.println("ignoring equal file: " + file.getAbsolutePath());



						if(FilenameUtils.getExtension(file.getAbsolutePath()).equalsIgnoreCase("java")){
							++FSTGenMerger.javaEqualFiles;
						}else{
							++FSTGenMerger.nonJavaEqualFiles;
						}
					}
				}
			}
		}
	}

	private void moveNonJavaFiles(String mineRevName, String baseRevName, String baseFolder, String othersRevName) throws IOException{
		File directory = new File(baseFolder);
		if(directory.exists()){
			File[] fList = directory.listFiles();
			for (File baseFile : fList){
				if (baseFile.isDirectory()){
					moveNonJavaFiles(mineRevName, baseRevName, baseFile.getAbsolutePath(), othersRevName);
				} else {
					if(!FilenameUtils.getExtension(baseFile.getAbsolutePath()).equalsIgnoreCase("java")){
						++FSTGenMerger.nonJavaMergedFiles;

						//moving base
						String temp = baseFile.getAbsolutePath().replaceFirst(baseRevName, "_nonJavaFiles/"+baseRevName);
						FileUtils.moveFile(baseFile, new File(temp));

						//moving right if exists
						String rightFilePath  = baseFile.getAbsolutePath().replaceFirst(baseRevName, othersRevName);
						File rightFile = new File(rightFilePath);
						if(rightFile.exists()){
							temp = rightFile.getAbsolutePath().replaceFirst(othersRevName, "_nonJavaFiles/"+othersRevName);
							FileUtils.moveFile(rightFile, new File(temp));
						}

						if(mineRevName!=null){
							//moving left if exists
							String leftFilePath   = baseFile.getAbsolutePath().replaceFirst(baseRevName, mineRevName);
							File leftFile = new File(leftFilePath);
							if(leftFile.exists()){
								temp = leftFile.getAbsolutePath().replaceFirst(mineRevName, "_nonJavaFiles/"+mineRevName);
								FileUtils.moveFile(leftFile, new File(temp));
							}
						}

						System.out.println("moving non-java file: " + baseFile.getAbsolutePath());
					} 
				}
			}
		}
	}

	private void moveEqualFilesBack(String tempAlias, String tempDir, String resultAlias) throws IOException{
		File directory = new File(tempDir);
		File[] fList = directory.listFiles();
		for (File file : fList){
			if (file.isDirectory()){
				moveEqualFilesBack(tempAlias,file.getAbsolutePath(),resultAlias);
			} else {
				File finalFile = new File(file.getAbsolutePath().replaceFirst(tempAlias, resultAlias));
				try{
					FileUtils.moveFile(file, finalFile);
				}catch(Exception e){
					e.printStackTrace();
				}
				System.out.println("restoring: " + file.getAbsolutePath());
			}
		}
	}

	private static String prettyPrint(FSTNonTerminal nonTerminal){
		SimplePrintVisitor visitor = new SimplePrintVisitor();
		visitor.visit(nonTerminal);
		return visitor.getResult().replaceAll(("  "), " ");
	}

	private void logFilesStatistics(String revision, long mergeTime) throws IOException {
		try{
			badParsedFiles 	= LineBasedMerger.errorFiles.size();
			javaFiles 	 	= javaMergedFiles + javaEqualFiles + badParsedFiles;
			nonJavaFiles 	= nonJavaMergedFiles + nonJavaEqualFiles;
			totalFiles 	 	= javaFiles + nonJavaFiles;

			String header = "";
			File file = new File( "results/log_files_info.csv" );
			if(!file.exists()){
				file.createNewFile();
				header = "revision;totalFiles;nonJavaFiles;nonJavaEqualFiles;nonJavaMergedFiles;nonJavaFilesConfs;javaFiles;"
						+"javaEqualFiles;javaMergedFiles;badParsedFiles;javaFilesConfsSS;javaFilesConfsUN;mergeTime";
			}
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			try{
				if(!header.isEmpty()){
					bw.write(header+"\n");
				}
				String entry = revision+";"+totalFiles+";"+nonJavaFiles+";"+nonJavaEqualFiles+";"+nonJavaMergedFiles+";"
						+nonJavaFilesConfs+";"+javaFiles+";"+javaEqualFiles+";"+javaMergedFiles+";"
						+badParsedFiles+";"+javaFilesConfsSS+";"+javaFilesConfsUN+";"+mergeTime;
				bw.write(entry);
				bw.newLine();
				bw.close();
				fw.close();
			}catch(Exception e){
				e.printStackTrace();
				bw.close();
				fw.close();
			}


			//clear
			nonJavaMergedFiles= 0;
			nonJavaEqualFiles = 0;
			nonJavaFilesConfs = 0;
			nonJavaFiles      = 0;
			javaMergedFiles	= 0;
			javaEqualFiles	= 0;
			javaFilesConfsSS = 0;
			javaFilesConfsUN = 0;
			javaFiles		= 0;
			badParsedFiles  = 0;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
