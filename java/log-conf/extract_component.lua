function extract_component(tag, timestamp, record)
         if record["attributes"] and record["attributes"]["component"] then
             record["component"] = record["attributes"]["component"]
         end
         return 1, timestamp, record
     end
