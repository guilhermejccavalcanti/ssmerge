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
import java.util.regex.Pattern;

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

	static final String MERGE_SEPARATOR = "##FSTMerge##";
	static final String SEMANTIC_MERGE_MARKER = "~~FSTMerge~~";
	private static LinkedList<FSTNode> baseNodes = new LinkedList<FSTNode>();

	private MergeVisitor mergeVisitor = new MergeVisitor();

	//IMPORT ISSUE
	//	private int falseNegativeImports 	= 0;
	//	private int totalImports 			= 0;

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

	public void run(String[] args) {
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

		LinkedList<FSTNonTerminal> javaFiles = new LinkedList<FSTNonTerminal>();

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

					//IMPORT ISSUE
					//mergeVisitor.findJavaFilesFSTNodes(merged, javaFiles);

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

			//IMPORT ISSUE
			//countFalseNegativeImports(javaFiles);
			//printImportReport(expressionval);

			//RENAMING ISSUE
			//printRenamingReport(expressionval);
			printListOfRenamings();

			setFstnodes(AbstractFSTParser.fstnodes);

		} catch (MergeException me) {
			System.err.println(me.toString());
			me.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.lang.RuntimeException ru){
			try {
				System.err.println(ru.toString());
				FileUtils.deleteDirectory(new File(basedirval));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} finally {
			//removeBadParsedFiles(expressionval,basedirval);
		}
	}

	public static void main(String[] args) {
		try {
			//												FileInputStream stream;
			//												stream = new FileInputStream("C:\\Users\\Guilherme\\Desktop\\rename_revisions.txt");
			//												InputStreamReader reader = new InputStreamReader(stream);
			//												BufferedReader br = new BufferedReader(reader);
			//												String linha = br.readLine();
			//												while(linha != null){
			//													FSTGenMerger merger = new FSTGenMerger();
			//													String file 	= linha;	
			//													String files[] 	= {"--expression",file};
			//													merger.run(files);
			//													linha = br.readLine();
			//												}


			FSTGenMerger merger = new FSTGenMerger();
			String file 	= "C:\\Users\\Guilherme\\Desktop\\rename2\\rev.revisions";
			String files[] 	= {"--expression",file};
			merger.run(files);

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
						if (firstPass) {
							baseNodes.add(cloneB);
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

	private void removeBadParsedFiles(String expressionval, String basedirval) {
		try {

			//first, search and delete bad parsed files
			StringBuffer sb = new StringBuffer(expressionval);
			sb.setLength(sb.lastIndexOf("."));
			sb.delete(0, sb.lastIndexOf(File.separator) + 1);
			FSTGenProcessor composer = fileLoader.getComposer();
			DuplicateFreeLinkedList<File> parsedErrors = composer.getErrorFiles();

			//RENAMING ISSUE, DO NOT ACCOUNT THE RESULT FOR BAD PARSED FILES
			//			if(parsedErrors.size() != 0){
			//				mergeVisitor.getLineBasedMerger().setCountOfPossibleRenames(0);
			//			}

			for(File f : parsedErrors){
				String filePath = f.getAbsolutePath();
				String pattern = Pattern.quote(System.getProperty("file.separator"));
				String[] splittedFileName = filePath.split(pattern);
				String fileToDelete = "";
				for(int i = 0; i<splittedFileName.length;i++){
					if(splittedFileName[i].contains("rev_left") || splittedFileName[i].contains("rev_right") || splittedFileName[i].contains("rev_base")){
						splittedFileName[i] = sb.toString();
					} 
					fileToDelete = fileToDelete + splittedFileName[i] +File.separator;
				}
				String ssmergeout 	= fileToDelete.substring(0,fileToDelete.length()-1);
				String mergout 		= ssmergeout + ".merge";
				File file = new File(ssmergeout);
				if(file.exists())
					FileDeleteStrategy.FORCE.delete(file);
				file = new File(mergout);
				if(file.exists())
					FileDeleteStrategy.FORCE.delete(file);
			}

			//second, delete the output folder if it gets empty
			String outputFolder = basedirval + File.separator + sb;
			File f = new File(outputFolder);
			if(isEmpty(f))
				FileUtils.deleteDirectory(new File(basedirval));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean isEmpty(File directory){
		boolean result = true;
		if(directory.isDirectory()){
			String files[] = directory.list();
			for (String temp : files) {
				File fileDelete = new File(directory, temp);
				result = result && isEmpty(fileDelete);
			}
		}else{
			result = false;
		}
		return result;
	}

	//DIFFMERGED 
	private static void tagFSTNodeFromLeft(FSTNode childA) {
		if(childA instanceof FSTTerminal){
			if(!((FSTTerminal)childA).getPrefix().contains("CONTRIB::") && !childA.getType().equals("EmptyDecl"))
				((FSTTerminal)childA).setPrefix(((FSTTerminal)childA).getPrefix() + "CONTRIB::LEFT::");
		}
		if(childA instanceof FSTNonTerminal){
			for (FSTNode childc : ((FSTNonTerminal) childA).getChildren()) {
				if(childc.getType().equals("Id")){
					if(!((FSTTerminal)childc).getBody().contains("CONTRIB::") && !childc.getType().equals("EmptyDecl")){
						((FSTTerminal)childc).setBody("CONTRIB::LEFT::" + ((FSTTerminal)childc).getBody());
						break;
					}
				}
			}
		}
	}

	//DIFFMERGED 
	private static void tagFSTNodeFromRight(FSTNode childB) {
		if(childB instanceof FSTTerminal){
			if(!((FSTTerminal)childB).getPrefix().contains("CONTRIB::") && !childB.getType().equals("EmptyDecl"))
				((FSTTerminal)childB).setPrefix(((FSTTerminal)childB).getPrefix() + "CONTRIB::RIGHT::");
		}
		if(childB instanceof FSTNonTerminal){
			for (FSTNode childc : ((FSTNonTerminal) childB).getChildren()) {
				if(childc.getType().equals("Id")){
					if(!((FSTTerminal)childc).getBody().contains("CONTRIB::") && !childc.getType().equals("EmptyDecl")){
						((FSTTerminal)childc).setBody("CONTRIB::RIGHT::" + ((FSTTerminal)childc).getBody());
						break;
					}
				}
			}
		}
	}

	//	//IMPORT ISSUE
	//	private void countFalseNegativeImports(LinkedList<FSTNonTerminal> javaFiles) {
	//		for(FSTNonTerminal javaFile : javaFiles){
	//			LinkedList<String> importDeclarations = new LinkedList<String>();
	//			mergeVisitor.findImportDeclarations(javaFile, importDeclarations);
	//			this.totalImports+= importDeclarations.size();
	//			while(!importDeclarations.isEmpty()){
	//				String importStatementCurrent 	= importDeclarations.poll();
	//				String[] auxCurrent 			= (importStatementCurrent.replace(' ','.')).split("\\.");
	//				String elementNameCurrent 		= auxCurrent[auxCurrent.length-1];
	//				for(int i=0; i<importDeclarations.size();i++)
	//				{
	//					String importStatementIte 	= importDeclarations.get(i);
	//					String[] auxIte 			= importStatementIte.split("\\.");
	//					String elementNameIte 		= auxIte[auxIte.length-1];
	//					if(!elementNameIte.equals("*;") && !elementNameCurrent.equals("*;") ){
	//						if(elementNameIte.equals(elementNameCurrent)){
	//							this.falseNegativeImports++;
	//						}
	//					}
	//				}
	//			}
	//		}
	//	}
	//
	//	//IMPORT ISSUE
	//	private void printImportReport(String expressionval) throws IOException {
	//		File file = new File( "imports_report.txt" );
	//		FileWriter fw = new FileWriter(file, true);
	//		BufferedWriter bw = new BufferedWriter( fw );
	//		bw.write(expressionval+":"+this.totalImports +":"+this.falseNegativeImports);
	//		bw.newLine();
	//		bw.close();
	//		fw.close();
	//		this.falseNegativeImports 	= 0;
	//		this.totalImports			= 0;
	//	}
	//
	
	//RENAMING ISSUE
	private void printRenamingReport(String expressionval) throws IOException {
		File file = new File( "renaming_report.txt" );
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		bw.write(expressionval+":"+this.mergeVisitor.getLineBasedMerger().getCountOfPossibleRenames());
		bw.newLine();
		bw.close();
		fw.close();
		this.mergeVisitor.getLineBasedMerger().setCountOfPossibleRenames(0);
		this.mergeVisitor.setLineBasedMerger(null);
	}
	
	
	//RENAMING ISSUE
	private void printListOfRenamings() throws IOException {
		File file = new File( "renaming_list.txt" );
		FileWriter fw = new FileWriter(file, true);
		BufferedWriter bw = new BufferedWriter( fw );
		for(String e : this.mergeVisitor.getLineBasedMerger().listRenames){
			bw.write(e);
			bw.newLine();
		}
		bw.close();
		fw.close();
		this.mergeVisitor.setLineBasedMerger(null);
	}
}
