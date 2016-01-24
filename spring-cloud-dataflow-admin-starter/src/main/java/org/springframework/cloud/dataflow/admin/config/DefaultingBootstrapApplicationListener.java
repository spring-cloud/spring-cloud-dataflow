package org.springframework.cloud.dataflow.admin.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.cloud.bootstrap.BootstrapApplicationListener;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

/**
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Deprecated
public class DefaultingBootstrapApplicationListener
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment configurableEnvironment = event.getEnvironment();
        configurableEnvironment.getPropertySources()
                .addFirst(new MapPropertySource("defaultBootstrap",
                        Collections.singletonMap("spring.cloud.bootstrap.name", Object.class.cast("admin"))));
    }

    @Override
    public int getOrder() {
        return BootstrapApplicationListener.DEFAULT_ORDER - 1;
    }
}
