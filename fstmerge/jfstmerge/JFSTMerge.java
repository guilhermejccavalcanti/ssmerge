package jfstmerge;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

/**
 * Class responsible for performing <i>semistructured</i> merge in java files.
 * It also merges non java files, however, in these cases, traditional linebased
 * (unstructured) merge is invoked.
 * @author Guilherme
 */
public class JFSTMerge {

	static final String MERGE_SEPARATOR = "##FSTMerge##";
	static final String SEMANTIC_MERGE_MARKER = "~~FSTMerge~~";

	/**
	 * Merges merge scenarios, indicated by .revisions files. 
	 * This is mainly used for evaluation purposes.
	 * A .revisions file contains the directories of the revisions to merge in top-down order: 
	 * first revision, base revision, second revision (three-way merge).
	 * @param revisionsPath file path
	 */
	public MergeScenario mergeRevisions(String revisionsPath){
		MergeScenario scenario = null;

		//reading the .revisions file line by line to get revisions directories
		List<String> listRevisions = new ArrayList<>();
		try{
			BufferedReader reader = Files.newBufferedReader(Paths.get(revisionsPath));
			listRevisions = reader.lines().collect(Collectors.toList());
		} catch(Exception e){
			e.printStackTrace();
		}

		//merging the identified directories
		if(!listRevisions.isEmpty()){
			String revisionFileFolder = (new File(revisionsPath)).getParent();
			String leftDir  = revisionFileFolder+ File.separator+ listRevisions.get(0);
			String baseDir  = revisionFileFolder+ File.separator+ listRevisions.get(1);
			String rightDir = revisionFileFolder+ File.separator+ listRevisions.get(2);

			List<FilesTuple> mergedTuples = mergeDirectories(leftDir, baseDir, rightDir);

			//using the name of the revisions directories as revisions identifiers
			scenario = new MergeScenario(revisionsPath, listRevisions.get(0), listRevisions.get(1), listRevisions.get(2), mergedTuples);
		}
		return scenario;
	}

	
	/**
	 * Merges directories.
	 * @param leftDir
	 * @param baseDir
	 * @param rightDir
	 * @return merged files tuples
	 */
	public List<FilesTuple> mergeDirectories(String leftDir, String baseDir, String rightDir){
		List<FilesTuple> filesTuple = FilesManager.fillFilesTuples(leftDir, baseDir, rightDir);
		for(FilesTuple tuple : filesTuple){
			File left = tuple.getLeftFile();
			File base = tuple.getBaseFile();
			File right= tuple.getRightFile();

			MergeContext context = mergeFiles(left, base, right);
			tuple.setContext(context);
		}
		return filesTuple;
	}
	

	/**
	 * Three-way semistructured merge of the given .java files.
	 * @param left version of the file
	 * @param base version of the file
	 * @param right version of the file
	 * @return context with relevant information gathered during the merging process.
	 */
	public MergeContext mergeFiles(File left, File base, File right){
		MergeContext context = new MergeContext();
		try{
			JParser parser = new JParser();
			FSTNode leftTree = parser.parse(left);
			FSTNode baseTree = parser.parse(base);
			FSTNode rightTree = parser.parse(right);
			context = merge(leftTree,baseTree,rightTree);
			//TODO postmerge, textualmerge, prettyprinter
		} catch(Exception e){
			//in case of any error merge with unstructured merge
			context.unstructuredOutput = TextualMerge.merge(left, base, right, true);
			context.semistructuredOutput = TextualMerge.merge(left, base, right,false);
		}
		return context;
	}

	/**
	 * Merges the AST representation of previous given java files.
	 * @param left tree
	 * @param base tree
	 * @param right tree
	 */
	private MergeContext merge(FSTNode left, FSTNode base, FSTNode right){
		MergeContext context = new MergeContext();
		FSTNode mergeLeftBase = superimpose(left, base, null, context, true);
		FSTNode mergeLeftBaseRight = superimpose(mergeLeftBase, right, null, context, false);
		removeRemainingBaseNodes(mergeLeftBaseRight, context);
		context.superImposedTree = mergeLeftBaseRight;
		return context;
	}

	/**
	 * Semistrucutred merge is based on the concept of <i>superimposition</i> of ASTs. 
	 * So, this method superimposes two given ASTs. Superimposition merges trees recursively, 
	 * beginning from the root, based on structural and nominal similarities.
	 * @param nodeA representing the first tree
	 * @param nodeB representing the second tree
	 * @param parent node to be superimposed in (can be null)
	 * @param context
	 * @param isProcessingBaseTree 
	 * @return superimposed tree
	 */
	private FSTNode superimpose(FSTNode nodeA, FSTNode nodeB, FSTNonTerminal parent, MergeContext context, boolean isProcessingBaseTree) {
		if (nodeA.compatibleWith(nodeB)) {
			FSTNode composed = nodeA.getShallowClone();
			composed.index = nodeB.index;
			composed.setParent(parent);

			if (nodeA instanceof FSTNonTerminal	&& nodeB instanceof FSTNonTerminal) {
				FSTNonTerminal nonterminalA = (FSTNonTerminal) nodeA;
				FSTNonTerminal nonterminalB = (FSTNonTerminal) nodeB;
				FSTNonTerminal nonterminalComposed = (FSTNonTerminal) composed;

				for (FSTNode childB : nonterminalB.getChildren()) { //nodes from base or right
					FSTNode childA = nonterminalA.getCompatibleChild(childB);
					if (childA == null) { //means that a base node was deleted by left, or that a right node was added
						FSTNode cloneB = childB.getDeepClone();
						if( childB.index == -1)
							childB.index = nodeB.index;
						cloneB.index = childB.index;
						nonterminalComposed.addChild(cloneB);//cloneB must be removed afterwards if it is a base node 
						if(isProcessingBaseTree) { 
							context.deletedBaseNodes.add(cloneB); //base nodes deleted by left
						} else {
							context.nodesAddedByRight.add(cloneB); //nodes added by right
						}
					} else {
						if( childA.index == -1)
							childA.index = nodeA.index;
						if( childB.index == -1)
							childB.index = nodeB.index;
						nonterminalComposed.addChild(superimpose(childA, childB, nonterminalComposed, context, isProcessingBaseTree));
					}
				}
				for (FSTNode childA : nonterminalA.getChildren()) { //nodes from left, leftBase
					FSTNode childB = nonterminalB.getCompatibleChild(childA);
					if (childB == null) { //is a new node from left, or a deleted base node in right
						FSTNode cloneA = childA.getDeepClone();
						if(childA.index == -1)
							childA.index = nodeA.index;
						cloneA.index = childA.index;
						nonterminalComposed.addChild(cloneA);
						if( context.deletedBaseNodes.contains(childA)) { //this is only possible when processing right nodes because this is a base node not present either in left and right
							context.deletedBaseNodes.remove(childA);
							context.deletedBaseNodes.add(cloneA);
						} else {
							context.nodesAddedByLeft.add(cloneA); //node added by left
						}
					} else { 
						if(!isProcessingBaseTree) {
							context.deletedBaseNodes.remove(childA); //node common to right and base but not to left
						}
					}
				}
				return nonterminalComposed;

			} else if (nodeA instanceof FSTTerminal && nodeB instanceof FSTTerminal && parent instanceof FSTNonTerminal) {
				FSTTerminal terminalA = (FSTTerminal) nodeA;
				FSTTerminal terminalB = (FSTTerminal) nodeB;
				FSTTerminal terminalComposed = (FSTTerminal) composed;

				if (!terminalA.getMergingMechanism().equals("Default")) {
					terminalComposed.setBody(markNodesContributions(terminalA.getBody(), terminalB.getBody(), isProcessingBaseTree, terminalA.index, terminalB.index));
				} 
				return terminalComposed;
			}
			return null;
		} else
			return null;
	}
	
	
	/**
	 * After superimposition, the content of a matched node is the content of who originated  him (left,base,right)
	 * So, this methods indicates the origin (left,base or right) in node's body content.
	 * @return node's body content marked
	 */
	private String markNodesContributions(String bodyA, String bodyB, boolean firstPass, int indexA, int indexB) { //#PAREI AQUI, indexB é necessário? pra que serve index
		if (bodyA.contains(SEMANTIC_MERGE_MARKER)) {
			return 	bodyA
					+ " "
					+ bodyB;
		}
		else {
			if(firstPass) {
				return  SEMANTIC_MERGE_MARKER 
						+ " " 
						+ bodyA 
						+ " " 
						+ MERGE_SEPARATOR 
						+ " " 
						+ bodyB 
						+ " " 
						+ MERGE_SEPARATOR;
			} else {
				if(indexA == 0){
					return  SEMANTIC_MERGE_MARKER 
							+ " " 
							+ bodyA 
							+ " "
							+ MERGE_SEPARATOR
							+ " "
							+ MERGE_SEPARATOR
							+ " "
							+ bodyB;
				} else {
					return  SEMANTIC_MERGE_MARKER
							+ " "
							+ MERGE_SEPARATOR
							+ " "
							+ bodyA
							+ " "
							+ MERGE_SEPARATOR
							+ " "
							+ bodyB;
				}	
			}
		}
	}
	
	/**
	 * After superimposition, base nodes supposed to be removed might remain.
	 * This method removes these nodes from the merged tree. 
	 * @param mergedTree
	 * @param context
	 */
	private void removeRemainingBaseNodes(FSTNode mergedTree, MergeContext context) {
		boolean removed = false;
		for(FSTNode loneBaseNode : context.deletedBaseNodes) {
			if(mergedTree == loneBaseNode) {
				FSTNonTerminal parent = (FSTNonTerminal)mergedTree.getParent();
				if(parent != null) {
					parent.removeChild(mergedTree);
					removed = true;
				}
			}
		}
		if(!removed && mergedTree instanceof FSTNonTerminal) {
			Object[] children = ((FSTNonTerminal)mergedTree).getChildren().toArray();
			for(Object child : children) {
				removeRemainingBaseNodes((FSTNode)child, context);
			}
		}
	}
}