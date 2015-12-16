package util;

/**
 * Class representing a merge conflict.
 * FPFN
 * @author Guilherme
 *
 */

public class MergeConflict {
	
	private String fileName;
	private String left;
	private String base;
	private String right;
	
	public MergeConflict(String fileName, String left, String base, String right) {
		this.fileName = fileName;
		this.left 	= left;
		this.right 	= right;
		this.base 	= base;
	}

	public MergeConflict(String fileName, String left, String right) {
		this.left = left;
		this.right = right;
		this.fileName = fileName;
	}

	public String getLeft() {
		return left;
	}

	public void setLeft(String left) {
		this.left = left;
	}

	public String getBase() {
		return base;
	}

	public void setBase(String base) {
		this.base = base;
	}

	public String getRight() {
		return right;
	}

	public void setRight(String right) {
		this.right = right;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}	
}
