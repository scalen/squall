package org.openimaj.rifcore.imports.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.conditions.RIFExternal;
import org.openimaj.rifcore.conditions.atomic.RIFError;
import org.openimaj.rifcore.conditions.atomic.RIFFrame;
import org.openimaj.rifcore.conditions.data.RIFConst;
import org.openimaj.rifcore.conditions.data.RIFDatum;
import org.openimaj.rifcore.conditions.data.RIFIRIConst;
import org.openimaj.rifcore.conditions.data.RIFLocalConst;
import org.openimaj.rifcore.conditions.data.RIFLocaleStringConst;
import org.openimaj.rifcore.conditions.data.RIFStringConst;
import org.openimaj.rifcore.conditions.data.RIFURIConst;
import org.openimaj.rifcore.conditions.data.RIFVar;
import org.openimaj.rifcore.conditions.data.RIFXSDTypedConst;
import org.openimaj.rifcore.conditions.formula.RIFAnd;
import org.openimaj.rifcore.conditions.formula.RIFAnon;
import org.openimaj.rifcore.conditions.formula.RIFEqual;
import org.openimaj.rifcore.conditions.formula.RIFExists;
import org.openimaj.rifcore.conditions.formula.RIFFormula;
import org.openimaj.rifcore.conditions.formula.RIFMember;
import org.openimaj.rifcore.conditions.formula.RIFOr;
import org.openimaj.rifcore.rules.RIFForAll;
import org.openimaj.rifcore.rules.RIFGroup;
import org.openimaj.rifcore.rules.RIFRule;
import org.openimaj.rifcore.rules.RIFSentence;
import org.openimaj.rifcore.rules.RIFStatement;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.hp.hpl.jena.util.FileManager;

/**
 * @author david.monks
 *
 */
public class RIFOwlRdfXmlImportHandler implements RIFEntailmentImportHandler {

	@Override
	public RIFRuleSet importToRuleSet(InputSource loc, RIFRuleSet ruleSet)
			throws SAXException, IOException {
		String fileNameOrUri = loc.toString();
		InputStream is = FileManager.get().open(fileNameOrUri);
		
		if (is != null) {
			ruleSet = importToRuleSet(is, ruleSet);
		} else {
	        System.err.println("cannot read " + fileNameOrUri);
	    }
		return ruleSet;
	}
	
	@Override
	public RIFRuleSet importToRuleSet(InputStream loc, RIFRuleSet ruleSet) throws SAXException, IOException {
		OntologyCompiler docComp = new OntologyCompiler(loc);
		
		ruleSet = docComp.compile(ruleSet);
		
		return ruleSet;
	}

	@Override
	public RIFRuleSet importToRuleSet(URI loc, RIFRuleSet ruleSet) throws SAXException, IOException {
		return importToRuleSet(new InputSource(loc.toASCIIString()), ruleSet);
	}

}

abstract class OWLTranslater <IO> {
	
	private static final Logger logger = Logger.getLogger(OWLTranslater.class);
	
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	
	public abstract IO compile (IO io);
	
	protected RIFMember constructRIFMemeber(RIFDatum object, String className){
		try {
			URI classURI = new URI(className);
			
			RIFIRIConst classIRI = new RIFIRIConst();
			classIRI.setData(classURI);
			
			return constructRIFMemeber(object, classIRI);
		} catch (URISyntaxException e) {
			throw new RuntimeException("class iri "+ className +" invalid: ", e);
		}
	}
	
	protected RIFMember constructRIFMemeber(RIFDatum object, RIFDatum classIRI){
		if (!(classIRI instanceof RIFIRIConst)){
			return constructRIFMemeber(object, classIRI.getNode().toString());
		}
		RIFMember typeStatement = new RIFMember();
		typeStatement.setInstance(object);
		typeStatement.setInClass(classIRI);
		
		return typeStatement;
	}
	
	protected RIFFrame constructOWLSameAs(RIFDatum subject, RIFDatum object){
		try {
			RIFIRIConst sameAs = new RIFIRIConst().setData(new URI(OWL_PREFIX + "sameAs"));
			
			RIFFrame frame = new RIFFrame();
			
			frame.setSubject(subject);
			frame.setPredicate(sameAs);
			frame.setObject(object);
			
			return frame;
		} catch (URISyntaxException e) {
			throw new RuntimeException("SHOULD NEVER HAPPEN (owl:sameAs is a fixed, valid URI)", e);
		}
	}
	
 	protected List<Element> getChildElementsByTagNameNS(Element parent, String namespace, String tag){
		List<Element> result = new ArrayList<Element>();
		
		NodeList children = parent.getChildNodes();
		Node child = children.item(0);
		if (child != null){
			for (int i = 1; i < children.getLength(); child = children.item(i++)){
				if (child.getLocalName() == null ? child.getNodeName().endsWith(tag) : child.getLocalName().equals(tag)){
					if (child.getLocalName() == null ? child.getNodeName().startsWith(namespace) : child.getNamespaceURI().equals(namespace)){
						if (child.getNodeType() == Node.ELEMENT_NODE){
							result.add((Element) child);
						}
					}
				}
			}
		}
		
		return result;
	}

 	protected RIFGroup addRule(RIFGroup rules, Collection<RIFVar> instances, RIFFormula head, RIFFormula body){
		RIFRule rule = new RIFRule();
		rule.setHead(head);
		rule.setBody(body);
		
		if (instances.isEmpty()){
			rules.addSentence(rule);
		} else {
			RIFForAll forall = new RIFForAll();
			for (RIFVar var : instances){
				forall.addUniversalVar(var);
			}
			forall.setStatement(rule);
			
			rules.addSentence(forall);
		}
		return rules;
	}
 	
 	protected RIFFormula flattenFormula(RIFFormula formula){
		if (formula instanceof RIFAnd){
			RIFAnd flattenedFormula = new RIFAnd();
			
			for (RIFFormula subformula : (RIFAnd) formula){
				RIFFormula sf = flattenFormula(subformula);
				if (sf != null){
					flattenedFormula.addFormula(sf);
				}
			}
			Iterator<RIFFormula> andList = flattenedFormula.iterator();
			if (andList.hasNext()){
				RIFFormula first = andList.next();
				if (!andList.hasNext()){
					return first;
				}
			} else {
				return null;
			}
			return flattenedFormula;
		} else if (formula instanceof RIFOr){
			RIFOr flattenedFormula = new RIFOr();
			
			for (RIFFormula subformula : (RIFOr) formula){
				RIFFormula sf = flattenFormula(subformula);
				if (sf != null){
					flattenedFormula.addFormula(sf);
				}
			}
			Iterator<RIFFormula> orList = flattenedFormula.iterator();
			if (orList.hasNext()){
				RIFFormula first = orList.next();
				if (!orList.hasNext()){
					return first;
				}
			} else {
				return null;
			}
			return flattenedFormula;
		} else if (formula instanceof RIFExists) {
			RIFFormula f = flattenFormula(((RIFExists) formula).getFormula());
			if (f == null){
				return null;
			} else {
				((RIFExists) formula).addFormula(f);
			}
		}
		
		return formula;
	}
	
	protected static class InvalidRIFRuleException extends Exception {
		public InvalidRIFRuleException(){
			super();
		}
		public InvalidRIFRuleException(String message){
			super(message);
		}
	}
	
	protected static class InvalidRIFAxiomException extends Exception {
		public InvalidRIFAxiomException(){
			super();
		}
		public InvalidRIFAxiomException(String message){
			super(message);
		}
	}
	
	protected Collection<RIFRule> constructValidRules(RIFFormula head, RIFFormula body) throws InvalidRIFRuleException{
		Collection<RIFRule> rules = new HashSet<RIFRule>();
		
		boolean valid = true;
		if (head instanceof RIFAnd){
			head = flattenFormula((RIFAnd) head);
		}
		if (head == null){
			throw new InvalidRIFRuleException("head to the rule with body " + body.toString() + " is empty.  In RIF, constraints have a head containing a rif:error.");
		} else if (head instanceof RIFAnd){
			for (RIFFormula h : (RIFAnd) head){
				if (h instanceof RIFOr
						|| h instanceof RIFExists
						|| h instanceof RIFExternal
						|| h instanceof RIFEqual){
					valid = false;
				}
			}
		} else if (head instanceof RIFOr
							|| head instanceof RIFExists
							|| head instanceof RIFExternal
							|| head instanceof RIFEqual){
			valid = false;
		}
		
		if (!valid){
			throw new InvalidRIFRuleException("head "+head.toString() + " is not a valid RIFCore head formula.");
		}
		
		body = flattenFormula(body);
		if (body == null){
			throw new InvalidRIFRuleException("body of rule with head "+head.toString() + " is empty.");
		} else if (body instanceof RIFOr){
			valid = false;
//			Collection<InvalidRIFRuleException> exceptions = new HashSet<InvalidRIFRuleException>();
			for (RIFFormula b : (RIFOr) body){
				if (head instanceof RIFAnd){
					for (RIFFormula h : (RIFAnd) head){						
						try {
							rules.add(constructValidRule(h, b));
							valid = true;
						} catch (InvalidRIFRuleException e){
							logger.info("Rule "+head.toString()+" :- "+b.toString()+" is not a valid/necessary RIFCore rule.", e);
//					exceptions.add(e);
						}
					}
				} else {
					try {
						rules.add(constructValidRule(head, b));
						valid = true;
					} catch (InvalidRIFRuleException e){
						logger.info("Rule "+head.toString()+" :- "+b.toString()+" is not a valid/necessary RIFCore rule.", e);
//				exceptions.add(e);
					}
				}
			}
			if (!valid){
				throw new InvalidRIFRuleException("body "+body.toString()+" contains only matches to head "+head.toString());
			}
		} else {
			if (head instanceof RIFAnd){
				for (RIFFormula h : (RIFAnd) head){
					rules.add(constructValidRule(h, body));
				}
			} else {
				rules.add(constructValidRule(head, body));
			}
		}
		
		return rules;
	}
	
	protected RIFRule constructValidRule(RIFFormula head, RIFFormula body) throws InvalidRIFRuleException{
		body = flattenFormula(body);
		
		if (body == null){
			throw new InvalidRIFRuleException("body of rule with head "+head.toString() + "is empty.");
		} else if (body instanceof RIFAnd){
			for (RIFFormula b : (RIFAnd) body){
				if (b.equals(head)){
					throw new InvalidRIFRuleException("body part "+b.toString()+" matches head "+head.toString());
				}
			}
		} else if (body.equals(head)){
			throw new InvalidRIFRuleException("body "+body.toString()+" matches head "+head.toString());
		}
		
		RIFRule rule = new RIFRule();
		rule.setHead(head);
		rule.setBody(body);
		
		return rule;
	}

	protected Collection<RIFStatement> constructValidAxioms(RIFFormula complexStatement) throws InvalidRIFAxiomException{
		Collection<RIFStatement> axioms = new HashSet<RIFStatement>();
		
		if (complexStatement instanceof RIFAnd){
			for (RIFFormula sub : (RIFAnd) complexStatement){
				try {
					axioms.addAll(constructValidAxioms(sub));
				} catch (InvalidRIFAxiomException e){
					logger.info("Axiom " + sub.toString() + " is not a valid RIFCore axiom, so has been ignored.", e);
				}
			}
		} else if (complexStatement instanceof RIFStatement){
			axioms.add((RIFStatement) complexStatement);
		} else {
			throw new InvalidRIFAxiomException("Axioms must be definite and concrete in RIFCore.");
		}
		
		return axioms;
	}
	
}

class OntologyCompiler extends OWLTranslater<RIFRuleSet> {
	
	private static final Logger logger = Logger.getLogger(OntologyCompiler.class);
	
	private Element rdf;
	
	public OntologyCompiler(InputStream stream) throws SAXException, IOException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(stream);
			
			doc.getDocumentElement().normalize();
			
			rdf = (Element) doc.getElementsByTagNameNS(RDF_PREFIX, "RDF").item(0);
			
			if (rdf == null){
				throw new Error("Missing RDF element in RDF/XML ontology specification");
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("SHOULD NOT HAPPEN, parser configured staticly.", e);
		}
	}
	
	@Override
	public RIFRuleSet compile(RIFRuleSet ruleSet){
		ruleSet = compilePrefixes(ruleSet);
		ruleSet = compileImports(ruleSet);
		
		RIFGroup rules = new RIFGroup();
		
		rules.addSentence(compileClassDescriptions());
		
		rules.addSentence(compilePropertyDescriptions());
		
		rules.addSentence(compileIndividualDescriptions());
		
		ruleSet.addRootGroup(rules);
		
		return ruleSet;
	}
	
	private RIFRuleSet compilePrefixes(RIFRuleSet ruleSet){
		Node attr = rdf.getAttributes().item(0);
		if (attr != null){
			for (int i = 1; i < rdf.getAttributes().getLength(); attr = rdf.getAttributes().item(i++)){
				Attr attribute = (Attr) attr;
				if (attribute.getName().startsWith("xmlns:")){
					try {
						ruleSet.addPrefix(attribute.getName().substring(6), new URI(attribute.getValue()));
					} catch (URISyntaxException e) {
						logger.error("Prefix location <" + attribute.getValue() + "> is not a valid URI.", e);
					}
				}
			}
		}
		
		return ruleSet;
	}
	
	private RIFRuleSet compileImports(RIFRuleSet ruleSet){
		Collection<Element> ontologies = getChildElementsByTagNameNS((Element) rdf, OWL_PREFIX, "Ontology");
		for (Element o : ontologies){
			Collection<Element> imports = getChildElementsByTagNameNS((Element) o, OWL_PREFIX, "imports");
			for (Element i : imports){
				String loc = i.getAttributeNS(RDF_PREFIX, "resource");
				try {
					ruleSet.addImport(new URI(loc), null);
				} catch (URISyntaxException e) {
					logger.error("Import location <" + loc + "> is not a valid URI.", e);
				}
			}
		}
		
		return ruleSet;
	}
	
	private RIFGroup compileClassDescriptions() {
		RIFGroup classRules = new RIFGroup();
		
		Collection<Element> classes = getChildElementsByTagNameNS(rdf, OWL_PREFIX, "Class");
		classes.addAll(getChildElementsByTagNameNS(rdf, OWL_PREFIX, "Restriction"));
		for (Element c :classes){
			RIFVar instance = new RIFVar().setName("instance");
			OWLClassCompiler cc = new OWLClassCompiler(c, instance);
			classRules = cc.compile(classRules);
		}
		
		return cleanRIFCoreRules(classRules);
	}
	
	private RIFGroup cleanRIFCoreRules(RIFGroup rules){
		RIFGroup cleanRules = new RIFGroup();
		
		for (RIFSentence sentence : rules){
			if (sentence instanceof RIFGroup){
				cleanRules.addSentence(cleanRIFCoreRules((RIFGroup) sentence));
			} else {
				RIFSentence statement;
				if (sentence instanceof RIFForAll) {
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence; 
				}
				
				if (statement instanceof RIFRule){
					RIFRule rule = (RIFRule) statement;
					
					try {
						Collection<RIFRule> validRules = constructValidRules(rule.getHead(), rule.getBody());
						
						for (RIFRule r : validRules){
							if (sentence instanceof RIFForAll) {
								RIFForAll newSentence = new RIFForAll();
								for (RIFVar var : ((RIFForAll) sentence).universalVars()){
									newSentence.addUniversalVar(var);
								}
								newSentence.setStatement(r);
								cleanRules.addSentence(newSentence);
							} else {
								cleanRules.addSentence(r);
							}
						}
					} catch (InvalidRIFRuleException e) {
						logger.info("Rule "+rule.getHead().toString()+" :- "+rule.getBody().toString()+" is not a valid/necessary RIFCore rule.", e);
					}
				} else {
					cleanRules.addSentence(sentence);
				}
			}
		}
		
		return cleanRules;
	}
	
	private RIFGroup compilePropertyDescriptions() {
		RIFGroup propertyRules = new RIFGroup();
		
		//TODO
		
		return cleanRIFCoreRules(propertyRules);
	}
	
	private RIFGroup compileIndividualDescriptions() {
		RIFGroup individualRules = new RIFGroup();
		
		//TODO
		
		return cleanRIFCoreRules(individualRules);
	}
	
}

class OWLClassCompiler extends OWLTranslater<RIFGroup> {

	private static final Logger logger = Logger.getLogger(OWLClassCompiler.class);
	
	protected final Element clazz;
	protected RIFDatum instance;
	protected RIFMember classMembership;
	protected RIFOr subClassDescriptions = new RIFOr();
	protected RIFAnd superClassDescriptions = new RIFAnd();
	
	public OWLClassCompiler(Element clazz, RIFDatum instance){
		this.clazz = clazz;
		this.instance = instance;
		
		// get class URI
		String className = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className);
		} else {
			classMembership = null;
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		return this.compileProper(rules);
	}
	
	protected RIFGroup compileProper(RIFGroup rules){
		// if class has been described in relation to a property with a conrete subject/object, or as the type of a described individual...
		if (classMembership != null && !(instance instanceof RIFVar)){
			rules.addSentence(classMembership);

			String variableSub = instance.getNode().toString();
			instance = new RIFVar().setName(variableSub);
			classMembership = constructRIFMemeber(instance, classMembership.getInClass());
		}
		
		// Equivalent Classes
		Collection<Element> equivalentClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "equivalentClass");
		for (Element equivClass : equivalentClasses){
			rules = compileEquivalentClass(rules, equivClass);
		}
		
		// Subclasses
		Collection<Element> superClasses = getChildElementsByTagNameNS(clazz, RDFS_PREFIX, "subClassOf");
		for (Element superClass : superClasses){
			rules = compileSuperClass(rules, superClass);
		}
		
		Collection<Element> classIntersections = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "intersectionOf");
		Collection<Element> classUnions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "unionOf");
		Collection<Element> complementClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "complementOf");
		
		Collection<Element> existentialRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "someValuesFrom");
		Collection<Element> universalRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "allValuesFrom");
		Collection<Element> valueRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "hasValue");
		
//		Collection<Element> cardinalityRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "cardinality");
//		Collection<Element> qualifiedCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "qualifiedCardinality");
		Collection<Element> maxCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "maxCardinality");
		Collection<Element> minCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "minCardinality");
		Collection<Element> maxQualCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "maxQualifiedCardinality");
		Collection<Element> minQualCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "minQualifiedCardinality");
		
		if (!classIntersections.isEmpty()){
			rules = compileIntersections(rules, classIntersections);
		} else if (!classUnions.isEmpty()){
			rules = compileUnions(rules, classUnions);
		} else if (!complementClasses.isEmpty()){
			// handled along with disjoint classes
		} else if (!existentialRestrictions.isEmpty()){
			rules = compileExistentialRestrictions(rules, existentialRestrictions);
		} else if (!universalRestrictions.isEmpty()){
			rules = compileUniversalRestrictions(rules, universalRestrictions);
		} else if (!valueRestrictions.isEmpty()){
			rules = compileValueRestrictions(rules, valueRestrictions);
		// Not valid in OWL 2 RL because 
//		} else if (!cardinalityRestrictions.isEmpty()){
//			rules = compileCardinalityRestrictions(rules, cardinalityRestrictions);
//		} else if (!qualifiedCardRestrictions.isEmpty()){
//			rules = compileQualCardRestrictions(rules, qualifiedCardRestrictions);
		} else if (!maxCardRestrictions.isEmpty()){
			rules = compileMaxCardRestrictions(rules, maxCardRestrictions);
		} else if (!minCardRestrictions.isEmpty()){
			rules = compileMinCardRestrictions(rules, minCardRestrictions);
		} else if (!maxQualCardRestrictions.isEmpty()){
			rules = compileMaxQualCardRestrictions(rules, maxQualCardRestrictions);
		} else if (!minQualCardRestrictions.isEmpty()){
			rules = compileMinQualCardRestrictions(rules, minQualCardRestrictions);
		}
		
		// Disjoint Classes
		// Not strictly accurate outside of OWL 2 RL entailments, but within that scope they are treated the same
		complementClasses.addAll(getChildElementsByTagNameNS(clazz, OWL_PREFIX, "disjointWith"));
		rules = compileComplements(rules, complementClasses);
		
		// After establishing the class description and the hierarchy directly above and below the class, express subsumption rules concerning the class
		// If anonymous, all subclasses imply all superclasses...
		if (classMembership == null) { 
			if (instance instanceof RIFVar) {
				for (RIFFormula body : subClassDescriptions){
					for (RIFFormula head : superClassDescriptions){
						addRule(rules, head, body);
					}
				}
			} else {
				// ... unless the instance is an individual not a variable, in which case, all definite concrete superclass statements can be stated about that individual
				try {
					for (RIFStatement axiom : constructValidAxioms(superClassDescriptions)) {
						rules.addSentence(axiom);
					}
				} catch (org.openimaj.rifcore.imports.profiles.OWLTranslater.InvalidRIFAxiomException e) {
					logger.info("Complex axiom " + superClassDescriptions.toString() + " is not a valid RIFCore axiom, so has been ignored.", e);
				}
			}
		} else {
		// If named, all subclasses imply this class, and this class implies all superclasses
			for (RIFFormula body : subClassDescriptions){
				addRule(rules, classMembership, body);
			}
			for (RIFFormula head : superClassDescriptions){
				addRule(rules, head, classMembership);
			}
		}
		
		return rules;
	}

	// Subsumption Compilation methods

	private RIFGroup compileEquivalentClass(RIFGroup rules, Element equivClass) {
		String equivClassName = equivClass.getAttributeNS(RDF_PREFIX, "resource");
		if (equivClassName.length() > 0){
			RIFMember equivClassMembership = constructRIFMemeber(instance, equivClassName);
			
			subClassDescriptions.addFormula(equivClassMembership);
			superClassDescriptions.addFormula(equivClassMembership);
		} else if (equivClass.hasChildNodes()){
			Collection<Element> classes = getChildElementsByTagNameNS(equivClass, OWL_PREFIX, "Class");
			classes.addAll(getChildElementsByTagNameNS(equivClass, OWL_PREFIX, "Restriction"));
			for (Element ec : classes){
				OWLEquivalentClassCompiler scc = new OWLEquivalentClassCompiler(
														ec, instance, classMembership, subClassDescriptions, superClassDescriptions);
				rules = scc.compile(rules);
			}
		} else {
			throw new RuntimeException("owl:equivalentClass statement is missing an object, either reference or description.");
		}
		return rules;
	}

	private RIFGroup compileSuperClass(RIFGroup rules, Element superClass) {
		String superClassName = superClass.getAttributeNS(RDF_PREFIX, "resource");
		if (superClassName.length() > 0){
			superClassDescriptions.addFormula(constructRIFMemeber(instance, superClassName));
		} else if (superClass.hasChildNodes()){
			Collection<Element> classes = getChildElementsByTagNameNS(superClass, OWL_PREFIX, "Class");
			classes.addAll(getChildElementsByTagNameNS(superClass, OWL_PREFIX, "Restriction"));
			for (Element sc : classes){
				OWLSuperClassCompiler scc = new OWLSuperClassCompiler(sc, instance, classMembership, subClassDescriptions, superClassDescriptions);
				rules = scc.compile(rules);
			}
		} else {
			throw new RuntimeException("rdfs:subClassOf statement is missing an object, either reference or description.");
		}
		return rules;
	}

	// Class Description Compilation methods
	
	private RIFGroup compileIntersections(RIFGroup rules, Collection<Element> classIntersections) {
		for (Element intersection : classIntersections){
			RIFAnd and = null;
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(intersection, RDF_PREFIX, "Description");
			if (!classReferences.isEmpty()) {
				and = new RIFAnd();
				
				for (Element reference : classReferences){
					String referenceName = reference.getAttributeNS(RDF_PREFIX, "about");
					 
					RIFMember intersectedClassMembership = constructRIFMemeber(instance, referenceName);
					
					// class membership of a class intersection implies membership of every intersected class
					superClassDescriptions.addFormula(intersectedClassMembership);
					// class membership of all intersected classes implies membership of the class intersection 
					and.addFormula(intersectedClassMembership);
				}
			}
			
			Set<RIFAnd> ands = new HashSet<RIFAnd>();
			if (and != null){
				ands.add(and);
			}
			
			Collection<Element> classes = getChildElementsByTagNameNS(intersection, OWL_PREFIX, "Class");
			classes.addAll(getChildElementsByTagNameNS(intersection, OWL_PREFIX, "Restriction"));
			for (Element ic : classes){
				OWLIntersectionMemberCompiler imc = new OWLIntersectionMemberCompiler(
								ic,
								instance,
								superClassDescriptions,
								ands);
				rules = imc.compile(rules);
			}
			// add all possible intersections to the set of bodies implying membership of this class
			for (RIFAnd a : ands){
				subClassDescriptions.addFormula(a);
			}
		}
		return rules;
	}

	private RIFGroup compileUnions(RIFGroup rules, Collection<Element> classUnions) {
		for (Element union : classUnions){
			RIFOr or = new RIFOr();
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(union, RDF_PREFIX, "Description");
			for (Element reference : classReferences){
				String referenceName = reference.getAttributeNS(RDF_PREFIX, "about");
				 
				RIFMember unionMembership = constructRIFMemeber(instance, referenceName);
				
				// class membership of any of the unioned classes implies membership of the class union
				subClassDescriptions.addFormula(unionMembership);
				// class membership of the class union implies membership of at least one of the unioned classes
				or.addFormula(unionMembership);
			}
			
			Collection<Element> classes = getChildElementsByTagNameNS(union, OWL_PREFIX, "Class");
			classes.addAll(getChildElementsByTagNameNS(union, OWL_PREFIX, "Restriction"));
			if (!classes.isEmpty()){
				for (Element uc : classes){
					OWLUnionMemberCompiler umc = new OWLUnionMemberCompiler(uc, instance, subClassDescriptions, or);
					rules = umc.compile(rules);
				}
			}
			
			// add the union to the set of heads implied by membership of this class, and the class description
			superClassDescriptions.addFormula(or);
		}
		return rules;
	}
	
	private RIFGroup compileComplements(RIFGroup rules, Collection<Element> complementClasses) {
		for (Element complementClass : complementClasses){
			String complementClassName = complementClass.getAttributeNS(RDF_PREFIX, "resource");
			if (complementClassName.length() > 0){
				RIFMember complementClassMembership = constructRIFMemeber(instance, complementClassName);
				
				RIFAnd complementPair = new RIFAnd();
				complementPair.addFormula(complementClassMembership);
				complementPair.addFormula(classMembership == null ? subClassDescriptions : classMembership);
				
				RIFExists exists = new RIFExists();
				if (instance instanceof RIFVar) {
					exists.addExistentialVar((RIFVar) instance);
				}
				exists.addFormula(complementPair);
				
				addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
			}
			if (complementClass.hasChildNodes()){
				Collection<Element> classes = getChildElementsByTagNameNS(complementClass, OWL_PREFIX, "Class");
				classes.addAll(getChildElementsByTagNameNS(complementClass, OWL_PREFIX, "Restriction"));
				for (Element ec : classes){
					OWLComplementClassCompiler ccc = new OWLComplementClassCompiler(
									ec,
									instance,
									classMembership == null ? subClassDescriptions : classMembership);
					rules = ccc.compile(rules);				
				}
			}				
		}
		return rules;
	}
	
	// Property Restriction Compilation methods
	
	private OWLPropertyCompiler prepareRestrictedProperty() {
		Collection<Element> restrictedProperties = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "onProperty");
		try {
			Element restrictedProperty = restrictedProperties.iterator().next();
			String propertyName = restrictedProperty.getAttributeNS(RDF_PREFIX, "resource");
			if (propertyName.length() > 0){
				try {
					URI propertyURI = new URI(propertyName);
					return new OWLReferencedPropertyCompiler(propertyURI, instance);
				} catch (URISyntaxException e) {
					throw new RuntimeException("Property IRI " + propertyName + " is not a valid URI.", e);
				}
			} else {
				Collection<Element> properties = getChildElementsByTagNameNS(restrictedProperty, OWL_PREFIX, "ObjectProperty");
				try {
					return new OWLPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {}
				properties = getChildElementsByTagNameNS(restrictedProperty, OWL_PREFIX, "DatatypeProperty");
				try {
					return new OWLPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {}
				properties = getChildElementsByTagNameNS(restrictedProperty, RDF_PREFIX, "Description");
				try {
					return new OWLPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {
					throw new RuntimeException("No property specified in Restriction " + (classMembership == null ? (subClassDescriptions.iterator().hasNext() ? subClassDescriptions.toString() : "at ontology root") : classMembership.toString()) + ".",e); 
				}
			}
		} catch (NoSuchElementException e) {
			throw new RuntimeException("No property specified in Restriction " + (classMembership == null ? (subClassDescriptions.iterator().hasNext() ? subClassDescriptions.toString() : "at ontology root") : classMembership.toString()) + ".",e); 
		}
	}

	private RIFGroup compileExistentialRestrictions(RIFGroup rules, Collection<Element> existentialRestrictions) {
		OWLPropertyCompiler property = prepareRestrictedProperty();
		
		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
		RIFVar object = new RIFVar().setName(objectVarName);
		
		RIFExists restriction = new RIFExists();
		
		RIFAnd body = new RIFAnd();
		restriction.addFormula(body);
		restriction.addExistentialVar(object);
		
		rules = property.compile(rules, object);
		body.addFormula(property.getPropertyDescription());
		
		for (Element range : existentialRestrictions){
			String rangeName = range.getAttributeNS(RDF_PREFIX, "resource");

			if (!rangeName.equals(OWL_PREFIX + "Thing")){
				// The restriction is qualified...
				if (rangeName.length() > 0) {
					// ... with a class/datatype reference
					body.addFormula(constructRIFMemeber(object, rangeName));
				} else {
					// ... with a class description
					Collection<Element> restrictionQualifiers = getChildElementsByTagNameNS(range, OWL_PREFIX, "Class");
					restrictionQualifiers.addAll(getChildElementsByTagNameNS(range, OWL_PREFIX, "Restriction"));
					if (!restrictionQualifiers.isEmpty()){
						Element restrictionQualifier = restrictionQualifiers.iterator().next();
						OWLExistentialRangeCompiler erc = new OWLExistentialRangeCompiler(
																		restrictionQualifier,
																		object,
																		body);
						rules = erc.compile(rules);
					}
				}
			}
			
			subClassDescriptions.addFormula(restriction);

			return rules;
		}
		
		return rules;
	}

	private RIFGroup compileUniversalRestrictions(RIFGroup rules, Collection<Element> universalRestrictions) {
		OWLPropertyCompiler property = prepareRestrictedProperty();
		
		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
		RIFVar object = new RIFVar().setName(objectVarName);
		
		RIFExists restriction = new RIFExists();
		
		RIFAnd body = new RIFAnd();
		restriction.addFormula(body);
		
		rules = property.compile(rules, object);
		body.addFormula(property.getPropertyDescription());
		
		body.addFormula(classMembership == null ? subClassDescriptions : classMembership);
		RIFFormula head = new RIFAnd();
		
		for (Element range : universalRestrictions){
			String rangeName = range.getAttributeNS(RDF_PREFIX, "resource");
			if (rangeName.length() > 0) {
				// ... with a class/datatype reference
				head = constructRIFMemeber(object, rangeName);
			} else {
				// ... with a class description
				Collection<Element> restrictionQualifiers = getChildElementsByTagNameNS(range, OWL_PREFIX, "Class");
				restrictionQualifiers.addAll(getChildElementsByTagNameNS(range, OWL_PREFIX, "Restriction"));
				if (!restrictionQualifiers.isEmpty()){
					try{ 
						Element restrictionQualifier = restrictionQualifiers.iterator().next();
						OWLUniversalRangeCompiler erc = new OWLUniversalRangeCompiler(
																		restrictionQualifier,
																		object,
																		(RIFAnd) head);
						rules = erc.compile(rules);
					} catch (NoSuchElementException e) {
						throw new RuntimeException("No range specified in Existential Restriction " + (superClassDescriptions.iterator().hasNext() ? superClassDescriptions.toString() : "at ontology root") + ".",e); 
					}
				}
			}
			
			if (instance instanceof RIFVar) {
				restriction.addExistentialVar((RIFVar) instance);
			}
			addRule(rules, object, head, restriction);
			return rules;
		}
		
		return rules;
	}
	
	private RIFGroup compileValueRestrictions(RIFGroup rules, Collection<Element> valueRestrictions) {
		OWLPropertyCompiler property = prepareRestrictedProperty();
		
		@SuppressWarnings("rawtypes")
		RIFConst object;
		if (!valueRestrictions.isEmpty()){
			Element range = valueRestrictions.iterator().next();
			
			String rangeName = range.getAttributeNS(RDF_PREFIX, "resource");
			if (rangeName.length() > 0) {
				try { 
					// construct a datum for the named individual
					object = new RIFIRIConst().setData(new URI(rangeName));
				} catch (URISyntaxException e){
					throw new RuntimeException("Value resource IRI " + rangeName + " is not a valid IRI.", e);
				}
			} else if (range.getChildNodes().getLength() == 0){
				throw new RuntimeException("Value restrictions must restrict to either a resource reference, a data value or an individual description.");
			} else {
				Collection<Element> childElements = new HashSet<Element>();
				int i = 0;
				while (i < range.getChildNodes().getLength()){
					if (range.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE){
						childElements.add((Element)range.getChildNodes().item(i));
					}
					i++;
				}
				if (childElements.iterator().hasNext()) {				
					OWLIndividualCompiler erc = new OWLIndividualCompiler(childElements.iterator().next());
					rules = erc.compile(rules);
					
					object = erc.getIndividualConst();
				} else if (range.getTextContent().trim().length() > 0) {
					String rangeType = range.getAttributeNS(RDF_PREFIX, "datatype");
					String rangeValue = range.getTextContent();
					if (rangeType.length() > 0){
						try {
							object = new RIFXSDTypedConst(new URI(rangeType)).setData(rangeValue);
						} catch (URISyntaxException e){
							throw new RuntimeException("The datatype " + rangeType + " of value " + rangeValue + " is improperly formatted", e);
						}
					} else {
						if (rangeValue.matches(".*\\^\\^.*")){
							rangeType = rangeValue.substring(rangeValue.lastIndexOf('^') + 1);
							try {
								object = new RIFXSDTypedConst(new URI(rangeType)).setData(rangeValue);
							} catch (URISyntaxException e){
								throw new RuntimeException("The datatype of value " + rangeValue + " is improperly formatted", e);
							}
							rangeValue = rangeValue.substring(0, rangeValue.indexOf('^') - 1);
						} else if (rangeValue.matches(".*@.*")) {
							String rangeLocale = rangeValue.substring(rangeValue.lastIndexOf('@') + 1);
							rangeValue = rangeValue.substring(0, rangeValue.lastIndexOf('@') - 1);
							object = new RIFLocaleStringConst(rangeLocale).setData(rangeValue);
						} else {
							object = new RIFStringConst().setData(rangeValue);
						}
					}
				} else {
					throw new RuntimeException("Value restrictions must restrict to either a resource reference, a data value or an individual description.");
				}
			}
		} else {
			throw new RuntimeException("SHOULD NEVER HAPPEN (must have been value restrictions to have descended into this method).");
		}
		
		rules = property.compile(rules, object);
		RIFFormula propertyDescription = property.getPropertyDescription();
		
		subClassDescriptions.addFormula(propertyDescription);
		superClassDescriptions.addFormula(propertyDescription);
		
		return rules;
	}
	
//	private RIFGroup compileCardinalityRestrictions(RIFGroup rules, Collection<Element> cardinalityRestrictions) {
//		int number = -1;
//		for (Element cardRestrict : cardinalityRestrictions){
//			if (cardRestrict.hasChildNodes()){
//				String numberString = cardRestrict.getTextContent();
//				try {
//					number = Integer.parseInt(numberString);
//				} catch (NumberFormatException e) {
//					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
//				}
//			}
//		}
//		
//		if (number < 0) {
//			throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
//		}
//		if (number == 1){
//			for (Element cardRestrict : cardinalityRestrictions){
//				cardRestrict.setAttributeNS(RDF_PREFIX, "resource", OWL_PREFIX + "Thing");
//			}
//			rules = compileExistentialRestrictions(rules, cardinalityRestrictions);
//			return compileMaxCardRestrictions(rules, cardinalityRestrictions);
//		}
//		if (number == 0) {
//			return compileMaxCardRestrictions(rules, cardinalityRestrictions);
//		}
//		
//		logger.info("Cardinality " + number + " restriction ignored in OWL 2 RL.");
//		return rules;
//	}
	
	private RIFGroup compileMaxCardRestrictions(RIFGroup rules, Collection<Element> maxCardRestrictions) {
		int number = -1;
		for (Element cardRestrict : maxCardRestrictions){
			if (cardRestrict.hasChildNodes()){
				String numberString = cardRestrict.getTextContent();
				try {
					number = Integer.parseInt(numberString);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
				}
			}
		}
		
		if (number < 0) {
			throw new RuntimeException("Maximum Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
		}
		if (number > 1) {
			logger.info("Maximum Cardinality " + number + " Restriction ignored in OWL 2 RL.");
			return rules;
		}
		
		OWLPropertyCompiler property = prepareRestrictedProperty();

		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
		RIFVar object = new RIFVar().setName(objectVarName);
		
		RIFExists restriction = new RIFExists();
		if (instance instanceof RIFVar) {
			restriction.addExistentialVar((RIFVar) instance);
		}
		
		RIFAnd body = new RIFAnd();
		restriction.addFormula(body);
		
		RIFGroup subrules = new RIFGroup();
		subrules = property.compile(subrules, object);
		
		for (RIFSentence sentence : subrules){
			RIFSentence statement;
			if (sentence instanceof RIFForAll){
				statement = ((RIFForAll) sentence).getStatement();
			} else {
				statement = sentence;
			}
			
			if (statement instanceof RIFRule) {
				RIFRule rule = (RIFRule) statement;
				
				if (rule.getHead() == null){
					body.addFormula(rule.getBody());
					continue;
				}
				
				if (rule.getBody() == null){
					continue;
				}
			}
			
			rules.addSentence(sentence);
		}
		
		body.addFormula(classMembership == null ? subClassDescriptions : classMembership);
		Collection<RIFVar> universalVars = new HashSet<RIFVar>();
		
		if (number == 0){
			restriction.addExistentialVar(object);
			addRule(rules, universalVars, new RIFError(), restriction);		
		} else /*number == 1*/ {
			universalVars.add(object);
			
			// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
			objectVarName += "~";
			while (instance.getNode().getName().startsWith(objectVarName)){
				objectVarName += "~";
			}
			RIFVar object2 = new RIFVar().setName(objectVarName);
			universalVars.add(object2);
			
			subrules = new RIFGroup();
			subrules = property.compile(subrules, object2);
			
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}
				
				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;
					
					if (rule.getHead() == null){
						body.addFormula(rule.getBody());
						continue;
					}
					
					if (rule.getBody() == null){
						continue;
					}
				}
				
				rules.addSentence(sentence);
			}
			
			addRule(rules, universalVars, constructOWLSameAs(object, object2), restriction);
		}

		return rules;
	}
	
	private RIFGroup compileMinCardRestrictions(RIFGroup rules, Collection<Element> minCardRestrictions) {
		int number = -1;
		for (Element cardRestrict : minCardRestrictions){
			if (cardRestrict.hasChildNodes()){
				String numberString = cardRestrict.getTextContent();
				try {
					number = Integer.parseInt(numberString);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
				}
			}
		}
		
		if (number < 0) {
			throw new RuntimeException("Minimum Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
		}
		if (number == 0){
			logger.info("Minimum Cardinality 0 restriction has no effect in OWL 2 RL.");
			return rules;
		}
		if (number == 1){
			for (Element cardRestrict : minCardRestrictions){
				cardRestrict.setAttributeNS(RDF_PREFIX, "resource", OWL_PREFIX + "Thing");
			}
			return compileExistentialRestrictions(rules, minCardRestrictions);
		}
		
		// Could do something clever with owl:differentFrom here, but it isn't in the OWL 2 RL rules specification.
		
		logger.info("Cardinality " + number + " restriction ignored in OWL 2 RL.");
		return rules;
	}
	
//	private RIFGroup compileQualCardRestrictions(RIFGroup rules, Collection<Element> qualifiedCardRestrictions) {
//		int number = -1;
//		for (Element cardRestrict : qualifiedCardRestrictions){
//			if (cardRestrict.hasChildNodes()){
//				String numberString = cardRestrict.getTextContent();
//				try {
//					number = Integer.parseInt(numberString);
//				} catch (NumberFormatException e) {
//					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
//				}
//			}
//		}
//		
//		if (number < 0) {
//			throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
//		}
//		if (number == 1){
//			// Extract qualifying class identifiers/descriptive Elements
//			Collection<Element> qualifyingClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "onClass");
//			
//			rules = compileExistentialRestrictions(rules, qualifyingClasses);
//			return compileMaxQualCardRestrictions(rules, qualifiedCardRestrictions);
//		}
//		if (number == 0) {
//			return compileMaxQualCardRestrictions(rules, qualifiedCardRestrictions);
//		}
//		
//		logger.info("Cardinality " + number + " restriction ignored in OWL 2 RL.");
//		return rules;
//	}
	
	private RIFGroup compileMaxQualCardRestrictions(RIFGroup rules, Collection<Element> maxQualCardRestrictions) {
		int number = -1;
		for (Element cardRestrict : maxQualCardRestrictions){
			if (cardRestrict.hasChildNodes()){
				String numberString = cardRestrict.getTextContent();
				try {
					number = Integer.parseInt(numberString);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
				}
			}
		}
		
		if (number < 0) {
			throw new RuntimeException("Maximum Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
		}
		if (number > 1) {
			logger.info("Maximum Cardinality " + number + " Restriction ignored in OWL 2 RL.");
			return rules;
		}
		
		// If either max 0 or max 1, compile the property, subject class membership and object class membership into a body formula
		
		OWLPropertyCompiler property = prepareRestrictedProperty();

		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
		RIFVar object = new RIFVar().setName(objectVarName);
		
		RIFExists restriction = new RIFExists();
		if (instance instanceof RIFVar){
			restriction.addExistentialVar((RIFVar) instance);
		}
		
		RIFAnd body = new RIFAnd();
		restriction.addFormula(body);
		
		RIFGroup subrules = new RIFGroup();
		subrules = property.compile(subrules, object);
		
		for (RIFSentence sentence : subrules){
			RIFSentence statement;
			if (sentence instanceof RIFForAll){
				statement = ((RIFForAll) sentence).getStatement();
			} else {
				statement = sentence;
			}
			
			if (statement instanceof RIFRule) {
				RIFRule rule = (RIFRule) statement;
				
				if (rule.getHead() == null){
					body.addFormula(rule.getBody());
					continue;
				}
				
				if (rule.getBody() == null){
					continue;
				}
			}
			
			rules.addSentence(sentence);
		}
		
		Element restrictionQualifier = null;
		String rangeName = null;
		
		// Extract qualifying class identifiers/descriptive Elements
		Collection<Element> qualifyingClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "onClass");
		for (Element range : qualifyingClasses){
			rangeName = range.getAttributeNS(RDF_PREFIX, "resource");

			if (!rangeName.equals(OWL_PREFIX + "Thing")){
				if (rangeName.length() == 0) {
					Collection<Element> restrictionQualifiers = getChildElementsByTagNameNS(range, OWL_PREFIX, "Class");
					restrictionQualifiers.addAll(getChildElementsByTagNameNS(range, OWL_PREFIX, "Restriction"));
					if (!restrictionQualifiers.isEmpty()){
						restrictionQualifier = restrictionQualifiers.iterator().next();
					}
				}
			}
		}
		
		// The restriction is qualified...
		if (restrictionQualifier == null){
			if (rangeName == null || rangeName.length() == 0){
				throw new RuntimeException("All Qualified Cardinality Restrictions must contain a reference or a description of the restricted class.");
			} else if (!rangeName.equals(OWL_PREFIX + "Thing")){
				// ... with a class/datatype reference
				body.addFormula(constructRIFMemeber(object, rangeName));
			}
		} else {
			// ... with a class description
			OWLExistentialRangeCompiler erc = new OWLExistentialRangeCompiler(
					restrictionQualifier,
					object,
					body);
			rules = erc.compile(rules);
		}
		
		body.addFormula(classMembership == null ? subClassDescriptions : classMembership);
		Collection<RIFVar> universalVars = new HashSet<RIFVar>();
		
		if (number == 0){
			// Add a rule to express that the body constructed thus far implies an error in the data/ontology
			restriction.addExistentialVar(object);
			addRule(rules, new RIFError(), restriction);		
		} else /*number == 1*/ {
			universalVars.add(object);
			
			// create a second property object var name not previously used in this descent of class descriptions by appending tilda to the existing property object var name, then repeatedly again until it no longer appears.
			objectVarName += "~";
			while (instance.getNode().getName().startsWith(objectVarName)){
				objectVarName += "~";
			}
			RIFVar object2 = new RIFVar().setName(objectVarName);
			universalVars.add(object2);
			
			// compile the property again with the second property object
			
			subrules = new RIFGroup();
			subrules = property.compile(subrules, object2);
			
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}
				
				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;
					
					if (rule.getHead() == null){
						body.addFormula(rule.getBody());
						continue;
					}
					
					if (rule.getBody() == null){
						continue;
					}
				}
				
				rules.addSentence(sentence);
			}
			
			// The restriction is qualified for the second object as well...
			if (restrictionQualifier == null){
				if (rangeName == null || rangeName.length() == 0){
					throw new RuntimeException("All Qualified Cardinality Restrictions must contain a reference or a description of the restricted class.");
				} else if (!rangeName.equals(OWL_PREFIX + "Thing")){
					// ... with a class/datatype reference
					body.addFormula(constructRIFMemeber(object2, rangeName));
				}
			} else {
				// ... with a class description
				OWLExistentialRangeCompiler erc = new OWLExistentialRangeCompiler(
						restrictionQualifier,
						object2,
						body);
				rules = erc.compile(rules);
			}
			
			// add a rule specifying that when two individuals of the qualifying class are linked to the same instance by the given predicate, they must be the same individual. 
			addRule(rules, universalVars, constructOWLSameAs(object, object2), restriction);
		}

		return rules;
	}
	
	private RIFGroup compileMinQualCardRestrictions(RIFGroup rules, Collection<Element> minQualCardRestrictions) {
		int number = -1;
		for (Element cardRestrict : minQualCardRestrictions){
			if (cardRestrict.hasChildNodes()){
				String numberString = cardRestrict.getTextContent();
				try {
					number = Integer.parseInt(numberString);
				} catch (NumberFormatException e) {
					throw new RuntimeException("Cardinality Restrictions must be restricted to a non-negative integer value, not " + numberString, e);
				}
			}
		}
		
		if (number < 0) {
			throw new RuntimeException("Minimum Cardinality Restrictions must be restricted to a non-negative integer value, not " + number);
		}
		if (number == 0){
			logger.info("Minimum Cardinality 0 restriction has no effect in OWL 2 RL.");
			return rules;
		}
		if (number == 1){
			// Extract qualifying class identifiers/descriptive Elements
			Collection<Element> qualifyingClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "onClass");
			
			return compileExistentialRestrictions(rules, qualifyingClasses);
		}
		
		// Could do something clever with owl:differentFrom here, but it isn't in the OWL 2 RL rules specification.
		
		logger.info("Cardinality " + number + " restriction ignored in OWL 2 RL.");
		return rules;
	}
	
	// methods for abbreviating adding RIFForAll wrapped rules to the ruleset

	protected RIFGroup addRule(RIFGroup rules, RIFFormula head, RIFFormula body){
		if (instance instanceof RIFVar){
			return addRule(rules, (RIFVar) instance, head, body);
		}
		return addRule(rules, new HashSet<RIFVar>(), head, body);
	}
	
	protected RIFGroup addRule(RIFGroup rules, RIFVar instance, RIFFormula head, RIFFormula body){
		Collection<RIFVar> universalVars = new HashSet<RIFVar>();
		universalVars.add(instance);
		
		return addRule(rules, universalVars, head, body);
	}
	
}

// Compilers for equivalent- and superclasses

class OWLEquivalentClassCompiler extends OWLClassCompiler {

	public OWLEquivalentClassCompiler(Element clazz, RIFDatum instance, RIFMember equivalentClassMembership, RIFOr inheritedSubClass, RIFAnd inheritedSuperClass) {
		super(clazz, instance);
		
		if (classMembership == null){
			if (equivalentClassMembership == null){
				subClassDescriptions.addFormula(inheritedSubClass);
				superClassDescriptions.addFormula(inheritedSuperClass);
			} else {
				classMembership = equivalentClassMembership;
			}
		} else {
			inheritedSubClass.addFormula(classMembership);
			inheritedSuperClass.addFormula(classMembership);
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		return this.compileProper(rules);
	}
	
}

class OWLSuperClassCompiler extends OWLClassCompiler {

	public OWLSuperClassCompiler(Element clazz, RIFDatum instance, RIFMember subClassMembership, RIFOr subClassDescription, RIFAnd subClassImplications) {
		super(clazz, instance);
		
		if (classMembership == null){
			if (subClassMembership == null){
				this.subClassDescriptions.addFormula(subClassDescription);
			} else {
				this.subClassDescriptions.addFormula(subClassMembership);
			}
		} else {
			subClassImplications.addFormula(classMembership);
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		return this.compileProper(rules);
	}
	
}

// Compilers for class description components

class OWLIntersectionMemberCompiler extends OWLClassCompiler {

	private final boolean anonymous;
	private final RIFAnd parentClassDescription;
	private final Collection<RIFAnd> intersections;
	
	public OWLIntersectionMemberCompiler(Element clazz, RIFDatum instance, RIFAnd parentSuperClasses, Collection<RIFAnd> intersections) {
		super(clazz, instance);
		this.intersections = intersections;
		this.parentClassDescription = parentSuperClasses;
		
		if (classMembership == null){
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		} else {
			anonymous = false;
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			parentClassDescription.addFormula(classMembership);
			for (RIFAnd intersection : intersections){
				intersection.addFormula(classMembership);
			}
		}

		// Compile intersection member with either declared or Anonymous class membership as the top 
		subrules = this.compileProper(subrules);
			
		if (anonymous){
			Set<RIFAnd> newAnds = new HashSet<RIFAnd>();
			for (RIFSentence sentence : subrules){
				// process subrules - some may be independent rules derived from named class definitions, some may be tied to the parent class definition via an anonymous class.
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}
				
				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;
					// heads of bodyless rules from a class intersection are also implied by membership of this class
					if (rule.getBody() == classMembership){
						parentClassDescription.addFormula(rule.getHead());
						continue;
					}
					// bodies of headless rules from a class intersection each contribute to implying membership of this class
					//   (the possible implications consist of the Cartesian Product of the sets of implications of all classes
					//    in the intersection.)
					if (rule.getHead() == classMembership){
						if (intersections.isEmpty()){
							RIFAnd newAnd = new RIFAnd();
							newAnd.addFormula(rule.getBody());
							newAnds.add(newAnd);
						} else {
							for (RIFAnd intersection : intersections){
								RIFAnd newAnd = new RIFAnd();
								for (RIFFormula formula : intersection){
									newAnd.addFormula(formula);
								}
								newAnd.addFormula(rule.getBody());
								
								newAnds.add(newAnd);
							}
						}
						continue;
					}
				}
				
				rules.addSentence(sentence);
			}
			intersections.clear();
			intersections.addAll(newAnds);
		}
		
		return rules;
	}
	
}

class OWLUnionMemberCompiler extends OWLClassCompiler {

	private final RIFOr union;
	private final RIFOr parentClassDescription;
	private final boolean anonymous;

	public OWLUnionMemberCompiler(Element clazz, RIFDatum instance, RIFOr parentClassDescription, RIFOr union) {
		super(clazz, instance);
		this.union = union;
		this.parentClassDescription = parentClassDescription;
		
		if (classMembership == null){
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		} else {
			anonymous = false;
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			parentClassDescription.addFormula(classMembership);
			union.addFormula(classMembership);
		}
		
		subrules = this.compileProper(subrules);
		
		if (anonymous){
			RIFAnd and = new RIFAnd();
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}
				
				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;
					
					// bodies of headless rules from a class union also imply membership of this class
					if (rule.getHead() == classMembership){
						parentClassDescription.addFormula(rule.getBody());
						continue;
					}
					// all heads of bodyless rules, collectively, from a class union are also potentially implied by membership of this class
					if (rule.getBody() == classMembership){
						and.addFormula(rule.getHead());
						continue;
					}
				}
				
				rules.addSentence(sentence);
			}
			union.addFormula(and);
		}
		
		return rules;
	}
	
}

class OWLComplementClassCompiler extends OWLClassCompiler {
	
	private final RIFFormula complementClassDescription;
	private final boolean anonymous;
	
	public OWLComplementClassCompiler(Element clazz, RIFDatum instance, RIFFormula complementClassDescription) {
		super(clazz, instance);
		this.complementClassDescription = complementClassDescription;
		
		if (classMembership == null){
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		} else {
			anonymous = false;
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			RIFAnd complementPair = new RIFAnd();
			complementPair.addFormula(classMembership);
			complementPair.addFormula(complementClassDescription);
			
			RIFExists exists = new RIFExists();
			if (instance instanceof RIFVar){
				exists.addExistentialVar((RIFVar) instance);
			}
			exists.addFormula(complementPair);
			
			addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
		}
		
		subrules = this.compileProper(subrules);
		
		if (anonymous){
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}

				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;

					// bodies of headless rules from this class should never be met by members of the complement class
					if (rule.getHead() == classMembership){
						RIFAnd complementPair = new RIFAnd();
						complementPair.addFormula(rule.getBody());
						complementPair.addFormula(complementClassDescription);
						
						RIFExists exists = new RIFExists();
						if (instance instanceof RIFVar){
							exists.addExistentialVar((RIFVar) instance);
						}
						exists.addFormula(complementPair);
						
						addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
						continue;
					}
					// membership of this class implies nothing specific 
					if (rule.getBody() == classMembership){
						continue;
					}
				}

				rules.addSentence(sentence);
			}
		}
		
		return rules;
	}
	
}

// Compilers for restriction ranges

class OWLExistentialRangeCompiler extends OWLClassCompiler {

	private final boolean anonymous;
	private final RIFAnd body;
	
	public OWLExistentialRangeCompiler(Element clazz, RIFDatum instance, RIFAnd propertyAssertion) {
		super(clazz, instance);
		this.body = propertyAssertion;

		if (classMembership == null){
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		} else {
			anonymous = false;
		}
	}
	
	public RIFGroup compile(RIFGroup rules){
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			this.body.addFormula(classMembership);
		}
		
		subrules = this.compileProper(subrules);
		
		if (anonymous){
			RIFOr qualifyingBodies = new RIFOr();
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}

				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;

					// bodies of headless rules from a qualifying class are also part of the implication of membership of this class
					if (rule.getHead() == classMembership){
						qualifyingBodies.addFormula(rule.getBody());
						continue;
					}
					// where rangeMembership implies something, this is ignored
					if (rule.getBody() == classMembership){
						continue;
					}
				}

				rules.addSentence(sentence);
			}
			
			this.body.addFormula(qualifyingBodies);
		}
		
		return rules;
	}
	
}

class OWLUniversalRangeCompiler extends OWLClassCompiler {

	private final boolean anonymous;
	private final RIFAnd head;
	
	public OWLUniversalRangeCompiler(Element clazz, RIFDatum instance, RIFAnd propertyAssertion) {
		super(clazz, instance);
		this.head = propertyAssertion;
		
		if (classMembership == null){
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		} else {
			anonymous = false;
		}
	}
	
	public RIFGroup compile(RIFGroup rules){
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			this.head.addFormula(classMembership);
		}
		
		subrules = this.compileProper(subrules);
		
		if (anonymous){
			RIFOr qualifyingBodies = new RIFOr();
			for (RIFSentence sentence : subrules){
				RIFSentence statement;
				if (sentence instanceof RIFForAll){
					statement = ((RIFForAll) sentence).getStatement();
				} else {
					statement = sentence;
				}

				if (statement instanceof RIFRule) {
					RIFRule rule = (RIFRule) statement;

					// where rangeMembership is implied by something, this is ignored
					if (rule.getHead() == classMembership){
						continue;
					}
					// heads of bodyless rules from a qualifying class are also part of the implied membership of this class
					if (rule.getBody() == classMembership){
						qualifyingBodies.addFormula(rule.getBody());
						continue;
					}
				}

				rules.addSentence(sentence);
			}
			
			this.head.addFormula(qualifyingBodies);
		}
		
		return rules;
	}	

}

// Compilers for properties

class OWLPropertyCompiler extends OWLTranslater<RIFGroup> {

	protected final Element property;
	
	protected RIFOr subPropertyDescription = new RIFOr();
	protected RIFAnd superPropertyDescription = new RIFAnd();
	
	protected final RIFDatum subject;
	protected RIFIRIConst propertyIRI = null;
	protected RIFDatum object = null;
	
	public OWLPropertyCompiler(Element property, RIFDatum subject){
		this.property = property;
		this.subject = subject;
		
		if (property != null){
			String propertyName = property.getAttributeNS(RDF_PREFIX, "about");
			if (propertyName.length() > 0){
				try {
					URI propertyURI = new URI(propertyName);
					propertyIRI = new RIFIRIConst();
					propertyIRI.setData(propertyURI);
				} catch (URISyntaxException e) {
					throw new RuntimeException("Property IRI " + propertyName + " is not a valid URI.", e);
				}
			}
		}
	}
	
	public OWLPropertyCompiler(Element property, RIFDatum subject, RIFDatum object){
		this(property, subject);
		this.object = object;		
	}
	
	public RIFFormula getPropertyDescription(){
		if (object == null){
			return getPropertyDescription(new RIFVar().setName("object"));
		} else {
			return getPropertyDescription(object);
		}
	}
	
	public RIFFormula getPropertyDescription(RIFDatum object){
		if (propertyIRI == null){
			return this.subPropertyDescription;
		}
		
		return getPropertyMembership(object);
	}
	
	private RIFFrame getPropertyMembership(RIFDatum object){
		if (propertyIRI != null){
			RIFFrame propertyMembership = new RIFFrame();
			propertyMembership.setSubject(subject);
			propertyMembership.setPredicate(propertyIRI);
			propertyMembership.setObject(object);
			
			return propertyMembership;
		} else {
			return null;
		}
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		if (this.object == null){
			this.object = new RIFVar().setName("object");
		}
		
		return this.compileProper(rules);
	}
	
	public RIFGroup compile(RIFGroup rules, RIFDatum object){
		this.object = object;
		return this.compileProper(rules);
	}
	
	protected RIFGroup compileProper(RIFGroup rules){
		// Construct property membership if possible, and initialise the property description
		RIFFrame propertyMembership = this.getPropertyMembership(object);
		
		// Property domain declarations
		Collection<Element> ranges = getChildElementsByTagNameNS(property, RDFS_PREFIX, "range");
		if (!ranges.isEmpty()) {
			rules = compileRangeRestrictions(rules, propertyMembership, ranges);
		}
		Collection<Element> domains = getChildElementsByTagNameNS(property, RDFS_PREFIX, "domain");
		if (!domains.isEmpty()) {
			rules = compileDomainRestrictions(rules, propertyMembership, domains);
		}
		Collection<Element> types = getChildElementsByTagNameNS(property, RDF_PREFIX, "type");
		if (!types.isEmpty()){
			// TODO check if typed anonymous properties are valid OWL 2 RL
			rules = compileTypeStatements(rules, propertyMembership, types);
		}
		Collection<Element> inverses = getChildElementsByTagNameNS(property, OWL_PREFIX, "inverseOf");
		if (!inverses.isEmpty()) {
			rules = compileEquivalentProperties(rules, propertyMembership, inverses, true, true);
		}
		Collection<Element> equivs = getChildElementsByTagNameNS(property, OWL_PREFIX, "equivalentProperty");
		if (!equivs.isEmpty()) {
			rules = compileEquivalentProperties(rules, propertyMembership, equivs, false, true);
		}
		Collection<Element> superProps = getChildElementsByTagNameNS(property, RDFS_PREFIX, "subPropertyOf");
		if (!superProps.isEmpty()) {
			rules = compileEquivalentProperties(rules, propertyMembership, superProps, false, false);
		}
		Collection<Element> disjoints = getChildElementsByTagNameNS(property, OWL_PREFIX, "propertyDisjointWith");
		if (!disjoints.isEmpty()) {
			rules = compileDisjointProperties(rules, propertyMembership, disjoints);
		}
		Collection<Element> chains = getChildElementsByTagNameNS(property, OWL_PREFIX, "propertyChainAxiom");
		if (!chains.isEmpty()) {
			for (Element chain : chains){
				RIFAnd body = new RIFAnd();
				NodeList links = chain.getChildNodes();
				int i = 0;
				RIFDatum subject = this.subject;
				while (i < links.getLength()){
					RIFDatum object;
					if (i == links.getLength() - 1){
						object = this.object;
					} else {
						object = new RIFVar().setName(this.object.getNode().toString() + "Intermediate" + i);
					}
					if (links.item(i).getNodeType() == Node.ELEMENT_NODE){
						Element link = (Element) links.item(i);
						
						OWLPropertyCompiler pc = null;
						if (link.getNodeName().endsWith("Description")){
							String linkName = link.getAttributeNS(RDF_PREFIX, "about");
							try {
								pc = new OWLReferencedPropertyCompiler(new URI(linkName), subject, object);
							} catch (URISyntaxException e) {
								throw new RuntimeException("Referenced property " + linkName + " in chain must be a valid IRI.", e);
							}
						} else if (link.getNodeName().endsWith("Property")){
							pc = new OWLPropertyCompiler(link, subject, object);
							rules = pc.compile(rules);
						}
						
						body.addFormula(pc.getPropertyDescription());
					}
					
					subject = object;
					i++;
				}
				
				subPropertyDescription.addFormula(body);
			}
		}
		
		return rules;
	}

	private RIFGroup compileRangeRestrictions(RIFGroup rules, RIFFrame propertyMembership, Collection<Element> ranges) {
		for (Element range : ranges){
			String rangeName = range.getAttributeNS(RDF_PREFIX, "resource");
			RIFAnd head = new RIFAnd();
			if (rangeName.length() > 0){
				head.addFormula(constructRIFMemeber(object, rangeName));
			} else if (range.hasChildNodes()) {
				Collection<Element> rangeQualifiers = getChildElementsByTagNameNS(range, OWL_PREFIX, "Class");
				rangeQualifiers.addAll(getChildElementsByTagNameNS(range, OWL_PREFIX, "Restriction"));
				if (!rangeQualifiers.isEmpty()){
					try{ 
						Element restrictionQualifier = rangeQualifiers.iterator().next();
						OWLUniversalRangeCompiler erc = new OWLUniversalRangeCompiler(
																		restrictionQualifier,
																		object,
																		(RIFAnd) head);
						rules = erc.compile(rules);
					} catch (NoSuchElementException e) {
						throw new RuntimeException("No range specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".",e); 
					}
				}
			} else {
				throw new RuntimeException("No range specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".");
			}
			
			superPropertyDescription.addFormula(head);
		}
		return rules;
	}
	
	private RIFGroup compileDomainRestrictions(RIFGroup rules, RIFFrame propertyMembership, Collection<Element> domains) {
		for (Element domain : domains){
			String domainName = domain.getAttributeNS(RDF_PREFIX, "resource");
			RIFAnd head = new RIFAnd();
			if (domainName.length() > 0){
				head.addFormula(constructRIFMemeber(subject, domainName));
			} else if (domain.hasChildNodes()) {
				Collection<Element> domainQualifiers = getChildElementsByTagNameNS(domain, OWL_PREFIX, "Class");
				domainQualifiers.addAll(getChildElementsByTagNameNS(domain, OWL_PREFIX, "Restriction"));
				if (!domainQualifiers.isEmpty()){
					try{ 
						Element restrictionQualifier = domainQualifiers.iterator().next();
						OWLUniversalRangeCompiler erc = new OWLUniversalRangeCompiler(
																		restrictionQualifier,
																		subject,
																		(RIFAnd) head);
						rules = erc.compile(rules);
					} catch (NoSuchElementException e) {
						throw new RuntimeException("No domain specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".",e); 
					}
				}
			} else {
				throw new RuntimeException("No domain specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".");
			}
			
			superPropertyDescription.addFormula(head);
		}
		return rules;
	}
	
	private RIFGroup compileTypeStatements(RIFGroup rules, RIFFrame propertyMembership, Collection<Element> types){
		for (Element type : types){
			String typeName = type.getAttributeNS(RDF_PREFIX, "resource");
			if (typeName.length() > 0){
				if (typeName.equals("http://www.w3.org/2002/07/owl#FunctionalProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be functional.");
					}
					RIFVar second = new RIFVar().setName(object.getNode().toString() + "~");
					
					RIFFrame secondMembership = new RIFFrame();
					secondMembership.setSubject(subject);
					secondMembership.setPredicate(propertyIRI);
					secondMembership.setObject(second);
					
					RIFAnd body = new RIFAnd();
					body.addFormula(propertyMembership);
					body.addFormula(secondMembership);
					
					Collection<RIFVar> uvs = new HashSet<RIFVar>();
					if (subject instanceof RIFVar) {
						uvs.add((RIFVar) subject);
					}
					if (object instanceof RIFVar) {
						uvs.add((RIFVar) object);
					}
					uvs.add(second);
					
					rules = addRule(rules, uvs, constructOWLSameAs(object, second), body);
				} else if (typeName.equals("http://www.w3.org/2002/07/owl#InverseFunctionalProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be inverse functional.");
					}
					RIFVar second = new RIFVar().setName(subject.getNode().toString() + "~");
					
					RIFFrame secondMembership = new RIFFrame();
					secondMembership.setSubject(second);
					secondMembership.setPredicate(propertyIRI);
					secondMembership.setObject(object);
					
					RIFAnd body = new RIFAnd();
					body.addFormula(propertyMembership);
					body.addFormula(secondMembership);
					
					Collection<RIFVar> uvs = new HashSet<RIFVar>();
					if (subject instanceof RIFVar) {
						uvs.add((RIFVar) subject);
					}
					if (object instanceof RIFVar) {
						uvs.add((RIFVar) object);
					}
					uvs.add(second);
					
					rules = addRule(rules, uvs, constructOWLSameAs(subject, second), body);
				} else if (typeName.equals("http://www.w3.org/2002/07/owl#IrreflexiveProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be irreflexive.");
					}
					
					RIFFrame body = new RIFFrame();
					body.setSubject(subject);
					body.setPredicate(propertyIRI);
					body.setObject(subject);
					
					if (subject instanceof RIFVar){
						RIFExists exists = new RIFExists();
						exists.addExistentialVar((RIFVar) subject);
						exists.addFormula(body);
						
						rules = addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
					} else {
						rules = addRule(rules, new HashSet<RIFVar>(), new RIFError(), body);
					}
				} else if (typeName.equals("http://www.w3.org/2002/07/owl#SymmetricProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be symmetric.");
					}
					
					RIFFrame body = new RIFFrame();
					body.setSubject(object);
					body.setPredicate(propertyIRI);
					body.setObject(subject);
					
					subPropertyDescription.addFormula(body);
				} else if (typeName.equals("http://www.w3.org/2002/07/owl#AsymmetricProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be asymmetric.");
					}
					
					RIFFrame symmetric = new RIFFrame();
					symmetric.setSubject(object);
					symmetric.setPredicate(propertyIRI);
					symmetric.setObject(subject);
					
					RIFAnd body = new RIFAnd();
					body.addFormula(propertyMembership);
					body.addFormula(symmetric);
					
					RIFExists exists = new RIFExists();
					if (subject instanceof RIFVar){
						exists.addExistentialVar((RIFVar) subject);
					}
					if (object instanceof RIFVar){
						exists.addExistentialVar((RIFVar) object);
					}
					if (exists.existentialVars().iterator().hasNext()){
						exists.addFormula(body);
						rules = addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
					} else {
						rules = addRule(rules, new HashSet<RIFVar>(), new RIFError(), body);						
					}
					
				} else if (typeName.equals("http://www.w3.org/2002/07/owl#TransitiveProperty")){
					if (propertyMembership == null) {
						// TODO check
						throw new RuntimeException("Anonymous property descriptions cannot be asymmetric.");
					}
					
					RIFVar second = new RIFVar().setName(object.getNode().toString() + "~");
					
					RIFFrame transitive = new RIFFrame();
					transitive.setSubject(object);
					transitive.setPredicate(propertyIRI);
					transitive.setObject(second);
					
					RIFAnd body = new RIFAnd();
					body.addFormula(propertyMembership);
					body.addFormula(transitive);
					
					Collection<RIFVar> uvs = new HashSet<RIFVar>();
					if (subject instanceof RIFVar) {
						uvs.add((RIFVar) subject);
					}
					uvs.add(second);
					
					RIFFrame head = new RIFFrame();
					head.setSubject(subject);
					head.setPredicate(propertyIRI);
					head.setObject(second);
					
					if (object instanceof RIFVar){
						RIFExists exists = new RIFExists();
						exists.addExistentialVar((RIFVar) object);
						exists.addFormula(body);
						
						rules = addRule(rules, uvs, head, exists);
					} else {
						rules = addRule(rules, uvs, head, body);						
					}					
				} else {
					throw new RuntimeException("Property type " + typeName + "is not a valid type for a property in OWL 2 RL.");
				}
			}
		}
		return rules;
	}
	
	private RIFGroup compileEquivalentProperties(RIFGroup rules, RIFFrame propertyMembership, Collection<Element> equivalents, boolean inverse, boolean subProperty) {
		for (Element equiv : equivalents){
			String equivName = equiv.getAttributeNS(RDF_PREFIX, "resource");
			RIFAnd head = new RIFAnd();
			if (equivName.length() > 0){
				RIFFrame secondMembership = new RIFFrame();
				if (inverse){
					secondMembership.setSubject(object);
					secondMembership.setObject(subject);					
				} else {
					secondMembership.setSubject(subject);
					secondMembership.setObject(object);
				}
				secondMembership.setPredicate(propertyIRI);
				head.addFormula(secondMembership);
			} else if (equiv.hasChildNodes()) {
				Collection<Element> inverseProperties = getChildElementsByTagNameNS(equiv, OWL_PREFIX, "ObjectProperty");
				inverseProperties.addAll(getChildElementsByTagNameNS(equiv, OWL_PREFIX, "DataProperty"));
				if (!inverseProperties.isEmpty()){
					try{
						Element inverseProperty = inverseProperties.iterator().next();
						OWLPropertyCompiler erc;
						if (inverse){
							erc = new OWLPropertyCompiler(
									inverseProperty,
									object,
									subject);
						} else {
							erc = new OWLPropertyCompiler(
									inverseProperty,
									subject,
									object);
						}
						rules = erc.compile(rules);
						head.addFormula(erc.getPropertyDescription());
					} catch (NoSuchElementException e) {
						throw new RuntimeException("No inverse specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".",e); 
					}
				}
			} else {
				throw new RuntimeException("No inverse specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".");
			}
			
			Collection<RIFVar> uvs = new HashSet<RIFVar>();
			if (subject instanceof RIFVar){
				uvs.add((RIFVar) subject);
			}
			if (object instanceof RIFVar){
				uvs.add((RIFVar) object);
			}
			
			superPropertyDescription.addFormula(head);
			if (subProperty){
				subPropertyDescription.addFormula(head);
			}
		}
		return rules;
	}
	
	private RIFGroup compileDisjointProperties(RIFGroup rules, RIFFrame propertyMembership, Collection<Element> disjoints) {
		for (Element equiv : disjoints){
			String equivName = equiv.getAttributeNS(RDF_PREFIX, "resource");
			RIFAnd body = new RIFAnd();
			if (equivName.length() > 0){
				RIFFrame secondMembership = new RIFFrame();
				secondMembership.setSubject(subject);
				secondMembership.setObject(object);
				secondMembership.setPredicate(propertyIRI);
				body.addFormula(secondMembership);
			} else if (equiv.hasChildNodes()) {
				Collection<Element> inverseProperties = getChildElementsByTagNameNS(equiv, OWL_PREFIX, "ObjectProperty");
				inverseProperties.addAll(getChildElementsByTagNameNS(equiv, OWL_PREFIX, "DataProperty"));
				if (!inverseProperties.isEmpty()){
					try{
						Element inverseProperty = inverseProperties.iterator().next();
						OWLPropertyCompiler erc;
						erc = new OWLPropertyCompiler(
								inverseProperty,
								subject,
								object);
						rules = erc.compile(rules);
						body.addFormula(erc.getPropertyDescription());
					} catch (NoSuchElementException e) {
						throw new RuntimeException("No inverse specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".",e); 
					}
				}
			} else {
				throw new RuntimeException("No inverse specified for property " + (propertyMembership == null ? subPropertyDescription : propertyMembership) + ".");
			}
			
			body.addFormula(propertyMembership == null ? subPropertyDescription : propertyMembership);
			
			RIFExists exists = new RIFExists();
			if (subject instanceof RIFVar){
				exists.addExistentialVar((RIFVar) subject);
			}
			if (object instanceof RIFVar){
				exists.addExistentialVar((RIFVar) object);
			}
			if (exists.existentialVars().iterator().hasNext()){
				exists.addFormula(body);
				
				addRule(rules, new HashSet<RIFVar>(), new RIFError(), exists);
			} else {
				addRule(rules, new HashSet<RIFVar>(), new RIFError(), body);
			}
		}
		return rules;
	}
	
	// add rule stuff
	
	protected RIFGroup addRule(RIFGroup rules, RIFFormula head, RIFFormula body){
		Collection<RIFVar> universalVars = new HashSet<RIFVar>();
		if (subject instanceof RIFVar){
			universalVars.add((RIFVar) subject);
		}
		if (object instanceof RIFVar){
			universalVars.add((RIFVar) object);
		}
		
		return addRule(rules, universalVars, head, body);
	}
	
}

class OWLReferencedPropertyCompiler extends OWLPropertyCompiler {
	
	public OWLReferencedPropertyCompiler(URI propertyName, RIFDatum subject, RIFDatum object) {
		super(null, subject, object);
		
		this.propertyIRI = new RIFIRIConst();
		this.propertyIRI.setData(propertyName);
	}
	
	public OWLReferencedPropertyCompiler(URI propertyName, RIFDatum subject) {
		super(null, subject);
		
		this.propertyIRI = new RIFIRIConst();
		this.propertyIRI.setData(propertyName);
	}

	@Override
	protected RIFGroup compileProper(RIFGroup rules){
		return rules;
	}
	
}

// Compiler for individuals

class OWLIndividualCompiler extends OWLTranslater<RIFGroup>{

	protected static int anonCount = 0; 
	
	protected final Element individual;
	
	protected final RIFIRIConst className;
	protected final RIFURIConst identifier;
	
	public OWLIndividualCompiler(Element individual){
		this.individual = individual;
		
		String cName = individual.getTagName();
		URI classURI;
		try {
			classURI = new URI(cName);
		} catch (URISyntaxException e) {
			throw new RuntimeException("Resource IRI " + cName + " is not a valid URI.", e);
		}
		className = new RIFIRIConst().setData(classURI);
		
		String identifierString = individual.getAttributeNS(RDF_PREFIX, "about");
		URI identURI = null;
		boolean local = false;
		try {
			identURI = new URI(identifierString);
		} catch (URISyntaxException e) {
			local = true;
			if (identifierString.length() > 0){
				try {
					identURI = new URI(individual.getBaseURI()+identifierString);
				} catch (URISyntaxException e1) {
					throw new RuntimeException("Base URI " + individual.getBaseURI() + " plus identifier " + identifierString + " is not a valid URI.", e);
				}
			} else {
				try {
					identURI = new URI(individual.getBaseURI()+"AnonymousIdentifier-"+anonCount);
					anonCount++;
				} catch (URISyntaxException e1) {
					throw new RuntimeException("SHOULD NEVER HAPPEN, base URI " + individual.getBaseURI() + " plus AnonymousIdentifier-" + anonCount, e);
				}
			}
		}
		if (local) {
			identifier = new RIFLocalConst().setData(identURI);
		} else {
			identifier = new RIFIRIConst().setData(identURI);
		}
	}
	
	public RIFIRIConst getClassConst(){
		return className;
	}
	
	public RIFURIConst getIndividualConst(){
		return identifier;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		// TODO Auto-generated method stub
		return rules;
	}
	
}