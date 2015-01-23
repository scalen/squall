package org.openimaj.rifcore.imports.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.conditions.atomic.RIFError;
import org.openimaj.rifcore.conditions.atomic.RIFFrame;
import org.openimaj.rifcore.conditions.data.RIFDatum;
import org.openimaj.rifcore.conditions.data.RIFIRIConst;
import org.openimaj.rifcore.conditions.data.RIFVar;
import org.openimaj.rifcore.conditions.formula.RIFAnd;
import org.openimaj.rifcore.conditions.formula.RIFAnon;
import org.openimaj.rifcore.conditions.formula.RIFExists;
import org.openimaj.rifcore.conditions.formula.RIFFormula;
import org.openimaj.rifcore.conditions.formula.RIFMember;
import org.openimaj.rifcore.conditions.formula.RIFOr;
import org.openimaj.rifcore.rules.RIFForAll;
import org.openimaj.rifcore.rules.RIFGroup;
import org.openimaj.rifcore.rules.RIFRule;
import org.openimaj.rifcore.rules.RIFSentence;
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
	
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	
	public abstract IO compile (IO io);
	
	protected RIFMember constructRIFMemeber(RIFVar instance, String className){
		try {
			URI classURI = new URI(className);
			
			RIFIRIConst classIRI = new RIFIRIConst();
			classIRI.setData(classURI);
			
			RIFMember typeStatement = new RIFMember();
			typeStatement.setInstance(instance);
			typeStatement.setInClass(classIRI);
			
			return typeStatement;
		} catch (URISyntaxException e) {
			throw new RuntimeException("class iri "+ className +" invalid: ", e);
		}
	}
	
	protected Collection<Element> getChildElementsByTagNameNS(Element parent, String namespace, String tag){
		Collection<Element> result = new HashSet<Element>();
		
		NodeList children = parent.getChildNodes();
		Node child = children.item(0);
		if (child != null){
			for (int i = 1; i < children.getLength(); child = children.item(i++)){
				if (child.getLocalName().equals(tag) && child.getNamespaceURI().equals(namespace) && child.getNodeType() == Node.ELEMENT_NODE){
						result.add((Element) child);
				}
			}
		}
		
		return result;
	}
}

class OntologyCompiler extends OWLTranslater<RIFRuleSet> {
	
	private static final Logger logger = Logger.getLogger(OntologyCompiler.class);
	
	private Document doc;
	
	public OntologyCompiler(InputStream stream){
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(stream);
			
			doc.getDocumentElement().normalize();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public RIFRuleSet compile(RIFRuleSet ruleSet){
		ruleSet = compilePrefixes(ruleSet);
		ruleSet = compileImports(ruleSet);
		
		RIFGroup rules = new RIFGroup();
		
		rules = compileClassDescriptions(rules);
		
		rules = compilePropertyDescriptions(rules);
		
		ruleSet.addRootGroup(rules);
		
		return ruleSet;
	}
	
	private RIFRuleSet compilePrefixes(RIFRuleSet ruleSet){
		Node attr = doc.getAttributes().item(0);
		if (attr != null){
			for (int i = 1; i < doc.getAttributes().getLength(); attr = doc.getAttributes().item(i++)){
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
		Collection<Element> ontologies = getChildElementsByTagNameNS((Element) doc, OWL_PREFIX, "Ontology");
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
	
	private RIFGroup compileClassDescriptions(RIFGroup rules) {
		RIFGroup classRules = new RIFGroup();
		
		Collection<Element> classes = getChildElementsByTagNameNS((Element) doc, OWL_PREFIX, "Class");
		classes.addAll(getChildElementsByTagNameNS((Element) doc, OWL_PREFIX, "Restriction"));
		for (Element c :classes){
			RIFVar instance = new RIFVar().setName("instance");
			OWLClassCompiler cc = new OWLClassCompiler(c, instance);
			classRules = cc.compile(classRules);
		}
		
		rules.addSentence(classRules);
		
		return rules;
	}
	
	private RIFGroup compilePropertyDescriptions(RIFGroup ruleSet) {
		// TODO
		
		return ruleSet;
	}
	
}

class OWLClassCompiler extends OWLTranslater<RIFGroup> {

	protected final Element clazz;
	protected final RIFVar instance;
	protected RIFFormula subClassDescription = null;
	protected RIFFormula superClassDescription = null;
	
	public OWLClassCompiler(Element clazz, RIFVar instance){
		this.clazz = clazz;
		this.instance = instance;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		// get class URI
		String className = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (className.length() > 0){
			RIFMember classMembership = constructRIFMemeber(instance, className);
			this.subClassDescription = classMembership;
			this.superClassDescription = classMembership;
		}
				
		return this.compileProper(rules);
	}
	
	protected RIFGroup compileProper(RIFGroup rules){
		RIFOr classDescription = new RIFOr();
		
		Collection<Element> classIntersections = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "intersectionOf");
		Collection<Element> classUnions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "unionOf");
		Collection<Element> complementClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "complementOf");
		
		Collection<Element> existentialRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "someValueFrom");
		Collection<Element> universalRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "allValuesFrom");
		Collection<Element> valueRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "hasValue");
		
		Collection<Element> cardinalityRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "cardinality");
		Collection<Element> maxCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "maxCardinality");
		Collection<Element> minCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "minCardinality");
		Collection<Element> maxQualCardRestrictions = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "maxQualifiedCardinality");
		
		if (!classIntersections.isEmpty()){
			rules = compileIntersections(rules, classIntersections, classDescription);
		} else if (!classUnions.isEmpty()){
			rules = compileUnions(rules, classUnions, classDescription);
		} else if (!complementClasses.isEmpty()){
			// handled along with disjoint classes
		} else if (!existentialRestrictions.isEmpty()){
			rules = compileExistentialRestrictions(rules, existentialRestrictions, classDescription);
		// TODO
//		} else if (!universalRestrictions.isEmpty()){
//			rules = compileUniversalRestrictions(rules, universalRestrictions, classDescription);
//		} else if (!valueRestrictions.isEmpty()){
//			rules = compileValueRestrictions(rules, valueRestrictions, classDescription);
//		} else if (!cardinalityRestrictions.isEmpty()){
//			rules = compilecardinalityRestrictions(rules, cardinalityRestrictions, classDescription);
//		} else if (!maxCardRestrictions.isEmpty()){
//			rules = compileMaxCardRestrictions(rules, maxCardRestrictions, classDescription);
//		} else if (!minCardRestrictions.isEmpty()){
//			rules = compileMinCardRestrictions(rules, minCardRestrictions, classDescription);
//		} else if (!maxQualCardRestrictions.isEmpty()){
//			rules = compileMaxQualCardRestrictions(rules, maxQualCardRestrictions, classDescription);
		}
		
		// Disjoint Classes
		// Not strictly accurate outside of OWL 2 RL entailments, but within that scope they are treated the same
		complementClasses.addAll(getChildElementsByTagNameNS(clazz, OWL_PREFIX, "disjointWith"));
		rules = compileComplements(rules, complementClasses, classDescription);
		
		// TODO migrate to methods
		// Equivalent Classes
		Collection<Element> equivalentClasses = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "equivalentClass");
		if (!equivalentClasses.isEmpty()){
			for (Node c : equivalentClasses){
				Element equivClass = (Element) c;
				String equivClassName = equivClass.getAttributeNS(RDF_PREFIX, "resource");
				if (equivClassName.length() > 0){
					RIFMember equivClassMembership = constructRIFMemeber(instance, equivClassName);
					
					addRule(rules,
							equivClassMembership,
							subClassDescription == null ? classDescription : subClassDescription);
					addRule(rules,
							superClassDescription == null ? classDescription : superClassDescription,
							equivClassMembership);
				}
				if (equivClass.hasChildNodes()){
					Collection<Element> classes = getChildElementsByTagNameNS(equivClass, OWL_PREFIX, "Class");
					if (!classes.isEmpty()){
						for (Element ec : classes){
							OWLEquivalentClassCompiler scc = new OWLEquivalentClassCompiler(
																		ec, instance,
																		superClassDescription == null ? classDescription : superClassDescription);
							rules = scc.compile(rules);
						}
					}
				}				
			}
		}
		
		// Subclasses
		Collection<Element> superClasses = getChildElementsByTagNameNS(clazz, RDFS_PREFIX, "subClassOf");
		if (!superClasses.isEmpty()){
			for (Node c : superClasses){
				Element superClass = (Element) c;
				String superClassName = superClass.getAttributeNS(RDF_PREFIX, "resource");
				if (superClassName.length() > 0){
					addRule(rules,
							constructRIFMemeber(instance, superClassName),
							subClassDescription == null ? classDescription : subClassDescription);
				}
				if (superClass.hasChildNodes()){
					Collection<Element> classes = getChildElementsByTagNameNS(superClass, OWL_PREFIX, "Class");
					if (!classes.isEmpty()){
						for (Element sc : classes){
							OWLSuperClassCompiler scc = new OWLSuperClassCompiler(
																sc, instance,
																subClassDescription == null ? classDescription : subClassDescription);
							rules = scc.compile(rules);
						}
					}
				}				
			}
		}
		
		return rules;
	}
	
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
				Collection<Element> properties = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "ObjectProperty");
				try {
					return new OWLNamedPropertyCompiler(properties.iterator().next(), instance);
				} catch (URISyntaxException e) {
					return new OWLAnonymousPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {}
				properties = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "DatatypeProperty");
				try {
					return new OWLNamedPropertyCompiler(properties.iterator().next(), instance);
				} catch (URISyntaxException e) {
					return new OWLAnonymousPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {}
				properties = getChildElementsByTagNameNS(clazz, OWL_PREFIX, "Description");
				try {
					return new OWLAnonymousPropertyCompiler(properties.iterator().next(), instance);
				} catch (NoSuchElementException e) {
					throw new RuntimeException("No property specified in Restriction " + (subClassDescription == null ? "at ontology root" : subClassDescription.toString()) + ".",e); 
				}
			}
		} catch (NoSuchElementException e) {
			throw new RuntimeException("No property specified in Restriction " + (subClassDescription == null ? "at ontology root" : subClassDescription.toString()) + ".",e); 
		}
	}

	private RIFGroup compileExistentialRestrictions(RIFGroup rules,
			Collection<Element> existentialRestrictions, RIFOr classDescription) {
		OWLPropertyCompiler property = prepareRestrictedProperty();
		
		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
		RIFVar object = new RIFVar().setName(objectVarName);
		
		RIFExists restriction = new RIFExists();
		restriction.addExistentialVar(object);
		
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
		
		for (Element range : existentialRestrictions){
			String rangeName = range.getAttributeNS(RDF_PREFIX, "resource");

			if (!rangeName.equals("http://www.w3.org/2002/07/owl#Thing")){
				// The restriction is qualified...
				// ... so establish an intersection of conditions including the existing triple template
				
				if (rangeName.length() > 0) {
					// ... with a class/datatype reference
					body.addFormula(constructRIFMemeber(object, rangeName));
				} else {
					// ... with a class description
					Collection<Element> restrictionQualifiers = getChildElementsByTagNameNS(range, OWL_PREFIX, "Class");
					restrictionQualifiers.addAll(getChildElementsByTagNameNS(range, OWL_PREFIX, "Restriction"));
					if (!restrictionQualifiers.isEmpty()){
						try{ 
							Element restrictionQualifier = restrictionQualifiers.iterator().next();
							OWLExistentialRangeCompiler erc = new OWLExistentialRangeCompiler(
																			restrictionQualifier,
																			object,
																			body);
							rules = erc.compile(rules);
						} catch (NoSuchElementException e) {
							throw new RuntimeException("No range specified in Existential Restriction " + (superClassDescription == null ? "at ontology root" : superClassDescription.toString()) + ".",e); 
						}
					}
				}
			}
			
			if (superClassDescription != null){
				addRule(rules, superClassDescription, restriction);
			}
			classDescription.addFormula(restriction);
			return rules;
		}
		
		
		
		return rules;
	}

	private RIFGroup compileComplements(RIFGroup rules, Collection<Element> complementClasses, RIFOr classDescription) {
		for (Element complementClass : complementClasses){
			String complementClassName = complementClass.getAttributeNS(RDF_PREFIX, "resource");
			if (complementClassName.length() > 0){
				RIFMember complementClassMembership = constructRIFMemeber(instance, complementClassName);
				
				RIFAnd complementPair = new RIFAnd();
				complementPair.addFormula(complementClassMembership);
				complementPair.addFormula(subClassDescription == null ? classDescription : subClassDescription);
				
				RIFError error = new RIFError();
				
				addRule(rules, error, complementPair);
			}
			if (complementClass.hasChildNodes()){
				Collection<Element> classes = getChildElementsByTagNameNS(complementClass, OWL_PREFIX, "Class");
				classes.addAll(getChildElementsByTagNameNS(complementClass, OWL_PREFIX, "Restriction"));
				if (!classes.isEmpty()){
					for (Element ec : classes){
						OWLComplementClassCompiler ccc = new OWLComplementClassCompiler(
										ec,
										instance,
										subClassDescription == null ? classDescription : subClassDescription);
						rules = ccc.compile(rules);				
					}
				}
			}				
		}
		return rules;
	}

	private RIFGroup compileIntersections(RIFGroup rules, Collection<Element> classIntersections, RIFOr classDescription) {
		for (Element intersection : classIntersections){
			RIFAnd and = null;
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(intersection, RDF_PREFIX, "Description");
			if (!classReferences.isEmpty()) {
				and = new RIFAnd();
				
				for (Element reference : classReferences){
					String referenceName = reference.getAttributeNS(RDF_PREFIX, "about");
					 
					RIFMember intersectedClassMembership = constructRIFMemeber(instance, referenceName);
					
					// class membership of a class intersection implies membership of every intersected class
					addRule(rules,
							intersectedClassMembership,
							subClassDescription == null ? classDescription : subClassDescription);
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
			if (!classes.isEmpty()){
				for (Element ic : classes){
					OWLIntersectionMemberCompiler imc = new OWLIntersectionMemberCompiler(
									ic,
									instance,
									subClassDescription == null ? classDescription : subClassDescription,
									ands);
					rules = imc.compile(rules);
				}
			}
			// add all possible intersections to the set of bodies implying membership of this class
			for (RIFAnd a : ands){
				if (superClassDescription != null){
					addRule(rules, superClassDescription, a);
				}
				classDescription.addFormula(a);
			}
		}
		return rules;
	}

	private RIFGroup compileUnions(RIFGroup rules, Collection<Element> classUnions, RIFOr classDescription) {
		for (Element union : classUnions){
			RIFOr or = new RIFOr();
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(union, RDF_PREFIX, "Description");
			for (Element reference : classReferences){
				String referenceName = reference.getAttributeNS(RDF_PREFIX, "about");
				 
				RIFMember unionMembership = constructRIFMemeber(instance, referenceName);
				
				// class membership of any of the unioned classes implies membership of the class union
				if (superClassDescription != null){
					addRule(rules, superClassDescription, unionMembership);
				}
				// class membership of the class union implies membership of at least one of the unioned classes
				or.addFormula(unionMembership);
			}
			
			Collection<Element> classes = getChildElementsByTagNameNS(union, OWL_PREFIX, "Class");
			classes.addAll(getChildElementsByTagNameNS(union, OWL_PREFIX, "Restriction"));
			if (!classes.isEmpty()){
				for (Element uc : classes){
					OWLUnionMemberCompiler umc = new OWLUnionMemberCompiler(uc, instance, superClassDescription, or);
					rules = umc.compile(rules);
				}
			}
			
			// add the union to the set of heads implied by membership of this class, and the class description
			addRule(rules, or, subClassDescription);
			classDescription.addFormula(or);
		}
		return rules;
	}
	
	protected RIFGroup addRule(RIFGroup rules, RIFFormula head, RIFFormula body){
		RIFForAll forall = new RIFForAll();
		forall.addUniversalVar(instance);
		
		RIFRule rule = new RIFRule();
		rule.setHead(head);
		rule.setBody(body);
		forall.setStatement(rule);
		
		rules.addSentence(forall);
		return rules;
	}
	
}

class OWLExistentialRangeCompiler extends OWLClassCompiler {

	private final RIFMember rangeMembership;
	private final boolean anonymous;
	private final RIFAnd body;
	
	public OWLExistentialRangeCompiler(Element clazz, RIFVar instance, RIFAnd propertyAssertion) {
		super(clazz, instance);
		this.body = propertyAssertion;
		
		// get class URI
		String rangeName = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (rangeName.length() > 0){
			anonymous = false;
			rangeMembership = constructRIFMemeber(instance, rangeName);
		} else {
			anonymous = true;
			rangeMembership = new RIFAnon();
			rangeMembership.setInstance(instance);
		}
		this.subClassDescription = rangeMembership;
		this.superClassDescription = rangeMembership;
	}
	
	public RIFGroup compile(RIFGroup rules){
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			this.body.addFormula(rangeMembership);
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
					if (rule.getHead() == rangeMembership){
						qualifyingBodies.addFormula(rule.getBody());
						continue;
					}
					// where rangeMembership implies something, this is ignored
					if (rule.getBody() == rangeMembership){
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

class OWLEquivalentClassCompiler extends OWLClassCompiler {

	private final RIFFormula classMembership;

	public OWLEquivalentClassCompiler(Element clazz, RIFVar instance, RIFFormula equivalentClassDescription) {
		super(clazz, instance);
		
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		} else {
			classMembership = null;
		}
		
		subClassDescription = equivalentClassDescription;
		superClassDescription = equivalentClassDescription;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		if (classMembership != null){
			addRule(rules, classMembership, subClassDescription);
			addRule(rules, superClassDescription, classMembership);
			subClassDescription = classMembership;
			superClassDescription = classMembership;
		}
		// TODO Auto-generated method stub
		return this.compileProper(rules);
	}
	
}

class OWLSuperClassCompiler extends OWLClassCompiler {

	private final RIFFormula classMembership;

	public OWLSuperClassCompiler(Element clazz, RIFVar instance, RIFFormula subClassDescription) {
		super(clazz, instance);
		
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		} else {
			classMembership = null;
		}
		
		this.subClassDescription = subClassDescription;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		if (classMembership != null){
			addRule(rules, classMembership, subClassDescription);
			subClassDescription = classMembership;
			superClassDescription = classMembership;
		}
		// TODO Auto-generated method stub
		return this.compileProper(rules);
	}
	
}

class OWLIntersectionMemberCompiler extends OWLClassCompiler {

	private final boolean anonymous;
	private final RIFMember classMembership;
	private final RIFFormula parentClassDescription;
	private final Collection<RIFAnd> intersections;
	
	public OWLIntersectionMemberCompiler(Element clazz, RIFVar instance, RIFFormula parentClassDescription, Collection<RIFAnd> intersections) {
		super(clazz, instance);
		this.intersections = intersections;
		this.parentClassDescription = parentClassDescription;
		
		// get class URI
		String className = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (className.length() > 0){
			anonymous = false;
			classMembership = constructRIFMemeber(instance, className);
		} else {
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		}
		this.subClassDescription = classMembership;
		this.superClassDescription = classMembership;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			addRule(rules, classMembership, parentClassDescription);
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
						addRule(rules,
								rule.getHead(),
								parentClassDescription);
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
	private final RIFFormula parentClassDescription;
	private final RIFMember classMembership;
	private final boolean anonymous;

	public OWLUnionMemberCompiler(Element clazz, RIFVar instance, RIFFormula parentClassDescription, RIFOr union) {
		super(clazz, instance);
		this.union = union;
		this.parentClassDescription = parentClassDescription;
		
		// get class URI
		String className = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (className.length() > 0){
			anonymous = false;
			classMembership = constructRIFMemeber(instance, className);
			this.subClassDescription = classMembership;
			this.superClassDescription = classMembership;
		} else {
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		}
		this.subClassDescription = classMembership;
		this.superClassDescription = classMembership;
	}
	
	@Override
	public RIFGroup compile(RIFGroup rules) {
		RIFGroup subrules;
		if (anonymous){
			subrules = new RIFGroup();
		} else {
			subrules = rules;
			if (parentClassDescription != null){
				addRule(rules, parentClassDescription, classMembership);
			}
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
						if (parentClassDescription != null){
							addRule(rules, parentClassDescription, rule.getBody());
						}
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
	private final RIFMember classMembership;
	private final boolean anonymous;
	
	public OWLComplementClassCompiler(Element clazz, RIFVar instance, RIFFormula complementClassDescription) {
		super(clazz, instance);
		this.complementClassDescription = complementClassDescription;
		
		// get class URI
		String className = clazz.getAttributeNS(RDF_PREFIX, "about");
		if (className.length() > 0){
			anonymous = false;
			classMembership = constructRIFMemeber(instance, className);
			this.subClassDescription = classMembership;
			this.superClassDescription = classMembership;
		} else {
			anonymous = true;
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);
		}
		this.subClassDescription = classMembership;
		this.superClassDescription = classMembership;
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
			
			RIFError error = new RIFError();
			
			addRule(rules, error, complementPair);
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
						
						RIFError error = new RIFError();
						
						addRule(rules, error, complementPair);
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

abstract class OWLPropertyCompiler extends OWLTranslater<RIFGroup> {

	protected final Element property;
	
	protected RIFFormula subPropertyDescription = null;
	protected RIFFormula superPropertyDescription = null;
	
	protected final RIFVar subject;
	protected RIFIRIConst propertyIRI = null;
	protected RIFDatum object = null;
	
	public OWLPropertyCompiler(Element property, RIFVar subject){
		this.property = property;
		this.subject = subject;
		
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
	
	public OWLPropertyCompiler(Element property, RIFVar subject, RIFDatum object){
		this(property, subject);
		this.object = object;		
	}
	
	protected abstract RIFGroup compileProper(RIFGroup rules);
	
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
	
	protected RIFGroup addRule(RIFGroup rules, RIFFormula head, RIFFormula body){
		RIFForAll forall = new RIFForAll();
		forall.addUniversalVar(subject);
		if (object instanceof RIFVar){
			forall.addUniversalVar((RIFVar) object);
		}
		
		RIFRule rule = new RIFRule();
		rule.setHead(head);
		rule.setBody(body);
		forall.setStatement(rule);
		
		rules.addSentence(forall);
		return rules;
	}
	
}

class OWLReferencedPropertyCompiler extends OWLPropertyCompiler {
	
	public OWLReferencedPropertyCompiler(URI propertyName, RIFVar subject, RIFDatum object) {
		super(null, subject, object);
		
		this.propertyIRI = new RIFIRIConst();
		this.propertyIRI.setData(propertyName);
	}
	
	public OWLReferencedPropertyCompiler(URI propertyName, RIFVar subject) {
		super(null, subject);
		
		this.propertyIRI = new RIFIRIConst();
		this.propertyIRI.setData(propertyName);
	}

	@Override
	protected RIFGroup compileProper(RIFGroup rules){
		RIFFrame frame = new RIFFrame();
		
		frame.setSubject(subject);
		frame.setPredicate(propertyIRI);
		frame.setObject(object);
		
		addRule(rules, frame, subPropertyDescription);
		addRule(rules, superPropertyDescription, frame);
		
		return rules;
	}
	
}

class OWLAnonymousPropertyCompiler extends OWLPropertyCompiler {
	
	public OWLAnonymousPropertyCompiler(Element property, RIFVar subject, RIFDatum object) {
		super(property, subject, object);
		// TODO Auto-generated constructor stub
	}
	
	public OWLAnonymousPropertyCompiler(Element property, RIFVar subject) {
		super(property, subject);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected RIFGroup compileProper(RIFGroup rules) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

class OWLNamedPropertyCompiler extends OWLPropertyCompiler {
	
	public OWLNamedPropertyCompiler(Element property, RIFVar subject, RIFDatum object) throws URISyntaxException {
		super(property, subject, object);
		if (propertyIRI == null){
			throw new URISyntaxException("Property IRI", "Property IRI missing or not a valid IRI.");
		}
	}
	
	public OWLNamedPropertyCompiler(Element property, RIFVar subject) throws URISyntaxException {
		super(property, subject);
		if (propertyIRI == null){
			throw new URISyntaxException("Property IRI", "Property IRI missing or not a valid IRI.");
		}
	}

	@Override
	protected RIFGroup compileProper(RIFGroup rules) {
		// TODO Auto-generated method stub
		return null;
	}
	
}