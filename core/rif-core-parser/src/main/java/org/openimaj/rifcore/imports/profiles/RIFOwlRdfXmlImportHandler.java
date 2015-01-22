package org.openimaj.rifcore.imports.profiles;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.rifcore.conditions.atomic.RIFError;
import org.openimaj.rifcore.conditions.data.RIFIRIConst;
import org.openimaj.rifcore.conditions.data.RIFVar;
import org.openimaj.rifcore.conditions.formula.RIFAnd;
import org.openimaj.rifcore.conditions.formula.RIFAnon;
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
		Collection<Element> ontologies = getChildElementsByTagNameNS((Element) doc, "http://www.w3.org/2002/07/owl#", "Ontology");
		for (Element o : ontologies){
			Collection<Element> imports = getChildElementsByTagNameNS((Element) o, "http://www.w3.org/2002/07/owl#", "imports");
			for (Element i : imports){
				String loc = i.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
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
		
		Collection<Element> classes = getChildElementsByTagNameNS((Element) doc, "http://www.w3.org/2002/07/owl#", "Class");
		classes.addAll(getChildElementsByTagNameNS((Element) doc, "http://www.w3.org/2002/07/owl#", "Restriction"));
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
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		if (className.length() > 0){
			RIFMember classMembership = constructRIFMemeber(instance, className);
			this.subClassDescription = classMembership;
			this.superClassDescription = classMembership;
		}
				
		return this.compileProper(rules);
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
	
	protected RIFGroup compileProper(RIFGroup rules){
		RIFOr classDescription = new RIFOr();
		
		Collection<Element> classIntersections = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "intersectionOf");
		Collection<Element> classUnions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "unionOf");
		Collection<Element> complementClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "complementOf");
		
		Collection<Element> existentialRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "someValueFrom");
		Collection<Element> universalRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "allValuesFrom");
		Collection<Element> valueRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "hasValue");
		
		Collection<Element> cardinalityRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "cardinality");
		Collection<Element> maxCardRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "maxCardinality");
		Collection<Element> minCardRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "minCardinality");
		Collection<Element> maxQualCardRestrictions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "maxQualifiedCardinality");
		
		if (!classIntersections.isEmpty()){
			rules = compileIntersections(rules, classIntersections, classDescription);
		} else if (!classUnions.isEmpty()){
			rules = compileUnions(rules, classUnions, classDescription);
		} else if (!complementClasses.isEmpty()){
			// handled along with disjoint classes
		} else if (!existentialRestrictions.isEmpty()){
			rules = compileExistentialRestrictions(rules, existentialRestrictions, classDescription);
		} else if (!universalRestrictions.isEmpty()){
			rules = compileUniversalRestrictions(rules, universalRestrictions, classDescription);
		} else if (!valueRestrictions.isEmpty()){
			rules = compileValueRestrictions(rules, valueRestrictions, classDescription);
		} else if (!cardinalityRestrictions.isEmpty()){
			rules = compilecardinalityRestrictions(rules, cardinalityRestrictions, classDescription);
		} else if (!maxCardRestrictions.isEmpty()){
			rules = compileMaxCardRestrictions(rules, maxCardRestrictions, classDescription);
		} else if (!minCardRestrictions.isEmpty()){
			rules = compileMinCardRestrictions(rules, minCardRestrictions, classDescription);
		} else if (!maxQualCardRestrictions.isEmpty()){
			rules = compileMaxQualCardRestrictions(rules, maxQualCardRestrictions, classDescription);
		}
		
		// Disjoint Classes
		// Not strictly accurate outside of OWL 2 RL entailments, but within that scope they are treated the same
		complementClasses.addAll(getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "disjointWith"));
		rules = compileComplements(rules, complementClasses, classDescription);
		
		// Equivalent Classes
		Collection<Element> equivalentClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "equivalentClass");
		if (!equivalentClasses.isEmpty()){
			for (Node c : equivalentClasses){
				Element equivClass = (Element) c;
				String equivClassName = equivClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
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
					Collection<Element> classes = getChildElementsByTagNameNS(equivClass, "http://www.w3.org/2002/07/owl#", "Class");
					if (!classes.isEmpty()){
						for (Node ec : classes){
							rules = compileEquivalentClass(rules,
														subClassDescription == null ? classDescription : subClassDescription,
														(Element) ec);				
						}
					}
				}				
			}
		}
		
		// Subclasses
		Collection<Element> superClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2000/01/rdf-schema#", "subClassOf");
		if (!superClasses.isEmpty()){
			for (Node c : superClasses){
				Element superClass = (Element) c;
				String superClassName = superClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
				if (superClassName.length() > 0){
					addRule(rules,
							constructRIFMemeber(instance, superClassName),
							subClassDescription == null ? classDescription : subClassDescription);
				}
				if (superClass.hasChildNodes()){
					Collection<Element> classes = getChildElementsByTagNameNS(superClass, "http://www.w3.org/2002/07/owl#", "Class");
					if (!classes.isEmpty()){
						for (Node sc : classes){
							rules = compileSubClassOf(rules,
													subClassDescription == null ? classDescription : subClassDescription,
													(Element) sc);
						}
					}
				}				
			}
		}
		
		return rules;
	}

	private RIFGroup compileComplements(RIFGroup rules, Collection<Element> complementClasses, RIFOr classDescription) {
		for (Element complementClass : complementClasses){
			String complementClassName = complementClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
			if (complementClassName.length() > 0){
				RIFMember complementClassMembership = constructRIFMemeber(instance, complementClassName);
				
				RIFAnd complementPair = new RIFAnd();
				complementPair.addFormula(complementClassMembership);
				complementPair.addFormula(subClassDescription == null ? classDescription : subClassDescription);
				
				RIFError error = new RIFError();
				
				addRule(rules, error, complementPair);
			}
			if (complementClass.hasChildNodes()){
				Collection<Element> classes = getChildElementsByTagNameNS(complementClass, "http://www.w3.org/2002/07/owl#", "Class");
				classes.addAll(getChildElementsByTagNameNS(complementClass, "http://www.w3.org/2002/07/owl#", "Restriction"));
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
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(intersection, "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description");
			if (!classReferences.isEmpty()) {
				and = new RIFAnd();
				
				for (Element reference : classReferences){
					String referenceName = reference.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
					 
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
			
			Collection<Element> classes = getChildElementsByTagNameNS(intersection, "http://www.w3.org/2002/07/owl#", "Class");
			classes.addAll(getChildElementsByTagNameNS(intersection, "http://www.w3.org/2002/07/owl#", "Restriction"));
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
			
			Collection<Element> classReferences = getChildElementsByTagNameNS(union, "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description");
			for (Element reference : classReferences){
				String referenceName = reference.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
				 
				RIFMember unionMembership = constructRIFMemeber(instance, referenceName);
				
				// class membership of any of the unioned classes implies membership of the class union
				if (superClassDescription != null){
					addRule(rules, superClassDescription, unionMembership);
				}
				// class membership of the class union implies membership of at least one of the unioned classes
				or.addFormula(unionMembership);
			}
			
			Collection<Element> classes = getChildElementsByTagNameNS(union, "http://www.w3.org/2002/07/owl#", "Class");
			classes.addAll(getChildElementsByTagNameNS(union, "http://www.w3.org/2002/07/owl#", "Restriction"));
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
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
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
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
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
			addRule(rules, parentClassDescription, classMembership);
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
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
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