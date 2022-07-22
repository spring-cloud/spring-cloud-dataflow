load("@ytt:data", "data")
load("@ytt:assert", "assert")

db_types = {"postgres": "postgres", "mysql": "mysql"}
binder_types = {"rabbit": "rabbit", "kafka": "kafka"}

def non_empty_string(value):
  return type(value) == "string" and len(value) > 0
end

def validate():
  errors = []

  if not (non_empty_string(data.values.scdf.server.image.tag) or non_empty_string(data.values.scdf.server.image.digest)) :
    errors.append("Either scdf.server.image.tag or scdf.server.image.digest must be defined")
  end

  if not (non_empty_string(data.values.scdf.skipper.image.tag) or non_empty_string(data.values.scdf.skipper.image.digest)) :
    errors.append("Either scdf.skipper.image.tag or scdf.skipper.image.digest must be defined")
  end

  if not (non_empty_string(data.values.scdf.ctr.image.tag) or non_empty_string(data.values.scdf.ctr.image.digest)) :
    errors.append("Either scdf.ctr.image.tag or scdf.ctr.image.digest must be defined")
  end

  if db_types.get(data.values.scdf.deploy.database.type) == None:
    errors.append("Illegal scdf.deploy.database.type")
  end

  if binder_types.get(data.values.scdf.deploy.binder.type) == None:
    errors.append("Illegal scdf.deploy.binder.type")
  end

  if len(errors) > 0:
    assert.fail("Validation failed with following errors: %s" % (errors,))
  end
end

validate()
