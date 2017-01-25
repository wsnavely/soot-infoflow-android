package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

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
	interface ConfigTransform<V> {
		void apply(InfoflowAndroidConfiguration s, V value);
	}

	interface Parser<V> {
		V getValue(String s);
	}

	class IdentityParser implements Parser<String> {
		@Override
		public String getValue(String s) {
			return s;
		}
	}

	class IntParser implements Parser<Integer> {
		@Override
		public Integer getValue(String s) {
			return Integer.parseInt(s);
		}
	}

	class BooleanParser implements Parser<Boolean> {
		@Override
		public Boolean getValue(String s) {
			return Boolean.parseBoolean(s);
		}
	}

	class FlowdroidSetting<V> {
		Parser<V> parser;
		ConfigTransform<V> tform;
		V defaultValue;

		public FlowdroidSetting(V defaultValue, Parser<V> parser, ConfigTransform<V> tform) {
			this.defaultValue = defaultValue;
			this.parser = parser;
			this.tform = tform;
		}
	}

	class StringSetting extends FlowdroidSetting<String> {
		public StringSetting(String defaultValue, Parser<String> parser, ConfigTransform<String> tform) {
			super(defaultValue, new IdentityParser(), tform);
		}
	}

	class IntSetting extends FlowdroidSetting<Integer> {
		public IntSetting(Integer defaultValue, ConfigTransform<Integer> tform) {
			super(defaultValue, new IntParser(), tform);
		}
	}

	class BoolSetting extends FlowdroidSetting<Boolean> {
		public BoolSetting(Boolean defaultValue, ConfigTransform<Boolean> tform) {
			super(defaultValue, new BooleanParser(), tform);
		}
	}

	class EnumSetting<E extends Enum<E>> extends FlowdroidSetting<E> {
		public EnumSetting(E defaultValue, ConfigTransform<E> tform) {
			super(defaultValue, new Parser<E>() {
				@Override
				public E getValue(String s) {
					Class type = defaultValue.getClass();
					return (E) Enum.valueOf(type, s);
				}
			}, tform);
		}
	}

	public Map<String, FlowdroidSetting> getDefaultOptions() {
		Map<String, FlowdroidSetting> options = new HashMap<String, FlowdroidSetting>();

		options.put("accessPathLength", new IntSetting(5, new ConfigTransform<Integer>() {
			public void apply(InfoflowAndroidConfiguration conf, Integer value) {
				InfoflowConfiguration.setAccessPathLength(value);
			}
		}));

		options.put("aliasingAlgorithm", new EnumSetting<AliasingAlgorithm>(AliasingAlgorithm.FlowSensitive,
				new ConfigTransform<AliasingAlgorithm>() {
					public void apply(InfoflowAndroidConfiguration conf, AliasingAlgorithm value) {
						conf.setAliasingAlgorithm(value);
					}
				}));

		options.put("callbackAnalyzer",
				new EnumSetting<CallbackAnalyzer>(CallbackAnalyzer.Default, new ConfigTransform<CallbackAnalyzer>() {
					public void apply(InfoflowAndroidConfiguration conf, CallbackAnalyzer value) {
						conf.setCallbackAnalyzer(value);
					}
				}));

		options.put("callgraphAlgorithm", new EnumSetting<CallgraphAlgorithm>(CallgraphAlgorithm.AutomaticSelection,
				new ConfigTransform<CallgraphAlgorithm>() {
					public void apply(InfoflowAndroidConfiguration conf, CallgraphAlgorithm value) {
						conf.setCallgraphAlgorithm(value);
					}
				}));

		options.put("codeEliminationMode", new EnumSetting<CodeEliminationMode>(CodeEliminationMode.PropagateConstants,
				new ConfigTransform<CodeEliminationMode>() {
					public void apply(InfoflowAndroidConfiguration conf, CodeEliminationMode value) {
						conf.setCodeEliminationMode(value);
					}
				}));

		options.put("computeResultPaths", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setComputeResultPaths(value);
			}
		}));

		options.put("enableArraySizeTainting", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableArraySizeTainting(value);
			}
		}));

		options.put("enableCallbacks", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableCallbacks(value);
			}
		}));

		options.put("enableCallbackSources", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableCallbackSources(value);
			}
		}));

		options.put("enableExceptions", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableExceptionTracking(value);
			}
		}));

		options.put("enableImplicitFlows", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableImplicitFlows(value);
			}
		}));

		options.put("enableIncrementalReporting", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setIncrementalResultReporting(value);
			}
		}));

		options.put("enableStaticTracking", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableStaticFieldTracking(value);
			}
		}));

		options.put("enableTaintAnalysis", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setTaintAnalysisEnabled(value);
			}
		}));

		options.put("enableTypeChecking", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setEnableTypeChecking(value);
			}
		}));

		options.put("flowSensitiveAliasing", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setFlowSensitiveAliasing(value);
			}
		}));

		options.put("ignoreFlowsInSystemPackages", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setIgnoreFlowsInSystemPackages(value);
			}
		}));

		options.put("inspectSinks", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setInspectSinks(value);
			}
		}));

		options.put("inspectSources", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setInspectSources(value);
			}
		}));

		options.put("layoutMatchingMode", new EnumSetting<LayoutMatchingMode>(LayoutMatchingMode.MatchSensitiveOnly,
				new ConfigTransform<LayoutMatchingMode>() {
					public void apply(InfoflowAndroidConfiguration conf, LayoutMatchingMode value) {
						conf.setLayoutMatchingMode(value);
					}
				}));

		options.put("logSourcesAndSinks", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setLogSourcesAndSinks(value);
			}
		}));

		options.put("maxThreadNum", new IntSetting(-1, new ConfigTransform<Integer>() {
			public void apply(InfoflowAndroidConfiguration conf, Integer value) {
				conf.setMaxThreadNum(value);
			}
		}));

		options.put("mergeNeighbors", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setMergeNeighbors(value);
			}
		}));

		options.put("oneResultPerAccessPath", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setOneResultPerAccessPath(value);
			}
		}));

		options.put("pathAgnosticResults", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setPathAgnosticResults(value);
			}
		}));

		options.put("pathBuilder", new EnumSetting<PathBuilder>(PathBuilder.ContextInsensitiveSourceFinder,
				new ConfigTransform<PathBuilder>() {
					public void apply(InfoflowAndroidConfiguration conf, PathBuilder value) {
						conf.setPathBuilder(value);
					}
				}));

		options.put("stopAfterFirstKFlows", new IntSetting(0, new ConfigTransform<Integer>() {
			public void apply(InfoflowAndroidConfiguration conf, Integer value) {
				conf.setStopAfterFirstKFlows(value);
			}
		}));

		options.put("useRecursiveAccessPaths", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setUseRecursiveAccessPaths(value);
			}
		}));

		options.put("useThisChainReduction", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setUseThisChainReduction(value);
			}
		}));

		options.put("useTypeTightening", new BoolSetting(true, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				InfoflowConfiguration.setUseTypeTightening(value);
			}
		}));

		options.put("writeOutputFiles", new BoolSetting(false, new ConfigTransform<Boolean>() {
			public void apply(InfoflowAndroidConfiguration conf, Boolean value) {
				conf.setWriteOutputFiles(value);
			}
		}));
		return options;
	}

	public static SetupApplication fromArgs(String[] args) {
		return fromArgs(args, null);
	}

	public static SetupApplication fromArgs(String[] args, IIPCManager im) {
		return null;
	}

	public SetupApplication fromJson(String json, String apk) throws IOException, XmlPullParserException {
		return fromJson(json, apk, null);
	}

	public SetupApplication fromJson(String json, String apk, IIPCManager im)
			throws IOException, XmlPullParserException {

		JsonParserFactory factory = JsonParserFactory.getInstance();
		JSONParser parser = factory.newJsonParser();

		// Parse the json and pull out the various settings groups
		Map jsonData = parser.parseJson(json);
		Map generalSettings = (Map) jsonData.get("general");
		Map analysisSettings = (Map) jsonData.get("analysis");
		Map twSettings = (Map) jsonData.get("taintwrapper");
		Map<String, FlowdroidSetting> settings = this.getDefaultOptions();

		// Construct the SetupApplication
		SetupApplication app;
		String platforms = (String) generalSettings.get("androidPlatformsFolder");
		if (im == null) {
			app = new SetupApplication(platforms, apk);
		} else {
			app = new SetupApplication(platforms, apk, im);
		}

		// Configure the infoflow analysis
		InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
		for (String name : settings.keySet()) {
			FlowdroidSetting setting = settings.get(name);
			Object value = setting.defaultValue;
			if (analysisSettings.containsKey(name)) {
				String rawValue = analysisSettings.get(name).toString();
				value = setting.parser.getValue(rawValue);
			}
			setting.tform.apply(config, value);
		}
		app.setConfig(config);

		// Calculate sources and sinks
		app.calculateSourcesSinksEntrypoints((String) generalSettings.get("sourcesAndSinksFile"));

		// Configure the taint wrapper
		String taintWrapper = (String) twSettings.get("taintWrapperFile");
		if (taintWrapper != null && !taintWrapper.isEmpty()) {
			EasyTaintWrapper easyTaintWrapper;
			easyTaintWrapper = new EasyTaintWrapper(taintWrapper);
			boolean mode = Boolean.parseBoolean((String) twSettings.get("aggressiveTaintWrapper"));
			easyTaintWrapper.setAggressiveMode(mode);
			app.setTaintWrapper(easyTaintWrapper);
		}

		return app;
	}
}