package soot.jimple.infoflow.android.TestApps;

public class BooleanExpression {
	private String expr = "";
	
	public static String and(String e1, String e2) {
		if(e1 == null || e1.isEmpty()) {
			return e2;
		}
		if(e2 == null || e2.isEmpty()) {
			return e1;
		}
		return String.format("(%s AND %s)", e1, e2);	
	}
	
	public static String or(String e1, String e2) {
		if(e1 == null || e1.isEmpty()) {
			return e2;
		}
		if(e2 == null || e2.isEmpty()) {
			return e1;
		}
		return String.format("(%s OR %s)", e1, e2);
	}
}
