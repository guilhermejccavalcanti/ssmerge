package jfstmerge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.UUID;

import cide.gparser.OffsetCharStream;
import cide.gparser.ParseException;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.parsers.generated_java18_merge.Java18MergeParser;

/**
 * Class responsible for parsing java files, based on a 
 * <i>featurebnf</i> Java 1.8 annotated grammar: 
 * {@link http://tinyurl.com/java18featurebnf}
 * For more information, see the documents in <i>guides</i> package.
 * @author Guilherme
 */
public class JParser {

	/**
	 * Parses a given .java file
	 * @param javaFile
	 * @return ast representing the java file
	 * @throws ParseException 
	 * @throws FileNotFoundException 
	 */
	public FSTNode parse(File javaFile) throws ParseException, FileNotFoundException{
		FSTNonTerminal generatedAst = new FSTNonTerminal("Entity", "ID_" + UUID.randomUUID().toString().replaceAll("-", ""));
		if(javaFile != null){
			Java18MergeParser parser = new Java18MergeParser(new OffsetCharStream(new FileInputStream(javaFile)));
			parser.CompilationUnit(false);
			generatedAst.addChild(new FSTNonTerminal("Java-File", javaFile.getName()));
			generatedAst.addChild(parser.getRoot());
		} 
		return generatedAst;
	}

	/*	public static void main(String[] args) {
		new JParser().parse(new File("C:\\GGTS\\workspace\\ssmerge\\fstgen\\test\\Test.java"));
	}*/
}