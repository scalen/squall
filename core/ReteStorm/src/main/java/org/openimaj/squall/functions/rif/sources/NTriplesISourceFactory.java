package org.openimaj.squall.functions.rif.sources;

import java.io.InputStream;
import java.net.URI;

import org.openimaj.squall.data.ISource;
import org.openimaj.squall.utils.JenaUtils;
import org.openimaj.util.data.Context;
import org.openimaj.util.data.ContextWrapper;
import org.openimaj.util.stream.CollectionStream;
import org.openimaj.util.stream.Stream;

import com.hp.hpl.jena.graph.Triple;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class NTriplesISourceFactory extends ISourceFactory {

	@Override
	public ISource<Stream<Context>> createSource(URI location) {
		return new ISource<Stream<Context>>() {
			
			private String nTripleStreamLocation;
			private InputStream nTripleStream;

			@Override
			public Stream<Context> apply(Stream<Context> in) {
				return apply();
			}
			
			@Override
			public Stream<Context> apply() {
				return new CollectionStream<Triple>(JenaUtils.readNTriples(nTripleStream))
						.map(new ContextWrapper("triple"));
//				return null;
			}
			
			@Override
			public void setup() { 
				this.nTripleStream = ISource.class.getResourceAsStream(this.nTripleStreamLocation);
			}
			
			@Override
			public void cleanup() { }
			
			public ISource<Stream<Context>> setInputStreamSource(URI loc){
				this.nTripleStreamLocation = loc.toString();
				return this;
			}
		}
		// Set the URI of the source before returning
		.setInputStreamSource(location);
	}

}
