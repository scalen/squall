package org.openimaj.squall.build.storm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimaj.squall.orchestrate.NamedNode;
import org.openimaj.squall.orchestrate.NamedStream;
import org.openimaj.storm.utils.StormUtils;
import org.openimaj.util.data.Context;
import org.openimaj.util.data.ContextKey;
import org.openimaj.util.function.Function;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IComponent;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

/**
 * @author Sina Samangooei (ss@ecs.soton.ac.uk)
 *
 */
public abstract class NamedNodeComponent implements IComponent{
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 843143889101579968L;
	private Set<String> streams;
	private List<String> outputStreams;
	private Map<String, String> correctedStreamName;
	private Map<String, Object> conf;
	private Fields fields = new Fields();
	/**
	 * @param nn
	 */
	public NamedNodeComponent(NamedNode<?> nn) {
		ContextKey[] keys = ContextKey.values();
		String[] strings = new String[keys.length - 1];
		int i = 0;
		for (ContextKey key : keys){
			if (!key.equals(ContextKey.STREAM_KEY)){
				strings[i++] = key.toString();
			}
		}
		setFields(strings);
		
		this.outputStreams = new ArrayList<String>();
		for (NamedStream edge : nn.childEdges()) {
				this.outputStreams.add(StormUtils.legalizeStormIdentifier(edge.identifier()));
		}
		this.correctedStreamName = new HashMap<String,String>();
		for (NamedStream edge : nn.parentEdges()) {
			String stormName = StormUtils.legalizeStormIdentifier(edge.identifier());
			this.correctedStreamName.put(stormName, edge.identifier());
		}
	}
	
	protected void setFields(String ... fields){
		this.fields = new Fields(fields);
	}
	
	/**
	 * @return
	 */
	public Fields getFields() {
		return this.fields;
	}
	
	/**
	 * Add a field of the context to be serialised and sent to the next processing bolt.
	 * @param field
	 * @return
	 */
	public boolean addField(String field){
		if (this.fields.contains(field)){
			return false;
		}
		String[] fs = new String[this.fields.size() + 1];
		for (int i = 0; i < this.fields.size(); i++){
			fs[i] = this.fields.get(i);
		}
		fs[this.fields.size()] = field;
		this.setFields(fs);
		return true;
	}
	
	/**
	 * @param conf
	 * @param context
	 */
	public void setup(Map<String, Object> conf, TopologyContext context) {
		this.streams = context.getThisStreams();
		this.conf = conf;
	}

	/**
	 * 
	 */
	public void cleanup() {
	}
	
	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		for (String strm : this.outputStreams) {			
			declarer.declareStream(strm,fields);
		}
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		return conf;
	}
	
	/**
	 * Register a stream and a function which must be applied before emitting to it
	 * @param t 
	 * @return the context from the tuple enriched with extra required information
	 */
	public Context getContext(Tuple t) {
		Context ctx = new Context();
		for (String field : fields){
			ctx.put(field, t.getValueByField(field));
		}
		ctx.put(ContextKey.STREAM_KEY.toString(), this.correctedStreamName.get(t.getSourceStreamId()));
		return ctx;
	}
	
	/**
	 * Fire this context to all listening streams
	 * @param ctx 
	 */
	public void fire(Context ctx) {
		fire(null,ctx);
	}
	
	/**
	 * Fire this context to the specified stream
	 * @param stream 
	 * @param t 
	 * @param ctx 
	 */
	public void fire(String stream, Tuple t, Context ctx) {
		Object[] vals = new Object[fields.size()];
		for (int i = 0; i < fields.size(); i++){
			vals[i] = ctx.get(fields.get(i));
		}
		Values em = new Values(vals);
		fire(stream, t, em);
	}
	
	/**
	 * Fire this context to all listening streams
	 * @param t 
	 * @param ctx 
	 */
	public void fire(Tuple t, Context ctx) {
		for ( String  strm : this.streams) {
			fire(strm, t, ctx);
		}
	}
	
	/**
	 * @param anchor 
	 * @param ret
	 */
	public void fire(Tuple anchor, List<Context> ret) {
		if(ret != null){			
			for (Context context : ret) {
				fire(anchor,context);
			}
		}
		
	}

	/**
	 * @param strm the streamId on which to emit
	 * @param anchor the anchor which cause this emit, might be null
	 * @param ctx this value to emit
	 */
	public abstract void fire(String strm, Tuple anchor, Values ctx) ;

	/**
	 * Given a parent, a stream and a namedNode construct the stream name in the right way
	 * @param parent
	 * @param strm
	 * @param namedNode
	 * @return stream named
	 */
	public static String constructStreamName(NamedNode<?> parent,NamedStream strm, NamedNode<?> namedNode) {
		
		return StormUtils.legalizeStormIdentifier(parent.getName() + "_" + strm.identifier() + "_" + namedNode.getName());
	}

}
