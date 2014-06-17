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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.jmx.JMXConnectionConfig;
import com.appdynamics.extensions.jmx.JMXConnectionUtil;
import com.appdynamics.extensions.tomcat.config.ConfigUtil;
import com.appdynamics.extensions.tomcat.config.Configuration;
import com.appdynamics.extensions.tomcat.config.MBeanData;
import com.appdynamics.extensions.tomcat.config.Server;
import com.appdynamics.extensions.tomcat.config.TomcatMBeanKeyPropertyEnum;
import com.appdynamics.extensions.tomcat.config.TomcatMonitorConstants;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class TomcatMonitor extends AManagedMonitor {

	public static final Logger logger = Logger.getLogger("com.singularity.extensions.TomcatMonitor");
	public static final String METRICS_SEPARATOR = "|";
	private static final String CONFIG_ARG = "config-file";
	private static final String FILE_NAME = "monitors/TomcatMonitor/config.yml";

	private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

	public TomcatMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		if (taskArgs != null) {
			logger.info("Starting the Tomcat Monitoring task.");
			String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
			try {
				Configuration config = configUtil.readConfig(configFilename, Configuration.class);
				Map<String, String> metrics = populateStats(config);
				printStats(config, metrics);
				logger.info("Completed the Tomcat Monitoring Task successfully");
				return new TaskOutput("Tomcat Monitor executed successfully");
			} catch (FileNotFoundException e) {
				logger.error("Config File not found: " + configFilename, e);
			} catch (Exception e) {
				logger.error("Metrics Collection Failed: ", e);
			}
		}
		throw new TaskExecutionException("Tomcat Monitor completed with failures");
	}

	private Map<String, String> populateStats(Configuration config) throws Exception {
		JMXConnectionUtil jmxConnector = null;
		Map<String, String> metrics = new HashMap<String, String>();
		Server server = config.getServer();
		MBeanData mbeanData = config.getMbeans();
		try {
			jmxConnector = new JMXConnectionUtil(new JMXConnectionConfig(server.getHost(), server.getPort(), server.getUsername(),
					server.getPassword()));
			if (jmxConnector != null && jmxConnector.connect() != null) {
				Set<ObjectInstance> allMbeans = jmxConnector.getAllMBeans();
				if (allMbeans != null) {
					metrics = extractMetrics(jmxConnector, mbeanData, allMbeans);
					metrics.put(TomcatMonitorConstants.METRICS_COLLECTED, TomcatMonitorConstants.SUCCESS_VALUE);
				}
			}
		} catch (Exception e) {
			logger.error("Error JMX-ing into Tomcat Server ", e);
			metrics.put(TomcatMonitorConstants.METRICS_COLLECTED, TomcatMonitorConstants.ERROR_VALUE);
		} finally {
			jmxConnector.close();
		}
		return metrics;
	}

	private Map<String, String> extractMetrics(JMXConnectionUtil jmxConnector, MBeanData mbeanData, Set<ObjectInstance> allMbeans) {
		Map<String, String> metrics = new HashMap<String, String>();
		Set<String> excludePatterns = mbeanData.getExcludePatterns();
		for (ObjectInstance mbean : allMbeans) {
			ObjectName objectName = mbean.getObjectName();
			if (isDomainAndKeyPropertyConfigured(objectName, mbeanData)) {
				MBeanAttributeInfo[] attributes = jmxConnector.fetchAllAttributesForMbean(objectName);
				if (attributes != null) {
					for (MBeanAttributeInfo attr : attributes) {
						if (attr.isReadable()) {
							Object attribute = jmxConnector.getMBeanAttribute(objectName, attr.getName());
							if (attribute != null && attribute instanceof Number) {
								String metricKey = getMetricsKey(objectName, attr);
								if (!isKeyExcluded(metricKey, excludePatterns)) {
									metrics.put(metricKey, attribute.toString());
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
		return metrics;
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

	private boolean isDomainAndKeyPropertyConfigured(ObjectName objectName, MBeanData mbeanData) {
		String domain = objectName.getDomain();
		String keyProperty = objectName.getKeyProperty(TomcatMBeanKeyPropertyEnum.TYPE.toString());
		Set<String> types = mbeanData.getTypes();
		boolean configured = mbeanData.getDomainName().equals(domain) && types.contains(keyProperty);
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

	private void printStats(Configuration config, Map<String, String> metrics) {
		String metricPath = config.getMetricPrefix();
		for (Map.Entry<String, String> entry : metrics.entrySet()) {
			printMetric(metricPath + entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Returns a config file name,
	 * 
	 * @param filename
	 * @return String
	 */
	private String getConfigFilename(String filename) {
		if (filename == null) {
			return "";
		}

		if ("".equals(filename)) {
			filename = FILE_NAME;
		}
		// for absolute paths
		if (new File(filename).exists()) {
			return filename;
		}
		// for relative paths
		File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
		String configFileName = "";
		if (!Strings.isNullOrEmpty(filename)) {
			configFileName = jarPath + File.separator + filename;
		}
		return configFileName;
	}

	private void printMetric(String metricPath, String metricValue) {
		printMetric(metricPath, metricValue, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
				MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE);
	}

	private void printMetric(String metricPath, String metricValue, String aggregation, String timeRollup, String cluster) {
		MetricWriter metricWriter = super.getMetricWriter(metricPath, aggregation, timeRollup, cluster);
		if (metricValue != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Metric [" + metricPath + " = " + metricValue + "]");
			}
			metricWriter.printMetric(metricValue);
		}
	}

	private static String getImplementationVersion() {
		return TomcatMonitor.class.getPackage().getImplementationTitle();
	}
}
