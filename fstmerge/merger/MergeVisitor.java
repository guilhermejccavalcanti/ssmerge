package merger;

import java.io.File;
import java.util.LinkedList;

import modification.traversalLanguageParser.addressManagement.DuplicateFreeLinkedList;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class MergeVisitor {

	private LinkedList<MergerInterface> mergerList = new LinkedList<MergerInterface>();

	//RENAMING ISSUE
	private LineBasedMerger lineBasedMerger = null;
	private DuplicateFreeLinkedList<File> errorFiles = null;
	private String currentRevision = "";

	public void registerMerger(MergerInterface merger) {
		mergerList.add(merger);
	}

	private LinkedList<MergerInterface> getMergerList() {
		return mergerList;
	}

	public void visit(FSTNode current) {
		if(current instanceof FSTNonTerminal) {
			for(FSTNode child : ((FSTNonTerminal)current).getChildren())
				visit(child);
		} else if(current instanceof FSTTerminal) {
			for(MergerInterface merger : getMergerList()) {
				try {
					if(((FSTTerminal)current).getBody().contains(FSTGenMerger.MERGE_SEPARATOR)) {
						//RENAMING ISSUE
						if((lineBasedMerger == null) && (merger instanceof LineBasedMerger)){
							this.lineBasedMerger =(LineBasedMerger) merger;
							((LineBasedMerger)merger).errorFiles = this.errorFiles;		
							((LineBasedMerger)merger).currentRevision = this.currentRevision;
						}
						String fileName= getFileName((FSTTerminal)current);
						this.lineBasedMerger.currentFile = fileName;


						merger.merge((FSTTerminal)current);
					}
				} catch (ContentMergeException e) {
					System.err.println(e.toString());
				} 
			}

		} else {
			System.err.println("MergerVisitor: node is neither non-terminal nor terminal!");			
		}
	}

	//	//IMPORT ISSUE
	//	public void findJavaFilesFSTNodes(FSTNode current, LinkedList<FSTNonTerminal> javaFiles) {
	//		if(current instanceof FSTNonTerminal) {
	//			if("Java-File".equals(current.getType())){
	//				javaFiles.add((FSTNonTerminal)current);
	//			}
	//			for(FSTNode child : ((FSTNonTerminal)current).getChildren())
	//				findJavaFilesFSTNodes(child,javaFiles);
	//		} 
	//	}
	//
	//	//IMPORT ISSUE
	//	public void findImportDeclarations(FSTNode javaFile, LinkedList<String> importDeclarations) {
	//		if(javaFile instanceof FSTNonTerminal) {
	//			for(FSTNode child : ((FSTNonTerminal)javaFile).getChildren())
	//				findImportDeclarations(child,importDeclarations);
	//		} else if(javaFile instanceof FSTTerminal) {
	//			if("ImportDeclaration".equals(javaFile.getType())){
	//				importDeclarations.add(((FSTTerminal) javaFile).getBody());
	//			}
	//		}
	//	}
	//
	
	
	//RENAMING ISSUE
	public LineBasedMerger getLineBasedMerger() {
		return lineBasedMerger;
	}

	//RENAMING ISSUE
	public void setLineBasedMerger(LineBasedMerger lineBasedMerger) {
		this.lineBasedMerger = lineBasedMerger;
	}

	//RENAMING ISSUE
	public void setErrorFiles(DuplicateFreeLinkedList<File> errorFiles) {
		this.errorFiles = errorFiles;
	}

	//RENAMING ISSUE
	private String getFileName(FSTNode current) {
		String fileName = "";
		if(!(current instanceof FSTNonTerminal)) {
			fileName = getFileName(current.getParent());
		} else {
			if(current.getType().contains("File")){
				fileName = current.getName();
			} else {
				fileName = getFileName(current.getParent());
			}
		}
		return fileName;
	}
	
	//RENAMING ISSUE
	public void setCurrentRevision (String revision){
		this.currentRevision = revision;
	}
}
