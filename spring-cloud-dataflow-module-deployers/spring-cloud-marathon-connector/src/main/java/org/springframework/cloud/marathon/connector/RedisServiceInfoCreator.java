package org.springframework.cloud.marathon.connector;

import mesosphere.marathon.client.model.v2.Task;

import org.springframework.cloud.service.common.RedisServiceInfo;

/**
 * A very simplistic redis service info creator that expects that the appId contains "redis".
 *
 * @author Eric Bottard
 */
public class RedisServiceInfoCreator extends MarathonServiceInfoCreator<RedisServiceInfo> {

	public boolean accept(Task task) {
		return task.getAppId().contains("redis");
	}

	public RedisServiceInfo createServiceInfo(Task task) {
		// Expect a single host
		return new RedisServiceInfo(task.getId(), task.getHost(), task.getPorts().iterator().next(), null);
	}
}
