package jfstmerge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class responsible for performing <i>semistructured</i> merge in java files.
 * It also merges non java files, however, in these cases, traditional linebased
 * (unstructured) merge is invoked.
 * @author Guilherme
 */
public class JFSTMerge {

	/**
	 * Merges merge scenarios, indicated by .revisions files. 
	 * This is mainly used for evaluation purposes.
	 * A .revisions file contains the directories of the revisions to merge in top-down order: 
	 * first revision, base revision, second revision (three-way merge).
	 * @param revisionsPath file path
	 */
	public MergeScenario mergeRevisions(String revisionsPath){
		MergeScenario scenario = null;
		try{
			//reading the .revisions file line by line to get revisions directories
			List<String> listRevisions = new ArrayList<>();
			BufferedReader reader = Files.newBufferedReader(Paths.get(revisionsPath));
			listRevisions = reader.lines().collect(Collectors.toList());
			if(listRevisions.size()!=3) throw new Exception("Invalid .revisions file!"); //TODO testar!

			//merging the identified directories
			if(!listRevisions.isEmpty()){
				System.out.println("MERGING REVISIONS: \n" 
						+ listRevisions.get(0) + "\n"
						+ listRevisions.get(1) + "\n"
						+ listRevisions.get(2)
						);

				String revisionFileFolder = (new File(revisionsPath)).getParent();
				String leftDir  = revisionFileFolder+ File.separator+ listRevisions.get(0);
				String baseDir  = revisionFileFolder+ File.separator+ listRevisions.get(1);
				String rightDir = revisionFileFolder+ File.separator+ listRevisions.get(2);

				List<FilesTuple> mergedTuples = mergeDirectories(leftDir, baseDir, rightDir, null);

				//using the name of the revisions directories as revisions identifiers
				scenario = new MergeScenario(revisionsPath, listRevisions.get(0), listRevisions.get(1), listRevisions.get(2), mergedTuples);

				//printing the resulting merged codes
				Prettyprinter.generateMergedScenario(scenario);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return scenario;
	}

	/**
	 * Merges directories.
	 * @param leftDirPath
	 * @param baseDirPath
	 * @param rightDirPath
	 * @param outputDirPath can be null, in this case, the output will only be printed in the console.
	 * @return merged files tuples
	 */
	public List<FilesTuple> mergeDirectories(String leftDirPath, String baseDirPath, String rightDirPath, String outputDirPath){
		List<FilesTuple> filesTuple = FilesManager.fillFilesTuples(leftDirPath, baseDirPath, rightDirPath);
		for(FilesTuple tuple : filesTuple){
			File left = tuple.getLeftFile();
			File base = tuple.getBaseFile();
			File right= tuple.getRightFile();

			//merging the file tuple
			MergeContext context = mergeFiles(left, base, right, null);
			tuple.setContext(context);

			//printing the resulting merged code
			Prettyprinter.generateMergedTuple(outputDirPath, tuple);

		}
		return filesTuple;
	}

	/**
	 * Three-way semistructured merge of the given .java files.
	 * @param left version of the file, or <b>null</b> in case of intentional empty file.
	 * @param base version of the file, or <b>null</b> in case of intentional empty file.
	 * @param right version of the file, or <b>null</b> in case of intentional empty file.
	 * @param outputFilePath of the merged file. Can be <b>null</b>, in this case, the output will only be printed in the console.
	 * @return context with relevant information gathered during the merging process.
	 */
	public MergeContext mergeFiles(File left, File base, File right, String outputFilePath){
		FilesManager.validateFiles(left, base, right);
		System.out.println("MERGING FILES: \n" 
				+ ((left != null)?left.getAbsolutePath() :"<empty left>") + "\n"
				+ ((base != null)?base.getAbsolutePath() :"<empty base>") + "\n"
				+ ((right!= null)?right.getAbsolutePath():"<empty right>")
				);
		MergeContext context = new MergeContext();
		try{
			context.semistructuredOutput= SemistructuredMerge.merge(left, base, right,context);
			context.unstructuredOutput 	= TextualMerge.merge(left, base, right, false);		
		} catch(Exception e){
			//in case of any error during the merging, merge with unstructured merge
			System.err.println(e.toString());
			System.err.println( "Error while merging.\n" + "Fallback merge strategy: call textual merge\n");

			//System.exit(-1); //TODO REMOVER DEPOIS DE TESTAR

			context.unstructuredOutput  = TextualMerge.merge(left, base, right, false);
			context.semistructuredOutput= TextualMerge.merge(left, base, right, false);
		}

		//printing the resulting merged code
		Prettyprinter.printOnScreenMergedCode(context);
		Prettyprinter.generateMergedFile(context, outputFilePath);
		System.out.println("Merge files finished.");

		return context;
	}

	public static void main(String[] args) {
		try {
			PrintStream pp = new PrintStream(new File("output-file.txt"));
			System.setOut(pp);
			System.setErr(pp);
		} catch (Exception e) {
			e.printStackTrace();
		}

		//		new JFSTMerge().mergeFiles(
		//				new File("C:\\Users\\Guilherme\\Desktop\\test\\left\\Teste.java"), 
		//				new File("C:\\Users\\Guilherme\\Desktop\\test\\base\\Teste.java"), 
		//				null,  
		//				"C:\\Users\\Guilherme\\Desktop\\test\\Test.java");

		//		new JFSTMerge().mergeFiles(
		//				new File("C:\\Users\\Guilherme\\Google Drive\\P�s-Gradua��o\\Pesquisa\\Outros\\running_examples\\exemplos diff3\\voldemort\\left\\Repartitioner.java"), 
		//				new File("C:\\Users\\Guilherme\\Google Drive\\P�s-Gradua��o\\Pesquisa\\Outros\\running_examples\\exemplos diff3\\voldemort\\base\\Repartitioner.java"), 
		//				new File("C:\\Users\\Guilherme\\Google Drive\\P�s-Gradua��o\\Pesquisa\\Outros\\running_examples\\exemplos diff3\\voldemort\\right\\Repartitioner.java"), 
		//				"C:\\Users\\Guilherme\\Desktop\\test\\Test.java");


		//new JFSTMerge().mergeRevisions("C:\\tstfstmerge\\java_lucenesolr\\rev_dc62b_aff97\\rev_dc62b-aff97.revisions");

		//TODO
		//C:\\tstfstmerge\\java_retrofit\\rev_941ae_2ef7c\\rev_left_941ae\\retrofit\\src\\main\\java\\retrofit\\http\\Header.java
		//C:\\tstfstmerge\\java_retrofit\\rev_941ae_2ef7c\\rev_941ae-2ef7c.revisions

		try {
			List<String> listRevisions = new ArrayList<>();
			BufferedReader reader;
			reader = Files.newBufferedReader(Paths.get("C:\\tstfstmerge\\all.revisions"));
			listRevisions = reader.lines().collect(Collectors.toList());
			long t0 = System.currentTimeMillis();
			for(String r : listRevisions){
				new JFSTMerge().mergeRevisions(r);
			}
			long tf = System.currentTimeMillis();
			System.out.println((tf - t0)/1000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}