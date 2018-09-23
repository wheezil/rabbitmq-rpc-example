
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.RpcClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;

public class RpcClientTest {

	private final Connection connection;
	private final Channel channel;
	private final String requestQueueName = "rpc_queue";
	private final RpcClient client;
	private static final int TIMEOUT_MS = 1000;
	private static final int ITER = 10;
	private static final int THREADS = 8;
	private static int counter = 1;
	// Set this to true to enable retry of requests
	private boolean doRetry = false;
	
	private final static Logger LOG = Logger.getLogger(RpcClientTest.class);
	public RpcClientTest() throws IOException, TimeoutException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		connection = factory.newConnection();
		channel = connection.createChannel();
		client = new RpcClient(channel, "", requestQueueName, TIMEOUT_MS);
	}

	public String call(String message) throws Exception {
		byte[] response = client.primitiveCall(message.getBytes("UTF-8"));
		return new String(response, "UTF-8");
	}

	public void close() throws IOException {
		client.close();
		connection.close();
	}
	
	private class ClientRunner implements Runnable {
		private int threadNumber;
		public ClientRunner() {
		}
		@Override
		public void run() {
			synchronized (this) {
				threadNumber = counter++;
			}
			LOG.info("ClientRunner: Starting");
			for (int i = 1; i <= ITER; i++) {
				String request = "REQUEST_" + threadNumber + "_" + i;
				String expected = request.replace("REQUEST", "RESPONSE");
				LOG.debug("ClientRunner: Procesing '" + request + "'");
				while (true) {
					try {
						String response = call(request);
						if (!response.equals(expected)) {
							LOG.error("ClientRunner: Expected '" + request + "', received '" + response + "'");
							return;
						}
						break;
					} catch (TimeoutException ex) {
						if (doRetry) {
							LOG.info("ClientRunner: Timeout procesing '" + request + "', retrying");
							continue;
						} else {
							LOG.info("ClientRunner: Timeout procesing '" + request + "', ignoring");
							break;
						}
					} catch (Exception ex) {
						LOG.error("ClientRunner: Error", ex);
						return;
					}
				}
			}
			LOG.info("ClientRunner: thread finished");
		}
	}
	
	public void test() {
		LOG.info("Running " + THREADS + " threads with " + ITER + " requests");
		try {
			ClientRunner handler = new ClientRunner();
			List<Thread> threads = new ArrayList<>();
			for (int i = 0; i < THREADS; i++) {
				Thread t = new Thread(handler);
				threads.add(t);
				t.start();
			}
			for (Thread t : threads) {
				t.join();
			}
			LOG.info("Client finished");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] argv) throws IOException, TimeoutException {
		RpcClientTest clientTest = new RpcClientTest();
		clientTest.test();
		clientTest.close();
	}
}
