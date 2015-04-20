
public class Test {
	
//	void foo(){
//		System.out.println("empty");
//	}
	
	void foo(int[] a){
		System.out.println("int[]");
	}
	
	void foo(byte... bs){
		System.out.println("byte...");
	}

	public static void main(String[] xx) throws Exception{
		new Test().foo();
		
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
		
//		NodeList.addNode("localhost", 12345);
//		Node n = NodeList.getBestLoadNodes(5).get(0);
//		
//		Socket s = new Socket(n.getAddress(), n.getPort());
//		
//		JsonObject o = new JsonObject();
//		o.add("header", Protocol.RESERVE_SPOT_REQUEST);
//		Protocol.send(s.getOutputStream(), o);
//		
//		System.out.println(Protocol.receive(s.getInputStream()).toString(WriterConfig.PRETTY_PRINT));
//		
//		
//		s.close();
	}
	
}
