local function isempty(s)
   return s == nil or s == ""
end

function modify_log_level(tag, timestamp, record)
  if isempty(record["level"]) then
    record["level"] = "UNKNOWN"
    
    return 2, 0, record
  end

  if string.find(string.lower(record["level"]), "deb") then
    record["level"] = "DEBUG"
  elseif string.find(string.lower(record["level"]), "inf") then
    record["level"] = "INFO"
  elseif string.find(string.lower(record["level"]), "not") then
    record["level"] = "NOTICE"
  elseif string.find(string.lower(record["level"]), "war") then
    record["level"] = "WARNING"
  elseif string.find(string.lower(record["level"]), "err") then
    record["level"] = "ERROR"
  else
    record["level"] = "UNKNOWN"
  end

  -- return code, timestamp, record
  return 2, 0, record
end

