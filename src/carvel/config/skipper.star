load("@ytt:data", "data")
load("binder/binder.star", "binder_install_enabled")
load("binder/binder.star", "rabbitmq_enabled")
load("binder/binder.star", "kafka_enabled")
load("binder/binder.star", "external_rabbitmq_enabled")
load("binder/binder.star", "external_kafka_enabled")
load("binder/binder.star", "external_rabbitmq_env_str")
load("binder/binder.star", "external_kafka_env_str")
load("monitoring/monitoring.star", "grafana_enabled")

def env_config():
  env = ""
  if rabbitmq_enabled():
    env = "SPRING_RABBITMQ_HOST=${RABBITMQ_SERVICE_HOST},SPRING_RABBITMQ_PORT=${RABBITMQ_SERVICE_PORT},SPRING_RABBITMQ_USERNAME=${rabbitmq-user},SPRING_RABBITMQ_PASSWORD=${rabbitmq-password}"
  elif kafka_enabled():
    env = "SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=kafka-broker:9092,SPRING_CLOUD_STREAM_KAFKA_BINDER_ZK_NODES=kafka-zk-client:2181"
  else:
    if external_rabbitmq_enabled():
      env = external_rabbitmq_env_str()
    elif external_kafka_enabled():
      env = external_kafka_env_str()
    end
  end
  return env
end

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

def skipper_image():
  if non_empty_string(data.values.scdf.skipper.image.digest):
    return data.values.scdf.skipper.image.repository + "@" + data.values.scdf.skipper.image.digest
  else:
    return data.values.scdf.skipper.image.repository + ":" + data.values.scdf.skipper.image.tag
  end
end

def skipper_container_env():
  envs = []
  envs.extend([{"name": "SPRING_CLOUD_CONFIG_ENABLED", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_PATHS", "value": "/etc/secrets"}])
  if grafana_enabled():
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED", "value": "true"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_ENABLED", "value": "true"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_HOST", "value": "prometheus-rsocket-proxy"}])
    envs.extend([{"name": "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_RSOCKET_PORT", "value": "7001"}])
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
  return data.values.scdf.skipper.service.type
end
