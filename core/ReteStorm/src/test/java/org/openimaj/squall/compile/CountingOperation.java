package org.openimaj.squall.compile;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.openimaj.squall.compile.data.IOperation;
import org.openimaj.util.data.Context;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public final class CountingOperation implements IOperation<Context>, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6987720033468611350L;
	private int expected;
	private Map<String, String> filters;
	private int count;
	
	/**
	 * creates a CountingOperation that will never realistically be satisfied.
	 */
	public CountingOperation() {
		this(Integer.MAX_VALUE);
	}
	
	/**
	 * @param expected
	 */
	public CountingOperation(int expected) {
		this(expected, new HashMap<String, String>());
	}
	
	/**
	 * @param expected
	 * @param filts
	 */
	public CountingOperation(int expected, Map<String, String> filts){
		this.expected = expected;
		this.filters = filts;
		this.count = 0;
	}

	@Override
	public void setup() {
		System.out.println("Starting Test");
	}

	@Override
	public void cleanup() {
		if(this.count != this.expected){
			String msg = String.format("THE TEST FAILED Expected %d saw %d",this.expected,this.count);
			System.out.println(msg);
			throw new RuntimeException(msg);
		}
		else{
			System.out.println("Success!");
		}
	}

	@Override
	public void perform(Context object) {
		boolean pass = true;
		try {
			for (String key : filters.keySet()){
				if (!(pass &= object.get(key).toString().matches(filters.get(key)))) break;
			}
		} catch (NullPointerException e) {
			pass = false;
		}
		if (pass){
			System.out.println("Expected result: " + object);
			this.count ++;
		} else {
			System.out.println("Also saw: " + object);
		}
	}

	@Override
	public void write(Kryo kryo, Output output) {
		output.writeInt(this.expected);
		output.writeInt(this.count);
		kryo.writeClassAndObject(output, filters);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Kryo kryo, Input input) {
		this.expected = input.readInt();
		this.count = input.readInt();
		this.filters = (Map<String, String>) kryo.readClassAndObject(input);
	}

	@Override
	public boolean isStateless() {
		return false;
	}

	@Override
	public boolean forcedUnique() {
		return true;
	}
	
}