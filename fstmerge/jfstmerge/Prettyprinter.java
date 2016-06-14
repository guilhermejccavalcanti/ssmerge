package jfstmerge;

import de.ovgu.cide.fstgen.ast.FSTNode;

/**
 * Class responsible for converting ASTs into source code.
 * @author Guilherme
 */
public class Prettyprinter {
	
	/**
	 * Converts a given tree into textual source code.
	 * @param tree
	 * @return textual representation of the given tree
	 */
	public String print(FSTNode tree){
		de.ovgu.cide.fstgen.parsers.generated_java18_merge.SimplePrintVisitor printer = new de.ovgu.cide.fstgen.parsers.generated_java18_merge.SimplePrintVisitor();
		tree.accept(printer);
		return printer.getResult();
	}
}
