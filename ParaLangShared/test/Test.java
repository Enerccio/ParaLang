import java.net.Socket;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

import cz.upol.vanusanik.paralang.connector.Node;
import cz.upol.vanusanik.paralang.connector.NodeList;
import cz.upol.vanusanik.paralang.connector.Protocol;



public class Test {
	
	@SuppressWarnings("finally")
	public static boolean test(boolean a){
		while (a)
			try {
				return getTrue();
			} finally {
				break;
			}
		return getFalse();
	}

	private static boolean getFalse() {
		System.err.println("FALSE");
		return false;
	}
	
	private static boolean getTrue() {
		System.err.println("TRUE");
		return true;
	}

	public static void main(String[] xx) throws Exception{
		test(true);
		
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
//		String so = test.toString("utf-8");
//		System.out.println(so);
		
		NodeList.addNode("localhost", 12345);
		Node n = NodeList.getBestLoadNodes(5).get(0);
		
		Socket s = new Socket(n.getAddress(), n.getPort());
		
		JsonObject o = new JsonObject();
		o.add("header", Protocol.RESERVE_SPOT_REQUEST);
		Protocol.send(s.getOutputStream(), o);
		
		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
		
		
		s.close();
	}
	
}
