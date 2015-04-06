package cz.upol.vanusanik.paralang.connector;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import com.eclipsesource.json.JsonObject;

public class Protocol {

	public static final String GET_STATUS_REQUEST  = "StatusRequest";
	public static final String GET_STATUS_RESPONSE = "StatusResponse";
	
	public static JsonObject load(InputStream inputStream) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		
		boolean escape = false;
		boolean inString = false;
		
		BufferedInputStream is = new BufferedInputStream(inputStream);
		int data;
		
		int bc = 0;
		
		while ((data = is.read()) != -1){
			char c = (char) data;
			bos.write(data);
			
			if (c == '\\' && !escape){
				escape = true;
			} if (c == '"' && !escape){
				inString = !inString;
			} if (c == '{' && !inString){
				++bc;
			} if (c == '}' && !inString){
				--bc;
			} if (c == '[' && !inString){
				++bc;
			} if (c == ']' && !inString){
				--bc;
			} else {
				escape = false;
			}
			
			if (bc == 0) break;
		}
		
		return JsonObject.readFrom(bos.toString("utf-8"));
	}

}
