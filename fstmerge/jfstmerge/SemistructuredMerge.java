package jfstmerge;

import java.io.File;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

/**
 * Represents semistructured merge.
 * Semistrucutred merge is based on the concept of <i>superimposition</i> of ASTs. 
 * Superimposition merges trees recursively, 
 * beginning from the root, based on structural and nominal similarities.
 * @author Guilherme
 */
public final class SemistructuredMerge {

	static final String MERGE_SEPARATOR 	  = "##FSTMerge##";
	static final String SEMANTIC_MERGE_MARKER = "~~FSTMerge~~";

	/**
	 * Three-way semistructured merge of three given files.
	 * @param left
	 * @param base
	 * @param right
	 * @param context an empty MergeContext to store relevant information of the merging process.
	 * @return string representing the merge result.
	 */
	public static String merge(File left, File base, File right, MergeContext context) throws Exception {
		JParser parser = new JParser();
		FSTNode leftTree = parser.parse(left);
		FSTNode baseTree = parser.parse(base);
		FSTNode rightTree= parser.parse(right);
		context = merge(leftTree,baseTree,rightTree);
		return Prettyprinter.print(context.superImposedTree);
	}

	/**
	 * Merges the AST representation of previous given java files.
	 * @param left tree
	 * @param base tree
	 * @param right tree
	 */
	private static MergeContext merge(FSTNode left, FSTNode base, FSTNode right){
		MergeContext context = new MergeContext();
		FSTNode mergeLeftBase 	  = superimpose(left, base, null, context, true);
		FSTNode mergeLeftBaseRight= superimpose(mergeLeftBase, right, null, context, false);
		removeRemainingBaseNodes(mergeLeftBaseRight, context);
		mergeMatchedNodesContent(mergeLeftBaseRight);
		//TODO: FPFN
		context.superImposedTree = mergeLeftBaseRight;
		return context;
	}

	/**
	 * Superimposes two given ASTs. 
	 * @param nodeA representing the first tree
	 * @param nodeB representing the second tree
	 * @param parent node to be superimposed in (can be null)
	 * @param context
	 * @param isProcessingBaseTree 
	 * @return superimposed tree
	 */
	private static FSTNode superimpose(FSTNode nodeA, FSTNode nodeB, FSTNonTerminal parent, MergeContext context, boolean isProcessingBaseTree) {
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
	 * After superimposition, the content of a matched node is the content of those that originated him (left,base,right)
	 * So, this methods indicates the origin (left,base or right) in node's body content.
	 * @return node's body content marked
	 */
	private static String markNodesContributions(String bodyA, String bodyB, boolean firstPass, int indexA, int indexB) { //#PAREI AQUI, indexB � necess�rio? pra que serve index
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
	private static void removeRemainingBaseNodes(FSTNode mergedTree, MergeContext context) {
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

	/**
	 * After superimposition, the content of a matched node is the content of those that originated him (left,base,right).
	 * This method merges these parents' content. For instance, calling unstructured merge to merge methods' body.
	 * We use the tags from the method {@link #markNodesContributions(String, String, boolean, int, int)} to guide this process.
	 * @param node to be merged
	 */
	private static void mergeMatchedNodesContent(FSTNode node) {
		if(node instanceof FSTNonTerminal) {
			for(FSTNode child : ((FSTNonTerminal)node).getChildren())
				mergeMatchedNodesContent(child);
		} else if(node instanceof FSTTerminal) {
				if(((FSTTerminal)node).getBody().contains(SemistructuredMerge.MERGE_SEPARATOR)) {
					String body = ((FSTTerminal) node).getBody() + " ";
					String[] splittedBodyContent = body.split(SemistructuredMerge.MERGE_SEPARATOR);
					
					String leftContent = splittedBodyContent[0].replace(SemistructuredMerge.SEMANTIC_MERGE_MARKER, "").trim();
					String baseContent = splittedBodyContent[1].trim();
					String rightContent= splittedBodyContent[2].trim();
					
					String mergedBodyContent = TextualMerge.merge(leftContent, baseContent, rightContent, true);
					((FSTTerminal) node).setBody(mergedBodyContent);
				}
		} else {
			System.err.println("Warning: node is neither non-terminal nor terminal!");			
		}		
	}
}
