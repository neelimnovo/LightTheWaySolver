package cpp.solver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class SolverInput {
    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Read JSON from stdin
        StringBuilder jsonInput = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            jsonInput.append(line).append("\n");
        }

        // Parse JSON
        Type inputType = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> inputMap = gson.fromJson(jsonInput.toString(), inputType);

        // Output simplified JSON with just the data we need
        Map<String, Object> output = new HashMap<>();
        output.put("gridWidth", inputMap.get("gridWidth"));
        output.put("gridHeight", inputMap.get("gridHeight"));
        output.put("emptySpots", inputMap.get("emptySpots"));
        output.put("receiverSpots", inputMap.get("receiverSpots"));
        output.put("dgoQueue", inputMap.get("dgoQueue"));

        System.out.println(gson.toJson(output));
    }
}
