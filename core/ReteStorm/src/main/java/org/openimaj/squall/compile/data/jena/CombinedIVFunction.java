package org.openimaj.squall.compile.data.jena;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openimaj.squall.compile.data.IFunction;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * A {@link CombinedIVFunction} performs all functions on the data.
 * Implementations know how to make an initial, empty output of 
 * a function and further know how to combine the output of 
 * multiple functions in a pairwise manner
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @param <A> 
 * @param <B> 
 *
 */
@SuppressWarnings("serial")
public abstract class CombinedIVFunction<A,B> implements IFunction<A,B> {

	private List<IFunction<A, B>> functions;

	/**
	 */
	public CombinedIVFunction() {
		super();
		this.functions = new ArrayList<IFunction<A,B>>();
	}
	
	/**
	 * @param func add a function to apply
	 */
	public void addFunction(IFunction<A,B> func){
		this.functions.add(func);
	}
	
	protected Iterable<IFunction<A,B>> functions(){
		return new Iterable<IFunction<A,B>>(){
			@Override
			public Iterator<IFunction<A,B>> iterator() {
				return CombinedIVFunction.this.functions.iterator();
			}
			
		};
	}
	
	@Override
	public List<B> apply(A in) {
		List<B> out = initial();
		for (IFunction<A,B> func: this.functions) {
			out = combine(out,func.apply(in));
		}
		return out;
	}

	protected abstract List<B> combine(List<B> out, List<B> apply) ;

	protected abstract List<B> initial() ;
	
//	@Override
//	public String identifier() {
//		StringBuilder out = new StringBuilder("Combined:");
//		for (int i = 0; i < this.functions.size(); i++) {
//			out.append("\n")
//			   .append(this.functions.get(i).identifier());
//		}
//		return out.toString();
//	}
//	
//	@Override
//	public String identifier(Map<String, String> varmap) {
//		StringBuilder out = new StringBuilder("Combined:");
//		for (int i = 0; i < this.functions.size(); i++) {
//			out.append("\n")
//			   .append(this.functions.get(i).identifier(varmap));
//		}
//		return out.toString();
//	}
	
	@Override
	public void setup() {
		for (IFunction<A, B> func : this.functions) {
			func.setup();
		}
	}
	
	@Override
	public void cleanup() {
		for (IFunction<A, B> func : this.functions) {
			func.cleanup();
		}
	}
	
	@Override
	public String toString() {
		return this.functions.toString();
	}
	
	@Override
	public void write(Kryo kryo, Output output) {
		output.writeInt(this.functions.size());
		for (int i = 0; i < this.functions.size(); i++){
			kryo.writeClassAndObject(output, this.functions.get(i));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Kryo kryo, Input input) {
		int size = input.readInt();
		for (int i = 0; i < size; i++){
			this.functions.add((IFunction<A, B>) kryo.readClassAndObject(input));
		}
	}
	
	@Override
	public boolean isStateless() {
		for (IFunction<A, B> func : this.functions){
			if (!func.isStateless()){
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean forcedUnique() {
		for (IFunction<A, B> func : this.functions){
			if (func.forcedUnique()){
				return true;
			}
		}
		return false;
	}

}
