import java.util.Arrays;
import java.util.Random;

/**
 * Representa el estado del tablero de N-Reinas.
 * queens[i] = fila de la reina en la columna i.
 * Esto garantiza que no haya dos reinas en la misma columna.
 */
public class NQueensState {
    private int[] queens;
    private int n;

    public NQueensState(int n) {
        this.n = n;
        this.queens = new int[n];
    }

    public NQueensState(int[] queens) {
        this.n = queens.length;
        this.queens = Arrays.copyOf(queens, queens.length);
    }

    /** Genera un estado aleatorio */
    public static NQueensState random(int n, Random rand) {
        NQueensState state = new NQueensState(n);
        for (int i = 0; i < n; i++) {
            state.queens[i] = rand.nextInt(n);
        }
        return state;
    }

    /**
     * Calcula el número de pares de reinas en conflicto.
     * Dos reinas se atacan si están en la misma fila o diagonal.
     */
    public int calculateConflicts() {
        int conflicts = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (queens[i] == queens[j]) conflicts++;
                if (Math.abs(queens[i] - queens[j]) == Math.abs(i - j)) conflicts++;
            }
        }
        return conflicts;
    }

    public int[] getQueens() { return queens; }
    public int getN() { return n; }

    public NQueensState copy() {
        return new NQueensState(Arrays.copyOf(queens, n));
    }

    /** Serializa el arreglo de reinas a JSON array */
    public String toJsonArray() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(",");
            sb.append(queens[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NQueensState)) return false;
        return Arrays.equals(queens, ((NQueensState) o).queens);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(queens);
    }
}
