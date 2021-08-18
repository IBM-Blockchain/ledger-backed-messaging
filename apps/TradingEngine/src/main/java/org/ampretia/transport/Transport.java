// SPDX-License-Identifier: Apache-2.0

package org.ampretia.transport;

import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.ibm.mq.constants.CMQC;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import org.ampretia.model.TradeMessage;
import org.eclipse.microprofile.config.inject.ConfigProperty;



/**
 * A minimal and simple application for Point-to-point messaging.
 *
 * Application makes use of fixed literals, any customisations will require
 * re-compilation of this source file. Application assumes that the named queue
 * is empty prior to a run.
 *
 * Notes:
 *
 * API type: JMS API (v2.0, simplified domain)
 *
 * Messaging domain: Point-to-point
 *
 * Provider type: IBM MQ
 *
 * Connection mode: Client connection
 *
 * JNDI in use: No
 *
 */
@ApplicationScoped
public class Transport {

	private static final Logger LOGGER = Logger.getLogger(Transport.class.getName());

	// Create variables for the connection to MQ
	@ConfigProperty(name = "mq.host")
	String HOST;

	@ConfigProperty(name = "MQ_PORT")
	int PORT;

	@ConfigProperty(name = "MQ_CHANNEL")
	String CHANNEL;

	@ConfigProperty(name = "MQ_QM")
	String QMGR;

	@ConfigProperty(name = "MQ_APP_USER")
	String APP_USER;

	@ConfigProperty(name = "MQ_APP_PASSWORD")
	String APP_PASSWORD;

	@ConfigProperty(name = "MQ_TRADE_OFFER_QUEUE")
	String TRADE_OFFER_Q;

	@ConfigProperty(name = "MQ_TRADE_RESPONSE_QUEUE")
	String TRADE_RESPONSE_Q;

	JMSContext context;
	JMSContext responseContext;
	JMSContext marketContext;

	/**
	 * Main method
	 *
	 * @param args
	 */
	public Transport() {
	}

	Consumer appConsumer;
	JMSConsumer marketConsumer;

	@PostConstruct
	public void init() {
		// Variables
		Destination destination = null;
		final JMSProducer producer = null;
		JMSConsumer consumer = null;

		try {
			// Create a connection factory
			final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
			final JmsConnectionFactory cf = ff.createConnectionFactory();
			System.out.println(HOST);
			// Set the properties
			cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
			cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
			cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
			cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
			cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
			cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPutGet (JMS)");
			cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
			cf.setStringProperty(WMQConstants.USERID, APP_USER);
			cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);

			// Create JMS objects
			context = cf.createContext(JMSContext.SESSION_TRANSACTED);

			context.setExceptionListener(new ExceptionListener() {

				@Override
				public void onException(final JMSException exception) {
					recordFailure(exception);

				}

			});
			// destination = context.createQueue("queue:///LEDGER.ACTIONS");

			context.start();

			responseContext = cf.createContext(JMSContext.SESSION_TRANSACTED);
			responseContext.start();

			marketContext = cf.createContext(JMSContext.SESSION_TRANSACTED);
			marketConsumer = marketContext.createConsumer(context.createQueue(TRADE_OFFER_Q));

			marketContext.start();
			LOGGER.info("Starting messaging");

		} catch (final JMSException jmsex) {
			recordFailure(jmsex);
		}

	} // end main()

	@PreDestroy
	public void shutdown() {

		marketContext.close();
		responseContext.close();
		context.close();
	}

	public String send(TradeMessage t) {
		Jsonb jsonb = JsonbBuilder.create();
		String s = jsonb.toJson(t);
		Queue q = this.context.createQueue(TRADE_OFFER_Q);
		LOGGER.info("++++++ sending offer as " + s);
		TextMessage msg = this.context.createTextMessage(s);
		try {
			msg.setIntProperty("JMS_IBM_Report_COA", CMQC.MQRO_COA_WITH_DATA);
			msg.setIntProperty("JMS_IBM_Report_COD", CMQC.MQRO_COD_WITH_DATA);

			msg.setJMSReplyTo(context.createQueue("queue:///LEDGER.ACTION"));
			msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_MSG_ID, CMQC.MQRO_PASS_MSG_ID);
			msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_CORREL_ID, CMQC.MQRO_PASS_CORREL_ID);
			msg.setStringProperty("TRADE_ID", t.tradeId);
			context.createProducer().send(q, msg);
			this.context.commit();

			String msgId = msg.getJMSMessageID();
			t.setMsgId(msgId);
			return msgId;
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
	}

	public String sendResponse(TradeMessage t) {
		Jsonb jsonb = JsonbBuilder.create();
		String s = jsonb.toJson(t);
		Queue q = this.context.createQueue(TRADE_RESPONSE_Q);

		TextMessage msg = this.context.createTextMessage(s);
		try {
			msg.setIntProperty("JMS_IBM_Report_COA", CMQC.MQRO_COA_WITH_DATA);
			msg.setIntProperty("JMS_IBM_Report_COD", CMQC.MQRO_COD_WITH_DATA);

			msg.setJMSReplyTo(context.createQueue("queue:///LEDGER.ACTION"));
			msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_MSG_ID, CMQC.MQRO_PASS_MSG_ID);
			msg.setIntProperty(WMQConstants.JMS_IBM_REPORT_PASS_CORREL_ID, CMQC.MQRO_PASS_CORREL_ID);
			msg.setStringProperty("TRADE_ID", t.tradeId);
			msg.setJMSCorrelationID(t.tradeId);
			context.createProducer().send(q, msg);
			this.context.commit();

			String msgId = msg.getJMSMessageID();
			t.setMsgId(msgId);
			return msgId;
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
	}

	public void setOfferQueueConsumer(Consumer<TradeMessage> market) {
		// autoclosable
		marketConsumer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(final Message message) {
				String json;
				try {
					LOGGER.info(message.toString());
					json = message.getBody(String.class);
					Jsonb jsonb = JsonbBuilder.create();
					TradeMessage tm = jsonb.fromJson(json, TradeMessage.class);
					tm.setMsgId(message.getJMSMessageID());
					market.accept(tm);
					context.commit();
				} catch (JMSException e) {
					throw new RuntimeException(e);
				}

			}

		});
	}

	public TradeMessage getResponse(String tradeId) {
		TradeMessage responseMsg = null;
		try {
			String messageSelector = "JMSCorrelationID='" + tradeId + "'";
			JMSConsumer consumer = responseContext.createConsumer(responseContext.createQueue(TRADE_RESPONSE_Q),
					messageSelector);

			Message msg = consumer.receiveNoWait();
			if (msg != null) {
				String json = msg.getBody(String.class);
				Jsonb jsonb = JsonbBuilder.create();
				responseMsg = jsonb.fromJson(json, TradeMessage.class);
			}
			LOGGER.info("Response for trade " + tradeId + " is " + msg);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			responseContext.commit();
		}
		return responseMsg;
	}

	/**
	 * Record this run as failure.
	 *
	 * @param ex
	 */
	private static void recordFailure(final Exception ex) {
		if (ex != null) {
			if (ex instanceof JMSException) {
				processJMSException((JMSException) ex);
			} else {
				System.out.println(ex);
			}
		}
		System.out.println("FAILURE");

		return;
	}

	/**
	 * Process a JMSException and any associated inner exceptions.
	 *
	 * @param jmsex
	 */
	private static void processJMSException(final JMSException jmsex) {
		System.out.println(jmsex);
		Throwable innerException = jmsex.getLinkedException();
		if (innerException != null) {
			System.out.println("Inner exception(s):");
		}
		while (innerException != null) {
			System.out.println(innerException);
			innerException = innerException.getCause();
		}
		return;
	}

}
