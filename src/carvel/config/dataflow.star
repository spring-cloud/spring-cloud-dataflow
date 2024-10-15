load("@ytt:data", "data")
load("monitoring/monitoring.star", "grafana_enabled")
load("monitoring/monitoring.star", "prometheus_rsocket_proxy_enabled")
load("common/common.star", "non_empty_string")

def dataflow_image():
  if non_empty_string(data.values.scdf.server.image.digest):
    return data.values.scdf.server.image.repository + "@" + data.values.scdf.server.image.digest
  else:
    return data.values.scdf.server.image.repository + ":" + data.values.scdf.server.image.tag
  end
end

def ctr_image():
  if non_empty_string(data.values.scdf.ctr.image.digest):
    return data.values.scdf.ctr.image.repository + "@" + data.values.scdf.ctr.image.digest
  else:
    return data.values.scdf.ctr.image.repository + ":" + data.values.scdf.ctr.image.tag
  end
end

def dataflow_db_dialect():
  return data.values.scdf.server.database.dialect
end

def dataflow_container_env():
  envs = []
  envs.extend([{"name": "LANG", "value": "en_US.utf8"}])
  envs.extend([{"name": "LC_ALL", "value": "en_US.utf8"}])
  envs.extend([{"name": "KUBERNETES_NAMESPACE", "valueFrom": {"fieldRef": {"fieldPath": "metadata.namespace"}}}])
  envs.extend([{"name": "SPRING_CLOUD_CONFIG_ENABLED", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_FEATURES_ANALYTICS_ENABLED", "value": "true"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_FEATURES_SCHEDULES_ENABLED", "value": "true"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI", "value": "docker://" + ctr_image()}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_PATHS", "value": "/workspace/runtime/secrets"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_SERVER_URI", "value": "http://${SCDF_SERVER_SERVICE_HOST}:${SCDF_SERVER_SERVICE_PORT}"}])
  envs.extend([{"name": "SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI", "value": "http://${SKIPPER_SERVICE_HOST}:${SKIPPER_SERVICE_PORT}/api"}])
  if non_empty_string(dataflow_db_dialect()):
    envs.extend([{"name": "SPRING_JPA_DATABASE_PLATFORM", "value": dataflow_db_dialect()}])
  end
  if grafana_enabled():
    envs.extend([{"name": "MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED", "value": "true"}])
  end
  if prometheus_rsocket_proxy_enabled():
    envs.extend([{"name": "MANAGEMENT_PROMETHEUS_METRICS_EXPORT_RSOCKET_ENABLED", "value": "true"}])
  end
  if non_empty_string(data.values.scdf.server.database.secretName):
    if non_empty_string(data.values.scdf.server.database.secretUsernameKey):
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.server.database.secretName, "key": data.values.scdf.server.database.secretUsernameKey}}}])
    else:
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.server.database.secretName, "key": "username"}}}])
    end
    if non_empty_string(data.values.scdf.server.database.secretPasswordKey):
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.server.database.secretName, "key": data.values.scdf.server.database.secretPasswordKey}}}])
    else:
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.server.database.secretName, "key": "password"}}}])
    end
  else:
    if non_empty_string(data.values.scdf.server.database.username):
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "value": data.values.scdf.server.database.username}])
    end
    if non_empty_string(data.values.scdf.server.database.password):
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "value": data.values.scdf.server.database.password}])
    end
  end
  for e in data.values.scdf.server.env:
      envs.extend([{"name": e.name, "value": e.value}])
  end
  return envs
end

def has_image_pull_secrets():
  return non_empty_string(data.values.scdf.registry.secret.ref)
end

def registry_secret_ref():
  return data.values.scdf.registry.secret.ref
end

def image_pull_secrets():
  return [{"name": registry_secret_ref()}]
end
def has_service_spec_type():
  return non_empty_string(data.values.scdf.server.service.type)
end

def service_spec_type_loadbalancer():
  return non_empty_string(data.values.scdf.server.service.type) and data.values.scdf.server.service.type == 'LoadBalancer'
end

def service_spec_type():
  return data.values.scdf.server.service.type
end

def service_spec_allocate_load_balancer_node_ports():
  return data.values.scdf.server.service.allocateLoadBalancerNodePorts
end

def has_service_spec_load_balancer_class():
  return non_empty_string(data.values.scdf.server.service.loadBalancerClass)
end

def service_spec_load_balancer_class():
  return data.values.scdf.server.service.loadBalancerClass
end

def context_path():
  return data.values.scdf.server.contextPath
end

def has_context_path():
  return non_empty_string(data.values.scdf.server.contextPath)
end

def dataflow_liveness_path():
  return data.values.scdf.server.contextPath + "/management/health/liveness"
end

def dataflow_readiness_path():
  return data.values.scdf.server.contextPath + "/management/health/readiness"
end

def dataflow_has_password():
  return non_empty_string(data.values.scdf.server.database.password)
end
