package org.acme;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Singleton;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.MessageListener;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import io.quarkus.runtime.Startup;


@Singleton
public class LedgerStore implements ExceptionListener, MessageListener {

    private static final Logger LOGGER = Logger.getLogger(LedgerStore.class.getName());
    // Create variables for the connection to MQ
    private static final String HOST = "mfnqm0-838f.qm.eu-gb.mq.appdomain.cloud"; // Host name or IP address
    private static final int PORT = 31654; // Listener port for your queue manager
    private static final String CHANNEL = "CLOUD.APP.SVRCONN"; // Channel name
    private static final String QMGR = "mfnqm0"; // Queue manager name
    private static final String APP_USER = "ledgermsg"; // User name that application uses to connect to MQ
    private static final String APP_PASSWORD = "aafDmxXmwHDkPD7GoQ-7iDZdLktYR1UHxwFnTLMltH0k"; // Password that the
                                                                                               // application uses to
                                                                                               // connect to MQ

    Destination destination = null;
    final JMSProducer producer = null;
    JMSConsumer consumer = null; // to and from
    JMSContext context;

    LedgerStore() {
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
            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "Ledger");
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            cf.setStringProperty(WMQConstants.USERID, APP_USER);
            cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);
            context = cf.createContext(JMSContext.SESSION_TRANSACTED);
            // context = cf.createContext(JMSContext.AUTO_ACKNOWLEDGE);

            context.setExceptionListener(this);
            destination = context.createQueue("queue:///LEDGER.ACTIONS");

            consumer = context.createConsumer(destination); // autoclosable
            consumer.setMessageListener(this);
            context.start();
            LOGGER.info("Starting consumer");
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
            // userid
            // destination
            LOGGER.info(message.toString());
            context.commit();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
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