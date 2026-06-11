#include "Solver.h"
#include <iostream>
#include <sstream>
#include <vector>
#include <string>
#include <memory>

std::string readInput() {
    std::ostringstream oss;
    char c;
    while (std::cin.get(c)) {
        oss << c;
     }
    return oss.str();
}

int main() {
    try {
        std::string input = readInput();

         // Parse JSON input manually
        size_t pos = 0;
        int gridWidth = 0, gridHeight = 0;
        std::vector<std::pair<int, int>> emptySpots;
        std::vector<std::pair<int, int>> receiverSpots;
        std::vector<std::string> dgoTypes;
        std::vector<int> dgoOrientations;
        std::vector<int> dgoColours;

        while (pos < input.size() && input[pos] != '}') {
            while (pos < input.size() && std::isspace(input[pos])) pos++;
            if (pos >= input.size() || input[pos] == '}') break;
            if (input[pos] == ',') { pos++; while (pos < input.size() && std::isspace(input[pos])) pos++; }
            if (pos >= input.size() || input[pos] != '"') break;

            size_t keyStart = pos + 1;
            size_t keyEnd = input.find('"', keyStart);
            if (keyEnd == std::string::npos) break;
            std::string key = input.substr(keyStart, keyEnd - keyStart);
            pos = keyEnd + 1;

            while (pos < input.size() && std::isspace(input[pos])) pos++;
            if (pos >= input.size() || input[pos] != ':') break;
            pos++;
            while (pos < input.size() && std::isspace(input[pos])) pos++;

            if (key == "gridWidth") {
                size_t numStart = pos;
                while (pos < input.size() && (std::isdigit(input[pos]) || input[pos] == '-')) pos++;
                gridWidth = std::stoi(input.substr(numStart, pos - numStart));
            } else if (key == "gridHeight") {
                size_t numStart = pos;
                while (pos < input.size() && (std::isdigit(input[pos]) || input[pos] == '-')) pos++;
                gridHeight = std::stoi(input.substr(numStart, pos - numStart));
            } else if (key == "emptySpots") {
                size_t arrStart = pos;
                while (pos < input.size() && input[pos] != ']') {
                    while (pos < input.size() && std::isspace(input[pos])) pos++;
                    if (pos >= input.size() || input[pos] == ']') break;
                    if (input[pos] == ',') { pos++; while (pos < input.size() && std::isspace(input[pos])) pos++; }
                    if (pos >= input.size()) break;
                    while (pos < input.size() && input[pos] != '[') pos++;
                    if (pos >= input.size()) break;
                    pos++;
                    size_t xStart = pos;
                    while (pos < input.size() && std::isdigit(input[pos])) pos++;
                    int x = std::stoi(input.substr(xStart, pos - xStart));
                    while (pos < input.size() && input[pos] != ']') pos++;
                    while (pos < input.size() && std::isspace(input[pos])) pos++;
                    if (pos >= input.size() || input[pos] == ']') break;
                    pos++;
                    size_t yStart = pos;
                    while (pos < input.size() && std::isdigit(input[pos])) pos++;
                    int y = std::stoi(input.substr(yStart, pos - yStart));
                    emptySpots.push_back({x, y});
                }
            } else if (key == "receiverSpots") {
                size_t arrStart = pos;
                while (pos < input.size() && input[pos] != ']') {
                    while (pos < input.size() && std::isspace(input[pos])) pos++;
                    if (pos >= input.size() || input[pos] == ']') break;
                    if (input[pos] == ',') { pos++; while (pos < input.size() && std::isspace(input[pos])) pos++; }
                    if (pos >= input.size()) break;
                    while (pos < input.size() && input[pos] != '[') pos++;
                    if (pos >= input.size()) break;
                    pos++;
                    size_t xStart = pos;
                    while (pos < input.size() && std::isdigit(input[pos])) pos++;
                    int x = std::stoi(input.substr(xStart, pos - xStart));
                    while (pos < input.size() && input[pos] != ']') pos++;
                    while (pos < input.size() && std::isspace(input[pos])) pos++;
                    if (pos >= input.size() || input[pos] == ']') break;
                    pos++;
                    size_t yStart = pos;
                    while (pos < input.size() && std::isdigit(input[pos])) pos++;
                    int y = std::stoi(input.substr(yStart, pos - yStart));
                    receiverSpots.push_back({x, y});
                }
            } else if (key == "dgoQueue") {
                size_t objStart = pos;
                while (pos < input.size() && input[pos] != ']') {
                    while (pos < input.size() && std::isspace(input[pos])) pos++;
                    if (pos >= input.size() || input[pos] == ']') break;
                    if (input[pos] == ',') { pos++; while (pos < input.size() && std::isspace(input[pos])) pos++; }
                    if (pos >= input.size() || input[pos] != '{') break;

                    std::string dgoType;
                    int dgoOrient = 0, dgoColour = 0;
                    size_t objEnd = input.find('}', pos);
                    std::string objStr = input.substr(pos, objEnd - pos + 1);

                    size_t innerPos = pos + 1;
                    while (innerPos < objStr.size() && objStr[innerPos] != '}') {
                        while (innerPos < objStr.size() && std::isspace(objStr[innerPos])) innerPos++;
                        if (innerPos >= objStr.size() || objStr[innerPos] == '}') break;
                        if (objStr[innerPos] == ',') { innerPos++; while (innerPos < objStr.size() && std::isspace(objStr[innerPos])) innerPos++; }
                        if (innerPos >= objStr.size()) break;

                        size_t innerKeyStart = innerPos + 1;
                        size_t innerKeyEnd = objStr.find('"', innerKeyStart);
                        if (innerKeyEnd == std::string::npos) break;
                        std::string innerKey = objStr.substr(innerKeyStart, innerKeyEnd - innerKeyStart);
                        innerPos = innerKeyEnd + 1;

                        while (innerPos < objStr.size() && std::isspace(objStr[innerPos])) innerPos++;
                        if (innerPos >= objStr.size() || objStr[innerPos] != ':') break;
                        innerPos++;
                        while (innerPos < objStr.size() && std::isspace(objStr[innerPos])) innerPos++;

                        if (innerKey == "type") {
                            size_t typeStart = innerPos + 1;
                            size_t typeEnd = objStr.find('"', typeStart);
                            dgoType = objStr.substr(typeStart, typeEnd - typeStart);
                            innerPos = typeEnd + 1;
                        } else if (innerKey == "orientation") {
                            size_t numStart = innerPos;
                            while (innerPos < objStr.size() && (std::isdigit(objStr[innerPos]) || objStr[innerPos] == '-')) innerPos++;
                            dgoOrient = std::stoi(objStr.substr(numStart, innerPos - numStart));
                        } else if (innerKey == "colour") {
                            size_t numStart = innerPos;
                            while (innerPos < objStr.size() && (std::isdigit(objStr[innerPos]) || objStr[innerPos] == '-')) innerPos++;
                            dgoColour = std::stoi(objStr.substr(numStart, innerPos - numStart));
                        }
                    }
                    dgoTypes.push_back(dgoType);
                    dgoOrientations.push_back(dgoOrient);
                    dgoColours.push_back(dgoColour);
                    pos = objEnd + 1;
                }
            }
        }

         // Create solver
        Solver solver(gridWidth, gridHeight);

         // Create DGOs
        std::vector<DynamicGridObject*> dgos;
        for (size_t i = 0; i < dgoTypes.size(); ++i) {
            DynamicGridObject* dgo = createDGO(dgoTypes[i], dgoOrientations[i], dgoColours[i]);
            if (dgo) dgos.push_back(dgo);
        }

         // Initialize solver
        solver.initialize(emptySpots, receiverSpots, dgos);

         // Run solver
        SolverResult result = solver.solve();

         // Cleanup DGOs
        for (auto dgo : dgos) {
            delete dgo;
        }

         // Output result as JSON
        std::ostringstream oss;
        oss << "{\n";
        oss << "    \"solutionFound\": " << (result.solutionFound ? "true" : "false") << ",\n";
        oss << "    \"attemptPermutations\": " << result.attemptPermutations << ",\n";
        oss << "    \"totalPermutations\": " << result.totalPermutations << ",\n";
        oss << "    \"timeSpent\": " << result.timeSpent << ",\n";
        oss << "    \"solutionGrid\": [\n";

        for (size_t i = 0; i < result.solutionGrid.size(); ++i) {
            oss << "      [";
            for (size_t j = 0; j < result.solutionGrid[i].size(); ++j) {
                oss << "\"" << result.solutionGrid[i][j] << "\"";
                if (j < result.solutionGrid[i].size() - 1) oss << ", ";
            }
            oss << "]";
            if (i < result.solutionGrid.size() - 1) oss << ",";
            oss << "\n";
        }

        oss << "     ]\n";
        oss << "}\n";

        std::cout << oss.str() << std::endl;

     } catch (const std::exception& e) {
        std::cerr << "Error: " << e.what() << std::endl;
        std::cerr << "{\"solutionFound\":false,\"attemptPermutations\":0,\"totalPermutations\":0,\"timeSpent\":0}" << std::endl;
        return 1;
     }

    return 0;
}
