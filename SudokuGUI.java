import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.Border;

public class SudokuGUI {

    private static final int GRID_SIZE = 9; // 9x9 Sudoku grid
    private static final int SUB_GRID_SIZE = 3; // 3x3 sub-grid

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SudokuGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Sudoku Solver");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 700); // Increased height for buttons
        frame.setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        JTextField[][] cells = new JTextField[GRID_SIZE][GRID_SIZE];

        // Initialize cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                JTextField cell = new JTextField();
                cell.setHorizontalAlignment(JTextField.CENTER);
                cell.setFont(new Font("Monospaced", Font.PLAIN, 40));

                // Disable text cursor and make the background change on focus
                cell.setCaretColor(Color.LIGHT_GRAY); // Make cursor "invisible"
                cell.addFocusListener(new CellHighlighter(cell));

                // Add input constraint to allow only single digits 0-9
                cell.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyTyped(KeyEvent e) {
                        if (!Character.isDigit(e.getKeyChar()) || cell.getText().length() >= 1) {
                            e.consume(); // Ignore non-digit or multi-character input
                        }
                    }
                });

                // Set bold borders for 3x3 sub-grids
                Border border = BorderFactory.createMatteBorder(
                        row % SUB_GRID_SIZE == 0 ? 3 : 1, // Top
                        col % SUB_GRID_SIZE == 0 ? 3 : 1, // Left
                        (row + 1) % SUB_GRID_SIZE == 0 ? 3 : 1, // Bottom
                        (col + 1) % SUB_GRID_SIZE == 0 ? 3 : 1, // Right
                        Color.BLACK
                );
                cell.setBorder(border);

                cells[row][col] = cell;
                gridPanel.add(cell);
            }
        }

        frame.add(gridPanel, BorderLayout.CENTER); // Add the grid panel to the center

        // Create a panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        JLabel stepCounterLabel = new JLabel(" # of recursions: 0");
        stepCounterLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(e -> {
            int[][] grid = new int[GRID_SIZE][GRID_SIZE];
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    String text = cells[row][col].getText();
                    grid[row][col] = text.isEmpty() ? 0 : Integer.parseInt(text);
                }
            }
            new SolveWorker(grid, cells, stepCounterLabel).execute(); // Start the solving process
        });
        buttonPanel.add(solveButton);

        // Add a "Clear" button
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> {
            // Clear all cells
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    cells[row][col].setText(""); // Clear each cell
                }
            }
        });
        buttonPanel.add(clearButton);

        // Add a "Load" button (for demonstration)
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> {
            // Load a predefined Sudoku puzzle
            int[][] samplePuzzle = {
                    {5, 3, 0, 0, 7, 0, 0, 0, 0},
                    {6, 0, 0, 1, 9, 5, 0, 0, 0},
                    {0, 9, 8, 0, 0, 0, 0, 6, 0},
                    {8, 0, 0, 0, 6, 0, 0, 0, 3},
                    {4, 0, 0, 8, 0, 3, 0, 0, 1},
                    {7, 0, 0, 0, 2, 0, 0, 0, 6},
                    {0, 6, 0, 0, 0, 0, 2, 8, 0},
                    {0, 0, 0, 4, 1, 9, 0, 0, 5},
                    {0, 0, 0, 0, 8, 0, 0, 7, 9}
            };
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (samplePuzzle[row][col] != 0) {
                        cells[row][col].setText(String.valueOf(samplePuzzle[row][col]));
                    } else {
                        cells[row][col].setText(""); // Clear the cell for empty spaces
                    }
                }
            }
        });
        buttonPanel.add(loadButton);
        buttonPanel.add(stepCounterLabel, BorderLayout.CENTER);

        frame.add(buttonPanel, BorderLayout.SOUTH); // Add the button panel to the bottom

        frame.setVisible(true);
    }

    // SwingWorker to solve the Sudoku puzzle
    static class SolveWorker extends SwingWorker<Void, int[][]> {
        private final int[][] grid;
        private final JTextField[][] cells;
        private final JLabel stepCounterLabel;
        private int steps = 0;

        public SolveWorker(int[][] grid, JTextField[][] cells, JLabel stepCounterLabel) {
            this.grid = grid;
            this.cells = cells;
            this.stepCounterLabel = stepCounterLabel;
        }

        @Override
        protected Void doInBackground() throws Exception {
            solve();
            return null;
        }

        private boolean isValid(int row, int col, int value) {
            for (int i = 0; i < GRID_SIZE; i++) {
                if (grid[row][i] == value || grid[i][col] == value) {
                    return false;
                }
            }
            int startRow = row - row % SUB_GRID_SIZE;
            int startCol = col - col % SUB_GRID_SIZE;
            for (int i = startRow; i < startRow + SUB_GRID_SIZE; i++) {
                for (int j = startCol; j < startCol + SUB_GRID_SIZE; j++) {
                    if (grid[i][j] == value) {
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean solve() throws InterruptedException {
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (grid[row][col] != 0) continue; // Skip filled cells
                    for (int value = 1; value <= GRID_SIZE; value++) {
                        if (isValid(row, col, value)) {
                            grid[row][col] = value; // Place the number
                            steps++;
                            publish(grid); // Publish the current state of the grid
                            Thread.sleep(1); // Delay for visualization
                            if (solve()) {
                                return true; // Found a solution
                            } else {
                                grid[row][col] = 0; // Reset on backtrack
                            }
                        }
                    }
                    return false; // No valid number found, backtrack
                }
            }
            return true; // Solved
        }

        @Override
        protected void process(java.util.List<int[][]> chunks) {
            for (int[][] gridState : chunks) {
                for (int row = 0; row < GRID_SIZE; row++) {
                    for (int col = 0; col < GRID_SIZE; col++) {
                        if (gridState[row][col] != 0) {
                            cells[row][col].setText(String.valueOf(gridState[row][col]));
                        }
                    }
                }
            }
            stepCounterLabel.setText(" # of recursions: " + steps);
        }

        @Override
        protected void done() {
            JOptionPane.showMessageDialog(null, "Sudoku solved!");
        }
    }
}

// CellHighlighter to change the background color when a cell is focused
class CellHighlighter implements FocusListener {
    private final JTextField cell;

    public CellHighlighter(JTextField cell) {
        this.cell = cell;
    }

    @Override
    public void focusGained(FocusEvent e) {
        cell.setBackground(Color.LIGHT_GRAY); // Highlight the cell when focused
    }

    @Override
    public void focusLost(FocusEvent e) {
        cell.setBackground(Color.WHITE); // Reset the background when focus is lost
    }
}

class SudokuClass {
    private static final int GRID_SIZE = 9;
    private static final int SUB_GRID_SIZE = 3;
    private final int[][] grid;

    public SudokuClass(int[][] grid) {
        this.grid = grid;
    }

    public boolean isValid(int row, int col, int value) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (grid[row][i] == value || grid[i][col] == value) {
                return false;
            }
        }
        int startRow = row - row % SUB_GRID_SIZE;
        int startCol = col - col % SUB_GRID_SIZE;
        for (int i = startRow; i < startRow + SUB_GRID_SIZE; i++) {
            for (int j = startCol; j < startCol + SUB_GRID_SIZE; j++) {
                if (grid[i][j] == value) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean solve() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] != 0) continue;
                for (int value = 1; value <= GRID_SIZE; value++) {
                    if (isValid(row, col, value)) {
                        grid[row][col] = value;
                        if (solve()) {
                            return true;
                        } else {
                            grid[row][col] = 0;
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }

    public int[][] getGrid() {
        return grid;
    }
}
