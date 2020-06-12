package model;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Level {
    public String title;
    public GridLayout gridLayout;

    public Level(String name, GridLayout layout) {
        this.title = name;
        this.gridLayout = layout;
    }


    public static Level load(String saveFileName) {
        Gson gson = new Gson();
        Level loadedLevel = null;
        try {
            FileReader fileReader = new FileReader("src\\saveFiles\\" + saveFileName+ ".json");
            JsonReader reader = new JsonReader(fileReader);
            loadedLevel = gson.fromJson(reader, Level.class);
        } catch (FileNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        return loadedLevel;
    }


    public static void save(Level level) {
        Gson gson = new Gson();
        String saveFileJSON = gson.toJson(level);
        try {
            FileWriter writer = new FileWriter("src\\saveFiles\\" + level.title + ".json");
            writer.write(saveFileJSON);
            writer.close();
        } catch (IOException e) {
            System.out.println(e);
            System.out.println("Couldn't save the level!");
        }
    }
}
