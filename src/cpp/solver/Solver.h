#ifndef SOLVER_H
#define SOLVER_H

#include "Grid.h"
#include <vector>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstring>

DynamicGridObject* createDGO(const std::string& type, int orientation, int colour);

struct SolverResult {
    bool solutionFound;
    std::vector<std::vector<std::string>> solutionGrid;
    long attemptPermutations;
    long totalPermutations;
    long timeSpent;
};

class Solver {
private:
    Grid* grid;
    std::vector<std::pair<int, int>> receiverSpots;
    std::vector<std::pair<int, int>> emptySpots;
    std::vector<DynamicGridObject*> dgoQueue;
    std::vector<std::pair<int, int>> sourceSpots;
    ShortQueue lightQueue;

    std::vector<int> litSpotX;
    std::vector<int> litSpotY;
    int litCount;

    long long startTime;
    long attemptPermutations;
    long totalPermutations;

    bool areIdenticalDGOs(DynamicGridObject* a, DynamicGridObject* b);
    int emptySpotIndex(const std::pair<int, int>& spot);
    bool solveRecursive(std::vector<DynamicGridObject*>& dgoQueue, int iterationSpotIndex);

public:
    Solver(int width, int height);
    ~Solver();

    void initialize(const std::vector<std::pair<int, int>>& emptySpots,
                     const std::vector<std::pair<int, int>>& receiverSpots,
                     const std::vector<DynamicGridObject*>& dgoData);

    SolverResult solve();

    std::vector<std::vector<std::string>> getSolutionGrid();
};

#endif // SOLVER_H
