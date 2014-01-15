package org.openimaj.squall.compile.rif;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.imports.profiles.RIFEntailmentImportProfiles;
import org.openimaj.squall.build.Builder;
import org.openimaj.squall.build.storm.StormStreamBuilder;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.data.IOperation;
import org.openimaj.squall.compile.functions.rif.external.ExternalLoader;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.squall.orchestrate.greedy.CombinedSourceGreedyOrchestrator;
import org.openimaj.util.data.Context;
import org.xml.sax.SAXException;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestRifRuleCompilerCombinedSourceGreedyOrchestratorStormBuilder {
	
	@SuppressWarnings("serial")
	private static final class PrintAllOperation implements IOperation<Context>, Serializable {
		@Override
		public void setup() {
			System.out.println("Starting Test");
		}

		@Override
		public void cleanup() {
		}

		@Override
		public void perform(Context object) { 
			System.out.println(object);
		}
	}



	private RIFRuleSet readRules(String ruleSource) {
		RIFRuleSet rules = null;
		RIFEntailmentImportProfiles profs = new RIFEntailmentImportProfiles();
		try {
			InputStream resourceAsStream = TestRifRuleCompilerCombinedSourceGreedyOrchestratorStormBuilder.class.getResourceAsStream(ruleSource);
//			System.out.println(FileUtils.readall(resourceAsStream));
			rules = profs.parse(
					resourceAsStream,
					new URI("http://www.w3.org/ns/entailment/Core")
				);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return rules;
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleRulesBuilder(){
		testRuleSet(readRules("/test.simple.rule.rif"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleJoinBuilder(){
		testRuleSet(readRules("/test.simplejoin.rule.rif"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testComplexRules(){
		testRuleSet(readRules("/test.complexjoin.rule.rif"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testMultiUnionRules(){
		testRuleSet(readRules("/test.multiunion.rule.rif"));
	}
	
	/**
	 * 
	 */
	@Test
	public void testLSBenchRulesBuilder(){
		testRuleSet(readRules("/lsbench/queries/rif/query-7.5-with-small-test-data.rif"));
	}
	
	private void testRuleSet(RIFRuleSet ruleSet) {
		ExternalLoader.loadExternals();
		
		IOperation<Context> op = new PrintAllOperation();

		RIFCoreRuleCompiler jrc = new RIFCoreRuleCompiler();
		CompiledProductionSystem comp = jrc.compile(ruleSet);
		
		CombinedSourceGreedyOrchestrator go = new CombinedSourceGreedyOrchestrator();
		OrchestratedProductionSystem orchestrated = go.orchestrate(comp, op );
		
		Builder builder = StormStreamBuilder.localClusterBuilder(5000);
		builder.build(orchestrated);
	}
	

}
