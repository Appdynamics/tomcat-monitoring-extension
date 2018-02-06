/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.tomcat.config;

public enum TomcatMBeanKeyPropertyEnum {
	TYPE("type"),
	CONTEXT("context"),
	HOST("host"),
	WORKER("worker"),
	NAME("name");
	
	private String name;
	
	private TomcatMBeanKeyPropertyEnum(String name) {
		this.name = name;
	}
	
	public String toString(){
        return name;
    }
}
