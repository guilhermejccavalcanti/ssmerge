package util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.commons.io.FilenameUtils;

public class Util {

	public static String generateMethodStub(String methodSourceCode) {
		String methodSignature = getMethodSignature(methodSourceCode);
		String returnType = getMethodReturnType(methodSignature);
		String returnStmt = "";

		if (returnType.equalsIgnoreCase("void")) {
		} else {
			returnStmt = "return " + getPrimitiveTypeDefaultValue(returnType)
					+ ";";
		}
		String methodStub = methodSignature + "\n" + returnStmt + "\n}";
		return methodStub;
	}

	public static String generateMultipleMethodBody(String left, String base, String right) {
		String newBody = "";
		String tempVar = "var"+System.currentTimeMillis();
		newBody += getMethodSignature(base);
		newBody += "\n";
		newBody += "int "+ tempVar + " = 0;\nif("+tempVar+"=="+tempVar+"){\n";
		newBody += getMethodBody(left);
		newBody += "}else{\n";
		newBody += getMethodBody(right);
		newBody += "}\n}";
		return newBody;


		//		try {
		//			BufferedReader buffer 	= new BufferedReader(new StringReader(oldBody));
		//			String currentLine 		= "";
		//			while ((currentLine=buffer.readLine())!=null) {
		//				if(currentLine.contains("<<<<<<<") && !currentLine.contains("//") && !currentLine.contains("/*")){
		//					String tempVar = "var"+System.currentTimeMillis();
		//					currentLine = "int "+ tempVar + " = 0;\nif("+tempVar+"=="+tempVar+"){";
		//				} else if(currentLine.equals("=======")){
		//					currentLine = "}else{";
		//				} else if(currentLine.contains(">>>>>>>") && !currentLine.contains("//") && !currentLine.contains("/*")){
		//					currentLine ="}";
		//				}
		//				newBody+=currentLine+"\n";
		//			}
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
	}

	public static void generateJarFile(String srcDirectory){
		try{
			Manifest manifest = new Manifest();
			manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION,"1.0");
			JarOutputStream target = new JarOutputStream(new FileOutputStream("output.jar"), manifest);
			addFilesToJar(new File(srcDirectory), target);
			target.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static String simplifyMethodSignature(String signature){
		String simplifiedMethodSignature = "";
		String firstPart = "";
		String parameters= "";
		String lastPart  = "";
		String aux 		 = "";

		signature = signature.replaceAll("\\s+","");

		for (int i = 0, n = signature.length(); i < n; i++) {
			char chr = signature.charAt(i);
			if (chr == '('){
				aux = signature.substring(i+1,signature.length());
				firstPart+="(";
				break;
			}else
				firstPart += chr;
		}
		for (int i = 0, n = aux.length(); i < n; i++) {
			char chr = aux.charAt(i);
			if (chr == ')'){
				lastPart = aux.substring(i,aux.length());
				break;
			}else
				parameters += chr;
		}

		simplifiedMethodSignature = firstPart + normalizeParameters(parameters) + lastPart;
		simplifiedMethodSignature = simplifiedMethodSignature.replace("{FormalParametersInternal}", "");
		return simplifiedMethodSignature;
	}

	public static String getMethodBody(String methodSource){
		String methodBody = "";
		String aux 		 = "";
		for (int i = 0, n = methodSource.length(); i < n; i++) {
			char chr = methodSource.charAt(i);
			if (chr == '{'){
				aux = methodSource.substring(i+1,methodSource.length());
				break;
			}
		}
		int ind = aux.lastIndexOf("}");
		methodBody = new StringBuilder(aux).replace(ind, ind+1,"").toString();
		return methodBody;
	}

	public static boolean isFilesContentEqual(String filePathLeft, String filePathBase, String filePathRight ){
		String leftContent 	= getFileContents(filePathLeft);
		String baseContent 	= getFileContents(filePathBase);
		String rightContent = getFileContents(filePathRight);

		leftContent = (leftContent.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
		baseContent = (baseContent.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");
		rightContent = (rightContent.replaceAll("\\r\\n|\\r|\\n","")).replaceAll("\\s+","");

		return (leftContent.equals(baseContent) && rightContent.equals(baseContent));
	}

	public static void countConflicts(String revision) {
		File revFile 		= new File(revision);
		if(revFile.exists()){
			String mergedFolder = revFile.getParentFile() + File.separator + (revFile.getName().split("\\.")[0]);
			File root 	 		= new File(mergedFolder);
			MergeResult mergeResult= new MergeResult();
			countConflicts(root,mergeResult);
			printConflictsReport(revision,mergeResult);
		}
	}

	private static String getFileContents(String filePath){
		String content = "";
		try {
			StringBuilder fileData = new StringBuilder(1000);
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[10];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
			content = fileData.toString();	
		} catch (IOException e) {return content;} 
		return content;
	}

	private static String normalizeParameters(String parameters){
		String normalizedParameters = "";
		String[] strs = parameters.split("-");
		for(int i = 0; i < strs.length; i++){
			if(i % 2 == 0){
				normalizedParameters+=(strs[i]+",");
			}
		}
		normalizedParameters = (normalizedParameters.substring(0,normalizedParameters.length()-1)) + "";
		return normalizedParameters;
	}

	private static void addFilesToJar(File source, JarOutputStream target) throws IOException {
		BufferedInputStream in = null;
		try {
			if (source.isDirectory()) {
				String name = source.getPath().replace("\\", "/");
				if (!name.isEmpty()) {
					if (!name.endsWith("/"))
						name += "/";
					JarEntry entry = new JarEntry(name);
					entry.setTime(source.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : source.listFiles())
					addFilesToJar(nestedFile, target);
				return;
			}

			JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
			entry.setTime(source.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(source));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1)
					break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null)
				in.close();
		}
	}

	private static String getMethodSignature(String methodSourceCode) {
		String methodSignature = "";
		for (int i = 0, n = methodSourceCode.length(); i < n; i++) {
			char chr = methodSourceCode.charAt(i);
			methodSignature += chr;
			if (chr == '{')
				break;
		}
		return methodSignature;
	}

	private static String getMethodReturnType(String methodSignature) {
		String[] strs = methodSignature.split("['\\s]");
		String returnType = "";
		if (hasAccessModifier(strs[0])) {
			if (isStatic(strs[1])) {
				if (!isGeneric(strs[2])) {
					returnType = strs[2];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			} else {
				if (!isGeneric(strs[1])) {
					returnType = strs[1];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			}
		} else {
			if (isStatic(strs[0])) {
				if (!isGeneric(strs[1])) {
					returnType = strs[1];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			} else {
				if (!isGeneric(strs[0])) {
					returnType = strs[0];
				} else {
					returnType = getMethodReturnType(removeGenerics(methodSignature));
				}
			}
		}
		return returnType;
	}

	private static String getPrimitiveTypeDefaultValue(String returnType) {
		String defaultValue = "";
		if (returnType.equalsIgnoreCase("int")
				|| returnType.equalsIgnoreCase("byte")
				|| returnType.equalsIgnoreCase("short")
				|| returnType.equalsIgnoreCase("long")
				|| returnType.equalsIgnoreCase("float")
				|| returnType.equalsIgnoreCase("double")) {
			defaultValue = "0";
		} else if (returnType.equalsIgnoreCase("char")) {
			defaultValue = "'\u0000'";
		} else if (returnType.equalsIgnoreCase("boolean")) {
			defaultValue = "false";
		} else {
			defaultValue = "null";
		}
		return defaultValue;
	}

	private static String removeGenerics(String methodSignature) {
		String res = "";
		int count = -1;
		boolean first = true;
		boolean canTake = false;
		for (int i = 0, n = methodSignature.length(); i < n; i++) {
			char chr = methodSignature.charAt(i);
			if (chr == '<') {
				if (first) {
					first = false;
					count = 1;
				} else {
					count++;
				}
			} else if (chr == '>') {
				count--;
			}
			if (canTake) {
				res += chr;
			}
			if (count == 0) {
				canTake = true;
			}
		}
		return res.replaceFirst(" ", "");
	}

	private static boolean isGeneric(String str) {
		return str.startsWith("<");
	}

	private static boolean isStatic(String str) {
		return str.equalsIgnoreCase("static");
	}

	private static boolean hasAccessModifier(String str) {
		return str.equalsIgnoreCase("private")
				|| str.equalsIgnoreCase("public")
				|| str.equalsIgnoreCase("protected");
	}

	private static void countConflicts(File folder, MergeResult mergeResult){
		File[] fList = folder.listFiles();
		for (File f : fList){
			if (f.isDirectory()){
				countConflicts(f, mergeResult);
			} else {
				if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java") ||
						FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")){
					countHeaders(f,mergeResult);
				} 
			}
		}
	}

	private static void countHeaders(File f, MergeResult mergeResult) {
		try {
			String CONFLICT_HEADER_BEGIN= "<<<<<<<";
			String CONFLICT_HEADER_END 	= ">>>>>>>";
			String SEMANTIC_HEADER 		= "~~FSTMerge~~";

			boolean isConflictOpen		= false;
			boolean isConflictingFile	= false;

			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(f.getAbsolutePath())));   
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if(line.contains(CONFLICT_HEADER_END)) {
					isConflictOpen	= false;
				}

				if(isConflictOpen){
					if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
						mergeResult.ssmergeLOC++;
					} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
						mergeResult.linebasedLOC++;
					}
				}

				if(line.contains(CONFLICT_HEADER_BEGIN)){
					isConflictingFile = true;
					isConflictOpen = true;
					if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
						mergeResult.ssmergeConfs++;
					} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
						mergeResult.linedbasedConfs++;
					}
				} else if(line.contains(SEMANTIC_HEADER)) {
					mergeResult.semanticConfs++;
				}
			}
			if(isConflictingFile){
				if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("java")){
					mergeResult.ssmergeFiles++;
				} else if(FilenameUtils.getExtension(f.getAbsolutePath()).equalsIgnoreCase("merge")) {
					mergeResult.linebasedFiles++;
				}
			}
			
			bufferedReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printConflictsReport(String revision, MergeResult mergeResult) {
		try {
			File file = new File("ssmerge_conflicts_report.csv" );
			FileWriter fw;
			fw = new FileWriter(file, true);
			BufferedWriter bw = new BufferedWriter( fw );
			bw.write(revision+";"+mergeResult.ssmergeConfs+";"+mergeResult.linedbasedConfs+";" +mergeResult.ssmergeLOC+";"+mergeResult.linebasedLOC+";"+mergeResult.ssmergeFiles+";"+mergeResult.linebasedFiles+";"+mergeResult.semanticConfs);
			bw.newLine();
			bw.close();
			fw.close();		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static class MergeResult {
		int ssmergeConfs 	= 0;
		int ssmergeLOC		= 0;
		int ssmergeFiles	= 0;

		int linedbasedConfs = 0;
		int linebasedLOC	= 0;
		int linebasedFiles	= 0;

		int semanticConfs	= 0;

	}

	public static void main(String[] args) {

		countConflicts("C:\\Users\\Guilherme\\Desktop\\rename2\\rev.revisions");
		countConflicts("C:\\Users\\Guilherme\\Desktop\\rename2\\rev.revisions");

		isFilesContentEqual("C:\\Users\\Guilherme\\Desktop\\n\\MemoryUtils.java", "C:\\Users\\Guilherme\\Desktop\\n\\MemoryUtils1.java", "C:\\Users\\Guilherme\\Desktop\\n\\MemoryUtils2.java");

		getMethodBody("public int soma(int a, int b, int c){   int result = a + b + c;   return result;  }");

		simplifyMethodSignature("soma(int-int-int-int) throws Execeptionption");

		// String str ="public int soma(int a, int c) throws Exception {";
		String str = "<T, S extends T> int copy(List<T> dest, List<S> src) {";
		// PEGANDO A ASSINATURA DO MÉTODO
		String methodSignature = getMethodSignature(str);
		String returnType = getMethodReturnType(methodSignature);
		System.out.println(returnType);
		System.out.println(methodSignature);
		System.out.println(removeGenerics(methodSignature));
		System.out.println(generateMethodStub(str));

		generateJarFile("C:\\GGTS\\workspace\\ASM_TesteCaller");

	}
}
