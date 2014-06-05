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
