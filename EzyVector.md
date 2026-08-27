# EzyVector — Installation & Integration Guide

EzyVector is a vector database plugin for [EzyPlatform](https://ezyplatform.com) that lets you store and search vectors (embeddings) directly on your EzyPlatform website without installing a separate Qdrant/Pinecone/Weaviate instance. This document explains how to install EzyVector and how to call its API from **any programming language** (no Java knowledge required) — EzyVector itself is a Java plugin running inside EzyPlatform, but clients interact with it purely over HTTP/JSON.

---

## 1. Architecture overview

EzyVector consists of 3 Maven modules:

| Module | Role |
|---|---|
| `ezyvector-sdk` | Core logic: models, services, HNSW index, segment management, writing vectors to the filesystem |
| `ezyvector-admin-plugin` | Admin panel UI inside EzyPlatform (collection/point/segment listing, API key/IP configuration) |
| `ezyvector-web-plugin` | Exposes the public REST API (`/ezyvector/collections/...`) for external applications to call |

Vector data and the HNSW index are stored on the **server's filesystem** (by default under `data/ezyvector` inside the website's data directory, configurable via the `ezyrag_vector_data_dir` setting). Metadata (collections/points/segments) is stored in **MySQL** — the same MySQL instance the EzyPlatform website already uses.

> Since EzyVector is only a plugin, you must already have a running EzyPlatform website to install it into — EzyVector does not run standalone as its own server.

---

## 2. Prerequisites

- A running **EzyPlatform** website with **MySQL** already configured.
- **Java 8** (JDK 8) and **Maven** if you are building the plugin from source yourself.
- **Admin panel** access on that website (to activate the module and configure the API key/IP).
- The server running EzyPlatform needs filesystem write permission (to store vector/HNSW index files).

If you are only going to **use** EzyVector (not build it yourself), installation is handled by the website owner/EzyPlatform operations team; the part you mainly need is section [6. Calling the API from your application](#6-calling-the-api-from-your-application).

---

## 3. Installing the plugin into an EzyPlatform website

### 3.1. Set up environment variables

```bash
# Path to the EzyPlatform SDK (the ezy.sh tool used to export a module)
export EZYPLATFORM_SDK=/path/to/ezyplatform/sdk
export PATH=$PATH:$EZYPLATFORM_SDK/bin

# Install path (root directory) of the target EzyPlatform website
export EZYPLATFORM_HOME=/path/to/your/ezyplatform-site
```

Or declare it in `local.properties` at the root of the EzyVector project:

```properties
build.tool=maven
ezyplatform.home=/path/to/your/ezyplatform-site
```

### 3.2. Build and export the plugin

From the root of the `ezyvector` project:

```bash
mvn -pl . clean install
mvn -pl ezyvector-sdk clean install
mvn -pl ezyvector-admin-plugin clean install -Pexport,\!test
mvn -pl ezyvector-web-plugin clean install -Pexport,\!test
ezy.sh package
ezy.sh export
```

(or simply run `./export.sh` on macOS/Linux, `export.bat` on Windows — both scripts run exactly the commands above).

`ezy.sh export` copies `module.properties`, resources (`resources/`), and the built jars into the exact directories EzyPlatform scans at startup:

```
$EZYPLATFORM_HOME/
├── admin/plugins/ezyvector/     ← from ezyvector-admin-plugin
│   ├── module.properties
│   ├── resources/
│   └── lib/
└── web/plugins/ezyvector/       ← from ezyvector-web-plugin
    ├── module.properties
    ├── resources/
    └── lib/
```

### 3.3. Activate the module in Admin

1. Start (or restart) the EzyPlatform website so it detects the two new plugins under `admin/plugins` and `web/plugins`.
2. Log into the **Admin panel** → **Modules**.
3. Find the two `ezyvector` modules (admin-plugin and web-plugin) → click **Activate**.
   - When a module is activated, the Java package declared in its `module.properties` (`package=org.youngmonkeys.ezyvector`) is added to the set of packages scanned for controllers/services (`packages_to_scan`) — this scan only happens at server startup.
4. **Restart** the website once more so EzyPlatform scans and registers EzyVector's controllers/services.

### 3.4. Verify the installation

- In the Admin panel, an **EzyVector** menu item (Bézier curve icon) should appear in the sidebar with two sub-pages: **Vector collections** and **Settings**.
- Try calling the API to fetch a collection that doesn't exist yet (section 6) — if you get back a `401` (API key not configured) or `404` (not found) instead of a connection error/404 caused by a missing route, the plugin is running correctly.

---

## 4. Configuring API security

Go to **Admin panel → EzyVector → Settings** and configure:

| Field | Meaning |
|---|---|
| **Vector collections API key** | The secret key clients must send with every request. Encrypted at rest in the DB. |
| **Vector collections allowed IPs** | List of IP/CIDR/IP range allowed to call the API. Leave **empty** to allow any IP. |

Valid IP formats per entry (you can enter multiple IPs separated by commas or whitespace within a single entry):

- Single IP: `1.2.3.4`
- CIDR: `1.2.3.0/24`
- Range: `1.2.3.1-1.2.3.100`

> If no API key is set, every API request will be rejected with `401 Unauthorized` (`{"apiKey":"invalid"}`) because an empty API key never matches the stored value — make sure to set an API key before integrating.

---

## 5. REST API overview

Base URL: `https://<your-ezyplatform-website-domain>/ezyvector/collections`

All APIs accept/return **JSON**, with an HTTP style similar to Qdrant, so developers already familiar with Qdrant will feel right at home.

### 5.1. Authentication (required on every request)

Send the API key using **one of three ways**:

1. Query param: `?api-key=<key>` (or `api_key`, `apikey`)
2. Header: `Authorization: Bearer <key>`
3. Header: `x-api-key: <key>`

If the allowed-IPs setting is non-empty, the caller's IP must also match the list, otherwise you'll get `401` (`{"clientIp":"notAllowed"}`).

### 5.2. Endpoint list

| Method | Path | Purpose |
|---|---|---|
| `PUT` | `/{collectionName}` | Create a collection (if it doesn't exist yet) |
| `GET` | `/{collectionName}` | Get collection info |
| `PUT` | `/{collectionName}/points` | Add/update (upsert) data points |
| `POST` | `/{collectionName}/points/search` | Search for nearest vectors |

### 5.3. Create a collection

`PUT /ezyvector/collections/{collectionName}`

Body:
```json
{
  "vectors": {
    "size": 1536,
    "distance": "cosine"
  }
}
```

- `size`: vector dimensionality (required, > 0).
- `distance`: distance metric. **Currently only `cosine` is supported** (case-insensitive).

Response `200`:
```json
{ "result": true, "status": "ok" }
```

### 5.4. Get collection info

`GET /ezyvector/collections/{collectionName}`

Response `200`:
```json
{
  "result": { "config": { "params": { "vectors": { "size": 1536 } } } },
  "status": "ok"
}
```

Response `404` if the collection doesn't exist.

### 5.5. Upsert data points

`PUT /ezyvector/collections/{collectionName}/points`

Body:
```json
{
  "points": [
    {
      "id": 1,
      "vector": [0.12, 0.98, -0.33, ...],
      "payload": { "title": "Article A", "url": "/a" }
    },
    {
      "id": 2,
      "vector": [0.45, -0.11, 0.77, ...],
      "payload": { "title": "Article B" }
    }
  ]
}
```

- `id`: 64-bit integer (long), used for upsert (a duplicate id overwrites the existing point).
- `vector`: array of floats; its length must match the `size` declared when the collection was created.
- `payload`: arbitrary JSON object — extra data that can be returned back on search.

Response `200`:
```json
{ "result": true, "status": "ok" }
```

### 5.6. Search

`POST /ezyvector/collections/{collectionName}/points/search`

Body:
```json
{
  "vector": [0.1, 0.2, 0.3, ...],
  "limit": 10,
  "with_payload": true
}
```

- `vector`: the query vector, same dimensionality as the collection.
- `limit`: maximum number of results to return (> 0).
- `with_payload`: whether to include `payload` in the results.

Response `200`:
```json
{
  "result": [
    { "id": 2, "score": 0.93, "payload": { "title": "Article B" } },
    { "id": 1, "score": 0.81, "payload": { "title": "Article A" } }
  ],
  "status": "ok"
}
```

> While the HNSW index is still being built in the background (right after creation/backfill), EzyVector automatically falls back to sequential search to guarantee accurate results — no special handling is needed on the client side.

### 5.7. Error codes

| HTTP status | When | Example body |
|---|---|---|
| `400` | Missing/invalid field in the request | `{"vectors.size": "required"}`, `{"points[0].vector": "required"}`, `{"vector": "required"}` |
| `401` | Wrong or missing API key | `{"apiKey": "invalid"}` |
| `401` | IP not in the allow-list | `{"clientIp": "notAllowed"}` |
| `404` | Collection not found (on GET) | message varies |

---

## 6. Calling the API from your application

Since this is plain HTTP + JSON, you can call it from any language. Below are examples for creating a collection, upserting, and searching.

### 6.1. curl

```bash
BASE=https://your-site.com/ezyvector/collections
KEY=your-api-key

# Create collection
curl -X PUT "$BASE/articles" \
  -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"vectors": {"size": 4, "distance": "cosine"}}'

# Upsert point
curl -X PUT "$BASE/articles/points" \
  -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"points": [{"id": 1, "vector": [0.1, 0.2, 0.3, 0.4], "payload": {"title": "Hello"}}]}'

# Search
curl -X POST "$BASE/articles/points/search" \
  -H "x-api-key: $KEY" -H "Content-Type: application/json" \
  -d '{"vector": [0.1, 0.2, 0.3, 0.4], "limit": 5, "with_payload": true}'
```

### 6.2. Python

```python
import requests

BASE = "https://your-site.com/ezyvector/collections"
HEADERS = {"x-api-key": "your-api-key"}

requests.put(f"{BASE}/articles", headers=HEADERS, json={
    "vectors": {"size": 4, "distance": "cosine"}
}).raise_for_status()

requests.put(f"{BASE}/articles/points", headers=HEADERS, json={
    "points": [
        {"id": 1, "vector": [0.1, 0.2, 0.3, 0.4], "payload": {"title": "Hello"}}
    ]
}).raise_for_status()

resp = requests.post(f"{BASE}/articles/points/search", headers=HEADERS, json={
    "vector": [0.1, 0.2, 0.3, 0.4],
    "limit": 5,
    "with_payload": True
})
print(resp.json())
```

### 6.3. Node.js

```javascript
const BASE = "https://your-site.com/ezyvector/collections";
const HEADERS = {
  "x-api-key": "your-api-key",
  "Content-Type": "application/json",
};

async function main() {
  await fetch(`${BASE}/articles`, {
    method: "PUT",
    headers: HEADERS,
    body: JSON.stringify({ vectors: { size: 4, distance: "cosine" } }),
  });

  await fetch(`${BASE}/articles/points`, {
    method: "PUT",
    headers: HEADERS,
    body: JSON.stringify({
      points: [{ id: 1, vector: [0.1, 0.2, 0.3, 0.4], payload: { title: "Hello" } }],
    }),
  });

  const res = await fetch(`${BASE}/articles/points/search`, {
    method: "POST",
    headers: HEADERS,
    body: JSON.stringify({ vector: [0.1, 0.2, 0.3, 0.4], limit: 5, with_payload: true }),
  });
  console.log(await res.json());
}

main();
```

### 6.4. PHP

```php
<?php
$base = "https://your-site.com/ezyvector/collections";
$headers = ["x-api-key: your-api-key", "Content-Type: application/json"];

function callApi($url, $method, $headers, $body) {
    $ch = curl_init($url);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($body));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $result = curl_exec($ch);
    curl_close($ch);
    return json_decode($result, true);
}

callApi("$base/articles", "PUT", $headers, ["vectors" => ["size" => 4, "distance" => "cosine"]]);

callApi("$base/articles/points", "PUT", $headers, [
    "points" => [["id" => 1, "vector" => [0.1, 0.2, 0.3, 0.4], "payload" => ["title" => "Hello"]]]
]);

$result = callApi("$base/articles/points/search", "POST", $headers, [
    "vector" => [0.1, 0.2, 0.3, 0.4], "limit" => 5, "with_payload" => true
]);
print_r($result);
```

### 6.5. Go

```go
package main

import (
    "bytes"
    "encoding/json"
    "net/http"
)

const base = "https://your-site.com/ezyvector/collections"
const apiKey = "your-api-key"

func call(method, url string, body interface{}) (*http.Response, error) {
    buf, _ := json.Marshal(body)
    req, _ := http.NewRequest(method, url, bytes.NewReader(buf))
    req.Header.Set("x-api-key", apiKey)
    req.Header.Set("Content-Type", "application/json")
    return http.DefaultClient.Do(req)
}

func main() {
    call("PUT", base+"/articles", map[string]interface{}{
        "vectors": map[string]interface{}{"size": 4, "distance": "cosine"},
    })

    call("PUT", base+"/articles/points", map[string]interface{}{
        "points": []map[string]interface{}{
            {"id": 1, "vector": []float32{0.1, 0.2, 0.3, 0.4}, "payload": map[string]string{"title": "Hello"}},
        },
    })

    call("POST", base+"/articles/points/search", map[string]interface{}{
        "vector": []float32{0.1, 0.2, 0.3, 0.4}, "limit": 5, "with_payload": true,
    })
}
```

---

## 7. Operational notes

- **Segments**: each collection's data is organized into mutable/immutable segments with their own status (building/active/merging/stale/failed) — visible under Admin → EzyVector → Vector collections → collection detail.
- **Automatic backfill**: if you enable indexing on a collection that already has data, EzyVector automatically backfills all existing points into the HNSW index in the background, without blocking reads/writes.
- **Where data is stored**: vector files and the HNSW index live under `data/ezyvector` (relative to the site's data directory), changeable via the `ezyrag_vector_data_dir` setting. Metadata (collections/points/segments) lives in the website's MySQL — back up both parts together if you need to restore.
- **Distance metric**: currently only `cosine` is supported; normalize your vectors before upserting if your embedding model doesn't already normalize them, so cosine similarity results stay accurate.

---

## 8. References

- Product page: https://ezyplatform.com/market/items/ezyvector
- Source code: `ezyvector-sdk`, `ezyvector-admin-plugin`, `ezyvector-web-plugin` in this repo.
