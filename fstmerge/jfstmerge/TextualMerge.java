package jfstmerge;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeAlgorithm;
import org.eclipse.jgit.merge.MergeFormatter;
import org.eclipse.jgit.merge.MergeResult;

/**
 * Represents unstructured, linebased, textual merge.
 * @author Guilherme
 */
public final class TextualMerge {

	/**
	 * Three-way unstructured merge of three given files.
	 * @param left
	 * @param base
	 * @param right
	 * @param ignoreWhiteSpaces to avoid false positives conflicts due to different spacings.
	 * @return string representing merge result (might be null in case of errors).
	 */
	public static String merge(File left, File base, File right, boolean ignoreWhiteSpaces){

		/*			String mergeCommand = ""; 
			if(System.getProperty("os.name").contains("Windows")){
				mergeCommand = "C:/KDiff3/bin/diff3.exe -m -E " + "\"" 
						+ left.getPath() + "\"" + " " + "\"" 
						+ base.getPath() + "\"" + " " + "\"" 
						+ right.getPath()+ "\"";
			} else {
				mergeCommand = "git merge-file -q -p " 
						+ left.getPath() + " " 
						+ base.getPath() + " " 
						+ right.getPath();// + " > " + fileVar1.getName() + "_output";
			}
			Runtime runtime = Runtime.getRuntime();
			Process process = runtime.exec(mergeCommand);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			textualMergeResult = reader.lines().collect(Collectors.joining("\n"));*/

		String textualMergeResult = null;
		try{
			BufferedReader reader = Files.newBufferedReader(Paths.get(left.getAbsolutePath()));
			String leftContent = reader.lines().collect(Collectors.joining("\n"));

			reader = Files.newBufferedReader(Paths.get(base.getAbsolutePath()));
			String baseContent = reader.lines().collect(Collectors.joining("\n"));

			reader = Files.newBufferedReader(Paths.get(right.getAbsolutePath()));
			String rightContent = reader.lines().collect(Collectors.joining("\n"));

			textualMergeResult = merge(leftContent,baseContent,rightContent,ignoreWhiteSpaces);

		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		return textualMergeResult;
	}

	public static void merge(MergeContext threeWayContext){

	}

	/**
	 * Merges textually three strings.
	 * @param leftContent
	 * @param baseContent
	 * @param rightContent
	 * @param ignoreWhiteSpaces to avoid false positives conflicts due to different spacings.
	 * @return merged string.
	 */
	private static String merge(String leftContent, String baseContent, String rightContent, boolean ignoreWhiteSpaces){
		String textualMergeResult = null;
		try{
			RawTextComparator textComparator = ((ignoreWhiteSpaces) ? RawTextComparator.WS_IGNORE_ALL : RawTextComparator.DEFAULT);
			@SuppressWarnings("rawtypes") MergeResult mergeCommand = new MergeAlgorithm().merge(textComparator,
					new RawText(Constants.encode(baseContent)), 
					new RawText(Constants.encode(leftContent)), 
					new RawText(Constants.encode(rightContent))
					);
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			(new MergeFormatter()).formatMerge(output, mergeCommand, "BASE", "LEFT", "RIGHT", Constants.CHARACTER_ENCODING);
			textualMergeResult = new String(output.toByteArray(), Constants.CHARACTER_ENCODING);
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		return textualMergeResult;
	}

	public static void main(String[] args) {
		TextualMerge.merge(
				new File("C:\\Users\\Guilherme\\Google Drive\\Pós-Graduação\\Pesquisa\\Outros\\running_examples\\examples_nssmerge\\rev2\\left\\Test.java"), 
				new File("C:\\Users\\Guilherme\\Google Drive\\Pós-Graduação\\Pesquisa\\Outros\\running_examples\\examples_nssmerge\\rev2\\base\\Test.java"), 
				new File("C:\\Users\\Guilherme\\Google Drive\\Pós-Graduação\\Pesquisa\\Outros\\running_examples\\examples_nssmerge\\rev2\\right\\Test.java"),
				true);
	}
}
