package mcmp.mc.observability.mco11yagent.monitoring.mapper.handler;

import mcmp.mc.observability.mco11yagent.monitoring.model.OpenSearchInfo;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.HashMap;
import java.util.Map;

public class OpenSearchInfoHandler implements ResultHandler<Map<String, Object>> {

	private final Map<Long, OpenSearchInfo> result = new HashMap<>();

	public Map<Long, OpenSearchInfo> getResult() {
		return result;
	}

	@Override
	public void handleResult(ResultContext<? extends Map<String, Object>> resultContext) {
		result.put(Long.parseLong(resultContext.getResultObject().get("SEQ").toString())
				, OpenSearchInfo.builder()
					.url(resultContext.getResultObject().get("URL").toString())
					.indexName(resultContext.getResultObject().get("INDEX_NAME").toString())
					.username(resultContext.getResultObject().get("USERNAME").toString())
					.password(resultContext.getResultObject().get("PASSWORD").toString())
					.build());
	}
}