package org.openimaj.squall.compile.rif;


import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.imports.profiles.RIFEntailmentImportProfiles;
import org.openimaj.squall.build.Builder;
import org.openimaj.squall.build.storm.StormStreamBuilder;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.data.IOperation;
import org.openimaj.squall.compile.rif.providers.predicates.ExternalLoader;
import org.openimaj.squall.orchestrate.OrchestratedProductionSystem;
import org.openimaj.squall.orchestrate.greedy.CombinedSourceGreedyOrchestrator;
import org.openimaj.util.data.Context;
import org.xml.sax.SAXException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public class TestRifRuleCompilerCombinedSourceGreedyOrchestratorStormBuilder {
	
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
			String typed = object.getTyped("rule");
			if (typed != null && typed.endsWith("lsbench-query-7.5")) 
				System.out.println(object);
		}

		@Override
		public void write(Kryo kryo, Output output) {}

		@Override
		public void read(Kryo kryo, Input input) {}

		@Override
		public boolean isStateless() {
			return true;
		}

		@Override
		public boolean forcedUnique() {
			return false;
		}
	}



	private RIFRuleSet simpleRules;
	private RIFRuleSet simplejoinRules;
	private RIFRuleSet complexjoinRules;
	private RIFRuleSet multiunionRules;
	private RIFRuleSet lsbench;

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
	 * @throws IOException 
	 * 
	 */
	@Before
	public void before() throws IOException{
		this.simpleRules = readRules("/test.simple.rule.rif");
		this.simplejoinRules = readRules("/test.simplejoin.rule.rif");
		this.complexjoinRules = readRules("/test.complexjoin.rule.rif");
		this.multiunionRules = readRules("/test.multiunion.rule.rif");
		this.lsbench = readRules("/lsbench/queries/queries.rif");
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleRulesBuilder(){
		testRuleSet(simpleRules);
	}
	
	/**
	 * 
	 */
	@Test
	public void testSimpleJoinBuilder(){
		testRuleSet(simplejoinRules);
	}
	
	/**
	 * 
	 */
	@Test
	public void testComplexRules(){
		testRuleSet(complexjoinRules);
	}
	
	/**
	 * 
	 */
	@Test
	public void testMultiUnionRules(){
		testRuleSet(multiunionRules);
	}
	
	/**
	 * 
	 */
	@Test
	public void testLSBenchRulesBuilder(){
		testRuleSet(lsbench);
	}
	
	private void testRuleSet(RIFRuleSet ruleSet) {
		ExternalLoader.loadExternals();
		
		IOperation<Context> op = new PrintAllOperation();

		RIFCoreRuleCompiler jrc = new RIFCoreRuleCompiler();
		CompiledProductionSystem comp = jrc.compile(ruleSet);
		
		CombinedSourceGreedyOrchestrator go = new CombinedSourceGreedyOrchestrator();
		OrchestratedProductionSystem orchestrated = go.orchestrate(comp, op );
		
		Builder builder = StormStreamBuilder.localClusterBuilder(-1);
		builder.build(orchestrated);
	}
	

}
