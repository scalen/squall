package org.openimaj.squall.compile.rif;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.imports.profiles.RIFEntailmentImportProfiles;
import org.openimaj.rifcore.imports.profiles.RIFImportProfiles.ProfileNotSupportedException;
import org.openimaj.squall.build.Builder;
import org.openimaj.squall.build.storm.StormStreamBuilder;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.CountingOperation;
import org.openimaj.squall.compile.data.IOperation;
import org.openimaj.squall.compile.rif.providers.predicates.ExternalLoader;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.squall.orchestrate.rete.CombinedSourceTPLOReteOrchestrator;
import org.openimaj.util.data.Context;
import org.openimaj.util.data.ContextKey;
import org.xml.sax.SAXException;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @author David Monks &lt;dm11g08@ecs.soton.ac.uk&gt;
 *
 */
public class TestRifRuleCompilerCombinedSourceTPLOReteOrchestratorStormBuilder {

	private RIFRuleSet readRules(String ruleSource) {
		RIFRuleSet rules = null;
		RIFEntailmentImportProfiles profs = new RIFEntailmentImportProfiles();
		try {
			InputStream resourceAsStream = TestRifRuleCompilerCombinedSourceTPLOReteOrchestratorStormBuilder.class.getResourceAsStream(ruleSource);
//			System.out.println(FileUtils.readall(resourceAsStream));
			rules = profs.parse(
					resourceAsStream,
					new URI("http://www.w3.org/ns/entailment/Core")
				);
		} catch (IOException | SAXException | URISyntaxException | ProfileNotSupportedException e) {
			e.printStackTrace();
		}
		return rules;
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleRulesBuilder(){
		IOperation<Context> op = new CountingOperation(2);
		testRuleSet(readRules("/test.simple.rule.rif"), op, 5000);
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleJoinBuilder(){
		IOperation<Context> op = new CountingOperation(1);
		testRuleSet(readRules("/test.simplejoin.rule.rif"), op, 5000);
	}
	
	/**
	 * 
	 */
	@Test
	public void testComplexRules(){
		IOperation<Context> op = new CountingOperation(1);
		testRuleSet(readRules("/test.complexjoin.rule.rif"), op, 5000);
	}
	
	/**
	 * 
	 */
	@Test
	public void testMultiUnionRules(){
		IOperation<Context> op = new CountingOperation(3);
		testRuleSet(readRules("/test.multiunion.rule.rif"), op, 5000);
	}
	
	/**
	 * 
	 */
	@Test
	public void testAllRules(){
		IOperation<Context> op = new CountingOperation(7);
		testRuleSet(readRules("/test.all.rif"), op, 5000);
	}
	
	/**
	 * 
	 */
	@Test
	public void testLSBenchRulesBuilder(){
		Map<String, String> filters = new HashMap<String, String>();
		filters.put(ContextKey.RULE_KEY.toString(), ".*lsbench-query.*");
		IOperation<Context> op = new CountingOperation(2, filters);
		testRuleSet(readRules("/lsbench/queries/rif/query-7.5-with-small-test-data.rif"), op, 5000);
	}
	
	private void testRuleSet(RIFRuleSet ruleSet, IOperation<Context> op, int sleep) {
		ExternalLoader.loadExternals();

		RIFCoreRuleCompiler jrc = new RIFCoreRuleCompiler();
		CompiledProductionSystem comp = jrc.compile(ruleSet);
		
		CombinedSourceTPLOReteOrchestrator go = new CombinedSourceTPLOReteOrchestrator();
		OrchestratedProductionSystem orchestrated = go.orchestrate(comp, op );
		
		Builder builder = StormStreamBuilder.localClusterBuilder(sleep);
		builder.build(orchestrated);
	}
	

}
