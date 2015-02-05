package org.openimaj.squall.tool.modes.translator;

import java.net.URI;
import java.net.URISyntaxException;

import org.kohsuke.args4j.Option;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.imports.profiles.RIFOWLImportProfiles;
import org.openimaj.rifcore.utils.RifUtils;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.rif.RIFCoreRuleCompiler;
import org.openimaj.squall.compile.rif.providers.predicates.ExternalLoader;
import org.openimaj.squall.tool.SquallToolOptions;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class OWLTranslatorMode extends TranslatorMode {

	private enum Profile {
		
		RDFXML("http://www.w3.org/ns/entailment/OWL-RDF-Based")
		;
		
		private URI profile;
		private Profile(String profileURI){
			try {
				this.profile = new URI(profileURI);
			} catch (URISyntaxException e) {
				throw new Error("Incorrectly defined profile enum value.", e);
			}
		}
		
		public URI getURI(){
			return this.profile;
		}
	}
	
	/**
	 * The rif document uri for the rules to load
	 */
	@Option(
			name = "--owl-ontology",
			aliases = "-owlo",
			required = true,
			usage = "Load the owl ontology from this URI",
			metaVar = "STRING")
	public String owlOntURI = null;
	
	/**
	 * the rif profile according to which the rif document to load should be interpretted.
	 */
	@Option(
			name = "--owl-profile",
			aliases = "-owlp",
			required = false,
			usage = "Load the rif rules according to this profile specification")
	public Profile owlProfile = Profile.RDFXML;
	
	@Override
	public CompiledProductionSystem cps() {
		RIFCoreRuleCompiler rif = new RIFCoreRuleCompiler();
		return rif.compile(createRifRuleSet());
	}

	private RIFRuleSet createRifRuleSet() {
		RIFRuleSet readRules = RifUtils.readRules(owlOntURI, owlProfile.getURI(), new RIFOWLImportProfiles());
		return readRules;
	}
	
	@Override
	public void setup(SquallToolOptions opts) {
		ExternalLoader.loadExternals();	
	}

}
