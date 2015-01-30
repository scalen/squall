package org.openimaj.rifcore.imports.profiles;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.openimaj.rifcore.RIFRuleSet;
import org.xml.sax.SAXException;

public class TestRIFOwlRdfXmlImportHandler {
	
	@Test
	public void testOWLImport() throws URISyntaxException, FileNotFoundException, SAXException, IOException{
		URI profileURI = new URI("http://www.w3.org/ns/entailment/OWL-RDF-Based");
		
		RIFRuleSet ruleSet = new RIFRuleSet(profileURI, new RIFOWLImportProfiles());
		
		URI resourceURI = new URI("java:///Test.owl");
		
		ruleSet.addImport(resourceURI, profileURI);
		
		System.out.println(ruleSet.toString());
	}
	
}
