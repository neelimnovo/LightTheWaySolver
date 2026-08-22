package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import model.interactionObjects.DynamicGridObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A hand drafted solution for a level: which dynamic grid object sits on which empty spot.
 *
 * Only the placements are persisted, not the whole solved grid. The grid can always be
 * rebuilt by re-loading the level and dropping the placements back onto it, and a
 * GridCell[][] cannot be deserialised anyway because DynamicGridObject is abstract.
 */
public class ReferenceSolution {

    public static final String FOLDER = "src/referenceSolutionFiles/";
    private static final String SUFFIX = " reference.json";

    public String levelName;
    // Whether light projection powered every receiver the last time this draft was saved
    public boolean valid;
    public ArrayList<Placement> placements = new ArrayList<>();

    public static class Placement {
        public int x;
        public int y;
        // Identifier produced by DynamicGridObject.getTypeId(), e.g. "upLight"
        public String type;

        public Placement() {}

        public Placement(int x, int y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    public ReferenceSolution() {}

    public ReferenceSolution(String levelName, boolean valid) {
        this.levelName = levelName;
        this.valid = valid;
    }

    // EFFECTS: Records the dynamic object placed on the given empty spot
    public void addPlacement(int x, int y, DynamicGridObject dgo) {
        placements.add(new Placement(x, y, dgo.getTypeId()));
    }

    // EFFECTS: Places every recorded object back onto the given grid
    // Placements that fall outside the grid or name an unknown object are skipped
    public void applyTo(GridCell[][] grid) {
        for (Placement placement : placements) {
            if (!GridLayout.isWithinBounds(grid, placement.x, placement.y)) continue;
            DynamicGridObject dgo = DynamicGridObject.fromTypeId(placement.type);
            if (dgo == null) {
                System.out.println("Unknown reference solution object: " + placement.type);
                continue;
            }
            grid[placement.x][placement.y].cellDynamicItem = dgo;
        }
    }

    // EFFECTS: Names of the levels that currently have a reference solution on disk, sorted
    public static List<String> savedLevelNames() {
        String[] files = new File(FOLDER).list();
        if (files == null) return new ArrayList<>();
        Arrays.sort(files);
        List<String> names = new ArrayList<>();
        for (String file : files) {
            if (file.endsWith(SUFFIX)) {
                names.add(file.substring(0, file.length() - SUFFIX.length()));
            }
        }
        return names;
    }

    public static boolean exists(String levelName) {
        return new File(FOLDER + levelName + SUFFIX).exists();
    }

    // EFFECTS: Loads the reference solution for the given level, or null if there isn't one
    public static ReferenceSolution load(String levelName) {
        Gson gson = new Gson();
        try (FileReader fileReader = new FileReader(FOLDER + levelName + SUFFIX)) {
            JsonReader reader = new JsonReader(fileReader);
            ReferenceSolution loaded = gson.fromJson(reader, ReferenceSolution.class);
            if (loaded != null && loaded.placements == null) {
                loaded.placements = new ArrayList<>();
            }
            return loaded;
        } catch (IOException e) {
            System.out.println("Couldn't load the reference solution for " + levelName);
            System.out.println(e);
            return null;
        }
    }

    // EFFECTS: Writes this reference solution to the referenceSolutionFiles folder
    // Returns the path written to, or null if saving failed
    public String save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File folder = new File(FOLDER);
        if (!folder.exists() && !folder.mkdirs()) {
            System.out.println("Couldn't create " + FOLDER);
            return null;
        }
        String path = FOLDER + levelName + SUFFIX;
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(gson.toJson(this));
            return path;
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Couldn't save the reference solution!");
            return null;
        }
    }
}
