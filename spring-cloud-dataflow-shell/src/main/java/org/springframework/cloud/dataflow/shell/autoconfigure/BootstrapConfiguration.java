package org.springframework.cloud.dataflow.shell.autoconfigure;


import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.cloud.dataflow.shell.EnableDataFlowShell;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.CommandLine;
import org.springframework.shell.ShellException;
import org.springframework.shell.core.ExitShellRequest;
import org.springframework.shell.core.JLineShellComponent;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.logging.Logger;

/**
 * This does basically the same thing as {@link org.springframework.shell.Bootstrap} in Spring Shell,
 * but using Spring Boot's {@link CommandLineRunner} as a callback hook for initialization, instead
 * of squatting on the application's one {@code main(String[] args)} method.
 *
 * @author <a href="mailto:josh@joshlong.com">Josh Long</a>
 */
@Configuration
@ComponentScan(basePackageClasses = EnableDataFlowShell.class)
public class BootstrapConfiguration {


	@Component
	public static class BootstrapCommandLineRunner
			implements CommandLineRunner, ApplicationContextAware {

		private StopWatch stopWatch = new StopWatch("Spring Shell");
		private Logger logger = Logger.getLogger(getClass().getName());
		private ApplicationContext applicationContext;

		@Autowired
		private JLineShellComponent lineShellComponent;

		@Autowired
		private CommandLine commandLine;

		private static class ShellExitCodeGenerator implements ExitCodeGenerator {

			private final ExitShellRequest exitShellRequest;

			public ShellExitCodeGenerator(ExitShellRequest exitShellRequest) {
				this.exitShellRequest = exitShellRequest;
			}

			@Override
			public int getExitCode() {
				return this.exitShellRequest.getExitCode();
			}
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public void run(String... args) throws Exception {
			SpringApplication.exit(this.applicationContext, new ShellExitCodeGenerator(this.doRun()));
		}

		private ExitShellRequest doRun() {
			this.stopWatch.start();
			try {

				String[] commandsToExecuteAndThenQuit = this.commandLine.getShellCommandsToExecute();
				ExitShellRequest exitShellRequest;
				if (null != commandsToExecuteAndThenQuit) {

					boolean successful = false;
					exitShellRequest = ExitShellRequest.FATAL_EXIT;

					for (String cmd : commandsToExecuteAndThenQuit) {
						if (!(successful = this.lineShellComponent.executeCommand(cmd).isSuccess()))
							break;
					}

					if (successful) {
						exitShellRequest = ExitShellRequest.NORMAL_EXIT;
					}
				} else {
					this.lineShellComponent.start();
					this.lineShellComponent.promptLoop();
					exitShellRequest = this.lineShellComponent.getExitShellRequest();
					if (exitShellRequest == null) {
						exitShellRequest = ExitShellRequest.NORMAL_EXIT;
					}
					this.lineShellComponent.waitForComplete();
				}

				if (this.lineShellComponent.isDevelopmentMode()) {
					System.out.println("Total execution time: " + this.stopWatch
							.getLastTaskTimeMillis() + " ms");
				}

				return exitShellRequest;
			} catch (Exception ex) {
				throw new ShellException(ex.getMessage(), ex);
			} finally {
				HandlerUtils.flushAllHandlers(this.logger);
				this.stopWatch.stop();
			}
		}
	}


}

