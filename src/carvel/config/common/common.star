load("@ytt:data", "data")

deploy_types = {"minikube": "minikube", "cloud": "cloud"}

def minikube_enabled():
  return deploy_types.get(data.values.scdf.deploy.mode) == "minikube"
end

def cloud_enabled():
  return deploy_types.get(data.values.scdf.deploy.mode) == "cloud"
end

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

def has_image_pull_secrets():
  return non_empty_string(data.values.scdf.registry.secret.ref)
end
