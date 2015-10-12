package org.springframework.cloud.marathon.connector;

import mesosphere.marathon.client.model.v2.Task;

import org.springframework.cloud.ServiceInfoCreator;
import org.springframework.cloud.service.ServiceInfo;

/**
 * Base class for ServiceInfoCreators running on Marathon. Needed for service discovery via AbstractCloudConnector.
 *
 * @author Eric Bottard
 */
public abstract class MarathonServiceInfoCreator<SI extends ServiceInfo> implements ServiceInfoCreator<SI, Task> {
}
