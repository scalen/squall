package org.openimaj.rifcore.conditions.data;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Node_Variable;

/**
 * @author david.monks
 *
 */
public class RIFVar extends RIFDatum {
	
	private Node_Variable node;
	
	/**
	 */
	public RIFVar(){
		this.node = null;
	}
	
	/**
	 * @param name
	 * @return 
	 */
	public RIFVar setName(String name){
		this.node = (Node_Variable) NodeFactory.createVariable(name);
		return this;
	}
	
	/**
	 * @return
	 */
	public Node_Variable getNode(){
		return this.node;
	}

}
