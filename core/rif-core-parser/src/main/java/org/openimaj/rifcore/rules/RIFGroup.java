package org.openimaj.rifcore.rules;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author david.monks
 *
 */
public class RIFGroup extends RIFSentence implements Iterable<RIFSentence> {

	private Set<RIFSentence> sentences;
	
	/**
	 * 
	 */
	public RIFGroup(){
		this.sentences = new HashSet<RIFSentence>();
	}
	
	/**
	 * @param sentence
	 */
	public void addSentence(RIFSentence sentence){
		this.sentences.add(sentence);
	}
	
	@Override
	public Iterator<RIFSentence> iterator(){
		return this.sentences.iterator();
	}
	
	@Override
	public String toString(String spacing) {
		StringBuilder string = new StringBuilder("Group (\n");
		for (RIFSentence sentence : this){
			string.append(spacing)
				  .append("  ")
				  .append(sentence.toString(spacing + "  "))
				  .append("\n");
		}
		return string.append(spacing)
					 .append(")")
					 .toString();
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder("Group (\n");
		for (RIFSentence sentence : this){
			string.append("  ")
				  .append(sentence.toString("  "))
				  .append("\n");
		}
		return string.append(")")
					 .toString();
	}
	
}
