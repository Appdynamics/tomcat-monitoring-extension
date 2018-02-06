/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.tomcat;

import java.util.Map;

import com.google.common.collect.Maps;

public class TomcatMetrics {

	private String displayName;
	private Map<String, String> metrics;

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Map<String, String> getMetrics() {
		if (metrics == null) {
			metrics = Maps.newHashMap();
		}
		return metrics;
	}

	public void setMetrics(Map<String, String> metrics) {
		this.metrics = metrics;
	}
}
