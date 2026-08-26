/*
 * Copyright 2026 youngmonkeys.org
 *
 * Licensed under the ezyplatform, Version 1.0.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://youngmonkeys.org/licenses/ezyplatform-1.0.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.youngmonkeys.ezyvector.web.test.controller.api;

import com.tvd12.ezyhttp.server.core.annotation.Controller;
import com.tvd12.ezyhttp.server.core.annotation.DoGet;
import com.tvd12.ezyhttp.server.core.annotation.DoPost;
import com.tvd12.ezyhttp.server.core.annotation.RequestBody;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.youngmonkeys.ezyvector.hnsw.HnswIndex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Controller("/test/hnsw")
public class TestHnswApiController {

    private static volatile HnswIndex index = new HnswIndex(
        HnswIndex.DEFAULT_MAX_M,
        HnswIndex.DEFAULT_EF_CONSTRUCTION
    );
    private static volatile int currentMaxM = HnswIndex.DEFAULT_MAX_M;
    private static volatile int currentEfConstruction =
        HnswIndex.DEFAULT_EF_CONSTRUCTION;
    private static final Map<Long, float[]> POINTS_BY_ID =
        new ConcurrentHashMap<>();
    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    @DoPost("/reset")
    public TestHnswGraphResponse reset(
        @RequestBody TestHnswResetRequest request
    ) {
        int maxM = request.getMaxM() != null && request.getMaxM() > 1
            ? request.getMaxM()
            : HnswIndex.DEFAULT_MAX_M;
        int efConstruction =
            request.getEfConstruction() != null
                && request.getEfConstruction() > 0
                ? request.getEfConstruction()
                : HnswIndex.DEFAULT_EF_CONSTRUCTION;
        currentMaxM = maxM;
        currentEfConstruction = efConstruction;
        index = new HnswIndex(maxM, efConstruction);
        POINTS_BY_ID.clear();
        NEXT_ID.set(1);
        return buildGraphSnapshot();
    }

    @DoGet("/graph")
    public TestHnswGraphResponse graph() {
        return buildGraphSnapshot();
    }

    @DoPost("/insert")
    public TestHnswInsertResponse insert(
        @RequestBody TestHnswInsertRequest request
    ) {
        long id = request.getId() != null
            ? request.getId()
            : NEXT_ID.getAndIncrement();
        NEXT_ID.updateAndGet(v -> Math.max(v, id + 1));
        float[] vector = new float[]{request.getX(), request.getY()};
        index.insert(id, vector);
        POINTS_BY_ID.put(id, vector);
        return TestHnswInsertResponse.builder()
            .id(id)
            .level(index.getNodeLevel(id))
            .newEntryPoint(Objects.equals(index.getEntryPointId(), id))
            .graph(buildGraphSnapshot())
            .build();
    }

    @DoPost("/remove")
    public TestHnswGraphResponse remove(
        @RequestBody TestHnswRemoveRequest request
    ) {
        index.remove(request.getId());
        POINTS_BY_ID.remove(request.getId());
        return buildGraphSnapshot();
    }

    @DoPost("/search")
    public TestHnswSearchResponse search(
        @RequestBody TestHnswSearchRequest request
    ) {
        int k = request.getK() != null && request.getK() > 0
            ? request.getK()
            : 5;
        int ef = request.getEf() != null && request.getEf() > 0
            ? request.getEf()
            : Math.max(50, k);
        float[] query = new float[]{request.getX(), request.getY()};
        HnswIndex.SearchTrace trace = index.searchWithTrace(query, k, ef);
        List<TestHnswSearchStepDto> steps =
            new ArrayList<>(trace.getSteps().size());
        for (HnswIndex.SearchStep step : trace.getSteps()) {
            float[] p = POINTS_BY_ID.get(step.getNodeId());
            steps.add(
                TestHnswSearchStepDto.builder()
                    .layer(step.getLayer())
                    .nodeId(step.getNodeId())
                    .x(p == null ? 0f : p[0])
                    .y(p == null ? 0f : p[1])
                    .distance(step.getDistance())
                    .visitedOnly(step.isVisitedOnly())
                    .build()
            );
        }
        List<TestHnswSearchResultDto> results =
            new ArrayList<>(trace.getResults().size());
        for (HnswIndex.SearchResult result : trace.getResults()) {
            float[] p = POINTS_BY_ID.get(result.getId());
            results.add(
                TestHnswSearchResultDto.builder()
                    .id(result.getId())
                    .x(p == null ? 0f : p[0])
                    .y(p == null ? 0f : p[1])
                    .score(result.getScore())
                    .build()
            );
        }
        return TestHnswSearchResponse.builder()
            .k(k)
            .ef(ef)
            .queryX(request.getX())
            .queryY(request.getY())
            .steps(steps)
            .results(results)
            .build();
    }

    private TestHnswGraphResponse buildGraphSnapshot() {
        HnswIndex idx = index;
        Set<Long> ids = idx.getNodeIds();
        List<TestHnswNodeDto> nodes = new ArrayList<>(ids.size());
        for (Long id : ids) {
            float[] p = POINTS_BY_ID.get(id);
            if (p == null) {
                continue;
            }
            nodes.add(
                TestHnswNodeDto.builder()
                    .id(id)
                    .x(p[0])
                    .y(p[1])
                    .level(idx.getNodeLevel(id))
                    .build()
            );
        }
        Set<String> seen = new HashSet<>();
        List<TestHnswEdgeDto> edges = new ArrayList<>();
        for (Long id : ids) {
            int level = idx.getNodeLevel(id);
            for (int layer = 0; layer <= level; ++layer) {
                for (Long neighborId : idx.getNeighborIds(id, layer)) {
                    if (!ids.contains(neighborId)) {
                        continue;
                    }
                    long a = Math.min(id, neighborId);
                    long b = Math.max(id, neighborId);
                    String key = layer + ":" + a + ":" + b;
                    if (seen.add(key)) {
                        edges.add(
                            TestHnswEdgeDto.builder()
                                .layer(layer)
                                .from(a)
                                .to(b)
                                .build()
                        );
                    }
                }
            }
        }
        return TestHnswGraphResponse.builder()
            .maxM(currentMaxM)
            .efConstruction(currentEfConstruction)
            .maxLevel(idx.getMaxLevel())
            .entryPointId(idx.getEntryPointId())
            .nodes(nodes)
            .edges(edges)
            .build();
    }

    @Getter
    @Setter
    public static class TestHnswResetRequest {
        private Integer maxM;
        private Integer efConstruction;
    }

    @Getter
    @Setter
    public static class TestHnswInsertRequest {
        private Long id;
        private float x;
        private float y;
    }

    @Getter
    @Setter
    public static class TestHnswRemoveRequest {
        private long id;
    }

    @Getter
    @Setter
    public static class TestHnswSearchRequest {
        private float x;
        private float y;
        private Integer k;
        private Integer ef;
    }

    @Getter
    @Builder
    public static class TestHnswNodeDto {
        private long id;
        private float x;
        private float y;
        private int level;
    }

    @Getter
    @Builder
    public static class TestHnswEdgeDto {
        private int layer;
        private long from;
        private long to;
    }

    @Getter
    @Builder
    public static class TestHnswGraphResponse {
        private int maxM;
        private int efConstruction;
        private int maxLevel;
        private Long entryPointId;
        private List<TestHnswNodeDto> nodes;
        private List<TestHnswEdgeDto> edges;
    }

    @Getter
    @Builder
    public static class TestHnswInsertResponse {
        private long id;
        private int level;
        private boolean newEntryPoint;
        private TestHnswGraphResponse graph;
    }

    @Getter
    @Builder
    public static class TestHnswSearchStepDto {
        private int layer;
        private long nodeId;
        private float x;
        private float y;
        private float distance;
        private boolean visitedOnly;
    }

    @Getter
    @Builder
    public static class TestHnswSearchResultDto {
        private long id;
        private float x;
        private float y;
        private float score;
    }

    @Getter
    @Builder
    public static class TestHnswSearchResponse {
        private int k;
        private int ef;
        private float queryX;
        private float queryY;
        private List<TestHnswSearchStepDto> steps;
        private List<TestHnswSearchResultDto> results;
    }
}
