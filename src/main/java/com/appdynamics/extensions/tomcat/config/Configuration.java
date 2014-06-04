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
package com.appdynamics.extensions.tomcat.config;

public class Configuration {

	private Server server;
	private MBeans mbeans;
	private String metricPrefix;

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public MBeans getMbeans() {
		return mbeans;
	}

	public void setMbeans(MBeans mbeans) {
		this.mbeans = mbeans;
	}

	public String getMetricPrefix() {
		return metricPrefix;
	}

	public void setMetricPrefix(String metricPrefix) {
		this.metricPrefix = metricPrefix;
	}
}
