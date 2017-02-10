package soot.jimple.infoflow.android.TestApps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xmlpull.v1.XmlPullParserException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.util.IntentTag;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;
import soot.util.MultiMap;

public class DidfailPhase1 {
	static class DidfailArgs {
		@Parameter
		private List<String> parameters = new ArrayList<String>();

		@Parameter(names = "-apk")
		private String apk;

		@Parameter(names = "-platforms")
		private String platforms;

		@Parameter(names = "-out")
		private String outfile;

		@Parameter(names = "-config")
		private String config = "fd.json";

		@Parameter(names = "-sourcesSinks")
		private String sourcesAndSinks;

		@Parameter(names = "-taintWrapper")
		private String taintWrapper;

		@Parameter(names = "-aggressiveTw", arity = 1)
		private boolean aggresive;
	}

	private static final class DidfailResultHandler extends AndroidInfoflowResultsHandler {
		private OutputStream os;
		private Document document;

		private DidfailResultHandler() {
			this.os = System.out;
		}

		private DidfailResultHandler(OutputStream os) {
			this.os = os;
		}

		public Element handleSink(ResultSinkInfo sinkInfo, IInfoflowCFG cfg, InfoflowResults results) {
			Stmt sink = sinkInfo.getSink();
			String methSig = getMethSig(sink);
			Element sinkElement = this.document.createElement("sink");
			sinkElement.setAttribute("method", methSig);

			if (methSig.startsWith("<android.content.ContentResolver:")) {
				sinkElement.setAttribute("contentprovider", "true");
				sinkElement.setAttribute("cpuri", Infoflow.extractContentProviderURI(sinkInfo.getSink(), cfg));
			}

			if (Infoflow.isIntentSink(sink)) {
				IntentTag tag = (IntentTag) sink.getTag("IntentID");
				sinkElement.setAttribute("is-intent", "1");
				sinkElement.setAttribute("intent-id", tag.getIntentID());

				try {
					InvokeExpr ie = sink.getInvokeExpr();
					AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
					Type baseType = aie.getBase().getType();
					sinkElement.setAttribute("component", baseType.toString());
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}

			if (Infoflow.isIntentResultSink(sink)) {
				SootMethod sm = cfg.getMethodOf(sink);
				SootClass cls = sm.getDeclaringClass();
				sinkElement.setAttribute("is-intent-result", "1");
				sinkElement.setAttribute("component", cls.toString());
			}

			if (Infoflow.isFileSink(sink)) {
				sinkElement.setAttribute("is-file", "1");
				sinkElement.setAttribute("filepath", Infoflow.extractFilePath(sink, cfg));
			} else {
				sinkElement.setAttribute("is-file", "0");
			}

			return sinkElement;
		}

		public Element handleSource(ResultSourceInfo srcInfo, ResultSinkInfo sinkInfo, IInfoflowCFG cfg,
				InfoflowResults results) {
			Stmt src = srcInfo.getSource();
			SootMethod sm = cfg.getMethodOf(src);
			String methName = sm.getName();
			String methSig = getMethSig(srcInfo.getSource());
			Element srcElement = this.document.createElement("source");
			srcElement.setAttribute("method", methSig);
			srcElement.setAttribute("in", methName);

			if (methSig.indexOf(" getIntent()") != -1) {
				InvokeExpr ie = src.getInvokeExpr();
				AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
				Type baseType = aie.getBase().getType();
				srcElement.setAttribute("component", baseType.toString());
			} else if (methSig.indexOf(":= @parameter") != -1) {
				SootClass cls = sm.getDeclaringClass();
				srcElement.setAttribute("component", cls.toString());
			}

			if (methSig.startsWith("<android.content.ContentResolver:")) {
				srcElement.setAttribute("contentprovider", "true");
				srcElement.setAttribute("cpuri", Infoflow.extractContentProviderURI(sinkInfo.getSink(), cfg));
			}

			if (Infoflow.isFileSource(src)) {
				srcElement.setAttribute("is-file", "1");
				srcElement.setAttribute("filepath", Infoflow.extractFilePath(src, cfg));
			} else {
				srcElement.setAttribute("is-file", "0");
			}

			return srcElement;
		}

		public String getMethSig(Stmt stmt) {
			if (!stmt.containsInvokeExpr()) {
				return "Stmt(" + stmt.toString() + ")";
			}
			AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
			SootMethod meth = ie.getMethod();
			return meth.getSignature();
		}

		private class SinkComparator implements Comparator<ResultSinkInfo> {
			public String sinkToString(ResultSinkInfo s) {
				String sig = getMethSig(s.getSink());
				String tag = "";
				if (s.getSink().hasTag("IntentID")) {
					tag = ((IntentTag) s.getSink().getTag("IntentID")).getIntentID();
				}
				return sig + tag;
			}

			@Override
			public int compare(ResultSinkInfo o1, ResultSinkInfo o2) {
				return sinkToString(o1).compareTo(sinkToString(o2));
			}
		}

		private class SourceComparator implements Comparator<ResultSourceInfo> {
			public String sourceToString(ResultSourceInfo s) {
				return getMethSig(s.getSource());
			}

			@Override
			public int compare(ResultSourceInfo o1, ResultSourceInfo o2) {
				return sourceToString(o1).compareTo(sourceToString(o2));
			}
		}

		public void buildDocument(IInfoflowCFG cfg, InfoflowResults results) {
			try {
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				this.document = docBuilder.newDocument();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				System.exit(-1);
			}

			Element rootElement = this.document.createElement("results");
			rootElement.setAttribute("package", this.getAppPackage());
			this.document.appendChild(rootElement);

			if (results == null) {
				System.err.println("[DIDFAIL] No flows to report.");
			}

			MultiMap<ResultSinkInfo, ResultSourceInfo> resultInfos;
			resultInfos = results.getResults();
			Comparator<ResultSinkInfo> sinkSorter = new SinkComparator();
			Comparator<ResultSourceInfo> sourceSorter = new SourceComparator();

			// Sort the sinks
			Set<ResultSinkInfo> sinkSet = results.getResults().keySet();
			List<ResultSinkInfo> sinks = new ArrayList<ResultSinkInfo>();
			sinks.addAll(sinkSet);
			Collections.sort(sinks, sinkSorter);

			for (ResultSinkInfo sinkInfo : sinks) {
				Element flowElement = this.document.createElement("flow");
				rootElement.appendChild(flowElement);
				flowElement.appendChild(this.handleSink(sinkInfo, cfg, results));

				// Sort the sourcesO
				Set<ResultSourceInfo> srcSet = resultInfos.get(sinkInfo);
				List<ResultSourceInfo> srcs = new ArrayList<ResultSourceInfo>();
				srcs.addAll(srcSet);
				Collections.sort(srcs, sourceSorter);

				for (ResultSourceInfo srcInfo : srcs) {
					flowElement.appendChild(this.handleSource(srcInfo, sinkInfo, cfg, results));
				}
			}
		}

		public void outputDocument(OutputStream out) throws IOException, TransformerException {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(new DOMSource(this.document), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			this.buildDocument(cfg, results);
			try {
				this.outputDocument(this.os);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

	public static void usage() {
		System.err.println("Usage: [<outfile>] -- <flowdroid arguments>");
	}

	private static String readFile(String pathname) throws IOException {
		File file = new File(pathname);
		StringBuilder fileContents = new StringBuilder((int) file.length());
		Scanner scanner = new Scanner(file);
		String lineSeparator = System.getProperty("line.separator");

		try {
			while (scanner.hasNextLine()) {
				fileContents.append(scanner.nextLine() + lineSeparator);
			}
			return fileContents.toString();
		} finally {
			scanner.close();
		}
	}

	public static void main(final String[] args) throws IOException, InterruptedException, XmlPullParserException {

		DidfailArgs jct = new DidfailArgs();
		new JCommander(jct, args);

		FlowDroidFactory factory = new FlowDroidFactory();
		SetupApplication app = factory.fromJson(readFile(jct.config), jct.apk, jct.platforms, jct.sourcesAndSinks,
				jct.taintWrapper, jct.aggresive);

		// Create the result handler
		DidfailResultHandler handler;

		OutputStream os = System.out;	
		if (jct.outfile != null && !jct.outfile.isEmpty()) {
			File out = new File(jct.outfile);
			os = new FileOutputStream(out);
		}
		handler = new DidfailResultHandler(os);
		handler.setAppPackage(app.getSourceSinkManager().getAppPackageName());
		app.runInfoflow(handler);
	}
}
