package printer.sdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.parsers.generated_sdf.SimplePrintVisitor;
import printer.ArtifactPrintVisitor;
import printer.PrintVisitorException;

public class SDFPrintVisitor extends ArtifactPrintVisitor {

	public SDFPrintVisitor() {
		super("SDF-File");
	}

	public void processNode(FSTNode node, File folderPath)
			throws PrintVisitorException {
		if (node instanceof FSTNonTerminal) {
			FSTNonTerminal nonterminal = (FSTNonTerminal) node;
			for (FSTNode child : nonterminal.getChildren()) {
				String fileName = folderPath.getPath() + File.separator
						+ nonterminal.getName();

				SimplePrintVisitor visitor;
				try {
					visitor = new SimplePrintVisitor(new PrintStream(fileName));
					visitor.visit((FSTNonTerminal) child);
					visitor.getResult();
				} catch (FileNotFoundException e) {
					throw new PrintVisitorException(e.getMessage());
				}
			}
		} else {
			assert (!(node instanceof FSTNonTerminal));
		}
	}
}
