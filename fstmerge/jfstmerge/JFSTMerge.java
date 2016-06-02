package jfstmerge;

import java.io.File;

import de.ovgu.cide.fstgen.AST.FSTNode;

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
	 * @param revisions file
	 */
	public void mergeRevisions(File revisions){
		
	}
	
	/**
	 * Merges directories.
	 * @param leftDir
	 * @param baseDir
	 * @param rightDir
	 */
	public void mergeDirectories(File leftDir, File baseDir, File rightDir){
		
	}

	/**
	 * Three-way semistructured merge of the given .java files.
	 * @param left version of the file
	 * @param base version of the file
	 * @param right version of the file
	 */
	public void mergeFiles(File left, File base, File right){
		JParser parser = new JParser();
		FSTNode leftTree = parser.parse(left);
		FSTNode baseTree = parser.parse(base);
		FSTNode rightTree = parser.parse(right);
		merge(leftTree,baseTree,rightTree);
	}
	
	/**
	 * Merges the AST representation of previous given java files.
	 * @param left tree
	 * @param base tree
	 * @param right tree
	 */
	private void merge(FSTNode left, FSTNode base, FSTNode right){
		FSTNode mergeLeftBase = superimpose(left, base, true);
		FSTNode mergeLeftBaseRight = superimpose(mergeLeftBase, right, false);
		
	}

	/**
	 * Semistrucutred merge is based on the concept of <i>superimposition</i> of ASTs. 
	 * So, this method superimpose two given ASTs. Superimposition merges trees recursively, 
	 * beginning from the root, based on structural and nominal similarities.
	 * @param firstTree 
	 * @param secondTree
	 * @param isProcessingBaseTree flag for three-way merge purpose
	 * @return superimposed tree
	 */
	private FSTNode superimpose(FSTNode firstTree, FSTNode secondTree, boolean isProcessingBaseTree) {
		// TODO Auto-generated method stub
		return null;
	}
}
