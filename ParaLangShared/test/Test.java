import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.Socket;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.compiler.DiskFileDesignator;
import cz.upol.vanusanik.paralang.compiler.PLCompiler;
import cz.upol.vanusanik.paralang.connector.Protocol;
import cz.upol.vanusanik.paralang.runtime.PLRuntime;


public class Test {

	public static void main(String[] xx) throws Exception{
		
		File f = new File("bin\\x.plang");
		PLCompiler c = new PLCompiler();
		
		PLRuntime r = new PLRuntime();
		r.setRestricted(false);
		r.setSafeContext(false);
		c.compile(new DiskFileDesignator(f));
		
		try {
			r.run("HelloWorldModule", "run");
		} catch (Exception e){
			e.printStackTrace();
		}
		
		ByteArrayOutputStream test = new ByteArrayOutputStream();
		
		r.serializeRuntimeContent(test, 0);
		String so = test.toString("utf-8");
		System.out.println(so);
		
		Socket s = new Socket("localhost", 12345);
		
		JsonObject o = new JsonObject();
		o.add("header", Protocol.GET_STATUS_REQUEST);
		Protocol.send(s.getOutputStream(), o);
		
		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
		
		o = new JsonObject();
		o.add("header", Protocol.RESERVE_SPOT_REQUEST);
		Protocol.send(s.getOutputStream(), o);
		
		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
		
		o = new JsonObject();
		o.add("header", Protocol.GET_STATUS_REQUEST);
		Protocol.send(s.getOutputStream(), o);
		
		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
		
		
		s.close();
	}
	
}
