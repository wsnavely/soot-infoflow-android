package soot.jimple.infoflow.android.TestApps;

import soot.Value;

public class Assignment {
	private Value lhs;
	private Value rhs;
	private String tag;

	public Assignment(Value lhs, Value rhs, String tag) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.tag = tag;
	}

	public Value getLHS() {
		return this.lhs;
	}

	public Value getRHS() {
		return this.rhs;
	}

	public String getTag() {
		return this.tag;
	}
}