package mcmp.mc.observability.agent.monitoring.mapper.handler;

import mcmp.mc.observability.agent.monitoring.model.InfluxDBInfo;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

import java.util.HashMap;
import java.util.Map;

public class InfluxDBInfoHandler implements ResultHandler<Map<String, Object>> {

	private final Map<Long, InfluxDBInfo> result = new HashMap<>();

	public Map<Long, InfluxDBInfo> getResult() {
		return result;
	}

	@Override
	public void handleResult(ResultContext<? extends Map<String, Object>> resultContext) {
		result.put(Long.parseLong(resultContext.getResultObject().get("SEQ").toString())
				, InfluxDBInfo.builder()
					.url(resultContext.getResultObject().get("URL").toString())
					.database(resultContext.getResultObject().get("DATABASE").toString())
					.retentionPolicy(resultContext.getResultObject().get("RETENTION_POLICY").toString())
					.username(resultContext.getResultObject().get("USERNAME").toString())
					.password(resultContext.getResultObject().get("PASSWORD").toString())
					.build());
	}
}
