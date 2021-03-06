package cz.upol.vanusanik.paralang.runtime;

import cz.upol.vanusanik.paralang.plang.PLangObject;
import cz.upol.vanusanik.paralang.plang.types.FunctionWrapper;
import cz.upol.vanusanik.paralang.plang.types.NoValue;

/**
 * System.NetworkException
 * 
 * @author Enerccio
 *
 */
public class NetworkException extends PLClass {
	private static final long serialVersionUID = -1870114368385241652L;

	public static final String listKey 				= "___prevExps";
	public static final String getExceptions 		= "get_exceptions";
	public static final String partialValues 		= "___partialValues";
	public static final String getPartialReslts		= "get_partial_results";

	public NetworkException() {

	}

	@Override
	protected void ___init_internal_datafields(BaseCompiledStub self) {
		this.___restrictedOverride = true;

		BaseException be = new BaseException();
		be.___init_class();
		___setkey(PLClass.___superKey, be);
		___setkey("init", new FunctionWrapper("__init__base", this, true));
		___setkey(BaseException.__messageField, NoValue.NOVALUE);
		___setkey(listKey, NoValue.NOVALUE);
		___setkey(getExceptions, new FunctionWrapper("__get_exceptions", this,
				true));
		___setkey(partialValues, NoValue.NOVALUE);
		___setkey(getPartialReslts, new FunctionWrapper("__get_partial_result", this,
				true));
		
		this.___restrictedOverride = false;
	}

	public PLangObject __get_exceptions(PLangObject self) {
		return ((PLClass) self).___getkey(listKey, false);
	}
	
	public PLangObject __get_partial_result(PLangObject self) {
		return ((PLClass) self).___getkey(partialValues, false);
	}

	public PLangObject __init__base(PLangObject self, PLangObject message) {
		// run super init
		___restrictedOverride = true;
		BaseException bc = (BaseException) ___getkey(PLClass.___superKey, true);
		PLRuntime.getRuntime().run(bc.___getkey("init", true), bc, message);

		___setkey(BaseException.__messageField, message);
		___restrictedOverride = false;
		return NoValue.NOVALUE;
	}

}
