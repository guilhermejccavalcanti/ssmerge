package printer.text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import printer.ArtifactPrintVisitor;
import printer.PrintVisitorException;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class TextPrintVisitor extends ArtifactPrintVisitor {

	public TextPrintVisitor(String suffix) {
		super(suffix + "-File");
	}

	public void processNode(FSTNode node, File folderPath) throws PrintVisitorException {
		if(node instanceof FSTNonTerminal) {
			FSTNonTerminal nonterminal = (FSTNonTerminal)node;
			assert(nonterminal.getChildren().isEmpty());
			assert(!(nonterminal.getChildren().get(0) instanceof FSTTerminal));
			
			String content = ((FSTTerminal)nonterminal.getChildren().get(0)).getBody();
			File textFile = new File(folderPath, nonterminal.getName());
			BufferedWriter textFileWriter = null;
			try {
				textFile.createNewFile();
				textFileWriter = new BufferedWriter(new FileWriter(textFile));
				textFileWriter.write(content + "\n");
				textFileWriter.flush();
			} catch (IOException e) {
				throw new PrintVisitorException(e.getMessage());
			} finally {
				if (textFileWriter != null) {
					try {
						textFileWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			assert(!(node instanceof FSTNonTerminal));
		}
	}
}
