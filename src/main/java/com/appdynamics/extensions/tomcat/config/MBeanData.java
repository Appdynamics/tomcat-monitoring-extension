/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.tomcat.config;

import java.util.HashSet;
import java.util.Set;

public class MBeanData {

	private String domainName;
	private Set<String> types = new HashSet<String>();
	private Set<String> excludePatterns = new HashSet<String>();

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public Set<String> getTypes() {
		return types;
	}

	public void setTypes(Set<String> types) {
		this.types = types;
	}

	public Set<String> getExcludePatterns() {
		return excludePatterns;
	}

	public void setExcludePatterns(Set<String> excludePatterns) {
		this.excludePatterns = excludePatterns;
	}
}
