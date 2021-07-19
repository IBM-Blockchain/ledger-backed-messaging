// SPDX-License-Identifier: Apache-2.0

package org.ledger;

import java.util.logging.Logger;

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

@Singleton
public class LedgerStore implements ExceptionListener, MessageListener {

    private static final Logger LOGGER = Logger.getLogger(LedgerStore.class.getName());

    // Create variables for the connection to MQ
    @ConfigProperty(name="MQ_HOST")
    private String HOST; 

    @ConfigProperty(name="MQ_PORT")
    private int PORT;

    @ConfigProperty(name="MQ_CHANNEL")
    private String CHANNEL;

    @ConfigProperty(name="MQ_QM")
    private String QMGR;

    @ConfigProperty(name="MQ_APP_USER")
    private String APP_USER;

    @ConfigProperty(name="MQ_APP_PASSWORD")
    private String APP_PASSWORD;

    
    private String LEDGER_ACTIONS="queue:///LEDGER.ACTION";

    Destination destination = null;
    final JMSProducer producer = null;
    JMSConsumer consumer = null; // to and from
    JMSContext context;


    @Inject
    public FabricFactory factory;


    LedgerStore() {
    }

    public void disconnect(){
        context.close();
    }

    public void connect() {
        LOGGER.info("LedgerStore  started");

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
            throw new RuntimeException("Failued to handle error");
        }
    }

    @Override
    public void onMessage(final javax.jms.Message message) {
        try {
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

            LOGGER.info("For Trade ["+tradeId+"] message ["+msgId+"] has ["+event+"]" );

            Fabric f = factory.getFabric();
            f.submitEvent(tradeId,msgId,event);

            context.commit();
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException("Failued to handle error");
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