load("@ytt:data", "data")
load("binder/binder.star", "external_rabbitmq_enabled")
load("binder/binder.star", "external_kafka_enabled")
load("binder/binder.star", "external_rabbitmq_env_str")
load("binder/binder.star", "external_kafka_env_str")
load("monitoring/monitoring.star", "grafana_enabled")
load("monitoring/monitoring.star", "prometheus_rsocket_proxy_enabled")
load("common/common.star", "non_empty_string")
def env_config():
  env = [] 
  env.append("LANG=en_US.utf8")
  env.append("LC_ALL=en_US.utf8")
  env.append("JDK_JAVA_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8")
  if external_rabbitmq_enabled():
    env.append(external_rabbitmq_env_str())
  elif external_kafka_enabled():
    env.append(external_kafka_env_str())
  end
  return ",".join(env)
end

def skipper_image():
  if non_empty_string(data.values.scdf.skipper.image.digest):
    return data.values.scdf.skipper.image.repository + "@" + data.values.scdf.skipper.image.digest
  else:
    return data.values.scdf.skipper.image.repository + ":" + data.values.scdf.skipper.image.tag
  end
end

def skipper_db_dialect():
  return data.values.scdf.skipper.database.dialect
end

def skipper_container_env():
  envs = []
  envs.extend([{"name": "LANG", "value": "en_US.utf8"}])
  envs.extend([{"name": "LC_ALL", "value": "en_US.utf8"}])
  envs.extend([{"name": "JDK_JAVA_OPTIONS", "value": "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"}])
  envs.extend([{"name": "SPRING_CLOUD_CONFIG_ENABLED", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_CONFIG_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_ENABLE_API", "value": "false"}])
  envs.extend([{"name": "SPRING_CLOUD_KUBERNETES_SECRETS_PATHS", "value": "/workspace/runtime/secrets"}])
  if non_empty_string(skipper_db_dialect()):
    envs.extend([{"name": "SPRING_JPA_DATABASE_PLATFORM", "value": skipper_db_dialect()}])
  end
  if non_empty_string(data.values.scdf.skipper.database.secretName):
    if non_empty_string(data.values.scdf.skipper.database.secretUsernameKey):
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.skipper.database.secretName, "key": data.values.scdf.skipper.database.secretUsernameKey}}}])
    else:
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.skipper.database.secretName, "key": "username"}}}])
    end
    if non_empty_string(data.values.scdf.skipper.database.secretPasswordKey):
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.skipper.database.secretName, "key": data.values.scdf.skipper.database.secretPasswordKey}}}])
    else:
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": {"secretKeyRef": {"name": data.values.scdf.skipper.database.secretName, "key": "password"}}}])
    end
  else:
    if non_empty_string(data.values.scdf.skipper.database.username):
      envs.extend([{"name": "SPRING_DATASOURCE_USERNAME", "value": data.values.scdf.skipper.database.username}])
    end
    if non_empty_string(data.values.scdf.skipper.database.password):
      envs.extend([{"name": "SPRING_DATASOURCE_PASSWORD", "value": data.values.scdf.skipper.database.password}])
    end
  end
  if grafana_enabled():
    envs.extend([{"name": "MANAGEMENT_PROMETHEUS_METRICS_EXPORT_ENABLED", "value": "true"}])
  end
  if prometheus_rsocket_proxy_enabled():
    envs.extend([{"name": "MANAGEMENT_PROMETHEUS_METRICS_EXPORT_RSOCKET_ENABLED", "value": "true"}])
  end
  for e in data.values.scdf.skipper.env:
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

def service_spec_type():
  return data.values.scdf.skipper.service.type
end

def service_spec_type_loadbalancer():
  return non_empty_string(data.values.scdf.skipper.service.type) and data.values.scdf.skipper.service.type == 'LoadBalancer'
end

def service_spec_allocate_load_balancer_node_ports():
  return data.values.scdf.skipper.service.allocateLoadBalancerNodePorts
  end

def has_service_spec_load_balancer_class():
  return non_empty_string(data.values.scdf.skipper.service.loadBalancerClass)
  end

def service_spec_load_balancer_class():
  return data.values.scdf.skipper.service.loadBalancerClass
end

def skipper_has_password():
  return non_empty_string(data.values.scdf.skipper.database.password)
end

