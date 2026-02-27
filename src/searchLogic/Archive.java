public class Archive {
        // public boolean solveLevel2(GridCell[][] grid, ArrayList<Pair<Integer, Integer>> emptySpots,
    //                           LinkedList<DynamicGridObject> dgoQueue) {
    //     // Convert the dgoQueue into an arraylist
    //     ArrayList<DynamicGridObject> dgoArrayList = new ArrayList<>(dgoQueue);

    //     // Solve level using a for loop
    //     GridCell[][] loopGrid = updateGridCellArray(grid);
    //     //for each DGO
    //     // place them onto the grid one by one, by checking filtered Empty spots


    //     for (DynamicGridObject dgo : dgoArrayList) {
    //         ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(loopGrid, this.emptySpots);

    //         int spotX = spot.getKey();
    //         int spotY = spot.getValue();
    //         if (loopGrid[spotX][spotY].cellDynamicItem == null) {
    //                 trackLightSources(dgo, spotX, spotY);
    //                 loopGrid[spotX][spotY].cellDynamicItem = dgo;
    //                 LinkedList<DynamicGridObject> copyQueue = new LinkedList<>(dgoQueue);
    //                 boolean isSolutionFound = solveLevel2(copyGrid, this.emptySpots, copyQueue);
    //                 if (isSolutionFound) {
    //                     return true;
    //                 }
    //                 // grid[spotX][spotY].cellDynamicItem = null;
    //             }

    //         for (Pair<Integer, Integer> spot : filteredEmptySpots) {

    //             int spotX = spot.getKey();
    //             int spotY = spot.getValue();
    //             if (loopGrid[spotX][spotY].cellDynamicItem == null) {
    //                 trackLightSources(dgo, spotX, spotY);
    //                 loopGrid[spotX][spotY].cellDynamicItem = dgo;
    //                 LinkedList<DynamicGridObject> copyQueue = new LinkedList<>(dgoQueue);
    //                 boolean isSolutionFound = solveLevel2(copyGrid, this.emptySpots, copyQueue);
    //                 if (isSolutionFound) {
    //                     return true;
    //                 }
    //                 // grid[spotX][spotY].cellDynamicItem = null;
    //             }
    //         }
    //     }
                                
    //     if (!dgoQueue.isEmpty()) {
    //         // System.out.println(!dgoQueue.isEmpty());
    //         DynamicGridObject dgo = dgoQueue.remove();
    //         ArrayList<Pair<Integer, Integer>> filteredEmptySpots = dgo.filter(grid, this.emptySpots);
    //         // System.out.println(dgoQueue + " " + dgoQueue.size());
    //         // System.out.println(dgo + " " + filteredEmptySpots.size());
    //         // Collections.shuffle(filteredEmptySpots);
    //         for (Pair<Integer, Integer> spot : filteredEmptySpots) {
    //             int spotX = spot.getKey();
    //             int spotY = spot.getValue();
    //             if (grid[spotX][spotY].cellDynamicItem == null) {
    //                 trackLightSources(dgo, spotX, spotY);
    //                 GridCell[][] copyGrid = copyGridCellArray(grid);
    //                 copyGrid[spotX][spotY].cellDynamicItem = dgo;
    //                 LinkedList<DynamicGridObject> copyQueue = new LinkedList<>(dgoQueue);
    //                 boolean isSolutionFound = solveLevel2(copyGrid, this.emptySpots, copyQueue);
    //                 if (isSolutionFound) {
    //                     return true;
    //                 }
    //                 // grid[spotX][spotY].cellDynamicItem = null;
    //             }
    //         }
    //     } else {
    //         return projectLight(grid);
    //     }
    //     // // unable to convince compiler that this statement is unreachable because of dynamic recursion
    //     return false;
    // }
    
}
