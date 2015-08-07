package org.springframework.cloud.data.shell.command;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.shell.AbstractShellIntegrationTest;

/**
 * @author Ilayaperumal Gopinathan
 */
public class StreamCommandTests extends AbstractShellIntegrationTest {


	private static final Logger logger = LoggerFactory.getLogger(StreamCommandTests.class);

	@Test
	public void testStreamLifecycleForTickTock() throws InterruptedException {
		logger.info("Starting Stream Test for TickTock");
		String streamName = generateStreamName();
		stream().create(streamName, "time | log");
	}
}
