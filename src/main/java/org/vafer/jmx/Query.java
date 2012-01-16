package org.vafer.jmx;

import java.util.Collection;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class Query {
    
    public void run(String url, String expression, Filter filter, Output output) throws Exception {
        JMXConnector connector = null;
        try {
        	connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
            MBeanServerConnection connection = connector.getMBeanServerConnection();
            final Collection<ObjectInstance> mbeans = connection.queryMBeans(new ObjectName(expression), null);

            for(ObjectInstance mbean : mbeans) {
                final ObjectName mbeanName = mbean.getObjectName();
                final MBeanInfo mbeanInfo = connection.getMBeanInfo(mbeanName);
                final MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
                for (final MBeanAttributeInfo attribute : attributes) {
                    if (attribute.isReadable()) {
                        if (filter.include(mbeanName, attribute.getName())) {
                            final String attributeName = attribute.getName();
                            try {
                                output.output(
                                        mbean.getObjectName(),
                                        attributeName,
                                        connection.getAttribute(mbeanName, attributeName)
                                        );
                            } catch(Exception e) {
                                // System.err.println("Failed to read " + mbeanName + "." + attributeName);
                            }                        
                        }
                    }
                }

            }
        } finally {
        	if (connector != null) {
        		connector.close();
        	}
        }
    }
}
