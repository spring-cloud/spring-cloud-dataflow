load("@ytt:data", "data")

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

def has_image_pull_secrets():
  return non_empty_string(data.values.registrySecretRef)
end

def registry_secret_ref():
  return data.values.registrySecretRef
end

def image_pull_secrets():
  return [{"name": registry_secret_ref()}]
end
