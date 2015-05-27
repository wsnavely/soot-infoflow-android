package soot.jimple.infoflow.android.TestApps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import soot.jimple.infoflow.taintWrappers.EasyTaintWrapper;
import soot.jimple.infoflow.taintWrappers.ITaintPropagationWrapper;
import soot.jimple.infoflow.taintWrappers.TaintWrapperSet;

public class FlowDroid {
	static String command;
	static boolean generate = false;

	public static boolean stopAfterFirstFlow = false;
	public static boolean implicitFlows = false;
	public static boolean staticTracking = true;
	public static boolean enableCallbacks = true;
	public static boolean enableExceptions = true;
	public static int accessPathLength = 5;
	public static LayoutMatchingMode layoutMatchingMode = LayoutMatchingMode.MatchSensitiveOnly;
	public static boolean flowSensitiveAliasing = true;
	public static boolean computeResultPaths = true;
	public static boolean aggressiveTaintWrapper = false;
	public static boolean librarySummaryTaintWrapper = false;
	public static String summaryPath = "";
	public static PathBuilder pathBuilder = PathBuilder.ContextInsensitiveSourceFinder;
	public static List<PreAnalysisHandler> preprocessors = new ArrayList<PreAnalysisHandler>();

	public static CallgraphAlgorithm callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;

	private static boolean DEBUG = false;

	private static IIPCManager ipcManager = null;

	public static void setIPCManager(IIPCManager ipcManager) {
		FlowDroid.ipcManager = ipcManager;
	}

	public static IIPCManager getIPCManager() {
		return FlowDroid.ipcManager;
	}

	public static void run(String[] args, AndroidInfoflowResultsHandler handler)
			throws IOException, InterruptedException {

		if (args.length < 2) {
			printUsage();
			return;
		}
		// start with cleanup:
		File outputDir = new File("JimpleOutput");
		if (outputDir.isDirectory()) {
			boolean success = true;
			for (File f : outputDir.listFiles()) {
				success = success && f.delete();
			}
			if (!success) {
				System.err.println("Cleanup of output directory " + outputDir
						+ " failed!");
			}
			outputDir.delete();
		}

		// Parse additional command-line arguments
		if (!parseAdditionalOptions(args))
			return;
		if (!validateAdditionalOptions())
			return;

		List<String> apkFiles = new ArrayList<String>();
		File apkFile = new File(args[0]);

		if (apkFile.isDirectory()) {
			String[] dirFiles = apkFile.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String name) {
					return (name.endsWith(".apk"));
				}

			});
			for (String s : dirFiles)
				apkFiles.add(s);
		} else {
			// apk is a file so grab the extension
			String extension = apkFile.getName().substring(
					apkFile.getName().lastIndexOf("."));
			if (extension.equalsIgnoreCase(".txt")) {
				BufferedReader rdr = new BufferedReader(new FileReader(apkFile));
				String line = null;
				while ((line = rdr.readLine()) != null)
					apkFiles.add(line);
				rdr.close();
			} else if (extension.equalsIgnoreCase(".apk"))
				apkFiles.add(args[0]);
			else {
				System.err.println("Invalid input file format: " + extension);
				return;
			}
		}

		for (final String fileName : apkFiles) {
			final String fullFilePath;

			// Directory handling
			if (apkFiles.size() > 1) {
				if (apkFile.isDirectory())
					fullFilePath = args[0] + File.separator + fileName;
				else
					fullFilePath = fileName;
				System.out.println("Analyzing file " + fullFilePath + "...");
				File flagFile = new File("_Run_" + new File(fileName).getName());
				if (flagFile.exists())
					continue;
				flagFile.createNewFile();
			} else {
				fullFilePath = fileName;
			}

			runAnalysis(fullFilePath, args[1], handler);
			System.gc();
		}
	}

	private static boolean parseAdditionalOptions(String[] args) {
		int i = 2;
		while (i < args.length) {
			if (args[i].equalsIgnoreCase("--singleflow")) {
				stopAfterFirstFlow = true;
				i++;
			} else if (args[i].equalsIgnoreCase("--implicit")) {
				implicitFlows = true;
				i++;
			} else if (args[i].equalsIgnoreCase("--nostatic")) {
				staticTracking = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--aplength")) {
				accessPathLength = Integer.valueOf(args[i + 1]);
				i += 2;
			} else if (args[i].equalsIgnoreCase("--cgalgo")) {
				String algo = args[i + 1];
				if (algo.equalsIgnoreCase("AUTO"))
					callgraphAlgorithm = CallgraphAlgorithm.AutomaticSelection;
				else if (algo.equalsIgnoreCase("CHA"))
					callgraphAlgorithm = CallgraphAlgorithm.CHA;
				else if (algo.equalsIgnoreCase("VTA"))
					callgraphAlgorithm = CallgraphAlgorithm.VTA;
				else if (algo.equalsIgnoreCase("RTA"))
					callgraphAlgorithm = CallgraphAlgorithm.RTA;
				else if (algo.equalsIgnoreCase("SPARK"))
					callgraphAlgorithm = CallgraphAlgorithm.SPARK;
				else {
					System.err.println("Invalid callgraph algorithm");
					return false;
				}
				i += 2;
			} else if (args[i].equalsIgnoreCase("--nocallbacks")) {
				enableCallbacks = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--noexceptions")) {
				enableExceptions = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--layoutmode")) {
				String algo = args[i + 1];
				if (algo.equalsIgnoreCase("NONE"))
					layoutMatchingMode = LayoutMatchingMode.NoMatch;
				else if (algo.equalsIgnoreCase("PWD"))
					layoutMatchingMode = LayoutMatchingMode.MatchSensitiveOnly;
				else if (algo.equalsIgnoreCase("ALL"))
					layoutMatchingMode = LayoutMatchingMode.MatchAll;
				else {
					System.err.println("Invalid layout matching mode");
					return false;
				}
				i += 2;
			} else if (args[i].equalsIgnoreCase("--aliasflowins")) {
				flowSensitiveAliasing = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--nopaths")) {
				computeResultPaths = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--aggressivetw")) {
				aggressiveTaintWrapper = false;
				i++;
			} else if (args[i].equalsIgnoreCase("--pathalgo")) {
				String algo = args[i + 1];
				if (algo.equalsIgnoreCase("CONTEXTSENSITIVE"))
					pathBuilder = PathBuilder.ContextSensitive;
				else if (algo.equalsIgnoreCase("CONTEXTINSENSITIVE"))
					pathBuilder = PathBuilder.ContextInsensitive;
				else if (algo.equalsIgnoreCase("SOURCESONLY"))
					pathBuilder = PathBuilder.ContextInsensitiveSourceFinder;
				else {
					System.err.println("Invalid path reconstruction algorithm");
					return false;
				}
				i += 2;
			} else if (args[i].equalsIgnoreCase("--libsumtw")) {
				librarySummaryTaintWrapper = true;
				i++;
			} else if (args[i].equalsIgnoreCase("--summarypath")) {
				summaryPath = args[i + 1];
				i += 2;
			} else
				i++;
		}
		return true;
	}

	private static boolean validateAdditionalOptions() {
		if (!flowSensitiveAliasing
				&& callgraphAlgorithm != CallgraphAlgorithm.OnDemand
				&& callgraphAlgorithm != CallgraphAlgorithm.AutomaticSelection) {
			System.err
					.println("Flow-insensitive aliasing can only be configured for callgraph "
							+ "algorithms that support this choice.");
			return false;
		}
		if (librarySummaryTaintWrapper && summaryPath.isEmpty()) {
			System.err
					.println("Summary path must be specified when using library summaries");
			return false;
		}
		return true;
	}

	private static InfoflowResults runAnalysis(final String fileName,
			final String androidJar, AndroidInfoflowResultsHandler handler) {
		try {
			final long beforeRun = System.nanoTime();

			final SetupApplication app;
			if (null == ipcManager) {
				app = new SetupApplication(androidJar, fileName);
			} else {
				app = new SetupApplication(androidJar, fileName, ipcManager);
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

			final ITaintPropagationWrapper taintWrapper;
			if (librarySummaryTaintWrapper) {
				taintWrapper = createLibrarySummaryTW();
			} else {
				final EasyTaintWrapper easyTaintWrapper;
				if (new File("../soot-infoflow/EasyTaintWrapperSource.txt")
						.exists())
					easyTaintWrapper = new EasyTaintWrapper(
							"../soot-infoflow/EasyTaintWrapperSource.txt");
				else
					easyTaintWrapper = new EasyTaintWrapper(
							"EasyTaintWrapperSource.txt");
				easyTaintWrapper.setAggressiveMode(aggressiveTaintWrapper);
				taintWrapper = easyTaintWrapper;
			}
			app.setTaintWrapper(taintWrapper);
			app.calculateSourcesSinksEntrypoints("SourcesAndSinks.txt");
			// app.calculateSourcesSinksEntrypoints("SuSiExport.xml");

			if (DEBUG) {
				app.printEntrypoints();
				app.printSinks();
				app.printSources();
			}

			System.out.println("Running data flow analysis...");
			handler.setAppPackage(app.getSourceSinkManager()
					.getAppPackageName());
			final InfoflowResults res = app.runInfoflow(handler);
			System.out.println("Analysis has run for "
					+ (System.nanoTime() - beforeRun) / 1E9 + " seconds");
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

	/**
	 * Creates the taint wrapper for using library summaries
	 * 
	 * @return The taint wrapper for using library summaries
	 * @throws IOException
	 *             Thrown if one of the required files could not be read
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static ITaintPropagationWrapper createLibrarySummaryTW()
			throws IOException {
		try {
			Class clzLazySummary = Class
					.forName("soot.jimple.infoflow.methodSummary.data.impl.LazySummary");

			Object lazySummary = clzLazySummary.getConstructor(File.class)
					.newInstance(new File(summaryPath));

			ITaintPropagationWrapper summaryWrapper = (ITaintPropagationWrapper) Class
					.forName(
							"soot.jimple.infoflow.methodSummary.taintWrappers.SummaryTaintWrapper")
					.getConstructor(clzLazySummary).newInstance(lazySummary);

			final TaintWrapperSet taintWrapperSet = new TaintWrapperSet();
			taintWrapperSet.addWrapper(summaryWrapper);
			taintWrapperSet.addWrapper(new EasyTaintWrapper(
					"EasyTaintWrapperConversion.txt"));
			return taintWrapperSet;
		} catch (ClassNotFoundException | NoSuchMethodException ex) {
			System.err.println("Could not find library summary classes: "
					+ ex.getMessage());
			ex.printStackTrace();
			return null;
		} catch (InvocationTargetException ex) {
			System.err.println("Could not initialize library summaries: "
					+ ex.getMessage());
			ex.printStackTrace();
			return null;
		} catch (IllegalAccessException | InstantiationException ex) {
			System.err
					.println("Internal error in library summary initialization: "
							+ ex.getMessage());
			ex.printStackTrace();
			return null;
		}
	}

	private static void printUsage() {
		System.out
				.println("FlowDroid (c) Secure Software Engineering Group @ EC SPRIDE");
		System.out.println();
		System.out
				.println("Incorrect arguments: [0] = apk-file, [1] = android-jar-directory");
		System.out.println("Optional further parameters:");
		System.out.println("\t--TIMEOUT n Time out after n seconds");
		System.out
				.println("\t--SYSTIMEOUT n Hard time out (kill process) after n seconds, Unix only");
		System.out.println("\t--SINGLEFLOW Stop after finding first leak");
		System.out.println("\t--IMPLICIT Enable implicit flows");
		System.out.println("\t--NOSTATIC Disable static field tracking");
		System.out.println("\t--NOEXCEPTIONS Disable exception tracking");
		System.out.println("\t--APLENGTH n Set access path length to n");
		System.out.println("\t--CGALGO x Use callgraph algorithm x");
		System.out.println("\t--NOCALLBACKS Disable callback analysis");
		System.out
				.println("\t--LAYOUTMODE x Set UI control analysis mode to x");
		System.out
				.println("\t--ALIASFLOWINS Use a flow insensitive alias search");
		System.out.println("\t--NOPATHS Do not compute result paths");
		System.out
				.println("\t--AGGRESSIVETW Use taint wrapper in aggressive mode");
		System.out.println("\t--PATHALGO Use path reconstruction algorithm x");
		System.out.println("\t--LIBSUMTW Use library summary taint wrapper");
		System.out.println("\t--SUMMARYPATH Path to library summaries");
		System.out.println();
		System.out
				.println("Supported callgraph algorithms: AUTO, CHA, RTA, VTA, SPARK");
		System.out.println("Supported layout mode algorithms: NONE, PWD, ALL");
		System.out
				.println("Supported path algorithms: CONTEXTSENSITIVE, CONTEXTINSENSITIVE, SOURCESONLY");
	}

}
