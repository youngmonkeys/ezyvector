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

package org.youngmonkeys.ezyvector.service;

import com.tvd12.ezyfox.util.EzyLoggable;
import com.tvd12.ezyfox.util.EzyNext;
import lombok.AllArgsConstructor;
import org.youngmonkeys.ezyplatform.manager.FileSystemManager;
import org.youngmonkeys.ezyvector.converter.EzyVectorEntityToModelConverter;
import org.youngmonkeys.ezyvector.converter.EzyVectorModelToEntityConverter;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollection;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionPoint;
import org.youngmonkeys.ezyvector.entity.EzyVectorCollectionSegment;
import org.youngmonkeys.ezyvector.hnsw.HnswIndex;
import org.youngmonkeys.ezyvector.model.EzyVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorCollectionModel;
import org.youngmonkeys.ezyvector.model.SaveVectorPointModel;
import org.youngmonkeys.ezyvector.model.EzyVectorSearchResultModel;
import org.youngmonkeys.ezyvector.repo.RagCollectionPointRepository;
import org.youngmonkeys.ezyvector.repo.RagCollectionRepository;
import org.youngmonkeys.ezyvector.repo.RagCollectionSegmentRepository;
import org.youngmonkeys.ezyvector.storage.EzyVectorFileStorage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public class EzyVectorService extends EzyLoggable {

    private final FileSystemManager fileSystemManager;
    private final EzyVectorSettingService ezyVectorSettingService;
    private final RagCollectionRepository collectionRepository;
    private final RagCollectionPointRepository collectionPointRepository;
    private final RagCollectionSegmentRepository collectionSegmentRepository;
    private final EzyVectorEntityToModelConverter entityToModelConverter;
    private final EzyVectorModelToEntityConverter modelToEntityConverter;
    private final Object writeLock = new Object();
    private final Set<Long> backfillingCollectionIds =
        ConcurrentHashMap.newKeySet();
    private final Map<Long, HnswIndex> hnswIndexByCollectionId =
        new ConcurrentHashMap<>();
    private final Set<Long> readyHnswCollectionIds =
        ConcurrentHashMap.newKeySet();
    private final Set<Long> buildingHnswCollectionIds =
        ConcurrentHashMap.newKeySet();

    public EzyVectorCollectionModel createCollectionIfAbsent(
        String collectionName,
        SaveVectorCollectionModel model
    ) throws Exception {
        EzyVectorCollection entity = collectionRepository
            .findByName(collectionName);
        if (entity == null) {
            entity = modelToEntityConverter
                .toVectorCollectionEntity(collectionName, model);
            collectionRepository.save(entity);
        }
        ensureMutableSegment(collectionName);
        startBackfillIfNecessary(collectionName);
        startHnswBuildIfNecessary(collectionName);
        return EzyVectorCollectionModel.builder()
            .vectorSize(entity.getVectorSize())
            .build();
    }

    public void upsert(
        List<SaveVectorPointModel> points
    ) throws Exception {
        EzyVectorCollection collection = getCollectionOrThrow();
        ensureMutableSegment(collection);
        startBackfillIfNecessary(collection);
        startHnswBuildIfNecessary(collection);
        EzyVectorFileStorage storage = newVectorFileStorage();
        long collectionId = collection.getId();
        synchronized (writeLock) {
            List<EzyVectorFileStorage.VectorRecord> records =
                new ArrayList<>(points.size());
            for (SaveVectorPointModel point : points) {
                EzyVectorCollectionPoint entity = collectionPointRepository
                    .findByCollectionIdAndPointId(
                        collectionId,
                        point.getId()
                    );
                if (entity == null) {
                    entity = modelToEntityConverter
                        .toVectorCollectionPointEntity(
                            collectionId,
                            point
                        );
                } else {
                    modelToEntityConverter.mergeToCollectionPointEntity(
                        point,
                        entity
                    );
                }
                collectionPointRepository.save(entity);
                records.add(
                    new EzyVectorFileStorage.VectorRecord(
                        entity.getId(),
                        point.getId(),
                        point.getVector()
                    )
                );
            }
            storage.upsertAll(
                collection.getId(),
                collection.getVectorSize(),
                records
            );
            updateHnswIndex(collection, records);
        }
        startHnswBuildIfNecessary(collection);
    }

    public List<EzyVectorSearchResultModel> search(
        float[] vector,
        int limit
    ) throws Exception {
        EzyVectorCollection collection = getCollectionOrThrow();
        startBackfillIfNecessary(collection);
        startHnswBuildIfNecessary(collection);
        HnswIndex hnswIndex = getReadyHnswIndex(collection);
        if (hnswIndex != null) {
            List<HnswIndex.SearchResult> hits = hnswIndex.search(
                vector,
                limit,
                Math.max(limit * 8, 64)
            );
            return toSearchResults(collection, hits);
        }
        List<EzyVectorFileStorage.SearchResult> hits =
            newVectorFileStorage().search(
                collection.getId(),
                collection.getVectorSize(),
                vector,
                limit
            );
        return toExactSearchResults(collection, hits);
    }

    private List<EzyVectorSearchResultModel> toSearchResults(
        EzyVectorCollection collection,
        List<HnswIndex.SearchResult> hits
    ) throws Exception {
        List<EzyVectorSearchResultModel> results =
            new ArrayList<>(hits.size());
        for (HnswIndex.SearchResult hit : hits) {
            EzyVectorCollectionPoint point = collectionPointRepository
                .findByCollectionIdAndPointId(
                    collection.getId(),
                    hit.getId()
                );
            results.add(
                EzyVectorSearchResultModel.builder()
                    .chunkId(hit.getId())
                    .score(hit.getScore())
                    .payload(
                        point == null
                            ? null
                            : toPayloadMap(point.getPayload())
                    )
                    .build()
            );
        }
        return results;
    }

    private List<EzyVectorSearchResultModel> toExactSearchResults(
        EzyVectorCollection collection,
        List<EzyVectorFileStorage.SearchResult> hits
    ) throws Exception {
        List<EzyVectorSearchResultModel> results =
            new ArrayList<>(hits.size());
        for (EzyVectorFileStorage.SearchResult hit : hits) {
            EzyVectorCollectionPoint point = collectionPointRepository
                .findByCollectionIdAndPointId(
                    collection.getId(),
                    hit.getId()
                );
            results.add(
                entityToModelConverter.toSearchResultModel(
                    hit,
                    point
                )
            );
        }
        return results;
    }

    private EzyVectorCollection getCollectionOrThrow(
        String collectionName
    ) {
        EzyVectorCollection collection = collectionRepository
            .findByName(collectionName);
        if (collection == null) {
            throw new IllegalStateException(
                "You need to setup MySQL vector database first"
            );
        }
        return collection;
    }

    private void ensureMutableSegment(
        String collectionName
    ) {
        EzyVectorCollection collection = collectionRepository
            .findByName(collectionName);
        if (collection != null) {
            ensureMutableSegment(collection);
        }
    }

    private void ensureMutableSegment(
        EzyVectorCollection collection
    ) {
        EzyVectorCollectionSegment segment = collectionSegmentRepository
            .findByCollectionIdAndSegmentNo(collection.getId(), 1L);
        if (segment != null) {
            return;
        }
        segment = new EzyVectorCollectionSegment();
        segment.setCollectionId(collection.getId());
        segment.setSegmentNo(1L);
        segment.setSegmentType("MUTABLE");
        segment.setStatus("ACTIVE");
        segment.setIndexVersion(1L);
        LocalDateTime now = LocalDateTime.now();
        segment.setCreatedAt(now);
        segment.setUpdatedAt(now);
        collectionSegmentRepository.save(segment);
    }

    private void startBackfillIfNecessary(
        String collectionName
    ) throws Exception {
        EzyVectorCollection collection = collectionRepository
            .findByName(collectionName);
        if (collection != null) {
            startBackfillIfNecessary(collection);
        }
    }

    private void startBackfillIfNecessary(
        EzyVectorCollection collection
    ) throws Exception {
        EzyVectorFileStorage storage = newVectorFileStorage();
        long backfillProgress =
            storage.getBackfillProgress(collection.getId());
        List<EzyVectorCollectionPoint> points = collectionPointRepository
            .findListByCollectionIdAndIdGreaterThan(
                collection.getId(),
                backfillProgress,
                EzyNext.fromLimit(1)
            );
        if (points.isEmpty()
            || !backfillingCollectionIds.add(collection.getId())) {
            return;
        }
        Thread thread = new Thread(
            () -> backfillCollection(collection),
            "ezyvector-vector-backfill-" + collection.getId()
        );
        thread.setDaemon(true);
        thread.start();
    }

    private void backfillCollection(EzyVectorCollection collection) {
        try {
            EzyVectorFileStorage storage = newVectorFileStorage();
            long lastId = storage.getBackfillProgress(collection.getId());
            while (true) {
                List<EzyVectorCollectionPoint> points = collectionPointRepository
                    .findListByCollectionIdAndIdGreaterThan(
                        collection.getId(),
                        lastId,
                        EzyNext.fromLimit(500)
                    );
                if (points.isEmpty()) {
                    return;
                }
                synchronized (writeLock) {
                    List<EzyVectorFileStorage.VectorRecord> records =
                        new ArrayList<>(points.size());
                    for (EzyVectorCollectionPoint point : points) {
                        records.add(
                            new EzyVectorFileStorage.VectorRecord(
                                point.getId(),
                                point.getPointId(),
                                point.getVector()
                            )
                        );
                        lastId = point.getId();
                    }
                    storage.upsertAll(
                        collection.getId(),
                        collection.getVectorSize(),
                        records
                    );
                }
                storage.saveBackfillProgress(collection.getId(), lastId);
            }
        } catch (Exception e) {
            logger.warn(
                "backfill vector collection: {} failed",
                collection.getId(),
                e
            );
        } finally {
            backfillingCollectionIds.remove(collection.getId());
        }
    }

    private void startHnswBuildIfNecessary(
        String collectionName
    ) throws Exception {
        EzyVectorCollection collection = collectionRepository
            .findByName(collectionName);
        if (collection != null) {
            startHnswBuildIfNecessary(collection);
        }
    }

    private void startHnswBuildIfNecessary(
        EzyVectorCollection collection
    ) {
        long collectionId = collection.getId();
        if (readyHnswCollectionIds.contains(collectionId)) {
            return;
        }
        EzyVectorFileStorage storage = newVectorFileStorage();
        if (storage.isHnswPresent(collectionId)) {
            try {
                hnswIndexByCollectionId.put(
                    collectionId,
                    HnswIndex.load(storage.getHnswPath(collectionId))
                );
                readyHnswCollectionIds.add(collectionId);
                return;
            } catch (Exception e) {
                logger.warn(
                    "load hnsw index for vector collection: {} failed",
                    collectionId,
                    e
                );
            }
        }
        if (!buildingHnswCollectionIds.add(collectionId)) {
            return;
        }
        hnswIndexByCollectionId.put(collectionId, new HnswIndex());
        Thread thread = new Thread(
            () -> buildHnswIndex(collection),
            "ezyvector-vector-hnsw-build-" + collectionId
        );
        thread.setDaemon(true);
        thread.start();
    }

    private HnswIndex getReadyHnswIndex(
        EzyVectorCollection collection
    ) {
        long collectionId = collection.getId();
        if (!readyHnswCollectionIds.contains(collectionId)) {
            return null;
        }
        HnswIndex index = hnswIndexByCollectionId.get(collectionId);
        if (index != null) {
            return index;
        }
        EzyVectorFileStorage storage = newVectorFileStorage();
        if (!storage.isHnswPresent(collectionId)) {
            return null;
        }
        try {
            index = HnswIndex.load(storage.getHnswPath(collectionId));
            hnswIndexByCollectionId.put(collectionId, index);
            return index;
        } catch (Exception e) {
            readyHnswCollectionIds.remove(collectionId);
            logger.warn(
                "load hnsw index for vector collection: {} failed",
                collectionId,
                e
            );
            startHnswBuildIfNecessary(collection);
            return null;
        }
    }

    private void updateHnswIndex(
        EzyVectorCollection collection,
        List<EzyVectorFileStorage.VectorRecord> records
    ) throws Exception {
        HnswIndex index = hnswIndexByCollectionId.get(collection.getId());
        if (index == null) {
            return;
        }
        for (EzyVectorFileStorage.VectorRecord record : records) {
            index.insert(record.getPointId(), record.getVector());
        }
        if (readyHnswCollectionIds.contains(collection.getId())) {
            index.save(newVectorFileStorage().getHnswPath(collection.getId()));
        }
    }

    private void buildHnswIndex(EzyVectorCollection collection) {
        try {
            HnswIndex index = hnswIndexByCollectionId.get(collection.getId());
            if (index == null) {
                index = new HnswIndex();
                hnswIndexByCollectionId.put(collection.getId(), index);
            }
            long lastId = 0L;
            while (true) {
                List<EzyVectorCollectionPoint> points = collectionPointRepository
                    .findListByCollectionIdAndIdGreaterThan(
                        collection.getId(),
                        lastId,
                        EzyNext.fromLimit(500)
                    );
                if (points.isEmpty()) {
                    break;
                }
                for (EzyVectorCollectionPoint point : points) {
                    index.insert(point.getPointId(), point.getVector());
                    lastId = point.getId();
                }
            }
            index.save(newVectorFileStorage().getHnswPath(collection.getId()));
            readyHnswCollectionIds.add(collection.getId());
        } catch (Exception e) {
            logger.warn(
                "build hnsw index for vector collection: {} failed",
                collection.getId(),
                e
            );
        } finally {
            buildingHnswCollectionIds.remove(collection.getId());
        }
    }

    private EzyVectorFileStorage newVectorFileStorage() {
        return new EzyVectorFileStorage(
            fileSystemManager,
            ezyVectorSettingService.getVectorDataDir()
        );
    }
}
