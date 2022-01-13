load("@ytt:data", "data")
load("@ytt:yaml", "yaml")

def schema():
  content = yaml.decode(data.read("values-schema.yml"))
  return content.get("components").get("schemas").get("scdfPackage")
end
