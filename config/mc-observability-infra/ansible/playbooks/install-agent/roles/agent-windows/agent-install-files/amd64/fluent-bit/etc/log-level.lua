local function isempty(s)
   return s == nil or s == ""
end

-- Windows Event Log Level to text mapping
-- 0: LogAlways (Audit Success/Failure)
-- 1: Critical
-- 2: Error
-- 3: Warning
-- 4: Informational
-- 5: Verbose
local function windows_level_to_text(level_num)
  local level_map = {
    [0] = "AUDIT",
    [1] = "CRITICAL",
    [2] = "ERROR",
    [3] = "WARNING",
    [4] = "INFO",
    [5] = "DEBUG"
  }
  return level_map[level_num] or "UNKNOWN"
end

function modify_log_level(tag, timestamp, record)
  -- Check if this is a Windows Event Log record
  if record["Level"] ~= nil and record["Computer"] ~= nil then
    -- Convert Windows Event Log to Linux syslog-like format
    local new_record = {}

    -- host: Computer
    new_record["host"] = record["Computer"] or ""

    -- time: TimeCreated
    new_record["time"] = record["TimeCreated"] or ""

    -- service: ProviderName (use Channel as fallback)
    new_record["service"] = record["ProviderName"] or record["Channel"] or "windows"

    -- level: Level (numeric to text)
    local level_num = tonumber(record["Level"])
    if level_num ~= nil then
      new_record["level"] = windows_level_to_text(level_num)
    else
      new_record["level"] = "UNKNOWN"
    end

    -- filename: already set by modify filter
    new_record["filename"] = record["filename"] or ""

    -- source: already set by modify filter (use Channel as fallback)
    new_record["source"] = record["source"] or record["Channel"] or "windows"

    -- message: Message
    new_record["message"] = record["Message"] or ""

    -- pid: ProcessID
    new_record["pid"] = tostring(record["ProcessID"] or "")

    -- event_id: EventID (Windows specific, useful for filtering)
    new_record["event_id"] = tostring(record["EventID"] or "")

    return 2, 0, new_record
  end

  -- Fallback for non-Windows Event Log records
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
