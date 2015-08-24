//package soot.jimple.infoflow.android.TestApps;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import soot.SootMethod;
//import soot.Unit;
//import soot.Value;
//import soot.jimple.AssignStmt;
//import soot.jimple.InvokeExpr;
//import soot.toolkits.graph.UnitGraph;
//
//public class Simple extends JimpleAnalysis<Set<Assignment>> {
//
//	private IntentOracle io;
//
//	public Simple(UnitGraph graph) {
//		super(graph);
//		io = new AndroidIntentOracle();
//		doAnalysis();
//	}
//
//	@Override
//	protected void flowThrough(Set<Assignment> in, Unit s,
//			List<Set<Assignment>> fall, List<Set<Assignment>> branch) {
//		for (Map m : fall) {
//			m.putAll(in);
//		}
//		for (Map m : branch) {
//			m.putAll(in);
//		}
//		super.flowThrough(in, s, fall, branch);
//	}
//
//	private void printAssignments(Map m) {
//		System.out.println("Assignments");
//		System.out.println("-----------------");
//		for (Object key : m.keySet()) {
//			System.out.print(key + ": ");
//			boolean first = true;
//			for (Object val : (List) m.get(key)) {
//				if (first) {
//					first = false;
//				} else {
//					System.out.print(", ");
//				}
//				System.out.print(val);
//			}
//			System.out.println();
//		}
//	}
//
//	@Override
//	protected void flowThroughAssign(Map in, AssignStmt s, List<Map> fall,
//			List<Map> branch) {
//		//List lst = new ArrayList();
//		//lst.add(stmt.getRightOp());
//		//fallOut.get(0).put(stmt.getLeftOp(), lst);
//
//		Value lhs = s.getLeftOp();
//		Value rhs = s.getRightOp();
//
//		// We want to know: does the righthand side of the assignment 
//		// produce an action string?  This is possible if:
//		// 1) The RHS is a call to the getAction method on an Intent
//		// 2) The assignment aliases an existing action string variable
//		// 3) The RHS is an expression that involves an action string
//		// 4) The RHS is a procedure call that returns an action string
//		if (isCallToGetAction(rhs)) {
//
//		}
//	}
//
//	private boolean isCallToGetAction(Value rhs) {
//		if (rhs instanceof InvokeExpr) {
//			InvokeExpr ie = (InvokeExpr) rhs;
//			SootMethod sm = ie.getMethod();
//			if (this.io.isGetAction(sm)) {
//				return true;
//			}
//		}
//		return false;
//	}
//
//	@Override
//	protected Map newInitialFlow() {
//		return new HashMap();
//	}
//
//	@Override
//	protected void merge(Map in1, Map in2, Map out) {
//		out.putAll(in1);
//		for (Object key : in2.keySet()) {
//			if (out.containsKey(key)) {
//				((List) out.get(key)).addAll((List) in2.get(key));
//			} else {
//				out.put(key, in2.get(key));
//			}
//		}
//	}
//
//	@Override
//	protected void copy(Map source, Map dest) {
//		dest.putAll(source);
//	}
//}
