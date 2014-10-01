/**
 * Copyright 2014 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.appdynamics.extensions.tomcat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.tomcat.config.MBeanData;
import com.appdynamics.extensions.tomcat.config.Server;
import com.appdynamics.extensions.tomcat.config.TomcatMBeanKeyPropertyEnum;
import com.appdynamics.extensions.tomcat.config.TomcatMonitorConstants;
import com.appdynamics.extensions.util.MetricUtils;
import com.google.common.base.Strings;

public class TomcatMonitorTask implements Callable<TomcatMetrics> {

	public static final String METRICS_SEPARATOR = "|";
	private Server server;
	private Map<String, MBeanData> mbeanLookup;
	private JMXConnectionUtil jmxConnector;
	public static final Logger logger = Logger.getLogger("com.singularity.extensions.TomcatMonitorTask");

	public TomcatMonitorTask(Server server, MBeanData[] mbeansData) {
		this.server = server;
		createMBeansLookup(mbeansData);
	}

	private void createMBeansLookup(MBeanData[] mbeansData) {
		mbeanLookup = new HashMap<String, MBeanData>();
		if (mbeansData != null) {
			for (MBeanData mBeanData : mbeansData) {
				mbeanLookup.put(mBeanData.getDomainName(), mBeanData);
			}
		}
	}

	public TomcatMetrics call() throws Exception {
		TomcatMetrics tomcatMetrics = new TomcatMetrics();
		tomcatMetrics.setDisplayName(server.getDisplayName());
		try {
			jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(),
					server.getPassword()));
			JMXConnector connector = jmxConnector.connect();
			if (connector != null) {
				Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
				if (allMbeans != null) {
					Map<String, String> filteredMetrics = applyExcludePatternsAndExtractMetrics(allMbeans);
					filteredMetrics.put(TomcatMonitorConstants.METRICS_COLLECTED, TomcatMonitorConstants.SUCCESS_VALUE);
					tomcatMetrics.setMetrics(filteredMetrics);
				}
			}
		} catch (Exception e) {
			logger.error("Error JMX-ing into Tomcat Server " + tomcatMetrics.getDisplayName(), e);
			tomcatMetrics.getMetrics().put(TomcatMonitorConstants.METRICS_COLLECTED, TomcatMonitorConstants.ERROR_VALUE);
		} finally {
			try {
				if (jmxConnector != null) {
					jmxConnector.close();
				}
			} catch (Exception e) {
				// Ignore
			}
		}
		return tomcatMetrics;
	}

	private Map<String, String> applyExcludePatternsAndExtractMetrics(Set<ObjectInstance> allMbeans) {
		Map<String, String> filteredMetrics = new HashMap<String, String>();
		for (ObjectInstance mbean : allMbeans) {
			ObjectName objectName = mbean.getObjectName();
			if (isDomainAndKeyPropertyConfigured(objectName)) {
				MBeanData mbeanData = mbeanLookup.get(objectName.getDomain());
				Set<String> excludePatterns = mbeanData.getExcludePatterns();
				MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
				if (attributes != null) {
					for (MBeanAttributeInfo attr : attributes) {
						if (attr.isReadable()) {
							Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
							if (attribute != null && attribute instanceof Number) {
								String metricKey = getMetricsKey(objectName, attr);
								if (!isKeyExcluded(metricKey, excludePatterns)) {
									String attrStrValue = MetricUtils.toWholeNumberString(attribute);
									filteredMetrics.put(metricKey, attrStrValue);
								} else {
									if (logger.isDebugEnabled()) {
										logger.info(metricKey + " is excluded");
									}
								}
							}
						}
					}
				}
			}
		}
		return filteredMetrics;
	}

	private boolean isDomainAndKeyPropertyConfigured(ObjectName objectName) {
		MBeanData mBeanData = mbeanLookup.get(objectName.getDomain());
		if (mBeanData == null) {
			return false;
		}
		String domain = mBeanData.getDomainName();
		String keyProperty = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.TYPE.toString());
		Set<String> types = mBeanData.getTypes();
		boolean configured = mBeanData.getDomainName().equals(domain) && types.contains(keyProperty);
		return configured;
	}

	private String getMetricsKey(ObjectName objectName, MBeanAttributeInfo attr) {
		String type = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.TYPE.toString());
		String context = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.CONTEXT.toString());
		String host = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.HOST.toString());
		String worker = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.WORKER.toString());
		String name = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.NAME.toString());

		StringBuilder metricsKey = new StringBuilder();
		metricsKey.append(Strings.isNullOrEmpty(type) ? "" : type + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(context) ? "" : context + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(host) ? "" : host + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(worker) ? "" : worker + METRICS_SEPARATOR);
		metricsKey.append(Strings.isNullOrEmpty(name) ? "" : name + METRICS_SEPARATOR);
		metricsKey.append(attr.getName());

		return metricsKey.toString();
	}

	private boolean isKeyExcluded(String metricKey, Set<String> excludePatterns) {
		for (String excludePattern : excludePatterns) {
			if (metricKey.matches(escapeText(excludePattern))) {
				return true;
			}
		}
		return false;
	}

	private String escapeText(String excludePattern) {
		return excludePattern.replaceAll("\\|", "\\\\|");
	}
}
