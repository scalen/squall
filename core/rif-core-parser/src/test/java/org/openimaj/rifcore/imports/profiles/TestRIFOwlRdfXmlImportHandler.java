package org.openimaj.rifcore.imports.profiles;

import java.io.FileInputStream;
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
		RIFRuleSet ruleSet = new RIFRuleSet(new URI("http://www.w3.org/ns/entailment/OWL-RDF-Based"), new RIFOWLImportProfiles());
		
		RIFOwlRdfXmlImportHandler handler = new RIFOwlRdfXmlImportHandler();
		
		ruleSet = handler.importToRuleSet(new FileInputStream("/Users/david.monks/squall/core/rif-core-parser/src/test/resources/Test.owl"), ruleSet);
		
		System.out.println(ruleSet.toString());
	}
	
}
