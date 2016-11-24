package org.springframework.cloud.dataflow.server.repository;

import org.springframework.hateoas.ResourceSupport;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by A626200 on 23/11/2016.
 */
public class AppPropertiesResource extends ResourceSupport implements Serializable {

    String appName;

    Map<String, String> appProperties;

    public AppPropertiesResource(String appName, Map<String, String> appProperties) {
        this.appName = appName;
        this.appProperties = appProperties;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Map<String, String> getAppProperties() {
        if( appProperties == null)
            appProperties = new HashMap<>();
        return appProperties;
    }

    public void setAppProperties(Map<String, String> appProperties) {
        this.appProperties = appProperties;
    }
}
