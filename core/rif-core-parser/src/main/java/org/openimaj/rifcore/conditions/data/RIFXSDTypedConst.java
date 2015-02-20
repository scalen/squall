package org.openimaj.rifcore.conditions.data;

import java.net.URI;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Node_Concrete;

/**
 * @author david.monks
 *
 */
public class RIFXSDTypedConst extends RIFConst<String> {
	
	private final URI dtype; 
	
	/**
	 * @param dtype
	 */
	public RIFXSDTypedConst(URI dtype){
		if(!dtype.toString().startsWith(XSDDatatype.XSD)){
			throw new UnsupportedOperationException("Unsupported dtype");
		}
		this.dtype = dtype;
	}
	
	@Override
	public String getDatatype(){
		return this.dtype.toString();
	}

	@Override
	public RIFXSDTypedConst setData(String data) {
		this.node = (Node_Concrete) NodeFactory.createLiteral(data,new XSDDatatype(this.dtype.getFragment()));
		return this;
	}

}
