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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.tomcat.config.ConfigUtil;
import com.appdynamics.extensions.tomcat.config.Configuration;
import com.appdynamics.extensions.tomcat.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;

public class TomcatMonitor extends AManagedMonitor {

	public static final Logger logger = Logger.getLogger(TomcatMonitor.class);
	public static final String METRICS_SEPARATOR = "|";
	private static final String CONFIG_ARG = "config-file";
	private static final String FILE_NAME = "monitors/TomcatMonitor/config.yml";
	private static final int DEFAULT_NUMBER_OF_THREADS = 10;
	public static final int DEFAULT_THREAD_TIMEOUT = 10;

	private ExecutorService threadPool;

	private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

	public TomcatMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
	}

	public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext arg1) throws TaskExecutionException {
		if (taskArgs != null) {
			logger.info("Starting " + getImplementationVersion() + " Monitoring Task");
			String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
			try {
				Configuration config = configUtil.readConfig(configFilename, Configuration.class);
				threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
				List<Future<TomcatMetrics>> parallelTasks = createConcurrentTasks(config);

				List<TomcatMetrics> tomcatMetrics = collectMetrics(parallelTasks,
						config.getThreadTimeout() == 0 ? DEFAULT_THREAD_TIMEOUT : config.getThreadTimeout());
				printStats(config, tomcatMetrics);
				logger.info("Completed the Tomcat Monitoring Task successfully");
				return new TaskOutput("Tomcat Monitor executed successfully");
			} catch (FileNotFoundException e) {
				logger.error("Config File not found: " + configFilename, e);
			} catch (Exception e) {
				logger.error("Metrics Collection Failed: ", e);
			} finally {
				if (!threadPool.isShutdown()) {
					threadPool.shutdown();
				}
			}
		}
		throw new TaskExecutionException("Tomcat Monitor completed with failures");
	}

	private List<Future<TomcatMetrics>> createConcurrentTasks(Configuration config) {
		List<Future<TomcatMetrics>> parallelTasks = new ArrayList<Future<TomcatMetrics>>();
		if (config != null && config.getServers() != null) {
			for (Server server : config.getServers()) {
				TomcatMonitorTask tomcatTask = new TomcatMonitorTask(server, config.getMbeans());
				parallelTasks.add(threadPool.submit(tomcatTask));
			}
		}
		return parallelTasks;
	}

	private List<TomcatMetrics> collectMetrics(List<Future<TomcatMetrics>> parallelTasks, int timeout) {
		List<TomcatMetrics> allMetrics = new ArrayList<TomcatMetrics>();
		for (Future<TomcatMetrics> aParallelTask : parallelTasks) {
			TomcatMetrics tomcatMetric = null;
			try {
				tomcatMetric = aParallelTask.get(timeout, TimeUnit.SECONDS);
				allMetrics.add(tomcatMetric);
			} catch (Exception e) {
				logger.error("Exception " + e);
			}
		}
		return allMetrics;
	}

	private void printStats(Configuration config, List<TomcatMetrics> tomcatMetrics) {
		for (TomcatMetrics tMetric : tomcatMetrics) {
			StringBuilder metricPath = new StringBuilder();
			metricPath.append(config.getMetricPrefix()).append(tMetric.getDisplayName()).append(METRICS_SEPARATOR);
			Map<String, String> metricsForAServer = tMetric.getMetrics();
			for (Map.Entry<String, String> entry : metricsForAServer.entrySet()) {
				printMetric(metricPath.toString() + entry.getKey(), entry.getValue());
			}
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
