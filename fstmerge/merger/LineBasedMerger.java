package merger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import modification.traversalLanguageParser.addressManagement.DuplicateFreeLinkedList;
import util.Util;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class LineBasedMerger implements MergerInterface {

	String encoding = "UTF-8";

	//RENAMING ISSUE
	private int countOfPossibleRenames 			= 0;
	DuplicateFreeLinkedList<File> errorFiles 	= null;
	String currentFile							= "";
	String currentRevision						= "";
	List<String> listRenames					= new ArrayList();


	static final String CONFLICT_DELIMITER 	= "<<<<<";
	static final int LEFT_CONTENT 	= 0;
	static final int BASE_CONTENT 	= 1;
	static final int RIGHT_CONTENT 	= 2;


	@SuppressWarnings("static-access")
	public void merge(FSTTerminal node) throws ContentMergeException {

		String body = node.getBody() + " ";
		String[] tokens = body.split(FSTGenMerger.MERGE_SEPARATOR);

		try {
			tokens[0] = tokens[0].replace(FSTGenMerger.SEMANTIC_MERGE_MARKER, "").trim();
			tokens[1] = tokens[1].trim();
			tokens[2] = tokens[2].trim();
		} catch (ArrayIndexOutOfBoundsException e) {
			System.err.println("|"+body+"|");
			e.printStackTrace();
		}

		//System.out.println("|" + tokens[0] + "|");
		//System.out.println("|" + tokens[1] + "|");
		//System.out.println("|" + tokens[2] + "|");
		//System.out.println("--------------------");

		// SPECIAL CONFLICT HANDLER
		if(!(node.getType().contains("-Content") ||
				node.getMergingMechanism().equals("LineBased")
				)) {
			if(tokens[0].length() == 0 && tokens[1].length() == 0 && tokens[2].length() == 0) {
				node.setBody("");
			} else if(tokens[0].equals(tokens[2])) {
				node.setBody(tokens[0]);
			} else if(tokens[0].equals(tokens[1]) && tokens[2].length() > 0) {
				node.setBody(tokens[2]);
			} else if(tokens[2].equals(tokens[1]) && tokens[0].length() > 0) {
				node.setBody(tokens[0]);
			} else if(tokens[0].equals(tokens[1]) && tokens[2].length() == 0) {
				node.setBody("");
			} else if(tokens[2].equals(tokens[1]) && tokens[0].length() == 0) {
				node.setBody("");
			}
			//System.out.println(node.getMergingMechanism());
			//System.out.println("|" + tokens[1] + "|");
			//System.out.println("|" + tokens[2] + "|");
			//System.out.println("--------------------");


			return;
		}

		try {
			long time = System.currentTimeMillis();
			File tmpDir = new File(System.getProperty("user.dir") + File.separator + "fstmerge_tmp"+time);
			tmpDir.mkdir();

			File fileVar1 = File.createTempFile("fstmerge_var1_", "", tmpDir);
			File fileBase = File.createTempFile("fstmerge_base_", "", tmpDir);
			File fileVar2 = File.createTempFile("fstmerge_var2_", "", tmpDir);

			BufferedWriter writerVar1 = new BufferedWriter(new FileWriter(fileVar1));
			if(node.getType().contains("-Content") || tokens[0].length() == 0)
				writerVar1.write(tokens[0]);
			else 
				writerVar1.write(tokens[0] + "\n");
			writerVar1.close();

			BufferedWriter writerBase = new BufferedWriter(new FileWriter(fileBase));
			if(node.getType().contains("-Content") || tokens[1].length() == 0)
				writerBase.write(tokens[1]);
			else 
				writerBase.write(tokens[1] + "\n");
			writerBase.close();

			BufferedWriter writerVar2 = new BufferedWriter(new FileWriter(fileVar2));
			if(node.getType().contains("-Content") || tokens[2].length() == 0)
				writerVar2.write(tokens[2]);
			else 
				writerVar2.write(tokens[2] + "\n");
			writerVar2.close();

			String mergeCmd = ""; 
			if(System.getProperty("os.name").contains("Windows"))
				mergeCmd = "C:/KDiff3/bin/diff3.exe -m -E " + "\"" + fileVar1.getPath() + "\"" + " " + "\"" + fileBase.getPath() + "\"" + " " + "\"" + fileVar2.getPath() + "\"";// + " > " + fileVar1.getName() + "_output";
			else
				mergeCmd = "merge -q -p " + fileVar1.getPath() + " " + fileBase.getPath() + " " + fileVar2.getPath();// + " > " + fileVar1.getName() + "_output";
			Runtime run = Runtime.getRuntime();
			Process pr = run.exec(mergeCmd);

			BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			String line = "";
			String res = "";
			while ((line=buf.readLine())!=null) {
				res += line + "\n";
			}
			pr.getInputStream().close();

			//DIFFMERGED
			//			if(!res.contains(this.CONFLICT_DELIMITER)){
			//				DiffMerged diffMerged = new DiffMerged();
			//				ArrayList<ArrayList<String>> linesContributions = diffMerged.findLinesContributions(fileVar1, fileVar2, fileBase);
			//				res = diffMerged.markLineContributions(linesContributions, res);
			//			}

			node.setBody(res);



			//RENAMING ISSUE
			//CHECKING POSSIBLE RENAMING ISSUES, ONLY IF THERE ARE CONFLICT
			if(res.contains(this.CONFLICT_DELIMITER))
				identifyAndAccountRenaming(node, tokens);


			buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			while ((line=buf.readLine())!=null) {
				System.err.println(line);
			}
			pr.getErrorStream().close();
			pr.getOutputStream().close();

			fileVar1.delete();
			fileBase.delete();
			fileVar2.delete();
			tmpDir.delete();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//RENAMING ISSUE
	private void identifyAndAccountRenaming(FSTTerminal node, String[] tokens) {
		if( 	node.getType().contains("MethodDecl")|| 
				node.getType().contains("FunctionDefinition") ||
				node.getType().contains("classsmall_stmt1") ||
				node.getType().contains("class_member_declarationEnd6")){
			if(isFileAllowed()){
				if(		(tokens[0].isEmpty() && !tokens[1].isEmpty() && !tokens[2].isEmpty()) ||
						(!tokens[0].isEmpty() && tokens[1].isEmpty() && !tokens[2].isEmpty()) ||
						(!tokens[0].isEmpty() && !tokens[1].isEmpty() && tokens[2].isEmpty())	){
					this.countOfPossibleRenames++;


					String methodSignature = this.getMethodSignature(node);
					if(!methodSignature.equals("")){
						this.listRenames.add(this.getMergedFolder()+";"+ this.getFileAbsolutePath(node)+";"+methodSignature);
					}


					//SOLVING CONFLICT FOR BUILD PURPOSES
					if(!tokens[LineBasedMerger.LEFT_CONTENT].isEmpty()){
						String methodStub = Util.generateMethodStub(tokens[LineBasedMerger.LEFT_CONTENT]);
						node.setBody(methodStub);
					}else{ 
						if (!tokens[LineBasedMerger.RIGHT_CONTENT].isEmpty()){
							String methodStub = Util.generateMethodStub(tokens[LineBasedMerger.RIGHT_CONTENT]);
							node.setBody(methodStub);
						}
					}
				} else {//SOLVING CONFLICT FOR BUILD PURPOSES
					node.setBody(Util.generateMultipleMethodBody(node.getBody()));
				}
			}
		}
	}

	//RENAMING ISSUE
	public int getCountOfPossibleRenames() {
		return countOfPossibleRenames;
	}

	//RENAMING ISSUE
	public void setCountOfPossibleRenames(int countOfPossibleRenames) {
		this.countOfPossibleRenames = countOfPossibleRenames;
	}

	//RENAMING ISSUE
	private boolean isFileAllowed(){
		for(File f: this.errorFiles){
			if(f.getName().equals(this.currentFile))
				return false;
		}
		return true;
	}

	//RENAMING ISSUE
	private String getMethodSignature(FSTTerminal node){
		String methodSignarute = "";
		if( node.getType().contains("MethodDecl")|| 
				node.getType().contains("FunctionDefinition") ||
				node.getType().contains("classsmall_stmt1") ||
				node.getType().contains("class_member_declarationEnd6")){

			methodSignarute = node.getName();
		}
		return Util.simplifyMethodSignature(methodSignarute);
	}

	//RENAMING ISSUE
	private String getFileAbsolutePath(FSTTerminal node) {
		return this.getFilePath(node)+"/"+(currentFile.split("\\."))[0];
	}
	
	//RENAMING ISSUE
	private String getFilePath(FSTNode node){
		String dir = "";
		if(node == null){
		} else 	if(node.getType().equals("Folder")){
			dir = getFilePath(node.getParent()) + "/" + node.getName();
		} else {
			dir = getFilePath(node.getParent()) + dir;
		}
		return dir;
	}
	
	//RENAMING ISSUE
	private String getMergedFolder() {
		return ((currentRevision.split("\\."))[0]).replace("\\", "/");
	}

}

