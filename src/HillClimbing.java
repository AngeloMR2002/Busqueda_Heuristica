import java.util.Random;

/**
 * Algoritmo de Escalando la Colina - Versión PURA (Steepest-Ascent Hill Climbing).
 * 
 * En cada iteración, evalúa todos los vecinos posibles y selecciona
 * el que tiene menor número de conflictos. Si no hay mejora, se detiene
 * (óptimo local). NO tiene reinicios aleatorios.
 * 
 * Tasa de éxito esperada para N=8: ~14-16% (teórico).
 */
public class HillClimbing {

    public static String solve(int n, int maxIterations) {
        Random rand = new Random();
        long startTime = System.nanoTime();
        StringBuilder stepsJson = new StringBuilder();
        int stepCount = 0;

        NQueensState current = NQueensState.random(n, rand);
        int currentConflicts = current.calculateConflicts();
        boolean solved = currentConflicts == 0;
        int finalIteration = 0;

        // Registrar estado inicial
        stepsJson.append(formatStep(0, current, currentConflicts));
        stepCount++;

        for (int iter = 1; iter <= maxIterations && !solved; iter++) {
            int bestCol = -1, bestRow = -1;
            int bestNeighborConflicts = currentConflicts;

            // Evaluar TODOS los vecinos (mover cada reina a cada fila posible)
            for (int col = 0; col < n; col++) {
                int origRow = current.getQueens()[col];
                for (int row = 0; row < n; row++) {
                    if (row == origRow) continue;
                    current.getQueens()[col] = row;
                    int nc = current.calculateConflicts();
                    if (nc < bestNeighborConflicts) {
                        bestNeighborConflicts = nc;
                        bestCol = col;
                        bestRow = row;
                    }
                }
                current.getQueens()[col] = origRow;
            }

            // Si no hay mejora -> óptimo local, se detiene
            if (bestCol == -1) {
                finalIteration = iter;
                break;
            }

            // Aplicar el mejor movimiento
            current.getQueens()[bestCol] = bestRow;
            currentConflicts = bestNeighborConflicts;
            finalIteration = iter;

            // Registrar paso
            if (stepCount < 500) {
                stepsJson.append(",");
                stepsJson.append(formatStep(iter, current, currentConflicts));
                stepCount++;
            }

            if (currentConflicts == 0) {
                solved = true;
            }
        }

        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;

        return buildResult("Hill Climbing", n, solved, stepsJson.toString(),
                finalIteration, timeMs, currentConflicts, current.toJsonArray());
    }

    private static String formatStep(int iteration, NQueensState state, int conflicts) {
        return String.format(
            "{\"iteration\":%d,\"state\":%s,\"conflicts\":%d}",
            iteration, state.toJsonArray(), conflicts
        );
    }

    private static String buildResult(String algo, int n, boolean solved, String steps,
                                       int totalIter, double timeMs, int finalConf, String finalState) {
        return String.format(
            "{\"algorithm\":\"%s\",\"n\":%d,\"solved\":%s,\"steps\":[%s]," +
            "\"totalIterations\":%d,\"timeMs\":%.2f,\"finalConflicts\":%d,\"finalState\":%s}",
            algo, n, solved, steps, totalIter, timeMs, finalConf, finalState
        );
    }
}
