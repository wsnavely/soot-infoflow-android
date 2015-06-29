package soot.jimple.infoflow.android.TestApps;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.ForwardBranchedFlowAnalysis;

public abstract class JimpleAnalysis extends
		ForwardBranchedFlowAnalysis<BETSet> {
	
    private final Logger logger = LoggerFactory.getLogger(getClass());

	public JimpleAnalysis(UnitGraph graph) {
		super(graph);
	}

	protected void flowThroughNop(BETSet in, NopStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughIdentity(BETSet in, IdentityStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughAssign(BETSet in, AssignStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughIf(BETSet in, IfStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughGoto(BETSet in, GotoStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughTableSwitch(BETSet in, TableSwitchStmt stmt,
			List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughLookupSwitch(BETSet in, LookupSwitchStmt stmt,
			List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughInvoke(BETSet in, InvokeStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughReturn(BETSet in, ReturnStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughReturnVoid(BETSet in, ReturnVoidStmt stmt,
			List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughEnterMonitor(BETSet in, EnterMonitorStmt stmt,
			List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughExitMonitor(BETSet in, ExitMonitorStmt stmt,
			List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughThrow(BETSet in, ThrowStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	protected void flowThroughRet(BETSet in, RetStmt stmt, List<BETSet> fallOut, List<BETSet> branchOuts) {
		return;
	}

	@Override
	protected void flowThrough(BETSet in, Unit stmt, List<BETSet> fallOut,
			List<BETSet> branchOuts) {
		System.out.println(stmt);
		
		for(BETSet s : fallOut) {
			in.copy(s);
		}
		for(BETSet s : branchOuts) {
			in.copy(s);
		}
		
		if(in.booleanExpression.length() > 0) {
			logger.info("Tagging statement: " + stmt);
			System.out.println("Has Tag:" + stmt.hasTag("BooleanExpressionTag"));
			stmt.addTag(new Tag() {
				@Override
				public byte[] getValue() throws AttributeValueException {
					return in.booleanExpression.getBytes();
				}
				@Override
				public String getName() {
					return "BooleanExpressionTag";
				}
			});
		}
		if (stmt instanceof NopStmt) {
			flowThroughNop(in, (NopStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof IdentityStmt) {
			flowThroughIdentity(in, (IdentityStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof AssignStmt) {
			flowThroughAssign(in, (AssignStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof IfStmt) {
			flowThroughIf(in, (IfStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof GotoStmt) {
			flowThroughGoto(in, (GotoStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof TableSwitchStmt) {
			flowThroughTableSwitch(in, (TableSwitchStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof LookupSwitchStmt) {
			flowThroughLookupSwitch(in, (LookupSwitchStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof InvokeStmt) {
			flowThroughInvoke(in, (InvokeStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof ReturnStmt) {
			flowThroughReturn(in, (ReturnStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof ReturnVoidStmt) {
			flowThroughReturnVoid(in, (ReturnVoidStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof EnterMonitorStmt) {
			flowThroughEnterMonitor(in, (EnterMonitorStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof ExitMonitorStmt) {
			flowThroughExitMonitor(in, (ExitMonitorStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof ThrowStmt) {
			flowThroughThrow(in, (ThrowStmt) stmt, fallOut, branchOuts);
		} else if (stmt instanceof RetStmt) {
			flowThroughRet(in, (RetStmt) stmt, fallOut, branchOuts);
		}
	}

	@Override
	protected BETSet newInitialFlow() {
		return new BETSet();
	}

	@Override
	protected BETSet entryInitialFlow() {
		return new BETSet();
	}

	@Override
	protected void merge(BETSet in1, BETSet in2, BETSet out) {
		in1.merge(in2, out);
	}

	@Override
	protected void copy(BETSet source, BETSet dest) {
		source.copy(dest);
	}

}