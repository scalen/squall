package org.openimaj.squall.compile.owl;

//import java.util.Iterator;
//import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

import org.openimaj.rifcore.conditions.atomic.RIFError;
import org.openimaj.rifcore.conditions.atomic.RIFFrame;
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
//import org.openimaj.rifcore.RIFRuleSet;
import org.openimaj.squall.compile.CompiledProductionSystem;
import org.openimaj.squall.compile.Compiler;
//import org.openimaj.squall.compile.ContextCPS;
//import org.openimaj.squall.data.ISource;
//import org.openimaj.util.data.Context;
//import org.openimaj.util.stream.Stream;

//import com.hp.hpl.jena.graph.Triple;
//import com.hp.hpl.jena.ontology.OntModelSpec;
//import com.hp.hpl.jena.query.Query;
//import com.hp.hpl.jena.query.QueryExecution;
//import com.hp.hpl.jena.query.QueryExecutionFactory;
//import com.hp.hpl.jena.query.QueryFactory;
//import com.hp.hpl.jena.rdf.model.Model;
//import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class OWLRuleCompiler implements Compiler<File> {

	private Document doc;
	
	@Override
	public CompiledProductionSystem compile(File owlxml) {
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder;
		try {
			dBuilder = dbFactory.newDocumentBuilder();
			doc = dBuilder.parse(owlxml);
			
			doc.getDocumentElement().normalize();
			
			// Get imports
		
			Collection<Element> classes = getChildElementsByTagNameNS((Element) doc, "http://www.w3.org/2002/07/owl#", "Class");
			RIFGroup rules = new RIFGroup();
			for (Element c :classes){
				rules = compileClassNode(rules, c);
			}
			Collection<Element> restrictions = getChildElementsByTagNameNS((Element) doc, "http://www.w3.org/2002/07/owl#", "Restriction");
			for (Element c :classes){
				rules = compileClassNode(rules, c);
			}
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
		
//		
//		// Get Sources and create Context-based Compiled Production system, then add the former to the latter.
//		List<ISource<Stream<Context>>> sources = type.firstObject();
//		ContextCPS ret = new ContextCPS();
//		for (ISource<Stream<Context>> source : sources) {
//			ret.addStreamSource(source);
//		}
//		
//		// Fetch Ontology and load into Jena Model performing the greatest available amount of in memory static reasoning.
//		Model ontology = type.secondObject();
//		Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RULE_INF, ontology);
//		
//		// Fetch Rule Set
//		RIFRuleSet rules = type.thirdObject();
//		// Use Rule Set to query Jena Model
//			// TODO: Convert rules to SPARQL query Strings
//		String sparql = "nothing";
//		Query query = QueryFactory.create(sparql);
//		QueryExecution execution = QueryExecutionFactory.create(query, model);
//		Iterator<Triple> result = execution.execConstructTriples();
//			// TODO: Run each query in turn, producing a separate Compiled Production System from each row of bindings returned
//			//		and the original RIFRule whos query produced it.  
		return null;
	}
	
	private RIFGroup compileClassNode(RIFGroup rules, Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}
		
		return compileClassNode(rules, instance, classMembership, classMembership, clazz);
	}
	
	private RIFGroup compileEquivalentClass(RIFGroup rules, RIFFormula equivClassDescription, Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}
		
		if (classMembership == null){
			rules = compileClassNode(rules, instance, equivClassDescription, equivClassDescription, clazz);
		} else {
			rules = compileClassNode(rules, instance, classMembership, classMembership, clazz);
			Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
			forallVariables.add(instance);
			addRule(rules, classMembership, equivClassDescription, forallVariables);
			addRule(rules, equivClassDescription, classMembership, forallVariables);
		}
		return rules;
	}
	
	private RIFGroup compileSubClassOf(RIFGroup rules, RIFFormula subClassDescription, Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}
		
		if (classMembership == null){
			rules = compileClassNode(rules, instance, subClassDescription, null, clazz);
		} else {
			rules = compileClassNode(rules, instance, classMembership, classMembership, clazz);
			Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
			forallVariables.add(instance);
			addRule(rules, classMembership, subClassDescription, forallVariables);
		}
		return rules;
	}
	
	private RIFGroup compileIntersectionMember(RIFGroup rules,
												RIFFormula intersectionDescription,
												Set<RIFAnd> intersections,
												Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}
		
		Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
		forallVariables.add(instance);
		
		if (classMembership == null){
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);

			RIFGroup subrules = new RIFGroup();
			subrules = compileClassNode(subrules, instance, classMembership, classMembership, clazz);
			
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
								intersectionDescription,
								forallVariables);
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
		} else {
			addRule(rules, classMembership, intersectionDescription, forallVariables);
			for (RIFAnd intersection : intersections){
				intersection.addFormula(classMembership);
			}
			
			rules = compileClassNode(rules, instance, classMembership, classMembership, clazz);
		}
		
		return rules;
	}
	
	private RIFGroup compileUnionMember(RIFGroup rules,
										RIFFormula unionDescription,
										RIFOr union,
										Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}

		Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
		forallVariables.add(instance);

		if (classMembership == null){
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);

			RIFGroup subrules = new RIFGroup();
			subrules = compileClassNode(subrules, instance, classMembership, classMembership, clazz);

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
						if (unionDescription != null){
							addRule(rules, unionDescription, rule.getBody(), forallVariables);
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
		} else {
			addRule(rules, unionDescription, classMembership, forallVariables);
			union.addFormula(classMembership);

			rules = compileClassNode(rules, instance, classMembership, classMembership, clazz);
		}

		return rules;
	}
	
	private RIFGroup compileComplementClass(RIFGroup rules,
											RIFFormula complementDescription,
											Element clazz){
		// create variable for instance of class
		RIFVar instance = new RIFVar();
		instance.setName("instance");
		// get class URI
		String className = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
		RIFMember classMembership = null;
		if (className.length() > 0){
			classMembership = constructRIFMemeber(instance, className); 
		}

		Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
		forallVariables.add(instance);

		if (classMembership == null){
			classMembership = new RIFAnon();
			classMembership.setInstance(instance);

			RIFGroup subrules = new RIFGroup();
			subrules = compileClassNode(subrules, instance, classMembership, classMembership, clazz);

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
						RIFAnd complementPair = new RIFAnd();
						complementPair.addFormula(rule.getBody());
						complementPair.addFormula(complementDescription);
						
						RIFError error = new RIFError();
						
						addRule(rules, error, complementPair, forallVariables);
						continue;
					}
					// all heads of bodyless rules, collectively, from a class union are also potentially implied by membership of this class
					if (rule.getBody() == classMembership){
						continue;
					}
				}

				rules.addSentence(sentence);
			}
		} else {
			RIFAnd complementPair = new RIFAnd();
			complementPair.addFormula(classMembership);
			complementPair.addFormula(complementDescription);
			
			RIFError error = new RIFError();
			
			addRule(rules, error, complementPair, forallVariables);

			rules = compileClassNode(rules, instance, classMembership, classMembership, clazz);
		}

		return rules;
	}
	
	private RIFGroup compileRestrictionFragment(RIFGroup rules, RIFOr classDescription, RIFVar instance, RIFFormula subClassDescription, RIFFormula superClassDescription, Element restriction){
		Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
		forallVariables.add(instance);
		
		// establish property in question
		Collection<Element> properties = getChildElementsByTagNameNS(restriction, "http://www.w3.org/2002/07/owl#", "onProperty");
		RIFIRIConst propertyIRI = new RIFIRIConst();
		try {
			Element property = properties.iterator().next();
			String propertyName = property.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
			try {
				URI propertyURI = new URI(propertyName);
				propertyIRI.setData(propertyURI);
			} catch (URISyntaxException e) {
				throw new RuntimeException("Property IRI " + propertyName + " is not a valid URI.", e);
			}
		} catch (NoSuchElementException e) {
			throw new RuntimeException("No property specified in Restriction " + (subClassDescription == null ? "at ontology root" : subClassDescription.toString()) + ".",e); 
		}
		
		// create a property object var name not previously used in this descent of class descriptions by appending tilda until it no longer appears.
		String objectVarName = "propertyObject";
		while (instance.getNode().getName().startsWith(objectVarName)){
			objectVarName += "~";
		}
	
		RIFVar propertyObject = new RIFVar();
		propertyObject.setName(objectVarName);
		
		// some values from
		Collection<Element> ranges = getChildElementsByTagNameNS(restriction, "http://www.w3.org/2002/07/owl#", "someValuesFrom");
		
		if (!ranges.isEmpty()){
			// establish description of restriction as an existential expression with a single additional variable
			RIFExists restrictionDescription = new RIFExists();
			restrictionDescription.addExistentialVar(propertyObject);
			
			// establish triple template describing the application of the restricted property
			RIFFrame tripleTemplate = new RIFFrame();
			tripleTemplate.setSubject(instance);
			tripleTemplate.setPredicate(propertyIRI);
			tripleTemplate.setObject(propertyObject);
			// determine the nature of the restriction (qualified/unqualified)
			try {
				Element range = ranges.iterator().next();
				String rangeName = range.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");

				if (rangeName.equals("http://www.w3.org/2002/07/owl#Thing")){
					// The restriction is unqualified
					restrictionDescription.addFormula(tripleTemplate);
				} else {
					// the restriction is qualified...
					// ... so establish an intersection of conditions including the existing triple template
					RIFAnd and = new RIFAnd();
					and.addFormula(tripleTemplate);
					
					if (rangeName.length() > 0) {
						and.addFormula(constructRIFMemeber(propertyObject, rangeName));
					} else {
						// ... with a class description
						Collection<Element> classes = getChildElementsByTagNameNS(range, "http://www.w3.org/2002/07/owl#", "Class");
						if (!classes.isEmpty()){
							Element clazz = classes.iterator().next();
							
							// get class URI
							rangeName = clazz.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
							RIFMember rangeMembership = null;
							if (rangeName.length() > 0){
								rangeMembership = constructRIFMemeber(propertyObject, rangeName); 
							}
	
							if (rangeMembership == null){
								rangeMembership = new RIFAnon();
								rangeMembership.setInstance(propertyObject);
	
								RIFGroup subrules = new RIFGroup();
								subrules = compileClassNode(subrules, propertyObject, rangeMembership, rangeMembership, clazz);
	
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
								
								and.addFormula(qualifyingBodies);
							} else {
								and.addFormula(rangeMembership);
	
								rules = compileClassNode(rules, propertyObject, rangeMembership, rangeMembership, clazz);
							}
						}
					}
					// Finally, add the intersection of conditions to satisfy the restriction to the restrictionDefinition.
					restrictionDescription.addFormula(and);
				}
				
				if (superClassDescription != null){
					addRule(rules, superClassDescription, restrictionDescription, forallVariables);
				}
				classDescription.addFormula(restrictionDescription);
				return rules;
			} catch (NoSuchElementException e) {
				throw new RuntimeException("No property specified in Restriction " + (superClassDescription == null ? "at ontology root" : superClassDescription.toString()) + ".",e); 
			}
		}
		
		
		
		return rules;
	}
	
	private RIFGroup compileClassNode(RIFGroup rules, RIFVar instance, RIFFormula subClassDescription, RIFFormula superClassDescription, Element clazz){
		Collection<RIFVar> forallVariables = new HashSet<RIFVar>();
		forallVariables.add(instance);
		
		RIFOr classDescription = new RIFOr();
		
		// Check if the class is a restriction, and divert to the restriction processing method.
		Collection<Element> properties = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "onProperty");
		if (!properties.isEmpty()){
			rules = compileRestrictionFragment(rules, classDescription, instance, subClassDescription, superClassDescription, clazz);
		} else {
			// start processing non-restriction class
			
			// class intersection
			Collection<Element> classIntersections = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "intersectionOf");
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
								subClassDescription == null ? classDescription : subClassDescription,
								forallVariables);
						// class membership of all intersected classes implies membership of the class intersection 
						and.addFormula(intersectedClassMembership);
					}
				}
				
				Set<RIFAnd> ands = new HashSet<RIFAnd>();
				if (and != null){
					ands.add(and);
				}
				
				Collection<Element> classes = getChildElementsByTagNameNS(intersection, "http://www.w3.org/2002/07/owl#", "Class");
				if (!classes.isEmpty()){
					for (Element ic : classes){
						rules = compileIntersectionMember(rules,
														subClassDescription == null ? classDescription : subClassDescription,
														ands,
														ic);
					}
				}
				Collection<Element> restrictions = getChildElementsByTagNameNS(intersection, "http://www.w3.org/2002/07/owl#", "Restriction");
				if (!restrictions.isEmpty()){
					for (Element ir : restrictions){
						rules = compileIntersectionMember(rules,
															subClassDescription == null ? classDescription : subClassDescription,
															ands,
															ir);
					}
				}
				// add all possible intersections to the set of bodies implying membership of this class
				for (RIFAnd a : ands){
					if (superClassDescription != null){
						addRule(rules, superClassDescription, a, forallVariables);
					}
					classDescription.addFormula(a);
				}
			}
			
			// class union
			Collection<Element> classUnions = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "unionOf");
			if (!classUnions.isEmpty()){
				for (Element union : classUnions){
					RIFOr or = new RIFOr();
					
					Collection<Element> classReferences = getChildElementsByTagNameNS(union, "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Description");
					for (Element reference : classReferences){
						String referenceName = reference.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "about");
						 
						RIFMember unionMembership = constructRIFMemeber(instance, referenceName);
						
						// class membership of any of the unioned classes implies membership of the class union
						if (superClassDescription != null){
							addRule(rules, superClassDescription, unionMembership, forallVariables);
						}
						// class membership of the class union implies membership of at least one of the unioned classes
						or.addFormula(unionMembership);
					}
					
					Collection<Element> classes = getChildElementsByTagNameNS(union, "http://www.w3.org/2002/07/owl#", "Class");
					if (!classes.isEmpty()){
						for (Element uc : classes){
							rules = compileUnionMember(rules, superClassDescription, or, uc);
						}
						// add the union to the set of heads implying membership of this class
						addRule(rules, or, subClassDescription, forallVariables);
					}
				}
			}
			
			// Complementclass statements
			Collection<Element> complementClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "complementOf");
			if (!complementClasses.isEmpty()){
				for (Element complementClass : complementClasses){
					String complementClassName = complementClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
					if (complementClassName.length() > 0){
						RIFMember complementClassMembership = constructRIFMemeber(instance, complementClassName);
						
						RIFAnd complementPair = new RIFAnd();
						complementPair.addFormula(complementClassMembership);
						complementPair.addFormula(subClassDescription == null ? classDescription : subClassDescription);
						
						RIFError error = new RIFError();
						
						addRule(rules, error, complementPair, forallVariables);
					}
					if (complementClass.hasChildNodes()){
						Collection<Element> classes = getChildElementsByTagNameNS(complementClass, "http://www.w3.org/2002/07/owl#", "Class");
						if (!classes.isEmpty()){
							for (Element ec : classes){
								rules = compileComplementClass(rules,
																subClassDescription == null ? classDescription : subClassDescription,
																ec);				
							}
						}
					}				
				}
			}
		}
		
		// Super- and equivalent-class expressions
		
		// Equivalentclass statements
		Collection<Element> equivalentClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2002/07/owl#", "equivalentClass");
		if (!equivalentClasses.isEmpty()){
			for (Node c : equivalentClasses){
				Element equivClass = (Element) c;
				String equivClassName = equivClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
				if (equivClassName.length() > 0){
					RIFMember equivClassMembership = constructRIFMemeber(instance, equivClassName);
					
					addRule(rules,
							equivClassMembership,
							subClassDescription == null ? classDescription : subClassDescription,
							forallVariables);
					addRule(rules,
							superClassDescription == null ? classDescription : superClassDescription,
							equivClassMembership,
							forallVariables);
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
		
		// Subclass statements
		Collection<Element> superClasses = getChildElementsByTagNameNS(clazz, "http://www.w3.org/2000/01/rdf-schema#", "subClassOf");
		if (!superClasses.isEmpty()){
			for (Node c : superClasses){
				Element superClass = (Element) c;
				String superClassName = superClass.getAttributeNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "resource");
				if (superClassName.length() > 0){
					addRule(rules,
							constructRIFMemeber(instance, superClassName),
							subClassDescription == null ? classDescription : subClassDescription,
							forallVariables);
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

	private RIFGroup addRule(RIFGroup rules, RIFFormula head, RIFFormula body, Collection<RIFVar> forallVariables){
		RIFForAll forall = new RIFForAll();
		for (RIFVar var : forallVariables){
			forall.addUniversalVar(var);
		}
		
		RIFRule rule = new RIFRule();
		rule.setHead(head);
		rule.setBody(body);
		forall.setStatement(rule);
		
		rules.addSentence(forall);
		return rules;
	}
	
	private RIFMember constructRIFMemeber(RIFVar instance, String className){
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

	private Collection<Element> getChildElementsByTagNameNS(Element parent, String namespace, String tag){
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