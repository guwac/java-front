package org.metaborg.java.conformance;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.metaborg.java.conformance.util.TermTools;
import org.metaborg.java.conformance.util.Util;
import org.metaborg.runtime.task.ITaskEngine;
import org.metaborg.runtime.task.TaskManager;
import org.metaborg.sunshine.environment.ServiceRegistry;
import org.metaborg.sunshine.environment.SunshineMainArguments;
import org.metaborg.sunshine.services.analyzer.AnalysisResult;
import org.metaborg.sunshine.services.analyzer.AnalysisService;
import org.spoofax.interpreter.library.IOAgent;
import org.spoofax.interpreter.library.index.IIndex;
import org.spoofax.interpreter.library.index.IndexManager;
import org.spoofax.interpreter.terms.IStrategoTerm;
import org.spoofax.interpreter.terms.ITermFactory;
import org.spoofax.jsglr.client.imploder.ImploderOriginTermFactory;
import org.spoofax.terms.TermFactory;

import com.beust.jcommander.JCommander;

public class Main {
	public static void main(String[] args) {
		final String projectDir = args[0];

		final String javaSourcePath = projectDir + args[1];
		final String javaUnitName = args[2];
		final String javaFile = javaSourcePath + javaUnitName;

		final String languageDir = args[3];
		final String javFile = projectDir + args[4];

		ResultLogger logger = new ResultLogger(projectDir, args[1] + args[2], true);

		try {
			// Get JDT results
			final CompilationUnit jdtAST = jdtFromJavaFile(javaSourcePath, javaUnitName, javaFile);

			// Get Spoofax Results
			final ITermFactory termFactory = new ImploderOriginTermFactory(new TermFactory());
			TermTools.factory = termFactory;

			final SpoofaxResult spxResult = spoofaxFromJavaFile(termFactory, languageDir, projectDir, javFile);

			// Do conformance check
			final Conformance conformance =
				new Conformance(jdtAST, spxResult.index, spxResult.taskEngine, spxResult.ast, logger);
			conformance.testCompilationUnit();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("deprecation")
	private static CompilationUnit jdtFromJavaFile(String sourcePath, String unitName, String file) throws IOException {
		final ASTParser parser = ASTParser.newParser(AST.JLS2);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(Util.getBytes(file));
		parser.setEnvironment(new String[] {}, new String[] { sourcePath }, null, true);
		parser.setUnitName(unitName);
		parser.setResolveBindings(true);
		parser.setStatementsRecovery(false);
		parser.setBindingsRecovery(false);
		return (CompilationUnit) parser.createAST(null);
	}

	private static SpoofaxResult spoofaxFromJavaFile(ITermFactory termFactory, String languageDir, String projectDir,
		String file) throws IOException {
		// Setup sunshine
		org.metaborg.sunshine.drivers.Main.jc = new JCommander();
		String[] sunshineArgs =
			new String[] { "--project", projectDir, "--auto-lang", languageDir, "--observer", "analysis-default-cmd",
				"--non-incremental" };
		SunshineMainArguments params = new SunshineMainArguments();
		final boolean argsFine = org.metaborg.sunshine.drivers.Main.parseArguments(sunshineArgs, params);
		if(!argsFine) {
			System.exit(1);
		}
		params.validate();
		org.metaborg.sunshine.drivers.Main.initEnvironment(params);

		// Analyze file
		final ServiceRegistry services = ServiceRegistry.INSTANCE();
		final AnalysisService analyzer = services.getService(AnalysisService.class);
		final Collection<AnalysisResult> analyzerResult =
			analyzer.analyze(Arrays.asList(new File[] { new File(file) }));
		final AnalysisResult result = analyzerResult.iterator().next();

		// Return results
		final IStrategoTerm ast = result.ast();
		final IOAgent agent = new IOAgent();
		final IIndex index = IndexManager.getInstance().loadIndex(projectDir, "Java", termFactory, agent);
		final ITaskEngine taskEngine = TaskManager.getInstance().loadTaskEngine(projectDir, termFactory, agent);
		return new SpoofaxResult(ast, index, taskEngine);
	}
}

class SpoofaxResult {
	public final IStrategoTerm ast;
	public final IIndex index;
	public final ITaskEngine taskEngine;

	public SpoofaxResult(IStrategoTerm ast, IIndex index, ITaskEngine taskEngine) {
		super();
		this.ast = ast;
		this.index = index;
		this.taskEngine = taskEngine;
	}
}
