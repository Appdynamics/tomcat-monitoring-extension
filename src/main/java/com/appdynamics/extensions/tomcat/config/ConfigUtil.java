/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 * All Rights Reserved.
 * This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 * The copyright notice above does not evidence any actual or intended publication of such source code.
 */
package com.appdynamics.extensions.tomcat.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

public class ConfigUtil<T> {

	private static final Logger logger = Logger.getLogger("com.singularity.extensions.ConfigUtil");

	public T readConfig(String fileName, Class<T> clazz) throws FileNotFoundException {
		logger.info("Reading config file: " + fileName);
		Yaml yaml = new Yaml(new Constructor(clazz));
		T config = (T) yaml.load(new FileInputStream(fileName));
		return config;
	}

}
