package mcmp.mc.observability.mco11yagent.monitoring.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.OpensearchMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.LogsInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.OpensearchInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.MonitoringConfigInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.dto.ResBody;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpensearchService {

    private final OpensearchMapper opensearchMapper;
    private final MonitoringConfigService monitoringConfigService;

    public ResBody<List<OpensearchInfo>> getList() {
        List<MonitoringConfigInfo> storageInfoList = monitoringConfigService.list(null, null, null);
        storageInfoList = storageInfoList.stream().filter(f -> f.getPluginName().equalsIgnoreCase("opensearch")).collect(Collectors.toList());

        List<OpensearchInfo> opensearchInfoList = new ArrayList<>();
        for (MonitoringConfigInfo hostStorageInfo : storageInfoList) {
            opensearchInfoList.add(new OpensearchInfo(hostStorageInfo.getPluginConfig()));
        }

        syncSummaryOpensearchList(opensearchInfoList.stream().distinct().collect(Collectors.toList()));

        ResBody<List<OpensearchInfo>> res = new ResBody<>();
        res.setData(opensearchMapper.getOpensearchInfoList());

        return res;
    }

    private void syncSummaryOpensearchList(List<OpensearchInfo> opensearchInfoList) {
        Map<Long, OpensearchInfo> summaryOpensearchInfoList = opensearchMapper.getOpensearchInfoMap();
        List<OpensearchInfo> newList = new ArrayList<>();
        List<Long> delList = new ArrayList<>();

        for( OpensearchInfo info : opensearchInfoList ) {
            Optional<Map.Entry<Long, OpensearchInfo>> findEntry = summaryOpensearchInfoList.entrySet().stream().filter(a -> a.getValue().hashCode() == info.hashCode()).findAny();
            if (findEntry.isPresent()) {
                summaryOpensearchInfoList.remove(findEntry.get().getKey());
            } else {
                newList.add(info);
            }
        }

        summaryOpensearchInfoList.forEach((key, value) -> delList.add(key));

        if( !newList.isEmpty() ) opensearchMapper.insertOpensearchInfoList(newList);
        if( !delList.isEmpty() ) opensearchMapper.deleteOpensearchInfoList(delList);
    }

    public List<Map<String, Object>> getLogs(LogsInfo logsInfo) {
        OpensearchInfo opensearchInfo = opensearchMapper.getOpensearchInfo(logsInfo.getOpensearchSeq());

        List<Map<String, Object>> result;

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(opensearchInfo.getUsername(), opensearchInfo.getPassword()));

        try (RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(HttpHost.create(opensearchInfo.getUrl())).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)))) {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if( logsInfo.getConditions() != null ) logsInfo.getConditions().forEach(f -> boolQueryBuilder.must(QueryBuilders.matchQuery(f.getKey(), f.getValue())));

            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("@timestamp")
                    .gte("now-" + logsInfo.getRange())
                    .lte("now");
            boolQueryBuilder.filter(rangeQueryBuilder);

            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQueryBuilder);
            if( logsInfo.getLimit() != null ) searchSourceBuilder.size(logsInfo.getLimit().intValue());

            SearchRequest searchRequest = new SearchRequest(opensearchInfo.getIndexName());
            searchRequest.source(searchSourceBuilder);

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            JsonArray ja = new JsonArray();
            searchResponse.getHits().forEach(f -> ja.add(JsonParser.parseString(f.getSourceAsString()).getAsJsonObject()));

            result = new Gson().fromJson(ja.toString(), new TypeToken<List<Map<String, Object>>>(){}.getType());

            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
