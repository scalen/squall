package org.openimaj.squall.orchestrate.revised;

import org.openimaj.squall.compile.data.revised.AnonimisedRuleVariableHolder;
import org.openimaj.squall.compile.data.revised.IOperation;
import org.openimaj.squall.compile.data.revised.IVFunction;
import org.openimaj.squall.compile.data.revised.Initialisable;
import org.openimaj.squall.compile.data.revised.VariableHolder;
import org.openimaj.squall.orchestrate.NamedNode;
import org.openimaj.util.data.Context;
import org.openimaj.util.function.Source;
import org.openimaj.util.stream.Stream;

/**
 * A {@link NamedNode} that holds an {@link IVFunction}
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 */
public class RevisedNNIVFunction extends RevisedNamedNode<IVFunction<Context, Context>>{

	private IVFunction<Context, Context> varfunc;
	private IVFunction<Context, Context> wrapped;

	/**
	 * @param parent
	 * @param name
	 * @param func
	 */
	public RevisedNNIVFunction(RevisedOrchestratedProductionSystem parent, String name, IVFunction<Context, Context> func) {
		super(parent, name);
		this.varfunc = func;
		this.wrapped = new RevisedWrappedIVFunction(func, this);
	}
	
	public IVFunction<Context, Context> getData(){
		return varfunc;
	}

	@Override
	public boolean isSource() {
		return false;
	}

	@Override
	public boolean isFunction() {
		return true;
	}

	@Override
	public Source<Stream<Context>> getSource() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IVFunction<Context, Context> getFunction() {
		return this.wrapped;
	}

	@Override
	public Initialisable getInit() {
		return this.varfunc;
	}

	@Override
	public boolean isInitialisable() {
		return true;
	}

	@Override
	public boolean isVariableHolder() {
		return true;
	}

	@Override
	public AnonimisedRuleVariableHolder getVariableHolder() {
		return this.wrapped;
	}

	@Override
	public boolean isOperation() {
		return false;
	}

	@Override
	public IOperation<Context> getOperation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isReentrantSource() {
		return false;
	}
	
}