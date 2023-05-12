load("@ytt:data", "data")

def grafana_enabled():
  return data.values.scdf.feature.monitoring.grafana.enabled == True
end

def prometheus_rsocket_proxy_enabled():
  return data.values.scdf.feature.monitoring.prometheusRsocketProxy.enabled == True
end

def monitoring_enabled():
  return grafana_enabled() or prometheus_rsocket_proxy_enabled()
end
