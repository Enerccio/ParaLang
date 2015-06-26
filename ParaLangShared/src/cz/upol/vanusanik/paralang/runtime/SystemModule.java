package cz.upol.vanusanik.paralang.runtime;

import java.io.Serializable;
import java.util.Set;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.PlangObjectType;
import cz.upol.vanusanik.paralang.plang.types.Array;
import cz.upol.vanusanik.paralang.plang.types.BooleanValue;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.Int;
import cz.upol.vanusanik.paralang.plang.types.NoValue;
import cz.upol.vanusanik.paralang.plang.types.Str;

/**
 * System module
 * 
 * @author Enerccio
 *
 */
public class SystemModule extends PLModule implements Serializable {
	private static final long serialVersionUID = -499503904346523233L;

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;

		___setkey("init", new FunctionWrapper("__init", this, false));
		___setkey("current_time", new FunctionWrapper("currentTime", this,
				false));
		___setkey("free_nodes", new FunctionWrapper("freeNodes", this, false));
		___setkey("is_restricted", new FunctionWrapper("isRestricted", this, false));
		___setkey("apply", new FunctionWrapper("apply", this, false));

		this.___restrictedOverride = false;
	}

	public PLangObject __init() {
		return NoValue.NOVALUE;
	}

	public PLangObject currentTime() {
		return new Int(System.currentTimeMillis());
	}

	public PLangObject freeNodes() {
		return new Int(NodeList.expectNumberOfNodes());
	}
	
	public PLangObject isRestricted(){
		return BooleanValue.fromBoolean(PLRuntime.getRuntime().isRestricted());
	}

	@Override
	public JsonValue ___toObject(Set<Long> alreadySerialized, boolean serializeFully) {
		JsonObject metaData = new JsonObject().add("metaObjectType",
				___getType().toString());
		if (alreadySerialized.contains(___getObjectId())) {
			metaData.add("link", true).add("linkId", ___getObjectId());
		} else {
			alreadySerialized.add(___getObjectId());
			metaData.add("isBaseClass", false).add("link", false)
					.add("isInited", ___isInited)
					.add("thisLink", ___getObjectId())
					.add("className", "System").add("fields", ___getFields(alreadySerialized, serializeFully));
		}
		return metaData;
	}
	
	public PLangObject apply(PLangObject runnable, PLangObject array){
		if (array.___getType() != PlangObjectType.ARRAY){
			throw PLRuntime.getRuntime().newInstance("System.BaseException", new Str("Argument must by type of array"));
		}
		return PLRuntime.getRuntime().run(runnable, runnable, ((Array)array).getArray().clone());
	}
}
