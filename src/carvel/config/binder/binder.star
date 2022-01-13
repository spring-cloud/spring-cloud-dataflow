load("@ytt:data", "data")
binder_types = {"rabbit": "rabbit", "kafka": "kafka"}

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

def binder_deploy_enabled():
  return data.values.scdf.deploy.binder.enabled == True
end

def rabbitmq_enabled():
  return binder_types.get(data.values.scdf.deploy.binder.type) == "rabbit" and binder_deploy_enabled()
end

def kafka_enabled():
  return binder_types.get(data.values.scdf.deploy.binder.type) == "kafka" and binder_deploy_enabled()
end

def external_rabbitmq_enabled():
  return non_empty_string(data.values.scdf.binder.rabbit.host);
end

def external_kafka_enabled():
  return non_empty_string(data.values.scdf.binder.kafka.broker.host);
end

def binder_install_enabled():
  return not non_empty_string(data.values.scdf.binder.rabbit.host) or not non_empty_string(data.values.scdf.binder.kafka.broker.host);
end

def external_rabbitmq_env_str():
  values = []
  if non_empty_string(data.values.scdf.binder.rabbit.host):
    values.append("SPRING_RABBITMQ_HOST=" + data.values.scdf.binder.rabbit.host)
  end
  if non_empty_string(str(data.values.scdf.binder.rabbit.port)):
    values.append("SPRING_RABBITMQ_PORT=" + str(data.values.scdf.binder.rabbit.port))
  end
  if non_empty_string(data.values.scdf.binder.rabbit.username):
    values.append("SPRING_RABBITMQ_USERNAME=" + data.values.scdf.binder.rabbit.username)
  end
  if non_empty_string(data.values.scdf.binder.rabbit.password):
    values.append("SPRING_RABBITMQ_PASSWORD=" + data.values.scdf.binder.rabbit.password)
  end
  return ",".join(values)
end

def external_kafka_env_str():
  values = []
  if non_empty_string(data.values.scdf.binder.kafka.broker.host):
    values.append("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS=" + data.values.scdf.binder.kafka.broker.host + ":" + str(data.values.scdf.binder.kafka.broker.port))
    values.append("SPRING_CLOUD_STREAM_KAFKA_BINDER_ZK_NODES=" + data.values.scdf.binder.kafka.zk.host + ":" + str(data.values.scdf.binder.kafka.zk.port))
  end
  return ",".join(values)
end
