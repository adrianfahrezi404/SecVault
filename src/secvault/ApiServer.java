package secvault;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Base64;

public class ApiServer {
    private P2PNode node;
    private HttpServer server;

    public ApiServer(P2PNode node) {
        this.node = node;
    }

    public void start() {
        try {
            int port = 9000 + node.id;
            // Buat HTTP Server dengan port terdedikasi per-node
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            // Daftarkan API Endpoints
            server.createContext("/api/info", new InfoHandler());
            server.createContext("/api/logs", new LogsHandler());
            server.createContext("/api/files", new FilesHandler());
            server.createContext("/api/upload", new UploadHandler());
            server.createContext("/api/search", new SearchHandler());
            
            // Multithreading untuk HTTP Server
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            System.out.println("[API] Server Web HTTP berjalan di http://localhost:" + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // UTILITAS HTTP
    // ==========================================
    
    /**
     * Memasang header CORS untuk frontend (misal React/Next.js) dan menangani Preflight.
     */
    private void enableCORS(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        }
    }
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String jsonResponse) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        byte[] bytes = jsonResponse.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    private String getBody(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[8192]; // Buffer 8KB
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return new String(buffer.toByteArray(), "UTF-8");
    }
    
    /**
     * Parsing nilai String dari JSON secara manual murni tanpa library eksternal.
     * Hanya mendukung struktur JSON datar (satu level).
     */
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIdx = json.indexOf(searchKey);
        if (startIdx == -1) {
            // Coba dengan spasi misal "key": "value"
            searchKey = "\"" + key + "\" :";
            startIdx = json.indexOf(searchKey);
            if (startIdx == -1) return null;
        }
        
        startIdx += searchKey.length();
        // Cari tanda kutip pembuka dari value string
        startIdx = json.indexOf("\"", startIdx);
        if (startIdx == -1) return null;
        
        startIdx++; // Lewati tanda kutip
        // Cari tanda kutip penutup
        int endIdx = json.indexOf("\"", startIdx);
        if (endIdx == -1) return null;
        
        return json.substring(startIdx, endIdx);
    }
    
    // ==========================================
    // HANDLERS
    // ==========================================

    class InfoHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
            
            // Merangkai respons JSON murni secara manual
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            sb.append("  \"id\": ").append(node.id).append(",\n");
            
            sb.append("  \"activeNodes\": [");
            for(int i=0; i<node.activeNodes.length; i++) {
                sb.append(node.activeNodes[i]);
                if(i < node.activeNodes.length - 1) sb.append(", ");
            }
            sb.append("],\n");
            
            sb.append("  \"fingerTable\": [\n");
            for(int i=0; i<5; i++) {
                sb.append("    {\"start\": ").append(node.fingerTable[i].start)
                  .append(", \"successor\": ").append(node.fingerTable[i].successor).append("}");
                if(i < 4) sb.append(",");
                sb.append("\n");
            }
            sb.append("  ]\n}");
            
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    class LogsHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
            
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            synchronized(node.systemLogs) {
                for(int i=0; i<node.systemLogs.size(); i++) {
                    String log = node.systemLogs.get(i).replace("\"", "\\\""); // Escape kutip ganda
                    sb.append("  \"").append(log).append("\"");
                    if(i < node.systemLogs.size() - 1) sb.append(",");
                    sb.append("\n");
                }
            }
            sb.append("]");
            
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    class FilesHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
            
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            File[] files = node.storageDir.listFiles();
            if(files != null) {
                for(int i=0; i<files.length; i++) {
                    // Hindari membaca file temp
                    if(files[i].getName().startsWith("temp_")) continue;
                    
                    sb.append("  \"").append(files[i].getName().replace("\"", "\\\"")).append("\"");
                    if(i < files.length - 1) sb.append(",");
                    sb.append("\n");
                }
            }
            sb.append("]");
            
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    class SearchHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
            
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = getBody(exchange.getRequestBody());
                String fileName = extractJsonValue(body, "fileName");
                
                if (fileName != null && !fileName.isEmpty()) {
                    // Pemicu asinkronus (DHT akan merutekan ke network tanpa memblokir thread ini)
                    node.searchAndOpenFile(fileName, false);
                    sendJsonResponse(exchange, 200, "{\"status\": \"Pencarian untuk '" + fileName + "' dimulai. Pantau log untuk hasil.\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"error\": \"fileName tidak ditemukan di dalam JSON\"}");
                }
            }
        }
    }

    class UploadHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            enableCORS(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) return;
            
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String body = getBody(exchange.getRequestBody());
                    String fileName = extractJsonValue(body, "fileName");
                    String contentBase64 = extractJsonValue(body, "contentBase64");
                    
                    if (fileName != null && contentBase64 != null) {
                        // 1. Decode string base64 menjadi byte array
                        byte[] fileBytes = Base64.getDecoder().decode(contentBase64);
                        
                        // 2. Tulis byte array menjadi file fisik sementara di disk lokal
                        File tempFile = new File(node.storageDir, fileName);
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            fos.write(fileBytes);
                        }
                        
                        // 3. Masukkan file fisik tersebut ke rutinitas P2P Node 
                        // Ini akan meneruskannya ke Node Successor via DataOutputStream P2P
                        node.uploadFile(tempFile);
                        
                        sendJsonResponse(exchange, 200, "{\"status\": \"Berhasil mem-parsing Base64 dan memicu routing Upload P2P\"}");
                    } else {
                        sendJsonResponse(exchange, 400, "{\"error\": \"Parameter fileName atau contentBase64 hilang\"}");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    sendJsonResponse(exchange, 500, "{\"error\": \"Internal Server Error saat dekode Base64\"}");
                }
            }
        }
    }
}
