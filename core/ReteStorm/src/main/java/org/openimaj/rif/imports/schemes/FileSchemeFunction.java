package org.openimaj.rif.imports.schemes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.openimaj.util.function.Function;

/**
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 *
 */
public class FileSchemeFunction implements Function<URI, InputStream> {

	@Override
	public InputStream apply(URI in) {
		File f = new File(in.getPath());
		try {
			return new FileInputStream(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
