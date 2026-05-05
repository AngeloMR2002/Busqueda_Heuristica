import java.util.*;

/**
 * Algoritmo de Búsqueda Tabú (Tabu Search) para N-Reinas.
 * 
 * Exploración COMPLETA del vecindario (todas las columnas × todas las filas).
 * Lista tabú almacena MOVIMIENTOS INVERSOS ("col:filaOriginal") usando Queue
 * con tamaño máximo de 20 para prohibir regresar una reina a su posición anterior.
 * Máximo 100 iteraciones.
 */
public class TabuSearch {

    private static final int TABU_SIZE = 20;
    private static final int MAX_ITERATIONS = 100;

    public static String solve(int n, int maxIterations, int tabuSize) {
        Random rand = new Random();
        long startTime = System.nanoTime();
        StringBuilder stepsJson = new StringBuilder();
        int stepCount = 0;

        NQueensState current = NQueensState.random(n, rand);
        int currentConflicts = current.calculateConflicts();
        NQueensState best = current.copy();
        int bestConflicts = currentConflicts;
        boolean solved = currentConflicts == 0;
        int finalIteration = 0;

        // Lista tabú como Queue: almacena movimientos inversos "col:filaOriginal"
        Queue<String> tabuList = new LinkedList<>();

        // Registrar estado inicial
        stepsJson.append(formatStep(0, current, currentConflicts, 0));
        stepCount++;

        for (int iter = 1; iter <= maxIterations && !solved; iter++) {
            int[] mejorVecino = null;
            int mejorHVecino = Integer.MAX_VALUE;
            String mejorMovimientoInverso = "";
            finalIteration = iter;

            // Exploración COMPLETA del vecindario
            for (int col = 0; col < n; col++) {
                int filaOriginal = current.getQueens()[col];
                for (int filaNueva = 0; filaNueva < n; filaNueva++) {
                    if (filaNueva == filaOriginal) continue;

                    current.getQueens()[col] = filaNueva;
                    int h = current.calculateConflicts();

                    // El movimiento inverso: "col:filaOriginal" (prohibir volver)
                    String movimientoInverso = col + ":" + filaOriginal;

                    // Solo aceptar si NO está en la lista tabú
                    if (!tabuList.contains(movimientoInverso) && h < mejorHVecino) {
                        mejorHVecino = h;
                        mejorVecino = Arrays.copyOf(current.getQueens(), n);
                        mejorMovimientoInverso = movimientoInverso;
                    }

                    current.getQueens()[col] = filaOriginal; // Restaurar
                }
            }

            if (mejorVecino == null) break; // No hay movimiento válido

            // Aplicar el mejor movimiento
            for (int i = 0; i < n; i++) {
                current.getQueens()[i] = mejorVecino[i];
            }
            currentConflicts = mejorHVecino;

            // Agregar movimiento inverso a la lista tabú
            tabuList.add(mejorMovimientoInverso);
            if (tabuList.size() > tabuSize) tabuList.poll();

            // Actualizar mejor solución global
            if (currentConflicts < bestConflicts) {
                best = current.copy();
                bestConflicts = currentConflicts;
            }

            // Registrar paso
            if (stepCount < 500) {
                stepsJson.append(",");
                stepsJson.append(formatStep(iter, current, currentConflicts, tabuList.size()));
                stepCount++;
            }

            if (currentConflicts == 0) {
                solved = true;
            }
        }

        long endTime = System.nanoTime();
        double timeMs = (endTime - startTime) / 1_000_000.0;

        return buildResult("Tabu Search", n, solved, stepsJson.toString(),
                finalIteration, timeMs, bestConflicts, best.toJsonArray());
    }

    private static String formatStep(int iteration, NQueensState state, int conflicts, int tabuSize) {
        return String.format(
            "{\"iteration\":%d,\"state\":%s,\"conflicts\":%d,\"tabuListSize\":%d}",
            iteration, state.toJsonArray(), conflicts, tabuSize
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
