//Coded in Java

import java.util.*;

// Enum for tile types
enum TileType {
    FULL_BLOCK,
    OUTER_BOUNDARY,
    EL_SHAPE
}

// Class representing a tile
class Tile {
    private TileType type;
    private int[] position;

    public Tile(TileType type, int[] position) {
        this.type = type;
        this.position = position;
    }

    public TileType getType() {
        return type;
    }

    public int[] getPosition() {
        return position;
    }
}

class TilePlacement {
   private static final int FULL_BLOCK_THRESHOLD = 13;
  private static final int OUTER_BOUNDARY_THRESHOLD = 6;
  private static final int EL_THRESHOLD = 3;
    private static final int SUBGRID_SIZE = 4; // Sub grid size can be modified size as needed
    private int[][] landscape;
    private Map<TileType, Integer> tiles;
    private Map<Integer, Integer> targets;
    private ArrayList<Tile> placements;

    public TilePlacement(int[][] landscape, Map<TileType, Integer> tiles, Map<Integer, Integer> targets) {
        this.landscape = landscape;
        this.tiles = tiles;
        this.targets = targets;
        this.placements = new ArrayList<>();
    }

    // Method to display the current landscape
    private void displayLandscape() {
        for (int i = 0; i < landscape.length; i++) {
            for (int j = 0; j < landscape[0].length; j++) {
                System.out.print(landscape[i][j] + " ");
            }
            System.out.println();
        }
    }

    public Map<Integer, Integer> solve() {
        if (tiles.isEmpty()) {
            System.out.println("No tiles to place.");
            return null;
        }

        // Divide the landscape into subgrids and determine operation for each subgrid
        List<Map<Integer, Integer>> subgridFrequencies = new ArrayList<>();
        for (int i = 0; i < landscape.length; i += SUBGRID_SIZE) {
            for (int j = 0; j < landscape[0].length; j += SUBGRID_SIZE) {
                // Extract subgrid
                int[][] subgrid = extractSubgrid(i, j);

                // Calculate tile frequency in the subgrid
                Map<Integer, Integer> subgridFrequency = calculateTileFrequency(subgrid);

                // Determine operation type for the subgrid based on the subgrid target quantities
                TileType operationType = determineOperationType(subgridFrequency);

                // Perform operation on the subgrid
                performOperation(subgrid, operationType);

                // Add the frequency of the modified subgrid to the list
                subgridFrequencies.add(calculateTileFrequency(subgrid));
            }
        }

        // Combine frequencies from all subgrids
        Map<Integer, Integer> frequencyMap = combineSubgridFrequencies(subgridFrequencies);

        // Display the final tile frequency
        displayTileFrequency(frequencyMap);

        // Perform AC3 for constraint propagation
        if (!AC3()) {
        //    System.out.println("AC3: No solution found.");
            return null;
        }

        System.out.println("Solution found.");
        return frequencyMap;
    }

    // Method to extract a subgrid from the landscape starting at position (startX, startY)
    private int[][] extractSubgrid(int startX, int startY) {
        int[][] subgrid = new int[SUBGRID_SIZE][SUBGRID_SIZE];
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            System.arraycopy(landscape[startX + i], startY, subgrid[i], 0, SUBGRID_SIZE);
        }
        return subgrid;
    }

    // Method to combine frequencies from all subgrids into a single frequency map
    private Map<Integer, Integer> combineSubgridFrequencies(List<Map<Integer, Integer>> subgridFrequencies) {
        Map<Integer, Integer> combinedFrequency = new HashMap<>();
        for (Map<Integer, Integer> subgridFrequency : subgridFrequencies) {
            for (Map.Entry<Integer, Integer> entry : subgridFrequency.entrySet()) {
                combinedFrequency.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return combinedFrequency;
    }

    private TileType determineOperationType(Map<Integer, Integer> subgridFrequency) {
        // Analyze the subgrid target quantities to determine the appropriate operation type
        for (Map.Entry<Integer, Integer> entry : subgridFrequency.entrySet()) {
            int tileNumber = entry.getKey();
            int tileCount = entry.getValue();
            if (tileCount >= FULL_BLOCK_THRESHOLD && tiles.get(TileType.FULL_BLOCK) > 0) {
                return TileType.FULL_BLOCK;
            } else if (tileCount >= OUTER_BOUNDARY_THRESHOLD && tiles.get(TileType.OUTER_BOUNDARY) > 0) {
                return TileType.OUTER_BOUNDARY;
            } else if (tileCount > 0 && tiles.get(TileType.EL_SHAPE) > 0) {
                // Add condition for EL_SHAPE based on tile count
                if (tileCount < EL_THRESHOLD) { // Adjust EL_THRESHOLD according to your requirement
                    return TileType.EL_SHAPE;
                }
            }
        }
        return null;
    }



    private void performOperation(int[][] subgrid, TileType operationType) {
        // Perform the chosen operation based on the operation type
        switch (operationType) {
            case FULL_BLOCK:
                placeFullBlock(subgrid);
                break;
            case OUTER_BOUNDARY:
                placeOuterBoundary(subgrid);
                break;
            case EL_SHAPE:
                placeELShape(subgrid);
                break;
            default:
                System.out.println("Invalid operation type.");
                break;
        }
    }

    private Map<Integer, Integer> calculateTileFrequency(int[][] subgrid) {
        Map<Integer, Integer> frequencyMap = new HashMap<>();
        // Initialize frequency map
        for (int i = 0; i <= 4; i++) {
            frequencyMap.put(i, 0);
        }

        for (int[] row : subgrid) {
            for (int tile : row) {
                frequencyMap.put(tile, frequencyMap.get(tile) + 1);
            }
        }
        return frequencyMap;
    }

    public Map<Integer, Integer> displayTileFrequency(Map<Integer, Integer> frequencyMap) {
        // Display frequency of each tile type
        System.out.println("Tile Frequency:");
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            System.out.println("Tile " + entry.getKey() + ": " + entry.getValue());
        }

        return frequencyMap;
    }

    // Method to place EL_SHAPE tiles
    private void placeELShape(int[][] subgrid) {
        int countRow4 = 0;
        int countColumn1 = 0;
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            if (subgrid[3][i] != 0) {
                countRow4++;
            }
            if (subgrid[i][0] != 0) {
                countColumn1++;
            }
        }

        // Decide which side to remove based on tile counts
        if (countRow4 >= countColumn1) {
            // Remove row 4 and column 1
            for (int i = 0; i < SUBGRID_SIZE; i++) {
                subgrid[3][i] = 0; // Row 4
                subgrid[i][0] = 0; // Column 1
            }
        } else {
            // Remove row 1 and column 4
            for (int i = 0; i < SUBGRID_SIZE; i++) {
                subgrid[0][i] = 0; // Row 1
                subgrid[i][3] = 0; // Column 4
            }
        }

        System.out.println("Placed EL_SHAPE and adjusted based on tile counts.");
        // Display the landscape after placing EL_SHAPE tile and adjusting based on tile counts
        System.out.println("Updated Subgrid:");
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                System.out.print(subgrid[i][j] + " ");
            }
            System.out.println();
        }
    }

    // Method to place FULL_BLOCK tiles
    private void placeFullBlock(int[][] subgrid) {
        // Implement logic to place FULL_BLOCK tiles
        // For demonstration, placing FULL_BLOCK tiles at random positions
        for (int x = 0; x < SUBGRID_SIZE; x++) {
            for (int y = 0; y < SUBGRID_SIZE; y++) {
                subgrid[x][y] = TileType.FULL_BLOCK.ordinal(); // Assuming ordinal values represent FULL_BLOCK
            }
        }

        System.out.println("Placed FULL_BLOCK covering the entire subgrid.");

        // Display the landscape after placing FULL_BLOCK tile
        System.out.println("Updated Subgrid:");
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                System.out.print(subgrid[i][j] + " ");
            }
            System.out.println();
        }
    }

    // Method to place OUTER_BOUNDARY tiles
    private void placeOuterBoundary(int[][] subgrid) {
        // Implement logic to place OUTER_BOUNDARY tiles
        // For demonstration, placing OUTER_BOUNDARY tiles at random positions
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            subgrid[i][0] = 0; // Left column
            subgrid[i][SUBGRID_SIZE - 1] = 0; // Right column
        }

        for (int i = 0; i < SUBGRID_SIZE; i++) {
            subgrid[0][i] = 0; // Top row
            subgrid[SUBGRID_SIZE - 1][i] = 0; // Bottom row
        }

        System.out.println("Placed OUTER_BOUNDARY and replaced outer boundaries with 0.");

        // Display the landscape after placing OUTER_BOUNDARY tiles and replacing outer boundaries with 0
        System.out.println("Updated Subgrid:");
        for (int i = 0; i < SUBGRID_SIZE; i++) {
            for (int j = 0; j < SUBGRID_SIZE; j++) {
                System.out.print(subgrid[i][j] + " ");
            }
            System.out.println();
        }
    }

    // AC3 algorithm for constraint propagation
    private boolean AC3() {
        Queue<int[]> queue = new LinkedList<>();
        for (int i = 0; i < landscape.length; i++) {
            for (int j = 0; j < landscape[0].length; j++) {
                if (landscape[i][j] == 0) {
                    queue.offer(new int[]{i, j});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int x = cell[0];
            int y = cell[1];
            boolean revised = revise(x, y);
            if (revised) {
                if (tiles.isEmpty()) {
                    return true; // Solution found
                }
                for (int i = 0; i < landscape.length; i++) {
                    for (int j = 0; j < landscape[0].length; j++) {
                        if (landscape[i][j] == 0) {
                            queue.offer(new int[]{i, j});
                        }
                    }
                }
            }
        }
        return false; // No solution found
    }

    private boolean revise(int x, int y) {
        boolean revised = false;
        for (Map.Entry<TileType, Integer> entry : tiles.entrySet()) {
            TileType type = entry.getKey();
            int remaining = entry.getValue();
            if (type == TileType.EL_SHAPE) {
                if (x + 1 < landscape.length && landscape[x + 1][y] == 0) {
                    if (remaining == 0) {
                        landscape[x + 1][y] = -1; // Assigning -1 to indicate invalid position for EL_SHAPE
                        revised = true;
                    }
                }
                if (y + 1 < landscape[0].length && landscape[x][y + 1] == 0) {
                    if (remaining == 0) {
                        landscape[x][y + 1] = -1; // Assigning -1 to indicate invalid position for EL_SHAPE
                        revised = true;
                    }
                }
            } else {
                if (remaining == 0) {
                    landscape[x][y] = -1; // Assigning -1 to indicate invalid position for the tile type
                    revised = true;
                }
            }
        }
        return revised;
    }
}

public class Main {
    public static void main(String[] args) {
        // Example landscapes, tiles, and targets
        int[][] landscape = {
                {2, 2, 1, 3, 0, 3, 4, 0, 4, 2, 2, 4, 0, 4, 0, 0, 0, 2, 4, 3},
                {0, 2, 1, 3, 2, 2, 0, 0, 0, 1, 0, 1, 2, 2, 0, 2, 3, 4, 4, 4},
                {2, 1, 0, 0, 0, 2, 2, 4, 3, 2, 2, 2, 4, 4, 4, 4, 0, 0, 0, 0},
                {0, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 0, 0, 0, 1},
                {4, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 0, 0, 0, 2},
                {2, 2, 2, 2, 1, 1, 1, 1, 4, 4, 4, 4, 3, 3, 3, 3, 0, 0, 0, 2},
                {4, 2, 2, 2, 1, 1, 1, 1, 4, 4, 4, 4, 3, 3, 3, 3, 0, 0, 0, 4},
                {2, 2, 2, 2, 1, 1, 1, 1, 4, 4, 4, 4, 3, 3, 3, 3, 0, 0, 0, 0},
                {1, 2, 2, 2, 1, 1, 1, 1, 4, 4, 4, 4, 3, 3, 3, 3, 0, 0, 0, 0},
                {0, 2, 2, 2, 1, 1, 1, 1, 4, 4, 4, 4, 3, 3, 3, 3, 0, 0, 0, 3},
                {0, 3, 3, 3, 4, 4, 4, 4, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 3},
                {1, 3, 3, 3, 4, 4, 4, 4, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 3},
                {2, 3, 3, 3, 4, 4, 4, 4, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 4},
                {4, 3, 3, 3, 4, 4, 4, 4, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 3},
                {3, 3, 3, 3, 4, 4, 4, 4, 1, 1, 1, 1, 2, 2, 2, 2, 0, 0, 0, 0},
                {3, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0, 4},
                {0, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 0, 4},
                {0, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0, 0, 0, 1},
                {3, 4, 4, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 0, 1},
                {4, 0, 3, 4, 0, 4, 3, 4, 3, 2, 2, 2, 1, 4, 3, 4, 3, 0, 0, 1}
        };
        Map<TileType, Integer> tiles = Map.of(TileType.EL_SHAPE, 6, TileType.OUTER_BOUNDARY, 7, TileType.FULL_BLOCK, 12);
        Map<Integer, Integer> targets = Map.of(1, 1, 2, 1, 3, 1, 4, 2);

        // Solve the problem
        TilePlacement solver = new TilePlacement(landscape, tiles, targets);
        solver.solve();
    }
}

