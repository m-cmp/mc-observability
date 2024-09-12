package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpensearchInfo {
    private Long seq;
    private String url;
    private String indexName;
    private String username;
    private String password;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OpensearchInfo that = (OpensearchInfo) o;

        if (!Objects.equals(url, that.url)) return false;
        if (!Objects.equals(indexName, that.indexName)) return false;
        if (!Objects.equals(username, that.username)) return false;
        return Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, indexName, username, password);
    }
    public OpensearchInfo(String setting) {
        setting = setting.replaceAll(" ", "");

        this.url = parseRegex(setting, "(urls=\\[\".*\")\\]", "(urls=)|\\[|\\]|\"");
        this.indexName = parseRegex(setting, "(index_name=\".*\")", "(index_name=)|\\[|\\]|\"");
        this.username = parseRegex(setting, "(username=\".*\")", "(username=)|\\[|\\]|\"");
        this.password = parseRegex(setting, "(password=\".*\")", "(password=)|\\[|\\]|\"");
    }

    private String parseRegex(String origin, String findRegex, String replaceRegex) {
        Pattern p = Pattern.compile(findRegex);
        Matcher m = p.matcher(origin);

        if( m.find() ) {
            String findStr = m.group();
            return findStr.replaceAll(replaceRegex, "");
        }

        return "";
    }
}
