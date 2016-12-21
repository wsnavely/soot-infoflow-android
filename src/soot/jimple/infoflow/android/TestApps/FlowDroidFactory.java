package soot.jimple.infoflow.android.TestApps;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.IInfoflow.CallgraphAlgorithm;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.AndroidSourceSinkManager.LayoutMatchingMode;
import soot.jimple.infoflow.data.pathBuilders.DefaultPathBuilderFactory.PathBuilder;
import soot.jimple.infoflow.ipc.IIPCManager;
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;

import com.json.parsers.JSONParser;
import com.json.parsers.JsonParserFactory;

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

		@Override
		public Enum getValue() {
			return Enum.valueOf(this.type, this.repr);
		}
	}

	public Map<String, Option> getDefaultOptions() {
		Map<String, Option> options = new HashMap<String, Option>();
		options.put("flowSensitiveAliasing", new BooleanOption(false));
		options.put("enableExceptions", new BooleanOption(false));
		options.put("stopAfterFirstFlow", new BooleanOption(false));
		options.put("computeResultPaths", new BooleanOption(false));
		options.put("enableCallbacks", new BooleanOption(false));
		options.put("staticTracking", new BooleanOption(false));
		options.put("implicitFlows", new BooleanOption(false));
		options.put("flowSensitiveAliasing", new BooleanOption(false));
		options.put("aggressiveTaintWrapper", new BooleanOption(false));
		options.put("pathBuilder", new EnumOption(PathBuilder.class,
				"ContextInsensitiveSourceFinder"));
		options.put("callgraphAlgorithm", new EnumOption(
				CallgraphAlgorithm.class, "AutomaticSelection"));
		options.put("layoutMatchingMode", new EnumOption(
				LayoutMatchingMode.class, "MatchSensitiveOnly"));
		options.put("sourcesAndSinksFile", new SimpleOption(
				"SourcesAndSinks.txt"));
		options.put("taintWrapperFile", new SimpleOption(
				"EasyTaintWrapperSource.txt"));
		options.put("androidPlatformsFolder", new SimpleOption(""));
		options.put("accessPathLength", new IntOption(5));
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

		// @formatter:off
		app.setStopAfterFirstFlow((boolean) options.get("stopAfterFirstFlow").getValue());
		app.setEnableImplicitFlows((boolean) options.get("implicitFlows").getValue());
		app.setEnableStaticFieldTracking((boolean) options.get("staticTracking").getValue());
		app.setEnableCallbacks((boolean) options.get("enableCallbacks").getValue());
		app.setEnableExceptionTracking((boolean) options.get("enableExceptions").getValue());
		app.setFlowSensitiveAliasing((boolean) options.get("flowSensitiveAliasing").getValue());
		app.setComputeResultPaths((boolean) options.get("computeResultPaths").getValue());
		app.setAccessPathLength((int) options.get("accessPathLength").getValue());
		app.setLayoutMatchingMode((LayoutMatchingMode) options.get("layoutMatchingMode").getValue());
		app.setPathBuilder((PathBuilder) options.get("pathBuilder").getValue());
		app.setCallgraphAlgorithm((CallgraphAlgorithm) options.get("callgraphAlgorithm").getValue());
		app.calculateSourcesSinksEntrypoints((String) options.get("sourcesAndSinksFile").getValue());
		// @formatter:on

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