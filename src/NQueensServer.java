import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

/**
 * Servidor HTTP para el problema de N-Reinas.
 * Sirve el frontend estático y expone endpoints REST
 * para ejecutar los algoritmos de búsqueda heurística.
 */
public class NQueensServer {

    private static final int PORT = 8080;
    private static String frontendDir;

    public static void main(String[] args) throws Exception {
        frontendDir = System.getProperty("user.dir") + File.separator + "frontend";

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/solve", new SolveHandler());
        server.createContext("/api/compare", new CompareHandler());
        server.createContext("/", new StaticFileHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("=====================================================");
        System.out.println("  N-Queens Heuristic Search Server");
        System.out.println("  http://localhost:" + PORT);
        System.out.println("  Frontend: " + frontendDir);
        System.out.println("=====================================================");
    }

    /** Handler para /api/solve - Ejecuta un algoritmo individual */
    static class SolveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String algorithm = getParam(query, "algorithm", "hillclimbing");
            int n = Integer.parseInt(getParam(query, "n", "8"));

            // Limitar N para evitar tiempos excesivos
            if (n < 4) n = 4;
            if (n > 20) n = 20;

            String result;
            switch (algorithm.toLowerCase()) {
                case "hillclimbing":
                    result = HillClimbing.solve(n, 1000);
                    break;
                case "simulatedannealing":
                    result = SimulatedAnnealing.solve(n, 100.0, 0.99, 5000);
                    break;
                case "tabusearch":
                    result = TabuSearch.solve(n, 100, 20);
                    break;
                default:
                    result = "{\"error\":\"Unknown algorithm: " + algorithm + "\"}";
            }

            sendJsonResponse(exchange, result);
        }
    }

    /** Handler para /api/compare - Ejecuta los 3 algoritmos múltiples veces */
    static class CompareHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            int n = Integer.parseInt(getParam(query, "n", "8"));
            int runs = Integer.parseInt(getParam(query, "runs", "30"));

            if (n < 4) n = 4;
            if (n > 20) n = 20;
            if (runs < 1) runs = 1;
            if (runs > 100) runs = 100;

            String hcStats = runMultiple("hillclimbing", n, runs);
            String saStats = runMultiple("simulatedannealing", n, runs);
            String tsStats = runMultiple("tabusearch", n, runs);

            String result = String.format(
                "{\"n\":%d,\"runs\":%d,\"results\":{\"hillclimbing\":%s,\"simulatedannealing\":%s,\"tabusearch\":%s}}",
                n, runs, hcStats, saStats, tsStats
            );

            sendJsonResponse(exchange, result);
        }

        private String runMultiple(String algorithm, int n, int runs) {
            int successes = 0;
            long totalIter = 0;
            double totalTime = 0;
            long minIter = Long.MAX_VALUE, maxIter = 0;
            double minTime = Double.MAX_VALUE, maxTime = 0;

            for (int i = 0; i < runs; i++) {
                String json;
                switch (algorithm) {
                    case "hillclimbing":
                        json = HillClimbing.solve(n, 1000);
                        break;
                    case "simulatedannealing":
                        json = SimulatedAnnealing.solve(n, 100.0, 0.99, 5000);
                        break;
                    case "tabusearch":
                        json = TabuSearch.solve(n, 100, 20);
                        break;
                    default:
                        continue;
                }

                // Parsear resultados manualmente del JSON
                boolean solved = json.contains("\"solved\":true");
                int iterIdx = json.indexOf("\"totalIterations\":");
                int iterEnd = json.indexOf(",", iterIdx);
                int iterations = Integer.parseInt(json.substring(iterIdx + 18, iterEnd).trim());

                int timeIdx = json.indexOf("\"timeMs\":");
                int timeEnd = json.indexOf(",", timeIdx);
                double time = Double.parseDouble(json.substring(timeIdx + 9, timeEnd).trim());

                if (solved) successes++;
                totalIter += iterations;
                totalTime += time;
                if (iterations < minIter) minIter = iterations;
                if (iterations > maxIter) maxIter = iterations;
                if (time < minTime) minTime = time;
                if (time > maxTime) maxTime = time;
            }

            double successRate = (double) successes / runs * 100;
            double avgIter = (double) totalIter / runs;
            double avgTime = totalTime / runs;

            return String.format(java.util.Locale.US,
                "{\"successRate\":%.1f,\"avgIterations\":%.1f,\"avgTimeMs\":%.2f," +
                "\"minIterations\":%d,\"maxIterations\":%d,\"minTimeMs\":%.2f,\"maxTimeMs\":%.2f,\"successes\":%d}",
                successRate, avgIter, avgTime, minIter, maxIter, minTime, maxTime, successes
            );
        }
    }

    /** Handler para servir archivos estáticos del frontend */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            File file = new File(frontendDir + path);
            if (!file.exists() || file.isDirectory()) {
                String notFound = "404 - File not found";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
                exchange.getResponseBody().close();
                return;
            }

            String contentType = getContentType(path);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    // --- Utilidades ---

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        byte[] bytes = json.getBytes("UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String getParam(String query, String key, String defaultValue) {
        if (query == null) return defaultValue;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return URLDecoder.decode(pair[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return defaultValue;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=UTF-8";
        if (path.endsWith(".css")) return "text/css; charset=UTF-8";
        if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
