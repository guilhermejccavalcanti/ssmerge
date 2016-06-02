package jfstmerge;

import java.io.File;
import java.io.FileInputStream;

import cide.gparser.OffsetCharStream;
import de.ovgu.cide.fstgen.ast.FSTNode;
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
	 */
	public FSTNode parse(File javaFile){
		Java18MergeParser parser = new Java18MergeParser(new OffsetCharStream(new FileInputStream(javaFile)));
		parser.CompilationUnit(false);
		return parser.getRoot();
	}
}

