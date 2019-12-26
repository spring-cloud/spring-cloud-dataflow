package org.springframework.cloud.dataflow.shell.command;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;

public class TaskScheduleCommandsTest extends AbstractShellIntegrationTest {

	@BeforeClass
	public static void setUp() throws InterruptedException {
		Thread.sleep(2000);
	}

	@Test
	public void createSchedule() {
		schedule().create("schedName", "def", "* * * * *", "", "");
	}

	@Test
	public void unschedule() {
		schedule().unschedule("schedName");
	}

	@Test
	public void list() {
		schedule().list();
	}

	@Test
	public void listByTaskDefinition() {
		schedule().listByTaskDefinition("definition1");
	}
}
