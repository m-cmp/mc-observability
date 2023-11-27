package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.enums.StorageKind;

@Getter
@Setter
@ToString
public class HostStorageInfo {
    private Long seq = 0L;
    private Long hostSeq = 0L;
    @JsonIgnore
    private StorageKind kind = StorageKind.INFLUXDB_V1;
    private String name;
    @JsonIgnore
    private String info;
    private String url;
    private String database;
    private String retentionPolicy;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String username;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String password;
    private StateOption state;
    private StateYN monitoringYn;

    public void setInfo(String info) {
        JsonObject json = new Gson().fromJson(info, JsonElement.class).getAsJsonObject();
        this.setUrl(json.get("url").getAsString());
        this.setDatabase(json.get("database") == null? null: json.get("database").getAsString());
        this.setRetentionPolicy(json.get("retentionPolicy") == null? null: json.get("retentionPolicy").getAsString());
        this.setUsername(json.get("username") == null? null: json.get("username").getAsString());
        this.setPassword(json.get("password") == null? null: json.get("password").getAsString());
    }

    public String getInfo() {
        JsonObject json = new JsonObject();
        json.addProperty("url", this.getUrl());
        json.addProperty("database", this.getDatabase());
        json.addProperty("retentionPolicy", this.getRetentionPolicy());
        if( this.getUsername() != null ) json.addProperty("username", this.getUsername());
        if( this.getPassword() != null ) json.addProperty("password", this.getPassword());

        return json.toString();
    }
}