load("@ytt:data", "data")

database_types = {"mariadb": "mariadb", "postgres": "postgres"}

def db_external_dataflow():
  x = {}
  if len(data.values.scdf.server.database.url) > 0:
    x.setdefault("url", data.values.scdf.server.database.url)
  end
  if len(data.values.scdf.server.database.username) > 0:
    x.setdefault("username", "${external-user}")
  end
  if len(data.values.scdf.server.database.password) > 0:
    x.setdefault("password", "${external-password}")
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
    x.setdefault("username", "${external-user}")
  end
  if len(data.values.scdf.skipper.database.password) > 0:
    x.setdefault("password", "${external-password}")
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

def db_postgres_username():
  return data.values.scdf.deploy.database.postgres.username;
end

def db_postgres_password():
  return data.values.scdf.deploy.database.postgres.password;
end

def db_mariadb_username():
  return data.values.scdf.deploy.database.mariadb.username;
end

def db_mariadb_password():
  return data.values.scdf.deploy.database.mariadb.password;
end
