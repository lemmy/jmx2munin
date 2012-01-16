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

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ThreadContentionQuery extends Query {
	
	public static final String CONTENTION = "org.vafer.jmx.contention";
	
    private static final String THREADING_OBJ_NAME = "java.lang:type=Threading";
    
    public void run(String url, String expression, Filter filter, Output output) throws Exception {
    	JMXConnector connector = null;
    	try {
    		connector = JMXConnectorFactory.connect(new JMXServiceURL(url));
    		final MBeanServerConnection connection = connector.getMBeanServerConnection();
    		
    		// activate contention monitoring
    		setContentionMonitoring(connection, true);
    		
    		// get uptime of VM runtime
    		double uptime = (Long) connection.getAttribute(new ObjectName("java.lang:type=Runtime"), "Uptime");
    		
    		// get all thread objs
    		final ObjectName objectName = new ObjectName(
    				THREADING_OBJ_NAME);
    		final CompositeData[] threads = (CompositeData[]) connection.invoke(objectName, "dumpAllThreads", new Object[] {
    				new Boolean(true), new Boolean(true) }, new String[] {
    				"boolean", "boolean" });
    		
    		int indexOf = expression.indexOf("=");
			final String aThread = expression.substring(indexOf + 1, expression.length());
			final String prefix = expression.substring(0, indexOf + 1);
    		
    		// Iterate all threads
    		for (final CompositeData thread : threads) {
    			final String threadName = (String) thread.get("threadName");
    			if (!threadName.contains(aThread)) {
    				continue;
    			}
    			
    			final long waitedTime = (Long) thread.get("waitedTime");
    			output.output(new ObjectName(prefix + threadName.toLowerCase()), "waitedTime", (waitedTime / uptime) * 100);
    			
    			final long blockedTime = (Long) thread.get("blockedTime");
    			output.output(new ObjectName(prefix + threadName.toLowerCase()), "blockedTime", (blockedTime / uptime) * 100);
    		}
    	} finally {
    		if(connector != null)
    			connector.close();
    	}
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
