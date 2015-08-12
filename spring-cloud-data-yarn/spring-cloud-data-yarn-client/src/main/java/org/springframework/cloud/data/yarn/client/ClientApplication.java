package org.springframework.cloud.data.yarn.client;

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
		commands.add(new YarnSubmittedCommand(new SubmittedOptionHandler("CLOUDDATA")));
		commands.add(new YarnKillCommand());
		// disable due to boot #3724 which broke
		// container registrar
		//commands.add(new YarnShutdownCommand());
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
