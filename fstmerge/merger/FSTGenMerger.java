package merger;

import jargs.gnu.CmdLineParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import modification.traversalLanguageParser.addressManagement.DuplicateFreeLinkedList;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;

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
import util.Util;
import builder.ArtifactBuilderInterface;
import builder.csharp.CSharpBuilder;
import builder.csharpm.CSharpMergeBuilder;
import builder.java.JavaBuilder;
import builder.javam.JavaMergeBuilder;
import builder.pythonm.PythonMergeBuilder;
import builder.textm.TextMergeBuilder;
import composer.FSTGenProcessor;
import de.ovgu.cide.fstgen.ast.AbstractFSTParser;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class FSTGenMerger extends FSTGenProcessor {

	static final String MERGE_SEPARATOR 		 = "##FSTMerge##";
	static final String SEMANTIC_MERGE_MARKER 	 = "~~FSTMerge~~";
	private static LinkedList<FSTNode> baseNodes = new LinkedList<FSTNode>();

	private MergeVisitor mergeVisitor = new MergeVisitor();

	//IMPORT ISSUE NEW
	private static LinkedList<String> importsFromBase  = new LinkedList<String>();
	private static LinkedList<String> importsFromRight = new LinkedList<String>();
	private static LinkedList<String> importsFromLeft  = new LinkedList<String>();
	private static String currentMergedClass;
	private static String currentMergedRevisionFilePath;
	private static MergeResult currentMergeResult;


	//DUPLICATED METHOD ISSUE
	//private static ArrayList<String> duplicatedMethods =  new ArrayList<String>();

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

			FPFNCandidates candidates = merger.run(files);

			Util.countConflicts(currentMergeResult);

			merger.restoreEqualFiles(currentMergedRevisionFilePath);

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

					//RENAMING ISSUE
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

			FPFNCandidates candidates = new FPFNCandidates();

			//IMPORT ISSUE NEW
			candidates.importCandidates 	= countAndPrintFalseNegativeImports(expressionval);

			//RENAMING ISSUE
			printRenamingNumbers(expressionval);
			candidates.renamingCandidates 	= printListOfRenamings();

			//DUPLICATED ISSUE
			printDuplicatedMethodsNumbers(expressionval);
			candidates.duplicatedCandidates	= printListOfDuplications();			

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
			//removeBadParsedFiles(expressionval,basedirval);
			removeBadParsedFiles();
		}
		return null;
	}

	public static void main(String[] args) {
		try {
			//			FileInputStream stream = new FileInputStream("C:\\Users\\Guilherme\\Desktop\\Itens Recentes\\import.revision");
			//			InputStreamReader reader = new InputStreamReader(stream);
			//			BufferedReader br 	= new BufferedReader(reader);
			//			String line 	 	= br.readLine();
			//			while(line != null){
			//				FSTGenMerger merger 			= new FSTGenMerger();
			//				String file 					= line;
			//				currentMergedRevisionFilePath 	= line;
			//				String files[] 	= {"--expression",file};
			//				merger.run(files);
			//				line = br.readLine();
			//			}
			//			br.close();

			FSTGenMerger merger = new FSTGenMerger();

			String revision 	= "C:\\Users\\Guilherme\\Desktop\\examplev\\rev.revisions";
			currentMergedRevisionFilePath = revision;
			String files[] 		= {"--expression",revision};

			long t0 = System.currentTimeMillis();

			merger.ignoreEqualFiles(revision);

			merger.run(files);

			Util.countConflicts(revision);

			merger.restoreEqualFiles(revision);

			long tf = System.currentTimeMillis();
			System.out.println("merge time: " + ((tf-t0)/60000) + " minutes");

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

		FSTNode mergeLeftBase = merge(tl.get(0), tl.get(1), true);
		FSTNode mergeLeftBaseRight = merge(mergeLeftBase, tl.get(2), false);
		removeLoneBaseNodes(mergeLeftBaseRight);
		return mergeLeftBaseRight;
	}

	public static FSTNode merge(FSTNode nodeA, FSTNode nodeB, boolean firstPass) {
		return merge(nodeA, nodeB, null, firstPass);
	}

	public static FSTNode merge(FSTNode nodeA, FSTNode nodeB,FSTNonTerminal compParent, boolean firstPass) {

		// System.err.println("nodeA: " + nodeA.getName() + " index: " +
		// nodeA.index);
		// System.err.println("nodeB: " + nodeB.getName() + " index: " +
		// nodeB.index);

		//IMPORT ISSUE NEW
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

						//IMPORT ISSUE NEW
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

					} else {
						if (childA.index == -1)
							childA.index = nodeA.index;
						if (childB.index == -1)
							childB.index = nodeB.index;
						nonterminalComp.addChild(merge(childA, childB, nonterminalComp, firstPass));
					}
				}
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

						//IMPORT ISSUE NEW
						//DOES THE NEW IMPORT DECLARATION CAME FROM LEFT REVISION?
						if(childA.getType().contains("ImportDeclaration")){
							String importStatement = currentMergedClass+":"+((FSTTerminal)childA).getBody();
							if(!importsFromBase.contains(importStatement) && !importsFromLeft.contains(importStatement))
								importsFromLeft.add(importStatement);
						}

					} else {
						if (!firstPass) {
							baseNodes.remove(childA);
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
		} catch (IOException e) {
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

	//IMPORT ISSUE NEW
	private ArrayList<String> countAndPrintFalseNegativeImports(String expressionval) {
		try{

			int pairOfImportedPackages 	 		= 0;
			int pairOfImportedPackageAndMember 	= 0;
			int pairOfImportedPackageAndMemberUsed 	= 0;
			int pairOfImportedSameMember 		= 0;
			int totalInsertedImports			= importsFromLeft.size() + importsFromRight.size();

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
									String issuedFile	= mergedFolder+leftImportsClass;

									rightImportStmt = (rightImportStmt.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
									String[] aux = rightImportStmt.split("\\.");
									String rightImportedMember = aux[aux.length-1];
									leftImportStmt = (leftImportStmt.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
									aux = leftImportStmt.split("\\.");
									String leftImportedMember = aux[aux.length-1];

									if(rightImportedMember.equals("*;") && leftImportedMember.equals("*;")){ 			//p.* vs q.*
										pairOfImportedPackages++;
										if(!issuedFiles.contains(issuedFile))issuedFiles.add(expressionval+";"+issuedFile);
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
										if(!issuedFiles.contains(issuedFile))issuedFiles.add(expressionval+";"+issuedFile);
									}
								}
							}
						}
					}
				}
			}

			//revision;packages;packages and members; same members
			File file = new File("results/ssmerge_import_numbers.csv");
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
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

	//IMPORT ISSUE NEW
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

			//VERIFYING IF ONE FILE USES A SAME NAMED MEMBER IMPORTED BY THE OTHER FILE
			try {
				String memberName;
				File base = new File(baseFolder+targetClass);
				if(rightImportedMember.equals("*")){
					memberName = leftImportedMember;
					File right = new File(rightFolder+targetClass);
					ArrayList<String> rightContribs = (new DiffMerged().findLinesContributionsNonNumeric(right, base, base)).get(0);
					for(String contrib : rightContribs){
						if(contrib.contains(memberName) && !contrib.contains("import")){
							result = true;
							break;
						}
					}
				} else {
					memberName = rightImportedMember;
					File left = new File(leftFolder+targetClass);
					ArrayList<String> leftContribs;
					leftContribs = (new DiffMerged().findLinesContributionsNonNumeric(left, base, base)).get(0);
					for(String contrib : leftContribs){
						if(contrib.contains(memberName) && !contrib.contains("import")){
							result = true;
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
						if(line.contains(memberName) && !line.contains("import")){
							result = true;
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

	//IMPORT ISSUE NEW
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

	//IMPORT ISSUE NEW
	private boolean isClassAllowed(String className) {
		DuplicateFreeLinkedList<File> parsedErrors = fileLoader.getComposer().getErrorFiles();
		for(File errorFile : parsedErrors){
			if(errorFile.getAbsolutePath().contains(className)){
				return false;
			}
		}
		return true;
	}

	//RENAMING ISSUE
	private void printRenamingNumbers(String expressionval) throws IOException {
		File file = new File( "results/ssmerge_renaming_numbers.csv" );
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames());
			currentMergeResult.renamingConflictsFromSsmerge = this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//RENAMING ISSUE
	private List<String> printListOfRenamings() throws IOException {
		try{
			List<String> listOfRenames = this.mergeVisitor.getLineBasedMerger().listRenames;
			File file = new File( "results/ssmerge_renaming_list.csv" );
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			for(String e : listOfRenames){
				bw.write(e);
				bw.newLine();
			}
			bw.close();
			fw.close();
			return listOfRenames;
		}catch(Exception e){}
		return new ArrayList<String>();
	}

	//DUPLICATED METHOD ISSUE
	private void printDuplicatedMethodsNumbers(String expressionval) throws IOException {
		File file = new File( "results/ssmerge_duplicated_numbers.csv" );
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		try{
			bw.write(expressionval+";"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleDuplications());
			currentMergeResult.duplicationIssuesFromSsmerge = this.mergeVisitor.getLineBasedMerger().getCountOfPossibleDuplications();
			bw.newLine();
			bw.close();
			fw.close();
		}catch(Exception e){
			bw.write(expressionval+";"+0);
			bw.newLine();
			bw.close();
			fw.close();
		}
	}

	//DUPLICATED METHOD ISSUE
	private List<String> printListOfDuplications() throws IOException {
		try{
			List<String> listOfDuplications = this.mergeVisitor.getLineBasedMerger().listDuplicatedMethods;
			File file = new File( "results/ssmerge_duplicated_list.csv" );
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			for(String e : listOfDuplications){
				bw.write(e);
				bw.newLine();
			}
			bw.close();
			fw.close();
			return listOfDuplications;
		}catch(Exception e){}
		return new ArrayList<String>();
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
						FileUtils.forceDelete(new File(leftFilePath));
						FileUtils.forceDelete(new File(rightFilePath));
						System.out.println("ignoring: " + file.getAbsolutePath());
					} else if(Util.isFilesContentEqual(leftFilePath, file.getAbsolutePath())){
						File rightFile = new File(rightFilePath);
						if(rightFile.exists()){
							String temp = rightFile.getAbsolutePath().replaceFirst(rightRevName, "_equalFiles");
							FileUtils.moveFile(rightFile, new File(temp));
						}
						FileUtils.forceDelete(new File(leftFilePath));
						FileUtils.forceDelete(new File(file.getAbsolutePath()));
						System.out.println("ignoring: " + file.getAbsolutePath());
					} else if(Util.isFilesContentEqual(rightFilePath, file.getAbsolutePath())){
						File leftFile = new File(leftFilePath);
						if(leftFile.exists()){
							String temp = leftFile.getAbsolutePath().replaceFirst(leftRevName, "_equalFiles");
							FileUtils.moveFile(leftFile, new File(temp));
						}
						FileUtils.forceDelete(new File(rightFilePath));
						FileUtils.forceDelete(new File(file.getAbsolutePath()));
						System.out.println("ignoring: " + file.getAbsolutePath());
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
}
