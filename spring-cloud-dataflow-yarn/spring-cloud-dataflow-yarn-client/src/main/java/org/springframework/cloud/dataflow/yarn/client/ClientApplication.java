/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.yarn.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.cli.command.Command;
import org.springframework.yarn.boot.cli.AbstractCli;
import org.springframework.yarn.boot.cli.YarnClusterCreateCommand;
import org.springframework.yarn.boot.cli.YarnClusterDestroyCommand;
import org.springframework.yarn.boot.cli.YarnClusterInfoCommand;
import org.springframework.yarn.boot.cli.YarnClusterModifyCommand;
import org.springframework.yarn.boot.cli.YarnClusterStartCommand;
import org.springframework.yarn.boot.cli.YarnClusterStopCommand;
import org.springframework.yarn.boot.cli.YarnClustersInfoCommand;
import org.springframework.yarn.boot.cli.YarnKillCommand;
import org.springframework.yarn.boot.cli.YarnPushCommand;
import org.springframework.yarn.boot.cli.YarnPushedCommand;
import org.springframework.yarn.boot.cli.YarnShutdownCommand;
import org.springframework.yarn.boot.cli.YarnSubmitCommand;
import org.springframework.yarn.boot.cli.YarnSubmittedCommand;
import org.springframework.yarn.boot.cli.YarnSubmittedCommand.SubmittedOptionHandler;
import org.springframework.yarn.boot.cli.shell.ShellCommand;

public class ClientApplication extends AbstractCli {

	public static void main(String... args) {
		List<Command> commands = new ArrayList<Command>();
		commands.add(new YarnPushCommand());
		commands.add(new YarnPushedCommand());
		commands.add(new YarnSubmitCommand());
		commands.add(new YarnSubmittedCommand(new SubmittedOptionHandler("DATAFLOW")));
		commands.add(new YarnKillCommand());
		commands.add(new YarnShutdownCommand());
		commands.add(new YarnClustersInfoCommand());
		commands.add(new YarnClusterInfoCommand());
		commands.add(new YarnClusterCreateCommand());
		commands.add(new YarnClusterStartCommand());
		commands.add(new YarnClusterStopCommand());
		commands.add(new YarnClusterModifyCommand());
		commands.add(new YarnClusterDestroyCommand());
		ClientApplication app = new ClientApplication();
		app.registerCommands(commands);
		app.registerCommand(new ShellCommand(commands));
		app.doMain(args);
	}

}
