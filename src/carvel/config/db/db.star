load("@ytt:data", "data")

database_types = {"mysql": "mysql", "postgres": "postgres"}

def db_deploy_enabled():
  return data.values.scdf.deploy.database.enabled == True
end

def db_external_dataflow():
  x = {}
  if len(data.values.scdf.server.database.url) > 0:
    x.setdefault("url", data.values.scdf.server.database.url)
  end
  if len(data.values.scdf.server.database.username) > 0:
    x.setdefault("username", data.values.scdf.server.database.username)
  end
  if len(data.values.scdf.server.database.password) > 0:
    x.setdefault("password", data.values.scdf.server.database.password)
  end
  if len(data.values.scdf.server.database.driverClassName) > 0:
    x.setdefault("driverClassName", data.values.scdf.server.database.driverClassName)
  end
  if len(data.values.scdf.server.database.validationQuery) > 0:
    x.setdefault("validationQuery", data.values.scdf.server.database.validationQuery)
  end
  if data.values.scdf.server.database.testOnBorrow == True:
    x.setdefault("testOnBorrow", True)
  elif data.values.scdf.server.database.testOnBorrow == False:
    x.setdefault("testOnBorrow", False)
  end
  return x
end

def db_external_skipper():
  x = {}
  if len(data.values.scdf.skipper.database.url) > 0:
    x.setdefault("url", data.values.scdf.skipper.database.url)
  end
  if len(data.values.scdf.skipper.database.username) > 0:
    x.setdefault("username", data.values.scdf.skipper.database.username)
  end
  if len(data.values.scdf.skipper.database.password) > 0:
    x.setdefault("password", data.values.scdf.skipper.database.password)
  end
  if len(data.values.scdf.skipper.database.driverClassName) > 0:
    x.setdefault("driverClassName", data.values.scdf.skipper.database.driverClassName)
  end
  if len(data.values.scdf.skipper.database.validationQuery) > 0:
    x.setdefault("validationQuery", data.values.scdf.skipper.database.validationQuery)
  end
  if data.values.scdf.skipper.database.testOnBorrow == True:
    x.setdefault("testOnBorrow", True)
  elif data.values.scdf.skipper.database.testOnBorrow == False:
    x.setdefault("testOnBorrow", False)
  end
  return x
end

def mysql_enabled():
  return database_types.get(data.values.scdf.deploy.database.type) == "mysql" and db_deploy_enabled()
end

def postgres_enabled():
  return database_types.get(data.values.scdf.deploy.database.type) == "postgres" and db_deploy_enabled()
end

def db_postgres_username():
  return data.values.scdf.deploy.database.postgres.username;
end

def db_postgres_password():
  return data.values.scdf.deploy.database.postgres.password;
end

def db_mysql_username():
  return data.values.scdf.deploy.database.mysql.username;
end

def db_mysql_password():
  return data.values.scdf.deploy.database.mysql.password;
end
