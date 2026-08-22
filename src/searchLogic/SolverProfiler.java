package searchLogic;

/**
 * Instrumentation for the level solver hot paths.
 *
 * Design notes:
 *  - ENABLED is a static final read once from a system property, so when profiling
 *    is off the JIT constant-folds every `if (SolverProfiler.ENABLED)` guard away
 *    and the instrumentation costs literally nothing in the shipped hot loop.
 *  - Counters are plain long increments (register-resident, effectively free).
 *    They tell you WHERE THE WORK VOLUME IS, which is what actually matters for
 *    an exponential search; you divide wall time by them to get cost per unit.
 *  - Timers use System.nanoTime(), which costs ~20-25ns per call. Calling that on
 *    a path that runs millions of times per second would dominate the measurement,
 *    so timed sections are SAMPLED (1 call in TIMER_SAMPLE_RATE) and scaled back up.
 *    That keeps distortion near counter-only levels while still attributing time.
 *
 * Usage:
 *   gradle runTest -PlevelName="Level 015.json" -Pprofile
 *   gradle runTest -PlevelName="Level 015.json" -Pprofile -Pdedupe
 */
public final class SolverProfiler {

    public static final boolean ENABLED = Boolean.getBoolean("solver.profile");

    /** Opt-in: also count how many DISTINCT physical light outcomes the leaves produced. */
    public static final boolean DEDUPE = Boolean.getBoolean("solver.profile.dedupe");

    /** Sample one in every 1024 timed calls, then scale. Power of two so we can mask. */
    private static final int TIMER_SAMPLE_RATE = 1024;
    private static final int TIMER_SAMPLE_MASK = TIMER_SAMPLE_RATE - 1;

    /**
     * Cost of a System.nanoTime() call pair, measured at startup.
     *
     * This matters a lot here: the sections being timed (one filter() call, one
     * projectLight() call) run in a few hundred nanoseconds, and a nanoTime pair
     * costs a meaningful fraction of that. Without subtracting it the estimates
     * come out wildly inflated -- an early version of this profiler reported
     * projectLight() at 225% of total wall time, which is obviously impossible
     * and was pure measurement overhead. Timings at this granularity carry high
     * relative error even after this correction, so the COUNTERS are the
     * trustworthy output and the time estimates are only indicative.
     */
    private static final long NANOTIME_OVERHEAD = calibrateNanoTimeOverhead();

    private static long calibrateNanoTimeOverhead() {
        if (!ENABLED) return 0L;
        // Warm up, then measure a large batch of back-to-back nanoTime pairs
        for (int i = 0; i < 200_000; i++) {
            long ignored = System.nanoTime();
            if (ignored == -1) System.out.print("");
        }
        final int reps = 1_000_000;
        long start = System.nanoTime();
        for (int i = 0; i < reps; i++) {
            long a = System.nanoTime();
            if (a == -1) System.out.print("");
        }
        long elapsed = System.nanoTime() - start;
        // Two calls bracket each timed section, so charge two calls' worth
        return Math.max(0L, (elapsed / reps) * 2);
    }

    // ---- Search tree counters ----
    public long nodesVisited;          // recursive calls that had a DGO left to place
    public long placementsTried;       // (node, spot) pairs actually descended into
    public long spotsRejectedOccupied; // candidate spots skipped because a DGO already sat there
    public long symmetryPrunes;        // whole subtrees skipped by symmetry breaking
    public long backtracks;

    // ---- Filtering counters ----
    public long filterCalls;
    public long filterSpotsScanned;    // total candidate spots fed into dgo.filter()
    public long filterSpotsKept;       // total spots that survived
    private long filterSampleCounter;
    public long filterNanosSampled;
    public long filterSamples;

    // ---- Leaf / light simulation counters ----
    public long leafEvaluations;       // full placements that reached projectLight()
    public long lightQueuePops;
    public long lightSingleSteps;      // incrementLight() advances that actually moved
    public long dgoInteractions;       // interactWithLight() dispatches
    public long receiverHits;
    public long wallStops;
    public long litCellsReset;
    private long projectSampleCounter;
    public long projectNanosSampled;
    public long projectSamples;

    // ---- Per-depth node counts: shows exactly where the tree explodes ----
    public long[] nodesAtDepth = new long[64];

    // ---- Distinct-outcome tracking (opt-in, diagnostic) ----
    private LongHashSet distinctOutcomes;
    public boolean outcomeSetSaturated;

    public SolverProfiler() {
        if (DEDUPE) {
            distinctOutcomes = new LongHashSet(1 << 22); // ~4.2M slots, ~33MB
        }
    }

    // ---- Search tree hooks ----

    public void nodeVisited(int depth) {
        nodesVisited++;
        if (depth >= 0 && depth < nodesAtDepth.length) {
            nodesAtDepth[depth]++;
        }
    }

    // ---- Filter hooks: return a start timestamp, pass it back to filterEnd ----

    public long filterStart() {
        filterCalls++;
        if ((filterSampleCounter++ & TIMER_SAMPLE_MASK) == 0) {
            return System.nanoTime();
        }
        return 0L;
    }

    public void filterEnd(long startedAt, int scanned, int kept) {
        filterSpotsScanned += scanned;
        filterSpotsKept += kept;
        if (startedAt != 0L) {
            filterNanosSampled += System.nanoTime() - startedAt;
            filterSamples++;
        }
    }

    // ---- Leaf / light hooks ----

    public long projectStart() {
        leafEvaluations++;
        if ((projectSampleCounter++ & TIMER_SAMPLE_MASK) == 0) {
            return System.nanoTime();
        }
        return 0L;
    }

    public void projectEnd(long startedAt) {
        if (startedAt != 0L) {
            projectNanosSampled += System.nanoTime() - startedAt;
            projectSamples++;
        }
    }

    /**
     * Records an order-independent signature of one leaf's physical light outcome.
     * Two placements that light exactly the same cells produce the same signature,
     * so (leafEvaluations - distinctOutcomes) is the amount of provably redundant work.
     */
    public void recordOutcome(int[] litX, int[] litY, int litCount, boolean solved) {
        if (distinctOutcomes == null || outcomeSetSaturated) return;
        long sig = solved ? 0x9E3779B97F4A7C15L : 0L;
        for (int i = 0; i < litCount; i++) {
            // Order-independent: mix each cell then XOR, so queue order doesn't matter.
            long h = (litX[i] * 31L + litY[i]) * 0x9E3779B97F4A7C15L;
            h ^= (h >>> 29);
            sig ^= h;
        }
        if (!distinctOutcomes.add(sig)) {
            // add() returns false when the table is saturated, not when the key existed
            if (distinctOutcomes.isSaturated()) {
                outcomeSetSaturated = true;
            }
        }
    }

    public long distinctOutcomeCount() {
        return distinctOutcomes == null ? -1 : distinctOutcomes.size();
    }

    // ---- Reporting ----

    public String report(long totalWallMillis) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ SOLVER PROFILE ================\n");
        sb.append(String.format("Total wall time              : %,d ms%n", totalWallMillis));

        sb.append("\n-- Search tree --\n");
        sb.append(String.format("Internal nodes visited       : %,d%n", nodesVisited));
        sb.append(String.format("Placements descended into    : %,d%n", placementsTried));
        sb.append(String.format("Spots skipped (occupied)     : %,d%n", spotsRejectedOccupied));
        sb.append(String.format("Symmetry-breaking prunes     : %,d%n", symmetryPrunes));
        sb.append(String.format("Backtracks                   : %,d%n", backtracks));
        sb.append(String.format("Leaf evaluations             : %,d%n", leafEvaluations));
        if (leafEvaluations > 0 && nodesVisited > 0) {
            sb.append(String.format("Internal nodes per leaf      : %.2f%n",
                    nodesVisited / (double) leafEvaluations));
        }

        sb.append("\n-- Nodes per depth (where the tree explodes) --\n");
        for (int d = 0; d < nodesAtDepth.length; d++) {
            if (nodesAtDepth[d] == 0) continue;
            sb.append(String.format("  depth %-2d : %,15d%n", d, nodesAtDepth[d]));
        }

        sb.append("\n-- Filtering (runs once per internal node) --\n");
        sb.append(String.format("filter() calls               : %,d%n", filterCalls));
        sb.append(String.format("Candidate spots scanned      : %,d%n", filterSpotsScanned));
        sb.append(String.format("Spots kept                   : %,d%n", filterSpotsKept));
        if (filterSpotsScanned > 0) {
            sb.append(String.format("Keep rate                    : %.1f%%%n",
                    100.0 * filterSpotsKept / filterSpotsScanned));
        }
        if (filterCalls > 0) {
            sb.append(String.format("Avg spots scanned per call   : %.1f%n",
                    filterSpotsScanned / (double) filterCalls));
        }
        sb.append(estimateSection("filter()", filterNanosSampled, filterSamples,
                filterCalls, totalWallMillis));

        sb.append("\n-- Light simulation (runs once per leaf) --\n");
        sb.append(String.format("Light queue pops             : %,d%n", lightQueuePops));
        sb.append(String.format("Single-cell advances         : %,d%n", lightSingleSteps));
        sb.append(String.format("DGO interactions             : %,d%n", dgoInteractions));
        sb.append(String.format("Receiver hits                : %,d%n", receiverHits));
        sb.append(String.format("Wall stops                   : %,d%n", wallStops));
        sb.append(String.format("Lit cells reset              : %,d%n", litCellsReset));
        if (leafEvaluations > 0) {
            sb.append(String.format("Queue pops per leaf          : %.2f%n",
                    lightQueuePops / (double) leafEvaluations));
        }
        sb.append(estimateSection("projectLight()", projectNanosSampled, projectSamples,
                leafEvaluations, totalWallMillis));

        if (DEDUPE) {
            sb.append("\n-- Redundancy (distinct physical light outcomes) --\n");
            long distinct = distinctOutcomeCount();
            if (outcomeSetSaturated) {
                sb.append(String.format("Distinct light outcomes      : >= %,d (table saturated)%n", distinct));
            } else {
                sb.append(String.format("Distinct light outcomes      : %,d%n", distinct));
                if (distinct > 0 && leafEvaluations > 0) {
                    sb.append(String.format("Redundant leaf evaluations   : %,d (%.2f%% of all leaves)%n",
                            leafEvaluations - distinct,
                            100.0 * (leafEvaluations - distinct) / leafEvaluations));
                    sb.append(String.format("Avg re-simulations per outcome: %.1fx%n",
                            leafEvaluations / (double) distinct));
                }
            }
        }

        if (leafEvaluations > 0 && totalWallMillis > 0) {
            sb.append("\n-- Throughput --\n");
            sb.append(String.format("Leaf evaluations / sec       : %,.0f%n",
                    leafEvaluations / (totalWallMillis / 1000.0)));
            sb.append(String.format("ns per leaf evaluation       : %,.0f%n",
                    (totalWallMillis * 1_000_000.0) / leafEvaluations));
        }
        sb.append("===============================================\n");
        return sb.toString();
    }

    // EFFECTS: Scales a sampled timing back up to a whole-run estimate, correcting
    // for the cost of the nanoTime calls used to take the measurement
    private String estimateSection(String name, long nanosSampled, long samples,
                                   long totalCalls, long totalWallMillis) {
        if (samples == 0 || totalCalls == 0) return "";
        double rawAvgNanos = nanosSampled / (double) samples;
        double avgNanos = Math.max(0.0, rawAvgNanos - NANOTIME_OVERHEAD);
        double estTotalMillis = (avgNanos * totalCalls) / 1_000_000.0;
        double pctOfWall = totalWallMillis > 0 ? 100.0 * estTotalMillis / totalWallMillis : 0.0;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Est. time in %-15s: ~%,.0f ms (~%.1f%% of wall)  "
                        + "[avg %.0f ns net of %d ns probe, x %,d calls, %,d samples]%n",
                name, estTotalMillis, pctOfWall, avgNanos, NANOTIME_OVERHEAD, totalCalls, samples));
        if (pctOfWall > 110.0) {
            sb.append(String.format("    !! estimate exceeds wall time -- section is too short to time "
                    + "reliably; trust the counters above, not this line%n"));
        }
        return sb.toString();
    }

    /**
     * Minimal open-addressing long set. Avoids boxing and avoids pulling in a
     * dependency just for a diagnostic mode. Stops accepting once ~70% full so a
     * pathological level can't exhaust the heap.
     */
    private static final class LongHashSet {
        private final long[] keys;
        private final boolean[] used;
        private final int mask;
        private final int capacity;
        private int size;
        private boolean saturated;

        LongHashSet(int slots) {
            this.keys = new long[slots];
            this.used = new boolean[slots];
            this.mask = slots - 1;
            this.capacity = (int) (slots * 0.7);
        }

        boolean add(long key) {
            if (saturated) return false;
            int i = (int) ((key ^ (key >>> 32)) & mask);
            while (used[i]) {
                if (keys[i] == key) return false; // already present
                i = (i + 1) & mask;
            }
            used[i] = true;
            keys[i] = key;
            if (++size >= capacity) saturated = true;
            return true;
        }

        int size() { return size; }
        boolean isSaturated() { return saturated; }
    }
}
