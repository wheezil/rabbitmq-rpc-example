
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import org.apache.log4j.Logger;

public class RpcServerTest {

	private static final String RPC_QUEUE_NAME = "rpc_queue";
	private final static Logger LOG = Logger.getLogger(RpcServerTest.class);
	
	private static class RPCConsumer extends DefaultConsumer {
		public RPCConsumer(Channel channel) {
			super(channel);
		}
		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
			try {
				AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
						.correlationId(properties.getCorrelationId())
						.build();
				String request = new String(body, "UTF-8");
				LOG.info("RPCConsumer: Processing: " + request);
				String response = request.replace("REQUEST", "RESPONSE");
				getChannel().basicPublish("", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));
			} catch (Exception ex) {
				LOG.error("RPCConsumer: Error", ex);
			} finally {
				getChannel().basicAck(envelope.getDeliveryTag(), false);
			}
		}
	}

	public static void main(String[] argv) {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");
		Connection connection = null;
		Consumer consumer = null;
		Channel channel = null;
		String consumerTag = null;
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
			channel.basicQos(1);
			LOG.info("Starting");
			consumer = new RPCConsumer(channel);
			consumerTag = channel.basicConsume(RPC_QUEUE_NAME, false, consumer);
			LOG.info("Consumer pool launched");
			System.out.println("Hit enter to exit server");
			System.in.read();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (consumerTag != null) {
				try {
					channel.basicCancel(consumerTag);
				} catch (IOException ex) {
					LOG.error("Error cancelling consumer", ex);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (IOException _ignore) {
				}
			}
		}
	}
}
