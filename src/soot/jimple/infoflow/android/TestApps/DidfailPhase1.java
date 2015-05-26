package soot.jimple.infoflow.android.TestApps;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.Infoflow;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.infoflow.results.ResultSinkInfo;
import soot.jimple.infoflow.results.ResultSourceInfo;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.util.IntentTag;
import soot.jimple.internal.AbstractInstanceInvokeExpr;
import soot.jimple.internal.AbstractInvokeExpr;

public class DidfailPhase1 {
	private static final class DidfailResultHandler extends
			AndroidInfoflowResultsHandler {
		private BufferedWriter wr;

		private DidfailResultHandler() {
			this.wr = null;
		}

		private DidfailResultHandler(BufferedWriter wr) {
			this.wr = wr;
		}

		public String getMethSig(Stmt stmt) {
			if (!stmt.containsInvokeExpr()) {
				return "Stmt(" + stmt.toString() + ")";
			}
			AbstractInvokeExpr ie = (AbstractInvokeExpr) stmt.getInvokeExpr();
			SootMethod meth = ie.getMethod();
			return meth.getSignature();
		}

		public void handleResults(IInfoflowCFG cfg, InfoflowResults results) {
			Comparator<ResultSinkInfo> sinkSorter = new Comparator<ResultSinkInfo>() {
				public String sinkToString(ResultSinkInfo s) {
					String sig = getMethSig(s.getSink());
					String tag = "";
					if (s.getSink().hasTag("IntentID")) {
						tag = ((IntentTag) s.getSink().getTag("IntentID"))
								.getIntentID();
					}
					return sig + tag;
				}

				@Override
				public int compare(ResultSinkInfo o1, ResultSinkInfo o2) {
					return sinkToString(o1).compareTo(sinkToString(o2));
				}
			};

			Comparator<ResultSourceInfo> sourceSorter = new Comparator<ResultSourceInfo>() {
				public String sourceToString(ResultSourceInfo s) {
					String sig = getMethSig(s.getSource());
					return sig;
				}

				@Override
				public int compare(ResultSourceInfo o1, ResultSourceInfo o2) {
					return sourceToString(o1).compareTo(sourceToString(o2));
				}
			};

			if (results == null) {
				print("No results found.");
			} else {
				String pkg = escapeXML(this.getAppPackage());
				println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
				println("<results package=\"" + pkg + "\">");

				Set<ResultSinkInfo> sinks = results.getResults().keySet();
				List<ResultSinkInfo> sortedSinks = new ArrayList<ResultSinkInfo>(
						sinks);
				Collections.sort(sortedSinks, sinkSorter);

				for (ResultSinkInfo sinkInfo : sortedSinks) {
					Stmt sink = sinkInfo.getSink();
					String methSig = getMethSig(sink);
					println("<flow>");
					printf("\t<sink method=\"%s\"", escapeXML(methSig));
					if (Infoflow.isIntentSink(sink)) {

					}
					if (Infoflow.isIntentSink(sink)) {
						IntentTag tag = (IntentTag) sink.getTag("IntentID");
						String id = escapeXML(tag.getIntentID());
						printf(" is-intent=\"1\"");
						printf(" intent-id=\"%s\"", id);
						try {
							InvokeExpr ie = sink.getInvokeExpr();
							AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
							Type baseType = aie.getBase().getType();
							String cmp = escapeXML(baseType.toString());
							printf(" component=\"%s\"", cmp);
						} catch (Exception e) {
						}
					}
					if (Infoflow.isIntentResultSink(sink)) {
						print(" is-intent-result=\"1\"");
						SootMethod sm = cfg.getMethodOf(sink);
						SootClass cls = sm.getDeclaringClass();
						String cmp = escapeXML(cls.toString());
						printf(" component=\"%s\"", cmp);
					}
					println("></sink>");

					Set<ResultSourceInfo> sources = results.getResults().get(
							sinkInfo);
					List<ResultSourceInfo> sortedSources = new ArrayList<ResultSourceInfo>(
							sources);
					Collections.sort(sortedSources, sourceSorter);

					for (ResultSourceInfo srcInfo : sortedSources) {
						Stmt src = srcInfo.getSource();
						SootMethod sm = cfg.getMethodOf(src);
						String methName = sm.getName();
						methSig = getMethSig(srcInfo.getSource());
						printf("\t<source method=\"%s\"", escapeXML(methSig));

						if (methSig.indexOf(" getIntent()") != -1) {
							InvokeExpr ie = src.getInvokeExpr();
							AbstractInstanceInvokeExpr aie = (AbstractInstanceInvokeExpr) ie;
							Type baseType = aie.getBase().getType();
							String cmp = escapeXML(baseType.toString());
							printf(" component=\"%s\"", cmp);
						} else if (methSig.indexOf(":= @parameter") != -1) {
							SootClass cls = sm.getDeclaringClass();
							String cmp = escapeXML(cls.toString());
							printf(" component=\"%s\"", cmp);
						}
						printf(" in=\"%s\"", escapeXML(methName));
						println("></source>");
					}
					println("</flow>");
				}
				println("</results>");
			}
		}

		@Override
		public void onResultsAvailable(IInfoflowCFG cfg, InfoflowResults results) {
			try {
				handleResults(cfg, results);
			} finally {
				if (this.wr != null) {
					try {
						this.wr.close();
					} catch (Exception e) {
					}
				}
			}
		}

		public static String escapeXML(Object obj) {
			return escapeXML(obj.toString(), "");
		}

		public static String escapeXML(String str, String retIfNull) {
			/*
			 * Based on
			 * http://www.docjar.com/html/api/org/apache/commons/lang/Entities
			 * .java.html
			 */
			if (str == null) {
				return retIfNull;
			}
			StringWriter writer = new StringWriter();

			int len = str.length();
			for (int i = 0; i < len; i++) {
				char c = str.charAt(i);
				if (c > 0x7F) {
					writer.write("&#");
					writer.write(Integer.toString(c, 10));
					writer.write(';');
				} else {
					switch ((byte) c) {
					case '&':
						writer.write("&amp;");
						break;
					case '<':
						writer.write("&lt;");
						break;
					case '>':
						writer.write("&gt;");
						break;
					case '"':
						writer.write("&quot;");
						break;
					case '\'':
						writer.write("&apos;");
						break;
					default:
						writer.write(c);
					}
				}
			}
			return writer.toString();
		}

		private void printf(String format, Object... args) {
			try {
				System.out.printf(format, args);
				if (wr != null)
					wr.write(String.format(format, args));
			} catch (IOException ex) {
				// ignore
			}
		}

		private void println(String string) {
			try {
				System.out.println(string);
				if (wr != null)
					wr.write(string + "\n");
			} catch (IOException ex) {
				// ignore
			}
		}

		private void print(String string) {
			try {
				System.out.print(string);
				if (wr != null)
					wr.write(string);
			} catch (IOException ex) {
				// ignore
			}
		}
	}

	public static void usage() {
		System.err.println("Usage: [<outfile>] -- <flowdroid arguments>");
	}

	public static void main(final String[] args) throws IOException,
			InterruptedException {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}

		String[] fdArgs = {};
		String outFile = null;
		if (args[1].equals("--")) {
			outFile = args[0];
			fdArgs = Arrays.copyOfRange(args, 2, args.length);
		} else if (args[0].equals("--")) {
			fdArgs = Arrays.copyOfRange(args, 1, args.length);
		} else {
			usage();
			System.exit(1);
		}

		DidfailResultHandler handler;

		if (outFile != null) {
			File out = new File(outFile);
			FileWriter fw = new FileWriter(out);
			BufferedWriter bw = new BufferedWriter(fw);
			handler = new DidfailResultHandler(bw);
			handler = new DidfailResultHandler(bw);
		} else {
			handler = new DidfailResultHandler();
		}

		FlowDroid.run(fdArgs, handler);
	}
}
