package org.springframework.cloud.marathon.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;
import mesosphere.marathon.client.model.v2.Task;

import org.springframework.cloud.AbstractCloudConnector;
import org.springframework.cloud.FallbackServiceInfoCreator;
import org.springframework.cloud.app.ApplicationInstanceInfo;
import org.springframework.cloud.app.BasicApplicationInstanceInfo;
import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.util.EnvironmentAccessor;

/**
 * A cloud connector for Mesos/Marathon.
 *
 * @author Eric Bottard
 */
public class MarathonCloudConnector extends AbstractCloudConnector<Task> {

	private final EnvironmentAccessor environment = new EnvironmentAccessor();

	private final Marathon marathon;

	public MarathonCloudConnector() {
		super((Class) MarathonServiceInfoCreator.class);
		String marathonHost = environment.getEnvValue("SPRING_CLOUD_MARATHON_HOST");
		if (marathonHost == null) {
			// Default value for mesos/playa
			marathonHost = "http://10.141.141.10:8080";
		}
		this.marathon = MarathonClient.getInstance(marathonHost);
	}

	@Override
	protected List<Task> getServicesData() {
		return new ArrayList<>(marathon.getTasks().getTasks());
	}

	@Override
	protected FallbackServiceInfoCreator<BaseServiceInfo, Task> getFallbackServiceInfoCreator() {
		return new FallbackServiceInfoCreator<BaseServiceInfo, Task>() {
			@Override
			public BaseServiceInfo createServiceInfo(Task task) {
				return new BaseServiceInfo(task.getId());
			}
		};
	}

	public boolean isInMatchingCloud() {
		return environment.getEnv().containsKey("MARATHON_APP_ID");
	}

	@Override
	public ApplicationInstanceInfo getApplicationInstanceInfo() {
		String instanceId = environment.getEnvValue("MESOS_TASK_ID");
		String appId = environment.getEnvValue("MARATHON_APP_ID");
		HashMap<String, Object> map = new HashMap<>();
		return new BasicApplicationInstanceInfo(instanceId, appId, map);
	}
}
