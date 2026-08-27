package com.mcmp.o11ymanager.manager.service.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.mcmp.o11ymanager.manager.dto.influx.MetricDTO;
import com.mcmp.o11ymanager.manager.dto.influx.MetricRequestDTO;
import com.mcmp.o11ymanager.manager.service.influx.ChunkedMetricQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ChunkedMetricQueryTest {

    private static final long NOW = 1_700_000_000L; // fixed clock (not step-aligned on purpose)

    private static MetricRequestDTO aggregated(String range, String step, Long limit) {
        MetricRequestDTO req = new MetricRequestDTO();
        req.setMeasurement("cpu");
        req.setRange(range);
        req.setGroupTime(step);
        req.setLimit(limit);
        MetricRequestDTO.FieldInfo f = new MetricRequestDTO.FieldInfo();
        f.setFunction("mean");
        f.setField("usage_active");
        req.setFields(new ArrayList<>(List.of(f)));
        return req;
    }

    private static ChunkedMetricQuery.Plan plan(MetricRequestDTO req) {
        return ChunkedMetricQuery.plan(req, NOW, 12, 48, 900, 60);
    }

    @Test
    void slicesCoverTheWholeWindowWithoutGapsOrOverlap() {
        ChunkedMetricQuery.Plan p = plan(aggregated("1h", "1m", null));

        assertThat(p.applicable()).isTrue();
        List<ChunkedMetricQuery.Slice> slices = p.slices();

        // the window ends at the last completed bucket, not at "now"
        assertThat(p.alignedEndSeconds() % 60).isZero();
        assertThat(p.alignedEndSeconds()).isLessThanOrEqualTo(NOW);
        assertThat(NOW - p.alignedEndSeconds()).isLessThan(60);
        assertThat(p.startSeconds()).isEqualTo(p.alignedEndSeconds() - 3600);

        assertThat(slices.get(0).startNanos()).isEqualTo(p.startSeconds() * 1_000_000_000L);
        assertThat(slices.get(slices.size() - 1).endNanos())
                .isEqualTo(p.alignedEndSeconds() * 1_000_000_000L);

        for (int i = 1; i < slices.size(); i++) {
            assertThat(slices.get(i).startNanos())
                    .as("slice %d must start exactly where the previous one ended", i)
                    .isEqualTo(slices.get(i - 1).endNanos());
        }
    }

    @Test
    void sliceBoundariesAreWholeMultiplesOfTheAggregationStep() {
        ChunkedMetricQuery.Plan p = plan(aggregated("6h", "5m", null));
        long stepNanos = 300L * 1_000_000_000L;

        for (ChunkedMetricQuery.Slice s : p.slices()) {
            assertThat(s.startNanos() % stepNanos)
                    .as("a bucket must never straddle a slice boundary")
                    .isZero();
            assertThat(s.endNanos() % stepNanos).isZero();
        }
    }

    @Test
    void theMostRecentSliceIsNeverCached() {
        ChunkedMetricQuery.Plan p = plan(aggregated("1h", "1m", null));
        List<ChunkedMetricQuery.Slice> slices = p.slices();

        assertThat(slices.get(slices.size() - 1).cacheable())
                .as("the slice touching now is still filling")
                .isFalse();
        assertThat(slices.stream().anyMatch(ChunkedMetricQuery.Slice::cacheable))
                .as("older slices should be reusable")
                .isTrue();
    }

    @Test
    void partialEdgeSlicesAreNotCached() {
        ChunkedMetricQuery.Plan p = plan(aggregated("1h", "1m", null));
        for (ChunkedMetricQuery.Slice s : p.slices()) {
            if (s.cacheable()) {
                assertThat(s.startNanos() / 1_000_000_000L)
                        .as("only whole slices may be cached")
                        .isEqualTo(s.chunkStartSeconds());
            }
        }
    }

    @Test
    void chunkingIsRefusedForRawQueriesWithoutAnAggregate() {
        MetricRequestDTO raw = aggregated("1h", "1m", null);
        raw.getFields().get(0).setFunction(null); // raw projection → no GROUP BY time()

        assertThat(plan(raw).applicable()).isFalse();
    }

    @Test
    void chunkingIsRefusedWhenALimitCouldTruncate() {
        // 1h / 1m = 60 points; a limit of 10 would be applied per slice, not overall
        assertThat(plan(aggregated("1h", "1m", 10L)).applicable()).isFalse();
        // a limit larger than the point count cannot truncate, so slicing stays safe
        assertThat(plan(aggregated("1h", "1m", 2000L)).applicable()).isTrue();
    }

    @Test
    void chunkingIsRefusedForShortRanges() {
        assertThat(plan(aggregated("5m", "1m", null)).applicable()).isFalse();
    }

    @Test
    void sliceCountStaysWithinTheCap() {
        ChunkedMetricQuery.Plan p =
                ChunkedMetricQuery.plan(aggregated("7d", "1m", null), NOW, 12, 48, 900, 60);
        assertThat(p.slices().size()).isLessThanOrEqualTo(49);
    }

    @Test
    void mergeKeepsNewestFirstOrderAcrossSlices() {
        // each slice is already newest-first; oldest slice first in the input list
        List<MetricDTO> oldest =
                List.of(series("cpu", Map.of("node_id", "vm-1"), row("t3"), row("t2")));
        List<MetricDTO> newest =
                List.of(series("cpu", Map.of("node_id", "vm-1"), row("t5"), row("t4")));

        List<MetricDTO> merged = ChunkedMetricQuery.merge(List.of(oldest, newest), null);

        assertThat(merged).hasSize(1);
        assertThat(merged.get(0).values())
                .containsExactly(row("t5"), row("t4"), row("t3"), row("t2"));
    }

    @Test
    void mergeKeepsSeriesWithDifferentTagsApart() {
        List<MetricDTO> slice =
                List.of(
                        series("cpu", Map.of("node_id", "vm-1"), row("t1")),
                        series("cpu", Map.of("node_id", "vm-2"), row("t1")));

        List<MetricDTO> merged = ChunkedMetricQuery.merge(List.of(slice), null);

        assertThat(merged).hasSize(2);
        assertThat(merged.stream().map(m -> m.tags().get("node_id")))
                .containsExactlyInAnyOrder("vm-1", "vm-2");
    }

    @Test
    void mergeAppliesTheLimitToTheCombinedResult() {
        List<MetricDTO> oldest = List.of(series("cpu", Map.of(), row("t1"), row("t2")));
        List<MetricDTO> newest = List.of(series("cpu", Map.of(), row("t4"), row("t3")));

        List<MetricDTO> merged = ChunkedMetricQuery.merge(List.of(oldest, newest), 3L);

        assertThat(merged.get(0).values()).containsExactly(row("t4"), row("t3"), row("t1"));
    }

    private static MetricDTO series(String name, Map<String, String> tags, List<Object>... rows) {
        return new MetricDTO(name, List.of("timestamp", "value"), tags, List.of(rows));
    }

    private static List<Object> row(String ts) {
        return List.of(ts, 1.0);
    }
}
