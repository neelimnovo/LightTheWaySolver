package tests;

import java.util.Random;

public class ArrayLocalityTest {

    private static final int GRID_SIZE = 50;
    private static final int TOTAL_ELEMENTS = GRID_SIZE * GRID_SIZE;
    private static final int NUM_DEREFERENCES = 10_000_000_0; // 10 billion to get measurable time

    public static void main(String[] args) {
        // Allocate 2D array
        int[][] array2D = new int[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                array2D[i][j] = i * GRID_SIZE + j;
            }
        }

        // Allocate 1D array
        int[] array1D = new int[TOTAL_ELEMENTS];
        for (int i = 0; i < TOTAL_ELEMENTS; i++) {
            array1D[i] = i;
        }

        // Generate random indices to prevent compiler from optimizing perfectly predictable loops
        Random random = new Random(42);
        int[] randomX = new int[NUM_DEREFERENCES];
        int[] randomY = new int[NUM_DEREFERENCES];
        for (int i = 0; i < NUM_DEREFERENCES; i++) {
            randomX[i] = random.nextInt(GRID_SIZE);
            randomY[i] = random.nextInt(GRID_SIZE);
        }

        // Warmup JVM
        long sumWarmup = 0;
        for (int i = 0; i < NUM_DEREFERENCES; i++) {
            sumWarmup += array2D[randomX[i]][randomY[i]];
            int index = randomX[i] * GRID_SIZE + randomY[i];
            sumWarmup += array1D[index];
        }
        System.out.println("Warmup Sum (ignore): " + sumWarmup);

        // Test 2D Array
        long startTime2D = System.nanoTime();
        long sum2D = 0;
        for (int i = 0; i < NUM_DEREFERENCES; i++) {
            sum2D += array2D[randomX[i]][randomY[i]];
        }
        long endTime2D = System.nanoTime();
        double time2Dms = (endTime2D - startTime2D) / 1_000_000.0;

        // Test 1D Array
        long startTime1D = System.nanoTime();
        long sum1D = 0;
        for (int i = 0; i < NUM_DEREFERENCES; i++) {
            int index = randomX[i] * GRID_SIZE + randomY[i];
            sum1D += array1D[index];
        }
        long endTime1D = System.nanoTime();
        double time1Dms = (endTime1D - startTime1D) / 1_000_000.0;

        // Output results
        System.out.println("--- Array Performance Test Results ---");
        System.out.println("Number of dereferences: " + NUM_DEREFERENCES);
        System.out.println("2D Array Time: " + time2Dms + " ms");
        System.out.println("1D Array Time: " + time1Dms + " ms");
        System.out.println("Sum 2D (to prevent dead-code elimination): " + sum2D);
        System.out.println("Sum 1D (to prevent dead-code elimination): " + sum1D);
        
        if (time1Dms < time2Dms) {
            System.out.println(String.format("Result: 1D array is %.2fx faster.", (time2Dms / time1Dms)));
        } else {
            System.out.println(String.format("Result: 2D array is %.2fx faster.", (time1Dms / time2Dms)));
        }
    }
}
