package mcmp.mc.observability.mco11yagent.monitoring.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mcmp.mc.observability.mco11yagent.monitoring.mapper.OpenSearchMapper;
import mcmp.mc.observability.mco11yagent.monitoring.model.LogsInfo;
import mcmp.mc.observability.mco11yagent.monitoring.model.OpenSearchInfo;
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
import org.opensearch.search.sort.SortOrder;
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
public class OpenSearchService {

    private final OpenSearchMapper opensearchMapper;
    private final MonitoringConfigService monitoringConfigService;

    public ResBody<List<OpenSearchInfo>> getList() {
        List<MonitoringConfigInfo> storageInfoList = monitoringConfigService.list(null, null, null);
        storageInfoList = storageInfoList.stream().filter(f -> f.getPluginName().equalsIgnoreCase("opensearch")).collect(Collectors.toList());

        List<OpenSearchInfo> opensearchInfoList = new ArrayList<>();
        for (MonitoringConfigInfo hostStorageInfo : storageInfoList) {
            opensearchInfoList.add(new OpenSearchInfo(hostStorageInfo.getPluginConfig()));
        }

        syncSummaryOpenSearchList(opensearchInfoList.stream().distinct().collect(Collectors.toList()));

        ResBody<List<OpenSearchInfo>> res = new ResBody<>();
        res.setData(opensearchMapper.getOpenSearchInfoList());

        return res;
    }

    private void syncSummaryOpenSearchList(List<OpenSearchInfo> opensearchInfoList) {
        Map<Long, OpenSearchInfo> summaryOpenSearchInfoList = opensearchMapper.getOpenSearchInfoMap();
        List<OpenSearchInfo> newList = new ArrayList<>();
        List<Long> delList = new ArrayList<>();

        for( OpenSearchInfo info : opensearchInfoList ) {
            Optional<Map.Entry<Long, OpenSearchInfo>> findEntry = summaryOpenSearchInfoList.entrySet().stream().filter(a -> a.getValue().hashCode() == info.hashCode()).findAny();
            if (findEntry.isPresent()) {
                summaryOpenSearchInfoList.remove(findEntry.get().getKey());
            } else {
                newList.add(info);
            }
        }

        summaryOpenSearchInfoList.forEach((key, value) -> delList.add(key));

        if( !newList.isEmpty() ) opensearchMapper.insertOpenSearchInfoList(newList);
        if( !delList.isEmpty() ) opensearchMapper.deleteOpenSearchInfoList(delList);
    }

    public List<Map<String, Object>> getLogs(LogsInfo logsInfo) {
        OpenSearchInfo opensearchInfo = opensearchMapper.getOpenSearchInfoList().get(0);

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

            searchSourceBuilder.sort("@timestamp", SortOrder.DESC);

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