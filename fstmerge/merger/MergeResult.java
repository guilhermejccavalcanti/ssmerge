package merger;

public class MergeResult {
		public String projectName	= "";
		public String revision		= "";
		
		public int ssmergeConfs 	= 0;
		public int ssmergeLOC		= 0;
		public int ssmergeFiles		= 0;

		public int linedbasedConfs 	= 0;
		public int linebasedLOC		= 0;
		public int linebasedFiles	= 0;

		public int semanticConfs	= 0;
		
		//false positives
		public int orderingConflictsFromSsmerge = 0;
		public int orderingConflictsFromParser  = 0;
		public int renamingConflictsFromSsmerge = 0;
		public int renamingConflictsFromParser  = 0;
		
		//false negatives
		public int importIssuesFromSsmergePackagePackage = 0;
		public int importIssuesFromSsmergePackageMember  = 0;
		public int importIssuesFromSsmergeMemberMember  = 0;
		public int importIssuesFromParser	    = 0;
		public int importsInserted				= 0;
		
		public int duplicationIssuesFromSsmerge = 0;
		public int duplicationIssuesFromParser  = 0;
}
