package org.springframework.cloud.dataflow.shell;

import org.springframework.cloud.dataflow.shell.autoconfigure.BootstrapConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Activates the Spring Cloud Data Flow shell.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(BootstrapConfiguration.class)
public @interface EnableDataFlowShell {

}
