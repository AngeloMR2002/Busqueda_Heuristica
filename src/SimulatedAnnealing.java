import java.util.Random;

/**
 * Algoritmo de Recocido Simulado (Simulated Annealing) para N-Reinas.
 * 
 * Comienza con temperatura alta (T=100) y la enfría gradualmente (α=0.99).
 * Acepta movimientos peores con probabilidad e^(-deltaE / T),
 * lo que permite escapar de óptimos locales.
 * Máximo 5000 iteraciones.
 */
public class SimulatedAnnealing {

    public static String solve(int n, double initialTemp, double coolingRate, int maxIterations) {
        Random rand = new Random();
        long startTime = System.nanoTime();
        StringBuilder stepsJson = new StringBuilder();
        int stepCount = 0;

        NQueensState current = NQueensState.random(n, rand);
        int currentConflicts = current.calculateConflicts();
        NQueensState best = current.copy();
        int bestConflicts = currentConflicts;
        double temperature = initialTemp;
        boolean solved = currentConflicts == 0;
        int finalIteration = 0;

        // Registrar estado inicial
        stepsJson.append(formatStep(0, current, currentConflicts, temperature));
        stepCount++;

        int sampleInterval = Math.max(1, maxIterations / 498);

        for (int iter = 1; iter <= maxIterations && !solved && currentConflicts > 0; iter++) {
            // Elegir vecino aleatorio: mover una reina aleatoria a una fila aleatoria
            int col = rand.nextInt(n);
            int filaOriginal = current.getQueens()[col];
            int filaNueva = rand.nextInt(n);

            // Aplicar movimiento temporal
            current.getQueens()[col] = filaNueva;
            int neighborConflicts = current.calculateConflicts();

            int delta = neighborConflicts - currentConflicts;

            // Aceptar si es mejor, o con probabilidad e^(-delta/T) si es peor
            if (delta < 0 || rand.nextDouble() < Math.exp(-delta / temperature)) {
                currentConflicts = neighborConflicts;

                if (currentConflicts < bestConflicts) {
                    best = current.copy();
                    bestConflicts = currentConflicts;
                }
            } else {
                // Deshacer movimiento
                current.getQueens()[col] = filaOriginal;
            }

            temperature *= coolingRate;
            finalIteration = iter;

            // Muestrear pasos para visualización
            boolean record = (iter % sampleInterval == 0) || (currentConflicts == 0);
            if (stepCount < 500 && record) {
                stepsJson.append(",");
                stepsJson.append(formatStep(iter, current, currentConflicts, temperature));
                stepCount++;
            }

            if (currentConflicts == 0) {
                solved = true;
            }
        }

        // Siempre registrar estado final
        if (stepCount > 1) {
            stepsJson.append(",");
        }
        stepsJson.append(formatStep(finalIteration, best, bestConflicts, temperature));

        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;

        return buildResult("Simulated Annealing", n, solved, stepsJson.toString(),
                finalIteration, timeMs, bestConflicts, best.toJsonArray());
    }

    private static String formatStep(int iteration, NQueensState state, int conflicts, double temp) {
        return String.format(java.util.Locale.US,
            "{\"iteration\":%d,\"state\":%s,\"conflicts\":%d,\"temperature\":%.6f}",
            iteration, state.toJsonArray(), conflicts, temp
        );
    }

    private static String buildResult(String algo, int n, boolean solved, String steps,
                                       int totalIter, double timeMs, int finalConf, String finalState) {
        return String.format(java.util.Locale.US,
            "{\"algorithm\":\"%s\",\"n\":%d,\"solved\":%s,\"steps\":[%s]," +
            "\"totalIterations\":%d,\"timeMs\":%.2f,\"finalConflicts\":%d,\"finalState\":%s}",
            algo, n, solved, steps, totalIter, timeMs, finalConf, finalState
        );
    }
}
