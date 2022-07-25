load("@ytt:data", "data")

def name():
  return data.values.name
end

def username():
  return data.values.username
end

def password():
  return data.values.password
end
