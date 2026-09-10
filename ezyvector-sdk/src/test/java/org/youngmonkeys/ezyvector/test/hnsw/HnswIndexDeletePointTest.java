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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.youngmonkeys.ezyvector.hnsw.HnswIndex.SearchResult;

public class HnswIndexDeletePointTest {

    private static final float SCORE_TOLERANCE = 1e-3f;

    @Test
    public void shouldDeletePointThatExistsTc01() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 8);

        index.remove(5L);

        Assert.assertFalse(index.getNodeIds().contains(5L));
        assertDoesNotContainPoint(
            5L,
            index.search(randomVector(new Random(2L), 8), 20, 200)
        );
    }

    @Test
    public void shouldNoopWhenDeletePointDoesNotExistTc02() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.remove(999L);

        Assert.assertEquals(index.size(), 1);
        assertContainsPoint(1L, index.search(vector(1f, 0f), 1, 64));
    }

    @Test
    public void shouldDeleteSamePointTwiceWithoutErrorTc03() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));

        index.remove(1L);
        index.remove(1L);

        Assert.assertEquals(index.size(), 1);
        assertDoesNotContainPoint(1L, index.search(vector(1f, 0f), 5, 64));
        assertContainsPoint(2L, index.search(vector(0f, 1f), 5, 64));
    }

    @Test
    public void shouldBecomeEmptyWhenDeleteOnlyPointTc04() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));

        index.remove(1L);

        Assert.assertEquals(index.size(), 0);
        Assert.assertTrue(index.search(vector(1f, 0f), 5, 64).isEmpty());
    }

    @Test
    public void shouldUpdateEntryPointWhenDeleteEntryPointTc05() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);
        Long entryPointId = index.getEntryPointId();

        index.remove(entryPointId);

        Long newEntryPointId = index.getEntryPointId();
        if (index.size() > 0) {
            Assert.assertNotNull(newEntryPointId);
            Assert.assertNotEquals(newEntryPointId, entryPointId);
            Assert.assertTrue(index.getNodeIds().contains(newEntryPointId));
        } else {
            Assert.assertNull(newEntryPointId);
        }
    }

    @Test
    public void shouldNotAffectSearchWhenDeletePointOnlyAtLayerZeroTc06() {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            50,
            8
        );
        long layerZeroId = findPointAtLevel(index, 0);

        index.remove(layerZeroId);

        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            if (entry.getKey() == layerZeroId) {
                continue;
            }
            assertContainsPoint(
                entry.getKey(),
                index.search(entry.getValue(), 1, 200)
            );
        }
    }

    @Test
    public void shouldDeletePointExistingInMultipleLayersTc07() {
        HnswIndex index = buildIndex(16, 50);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            200,
            8
        );
        long multiLayerId = findPointAtLevelAbove(index, 0);

        index.remove(multiLayerId);

        assertDoesNotContainPoint(
            multiLayerId,
            index.search(vectors.get(multiLayerId), 5, 200)
        );
        Assert.assertEquals(index.size(), vectors.size() - 1);
    }

    @Test
    public void shouldKeepGraphSearchableWhenDeletePointWithManyNeighborsTc08() {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            150,
            8
        );
        long busiestId = findPointWithMostNeighbors(index);

        index.remove(busiestId);

        for (Map.Entry<Long, float[]> entry : vectors.entrySet()) {
            if (entry.getKey() == busiestId) {
                continue;
            }
            assertContainsPoint(
                entry.getKey(),
                index.search(entry.getValue(), 1, 200)
            );
        }
    }

    @Test
    public void shouldKeepClusterSearchableWhenDeleteMiddlePointTc09() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(0f, 0f));
        index.insert(2L, vector(0.001f, 0f));
        index.insert(3L, vector(-0.001f, 0f));
        index.insert(4L, vector(0f, 0.001f));
        index.insert(5L, vector(0f, -0.001f));

        index.remove(1L);

        assertContainsPoint(2L, index.search(vector(0.001f, 0f), 1, 64));
        assertContainsPoint(3L, index.search(vector(-0.001f, 0f), 1, 64));
        assertContainsPoint(4L, index.search(vector(0f, 0.001f), 1, 64));
        assertContainsPoint(5L, index.search(vector(0f, -0.001f), 1, 64));
    }

    @Test
    public void shouldNotContainDeletedPointWhenSearchWithItsExactVectorTc10() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));

        index.remove(1L);

        assertDoesNotContainPoint(1L, index.search(vector(1f, 0f), 5, 64));
    }

    @Test
    public void shouldReturnNextNearestPointWhenDeleteNearestPointTc11() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.9f, 0.1f));
        index.insert(3L, vector(0f, 1f));

        index.remove(1L);

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);
        Assert.assertEquals(results.get(0).getId(), 2L);
    }

    @Test
    public void shouldReturnFullTopKAfterDeletingSomePointsTc12() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);
        for (long id = 0; id < 10; ++id) {
            index.remove(id);
        }

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            10,
            200
        );

        Assert.assertEquals(results.size(), 10);
        for (SearchResult result : results) {
            Assert.assertTrue(result.getId() >= 10);
        }
    }

    @Test
    public void shouldReturnOnlyRemainingPointsWhenKGreaterThanActiveSizeTc13() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 8);
        for (long id = 0; id < 15; ++id) {
            index.remove(id);
        }

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            100,
            200
        );

        Assert.assertEquals(results.size(), 5);
    }

    @Test
    public void shouldExcludeAllDeletedPointsAfterDeletingManyConsecutiveIdsTc14() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);
        Set<Long> deletedIds = new HashSet<>();
        for (long id = 0; id < 100; id += 2) {
            index.remove(id);
            deletedIds.add(id);
        }

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            100,
            300
        );

        for (SearchResult result : results) {
            Assert.assertFalse(deletedIds.contains(result.getId()));
        }
        Assert.assertEquals(index.size(), 100 - deletedIds.size());
    }

    @Test
    public void shouldBecomeEmptyWhenDeleteAllPointsOneByOneTc15() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 20, 8);

        for (long id = 0; id < 20; ++id) {
            index.remove(id);
        }

        Assert.assertEquals(index.size(), 0);
        Assert.assertTrue(
            index.search(randomVector(new Random(2L), 8), 5, 64).isEmpty()
        );
    }

    @Test
    public void shouldNotThrowWhenDeletingInInsertionOrderTc16() {
        HnswIndex index = buildIndex(16, 200);
        int total = 50;
        insertRandomPoints(index, new Random(1L), total, 8);

        for (long id = 0; id < total; ++id) {
            index.remove(id);
        }

        Assert.assertEquals(index.size(), 0);
    }

    @Test
    public void shouldNotThrowWhenDeletingInReverseInsertionOrderTc17() {
        HnswIndex index = buildIndex(16, 200);
        int total = 50;
        insertRandomPoints(index, new Random(1L), total, 8);

        for (long id = total - 1; id >= 0; --id) {
            index.remove(id);
        }

        Assert.assertEquals(index.size(), 0);
    }

    @Test
    public void shouldOnlyReturnActivePointsAfterRandomPartialDeletionTc18() {
        HnswIndex index = buildIndex(16, 200);
        int total = 200;
        insertRandomPoints(index, new Random(1L), total, 8);
        Random deleteRandom = new Random(3L);
        Set<Long> deletedIds = new HashSet<>();
        for (long id = 0; id < total; ++id) {
            if (deleteRandom.nextDouble() < 0.4) {
                index.remove(id);
                deletedIds.add(id);
            }
        }

        Assert.assertEquals(index.size(), total - deletedIds.size());
        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            total,
            300
        );
        for (SearchResult result : results) {
            Assert.assertFalse(deletedIds.contains(result.getId()));
        }
    }

    @Test
    public void shouldKeepOtherPointWhenDeleteOneOfDuplicateVectorsTc19() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(1f, 0f));

        index.remove(1L);

        Assert.assertEquals(index.size(), 1);
        List<SearchResult> results = index.search(vector(1f, 0f), 2, 64);
        assertContainsPoint(2L, results);
        assertDoesNotContainPoint(1L, results);
    }

    @Test
    public void shouldNotDeleteNearbyPointWhenDeletingSimilarPointTc20() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.999f, 0.001f));

        index.remove(1L);

        Assert.assertEquals(index.size(), 1);
        assertContainsPoint(2L, index.search(vector(0.999f, 0.001f), 1, 64));
    }

    @Test
    public void shouldOnlyHaveNewVersionWhenReinsertAfterDeleteWithNewVectorTc21() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.remove(1L);

        index.insert(1L, vector(0f, 1f));

        Assert.assertEquals(index.size(), 1);
        assertContainsPoint(1L, index.search(vector(0f, 1f), 1, 64));
        float[] currentVector = index.getVector(1L);
        Assert.assertEquals(currentVector[0], 0f, SCORE_TOLERANCE);
        Assert.assertEquals(currentVector[1], 1f, SCORE_TOLERANCE);
    }

    @Test
    public void shouldWorkNormallyWhenReinsertAfterDeleteWithSameVectorTc22() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        index.remove(1L);

        index.insert(1L, vector(1f, 0f));

        Assert.assertEquals(index.size(), 2);
        assertContainsPoint(1L, index.search(vector(1f, 0f), 1, 64));
    }

    @Test
    public void shouldNotAccumulateGhostStateAfterRepeatedAddDeleteCyclesTc23() {
        HnswIndex index = new HnswIndex();
        index.insert(2L, vector(0f, 1f));

        for (int i = 0; i < 20; ++i) {
            index.insert(1L, vector(1f, 0f));
            index.remove(1L);
        }

        Assert.assertEquals(index.size(), 1);
        Assert.assertFalse(index.getNodeIds().contains(1L));
        assertDoesNotContainPoint(1L, index.search(vector(1f, 0f), 5, 64));
        assertContainsPoint(2L, index.search(vector(0f, 1f), 5, 64));
    }

    @Test
    public void shouldDeleteSmallestPointIdTc24() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);

        index.remove(0L);

        Assert.assertEquals(index.size(), 29);
        assertDoesNotContainPoint(
            0L,
            index.search(randomVector(new Random(2L), 8), 30, 200)
        );
    }

    @Test
    public void shouldDeleteLargestPointIdTc25() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);

        index.remove(29L);

        Assert.assertEquals(index.size(), 29);
        assertDoesNotContainPoint(
            29L,
            index.search(randomVector(new Random(2L), 8), 30, 200)
        );
    }

    @Test
    public void shouldDeleteCorrectPointForNonSequentialIdsTc26() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(10L, vector(0.9f, 0.1f));
        index.insert(1000L, vector(0f, 1f));
        index.insert(999999L, vector(-1f, 0f));

        index.remove(1000L);

        Assert.assertEquals(index.size(), 3);
        List<SearchResult> results = index.search(vector(1f, 0f), 4, 64);
        assertContainsPoint(1L, results);
        assertContainsPoint(10L, results);
        assertContainsPoint(999999L, results);
        assertDoesNotContainPoint(1000L, results);
    }

    @Test
    public void shouldKeepRemainingPointAsEntryPointWhenIndexHasTwoPointsTc27() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));

        index.remove(1L);

        Assert.assertEquals(index.size(), 1);
        assertContainsPoint(2L, index.search(vector(0f, 1f), 1, 64));
        Assert.assertEquals(index.getEntryPointId(), Long.valueOf(2L));
    }

    @Test
    public void shouldKeepRemainingPointsSearchableWhenIndexHasThreePointsTc28() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.5f, 0.5f));
        index.insert(3L, vector(0f, 1f));

        index.remove(2L);

        Assert.assertEquals(index.size(), 2);
        assertContainsPoint(1L, index.search(vector(1f, 0f), 1, 64));
        assertContainsPoint(3L, index.search(vector(0f, 1f), 1, 64));
    }

    @Test
    public void shouldNotHaveDanglingNeighborReferencesAfterDeleteTc29() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 100, 8);

        for (long id = 0; id < 30; ++id) {
            index.remove(id);
        }

        Set<Long> activeIds = index.getNodeIds();
        for (long id : activeIds) {
            int level = index.getNodeLevel(id);
            for (int layer = 0; layer <= level; ++layer) {
                for (long neighborId : index.getNeighborIds(id, layer)) {
                    Assert.assertNotEquals(
                        index.getNodeLevel(neighborId),
                        -1
                    );
                }
            }
        }
    }

    @Test
    public void shouldCompleteSearchWithoutExceptionWhenPathMayPassThroughDeletedPointsTc30() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);

        for (long id = 0; id < 150; ++id) {
            index.remove(id);
        }

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            10,
            200
        );

        Assert.assertEquals(results.size(), 10);
        for (SearchResult result : results) {
            Assert.assertTrue(result.getId() >= 150);
        }
    }

    @Test
    public void shouldNotChangeNearestResultOrScoreAfterUnrelatedDeleteTc31() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 50, 8);
        float[] query = randomVector(new Random(2L), 8);
        SearchResult topBefore = index.search(query, 1, 200).get(0);
        long unrelatedId = topBefore.getId() == 0L ? 1L : 0L;

        index.remove(unrelatedId);

        SearchResult topAfter = index.search(query, 1, 200).get(0);
        Assert.assertEquals(topAfter.getId(), topBefore.getId());
        Assert.assertEquals(
            topAfter.getScore(),
            topBefore.getScore(),
            SCORE_TOLERANCE
        );
    }

    @Test
    public void shouldNotChangeVectorOfOtherPointsAfterDeleteTc32() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        float[] vectorBefore = index.getVector(2L);

        index.remove(1L);

        Assert.assertEquals(index.getVector(2L), vectorBefore);
    }

    @Test
    public void shouldHandleDeleteOnLargeIndexTc33() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 3000, 8);

        index.remove(1500L);

        Assert.assertEquals(index.size(), 2999);
        assertDoesNotContainPoint(
            1500L,
            index.search(randomVector(new Random(2L), 8), 10, 200)
        );
    }

    @Test
    public void shouldRemainStableAfterBulkDeleteAtIncreasingRatiosTc34() {
        int total = 1000;
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), total, 8);

        for (long id = 0; id < 100; ++id) {
            index.remove(id);
        }
        Assert.assertEquals(index.size(), total - 100);

        for (long id = 100; id < 500; ++id) {
            index.remove(id);
        }
        Assert.assertEquals(index.size(), total - 500);

        for (long id = 500; id < 900; ++id) {
            index.remove(id);
        }
        Assert.assertEquals(index.size(), total - 900);

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            10,
            200
        );
        for (SearchResult result : results) {
            Assert.assertTrue(result.getId() >= 900);
        }
    }

    @Test
    public void shouldNeverReturnDeletedPointsAcrossManySearchesTc35() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 200, 8);
        Set<Long> deletedIds = new HashSet<>();
        for (long id = 0; id < 60; ++id) {
            index.remove(id);
            deletedIds.add(id);
        }

        for (int i = 0; i < 200; ++i) {
            List<SearchResult> results = index.search(
                randomVector(new Random(1000L + i), 8),
                10,
                200
            );
            for (SearchResult result : results) {
                Assert.assertFalse(deletedIds.contains(result.getId()));
            }
        }
    }

    @Test
    public void shouldAlwaysHaveValidEntryPointWhenDeletingEntryPointRepeatedlyTc36() {
        HnswIndex index = buildIndex(16, 100);
        insertRandomPoints(index, new Random(1L), 100, 8);

        for (int i = 0; i < 10; ++i) {
            Long entryPointId = index.getEntryPointId();
            Assert.assertNotNull(entryPointId);

            index.remove(entryPointId);

            Long newEntryPointId = index.getEntryPointId();
            Assert.assertNotNull(newEntryPointId);
            Assert.assertNotEquals(newEntryPointId, entryPointId);
            Assert.assertTrue(index.getNodeIds().contains(newEntryPointId));
        }
    }

    @Test
    public void shouldLowerMaxLevelWhenDeletingOnlyPointAtHighestLevelTc37() {
        HnswIndex index = buildIndex(16, 50);
        insertRandomPoints(index, new Random(1L), 300, 8);
        Long entryPointId = index.getEntryPointId();
        int levelBefore = index.getNodeLevel(entryPointId);

        index.remove(entryPointId);

        int maxLevelAfter = index.getMaxLevel();
        Assert.assertTrue(maxLevelAfter <= levelBefore);
        for (long id : index.getNodeIds()) {
            Assert.assertTrue(index.getNodeLevel(id) <= maxLevelAfter);
        }
    }

    @Test
    public void shouldLowerHighestLayerWhenItsOnlyPointIsDeletedTc38() {
        HnswIndex index = buildIndex(16, 50);
        insertRandomPoints(index, new Random(1L), 300, 8);
        int maxLevelBefore = index.getMaxLevel();
        List<Long> topLevelIds = new ArrayList<>();
        for (long id : index.getNodeIds()) {
            if (index.getNodeLevel(id) == maxLevelBefore) {
                topLevelIds.add(id);
            }
        }

        index.remove(topLevelIds.get(0));

        if (topLevelIds.size() == 1) {
            Assert.assertTrue(index.getMaxLevel() < maxLevelBefore);
        } else {
            Assert.assertEquals(index.getMaxLevel(), maxLevelBefore);
        }
    }

    @Test
    public void shouldReflectDeletedPointAfterSaveTc39() throws IOException {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        index.remove(1L);
        Path file = tempFile("tc39-save-after-delete.dat");

        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertEquals(loaded.size(), 1);
        assertDoesNotContainPoint(1L, loaded.search(vector(1f, 0f), 5, 64));
    }

    @Test
    public void shouldNotReappearDeletedPointAfterLoadTc40() throws IOException {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0f, 1f));
        index.remove(1L);
        Path file = tempFile("tc40-load-after-delete.dat");
        index.save(file);

        HnswIndex loaded = HnswIndex.load(file);

        Assert.assertFalse(loaded.getNodeIds().contains(1L));
        assertContainsPoint(2L, loaded.search(vector(0f, 1f), 5, 64));
    }

    @Test
    public void shouldDeleteNormallyAfterLoadingIndexTc41() throws IOException {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            50,
            8
        );
        Path file = tempFile("tc41-delete-after-load.dat");
        index.save(file);
        HnswIndex loaded = HnswIndex.load(file);

        loaded.remove(0L);

        Assert.assertEquals(loaded.size(), 49);
        assertDoesNotContainPoint(0L, loaded.search(vectors.get(0L), 5, 200));
    }

    @Test
    public void shouldReturnCorrectNearestWhenKIsOneAfterDeleteTc44() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.9f, 0.1f));
        index.insert(3L, vector(0f, 1f));

        index.remove(1L);

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);
        Assert.assertEquals(results.size(), 1);
        Assert.assertEquals(results.get(0).getId(), 2L);
    }

    @Test
    public void shouldReturnExactlyActiveSizeResultsWhenKEqualsActiveSizeTc45() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);
        for (long id = 0; id < 10; ++id) {
            index.remove(id);
        }

        List<SearchResult> results = index.search(
            randomVector(new Random(2L), 8),
            20,
            200
        );

        Assert.assertEquals(results.size(), 20);
        Assert.assertEquals(new HashSet<>(idsOf(results)).size(), 20);
    }

    @Test
    public void shouldStillFindLastRemainingPointAfterDeletingAllOthersTc46() {
        HnswIndex index = buildIndex(16, 200);
        insertRandomPoints(index, new Random(1L), 30, 8);

        for (long id = 1; id < 30; ++id) {
            index.remove(id);
        }

        Assert.assertEquals(index.size(), 1);
        assertContainsPoint(
            0L,
            index.search(randomVector(new Random(2L), 8), 5, 200)
        );
    }

    @Test
    public void shouldNotChangeNearestResultWhenDeletingFarPointTc47() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.9f, 0.1f));
        index.insert(3L, vector(-1f, 0f));

        index.remove(3L);

        List<SearchResult> results = index.search(vector(1f, 0f), 1, 64);
        Assert.assertEquals(results.get(0).getId(), 1L);
    }

    @Test
    public void shouldPromoteNextPointsWhenDeletingATopRankedPointTc48() {
        HnswIndex index = new HnswIndex();
        index.insert(1L, vector(1f, 0f));
        index.insert(2L, vector(0.95f, 0.05f));
        index.insert(3L, vector(0.8f, 0.2f));
        index.insert(4L, vector(0f, 1f));

        index.remove(1L);

        List<SearchResult> results = index.search(vector(1f, 0f), 3, 64);
        Assert.assertEquals(results.get(0).getId(), 2L);
        Assert.assertEquals(results.get(1).getId(), 3L);
    }

    @Test
    public void shouldHandleRepeatedRandomAddDeleteSearchSequenceTc49() {
        HnswIndex index = buildIndex(16, 200);
        Random random = new Random(42L);
        Set<Long> active = new HashSet<>();

        for (int i = 0; i < 500; ++i) {
            int action = random.nextInt(3);
            if (action == 0 || active.isEmpty()) {
                long id = random.nextInt(100);
                index.insert(id, randomVector(random, 8));
                active.add(id);
            } else if (action == 1) {
                long id = new ArrayList<>(active).get(
                    random.nextInt(active.size())
                );
                index.remove(id);
                active.remove(id);
            } else {
                List<SearchResult> results = index.search(
                    randomVector(random, 8),
                    5,
                    64
                );
                for (SearchResult result : results) {
                    Assert.assertTrue(active.contains(result.getId()));
                }
            }
        }

        Assert.assertEquals(index.size(), active.size());
    }

    @Test
    public void shouldStayConsistentThroughFullAddSearchDeleteReaddLifecycleTc50() {
        HnswIndex index = buildIndex(16, 200);
        Map<Long, float[]> vectors = insertRandomPoints(
            index,
            new Random(1L),
            50,
            8
        );
        assertContainsPoint(0L, index.search(vectors.get(0L), 1, 200));

        for (long id = 0; id < 25; ++id) {
            index.remove(id);
        }
        Assert.assertEquals(index.size(), 25);
        for (long id = 0; id < 25; ++id) {
            assertDoesNotContainPoint(
                id,
                index.search(vectors.get(id), 1, 200)
            );
        }

        float[] newVector = randomVector(new Random(99L), 8);
        index.insert(0L, newVector);
        Assert.assertEquals(index.size(), 26);
        assertContainsPoint(0L, index.search(newVector, 1, 200));

        for (long id = 25; id < 50; ++id) {
            index.remove(id);
        }
        index.remove(0L);

        Assert.assertEquals(index.size(), 0);
        Assert.assertTrue(
            index.search(randomVector(new Random(2L), 8), 5, 200).isEmpty()
        );
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

    private static Map<Long, float[]> insertRandomPoints(
        HnswIndex index,
        Random random,
        int count,
        int dimension
    ) {
        Map<Long, float[]> vectors = new HashMap<>();
        for (long id = 0; id < count; ++id) {
            float[] v = randomVector(random, dimension);
            vectors.put(id, v);
            index.insert(id, v);
        }
        return vectors;
    }

    private static long findPointAtLevel(HnswIndex index, int level) {
        for (long id : index.getNodeIds()) {
            if (index.getNodeLevel(id) == level) {
                return id;
            }
        }
        throw new IllegalStateException(
            "no point found at level " + level
        );
    }

    private static long findPointAtLevelAbove(
        HnswIndex index,
        int minLevel
    ) {
        for (long id : index.getNodeIds()) {
            if (index.getNodeLevel(id) > minLevel) {
                return id;
            }
        }
        throw new IllegalStateException(
            "no point found above level " + minLevel
        );
    }

    private static long findPointWithMostNeighbors(HnswIndex index) {
        long bestId = -1;
        int bestCount = -1;
        for (long id : index.getNodeIds()) {
            int count = index.getNeighborIds(id, 0).size();
            if (count > bestCount) {
                bestCount = count;
                bestId = id;
            }
        }
        return bestId;
    }

    private static Path tempFile(String name) throws IOException {
        Path dir = Files.createTempDirectory("hnsw-delete-point-test");
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
}
