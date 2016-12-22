package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration.CallbackAnalyzer;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

public class FlowDroidFactory {
	abstract class Option<V> {
		String repr;

		public Option(String def) {
			this.repr = def;
		}

		public void setRepr(String val) {
			this.repr = val;
		}

		public abstract V getValue();
	}

	class SimpleOption extends Option<String> {
		public SimpleOption(String def) {
			super(def);
		}

		@Override
		public String getValue() {
			return this.repr;
		}
	}

	class BooleanOption extends Option<Boolean> {
		public BooleanOption(String def) {
			super(def);
		}

		public BooleanOption(Boolean b) {
			super(b.toString());
		}

		@Override
		public Boolean getValue() {
			return Boolean.parseBoolean(this.repr);
		}
	}

	class IntOption extends Option<Integer> {
		public IntOption(String def) {
			super(def);
		}

		public IntOption(Integer i) {
			super(i.toString());
		}

		@Override
		public Integer getValue() {
			return Integer.parseInt(this.repr);
		}
	}

	class EnumOption extends Option<Enum> {
		private Class type;

		public EnumOption(Class t, String def) {
			super(def);
			this.type = t;
		}
		
		public EnumOption(Enum e) {
			this(e.getClass(), e.toString());
		}

		@Override
		public Enum getValue() {
			return Enum.valueOf(this.type, this.repr);
		}
	}

	public Map<String, Option> getDefaultOptions() {
		Map<String, Option> options = new HashMap<String, Option>();
		options.put("accessPathLength", new IntOption(5));
		options.put("aliasingAlgorithm", new EnumOption(AliasingAlgorithm.FlowSensitive));
		options.put("callbackAnalyzer", new EnumOption(CallbackAnalyzer.Default));
		options.put("callgraphAlgorithm", new EnumOption(CallgraphAlgorithm.AutomaticSelection));
		options.put("codeEliminationMode", new EnumOption(CodeEliminationMode.PropagateConstants));
		options.put("computeResultPaths", new BooleanOption(true));
		options.put("enableArraySizeTainting", new BooleanOption(true));
		options.put("enableCallbacks", new BooleanOption(true));
		options.put("enableCallbackSources", new BooleanOption(true));
		options.put("enableExceptions", new BooleanOption(true));
		options.put("enableImplicitFlows", new BooleanOption(false));
		options.put("enableIncrementalReporting", new BooleanOption(false));
		options.put("enableStaticTracking", new BooleanOption(true));
		options.put("enableTaintAnalysis", new BooleanOption(true));
		options.put("enableTypeChecking", new BooleanOption(true));
		options.put("flowSensitiveAliasing", new BooleanOption(true));
		options.put("ignoreFlowsInSystemPackages", new BooleanOption(true));
		options.put("inspectSinks", new BooleanOption(false));
		options.put("inspectSources", new BooleanOption(false));
		options.put("layoutMatchingMode", new EnumOption(LayoutMatchingMode.MatchSensitiveOnly));
		options.put("logSourcesAndSinks", new BooleanOption(false));
		options.put("maxThreadNum", new IntOption(-1));
		options.put("mergeNeighbors", new BooleanOption(false));
		options.put("oneResultPerAccessPath", new BooleanOption(false));
		options.put("pathAgnosticResults", new BooleanOption(true));
		options.put("pathBuilder", new EnumOption(PathBuilder.ContextInsensitiveSourceFinder));
		options.put("stopAfterFirstKFlows", new IntOption(0));
		options.put("useRecursiveAccessPaths", new BooleanOption(true));
		options.put("useThisChainReduction", new BooleanOption(true));
		options.put("useTypeTightening", new BooleanOption(true));
		options.put("writeOutputFiles", new BooleanOption(false));
		
		return options;
	}

	public static SetupApplication fromArgs(String[] args) {
		return fromArgs(args, null);
	}

	public static SetupApplication fromArgs(String[] args, IIPCManager im) {
		return null;
	}

	public SetupApplication fromJson(String json, String apk)
			throws IOException, XmlPullParserException {
		return fromJson(json, apk, null);
	}

	public SetupApplication fromJson(String json, String apk, IIPCManager im)
			throws IOException, XmlPullParserException {
		Map<String, Option> options = this.getDefaultOptions();
		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();
		Map jsonData = parser.parseJson(json);

		for (Object key : jsonData.keySet()) {
			if (options.containsKey(key)) {
				Option opt = options.get(key);
				opt.getValue();
				opt.setRepr((String) jsonData.get(key));
			}
		}

		return fromOptions(options, apk, im);
	}

	public SetupApplication fromOptions(Map<String, Option> options,
			String apk, IIPCManager im) throws IOException,
			XmlPullParserException {
		SetupApplication app;
		String platforms = (String) options.get("androidPlatformsFolder")
				.getValue();
		if (im == null) {
			app = new SetupApplication(platforms, apk);
		} else {
			app = new SetupApplication(platforms, apk, im);
		}
		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		InfoflowConfiguration.setAccessPathLength((int) options.get("accessPathLength").getValue());
		config.setAliasingAlgorithm((AliasingAlgorithm) options.get("aliasingAlgorithm").getValue());
		config.setCallbackAnalyzer((CallbackAnalyzer) options.get("callbackAnalyzer").getValue());
		config.setCallgraphAlgorithm((CallgraphAlgorithm) options.get("callgraphAlgorithm").getValue());
		config.setCodeEliminationMode((CodeEliminationMode) options.get("codeEliminationMode").getValue());
		config.setComputeResultPaths((boolean) options.get("computeResultPaths").getValue());
		config.setEnableArraySizeTainting((boolean) options.get("enableArraySizeTainting").getValue());
		config.setEnableCallbacks((boolean) options.get("enableCallbacks").getValue());
		config.setEnableCallbackSources((boolean) options.get("enableCallbackSources").getValue());
		config.setEnableExceptionTracking((boolean) options.get("enableExceptions").getValue());
		config.setEnableImplicitFlows((boolean) options.get("enableImplicitFlows").getValue());
		config.setIncrementalResultReporting((boolean) options.get("enableIncrementalReporting").getValue());
		config.setEnableStaticFieldTracking((boolean) options.get("enableStaticTracking").getValue());
		config.setTaintAnalysisEnabled((boolean) options.get("enableTaintAnalysis").getValue());
		config.setEnableTypeChecking((boolean) options.get("enableTypeChecking").getValue());
		config.setFlowSensitiveAliasing((boolean) options.get("flowSensitiveAliasing").getValue());
		config.setIgnoreFlowsInSystemPackages((boolean) options.get("ignoreFlowsInSystemPackages").getValue());
		config.setInspectSinks((boolean) options.get("inspectSinks").getValue());
		config.setInspectSources((boolean) options.get("inspectSources").getValue());
		config.setLayoutMatchingMode((LayoutMatchingMode) options.get("layoutMatchingMode").getValue());
		config.setLogSourcesAndSinks((boolean) options.get("logSourcesAndSinks").getValue());
		config.setMaxThreadNum((int) options.get("maxTheadNum").getValue());
		InfoflowConfiguration.setMergeNeighbors((boolean) options.get("mergeNeighbors").getValue());
		InfoflowConfiguration.setOneResultPerAccessPath((boolean) options.get("oneResultPerAccessPath").getValue());
		InfoflowConfiguration.setPathAgnosticResults((boolean) options.get("pathAgnosticResults").getValue());
		config.setPathBuilder((PathBuilder) options.get("pathBuilder").getValue());
		config.setStopAfterFirstKFlows((int) options.get("stopAfterFirstKFlows").getValue());
		InfoflowConfiguration.setUseRecursiveAccessPaths((boolean) options.get("useRecursiveAccessPaths").getValue());
		InfoflowConfiguration.setUseThisChainReduction((boolean) options.get("useThisChainReduction").getValue());
		InfoflowConfiguration.setUseTypeTightening((boolean) options.get("useTypeTightening").getValue());
		config.setWriteOutputFiles((boolean) options.get("writeOutputFiles").getValue());
		
		app.setConfig(config);
		app.calculateSourcesSinksEntrypoints((String) options.get("sourcesAndSinksFile").getValue());

		String taintWrapper = (String) options.get("taintWrapperFile")
				.getValue();
		if (taintWrapper != null && !taintWrapper.isEmpty()) {
			EasyTaintWrapper easyTaintWrapper;
			easyTaintWrapper = new EasyTaintWrapper(taintWrapper);
			boolean mode = (boolean) options.get("aggressiveTaintWrapper")
					.getValue();
			easyTaintWrapper.setAggressiveMode(mode);
			app.setTaintWrapper(easyTaintWrapper);
		}

		return app;
	}
}