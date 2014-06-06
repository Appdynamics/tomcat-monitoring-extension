# AppDynamics Tomcat Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies. This eXtension monitors Tomcat instance and collects useful statistics exposed through MBeans and reports to AppDynamics Controller.

##Prerequisites

By default, JMX is disabled in the Tomcat instance. JMX has to be enabled in the Tomcat instance for this extension to work. To enable JMX please refer [here] (http://tomcat.apache.org/tomcat-7.0-doc/monitoring.html).

##Installation

1. Run 'mvn clean install' from the tomcat-monitoring-extension directory and find the TomcatMonitor.zip in the 'target' directory.
2. Unzip TomcatMonitor.zip and copy the 'TomcatMonitor' directory to `<MACHINE_AGENT_HOME>/monitors/`
3. Configure the extension by referring to the below section.
5. Restart the Machine Agent.

In the AppDynamics Metric Browser, look for: Application Infrastructure Performance  | \<Tier\> | Custom Metrics | Tomcat in the case of default metric path

## Configuration

Note : Please make sure not to use tab (\t) while editing yaml files. You can validate the yaml file using a [yaml validator](http://yamllint.com/)

1. Configure the Tomcat Extension by editing the config.yml file in `<MACHINE_AGENT_HOME>/monitors/TomcatMonitor/`.
2. Specify the Tomcat instance host, JMX port, username and password in the config.yml. Configure the MBeans for this extension to report the metrics to Controller. By default, "Catalina" is the domain name. Specify the keyproperty 'type' of MBeans you are interested. Tomcat MBean ObjectName is of the form 'Catalina:type=ThreadPool,name="ajp-bio-8009"'. Please refer [here](http://tomcat.apache.org/tomcat-6.0-doc/funcspecs/mbean-names.html) for detailed Tomcat MBean Names.
You can also add excludePatterns (regex) to exclude any metric tree from showing up in the AppDynamics controller.

   For eg.
   ```
        # Tomcat instance
        server:
            host: "localhost"
            port: 9044
            username: ""
            password: ""
            

        # Tomcat mbeans. Exclude patterns with regex can be used to exclude any unwanted metrics.
        mbeans:
            domainName: "Catalina"
            types: [ThreadPool,GlobalRequestProcessor]
            excludePatterns: [
              "Cache|.*",
              "connectionCount",
             ]

        #prefix used to show up metrics in AppDynamics
        metricPrefix:  "Custom Metrics|Tomcat|"

   ```
   In the above config file, metrics are being pulled from two mbeans with type=ThreadPool and type=GlobalRequestProcessor.
   Note that the patterns mentioned in the "excludePatterns" will be excluded from showing up in the AppDynamics Metric Browser.


3. Configure the path to the config.yml file by editing the <task-arguments> in the monitor.xml file in the `<MACHINE_AGENT_HOME>/monitors/TomcatMonitor/` directory. Below is the sample

     ```
     <task-arguments>
         <!-- config file-->
         <argument name="config-file" is-required="true" default-value="monitors/TomcatMonitor/config.yml" />
          ....
     </task-arguments>
    ```



##Metrics

* ThreadPool: maxThreads, currentThreadCount, currentThreadsBusy, connectionCount
* GlobalRequestProcessor: requestCount, errorCount, bytesReceived, bytesSent, processingTime, maxTime
* RequestProcessor: requestCount, errorCount, requestProcessingTime
* Manager (per Webapp): sessionCounter, activeSessions, expiredSessions, maxActive, rejectedSessions


## Custom Dashboard
![]()

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:ace-request@appdynamics.com).
