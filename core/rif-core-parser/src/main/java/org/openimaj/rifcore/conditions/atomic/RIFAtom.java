package org.openimaj.rifcore.conditions.atomic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openimaj.rifcore.conditions.data.RIFConst;
import org.openimaj.rifcore.conditions.data.RIFData;
import org.openimaj.rifcore.conditions.data.RIFDatum;

/**
 * @author david.monks
 *
 */
public class RIFAtom extends RIFAtomic implements Iterable<RIFDatum> {
	
	private RIFConst<?> op;
	private List<RIFDatum> args;
	
	/**
	 * 
	 */
	public RIFAtom(){
		this.args = new ArrayList<RIFDatum>();
	}
	
	/**
	 * @param op
	 */
	public void setOp(RIFConst<?> op){
		this.op = op;
	}
	
	/**
	 * @return
	 */
	public RIFConst<?> getOp(){
		return this.op;
	}
	
	/**
	 * @param arg
	 */
	public void addArg(RIFDatum arg){
		this.args.add(arg);
	}
	
	/**
	 * @param index
	 * @return
	 */
	public RIFData getArg(int index){
		return this.args.get(index);
	}
	
	/**
	 * @return
	 */
	public int getArgsSize(){
		return this.args.size();
	}
	
	@Override
	public Iterator<RIFDatum> iterator(){
		return this.args.iterator();
	}
	
	@Override
	public String toString(String spacing) {
		StringBuilder string = new StringBuilder(op.getNode().toString())
										.append("(");
		if (!this.args.isEmpty()){
			for (RIFDatum arg : this){
				string.append("\n")
					  .append(spacing)
					  .append("  ")
					  .append(arg.getNode().toString());
			}
			string.append("\n")
				  .append(spacing);
		}
		return string.append(")")
					 .toString();
	}
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder(op.getNode().toString())
										.append("(");
		if (!this.args.isEmpty()){
			for (RIFDatum arg : this){
				string.append("\n")
					  .append("  ")
					  .append(arg.getNode().toString());
			}
			string.append("\n");
		}
		return string.append(")")
					 .toString();
	}

}
