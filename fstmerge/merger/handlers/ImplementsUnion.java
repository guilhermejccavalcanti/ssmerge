package merger.handlers;

import java.util.LinkedHashSet;
import java.util.StringTokenizer;

import de.ovgu.cide.fstgen.ast.FSTTerminal;

public class ImplementsUnion {
	public final static String COMPOSITION_RULE_NAME = "ImplementsUnion";
	
	public void compose(String bodyLeft, String bodyBase, String bodyRight, FSTTerminal terminalComp) {
		System.out.println("HANDLING with " + COMPOSITION_RULE_NAME + ": " + terminalComp.getBody());
		
		String interfaceListA 	= bodyLeft.replaceFirst("implements", ", ");
		String interfaceListB 	= bodyRight.replaceFirst("implements", ", ");
		String concatenatedList = interfaceListB + interfaceListA;
		concatenatedList 		= concatenatedList.replaceAll("\\s*", "");
		StringTokenizer st 		= new StringTokenizer(concatenatedList, ",");
		
		LinkedHashSet<String> interfaceSet = new LinkedHashSet<String>(); 
	    while (st.hasMoreTokens()) {
	    	interfaceSet.add(st.nextToken());
	    }
	    
	    String removedDuplicates 	= new String();
	    String[] interfaceArray 	= new String[interfaceSet.size()];
	    interfaceSet.toArray(interfaceArray);
		
	    for(int i = 0; i < interfaceArray.length - 1; i++)
			removedDuplicates += interfaceArray[i] + ", ";
		
	    removedDuplicates += interfaceArray[interfaceArray.length - 1];
		terminalComp.setBody("implements " + removedDuplicates);
	}
}
