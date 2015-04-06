import java.net.Socket;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.connector.Protocol;


public class Test {

	public static void main(String[] xx) throws Exception{
		
		Socket s = new Socket("localhost", 12345);
		
		byte[] arr = { 0x1e, 0x00, 0x00, 0x00, 0x02, '{' };
		IOUtils.write(arr, s.getOutputStream());
		
		JsonObject o = new JsonObject();
		o.add("header", Protocol.GET_STATUS_REQUEST);
		Protocol.send(s.getOutputStream(), o);
		
		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
		s.close();
		
//		File f = new File("bin\\x.plang");
//		PLCompiler c = new PLCompiler();
//		
//		PLRuntime r = new PLRuntime();
//		r.setRestricted(false);
//		r.setSafeContext(false);
//		c.compile(new DiskFileDesignator(f));
//		
//		try {
//			r.run("HelloWorldModule", "run");
//		} catch (Exception e){
//			e.printStackTrace();
//		}
//		
//		ByteArrayOutputStream test = new ByteArrayOutputStream();
//		
//		r.serializeRuntimeContent(test, 0);
		
//		String s = test.toString("utf-8");
//		System.out.println(s);
	}
	
}
