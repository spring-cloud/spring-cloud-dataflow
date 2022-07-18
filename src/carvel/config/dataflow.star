load("@ytt:data", "data")
load("binder/binder.star", "rabbitmq_enabled")
load("binder/binder.star", "kafka_enabled")
load("monitoring/monitoring.star", "grafana_enabled")

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

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

def dataflow_container_env():
  envs = []
  envs.extend([{"name": "KUBERNETES_NAMESPACE", "valueFrom": {"fieldRef": {"fieldPath": "metadata.namespace"}}}])
  envs.extend([{"name": "SPRING_CLOUD_CONFIG_ENABLED", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_FEATURES_ANALYTICS_ENABLED", "value": "true"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_FEATURES_SCHEDULES_ENABLED", "value": "true"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_TASK_COMPOSEDTASKRUNNER_URI", "value": "docker://" + ctr_image()}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_PATHS", "value": "/etc/secrets"}])
  envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_SERVER_URI", "value": "http://${SCDF_SERVER_SERVICE_HOST}:${SCDF_SERVER_SERVICE_PORT}"}])
  envs.extend([{"name": "SPRING_CLOUD_SKIPPER_CLIENT_SERVER_URI", "value": "http://${SKIPPER_SERVICE_HOST}:${SKIPPER_SERVICE_PORT}/api"}])
  envs.extend([{"name": "SPRING_APPLICATION_JSON", "value": "{ \"maven\": { \"local-repository\": null, \"remote-repositories\": { \"repo1\": { \"url\": \"https://repo.spring.io/libs-snapshot\"} } } }"}])
  if grafana_enabled():
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED", "value": "true"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_ENABLED", "value": "true"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_HOST", "value": "prometheus-rsocket-proxy"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_PORT", "value": "7001"}])
    envs.extend([{"name": "SPRING_CLOUD_DATAFLOW_METRICS_DASHBOARD_URL", "value": "http://localhost:3000"}])
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

def service_spec_type():
  return data.values.scdf.server.service.type
end
