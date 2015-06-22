package cz.upol.vanusanik.paralang.connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;

/**
 * Static class only. Protocol handles communication between client and node.
 * 
 * @author Enerccio
 *
 */
public class Protocol {

	// Headers
	public static final String GET_STATUS_REQUEST = "StatusRequest";
	public static final String GET_STATUS_RESPONSE = "StatusResponse";
	public static final String RESERVE_SPOT_REQUEST = "ReserveSpotRequest";
	public static final String RESERVE_SPOT_RESPONSE = "ReserveSpotResponse";
	public static final String RUN_CODE = "RunCode";
	public static final String RETURNED_EXECUTION = "ReturnedExecution";
	public static final String REQUEST_DATA = "RequestData";
	public static final String SEND_DATA = "SendData";

	// Error codes
	public static final long ERROR_GENERAL_ERROR = 0x0;
	public static final long ERROR_NO_RESERVED_NODE = 0x1;
	public static final long ERROR_COMPILATION_FAILURE = 0x2;
	public static final long ERROR_DESERIALIZATION_FAILURE = 0x3;
	public static final long ERROR_UNKNOWN_OBJECT = 0x4;

	/** Clear bit used to mark next payload */
	private static byte clearBit = 0x1e;
	/** Threads used to provide future access */
	private static ExecutorService executor = Executors
			.newCachedThreadPool(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread t = Executors.defaultThreadFactory().newThread(r);
					t.setDaemon(true);
					return t;
				}
			});

	/**
	 * Receives payload from socket stream
	 * 
	 * @param inputStream
	 *            input socket stream
	 * @return JsonObject payload
	 * @throws Exception
	 */
	public static JsonObject receive(final InputStream inputStream)
			throws Exception {
		int cb = -1;
		while ((cb = inputStream.read()) != -1) {
			if (cb == clearBit)
				break;
		}

		if (cb == -1)
			return null;

		Future<JsonObject> future = executor.submit(new Callable<JsonObject>() {
			@Override
			public JsonObject call() {
				try {
					byte[] arr = new byte[4];
					IOUtils.read(inputStream, arr);
					int length = ByteBuffer.wrap(arr).asIntBuffer().get();

					byte[] data = new byte[length];
					IOUtils.read(inputStream, data);
					return JsonObject.readFrom(new String(data, "utf-8"));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		});

		return future.get(15, TimeUnit.SECONDS);
	}

	/**
	 * Sends the payload to the socket stream
	 * 
	 * @param os
	 *            Socket stream
	 * @param payload
	 *            JsonObject payload
	 * @throws Exception
	 */
	public static void send(OutputStream os, JsonObject payload)
			throws Exception {
		
		os.write(clearBit);

		String data = payload.toString(WriterConfig.PRETTY_PRINT);
		byte[] array = ByteBuffer.allocate(4).putInt(data.length()).array();

		IOUtils.write(array, os);
		IOUtils.write(data, os, "utf-8");
	}

}
