package org.openimaj.squall.functions.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.openimaj.squall.compile.JoinComponent.DecomposedFunctionJoinComponent;
import org.openimaj.squall.compile.data.IFunction;
import org.openimaj.squall.compile.data.RuleWrappedFunction;
import org.openimaj.squall.functions.filters.BindingsComparingFilterFunction.RuleWrappedBindingsFilter.TripleBindingsFiltARVH;
import org.openimaj.util.data.Context;
import org.openimaj.util.data.ContextKey;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Node_Concrete;
import com.hp.hpl.jena.graph.Node_Variable;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.reasoner.TriplePattern;
import com.hp.hpl.jena.reasoner.rulesys.Functor;

/**
 * @author David Monks (dm11g08@ecs.soton.ac.uk)
 *
 */
public abstract class BindingsComparingFilterFunction extends BaseFilterFunction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2686316545791268467L;

	private final static Logger logger = Logger.getLogger(BindingsComparingFilterFunction.class);
	
	private static final String SUBJECT = "s", OBJECT = "o", PREDICATE = "p";
	
	/**
	 * @author David Monks (dm11g08@ecs.soton.ac.uk)
	 *
	 */
	public static class TripleFilteringDecomposedJoinComponent extends DecomposedFunctionJoinComponent {

		private final TriplePattern pattern;
		
		/**
		 * @param p
		 */
		public TripleFilteringDecomposedJoinComponent(TriplePattern p) {
			this.pattern = p;
		}
		
		@Override
		public List<RuleWrappedFunction<? extends IFunction<Context, Context>>> getComponents(
				boolean chained) {
			TriplePattern clause = this.pattern;
			
			List<RuleWrappedFunction<? extends IFunction<Context, Context>>> chain =
					new ArrayList<RuleWrappedFunction<? extends IFunction<Context, Context>>>();
			
			RuleWrappedBindingsFilter.TripleBasedARVH prev = new RuleWrappedBindingsFilter.BlankTripleBindingsFiltARVH(clause);
				
			if (!clause.getPredicate().isVariable()){
				Node p = clause.getPredicate();
				clause = new TriplePattern(
							clause.getSubject(),
							prev.getPredicate(),
//							NodeFactory.createVariable(PREDICATE),
							clause.getObject()
						);
				RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
								prev,
								(Node_Variable) clause.getPredicate(),
								p
						); 
				chain.add(next);
				if (chained){
					prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
				}
			}
			if (!clause.getObject().isVariable()){
				Node o = clause.getObject();
				clause = new TriplePattern(
							clause.getSubject(),
							clause.getPredicate(),
							prev.getObject()
//							NodeFactory.createVariable(OBJECT)
						);
				RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
							prev,
							(Node_Variable) clause.getObject(),
							o
					); 
				chain.add(next);
				if (chained){
					prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
				}
			}
			if (!clause.getSubject().isVariable()){
				Node s = clause.getSubject();
				clause = new TriplePattern(
							prev.getSubject(),
//							NodeFactory.createVariable(SUBJECT),
							clause.getPredicate(),
							clause.getObject()
						);
				RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
								prev,
								(Node_Variable) clause.getSubject(),
								s
						); 
				chain.add(next);
				if (chained){
					prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
				}
			}
			if (clause.getSubject().isVariable()){
				if (clause.getSubject().matches(clause.getPredicate())){
					if (clause.getSubject().matches(clause.getObject())){
						clause = new TriplePattern(
									prev.getSubject(),
//									NodeFactory.createVariable(SUBJECT),
									prev.getPredicate(),
//									NodeFactory.createVariable(PREDICATE),
									prev.getObject()
//									NodeFactory.createVariable(OBJECT)
								);
						RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
										prev,
										(Node_Variable) clause.getSubject(),
										clause.getPredicate()
								);
						chain.add(next);
						if (chained){
							prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
						}
						next = new RuleWrappedBindingsFilter(
										prev,
										(Node_Variable) clause.getSubject(),
										clause.getObject()
								);
						chain.add(next);
						if (chained){
							prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
						}
					} else {
						clause = new TriplePattern(
									prev.getSubject(),
//									NodeFactory.createVariable(SUBJECT),
									prev.getPredicate(),
//									NodeFactory.createVariable(PREDICATE),
									clause.getObject()
								);
						RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
										prev,
										(Node_Variable) clause.getSubject(),
										clause.getPredicate()
								);
						chain.add(next);
						if (chained){
							prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
						}
					}
				} 
				if (clause.getSubject().matches(clause.getObject())){
					clause = new TriplePattern(
								prev.getSubject(),
//								NodeFactory.createVariable(SUBJECT),
								clause.getPredicate(),
								prev.getObject()
//								NodeFactory.createVariable(OBJECT)
							);
					RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
									prev,
									(Node_Variable) clause.getSubject(),
									clause.getObject()
							); 
					chain.add(next);
					if (chained){
						prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
					}
				}
			}
			if (clause.getPredicate().isVariable()){
				if (clause.getPredicate().matches(clause.getObject())){
					clause = new TriplePattern(
								clause.getSubject(),
								prev.getPredicate(),
//								NodeFactory.createVariable(PREDICATE),
								prev.getObject()
//								NodeFactory.createVariable(OBJECT)
							);
					RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
									prev,
									(Node_Variable) clause.getPredicate(),
									clause.getObject()
							); 
					chain.add(next);
					if (chained){
						prev = (RuleWrappedBindingsFilter.TripleBindingsFiltARVH) next.getVariableHolder();
					}
				}
			}
			
			return chain;
		}
		
	}
	
	/**
	 * @author David Monks (dm11g08@ecs.soton.ac.uk)
	 *
	 */
	public static class AtomFilteringDecomposedJoinComponent extends DecomposedFunctionJoinComponent {

		private final Functor pattern;
		
		/**
		 * @param p
		 */
		public AtomFilteringDecomposedJoinComponent(Functor p) {
			this.pattern = p;
		}
		
		@Override
		public List<RuleWrappedFunction<? extends IFunction<Context, Context>>> getComponents(
				boolean chained) {
			Node[] newArgs = new Node[this.pattern.getArgLength()];
			for (int i = 0; i < newArgs.length; i++){
				newArgs[i] = this.pattern.getArgs()[i];
			}
			Functor clause = new Functor(this.pattern.getName(),newArgs);
			
			List<RuleWrappedFunction<? extends IFunction<Context, Context>>> chain =
					new ArrayList<RuleWrappedFunction<? extends IFunction<Context, Context>>>();
			
			RuleWrappedBindingsFilter.AtomBasedARVH prev = new RuleWrappedBindingsFilter.BlankAtomBindingsFiltARVH(clause);
			
			int index = 0;
			while (index < clause.getArgLength()){
				if (clause.getArgs()[index].isVariable()){
					int first = index;
					Node firstNode = clause.getArgs()[index];
					
					clause.getArgs()[index] = prev.getArg(index);
					
					index ++;
					while (index < clause.getArgLength()
							&& clause.getArgs()[index].isVariable()
							&& firstNode.matches(clause.getArgs()[index])){
						
						clause.getArgs()[index] = prev.getArg(index);
						
						RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
										prev,
										(Node_Variable) clause.getArgs()[first],
										clause.getArgs()[index]
								); 
						chain.add(next);
						if (chained){
							prev = (RuleWrappedBindingsFilter.AtomBindingsFiltARVH) next.getVariableHolder();
						}
						
						index ++;
					}
					int rest = index + 1;
					while (rest < clause.getArgLength()){
						if (clause.getArgs()[rest].isVariable()
								&& firstNode.matches(clause.getArgs()[index])){
							
							clause.getArgs()[rest] = prev.getArg(rest);
							
							RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
											prev,
											(Node_Variable) clause.getArgs()[first],
											clause.getArgs()[rest]
									); 
							chain.add(next);
							if (chained){
								prev = (RuleWrappedBindingsFilter.AtomBindingsFiltARVH) next.getVariableHolder();
							}
						}
						rest ++;
					}
				} else {
					Node patternNode = clause.getArgs()[index];
					clause.getArgs()[index] = prev.getArg(index);
					
					RuleWrappedBindingsFilter next = new RuleWrappedBindingsFilter(
									prev,
									(Node_Variable) clause.getArgs()[index],
									patternNode
							); 
					chain.add(next);
					if (chained){
						prev = (RuleWrappedBindingsFilter.AtomBindingsFiltARVH) next.getVariableHolder();
					}
					
					index ++;
				}
			}
			
			return chain;
		}
		
	}
	
	/**
	 * @param f
	 * @param s
	 * @return
	 */
	public static BindingsComparingFilterFunction create(Node_Variable f, Node s){
		if (s.isVariable()){
			return new BindToBindFilter(f,(Node_Variable)s);
		} else {
			return new BindToConstFilter(f,(Node_Concrete)s);
		}
	}
	
	// a BindingsComparingFilterFunction that compares the binding for the variable to the constant value 
	private static class BindToConstFilter extends BindingsComparingFilterFunction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1831204234850691945L;
		// required for kryo serialisation
		private BindToConstFilter(){}
		private BindToConstFilter(Node_Variable f, Node_Concrete s) { super(f,s); }
		@Override protected boolean matchBinds(Map<String,Node> binds){
			try{
				return binds.get(this.first.getName()).matches(this.second);
			} catch (NullPointerException e){
				return false;
			}
		}
	}
	
	// a BindingsComparingFilterFunction that compares the binding for the first variable to the binding for the second
	private static class BindToBindFilter extends BindingsComparingFilterFunction {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1089509814121683508L;
		// required for kryo serialisation
		private BindToBindFilter(){}
		private BindToBindFilter(Node_Variable f, Node_Variable s) { super(f,s); }
		@Override protected boolean matchBinds(Map<String,Node> binds){
			try{
				return binds.get(this.first.getName()).matches(binds.get(this.second.getName()));
			} catch (NullPointerException e){
				return false;
			}
		}
	}
	
	protected Node_Variable first;
	protected Node second;

	private BindingsComparingFilterFunction(Node_Variable f, Node s) {
		super();
		this.first = f;
		this.second = s;
	}
	
	@Override
	protected BindingsComparingFilterFunction clone() throws CloneNotSupportedException {
		BindingsComparingFilterFunction clone = (BindingsComparingFilterFunction) super.clone();
		clone.first = this.first;
		clone.second = this.second;
		return clone;
	}
	
	// required for kryo serialisation
	private BindingsComparingFilterFunction(){}
	
	@Override
	public List<Context> apply(Context inc) {
		List<Context> ctxs = new ArrayList<Context>();
		Map<String,Node> binds = inc.getTyped(ContextKey.BINDINGS_KEY.toString());
		if (binds == null){
			binds = new HashMap<String, Node>();
			Triple t = inc.getTyped(ContextKey.TRIPLE_KEY.toString());
			if (t == null){
				Functor f = inc.getTyped(ContextKey.ATOM_KEY.toString());
				for (int i = 0; i < f.getArgLength(); i++){
					binds.put(Integer.toString(i), f.getArgs()[i]);
				}
			} else {
				binds.put(SUBJECT, t.getSubject());
				binds.put(PREDICATE, t.getPredicate());
				binds.put(OBJECT, t.getObject());
			}
		}
		
		logger.debug(String.format("Checking at Filter(%s == %s): %s", this.first, this.second, inc));
		
		if (this.matchBinds(binds)){
			logger.debug(String.format("Match at Filter(%s == %s): %s", this.first, this.second, inc));
			// We have a match!
			Context out = new Context();
			out.put(ContextKey.BINDINGS_KEY.toString(), binds);
			ctxs.add(out);
		}
		
		return ctxs ;
	}
	
	protected abstract boolean matchBinds(Map<String,Node> binds);
	
	@Override
	public void write(Kryo kryo, Output output) {
		kryo.writeClassAndObject(output, this.first);
		kryo.writeClassAndObject(output, this.second);
	}

	@Override
	public void read(Kryo kryo, Input input) {
		this.first = (Node_Variable) kryo.readClassAndObject(input);
		this.second = (Node) kryo.readClassAndObject(input);
	}
	
	@Override
	public String toString() {
		return String.format("FILTER: %s == %s", this.first, this.second);
	}
	
	/**
	 * @author David Monks <dm11g08@ecs.soton.ac.uk>
	 *
	 */
	public static class RuleWrappedBindingsFilter extends RuleWrappedFunction<BindingsComparingFilterFunction> {
		
		protected RuleWrappedBindingsFilter(TripleBasedARVH previousarvh, Node_Variable f, Node s){
			super(new TripleBindingsFiltARVH(previousarvh, f, s));
			this.wrap(BindingsComparingFilterFunction.create(f, s));
		}
		protected RuleWrappedBindingsFilter(AtomBasedARVH previousarvh, Node_Variable f, Node s){
			super(new AtomBindingsFiltARVH(previousarvh, f, s));
			this.wrap(BindingsComparingFilterFunction.create(f, s));
		}
		
		protected static abstract class PatternBasedARVH extends ARVHComponent {
			protected Node registerVariable(Node n, String name){
				Node var = NodeFactory.createVariable(name);
				this.addVariable(name);
				if (n.isVariable() && !this.ruleToBaseVarMap().containsKey(name)){
					this.putRuleToBaseVarMapEntry(n.getName(), name);
				}
				return var ;
			}
		}
		
		protected static abstract class TripleBasedARVH extends PatternBasedARVH {
			protected abstract Node getSubject();
			protected abstract Node getPredicate();
			protected abstract Node getObject();
			
			@Override
			public String toString() {
				return String.format("FILTER: (%s,%s,%s), variables: %s",this.getSubject(),this.getPredicate(),this.getObject(),this.variables().toString());
			}
		}
		protected static abstract class AtomBasedARVH extends PatternBasedARVH {
			protected abstract Node getArg(int index);
			protected abstract int getArgCount();
			protected abstract Node getOp();
			
			@Override
			public String identifier(Map<String, String> varmap) {	
				StringBuilder name = new StringBuilder(this.getOp().toString()).append("(")
						.append(super.mapNode(varmap, this.getArg(0)));
				for (int i = 1; i < this.getArgCount(); i++){
					name.append(" ").append(super.mapNode(varmap, this.getArg(0)));
				}
				return name.append(")").toString();
			}

			@Override
			public String identifier() {
				StringBuilder name = new StringBuilder(this.getOp().toString()).append("(")
						.append(super.stringifyNode(this.getArg(0)));
				for (int i = 1; i < this.getArgCount(); i++){
					name.append(" ").append(super.stringifyNode(this.getArg(0)));
				}
				return name.append(")").toString();
			}
			
			@Override
			public String toString() {
				StringBuilder name = new StringBuilder("FILTER: ").append(this.getOp().toString())
						.append("(").append(this.getArg(0).toString());
				for (int i = 1; i < this.getArgCount(); i++){
					name.append(" ").append(this.getArg(0).toString());
				}
				return name.append("), variables: ").append(this.variables().toString()).toString();
			}
		}
		
		protected static class TripleBindingsFiltARVH extends TripleBasedARVH {
			
			protected TripleBasedARVH previous;
			protected Node subject;
			protected Node predicate;
			protected Node object;
			
			protected TripleBindingsFiltARVH(TripleBasedARVH p, Node_Variable f, Node s){
				this.previous = p;
				
				if (this.previous.getObject().matches(f)){
					this.object = s;
				} else {
					this.object = null;
				}
				if (this.previous.getPredicate().matches(f)){
					this.predicate = s;
				} else {
					this.predicate = null;
				}
				if (this.previous.getSubject().matches(f)){
					this.subject = s;
				} else {
					this.subject = null;
				}
				
				for (String v : p.variables()){
					Node var = NodeFactory.createVariable(v);
					if (this.getSubject().matches(var) || this.getPredicate().matches(var) || this.getObject().matches(var)){
						this.addVariable(v);
					}
				}
				
				for (String r : p.ruleToBaseVarMap().keySet()){
					String v = p.ruleToBaseVarMap().get(r);
					this.putRuleToBaseVarMapEntry(r, v);
				}
			}
			
			@Override
			protected Node getSubject(){
				if (this.subject == null){
					return this.previous.getSubject();
				}
				return this.subject;
			}
			
			@Override
			protected Node getPredicate(){
				if (this.predicate == null){
					return this.previous.getPredicate();
				}
				return this.predicate;
			}
			
			@Override
			protected Node getObject(){
				if (this.object == null){
					return this.previous.getObject();
				}
				return this.object;
			}
			
			@Override
			public String identifier() {
				String subject = super.stringifyNode(this.getSubject()),
					   predicate = super.stringifyNode(this.getPredicate()),
					   object;
				if (Functor.isFunctor(this.getObject())){
					StringBuilder obj = new StringBuilder();
					Functor f = (Functor) this.getObject().getLiteralValue();
					obj.append(f.getName()).append("(")
					   .append(super.stringifyNode(f.getArgs()[0]));
					for (int i = 1; i < f.getArgLength(); i++){
						obj.append(",").append(super.stringifyNode(f.getArgs()[i]));
					}
					obj.append(")");
					object = obj.toString();
				}else{
					object = super.stringifyNode(this.getObject());
				}
				
				StringBuilder name = new StringBuilder("(");
				name.append(subject).append(" ")
					.append(predicate).append(" ")
					.append(object).append(")");
				return name.toString();
			}
			
			@Override
			public String identifier(Map<String, String> varmap) {
				String subject = super.mapNode(varmap, this.getSubject()),
					   predicate = super.mapNode(varmap, this.getPredicate()),
					   object;
				if (Functor.isFunctor(this.getObject())){
					StringBuilder obj = new StringBuilder();
					Functor f = (Functor) this.getObject().getLiteralValue();
					obj.append(f.getName()).append("(")
					   .append(super.mapNode(varmap, f.getArgs()[0]));
					for (int i = 1; i < f.getArgLength(); i++){
						obj.append(",").append(super.mapNode(varmap, f.getArgs()[i]));
					}
					obj.append(")");
					object = obj.toString();
				}else{
					object = super.mapNode(varmap, this.getObject());
				}
				
				StringBuilder name = new StringBuilder("(");
				name.append(subject).append(" ")
					.append(predicate).append(" ")
					.append(object).append(")");
				return name.toString();
			}
			
		}
		
		protected static class BlankTripleBindingsFiltARVH extends TripleBasedARVH {

			protected BlankTripleBindingsFiltARVH(TriplePattern pattern) {
				registerVariable(pattern.getSubject(), SUBJECT);
				registerVariable(pattern.getPredicate(), PREDICATE);
				registerVariable(pattern.getObject(), OBJECT);
			}
			
			@Override
			protected Node getSubject() {
				return NodeFactory.createVariable(SUBJECT);
			}

			@Override
			protected Node getPredicate() {
				return NodeFactory.createVariable(PREDICATE);
			}

			@Override
			protected Node getObject() {
				return NodeFactory.createVariable(OBJECT);
			}

			@Override
			public String identifier(Map<String, String> varmap) {	
				StringBuilder name = new StringBuilder("(");
				name.append(super.mapNode(varmap, this.getSubject())).append(" ")
					.append(super.mapNode(varmap, this.getPredicate())).append(" ")
					.append(super.mapNode(varmap, this.getObject())).append(")");
				return name.toString();
			}

			@Override
			public String identifier() {
				StringBuilder name = new StringBuilder("(");
				name.append(super.stringifyNode(this.getSubject())).append(" ")
					.append(super.stringifyNode(this.getPredicate())).append(" ")
					.append(super.stringifyNode(this.getObject())).append(")");
				return name.toString();
			}
			
		}
		
		protected static class AtomBindingsFiltARVH extends AtomBasedARVH {
			
			protected AtomBasedARVH previous;
			protected Node[] args;
			
			protected AtomBindingsFiltARVH(AtomBasedARVH p, Node_Variable f, Node s){
				this.previous = p;
				this.args = new Node[p.getArgCount()];
				
				for (int i = 0; i < this.args.length; i++){
					if (this.previous.getArg(i).matches(f)){
						this.args[i] = s;
					} else {
						this.args[i] = null;
					}
				}
				
				for (String v : p.variables()){
					Node var = NodeFactory.createVariable(v);
					boolean matched = false;
					for (int i = 0; i < this.args.length && !matched; i ++){
						matched = this.getArg(i).matches(var);
					}
					if (matched){
						this.addVariable(v);
					}
				}
				
				for (String r : p.ruleToBaseVarMap().keySet()){
					String v = p.ruleToBaseVarMap().get(r);
					this.putRuleToBaseVarMapEntry(r, v);
				}
			}
			
			@Override
			protected Node getArg(int index){
				if (this.args[index] == null){
					return this.previous.getArg(index);
				}
				return this.args[index];
			}
			
			@Override
			protected Node getOp(){
				return this.previous.getOp();
			}
			
			@Override
			protected int getArgCount(){
				return this.args.length;
			}
			
		}
		
		protected static class BlankAtomBindingsFiltARVH extends AtomBasedARVH {

			private Node op;
			private int args;
			
			protected BlankAtomBindingsFiltARVH(Functor pattern) {
				this.op = NodeFactory.createLiteral(pattern.getName());
				this.args = pattern.getArgLength();
				for (int i = 0; i < this.args; i++){
					registerVariable(pattern.getArgs()[i], Integer.toString(i));
				}
			}
			
			@Override
			protected Node getArg(int index) {
				if (index < this.args){
					return NodeFactory.createVariable(Integer.toString(index));
				} else {
					throw new ArrayIndexOutOfBoundsException("Attempted to access index "+index+" of "+this.args);
				}
			}

			@Override
			protected Node getOp() {
				return this.op;
			}

			@Override
			protected int getArgCount() {
				return this.args;
			}
			
		}
		
	}

}
