package org.openimaj.rdf.rules;

/**
 * @author david.monks
 *
 */
public class RIFVar implements RIFDatum {
	
	private String name;
	
	/**
	 * @param name
	 */
	public RIFVar(){
		
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	/**
	 * @return
	 */
	public String getName(){
		return this.name;
	}

}
