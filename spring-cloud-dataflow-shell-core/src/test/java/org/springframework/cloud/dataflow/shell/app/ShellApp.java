package org.springframework.cloud.dataflow.shell.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.dataflow.rest.client.config.DataFlowClientAutoConfiguration;
import org.springframework.cloud.dataflow.shell.EnableDataFlowShell;


@EnableDataFlowShell
@SpringBootApplication(exclude = DataFlowClientAutoConfiguration.class)
public class ShellApp {
}
