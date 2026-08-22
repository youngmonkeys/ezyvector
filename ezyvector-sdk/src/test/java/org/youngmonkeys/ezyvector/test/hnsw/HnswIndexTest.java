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

package org.youngmonkeys.ezyvector.test.hnsw;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.youngmonkeys.ezyvector.hnsw.HnswIndex;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.youngmonkeys.ezyvector.hnsw.HnswIndex.SearchResult;

public class HnswIndexTest {

    private static final float SCORE_TOLERANCE = 1e-3f;

    @Test
    public void shouldReturnEmptyListWhenSearchEmptyIndex() {
        HnswIndex index = new HnswIndex();

        List<SearchResult> results = index.search(vector(1f, 0f), 5, 64);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void shouldReturnSizeZeroWhenIndexIsEmpty() {
        HnswIndex index = new HnswIndex();

        Assert.assertEquals(index.size(), 0);
    }

    @Test
    public void shouldNoopWhenRemoveFromEmptyIndex() {
        HnswIndex index = new HnswIndex();

        index.remove(1L);

        Assert.assertEquals(index.size(), 0);
    }

    @Test
    public void shouldPersistAndLoadEmptyIndex() throws IOException {
        HnswIndex index = new HnswIndex();
        Path file = tempFile("empty-index.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(loaded.size(), 0);
        Assert.assertTrue(loaded.search(vector(1f, 0f), 5, 64).isEmpty());
    }

    @Test
    public void shouldHaveSizeOneAfterInsertOnePoint() {
        HnswIndex index = new HnswIndex();

        index.insert(1L, vector(1f, 0f));

        Assert.assertEquals(index.size(), 1);
    }

    @Test
    public void shouldReturnPointIdWhenSearchSameVector() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getId(), 1L);
        Assert.assertEquals(results.get(0).getScore(), 1f, SCORE_TOLERANCE);
    }

    @Test
    public void shouldReturnOnlyPointWhenSearchDifferentVectorWithSinglePoint() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(0f, 1f), 5, 64);

        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getId(), 1L);
    }

    @Test
    public void shouldReturnEmptyAfterDeleteOnlyPoint() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.remove(1L);

        Assert.assertEquals(index.size(), 0);
        Assert.assertTrue(index.search(vector(1f, 0f), 5, 64).isEmpty());
    }

    @Test
    public void shouldFindAllPointsAfterInsertMultiplePoints() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        index.insert(3L, vector(1f, 1f));

        Assert.assertEquals(index.size(), 3);
        assertContainsPoint(1L, index.search(vector(1f, 0f), 3, 64));
        assertContainsPoint(2L, index.search(vector(0f, 1f), 3, 64));
        assertContainsPoint(3L, index.search(vector(1f, 1f), 3, 64));
    }

    @Test
    public void shouldReturnCorrectExternalIdForNonSequentialPointIds() {
        HnswIndex index = new HnswIndex();
        index.insert(10L, vector(1f, 0f));
        index.insert(1000L, vector(0f, 1f));
        index.insert(999999L, vector(1f, 1f));

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);

        Assert.assertEquals(results.get(0).getId(), 10L);
    }

    @Test
    public void shouldAllowPointIdZero() {
        HnswIndex index = new HnswIndex();

        index.insert(0L, vector(1f, 0f));

        assertContainsPoint(0L, index.search(vector(1f, 0f), 1, 64));
    }

    @Test
    public void shouldAllowPointIdLongMaxValue() {
        HnswIndex index = new HnswIndex();

        index.insert(Long.MAX_VALUE, vector(1f, 0f));

        assertContainsPoint(
            Long.MAX_VALUE,
            index.search(vector(1f, 0f), 1, 64)
        );
    }

    @Test
    public void shouldReplaceVectorWhenInsertDuplicatePointId() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.insert(1L, vector(0f, 1f));

        Assert.assertEquals(index.size(), 1);
        List<SearchResult> results = index.search(vector(0f, 1f), 1, 64);
        Assert.assertEquals(results.get(0).getId(), 1L);
        Assert.assertEquals(results.get(0).getScore(), 1f, SCORE_TOLERANCE);
    }

    @Test
    public void shouldRankExactVectorFirst() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.9f, 0.1f));
        index.insert(3L, vector(0f, 1f));

        List<SearchResult> results = index.search(vector(1f, 0f), 3, 128);

        Assert.assertEquals(results.get(0).getId(), 1L);
    }

    @Test
    public void shouldReturnTopKInDescendingScoreOrder() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(123456789L), 50, 8);

        List<SearchResult> results = index.search(
            randomVector(new Random(42L), 8),
            10,
            128
        );

        assertSortedByScoreDescending(results);
    }

    @Test
    public void shouldReturnOneResultWhenLimitIsOne() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);

        Assert.assertEquals(results.size(), 1);
    }

    @Test
    public void shouldReturnLimitResultsWhenLimitLessThanSize() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 4);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            5,
            64
        );

        Assert.assertEquals(results.size(), 5);
    }

    @Test
    public void shouldReturnAllResultsWhenLimitEqualsSize() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 10, 4);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            10,
            128
        );

        Assert.assertEquals(results.size(), 10);
    }

    @Test
    public void shouldReturnAllResultsWhenLimitGreaterThanSize() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 5, 4);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            100,
            128
        );

        Assert.assertEquals(results.size(), 5);
    }

    @Test
    public void shouldReturnEmptyListWhenLimitIsZero() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), 0, 64);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void shouldReturnEmptyListWhenLimitIsNegative() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), -1, 64);

        Assert.assertTrue(results.isEmpty());
    }

    @Test
    public void shouldNormalizeEfSearchWhenLessThanLimit() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 4);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            10,
            1
        );

        Assert.assertEquals(results.size(), 10);
    }

    @Test
    public void shouldNotCorruptIndexWithLargeEfSearch() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 4);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            5,
            100000
        );

        Assert.assertEquals(results.size(), 5);
        Assert.assertEquals(index.size(), 20);
    }

    @Test
    public void shouldReturnSameResultsOnRepeatedSearch() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 50, 8);
        float[] query = randomVector(new Random(2L), 8);

        List<SearchResult> first = index.search(query, 10, 128);
        List<SearchResult> second = index.search(query, 10, 128);

        Assert.assertEquals(idsOf(first), idsOf(second));
    }

    @Test
    public void shouldReturnBothPointsWhenVectorsAreDuplicate() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), 2, 64);

        assertContainsPoint(1L, results);
        assertContainsPoint(2L, results);
    }

    @Test
    public void shouldGiveEqualScoreForDuplicateVectors() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), 2, 64);

        Assert.assertEquals(
            results.get(0).getScore(),
            results.get(1).getScore(),
            SCORE_TOLERANCE
        );
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenInsertNullVector() {
        new HnswIndex().insert(1L, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenInsertEmptyVector() {
        new HnswIndex().insert(1L, new float[0]);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenInsertVectorDimensionMismatch() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.insert(2L, vector(1f, 0f, 0f));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenSearchVectorDimensionMismatch() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.search(vector(1f, 0f, 0f), 1, 64);
    }

    @Test
    public void shouldNotProduceNaNOrInfiniteScoreForZeroVector() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(0f, 0f));
        index.insert(2L, vector(1f, 0f));

        List<SearchResult> results = index.search(vector(1f, 0f), 2, 64);

        for (SearchResult result : results) {
            Assert.assertFalse(Float.isNaN(result.getScore()));
            Assert.assertFalse(Float.isInfinite(result.getScore()));
        }
    }

    @Test
    public void shouldHandleVerySmallAndVeryLargeFloats() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1e-30f, 0f));
        index.insert(2L, vector(1e30f, 1e29f));
        index.insert(3L, vector(-1e20f, 1e20f));

        List<SearchResult> results = index.search(vector(1f, 0f), 3, 64);

        Assert.assertEquals(results.size(), 3);
        for (SearchResult result : results) {
            Assert.assertFalse(Float.isNaN(result.getScore()));
            Assert.assertTrue(result.getScore() >= -1f - SCORE_TOLERANCE);
            Assert.assertTrue(result.getScore() <= 1f + SCORE_TOLERANCE);
        }
    }

    @Test
    public void shouldNotReturnDeletedPointInSearch() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));

        index.remove(1L);

        assertDoesNotContainPoint(1L, index.search(vector(1f, 0f), 5, 64));
    }

    @Test
    public void shouldNoopWhenDeleteNonexistentPoint() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.remove(999L);

        Assert.assertEquals(index.size(), 1);
    }

    @Test
    public void shouldBeIdempotentWhenDeleteTwice() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.remove(1L);
        index.remove(1L);

        Assert.assertEquals(index.size(), 0);
    }

    @Test
    public void shouldReturnNextNearestPointWhenDeleteNearestPoint() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.9f, 0.1f));
        index.insert(3L, vector(0f, 1f));

        index.remove(1L);

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);
        Assert.assertEquals(results.get(0).getId(), 2L);
    }

    @Test
    public void shouldStillSearchAfterDeleteEntryPoint() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 4);
        long entryPointId = idOf(entryPointOf(index));

        index.remove(entryPointId);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 4),
            5,
            64
        );
        Assert.assertEquals(results.size(), 5);
        assertDoesNotContainPoint(entryPointId, results);
    }

    @Test
    public void shouldReturnEmptyAndNotCorruptAfterDeleteAllPoints() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 4);

        for (long id = 0; id < 20; ++id) {
            index.remove(id);
        }

        Assert.assertEquals(index.size(), 0);
        Assert.assertTrue(
            index.search(randomVector(new Random(2L), 4), 5, 64).isEmpty()
        );
    }

    @Test
    public void shouldHaveValidEntryPointWhenIndexNonEmpty() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        Object entryPoint = entryPointOf(index);

        Assert.assertNotNull(entryPoint);
        Assert.assertFalse(deletedOf(entryPoint));
    }

    @Test
    public void shouldHaveEntryPointReferencingNodeInGraph() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        Object entryPoint = entryPointOf(index);
        Map<Long, Object> nodes = nodesOf(index);

        Assert.assertSame(nodes.get(idOf(entryPoint)), entryPoint);
    }

    @Test
    public void shouldHaveMaxLevelEqualToEntryPointLevel() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);

        int maxLevel = maxLevelOf(index);
        Object entryPoint = entryPointOf(index);

        Assert.assertEquals(maxLevel, levelOf(entryPoint));
    }

    @Test
    public void shouldNotHaveSelfEdgeInAnyLevel() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        for (Object node : nodesOf(index).values()) {
            long id = idOf(node);
            for (List<Long> neighbors : neighborsByLevelOf(node)) {
                Assert.assertFalse(neighbors.contains(id));
            }
        }
    }

    @Test
    public void shouldNotHaveDuplicateNeighborInSameLevel() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        for (Object node : nodesOf(index).values()) {
            for (List<Long> neighbors : neighborsByLevelOf(node)) {
                Set<Long> unique = new HashSet<>(neighbors);
                Assert.assertEquals(unique.size(), neighbors.size());
            }
        }
    }

    @Test
    public void shouldRespectNeighborCountLimit() throws Exception {
        int maxM = 8;
        HnswIndex index = buildIndex(maxM, 100);
        insertRandomPoints(index, new Random(1L), 150, 8);

        for (Object node : nodesOf(index).values()) {
            List<List<Long>> neighborsByLevel = neighborsByLevelOf(node);
            for (int level = 0; level < neighborsByLevel.size(); ++level) {
                int limit = level == 0 ? maxM * 2 : maxM;
                Assert.assertTrue(
                    neighborsByLevel.get(level).size() <= limit
                );
            }
        }
    }

    @Test
    public void shouldHaveEveryNeighborReferenceExistInGraph() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        Map<Long, Object> nodes = nodesOf(index);
        for (Object node : nodes.values()) {
            for (List<Long> neighbors : neighborsByLevelOf(node)) {
                for (long neighborId : neighbors) {
                    Assert.assertTrue(nodes.containsKey(neighborId));
                }
            }
        }
    }

    @Test
    public void shouldRemainTraversableAfterManyInserts() {
        HnswIndex index = buildIndex(16, 200);
        Random random = new Random(1L);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            random,
            300,
            8
        );

        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            List<SearchResult> results = index.search(
                entry.getValue(),
                1,
                300
            );
            Assert.assertEquals(results.get(0).getId(), entry.getKey());
        }
    }

    @Test
    public void shouldNeverGenerateNegativeLevel() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);

        for (Object node : nodesOf(index).values()) {
            Assert.assertTrue(levelOf(node) >= 0);
        }
    }

    @Test
    public void shouldHaveFewerNodesAtHigherLevelStatistically() throws Exception {
        HnswIndex index = buildIndex(16, 50);
        insertRandomPoints(index, new Random(1L), 1000, 4);

        int level0Count = 0;
        int aboveLevel0Count = 0;
        for (Object node : nodesOf(index).values()) {
            if (levelOf(node) == 0) {
                ++level0Count;
            } else {
                ++aboveLevel0Count;
            }
        }

        Assert.assertTrue(level0Count > aboveLevel0Count);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenMaxMIsZero() {
        new HnswIndex(0, 200);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenMaxMIsNegative() {
        new HnswIndex(-1, 200);
    }

    @Test
    public void shouldBuildValidIndexWithSmallM() {
        HnswIndex index = new HnswIndex(2, 50);
        insertRandomPoints(index, new Random(1L), 30, 4);

        Assert.assertEquals(index.size(), 30);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenEfConstructionIsZero() {
        new HnswIndex(16, 0);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldThrowWhenEfConstructionIsNegative() {
        new HnswIndex(16, -10);
    }

    @Test
    public void shouldBuildSuccessfullyWithHigherEfConstruction() {
        HnswIndex index = new HnswIndex(16, 400);
        insertRandomPoints(index, new Random(1L), 20, 4);

        Assert.assertEquals(index.size(), 20);
    }

    @Test
    public void shouldUseDefaultConfigWithNoArgsConstructor() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        assertContainsPoint(1L, index.search(vector(1f, 0f), 1, 64));
    }

    @Test
    public void shouldPersistAndLoadSinglePointIndex() throws IOException {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        Path file = tempFile("single-point.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(loaded.size(), 1);
        assertContainsPoint(1L, loaded.search(vector(1f, 0f), 1, 64));
    }

    @Test
    public void shouldPersistAndLoadManyPointsIndex() throws IOException {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            100,
            8
        );
        Path file = tempFile("many-points.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(loaded.size(), 100);
        for (long id : vectors.keySet()) {
            assertContainsPoint(id, loaded.search(vectors.get(id), 1, 200));
        }
    }

    @Test
    public void shouldReturnSameSearchResultsBeforeAndAfterRestart()
        throws IOException {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);
        float[] query = randomVector(new Random(2L), 8);
        List<SearchResult> before = index.search(query, 10, 128);
        Path file = tempFile("restart.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);
        List<SearchResult> after = loaded.search(query, 10, 128);

        Assert.assertEquals(idsOf(after), idsOf(before));
    }

    @Test
    public void shouldPersistEntryPointAndMaxLevel() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);
        Path file = tempFile("entry-point.dat");
        long entryPointIdBefore = idOf(entryPointOf(index));
        int maxLevelBefore = maxLevelOf(index);

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(idOf(entryPointOf(loaded)), entryPointIdBefore);
        Assert.assertEquals(maxLevelOf(loaded), maxLevelBefore);
    }

    @Test
    public void shouldPersistTombstoneAcrossRestart() throws IOException {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        index.remove(1L);
        Path file = tempFile("tombstone.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(loaded.size(), 1);
        assertDoesNotContainPoint(1L, loaded.search(vector(1f, 0f), 5, 64));
        assertContainsPoint(2L, loaded.search(vector(0f, 1f), 5, 64));
    }

    @Test
    public void shouldPersistNeighborLists() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);
        Path file = tempFile("neighbors.dat");
        Map<Long, List<List<Long>>> before = new HashMap<>();
        for (Object node : nodesOf(index).values()) {
            before.put(idOf(node), copyNeighbors(neighborsByLevelOf(node)));
        }

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        for (Object node : nodesOf(loaded).values()) {
            Assert.assertEquals(
                copyNeighbors(neighborsByLevelOf(node)),
                before.get(idOf(node))
            );
        }
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldThrowWhenLoadingFileWithInvalidMagicHeader()
        throws IOException {
        Path file = tempFile("invalid-magic.dat");
        try (OutputStream out = Files.newOutputStream(file)) {
            out.write(new byte[]{0, 1, 2, 3, 4, 5, 6, 7});
        }

        HnswIndex.load(file);
    }

    @Test(expectedExceptions = IOException.class)
    public void shouldThrowWhenLoadingUnsupportedFormatVersion()
        throws IOException {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        Path validFile = tempFile("valid.dat");
        index.save(validFile);
        byte[] bytes = Files.readAllBytes(validFile);
        bytes[7] = (byte) 0xFF;
        Path corruptedFile = tempFile("wrong-version.dat");
        Files.write(corruptedFile, bytes);

        HnswIndex.load(corruptedFile);
    }

    @Test
    public void shouldHandleMultipleConcurrentSearches() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicBoolean failed = new AtomicBoolean(false);
        try {
            for (int i = 0; i < threadCount; ++i) {
                int seed = i;
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; ++j) {
                            index.search(
                                randomVector(new Random(seed * 1000L + j), 8),
                                10,
                                64
                            );
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            Assert.assertTrue(latch.await(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        Assert.assertFalse(failed.get());
    }

    @Test
    public void shouldHandleConcurrentSearchWhileInsert() throws Exception {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);
        AtomicBoolean stop = new AtomicBoolean(false);
        AtomicBoolean failed = new AtomicBoolean(false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            executor.submit(() -> {
                Random random = new Random(2L);
                while (!stop.get()) {
                    try {
                        index.search(randomVector(random, 8), 10, 64);
                    } catch (Exception e) {
                        failed.set(true);
                    }
                }
            });
            executor.submit(() -> {
                Random random = new Random(3L);
                for (long id = 1000; id < 1200; ++id) {
                    try {
                        index.insert(id, randomVector(random, 8));
                    } catch (Exception e) {
                        failed.set(true);
                    }
                }
                stop.set(true);
            });
            executor.shutdown();
            Assert.assertTrue(
                executor.awaitTermination(30, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
        Assert.assertFalse(failed.get());
        Assert.assertEquals(index.size(), 300);
    }

    @Test
    public void shouldHandleConcurrentInsertsOfDifferentPoints()
        throws Exception {
        HnswIndex index = buildIndex(16, 200);
        int threadCount = 4;
        int perThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger idGenerator = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);
        try {
            for (int t = 0; t < threadCount; ++t) {
                executor.submit(() -> {
                    Random random = new Random();
                    try {
                        for (int j = 0; j < perThread; ++j) {
                            long id = idGenerator.getAndIncrement();
                            index.insert(id, randomVector(random, 8));
                        }
                    } catch (Exception e) {
                        failed.set(true);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            Assert.assertTrue(latch.await(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
        Assert.assertFalse(failed.get());
        Assert.assertEquals(index.size(), threadCount * perThread);
    }

    @Test
    public void shouldMatchExactVectorSearchForTop1() {
        HnswIndex index = buildIndex(16, 200);
        Random random = new Random(1L);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            random,
            200,
            8
        );

        for (int i = 0; i < 20; ++i) {
            float[] query = randomVector(new Random(1000L + i), 8);
            long exactTop1 = exactTopK(vectors, query, 1).get(0);
            List<SearchResult> hnswResults = index.search(query, 1, 200);

            Assert.assertEquals(hnswResults.get(0).getId(), exactTop1);
        }
    }

    @Test
    public void shouldHaveHighRecallAtTen() {
        HnswIndex index = buildIndex(16, 200);
        Random random = new Random(1L);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            random,
            500,
            16
        );

        double totalRecall = 0;
        int trials = 20;
        for (int i = 0; i < trials; ++i) {
            float[] query = randomVector(new Random(5000L + i), 16);
            List<Long> exactTopK = exactTopK(vectors, query, 10);
            List<SearchResult> hnswResults = index.search(query, 10, 200);

            totalRecall += recall(exactTopK, hnswResults);
        }
        double averageRecall = totalRecall / trials;

        Assert.assertTrue(
            averageRecall >= 0.8,
            "expected recall@10 >= 0.8 but was " + averageRecall
        );
    }

    @Test
    public void shouldMatchScoreWithDirectlyComputedCosineSimilarity() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(3f, 4f));

        List<SearchResult> results = index.search(vector(3f, 4f), 1, 64);

        Assert.assertEquals(results.get(0).getScore(), 1f, SCORE_TOLERANCE);
    }

    @Test
    public void shouldFindEachInsertedVectorAsExactSelfMatch() {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(7L),
            100,
            8
        );

        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            List<SearchResult> results = index.search(
                entry.getValue(),
                1,
                200
            );
            Assert.assertEquals(results.get(0).getId(), entry.getKey());
        }
    }

    @Test
    public void shouldProduceSameFinalStateWhenReplayingSameOperations() {
        HnswIndex first = buildIndex(16, 200);
        HnswIndex second = buildIndex(16, 200);
        Map<Long, float[]> vectors = generateRandomVectors(
            new Random(9L),
            50,
            8
        );

        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            first.insert(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            second.insert(entry.getKey(), entry.getValue());
        }

        Assert.assertEquals(first.size(), second.size());
        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            List<SearchResult> firstResult =
                first.search(entry.getValue(), 1, 200);
            List<SearchResult> secondResult =
                second.search(entry.getValue(), 1, 200);
            Assert.assertEquals(
                firstResult.get(0).getId(),
                secondResult.get(0).getId()
            );
        }
    }

    private static float[] vector(float... values) {
        return values;
    }

    private static HnswIndex buildIndex(int maxM, int efConstruction) {
        return new HnswIndex(maxM, efConstruction);
    }

    private static float[] randomVector(Random random, int dimension) {
        float[] result = new float[dimension];
        for (int i = 0; i < dimension; ++i) {
            result[i] = random.nextFloat() * 2f - 1f;
        }
        return result;
    }

    private static Map<Long, float[]> generateRandomVectors(
        Random random,
        int count,
        int dimension
    ) {
        Map<Long, float[]> vectors = new HashMap<>();
        for (long id = 0; id < count; ++id) {
            vectors.put(id, randomVector(random, dimension));
        }
        return vectors;
    }

    private static Map<Long, float[]> insertRandomPoints(
        HnswIndex index,
        Random random,
        int count,
        int dimension
    ) {
        Map<Long, float[]> vectors = generateRandomVectors(
            random,
            count,
            dimension
        );
        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            index.insert(entry.getKey(), entry.getValue());
        }
        return vectors;
    }

    private static List<Long> exactTopK(
        Map<Long, float[]> vectors,
        float[] query,
        int k
    ) {
        float[] normalizedQuery = normalize(query);
        List<Map.Entry<Long, Float>> scored = new ArrayList<>();
        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            float score = dot(normalizedQuery, normalize(entry.getValue()));
            scored.add(new java.util.AbstractMap.SimpleEntry<>(
                entry.getKey(),
                score
            ));
        }
        scored.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        List<Long> result = new ArrayList<>(k);
        for (int i = 0; i < Math.min(k, scored.size()); ++i) {
            result.add(scored.get(i).getKey());
        }
        return result;
    }

    private static double recall(
        List<Long> exactTopK,
        List<SearchResult> hnswResults
    ) {
        Set<Long> exactSet = new HashSet<>(exactTopK);
        long hits = 0;
        for (SearchResult result : hnswResults) {
            if (exactSet.contains(result.getId())) {
                ++hits;
            }
        }
        return (double) hits / exactTopK.size();
    }

    private static float[] normalize(float[] vector) {
        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm == 0f) {
            return new float[vector.length];
        }
        float[] result = new float[vector.length];
        for (int i = 0; i < vector.length; ++i) {
            result[i] = vector[i] / norm;
        }
        return result;
    }

    private static float dot(float[] a, float[] b) {
        float result = 0f;
        for (int i = 0; i < a.length; ++i) {
            result += a[i] * b[i];
        }
        return result;
    }

    private static Path tempFile(String name) throws IOException {
        Path dir = Files.createTempDirectory("hnsw-index-test");
        dir.toFile().deleteOnExit();
        Path file = dir.resolve(name);
        file.toFile().deleteOnExit();
        return file;
    }

    private static List<Long> idsOf(List<SearchResult> results) {
        List<Long> ids = new ArrayList<>(results.size());
        for (SearchResult result : results) {
            ids.add(result.getId());
        }
        return ids;
    }

    private static void assertContainsPoint(
        long pointId,
        List<SearchResult> results
    ) {
        Assert.assertTrue(idsOf(results).contains(pointId));
    }

    private static void assertDoesNotContainPoint(
        long pointId,
        List<SearchResult> results
    ) {
        Assert.assertFalse(idsOf(results).contains(pointId));
    }

    private static void assertSortedByScoreDescending(
        List<SearchResult> results
    ) {
        for (int i = 1; i < results.size(); ++i) {
            Assert.assertTrue(
                results.get(i - 1).getScore() >= results.get(i).getScore()
            );
        }
    }

    private static List<List<Long>> copyNeighbors(
        List<List<Long>> neighborsByLevel
    ) {
        List<List<Long>> copy = new ArrayList<>(neighborsByLevel.size());
        for (List<Long> neighbors : neighborsByLevel) {
            copy.add(new ArrayList<>(neighbors));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Object> nodesOf(HnswIndex index)
        throws Exception {
        return (Map<Long, Object>) getField(index, "nodesById");
    }

    private static Object entryPointOf(HnswIndex index) throws Exception {
        return getField(index, "entryPoint");
    }

    private static int maxLevelOf(HnswIndex index) throws Exception {
        return (int) getField(index, "maxLevel");
    }

    private static long idOf(Object node) throws Exception {
        return (long) getField(node, "id");
    }

    private static int levelOf(Object node) throws Exception {
        return (int) getField(node, "level");
    }

    private static boolean deletedOf(Object node) throws Exception {
        return (boolean) getField(node, "deleted");
    }

    @SuppressWarnings("unchecked")
    private static List<List<Long>> neighborsByLevelOf(Object node)
        throws Exception {
        return (List<List<Long>>) getField(node, "neighborsByLevel");
    }

    private static Object getField(
        Object target,
        String name
    ) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
