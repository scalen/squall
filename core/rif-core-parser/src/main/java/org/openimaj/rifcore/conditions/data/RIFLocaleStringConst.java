package org.openimaj.rifcore.conditions.data;

public class RIFLocaleStringConst extends RIFStringConst {

	private final String locale;
	
	public RIFLocaleStringConst(String locale){
		this.locale = locale;
	}
	
	public String getLocale(){
		return locale;
	}
	
}
