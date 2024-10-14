package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpenSearchInfo {

    @JsonProperty("seq")
    private Long seq;

    @JsonProperty("url")
    private String url;

    @JsonProperty("index_name")
    private String indexName;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpenSearchInfo that = (OpenSearchInfo) o;

        if (!Objects.equals(url, that.url)) return false;
        if (!Objects.equals(indexName, that.indexName)) return false;
        if (!Objects.equals(username, that.username)) return false;
        return Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, indexName, username, password);
    }

    public OpenSearchInfo(String setting) {
        setting = setting.replaceAll(" ", "");

        this.url = parseRegex(setting, "(urls=\\[\".*\")\\]", "(urls=)|\\[|\\]|\"");
        this.indexName = parseRegex(setting, "(index_name=\".*\")", "(index_name=)|\\[|\\]|\"");
        this.username = parseRegex(setting, "(username=\".*\")", "(username=)|\\[|\\]|\"");
        this.password = parseRegex(setting, "(password=\".*\")", "(password=)|\\[|\\]|\"");
    }

    private String parseRegex(String origin, String findRegex, String replaceRegex) {
        Pattern p = Pattern.compile(findRegex);
        Matcher m = p.matcher(origin);

        if (m.find()) {
            String findStr = m.group();
            return findStr.replaceAll(replaceRegex, "");
        }

        return "";
    }
}
