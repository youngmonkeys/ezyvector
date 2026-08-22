# HNSW Test Cases cho EzyRAG

## 1. Mục tiêu

Tài liệu này liệt kê bộ test case nên có cho implementation HNSW trong EzyRAG.

Phạm vi bao gồm:

- Distance calculation.
- Insert / upsert / delete.
- Search correctness.
- HNSW graph invariants.
- Boundary conditions.
- Duplicate vectors.
- Persistence.
- Restart.
- Backfill.
- Segment.
- Recovery.
- Concurrent read/write.
- Corrupted storage.
- Large dataset.
- Recall benchmark.
- Performance regression.

Mục tiêu không chỉ là kiểm tra code chạy được, mà còn đảm bảo:

```text
correctness
+
consistency
+
durability
+
search quality
+
performance
```

---

## 2. Quy ước test

Nên chia thành:

```text
Unit Test
Integration Test
Persistence Test
Recovery Test
Concurrency Test
Benchmark Test
```

Tên test khuyến nghị:

```java
shouldReturnNearestPointWhenSearchExactVector()
shouldPersistIndexAndLoadAfterRestart()
shouldIgnoreDeletedPointWhenSearching()
```

Không nên:

```java
testSearch1()
testInsert2()
```

---

## 3. Fixtures cơ bản

### 3.1. Vector dimension nhỏ

Dùng dimension nhỏ để dễ kiểm tra bằng tay:

```text
dimension = 2
dimension = 3
dimension = 4
```

Ví dụ:

```java
float[] V1 = {1.0f, 0.0f};
float[] V2 = {0.0f, 1.0f};
float[] V3 = {1.0f, 1.0f};
float[] V4 = {-1.0f, 0.0f};
```

### 3.2. Dataset deterministic

Không dùng random không seed:

```java
Random random = new Random(123456789L);
```

---

## 4. Distance tests

### TC-DIST-001 - Cosine identical vector

Input:

```text
A = [1, 0]
B = [1, 0]
```

Expected:

```text
cosine similarity = 1
```

### TC-DIST-002 - Cosine orthogonal vector

```text
A = [1, 0]
B = [0, 1]
```

Expected:

```text
cosine similarity = 0
```

### TC-DIST-003 - Cosine opposite vector

```text
A = [1, 0]
B = [-1, 0]
```

Expected:

```text
cosine similarity = -1
```

### TC-DIST-004 - Dot product

```text
A = [1, 2]
B = [3, 4]
```

Expected:

```text
11
```

### TC-DIST-005 - Euclidean identical vectors

Expected:

```text
distance = 0
```

### TC-DIST-006 - Euclidean known result

```text
A = [0, 0]
B = [3, 4]
```

Expected:

```text
distance = 5
```

### TC-DIST-007 - Dimension mismatch

Expected:

```text
IllegalArgumentException
```

### TC-DIST-008 - Zero vector cosine

Không được sinh:

```text
NaN
Infinity
```

Phải define rõ policy: reject hoặc defined result.

### TC-DIST-009 - NaN input

Expected: reject.

### TC-DIST-010 - Infinity input

Expected: reject.

---

## 5. Empty index tests

### TC-EMPTY-001 - Search empty index

Expected:

```text
empty list
```

### TC-EMPTY-002 - Delete from empty index

Expected: safe no-op hoặc explicit not found.

### TC-EMPTY-003 - Persist empty index

Save/load thành công, `size = 0`.

---

## 6. Single point tests

### TC-SINGLE-001 - Insert one point

```text
pointId = 1
vector = [1, 0]
```

Expected `size = 1`.

### TC-SINGLE-002 - Search same vector

Expected `pointId = 1`.

### TC-SINGLE-003 - Search different vector

Nếu `limit >= 1`, vẫn trả point duy nhất.

### TC-SINGLE-004 - Delete only point

Search trả empty.

---

## 7. Insert tests

### TC-INSERT-001 - Insert multiple points

Insert nhiều point và verify tất cả searchable.

### TC-INSERT-002 - Point IDs không liên tục

```text
10
1000
999999
```

Search phải trả đúng external point ID.

### TC-INSERT-003 - Point ID = 0

Theo contract: allow hoặc reject rõ ràng.

### TC-INSERT-004 - Point ID = Long.MAX_VALUE

Verify serialization/mapping/search.

### TC-INSERT-005 - Duplicate point ID

Phải rõ contract: reject hoặc upsert.

---

## 8. Upsert/version tests

### TC-UPSERT-001 - Update vector cùng point ID

Old vector không còn là current state.

### TC-UPSERT-002 - Higher version wins

Version 2 phải thay version 1.

### TC-UPSERT-003 - Ignore older version

Nếu current version = 5, incoming version = 4 thì ignore.

### TC-UPSERT-004 - Same version idempotency

Replay cùng operation không tạo duplicate graph node.

### TC-UPSERT-005 - Reinsert deleted point với version mới hơn

```text
v1 ACTIVE
v2 DELETE
v3 UPSERT
```

Expected: searchable bằng v3.

---

## 9. Search correctness tests

### TC-SEARCH-001 - Exact vector rank first

### TC-SEARCH-002 - Top-K ordering đúng

### TC-SEARCH-003 - Limit = 1

### TC-SEARCH-004 - Limit < size

### TC-SEARCH-005 - Limit = size

### TC-SEARCH-006 - Limit > size

### TC-SEARCH-007 - Limit = 0

Khuyến nghị reject.

### TC-SEARCH-008 - Negative limit

Expected exception.

### TC-SEARCH-009 - efSearch < limit

Reject hoặc normalize thành `limit`.

### TC-SEARCH-010 - Large efSearch

Không corruption.

### TC-SEARCH-011 - Search deterministic

Cùng query/index/config nên có order ổn định nếu tie breaker được define.

---

## 10. Tie tests

### TC-TIE-001 - Duplicate vectors

Hai point cùng vector đều có thể được trả.

### TC-TIE-002 - Same score ordering

Khuyến nghị tie-breaker deterministic, ví dụ:

```text
score DESC
pointId ASC
```

---

## 11. Graph invariant tests

### TC-GRAPH-001 - Entry point tồn tại khi index non-empty

### TC-GRAPH-002 - Entry point tham chiếu node hợp lệ

### TC-GRAPH-003 - maxLevel hợp lệ

### TC-GRAPH-004 - Node level nằm trong range

### TC-GRAPH-005 - Neighbor reference tồn tại

### TC-GRAPH-006 - Không self-edge

### TC-GRAPH-007 - Không duplicate neighbor trong cùng level

### TC-GRAPH-008 - Neighbor count không vượt limit cấu hình

### TC-GRAPH-009 - Deleted node không được trả về

### TC-GRAPH-010 - Graph vẫn traversable sau nhiều insert

---

## 12. Level generation tests

### TC-LEVEL-001 - Level không âm

### TC-LEVEL-002 - Seeded RNG deterministic nếu hỗ trợ

### TC-LEVEL-003 - Higher level có ít node hơn theo thống kê

---

## 13. Delete tests

### TC-DELETE-001 - Delete existing point

Không còn xuất hiện trong search.

### TC-DELETE-002 - Delete nonexistent point

Safe no-op hoặc not found.

### TC-DELETE-003 - Delete twice

Idempotent.

### TC-DELETE-004 - Delete nearest point

Search phải trả nearest hợp lệ tiếp theo.

### TC-DELETE-005 - Delete entry point

Search vẫn hoạt động.

### TC-DELETE-006 - Delete highest-level node

Graph vẫn searchable.

### TC-DELETE-007 - Delete all points

Search empty, index không corrupt.

---

## 14. Tombstone tests

### TC-TOMB-001 - Tombstone physically remains but logically hidden

### TC-TOMB-002 - Tombstone survives restart

### TC-TOMB-003 - Compaction loại tombstone vật lý

---

## 15. Vector validation tests

### TC-VAL-001 - Wrong dimension insert

### TC-VAL-002 - Wrong dimension search

### TC-VAL-003 - Empty vector

### TC-VAL-004 - Null vector

### TC-VAL-005 - NaN

### TC-VAL-006 - Infinity

---

## 16. Numeric stability tests

### TC-NUM-001 - Very small floats

### TC-NUM-002 - Very large floats

### TC-NUM-003 - Mixed positive/negative

### TC-NUM-004 - Normalized vectors

---

## 17. Persistence tests

### TC-PERSIST-001 - Save empty index

### TC-PERSIST-002 - Save one-point index

### TC-PERSIST-003 - Save many-point index

### TC-PERSIST-004 - Search before/after restart tương đương

### TC-PERSIST-005 - Persist entry point

### TC-PERSIST-006 - Persist max level

### TC-PERSIST-007 - Persist node levels

### TC-PERSIST-008 - Persist neighbor lists

### TC-PERSIST-009 - Persist tombstones

### TC-PERSIST-010 - Persist versions

---

## 18. File format tests

### TC-FILE-001 - Valid magic header

### TC-FILE-002 - Invalid magic header

### TC-FILE-003 - Unsupported format version

### TC-FILE-004 - Collection ID mismatch

### TC-FILE-005 - Segment ID mismatch

### TC-FILE-006 - Vector dimension mismatch

### TC-FILE-007 - Truncated vectors.dat

### TC-FILE-008 - Truncated hnsw.dat

### TC-FILE-009 - Invalid point_ids.dat length

### TC-FILE-010 - Invalid checksum

---

## 19. Memory-mapped storage tests

### TC-MMAP-001 - Open vectors bằng mmap

### TC-MMAP-002 - File lớn hơn Java heap

### TC-MMAP-003 - Random offset read đúng vector

### TC-MMAP-004 - Offset calculation đúng

```text
offset = index * dimension * Float.BYTES
```

### TC-MMAP-005 - Read vector cuối file không overflow

---

## 20. Backfill tests

### TC-BACKFILL-001 - Empty collection

### TC-BACKFILL-002 - Fewer than batch size

### TC-BACKFILL-003 - Exactly batch size

### TC-BACKFILL-004 - Batch size + 1

### TC-BACKFILL-005 - Multiple batches

Không skip, không duplicate.

### TC-BACKFILL-006 - Resume from progress

### TC-BACKFILL-007 - Crash before progress save

Replay phải idempotent.

### TC-BACKFILL-008 - Crash after progress save

Không được skip point.

### TC-BACKFILL-009 - Deleted point trong source

### TC-BACKFILL-010 - Point update trong khi backfill

Latest version wins.

---

## 21. Segment tests

### TC-SEG-001 - Create first segment

Expected `segmentNo = 1`.

### TC-SEG-002 - Segment reaches max points

Freeze mutable, tạo segment mới.

### TC-SEG-003 - Search across two segments

### TC-SEG-004 - Search across many segments

### TC-SEG-005 - Same point ở old/new segment

Latest version wins.

### TC-SEG-006 - Deleted old version không được trả

### TC-SEG-007 - Chỉ ACTIVE segment được search

---

## 22. Segment lifecycle tests

### TC-SEG-LIFE-001 - BUILDING không searchable

### TC-SEG-LIFE-002 - ACTIVE searchable

### TC-SEG-LIFE-003 - OBSOLETE không searchable

### TC-SEG-LIFE-004 - CORRUPTED không searchable

### TC-SEG-LIFE-005 - Atomic BUILDING -> ACTIVE

Crash trước publish không ảnh hưởng old segments.

---

## 23. Compaction tests

### TC-COMP-001 - Merge two segments

### TC-COMP-002 - Search result preserved after compaction

### TC-COMP-003 - Deleted points removed

### TC-COMP-004 - Old versions removed

### TC-COMP-005 - Crash during compaction

### TC-COMP-006 - Crash after new files complete before metadata swap

### TC-COMP-007 - Crash after metadata swap

---

## 24. Operation log tests

### TC-OP-001 - UPSERT PENDING -> DONE

### TC-OP-002 - DELETE PENDING -> DONE

### TC-OP-003 - PROCESSING operation on restart

### TC-OP-004 - FAILED operation retry

### TC-OP-005 - Same operation processed twice

### TC-OP-006 - Older operation after newer operation

### TC-OP-007 - Delete followed by newer upsert

---

## 25. Restart tests

### TC-RESTART-001 - Empty collection

### TC-RESTART-002 - Active index

### TC-RESTART-003 - Mutable data

### TC-RESTART-004 - Pending operations

### TC-RESTART-005 - Obsolete segment

### TC-RESTART-006 - Corrupted segment

---

## 26. Multi-segment merge tests

### TC-MERGE-001 - Best result từ first segment

### TC-MERGE-002 - Best result từ last segment

### TC-MERGE-003 - Top-K phân tán nhiều segment

### TC-MERGE-004 - Duplicate point IDs across segments

Latest version wins.

### TC-MERGE-005 - Deleted candidate có score tốt nhất

Phải skip và lấy candidate hợp lệ tiếp theo.

### TC-MERGE-006 - Equal score across segments

Order deterministic.

---

## 27. efSearch tests

### TC-EF-001 - Minimum efSearch

Ví dụ 64.

### TC-EF-002 - Dynamic efSearch

```text
limit = 10
multiplier = 8
min = 64
```

Expected:

```text
80
```

### TC-EF-003 - Small limit

`limit = 1` -> 64.

### TC-EF-004 - Large limit

`limit = 100` -> 800 nếu không cap.

### TC-EF-005 - Maximum efSearch cap

Nếu implementation có cap.

### TC-EF-006 - Recall với ef lớn hơn không giảm đáng kể theo thống kê

---

## 28. M parameter tests

### TC-M-001 - M = 0

Reject.

### TC-M-002 - M < 0

Reject.

### TC-M-003 - Small M

Index vẫn hợp lệ.

### TC-M-004 - Neighbor count tuân thủ M

---

## 29. efConstruction tests

### TC-EFC-001 - Invalid efConstruction

### TC-EFC-002 - efConstruction < M

Define rõ reject/normalize.

### TC-EFC-003 - Higher efConstruction build succeeds

### TC-EFC-004 - Recall benchmark theo efConstruction

---

## 30. Configuration tests

### TC-CONFIG-001 - Default HNSW config

### TC-CONFIG-002 - Collection-specific config

### TC-CONFIG-003 - Invalid config

### TC-CONFIG-004 - Unsupported distance

### TC-CONFIG-005 - Unsupported index type

---

## 31. Concurrency tests

### TC-CONC-001 - Multiple concurrent searches

### TC-CONC-002 - Search while insert

### TC-CONC-003 - Search while delete

### TC-CONC-004 - Concurrent inserts khác point

### TC-CONC-005 - Concurrent upsert cùng point

Highest version wins.

### TC-CONC-006 - Concurrent delete/upsert cùng point

### TC-CONC-007 - Search while segment freeze

### TC-CONC-008 - Search while compaction publish

Query phải thấy old snapshot hoặc new snapshot, không được half-state.

---

## 32. Thread-safety tests

### TC-THREAD-001 - Không ConcurrentModificationException

### TC-THREAD-002 - Neighbor list publish an toàn

### TC-THREAD-003 - Entry point visibility an toàn

---

## 33. Corruption tests

### TC-CORR-001 - Missing vectors.dat

### TC-CORR-002 - Missing hnsw.dat

### TC-CORR-003 - Missing point_ids.dat

### TC-CORR-004 - Corrupt metadata

### TC-CORR-005 - Wrong file size

### TC-CORR-006 - Neighbor references invalid node

### TC-CORR-007 - Duplicate internal node ID

---

## 34. Collection isolation tests

### TC-COLL-001 - Same point ID ở hai collection

### TC-COLL-002 - Search collection A không trả B

### TC-COLL-003 - Delete A không ảnh hưởng B

### TC-COLL-004 - Different dimensions

### TC-COLL-005 - Different distance metrics

---

## 35. Exact-search oracle tests

Nên có brute-force implementation dùng làm oracle.

### TC-ORACLE-001 - Returned point phải tồn tại trong dataset

### TC-ORACLE-002 - Returned score phải khớp score tính trực tiếp

### TC-ORACLE-003 - efSearch rất cao phải tiến gần exact Top-K

---

## 36. Recall tests

Công thức:

```text
Recall@K =
|HNSW TopK ∩ Exact TopK|
/
K
```

### TC-RECALL-001 - Recall@1

### TC-RECALL-002 - Recall@10

### TC-RECALL-003 - Recall@100

### TC-RECALL-004 - Internal recall threshold

Ví dụ:

```text
Recall@10 >= 0.95
```

Chỉ dùng sau benchmark dataset/config thực tế.

---

## 37. Randomized/property tests

### TC-PROP-001 - Insert N random points, search exact inserted vectors

### TC-PROP-002 - Random insert/delete/update rồi persist/reload

### TC-PROP-003 - Replay operation list twice cho cùng final state

---

## 38. Large dataset tests

Nên gắn:

```java
@Tag("slow")
```

### TC-LARGE-001 - 10k points

### TC-LARGE-002 - 100k points

### TC-LARGE-003 - 1M points

Verify:

```text
build
restart
search
memory
```

---

## 39. Memory tests

### TC-MEM-001 - Heap không giữ toàn bộ immutable vectors sau mmap

### TC-MEM-002 - Repeated search không leak

### TC-MEM-003 - Repeated segment open/close không leak FD

### TC-MEM-004 - Direct/mapped buffer lifecycle

---

## 40. File descriptor tests

### TC-FD-001 - Open many segments

### TC-FD-002 - Close collection

### TC-FD-003 - Delete obsolete files sau close

---

## 41. Performance tests

Không assert latency quá cứng trong unit CI.

### TC-PERF-001 - Insert throughput

### TC-PERF-002 - Search P50

### TC-PERF-003 - Search P95

### TC-PERF-004 - Search P99

### TC-PERF-005 - Restart/open latency

### TC-PERF-006 - Backfill throughput với batch 100/500/1000

---

## 42. Regression tests

Mỗi production bug phải thêm regression test.

Ví dụ:

```text
REG-001 deleted entry point gây NPE
REG-002 duplicate neighbor gây loop
REG-003 progress save sai thứ tự làm skip point
REG-004 old version overwrite new version
```

---

## 43. Infinite-loop tests

### TC-LOOP-001 - Cyclic graph

Search phải terminate.

### TC-LOOP-002 - Dense graph

### TC-LOOP-003 - Duplicate/malformed edges

---

## 44. Visited-set tests

### TC-VISIT-001 - Node chỉ processed theo đúng visited semantics

### TC-VISIT-002 - Visited state không leak giữa hai search

---

## 45. Priority queue tests

### TC-PQ-001 - Candidate queue ordering

### TC-PQ-002 - Result queue ordering

### TC-PQ-003 - Bounded queue behavior

### TC-PQ-004 - Equal score deterministic

---

## 46. Internal/external ID mapping tests

### TC-ID-001 - internal ID khác point ID

### TC-ID-002 - Mapping survive restart

### TC-ID-003 - Mapping logically preserved after compaction

### TC-ID-004 - Deleted mapping không return

---

## 47. Version tests

### TC-VER-001 - Version increment

### TC-VER-002 - Same ID newer vector

### TC-VER-003 - Old version replay

### TC-VER-004 - Delete newer than upsert

### TC-VER-005 - Upsert newer than delete

---

## 48. Database integration tests

### TC-DB-001 - Create collection

### TC-DB-002 - Point + operation trong cùng transaction rollback

### TC-DB-003 - Point + operation commit

### TC-DB-004 - Unique `(collection_id, point_id)`

### TC-DB-005 - Backfill keyset pagination

---

## 49. Transaction ordering tests

### TC-TX-001 - DB commit trước index processing

### TC-TX-002 - Index operation fail nhưng vẫn retryable

### TC-TX-003 - Index durable trước DONE

### TC-TX-004 - Không bao giờ DONE trước durable write

Đây là invariant critical.

---

## 50. Startup tests

### TC-START-001 - No collections

### TC-START-002 - One active collection

### TC-START-003 - Many collections

### TC-START-004 - Collection chưa có segment

### TC-START-005 - Active immutable segments

### TC-START-006 - Pending backfill

### TC-START-007 - Một collection corrupt không làm chết collection khác nếu kiến trúc cho phép

---

## 51. Snapshot consistency tests

### TC-SNAP-001 - Query thấy stable segment list

### TC-SNAP-002 - Old segment không bị close khi reader còn dùng

---

## 52. Cosine normalization tests

Nếu normalize trước storage:

### TC-NORM-001 - Stored vector normalized

### TC-NORM-002 - Zero norm rejected

### TC-NORM-003 - Query normalized consistent

### TC-NORM-004 - Restart giữ representation đúng

---

## 53. Serialization tests

### TC-SER-001 - Float round-trip

### TC-SER-002 - Little endian

### TC-SER-003 - Header version round-trip

### TC-SER-004 - Long point ID round-trip

---

## 54. HNSW quality matrix

Nên benchmark theo matrix:

```text
M:
8
16
32

efConstruction:
64
100
200

efSearch:
32
64
128
256
```

Collect:

```text
Recall@10
P50
P95
index size
build time
```

---

## 55. Suggested JUnit structure

```text
src/test/java/
└── .../vector/
    ├── distance/
    │   ├── CosineDistanceTest.java
    │   ├── DotProductDistanceTest.java
    │   └── EuclideanDistanceTest.java
    ├── hnsw/
    │   ├── HnswIndexInsertTest.java
    │   ├── HnswIndexSearchTest.java
    │   ├── HnswIndexDeleteTest.java
    │   ├── HnswIndexUpsertTest.java
    │   ├── HnswGraphInvariantTest.java
    │   ├── HnswPersistenceTest.java
    │   ├── HnswCorruptionTest.java
    │   ├── HnswConcurrencyTest.java
    │   └── HnswRecallTest.java
    ├── storage/
    │   ├── VectorFileStorageTest.java
    │   ├── VectorFileFormatTest.java
    │   └── VectorMmapStorageTest.java
    ├── segment/
    │   ├── CollectionSegmentTest.java
    │   ├── SegmentCompactionTest.java
    │   └── SegmentLifecycleTest.java
    ├── recovery/
    │   ├── IndexOperationRecoveryTest.java
    │   └── BackfillRecoveryTest.java
    └── benchmark/
        ├── HnswRecallBenchmarkTest.java
        └── HnswPerformanceBenchmarkTest.java
```

---

## 56. Base test utilities

### TestVectors

```java
public final class TestVectors {

    private TestVectors() {}

    public static float[] vector(float... values) {
        return values;
    }
}
```

### ExactVectorSearch

Nên có brute-force implementation làm oracle:

```java
public final class ExactVectorSearch {

    private ExactVectorSearch() {}

    public static List<SearchResult> search(
        Map<Long, float[]> vectors,
        float[] query,
        int limit,
        VectorDistance distance
    ) {
        // brute-force implementation
    }
}
```

---

## 57. Assertion helpers

```java
assertTopPointId(expectedId, results);

assertContainsPoint(pointId, results);

assertDoesNotContainPoint(pointId, results);

assertSortedByScore(results);

assertGraphValid(index);

assertRecallAtLeast(
    exactResults,
    hnswResults,
    0.95
);
```

---

## 58. Minimum test suite trước release

- [ ] Distance correctness.
- [ ] Insert one/many points.
- [ ] Search exact vector.
- [ ] Top-K ordering.
- [ ] Wrong dimension validation.
- [ ] Duplicate point/upsert.
- [ ] Delete point.
- [ ] Delete entry point.
- [ ] Old version ignored.
- [ ] Search empty index.
- [ ] `limit > size`.
- [ ] `efSearch >= limit`.
- [ ] Graph neighbor validity.
- [ ] No self-edge.
- [ ] No duplicate edge.
- [ ] Persist + reload.
- [ ] Same search after restart.
- [ ] Tombstone persists restart.
- [ ] Backfill multiple batches.
- [ ] Resume backfill.
- [ ] Operation replay idempotency.
- [ ] Search across segments.
- [ ] Compaction preserves results.
- [ ] Corrupted file detected.
- [ ] Concurrent searches.
- [ ] Search during insert.
- [ ] Exact-search oracle comparison.
- [ ] Recall@10 benchmark.

---

## 59. Ưu tiên theo phase

### Phase 1 - HNSW core

```text
Distance
Insert
Search
Graph invariants
Delete
Upsert/version
Exact oracle
Recall
```

### Phase 2 - Persistent storage

```text
Serialization
File header
mmap
Save/load
Restart
Corruption
```

### Phase 3 - Segment

```text
Mutable
Immutable
Multi-segment search
Freeze
Lifecycle
```

### Phase 4 - Recovery

```text
Operation log
Backfill resume
Crash boundaries
Idempotency
```

### Phase 5 - Production hardening

```text
Concurrency
Compaction
Large dataset
Memory
File descriptor
Performance regression
```

---

## 60. Critical invariants

```text
1. Một logical point chỉ có một version hiệu lực.

2. Point DELETED không bao giờ được trả về search.

3. Old version không overwrite new version.

4. Mọi neighbor trong graph phải tồn tại.

5. Node không được neighbor chính nó.

6. Neighbor list không duplicate.

7. Search không được loop vô hạn.

8. Restart không cần rebuild toàn bộ từ MySQL.

9. Operation replay phải idempotent.

10. Backfill crash không được skip point.

11. Compaction không làm thay đổi logical search state.

12. BUILDING / OBSOLETE / CORRUPTED segment không tham gia search.

13. Operation chỉ DONE sau khi index durable.

14. Corrupted segment không được silently trả kết quả sai.

15. HNSW score phải khớp distance tính trực tiếp từ raw vector.
```

---

## 61. Definition of Done cho HNSW core

Có thể coi HNSW core đủ ổn khi:

```text
All correctness tests pass
Graph invariants pass
Delete/update/version pass
Exact oracle comparison pass
Recall benchmark đạt threshold nội bộ
100k-point stress test pass
Concurrent search pass
Repeated randomized test pass
```

---

## 62. Khuyến nghị

Không chỉ test kiểu:

```java
assertFalse(results.isEmpty());
```

Vì điều đó chỉ chứng minh code không crash.

Một test HNSW tốt nên kiểm tra ít nhất một trong các yếu tố:

```text
đúng point
đúng thứ tự
đúng score
đúng version
đúng graph invariant
đúng persistence
đúng recovery
đúng recall
```

Đặc biệt, nên giữ một implementation `ExactVectorSearch` đơn giản làm oracle. Đây là công cụ hữu ích nhất để phát hiện lỗi graph traversal, pruning, update và persistence.
