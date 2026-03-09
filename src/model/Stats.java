package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class Stats {
    @Expose
    public String levelName;
    // TODO Enable deserialisation of this through GSON somehow
    public GridCell[][] solutionGrid;
    @Expose
    public long totalTime;
    @Expose
    public BigInteger totalPermutations;
    @Expose
    public long attemptPermutations;
    @Expose
    public double permutationRatio;

    public Stats(){}

    public String getLevelName() {
        return levelName;
    }

    public long getTotalTime() {
        return totalTime / 1000;
    }

    public BigInteger getTotalPermutations() {
        return totalPermutations;
    }

    public long getAttemptPermutations() {
        return attemptPermutations;
    }

    public double getPermutationRatio() {
        return permutationRatio;
    }

    public Stats(GridCell[][] grid, long time, BigInteger total, long attempted) {
        this.solutionGrid = grid;
        this.totalTime = time;
        this.totalPermutations = total;
        this.attemptPermutations = attempted;
        setupPermutationRatio();
    }

    // EFFECTS: Sets up the attempted permutation ratio that is displayed after solving a level
    private void setupPermutationRatio() {
        BigDecimal attempted = BigDecimal.valueOf(this.attemptPermutations);
        BigDecimal total = new BigDecimal(this.totalPermutations);
        BigDecimal ratio = attempted.divide(total, 10, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100));
        ratio = ratio.round(new MathContext(4));
        this.permutationRatio = ratio.doubleValue();
    }

    public void load(String fileName) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        Stats loadedStats = null;
        try {
            FileReader fileReader = new FileReader("src\\solutionFiles\\" + fileName);
            JsonReader reader = new JsonReader(fileReader);
            loadedStats = gson.fromJson(reader, Stats.class);
            this.levelName = loadedStats.levelName;
            this.totalTime = loadedStats.totalTime;
            this.totalPermutations = loadedStats.totalPermutations;
            this.attemptPermutations = loadedStats.attemptPermutations;
            this.permutationRatio = loadedStats.permutationRatio;
        } catch (FileNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }


    /**
     * Loads a specific stat value from a saved Stats file.
     * Supported statName values: "permutationRatio", "totalTime", "totalPermutations", "attemptPermutations", "levelName"
     * Returns -1 for numeric stats if loading fails, or null for String stats.
     */
    public static Object loadPreexistingStat(String fileName, String statName) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        try {
            FileReader fileReader = new FileReader("src\\solutionFiles\\" + fileName + " solution.json");
            JsonReader reader = new JsonReader(fileReader);
            Stats loadedStats = gson.fromJson(reader, Stats.class);
            switch (statName) {
                case "permutationRatio":
                    return loadedStats.permutationRatio;
                case "totalTime":
                    return loadedStats.totalTime;
                case "totalPermutations":
                    return loadedStats.totalPermutations;
                case "attemptPermutations":
                    return loadedStats.attemptPermutations;
                case "levelName":
                    return loadedStats.levelName;
                default:
                    System.out.println("Unsupported statName: " + statName);
                    return null;
            }
        } catch (Exception e) {
            System.out.println("No pre-saved stat for " + statName + ", " + fileName);
            if (statName.equals("levelName")) return null;
            return -1;
        }
    }

    public static double loadPreexistingPermutationRatio(String fileName) {
        Object result = loadPreexistingStat(fileName, "permutationRatio");
        return result instanceof Double ? (Double) result : -1;
    }

    public static double loadPreexistingTotalTime(String fileName) {
        Object result = loadPreexistingStat(fileName, "totalTime");
        return result instanceof Long ? ((Long) result).doubleValue() : -1;
    }

    public void save(String fileName) {
        Gson gson = new Gson();
        String saveFileJSON = gson.toJson(this);
        try {
            FileWriter writer = new FileWriter("src\\solutionFiles\\" + fileName + " solution.json");
            writer.write(saveFileJSON);
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Couldn't save the level!");
        }
    }
}
