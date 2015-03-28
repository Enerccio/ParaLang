package cz.upol.vanusanik.paralang.plang;

import com.eclipsesource.json.JsonValue;

public abstract class PLangObject {

	public abstract PlangObjectType getType();
	
	public abstract JsonValue toObject(long previousTime);
	
}
