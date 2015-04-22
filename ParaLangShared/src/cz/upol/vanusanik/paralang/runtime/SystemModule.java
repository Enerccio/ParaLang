package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

public class SystemModule extends PLModule implements Serializable {
	private static final long serialVersionUID = -499503904346523233L;

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;
		
		___setkey("init", new FunctionWrapper("__init", this, false));
		___setkey("current_time", new FunctionWrapper("currentTime", this, false));
		___setkey("free_nodes", new FunctionWrapper("freeNodes", this, false));
		
		this.___restrictedOverride = false;
	}

	public PLangObject __init(){
		return NoValue.NOVALUE;
	}
	
	public PLangObject currentTime(){
		return new Int(System.currentTimeMillis());
	}
	
	public PLangObject freeNodes(){
		return new Int(NodeList.expectNumberOfNodes());
	}

	@Override
	public JsonValue ___toObject() {
		PLRuntime runtime = PLRuntime.getRuntime();
		JsonObject metaData = new JsonObject().add("metaObjectType", ___getType().toString());
		if (runtime.isAlreadySerialized(this)){
			metaData.add("link", true)
					.add("linkId", ___getObjectId());
		} else {
			runtime.setAsAlreadySerialized(this);
			metaData.add("isBaseClass", false)
					.add("link", false)
					.add("isInited", ___isInited)
					.add("thisLink", ___getObjectId())
					.add("className", "System")
					.add("fields", ___getFields());
		}
		return metaData;
	}
}
