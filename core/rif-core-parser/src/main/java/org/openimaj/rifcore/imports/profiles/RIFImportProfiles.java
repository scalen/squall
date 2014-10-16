package org.openimaj.rifcore.imports.profiles;

import java.net.URI;
import java.util.HashMap;

/**
 * 
 * @author David Monks <dm11g08@ecs.soton.ac.uk>
 * @param <T> 
 *
 */
public abstract class RIFImportProfiles <T> extends HashMap<URI, T> {

	/**
	 * @author David Monks &lt;dm11g08@ecs.soton.ac.uk&gt;
	 *
	 */
	public static class ProfileNotSupportedException extends Exception {
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 3874822217595718296L;

		/**
		 * @param prof
		 */
		public ProfileNotSupportedException(URI prof){
			super("The profile "+prof.toString()+" is not supported.");
		}
		
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8863623732831101311L;
	
}
