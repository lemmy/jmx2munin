/*
 * Copyright (c) 2011 Jonathan Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   Jonathan Lee
 *   Benoit Delbosc
 *   Markus Alexander Kuppe
 *
 */
package org.vafer.jmx;

import java.io.IOException;
import java.util.Collection;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ThreadContentionQuery extends Query {
	
	public static final String CONTENTION = "org.vafer.jmx.contention";
	
    private static final String THREADING_OBJ_NAME = "java.lang:type=Threading";
    
    private static final String RUNTIME_OBJ_NAME = "java.lang:type=Runtime";

    public void run(String url, String expression, Filter filter, Output output) throws Exception {
        final JMXConnector connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
        final MBeanServerConnection connection = connector.getMBeanServerConnection();

        // activate contention monitoring
		setContentionMonitoring(connection, true);
        
		// get uptime of VM runtime
		double uptime = getUptime(connection);
		
        // get all thread objs
		final ObjectName objectName = new ObjectName(
				THREADING_OBJ_NAME);
		final CompositeData[] threads = (CompositeData[]) connection.invoke(objectName, "dumpAllThreads", new Object[] {
				new Boolean(true), new Boolean(true) }, new String[] {
				"boolean", "boolean" });
        
		final String aThread = expression.substring(expression.indexOf("=") + 1, expression.length());

		// Iterate all threads
        for (final CompositeData thread : threads) {
        	final String threadName = (String) thread.get("threadName");
			if (!threadName.endsWith(aThread)) {
        		continue;
        	}
        	
        	final long waitedTime = (Long) thread.get("waitedTime");
			output.output(new ObjectName(expression), "waitedTime", (waitedTime / uptime) * 100);
        	
        	final long blockedTime = (Long) thread.get("blockedTime");
			output.output(new ObjectName(expression), "blockedTime", (blockedTime / uptime) * 100);
        }
    }
    
    private Long getUptime(MBeanServerConnection connection) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException, MalformedObjectNameException, NullPointerException, IntrospectionException {
        final Collection<ObjectInstance> mbeans = connection.queryMBeans(new ObjectName(RUNTIME_OBJ_NAME), null);
        for(ObjectInstance mbean : mbeans) {
            final ObjectName mbeanName = mbean.getObjectName();
            final MBeanInfo mbeanInfo = connection.getMBeanInfo(mbeanName);
            final MBeanAttributeInfo[] attributes = mbeanInfo.getAttributes();
            for (final MBeanAttributeInfo attribute : attributes) {
                final String attributeName = attribute.getName();
				if (attributeName.equals("Uptime")) {
                	return (Long) connection.getAttribute(mbeanName, attributeName);
                }
            }
        }
        return Long.valueOf(-1);
	}

	private static final String CONTENTION_ATTR = "ThreadContentionMonitoringEnabled";
    
    /**
     * Enable or disable the thread contention monitoring.
     *
     */
    private static void setContentionMonitoring(MBeanServerConnection mbsc,
            Boolean value) throws MalformedObjectNameException,
            AttributeNotFoundException,
            InstanceNotFoundException, MBeanException, ReflectionException,
            IOException, InvalidAttributeValueException {
        ObjectName objectName = new ObjectName(THREADING_OBJ_NAME);
        Object val = mbsc.getAttribute(objectName, CONTENTION_ATTR);
        if (value.toString().equals(val.toString())) {
            // already set
            return;
        }
        Attribute attribute = new Attribute(CONTENTION_ATTR, value);
        mbsc.setAttribute(objectName, attribute);
        // System.out.println(CONTENTION_ATTR + ":" + val.toString() + " to "
        //        + value.toString());
    }
}
