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

import org.youngmonkeys.ezyvector.hnsw.HnswIndex;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class HnswIndexSaveLoadBenchmark {

    private static final int FILE_MAGIC = 0x455A4857;
    private static final int FILE_VERSION = 1;
    private static final int VECTOR_DIMENSION = 128;
    private static final long RANDOM_SEED = 42L;

    public static void main(String[] args) throws IOException {
        int[] nodeCounts = parseNodeCounts(args);

        Path dir = Files.createTempDirectory("hnsw-save-load-benchmark");
        dir.toFile().deleteOnExit();

        warmUp(dir);

        System.out.println("save()");
        printHeader();
        for (int nodeCount : nodeCounts) {
            benchmarkSave(dir, nodeCount);
        }

        System.out.println();
        System.out.println("load()");
        printHeader();
        for (int nodeCount : nodeCounts) {
            benchmarkLoad(dir, nodeCount);
        }
    }

    private static int[] parseNodeCounts(String[] args) {
        if (args.length == 0) {
            return new int[] {10_000};
        }
        String[] parts = args[0].split(",");
        int[] nodeCounts = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            nodeCounts[i] = Integer.parseInt(parts[i].trim());
        }
        return nodeCounts;
    }

    private static void warmUp(Path dir) throws IOException {
        HnswIndex index = buildIndex(500);
        Path before = dir.resolve("warmup-before.dat");
        Path after = dir.resolve("warmup-after.dat");
        saveUnbuffered(index, before);
        index.save(after);
        loadUnbuffered(before);
        HnswIndex.load(after);
        Files.deleteIfExists(before);
        Files.deleteIfExists(after);
    }

    private static void printHeader() {
        System.out.printf(
            Locale.ROOT,
            "%-10s %-12s %-14s %-14s %-8s%n",
            "Nodes", "File size", "Before", "After", "Speed-up"
        );
    }

    private static void benchmarkSave(
        Path dir,
        int nodeCount
    ) throws IOException {
        HnswIndex index = buildIndex(nodeCount);
        Path before = dir.resolve("save-before-" + nodeCount + ".dat");
        Path after = dir.resolve("save-after-" + nodeCount + ".dat");

        long startBefore = System.nanoTime();
        saveUnbuffered(index, before);
        long msBefore = elapsedMillis(startBefore);

        long startAfter = System.nanoTime();
        index.save(after);
        long msAfter = elapsedMillis(startAfter);

        long fileSize = Files.size(after);
        printRow(nodeCount, fileSize, msBefore, msAfter);

        Files.deleteIfExists(before);
        Files.deleteIfExists(after);
    }

    private static void benchmarkLoad(
        Path dir,
        int nodeCount
    ) throws IOException {
        HnswIndex index = buildIndex(nodeCount);
        Path before = dir.resolve("load-before-" + nodeCount + ".dat");
        Path after = dir.resolve("load-after-" + nodeCount + ".dat");
        saveUnbuffered(index, before);
        index.save(after);
        long fileSize = Files.size(after);

        long startBefore = System.nanoTime();
        loadUnbuffered(before);
        long msBefore = elapsedMillis(startBefore);

        long startAfter = System.nanoTime();
        HnswIndex.load(after);
        long msAfter = elapsedMillis(startAfter);

        printRow(nodeCount, fileSize, msBefore, msAfter);

        Files.deleteIfExists(before);
        Files.deleteIfExists(after);
    }

    private static long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static void printRow(
        int nodeCount,
        long fileSize,
        long msBefore,
        long msAfter
    ) {
        double speedUp = msAfter == 0
            ? 0d
            : (double) msBefore / (double) msAfter;
        System.out.printf(
            Locale.ROOT,
            "%-10d %-12s %-14s %-14s %-8s%n",
            nodeCount,
            formatBytes(fileSize),
            msBefore + " ms",
            msAfter + " ms",
            String.format(Locale.ROOT, "%.1f×", speedUp)
        );
    }

    private static String formatBytes(long bytes) {
        double mb = bytes / (1024d * 1024d);
        if (mb >= 1024d) {
            return String.format(Locale.ROOT, "%.2f GB", mb / 1024d);
        }
        return String.format(Locale.ROOT, "%.2f MB", mb);
    }

    private static HnswIndex buildIndex(int nodeCount) {
        HnswIndex index = new HnswIndex();
        Random random = new Random(HnswIndexSaveLoadBenchmark.RANDOM_SEED);
        for (long id = 0; id < nodeCount; ++id) {
            index.insert(id, randomVector(random));
        }
        return index;
    }

    private static float[] randomVector(Random random) {
        float[] vector = new float[HnswIndexSaveLoadBenchmark.VECTOR_DIMENSION];
        for (int i = 0; i < HnswIndexSaveLoadBenchmark.VECTOR_DIMENSION; ++i) {
            vector[i] = random.nextFloat() * 2f - 1f;
        }
        return vector;
    }

    private static void saveUnbuffered(
        HnswIndex index,
        Path path
    ) throws IOException {
        try (
            DataOutputStream output = new DataOutputStream(
                Files.newOutputStream(path)
            )
        ) {
            Set<Long> ids = index.getNodeIds();
            Long entryPointId = index.getEntryPointId();
            output.writeInt(FILE_MAGIC);
            output.writeInt(FILE_VERSION);
            output.writeInt(HnswIndex.DEFAULT_MAX_M);
            output.writeInt(HnswIndex.DEFAULT_EF_CONSTRUCTION);
            output.writeInt(index.getVectorSize());
            output.writeInt(index.getMaxLevel());
            output.writeLong(entryPointId == null ? 0L : entryPointId);
            output.writeInt(ids.size());
            for (long id : ids) {
                writeNodeUnbuffered(index, id, output);
            }
        }
    }

    private static void writeNodeUnbuffered(
        HnswIndex index,
        long id,
        DataOutputStream output
    ) throws IOException {
        int level = index.getNodeLevel(id);
        float[] vector = index.getVector(id);
        output.writeLong(id);
        output.writeInt(level);
        output.writeBoolean(false);
        output.writeInt(vector.length);
        for (float value : vector) {
            output.writeFloat(value);
        }
        output.writeInt(level + 1);
        for (int layer = 0; layer <= level; ++layer) {
            List<Long> neighbors = index.getNeighborIds(id, layer);
            output.writeInt(neighbors.size());
            for (long neighborId : neighbors) {
                output.writeLong(neighborId);
            }
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    private static long loadUnbuffered(Path path) throws IOException {
        long checksum = 0L;
        try (
            DataInputStream input = new DataInputStream(
                Files.newInputStream(path)
            )
        ) {
            checksum += input.readInt();
            checksum += input.readInt();
            checksum += input.readInt();
            checksum += input.readInt();
            checksum += input.readInt();
            checksum += input.readInt();
            checksum += input.readLong();
            int nodeCount = input.readInt();
            for (int i = 0; i < nodeCount; ++i) {
                checksum += readNodeUnbuffered(input);
            }
        }
        return checksum;
    }

    private static long readNodeUnbuffered(
        DataInputStream input
    ) throws IOException {
        long checksum = input.readLong();
        checksum += input.readInt();
        input.readBoolean();
        int vectorLength = input.readInt();
        for (int i = 0; i < vectorLength; ++i) {
            checksum += Float.floatToIntBits(input.readFloat());
        }
        int levelCount = input.readInt();
        for (int layer = 0; layer < levelCount; ++layer) {
            int neighborCount = input.readInt();
            for (int j = 0; j < neighborCount; ++j) {
                checksum += input.readLong();
            }
        }
        return checksum;
    }
}
