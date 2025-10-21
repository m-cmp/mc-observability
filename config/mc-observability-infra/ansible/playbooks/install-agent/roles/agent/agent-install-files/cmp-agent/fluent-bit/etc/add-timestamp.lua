function add_timestamp(tag, timestamp, record)
    local time_offset = os.getenv("TIME_DIFF_SECONDS") or "+0"

    local value = tonumber(time_offset) or 0

    timestamp = os.time() + value
    
    return 1, timestamp, record
end
