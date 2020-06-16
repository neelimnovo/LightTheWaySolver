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
import java.math.MathContext;

public class Stats {
    @Expose
    public String levelName;
    // TODO Enable deserialisation of this through GSON somehow
    public GridCell[][] solutionGrid;
    @Expose
    public long totalTime;
    @Expose
    public long totalPermutations;
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

    public long getTotalPermutations() {
        return totalPermutations;
    }

    public long getAttemptPermutations() {
        return attemptPermutations;
    }

    public double getPermutationRatio() {
        return permutationRatio;
    }

    public Stats(GridCell[][] grid, long time, long total, long attempted) {
        this.solutionGrid = grid;
        this.totalTime = time;
        this.totalPermutations = total;
        this.attemptPermutations = attempted;
        setupPermutationRatio();
    }

    // EFFECTS: Sets up the attempted permutation ratio that is displayed after solving a level
    private void setupPermutationRatio() {
        double ratio = ((double)this.attemptPermutations/ (double)this.totalPermutations) * 100;
        BigDecimal bigDecimalRatio = new BigDecimal(ratio);
        bigDecimalRatio = bigDecimalRatio.round(new MathContext(4));
        double rounded = bigDecimalRatio.doubleValue();
        this.permutationRatio = rounded;
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
