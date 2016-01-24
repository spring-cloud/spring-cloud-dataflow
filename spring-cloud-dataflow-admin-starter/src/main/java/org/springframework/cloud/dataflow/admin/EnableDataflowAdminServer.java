package org.springframework.cloud.dataflow.admin;

import org.springframework.cloud.dataflow.admin.config.AdminConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(AdminConfiguration.class)
public @interface EnableDataflowAdminServer {
}
