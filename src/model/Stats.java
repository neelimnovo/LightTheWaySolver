package model;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

public class Stats {
    public GridCell[][] solutionGrid;
    public long totalTime;
    public long totalPermutations;
    public long attemptPermutations;
    public double permutationRatio;

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
        Gson gson = new Gson();
        Level loadedLevel = null;
        try {
            FileReader fileReader = new FileReader("src\\solutionFiles\\" + fileName+ " solution.json");
            JsonReader reader = new JsonReader(fileReader);
            loadedLevel = gson.fromJson(reader, Stats.class);
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
