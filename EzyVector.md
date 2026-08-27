# EzyVector — HNSW Algorithm & Storage Format Specification

This document is **not** an EzyPlatform plugin installation guide. Its purpose: describe, in enough detail, the **HNSW algorithm**, the **on-disk binary file formats**, and the **relational (DB) schema** that EzyVector uses (read directly from the `ezyvector-sdk` source code), so that a developer working in another language (Python, Go, Rust, Node.js, C#...) can **re-implement an equivalent system** without needing to know Java or depend on EzyPlatform.

Every number, offset, and byte order in this document is taken exactly from:
- `ezyvector-sdk/src/main/java/org/youngmonkeys/ezyvector/hnsw/HnswIndex.java`
- `ezyvector-sdk/src/main/java/org/youngmonkeys/ezyvector/storage/EzyVectorFileStorage.java`
- `ezyvector-sdk/src/main/java/org/youngmonkeys/ezyvector/service/EzyVectorService.java`
- `ezyvector-admin-plugin/src/main/resources/scripts/scripts.sql`

---

## 1. Overall architecture — 3 storage layers

Each collection is stored in **3 places in parallel**, not a single source of data:

```
                 ┌─────────────────────────────────────┐
                 │  1. RDBMS (metadata + source of truth)│
                 │   - ezyvector_collections             │
                 │   - ezyvector_collection_points        │← also stores the vector (blob) + payload
                 │   - ezyvector_collection_segments       │
                 └───────────────┬───────────────────────┘
                                 │ backfill / build (paginated reads, 500 rows at a time)
                                 ▼
   ┌─────────────────────────────────────────────────────────────┐
   │ 2. Raw files on the filesystem (one segment directory per     │
   │    collection)                                                │
   │    vectors.dat   — normalized vectors, accessed by slot        │
   │    point_ids.dat — point_id array, parallel to vectors.dat     │
   │    backfill.progress — resume cursor for the backfill process  │
   └───────────────────────────────┬───────────────────────────────┘
                                   │ build (reads everything from the DB, NOT from vectors.dat)
                                   ▼
                 ┌─────────────────────────────────────┐
                 │ 3. In-memory HNSW index + hnsw.dat    │
                 │    (multi-layer graph, used for low-   │
                 │    latency approximate search)         │
                 └─────────────────────────────────────┘
```

Why 3 layers: the DB is the source of truth (durable, normally queryable/backupable); the raw vector file lets EzyVector do **100%-accurate sequential search** while the HNSW index is still being built (no accuracy gap at all); HNSW gives speed. When you re-implement this yourself, you **can drop layer 2** if you're fine with slower/less-accurate search while the index is warming up (e.g. fall back to scanning the RDBMS itself) — this is a design choice, not a hard requirement of the HNSW algorithm.

---

## 2. Relational schema (metadata)

Original DDL (MySQL, `InnoDB`, `utf8mb4`) — the MySQL-specific parts (`bigint unsigned`, `mediumblob`, collation) can be swapped for equivalent types on another DB:

```sql
CREATE TABLE ezyvector_collections (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120)    NOT NULL,
    vector_size   BIGINT UNSIGNED NOT NULL,
    distance      VARCHAR(50)     NOT NULL,       -- currently only "COSINE" is used
    index_type    VARCHAR(50)     NOT NULL DEFAULT 'HNSW',
    status        VARCHAR(50)     NOT NULL DEFAULT 'ACTIVE', -- ACTIVATED | INACTIVATED
    points_count  BIGINT UNSIGNED NOT NULL DEFAULT 0,        -- see note below
    config        TEXT,
    created_at    DATETIME NOT NULL,
    updated_at    DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY key_name (name),
    INDEX index_status (status)
);

CREATE TABLE ezyvector_collection_points (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,  -- ★ used as the "slot id" for file storage
    collection_id  BIGINT UNSIGNED NOT NULL,
    point_id       BIGINT UNSIGNED NOT NULL,                 -- client-supplied id
    vector         MEDIUMBLOB NOT NULL,                      -- raw vector, sequential float32[]
    payload        MEDIUMTEXT,                               -- arbitrary JSON
    status         VARCHAR(50) NOT NULL,                     -- LIVE | DELETED (see section 7)
    version        BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY key_collection_point (collection_id, point_id),
    INDEX index_collection_status (collection_id, status),
    INDEX index_updated_at (updated_at)
);

CREATE TABLE ezyvector_collection_segments (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    collection_id  BIGINT UNSIGNED NOT NULL,
    segment_no     BIGINT UNSIGNED NOT NULL,   -- always = 1 in the current version (see section 6)
    segment_type   VARCHAR(50) NOT NULL,       -- MUTABLE | IMMUTABLE
    status         VARCHAR(50) NOT NULL,       -- BUILDING|ACTIVE|COMPACTING|OBSOLETE|CORRUPTED
    points_count   BIGINT UNSIGNED NOT NULL DEFAULT 0,  -- never updated (see note)
    min_point_id   BIGINT UNSIGNED,                     -- never updated (see note)
    max_point_id   BIGINT UNSIGNED,                     -- never updated (see note)
    index_version  BIGINT UNSIGNED NOT NULL DEFAULT 1,
    created_at     DATETIME NOT NULL,
    updated_at     DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY key_collection_segment (collection_id, segment_no),
    INDEX index_collection_status (collection_id, status)
);
```

**Important notes when reading this schema** (so you don't assume the engine is more fully built out than it is):

- The `points_count` column (on both tables), `min_point_id`, and `max_point_id` are declared in the schema and shown in the admin UI, but **nowhere in the current source code is a value ever written to them** (no `setPointsCount`/`setMinPointId`/`setMaxPointId` call exists beyond the column defaults). If you re-implement this and need these figures, compute them yourself with `COUNT(*)`/`MIN(point_id)`/`MAX(point_id)` — don't trust these columns as maintained state.
- `ezyvector_collection_segments` always has **exactly one row** per collection, `segment_no = 1`, `segment_type = MUTABLE`, `status = ACTIVE`, created once when the collection is first created/used. The `COMPACTING/OBSOLETE/CORRUPTED` statuses and the `IMMUTABLE` type exist in the enum but **no code path ever assigns them** — this is groundwork for future segment merging/compaction that isn't implemented yet. If you don't need multi-segment support, you can drop the segment concept entirely and just keep one data file/one index per collection.
- `ezyvector_collections.status` has only ever been set to `ACTIVATED`; no API sets `INACTIVATED` — the column exists to support enabling/disabling a collection later.
- The `vector` column (`MEDIUMBLOB`) stores the **raw vector as sent by the client** (not necessarily normalized — normalization only happens when writing to `vectors.dat`/HNSW, see sections 4 & 5), serialized by the framework's internal ORM (not standard Hibernate) as a sequential `float32` array in element order. When re-implementing, you **only need to preserve element order on read** (big-endian or little-endian both work, as long as it's consistent) — this is not a format that needs to interoperate/lock-step with `vectors.dat`; it's just the source-of-truth store used for backfill/rebuild.

---

## 3. The HNSW algorithm — parameters & in-memory data structures

Default parameters (`HnswIndex.DEFAULT_MAX_M = 16`, `DEFAULT_EF_CONSTRUCTION = 200`):

| Parameter | Value | Meaning |
|---|---|---|
| `M` (maxM) | 16 | Max neighbors per node at layers > 0 |
| `M0` (maxM0) | `2 × M` = 32 | Max neighbors at layer 0 (the bottom, densest layer) |
| `efConstruction` | 200 | Beam-search width used when **inserting** a new node (bigger = more accurate, slower) |
| `levelMultiplier` (mL) | `1 / ln(M)` | Coefficient for the random-level formula |

Each node in the graph consists of:
```
Node {
  id: int64                     // = point_id set by the client (NOT the DB slot id)
  vector: float32[]              // already L2-normalized (unit length = 1)
  level: int                     // highest level this node appears at
  neighborsByLevel: List<List<int64>>   // one neighbor list per level, from 0..level
  deleted: bool                  // tombstone, see section 7
}
```

Index-wide state: `entryPoint` (the node every traversal starts from), `maxLevel` (the current highest level in the graph), `vectorSize` (locked in after the first insert — every subsequent vector must have the same dimensionality, otherwise it's rejected with an error; there is **no** automatic resizing).

### 3.1. Distance metric — always cosine, regardless of what the `distance` field says

Whatever value `distance` was declared with at collection-creation time (currently only `"cosine"` is accepted), **`HnswIndex` itself never reads that field** — it always:

1. L2-normalizes every vector, both on insert **and** on query:
   ```
   normalize(v) = v / sqrt(Σ v[i]²)     // if norm = 0, returns an all-zero vector
   ```
2. Defines internal "distance" as `1 - dot(a, b)` (a, b already normalized).
3. Returns a `score` to the client = `1 - distance = dot(a, b)` = **cosine similarity**, in the range `[-1, 1]` (typically `[0, 1]` for real-world embeddings), **higher = more similar**.

If you want to support Euclidean/plain dot-product too, this is the spot to change when writing your own version.

### 3.2. Choosing a random level for a new node

```
r = random_uniform(0, 1)       // avoid an exact r = 0 (use the smallest positive double)
level = floor(-ln(r) × levelMultiplier)
```
This is the standard formula from the HNSW paper (an exponentially decaying distribution over levels, each level up being roughly `1/M` as likely).

### 3.3. Inserting a vector

```
insert(id, vector):
    normalized = normalize(vector)
    lock write
    if vectorSize not yet set → set it = len(vector); else if it doesn't match → error
    removeNode(id)                      # if id already exists: FULLY delete the old node from
                                         # the graph (unlinking it from every other node's
                                         # neighbor list) then re-insert from scratch — there is
                                         # NO "in-place update"
    level = randomLevel()
    node = Node(id, normalized, level)
    if entryPoint == null:
        entryPoint = node; maxLevel = level; return

    # (a) Greedy descent: starting at entryPoint, walk down from maxLevel to level+1,
    #     keeping only 1 "current best" point per layer, no beam expansion
    curr = entryPoint
    for lc in [maxLevel .. level+1]:
        repeat: look at all of curr's neighbors at layer lc; if any neighbor is
                closer to the target than curr, jump to that neighbor; repeat
                until nothing improves (single best-first walk, ef = 1)

    # (b) From layer min(level, maxLevel) down to 0: beam-search, then connect edges
    for lc in [min(level, maxLevel) .. 0]:
        candidates = searchLayer(target=normalized, entry=curr,
                                  ef=efConstruction, layer=lc,
                                  excludeDeleted=false)
        limit = (lc == 0) ? M0 : M
        selected = candidates[0 .. limit)     # already sorted ascending by distance,
                                               # take the "top-N nearest" — does NOT use the
                                               # diversification heuristic from the original paper
        for c in selected:
            node.connect(lc, c.id)
            neighbor = getNode(c.id)
            neighbor.connect(lc, node.id)     # connect both directions
            pruneNeighbors(neighbor, lc)      # see 3.5
        curr = getNode(candidates[0].id)      # nearest candidate, used as entry for the layer below

    if level > maxLevel:
        maxLevel = level; entryPoint = node
    unlock
```

> ⚠️ Note for reimplementers: the neighbor-selection step in (b) uses **"selection simple"** (top-N by raw distance) rather than the **"selection heuristic"** (direction-diversifying, avoiding overly dense clusters) that the original HNSW paper recommends as better for recall. This is a deliberate simplification in EzyVector — you can keep it as-is (easier to implement) or upgrade to the heuristic if you need better recall.

### 3.4. Single-layer beam search — `searchLayer(target, entry, ef, layer, excludeDeleted)`

This is the core routine used by both insert and search:

```
searchLayer(target, entry, ef, layer, excludeDeleted):
    visited = {entry.id}
    candidateHeap = min-heap by distance, containing (entry, dist(target, entry))
    resultHeap    = max-heap by distance, capped at size ef
    if !excludeDeleted or !entry.deleted: resultHeap.push(entry)

    while candidateHeap is not empty:
        current = candidateHeap.pop()   # nearest unprocessed candidate
        # early stop: if current is already farther than the worst candidate in a
        # FULL resultHeap (already has ef entries) → can't improve further, stop
        if resultHeap is full and current.dist > resultHeap.peek().dist: break

        for neighborId in graph[current].neighbors(layer):
            if neighborId already visited: skip
            mark visited
            d = dist(target, neighbor.vector)
            if resultHeap not yet full or d < resultHeap.worst():
                candidateHeap.push(neighbor, d)
                if !excludeDeleted or !neighbor.deleted:
                    resultHeap.push(neighbor, d)
                    if resultHeap.size() > ef: resultHeap.popWorst()

    return sort_ascending_by_distance(resultHeap)
```

A subtle point worth preserving when re-implementing: **a node flagged `deleted` is still used as a "bridge" while traversing the graph** (it still ends up in `candidateHeap`/`visited`, its neighbors still get expanded) — it's only excluded from `resultHeap` (i.e. excluded from the returned results) when `excludeDeleted = true`. That's what keeps deleting a point from "breaking" connectivity around it in the graph.

### 3.5. Pruning neighbors — `pruneNeighbors(node, layer)`

After a node gets a new edge connected to it, if its neighbor count at that layer now exceeds the limit (`M0` at layer 0, `M` elsewhere):
```
pruneNeighbors(node, layer):
    limit = (layer == 0) ? M0 : M
    if neighbor count <= limit: return
    compute distance(node, each neighbor); sort ascending
    keep the `limit` nearest neighbors, drop the rest
```

### 3.6. Searching — used by the `/points/search` API

```
search(queryVector, k, ef):
    normalized = normalize(queryVector)
    lock read
    start = (entryPoint == null or entryPoint.deleted)
              ? findActiveEntryPoint()   # ⚠️ scans every live node, O(n) — rarely triggered
              : entryPoint
    if start == null: return []

    # Greedy single-best-first descent from maxLevel down to 1 (same as step (a) in insert)
    curr = start
    for lc in [maxLevel .. 1]:
        repeat expanding toward the nearest neighbor at layer lc until it converges

    # The real beam search only happens at layer 0
    candidates = searchLayer(normalized, curr, ef=max(ef, k), layer=0, excludeDeleted=true)
    return candidates[0 .. k)   # already sorted ascending by distance
    # for each candidate, the returned score = 1 - distance = cosine similarity
```

EzyVector calls this with:
```
ef = max(limit × 8, 64)     # EXPLORATION_FACTOR_SEARCH_MULTIPLIER=8, MIN_EXPLORATION_FACTOR_SEARCH=64
```
i.e. it always explores at least 8x wider than the number of results requested (minimum 64), to boost recall — this is a tunable constant depending on your speed/accuracy needs.

### 3.7. Removing a node — `remove(id)`

```
remove(id):
    lock write
    node = get(id)
    if node exists:
        node.deleted = true                     # ONLY a tombstone flag, edges are NOT unlinked
        if node is the current entryPoint:
            entryPoint = findActiveEntryPoint()  # O(n) scan for the highest-level surviving node
            maxLevel   = max level among surviving nodes
    unlock
```

> `remove()` **is implemented in `HnswIndex`, but the web plugin currently has NO endpoint for deleting a data point** (the controller only exposes `PUT` create-collection, `PUT` upsert-points, `POST` search, `GET` collection — no `DELETE`). If you need deletion, this is an API you'll have to add yourself at the application layer when re-implementing.

---

## 4. `vectors.dat` / `point_ids.dat` file format (raw vector store, used for exact sequential search)

The key point: this is **not** the HNSW index — it's just a fixed-record-size array accessed randomly via a **"slot"**.

### 4.1. What a slot id actually is — the easiest thing to misunderstand

```
slotId = ezyvector_collection_points.id     # the AUTO_INCREMENT PRIMARY KEY of the WHOLE TABLE,
                                             # shared across EVERY collection, NOT a counter
                                             # scoped to each individual collection!
```
Because `id` is a global AUTO_INCREMENT on the `ezyvector_collection_points` table (not per-collection), if you have multiple collections being written to interleaved, one collection's `vectors.dat` file will end up with **gaps** at the slots that belong to other collections' points. The file size is therefore proportional to **the highest id ever assigned to a point in that collection**, not to **the actual number of points in that collection**. When re-implementing, you should consider using a **per-collection** counter instead, to avoid wasting disk space — this is a design choice EzyVector made, not a hard requirement of the algorithm.

`point_ids.dat` exists to distinguish a slot that "has real data" (value > 0) from a slot that's a "gap/never written" (default value 0, since the file is created via sparse writes) — during a sequential scan, any slot with `point_id <= 0` is skipped.

### 4.2. Exact byte layout

Both files use **little-endian**, `Float.BYTES = 4`, `Long.BYTES = 8`. `vectorSize` = the collection's vector dimensionality (from `ezyvector_collections.vector_size`).

**`vectors.dat`** — an array of fixed-size records, `vectorSize × 4` bytes each:
```
offset(slot i, i starting at 1) = (i - 1) × vectorSize × 4
bytes at that offset: vectorSize consecutive little-endian float32 values
                       = the ALREADY-NORMALIZED vector (not the raw vector the client sent!)
```

**`point_ids.dat`** — an array of fixed-size 8-byte records, positioned 1:1 with `vectors.dat`:
```
offset(slot i) = (i - 1) × 8
bytes at that offset: one little-endian int64 = the client-supplied point_id
                       (a value of 0 = an empty/gap slot, skipped during search)
```

**`backfill.progress`** — a single 8-byte file, little-endian int64 = the largest `id` (the global PK, not `point_id`) that has been fully backfilled for this collection. Used as the resume cursor (see section 6).

### 4.3. Writing (upsert) to the files — always writes at the slot's exact offset, never appends

```
upsertAll(collectionId, vectorSize, records[(slotId, pointId, vector)]):
    open/create 2 FileChannels (vectors.dat, point_ids.dat) in CREATE + WRITE mode
    for (slotId, pointId, vector) in records:
        normalized = normalize(vector)
        write normalized into vectors.dat at offset (slotId-1)×vectorSize×4
        write pointId    into point_ids.dat at offset (slotId-1)×8
    fsync (force(true)) both files before returning
```
Because writes go to exact offsets (random-access writes, not appends), the files can be **sparse** — modern filesystems (ext4, APFS, NTFS...) treat the unwritten portion as all-zero and don't consume real disk space for the gap.

### 4.4. Reading (exact sequential search) — used when the HNSW index isn't ready yet

```
search(collectionId, vectorSize, query, limit):
    normalizedQuery = normalize(query)
    memory-map vectors.dat + point_ids.dat in chunks ≤ 128MB
      (MAX_MAPPED_BYTES, to avoid mmap'ing the whole file at once if it's very large)
    maintain a min-heap of size `limit`, ordered by `score` (cosine similarity)
    for every slot in the entire file:
        if the point_id at that slot is <= 0: skip it (a gap)
        score = dot(normalizedQuery, the vector at that slot)   # both already normalized
        if the heap isn't full yet, or score > the worst element currently in the heap: push it in
    sort the results descending by score, return them
```
This is genuine brute-force **O(n)** search — 100% accurate, no recall trade-off, only a speed trade-off. EzyVector uses it as the fallback whenever the `HnswIndex` for that collection **hasn't finished building yet** (see section 6), so the search API is always correct even while the index is being built in the background.

---

## 5. `hnsw.dat` file format (a full snapshot of the HNSW graph)

**Unlike `vectors.dat`/`point_ids.dat` (little-endian, hand-rolled with `ByteBuffer`), `hnsw.dat` uses Java's standard `DataOutputStream`/`DataInputStream` → which defaults to BIG-ENDIAN.** This is very easy to get wrong when porting to another language — the two file groups in the same system use two different byte orders.

### 5.1. Header

| Field | Type | Value / meaning |
|---|---|---|
| magic | int32 BE | `0x455A4857` (ASCII `"EZHW"`) |
| version | int32 BE | `1` |
| maxM | int32 BE | the M parameter used at build time (usually 16) |
| efConstruction | int32 BE | the efConstruction parameter used at build time (usually 200) |
| vectorSize | int32 BE | vector dimensionality |
| maxLevel | int32 BE | the highest level currently in the graph |
| entryPointId | int64 BE | `point_id` of the entry point (0 if empty) |
| nodeCount | int32 BE | total node count (including nodes with `deleted=true`, see 5.2) |

### 5.2. Each node (repeated `nodeCount` times, any order)

| Field | Type | Notes |
|---|---|---|
| id | int64 BE | = `point_id` |
| level | int32 BE | |
| deleted | 1 byte (0/1) | tombstone flag |
| vectorLength | int32 BE | = vectorSize |
| vector | `vectorLength` × float32 BE | the **already normalized** vector |
| levelCount | int32 BE | = `level + 1` (number of layers this node has a neighbor list for) |
| — for each layer 0..level: | | |
| &nbsp;&nbsp;neighborCount | int32 BE | |
| &nbsp;&nbsp;neighborIds | `neighborCount` × int64 BE | `point_id`s of that layer's neighbors |

### 5.3. Safe file writing (atomic write)

```
save(path):
    lock read (blocks concurrent writes during the snapshot, doesn't block in-memory reads by
               ongoing searches)
    write the entire content above into a temp file  <path>.tmp
    rename <path>.tmp → <path> via an atomic move (falling back to a regular move if the
        filesystem doesn't support atomic move — e.g. certain Windows/mount configurations)
```
The atomic rename guarantees a reading process never sees `hnsw.dat` in a partially-written state if the process crashes mid-write.

### 5.4. A cost worth knowing when re-implementing: **every upsert rewrites the ENTIRE file**

In `EzyVectorService.updateHnswIndex()`, after inserting new point(s) into the in-memory HNSW graph, if the index is already "ready" (has finished its first build), EzyVector calls `index.save(hnswPath)` — meaning it **re-serializes the whole graph to disk after every single upsert**, not an incremental/append-only write. For a large collection (millions of points), this is a significant I/O cost on every write. If you need high write throughput, consider a write-ahead log (WAL) plus periodic snapshots instead of rewriting the full snapshot on every upsert.

---

## 6. End-to-end flow (all 3 layers together)

### 6.1. Creating a collection
1. Insert one row into `ezyvector_collections` (if the name doesn't already exist) with `vector_size`, `distance="cosine"`, `index_type="HNSW"`, `status="ACTIVATED"`.
2. Ensure a `ezyvector_collection_segments` row exists (segment_no=1, MUTABLE, ACTIVE) — create it if missing.
3. Check & (if needed) start two background processes: **backfill** (section 6.2) and **HNSW build** (section 6.3).

### 6.2. Upserting data points (PUT .../points)
1. Look up `collectionId`, `vectorSize` by collection name.
2. Ensure the segment exists; check & start backfill/build if needed.
3. **Everything written below is synchronized by EXACTLY ONE write lock (`writeLock`) shared across EVERY collection in the same process** — meaning two upserts into two different collections still have to wait on each other, they don't run concurrently. This is a bottleneck worth knowing about if you need high write throughput across many collections — switch to one lock per collection when re-implementing.
4. For each point in the request:
   - Look up the existing row by the unique key `(collection_id, point_id)`.
   - If none: insert a new row (`status=LIVE`, `version=1`); the newly generated `id` (PK) becomes the **slotId** used by file storage.
   - If one exists: update it in place (keep the existing `id`/slotId, `version += 1`, overwrite `vector`/`payload`/`updated_at`).
5. Write the `(slotId, pointId, vector)` tuples into `vectors.dat`/`point_ids.dat` at their exact slot offsets (section 4.3), fsync.
6. If an HNSW index is currently held in RAM for this collection: `insert()` each point into the graph; if the index is already "ready", `save()` the entire `hnsw.dat` again (section 5.4).
7. Check backfill/build once more (in case this was the collection's very first point ever, which needs to kick off the build right away).

### 6.3. Backfill (background job, for when a collection has DB data but file storage is missing it)
Purpose: get points that **already exist in MySQL but aren't in `vectors.dat` yet** (e.g. vector indexing was just turned on for a pre-existing collection, or file storage was lost) into file storage, running in the background without blocking ongoing reads/writes.

```
at most 1 backfill thread running per collection (guarded by an in-memory set of collectionIds)
cursor = read backfill.progress (defaults to 0 if absent)
loop:
    rows = SELECT * FROM ezyvector_collection_points
           WHERE collection_id = ? AND id > cursor
           ORDER BY id ASC LIMIT 500
    if rows is empty: stop
    under the same writeLock used for writes in section 6.2:
        write (id, point_id, vector) of each row into vectors.dat/point_ids.dat
        cursor = the largest id in this batch
    write cursor to backfill.progress (so it can resume if the process crashes mid-way)
```
This gets re-triggered (idempotent, self-checking) on **every call** to `createCollectionIfAbsent`/`upsert`/`search` — if it detects any point with `id > cursor` that hasn't been backfilled yet, it automatically resumes.

### 6.4. Building the HNSW index (background job, built from scratch)
```
at most 1 build thread running per collection (guarded the same way as backfill)
if hnsw.dat already exists on disk AND its vectorSize matches the current collection config:
    load it straight into RAM, mark it "ready", DO NOT rebuild
otherwise (no file yet, or vectorSize mismatch — e.g. the collection's config changed):
    create an empty HnswIndex, hold it in RAM (but NOT marked "ready" yet)
    run a background thread:
        read ALL of the collection's points from MySQL, paginated 500 rows at a time
          (NOT reading from vectors.dat)
        insert() each point into the in-memory graph
        once done: save() to hnsw.dat exactly once, mark it "ready"
```
**While the build is in progress (index not yet "ready"), every `search` call falls back to the branch in section 4.4 (exact brute-force over the files)** — this is exactly the "search never interrupted while the index builds" behavior advertised: users always get correct results, just slower, until the build finishes.

### 6.5. Searching (POST .../points/search)
```
look up collectionId, vectorSize by name
check & start backfill/build if needed (self-healing on every search call too)
if an HNSW index that's "ready" is available in RAM (or can be loaded from hnsw.dat):
    use the algorithm in section 3.6, with ef = max(limit×8, 64)
otherwise:
    use exact brute-force search over vectors.dat/point_ids.dat (section 4.4)
for each result (pointId, score): SELECT payload FROM ezyvector_collection_points
    WHERE collection_id=? AND point_id=? to fetch its payload (1 query per result — an N+1
    pattern; consider batching this into a single `IN (...)` query when you re-implement, to
    cut down round-trips)
```

---

## 7. Point status & deletion (LIVE / DELETED)

`ezyvector_collection_points.status` has 2 values in the enum, `LIVE`/`DELETED`, but **the current flow only ever assigns `LIVE`** (on creation) — no API or logic path ever transitions it to `DELETED`, and there's no delete-point API at the web layer either (already noted in section 3.7). This is a case of the enum "getting ahead of" the actual feature — if you need soft-delete, the convention is already there for you to use, but you'd have to wire it up yourself to: (a) a new DELETE endpoint, (b) a call to `HnswIndex.remove(pointId)`, (c) marking the corresponding slot in `point_ids.dat` (e.g. overwriting it with `point_id = 0` or a negative value) so the brute-force branch skips it too.

---

## 8. Summary of decisions to reconsider when building this in another language

| Issue | What EzyVector does | Worth reconsidering |
|---|---|---|
| Slot id for the vector file | The points table's **globally shared** auto-increment PK (many collections share one counter) | Use a per-collection counter instead, so files aren't sparse/wasteful |
| Neighbor selection on insert | "Simple" — top-N nearest | Could upgrade to the original HNSW paper's "heuristic" selection for better recall |
| Writing hnsw.dat | Rewrites the entire file after every upsert (once the index is ready) | A WAL + periodic snapshots, if you need high write throughput |
| Write lock | One global lock for every collection | One lock per collection, so writes to different collections can run concurrently |
| Segments | Always exactly 1 segment/collection, no merge/compaction | Drop the segment concept if you don't need it, or implement it properly if you need real multi-segment support |
| points_count / min_point_id / max_point_id | Columns exist but are never updated | Compute them yourself via aggregate queries if you need to display them |
| Deleting a point | `HnswIndex.remove()` exists but there's no web API for it, and it's not wired to file storage | Wire up all 3 places yourself: API, HNSW, file storage (section 7) |
| Multi-node / distributed | None — build/backfill are in-process threads, no leader election | Add your own distributed locking if multiple nodes will write to the same collection |
| Distance metric | Always cosine (via normalize + dot product); other `distance` values aren't actually supported | Add a Euclidean/plain-dot-product branch if you need one |

---

## 9. Quick reference: API ⇄ internal behavior mapping

If you rebuild a compatible REST API on top of this, here's what each endpoint corresponds to internally (see `WebApiEzyVectorCollectionController`):

| Endpoint | Corresponding internal behavior |
|---|---|
| `PUT /collections/{name}` | Section 6.1 (creates it if absent; if it already exists, **nothing** is updated, even if the body differs) |
| `GET /collections/{name}` | `SELECT` by `name`, returns `vector_size` |
| `PUT /collections/{name}/points` | Section 6.2 |
| `POST /collections/{name}/points/search` | Section 6.5, body: `{"vector": [...], "limit": N, "with_payload": bool}` |

(There's no endpoint to delete a collection, delete a point, or list points at the public API layer — those operations currently only exist in the Admin UI, which reads the DB directly.)
