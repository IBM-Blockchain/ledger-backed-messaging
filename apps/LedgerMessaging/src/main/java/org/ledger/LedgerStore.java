// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MessageListener;

import com.ibm.mq.constants.CMQC;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Quarkus;

@Singleton
public class LedgerStore implements ExceptionListener, MessageListener {

    private static final Logger LOGGER = Logger.getLogger(LedgerStore.class.getName());

    // Create variables for the connection to MQ
    @ConfigProperty(name="mq.host")
    private String HOST; 

    @ConfigProperty(name="mq.port")
    private int PORT;

    @ConfigProperty(name="mq.channel")
    private String CHANNEL;

    @ConfigProperty(name="mq.qm")
    private String QMGR;

    @ConfigProperty(name="mq.app.user")
    private String APP_USER;

    @ConfigProperty(name="mq.app.password")
    private String APP_PASSWORD;

    @ConfigProperty(name = "fabric.user")
    public String fabricuser;
    
    private String LEDGER_ACTIONS="queue:///LEDGER.ACTION";

    Destination destination = null;
    final JMSProducer producer = null;
    JMSConsumer consumer = null; // to and from
    JMSContext context;

    // Inject the service factory to connect you to Fabric Contracts
    @Inject
    public FabricServiceFactory factory;


    LedgerStore() {
    }

    @PreDestroy
    public void disconnect(){
        LOGGER.info("Closing the messaging context");
        context.close();
        LOGGER.info("Closed the messaging context");
    }

    /**
     * Connect to the IBM MQ queuemanagers,and register a message listener
     */
    public void connect() {
        LOGGER.info("LedgerStore started");

        // Create a connection factory
        try {
            final JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            final JmsConnectionFactory cf = ff.createConnectionFactory();

            // Set the properties
            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
            cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "LedgerStore");
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            cf.setStringProperty(WMQConstants.USERID, APP_USER);
            cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);

            LOGGER.info("Connecting to qm...");

            context = cf.createContext(JMSContext.SESSION_TRANSACTED);

            context.setExceptionListener(this);
            destination = context.createQueue(LEDGER_ACTIONS);

            consumer = context.createConsumer(destination); // autoclosable
            consumer.setMessageListener(this);
            context.start();
            LOGGER.info("Started consumer... all messages will come here");
        } catch (final JMSException jmsex) {
            processJMSException(jmsex);
            context.stop();
            Quarkus.asyncExit();
            throw new RuntimeException("Failued to handle error");
        }
    }

    /**
     * onMessage gets called when a message arrives on the queue
     * 
     * These are IBMMQ report messages, specifically the COD confirm-on-delivery
     * and COA and confirm-on-arrival
     */
    @Override
    public void onMessage(final javax.jms.Message message) {
        try {
            LOGGER.info("onMesage got message from MQ");
            //MQFB_COD
            //MQFB_COA
            String event;
            int feedback = message.getIntProperty(WMQConstants.JMS_IBM_FEEDBACK);
            if (feedback==CMQC.MQFB_COA){
                event="ARRVIED";
            } else if (feedback==CMQC.MQFB_COD){
                event="DELIVERED";
            } else {
                event="???";
            }

            String tradeId = message.getStringProperty("TRADE_ID");
            String msgId = message.getJMSMessageID();
            String timestamp = String.valueOf(message.getJMSTimestamp());
            String payload = message.getBody(String.class);

            LOGGER.info("For Trade ["+tradeId+"] message ["+msgId+"] has ["+event+"] at time ["+timestamp+"] payload:["+payload+"]" );

            LedgerableEvent le = new LedgerableEvent();
            le.eventId = tradeId;
            le.subId = msgId;

            LedgerableEvent.Log l = new LedgerableEvent.Log();            
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            l.dataHash =  Base64.getEncoder().encodeToString(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
            l.timestamp =timestamp;
            l.type = event;
            le.logs = new LedgerableEvent.Log[]{ l };

            // User id here is for the application as a whole
            LedgerableEventService f = factory.getLedgerableService(fabricuser);
            f.submitEvent(le);

            context.commit();
        } catch (Throwable e) {
            e.printStackTrace();
            Quarkus.asyncExit(200);
            Quarkus.waitForExit();
            throw new RuntimeException("Failed to handle error");
        }
        
    }

    @Override
    public void onException(final JMSException ex) {
        if (ex != null) {
            if (ex instanceof JMSException) {
                processJMSException((JMSException) ex);
            } else {
                LOGGER.severe(ex.toString());
            }
        }
        context.stop();
        Quarkus.asyncExit();
    }

    /**
     * Process a JMSException and any associated inner exceptions.
     *
     * @param jmsex
     */
    private void processJMSException(final JMSException jmsex) {
       LOGGER.info(jmsex.toString());
        Throwable innerException = jmsex.getLinkedException();
        if (innerException != null) {
            LOGGER.severe("Inner exception(s):");
        }
        while (innerException != null) {
            LOGGER.severe(innerException.toString());
            innerException = innerException.getCause();
        }
    }



}