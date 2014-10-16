package org.openimaj.rifcore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.openimaj.rifcore.imports.profiles.RIFEntailmentImportProfiles;
import org.openimaj.rifcore.imports.profiles.RIFImportProfiles.ProfileNotSupportedException;
import org.openimaj.rifcore.imports.profiles.RIFOWLImportProfiles;
import org.openimaj.rifcore.imports.schemes.RIFImportSchemes;
import org.openimaj.rifcore.rules.RIFGroup;
import org.openimaj.rifcore.rules.RIFSentence;
import org.xml.sax.SAXException;

/**
 * Converts a set of rules expressed in RIF/XML to a set of rules in a different format.
 * @author David Monks <david.monks@zepler.net>
 */
public class RIFRuleSet implements Iterable<RIFGroup> {
	
	//  VARIABLES
	
	private URI base;
	private Map<String,URI> prefixes;
	private Map<URI,URI> imports;
	private List<RIFGroup> rootGroup;
	
	private Stack<URI> profile;
	
	private RIFEntailmentImportProfiles parserMap;
	private RIFImportSchemes schemeMap;
	
	//  CONSTRUCTORS
	
	/**
	 * @param profile 
	 * @param pm 
	 * 
	 */
	public RIFRuleSet(URI profile,
					  RIFEntailmentImportProfiles pm){
		super();
		
		this.profile = new Stack<URI>();
		this.prefixes = new HashMap<String, URI>();
		this.imports = new HashMap<URI, URI>();
		this.rootGroup = new ArrayList<RIFGroup>();
		
		this.profile.push(profile);
		this.parserMap = pm;
		this.schemeMap = new RIFImportSchemes();
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param sm 
	 * 
	 */
	public RIFRuleSet(URI profile,
					  RIFEntailmentImportProfiles pm,
					  RIFImportSchemes sm){
		this(profile, pm);
		this.schemeMap = sm;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param base
	 * @param prefixes
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
			  		  URI base,
					  Map<String,URI> prefixes){
		this(profile, pm);
		
		this.base = base;
		this.prefixes = prefixes;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param sm 
	 * @param base
	 * @param prefixes
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
					  RIFImportSchemes sm,
			  		  URI base,
					  Map<String,URI> prefixes){
		this(profile, pm, sm);
		
		this.base = base;
		this.prefixes = prefixes;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param imports
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
			  		  Map<URI,URI> imports){
		this(profile, pm);
		
		this.imports = imports;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param sm 
	 * @param imports
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
					  RIFImportSchemes sm,
			  		  Map<URI,URI> imports){
		this(profile, pm, sm);
		
		this.imports = imports;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param base
	 * @param prefixes
	 * @param imports
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
			  		  URI base,
					  Map<String,URI> prefixes,
					  Map<URI,URI> imports){
		this(profile, pm, base, prefixes);
				
		this.imports = imports;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param sm 
	 * @param base
	 * @param prefixes
	 * @param imports
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
					  RIFImportSchemes sm,
			  		  URI base,
					  Map<String,URI> prefixes,
					  Map<URI,URI> imports){
		this(profile, pm, sm, base, prefixes);
				
		this.imports = imports;
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param base
	 * @param prefixes
	 * @param imports
	 * @param root
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
			  		  URI base,
					  Map<String,URI> prefixes,
					  Map<URI,URI> imports,
					  RIFGroup root){
		this(profile, pm, base, prefixes, imports);
		
		this.rootGroup.add(root);
	}
	
	/**
	 * @param profile 
	 * @param pm 
	 * @param sm 
	 * @param base
	 * @param prefixes
	 * @param imports
	 * @param root
	 */
	public RIFRuleSet(URI profile,
			  		  RIFEntailmentImportProfiles pm,
					  RIFImportSchemes sm,
			  		  URI base,
					  Map<String,URI> prefixes,
					  Map<URI,URI> imports,
					  RIFGroup root){
		this(profile, pm, sm, base, prefixes, imports);
		
		this.rootGroup.add(root);
	}
	
	//  GETTERS AND SETTERS
	
	/**
	 * @param base
	 */
	public void setBase(URI base){
		this.base = base;
	}
	
	/**
	 * @return
	 */
	public URI getBase(){
		return this.base;
	}
	
	/**
	 * @param pref
	 * @param loc
	 */
	public void addPrefix(String pref, URI loc){
		this.prefixes.put(pref, loc);
	}
	
	/**
	 * @param pref
	 * @return
	 */
	public URI getPrefix(String pref){
		return this.prefixes.get(pref);
	}
	
	/**
	 * @return
	 */
	public Set<String> getPrefixKeySet(){
		return this.prefixes.keySet();
	}
	
	/**
	 * @param loc
	 * @param prof
	 */
	public void addImport(URI loc, URI prof) {
		if (this.parserMap == null){
			this.imports.put(loc, prof);
			return;
		}
		
		InputStream resourceAsStream = this.schemeMap.get(loc.getScheme()).getInputStream(loc);
		if (prof == null){
			try {
				this.parserMap.parse(resourceAsStream, this.profile.peek(), this);
			} catch (ProfileNotSupportedException e) {
				this.imports.put(loc, this.profile.peek());
			} catch (IOException e){
				throw new RuntimeException("IO Error on location "+loc+" with core profile",e);
			} catch (SAXException e) {
				throw new RuntimeException("Parsing Error on location "+loc+" with core profile",e);
			} catch (UnsupportedOperationException e) {
				throw new RuntimeException("Unsupported operation while processing "+loc+" with core profile",e);
			} catch (Exception e) {
				throw new RuntimeException("Exception while processing "+loc+" with core profile",e);
			}
			return;
		}
		this.profile.push(prof);
		try {
			this.parserMap.parse(resourceAsStream, prof, this);
		} catch (ProfileNotSupportedException e) {
			this.imports.put(loc, prof);
		} catch (IOException e){
			throw new RuntimeException("IO Error on location "+loc+" with profile "+prof,e);
		} catch (SAXException e) {
			throw new RuntimeException("Parsing Error on location "+loc+" with profile "+prof,e);
		} catch (UnsupportedOperationException e) {
			throw new RuntimeException("Unsupported operation while processing "+loc+" with profile "+prof,e);
		} catch (Exception e) {
			throw new RuntimeException("Exception while processing "+loc+" with profile"+prof,e);
		} finally {
			this.profile.pop();
		}
	}
	
	/**
	 * @param loc
	 * @return
	 */
	public URI getImport(URI loc){
		return this.imports.get(loc);
	}
	
	/**
	 * @return
	 */
	public Set<URI> getImportKeySet(){
		return this.imports.keySet();
	}
	
	/**
	 * @param root
	 */
	public void addRootGroup(RIFGroup root){
		this.rootGroup.add(root);
	}
	
	/**
	 * @return
	 */
	public RIFGroup getRootGroup(){
		return this.rootGroup.get(this.rootGroup.size() - 1);
	}
	
	public String toString(){
		String fbase = "base: "+ (base == null ? "" : base.toString());
		String fprefixes = "prefixes:";
		for (String pref : prefixes.keySet()){
			fprefixes += "\n\t[ "+pref+": "+prefixes.get(pref).toString()+" ]";
		}
		String fimports = "imports:";
		for (URI loc : imports.keySet()){
			fimports += "\n\t[ loc: "+loc.toString()+", prof: "+(imports.get(loc) == null ? "" : imports.get(loc).toString())+" ]";
		}
		String fgroups = "groups:";
		for (RIFSentence sentence : rootGroup){
			fgroups += "\n"+sentence.toString();
		}
		return "[ \n  "+fbase+"\n  "+fprefixes+"\n  "+fimports+"\n  "+fgroups+"\n]";
	}

	@Override
	public Iterator<RIFGroup> iterator() {
		return this.rootGroup.iterator();
	}
	
	//  RIFXMLContentHandler Class
	
		
	
}