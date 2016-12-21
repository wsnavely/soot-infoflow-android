package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.handlers.PreAnalysisHandler;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;

public class FlowDroid {
	static String command;
	static boolean generate = false;

	public boolean stopAfterFirstFlow = false;
	public boolean implicitFlows = false;
	public boolean staticTracking = true;
	public boolean enableCallbacks = true;
	public boolean enableExceptions = true;
	public boolean flowSensitiveAliasing = true;
	public boolean computeResultPaths = true;
	public boolean aggressiveTaintWrapper = false;
	public String sourcesAndSinks = "";
	public String androidPlatforms = "";
	public int accessPathLength = 5;
	public ITaintPropagationWrapper taintWrapper = null;
	public PathBuilder pathBuilder = PathBuilder.ContextInsensitiveSourceFinder;
	public CallgraphAlgorithm callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
	public LayoutMatchingMode layoutMatchingMode = LayoutMatchingMode.MatchSensitiveOnly;
	public IIPCManager ipcManager = null;
	public List<PreAnalysisHandler> preprocessors = new ArrayList<PreAnalysisHandler>();

	public InfoflowResults runAnalysis(String fileName,
			AndroidInfoflowResultsHandler handler) {
		try {
			SetupApplication app;
			if (null == ipcManager) {
				app = new SetupApplication(androidPlatforms, fileName);
			} else {
				app = new SetupApplication(androidPlatforms, fileName,
						ipcManager);
			}

			app.setStopAfterFirstFlow(stopAfterFirstFlow);
			app.setEnableImplicitFlows(implicitFlows);
			app.setEnableStaticFieldTracking(staticTracking);
			app.setEnableCallbacks(enableCallbacks);
			app.setEnableExceptionTracking(enableExceptions);
			app.setAccessPathLength(accessPathLength);
			app.setLayoutMatchingMode(layoutMatchingMode);
			app.setFlowSensitiveAliasing(flowSensitiveAliasing);
			app.setPathBuilder(pathBuilder);
			app.setComputeResultPaths(computeResultPaths);
			app.setPreprocessors(preprocessors);
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");

			System.out.println("Running data flow analysis...");
			String pkg = app.getSourceSinkManager().getAppPackageName();
			handler.setAppPackage(pkg);
			final InfoflowResults res = app.runInfoflow(handler);
			return res;
		} catch (IOException ex) {
			System.err.println("Could not read file: " + ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		} catch (XmlPullParserException ex) {
			System.err.println("Could not read Android manifest file: "
					+ ex.getMessage());
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
