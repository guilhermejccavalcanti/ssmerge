package jfstmerge;

import java.io.File;
import java.util.List;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;

/**
 * Class responsible for converting ASTs into source code and
 * responsible for operations related to printing/generating merged code.
 * @author Guilherme
 */
public final class Prettyprinter {

	/**
	 * Converts a given tree into textual source code.
	 * @param tree
	 * @return textual representation of the given tree, or empty string in case of given empty tree.
	 */
	public static String print(FSTNode tree){
		String printable = "";
		de.ovgu.cide.fstgen.parsers.generated_java18_merge.SimplePrintVisitor printer = new de.ovgu.cide.fstgen.parsers.generated_java18_merge.SimplePrintVisitor();
		FSTNode root = getCompilationUnit(tree);
		if(root != null){
			root.accept(printer);
			printable = printer.getResult();
		}
		return printable;
	}

	/**
	 * Prints the merged code result of both unstructured and semistructured merge.
	 * @param context
	 */
	public static void printOnScreenMergedCode(MergeContext context) {
		System.out.println("SEMISTRUCTURED MERGE OUTPUT:");
		System.out.println((context.semistructuredOutput.isEmpty()?"empty (deleted, inexistent or invalid merged files)\n":context.semistructuredOutput));

		System.out.println("UNSTRUCTURED MERGE OUTPUT:");
		System.out.println((context.unstructuredOutput.isEmpty()?"empty (deleted, inexistent or invalid merged files)\n":context.unstructuredOutput));
	}

	/**
	 * Prints the merged code result of both unstructured and semistructured merge in the given
	 * output file.
	 * @param context
	 * @param outputFilePath of the merged file. 
	 * @throws Exception in case cannot write output file.
	 */
	public static void generateMergedFile(MergeContext context, String outputFilePath) {
		try{
			if(outputFilePath != null){
				String semistructuredOutputFilePath 	= outputFilePath;
				String semistructuredMergeOutputContent = context.semistructuredOutput;
				boolean writeSucceed = FilesManager.writeContent(semistructuredOutputFilePath, semistructuredMergeOutputContent);
				if(writeSucceed){
					String unstructuredOutputFilePath  		= outputFilePath +".merge"; 
					String unstructuredMergeOutputContent 	= context.unstructuredOutput;
					writeSucceed = FilesManager.writeContent(unstructuredOutputFilePath, unstructuredMergeOutputContent);
				}
				if(!writeSucceed){
					throw new Exception("Unable to generate merged output file!");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/**
	 * Prints the merged code of the given file tuple into a given directory.
	 * @param outputDirPath
	 * @param tuple
	 * @throws Exception in case cannot write output file.
	 */
	public static void generateMergedTuple(String outputDirPath, FilesTuple tuple) {
		if(outputDirPath != null){
			String fileNameExample;
			if(tuple.getBaseFile()!=null){
				fileNameExample = tuple.getBaseFile().getName();
			} else if(tuple.getLeftFile() != null){
				fileNameExample = tuple.getLeftFile().getName();
			} else {
				fileNameExample =tuple.getRightFile().getName();
			}
			String outputFilePath = outputDirPath+File.separator+fileNameExample;
			generateMergedFile(tuple.getContext(), outputFilePath);
		}
	}

	/**
	 * Create files with the resulting merged code of the given merge scenario.
	 */
	public static void generateMergedScenario(MergeScenario scenario)  {
		String mergedDirectory = 	(new File(scenario.getRevisionsFilePath())).getParent() +
				File.separator +
				scenario.getLeftRevisionID()+"_"+
				scenario.getRightRevisionID();
		List<FilesTuple> tuples = scenario.getTuples();
		for(FilesTuple mergedTuple : tuples){
			generateMergedTuple(mergedDirectory, mergedTuple);
		}
	}

	/**
	 * Returns the first printable node of a AST, namely, the compilation unit.
	 * @param tree
	 * @return node representing the compilation unit, or null in case there is no compilation unit
	 */
	private static FSTNonTerminal getCompilationUnit(FSTNode tree){
		if(null != tree && tree instanceof FSTNonTerminal){
			FSTNonTerminal node = (FSTNonTerminal)tree;
			if(node.getType().equals("CompilationUnit")){
				return node;
			} else {
				return node.getChildren().isEmpty()? null : getCompilationUnit(node.getChildren().get(1));
			}
		} else {
			return null;
		}
	}
}
