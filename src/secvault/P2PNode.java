package secvault;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.util.*;

public class P2PNode {
    public int id;
    public int[] activeNodes;
    public FingerTable[] fingerTable;
    public HashMap<Integer, String> nodeIps;
    public File storageDir;
    private NodeGUI gui;
    
    // Sistem Log untuk API
    public List<String> systemLogs = new ArrayList<>();
    
    // Server API Web
    private ApiServer apiServer;
    
    public P2PNode(int id, int[] activeNodes, HashMap<Integer, String> nodeIps) {
        this.id = id;
        this.activeNodes = Arrays.copyOf(activeNodes, activeNodes.length);
        Arrays.sort(this.activeNodes);
        this.fingerTable = new FingerTable[5];
        
        // Menggunakan Peta IP Address dari luar (Main)
        this.nodeIps = nodeIps;
        
        // Buat direktori storage unik untuk node ini
        this.storageDir = new File("./storage_node_" + id);
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        
        initFingerTable();
        startServer();
        
        // Mulai server API pada port 9000 + id
        this.apiServer = new ApiServer(this);
        this.apiServer.start();
    }

    public void setGUI(NodeGUI gui) {
        this.gui = gui;
    }

    private void logToGUI(String msg) {
        // Simpan ke log sistem untuk endpoint /api/logs
        synchronized(systemLogs) {
            systemLogs.add(msg);
            if (systemLogs.size() > 100) {
                systemLogs.remove(0); // Batasi 100 baris terakhir
            }
        }
        
        if (gui != null) {
            gui.appendLog(msg);
        } else {
            System.out.println(msg);
        }
    }

    private void initFingerTable() {
        for (int i = 1; i <= 5; i++) {
            int start = (this.id + (1 << (i - 1))) % 32;
            int successor = findFirstActiveNode(start);
            fingerTable[i - 1] = new FingerTable(start, successor);
        }
    }

    private int findFirstActiveNode(int start) {
        for (int node : activeNodes) {
            if (node >= start) return node;
        }
        return activeNodes[0];
    }
    
    private int getPredecessor() {
        int pred = activeNodes[activeNodes.length - 1];
        for (int i = 0; i < activeNodes.length; i++) {
            if (activeNodes[i] == this.id) {
                if (i > 0) pred = activeNodes[i - 1];
                break;
            }
        }
        return pred;
    }

    private void startServer() {
        new Thread(() -> {
            try {
                ServerSocket serverSocket = new ServerSocket(8000 + this.id, 50, InetAddress.getByName("0.0.0.0"));
                logToGUI("[SYSTEM] Server berjalan di port " + (8000 + id));
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(new ClientHandler(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    // ==========================================
    // LOGIKA API / ENTRY POINT DARI GUI
    // ==========================================
    
    public void uploadFile(File file) {
        int hash = Math.abs(file.getName().hashCode()) % 32;
        logToGUI("[UPLOAD] Memulai upload '" + file.getName() + "' (Hash: " + hash + ")...");
        routeOrProcessUpload(hash, this.id, file, file.getName());
    }
    
    public void searchAndOpenFile(String fileName, boolean downloadToLocal) {
        int hash = Math.abs(fileName.hashCode()) % 32;
        String action = downloadToLocal ? "SEARCH_AND_DOWNLOAD" : "SEARCH_AND_OPEN_REMOTE";
        logToGUI("[SEARCH] Mencari '" + fileName + "' (Hash: " + hash + ")");
        routeSearch(action, hash, this.id, fileName);
    }

    // ==========================================
    // LOGIKA ROUTING DHT (O(log n))
    // ==========================================

    private void routeOrProcessUpload(int targetHash, int senderId, File file, String originalFileName) {
        if (isMyHash(targetHash)) {
            // Targetnya adalah diri sendiri (local)
            logToGUI("[RECEIVE] Menerima file '" + originalFileName + "' (Hash: " + targetHash + "). Menyimpan ke lokal...");
            if (gui != null) gui.showRoutingAnimation(java.util.Arrays.asList(this.id, this.id));
            saveFileLocally(file, originalFileName);
        } else {
            int nextHop = getNextHop(targetHash);
            logToGUI("[FORWARD] Meneruskan hash " + targetHash + " ke Node " + nextHop + " (IP: " + nodeIps.get(nextHop) + ")");
            if (gui != null) gui.showRoutingAnimation(java.util.Arrays.asList(this.id, nextHop));
            try {
                FileTransferHandler.sendFile(nodeIps.get(nextHop), 8000 + nextHop, "UPLOAD", targetHash, senderId, file, originalFileName);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal upload ke Node " + nextHop + " - " + e.getMessage());
            }
        }
    }
    
    private void routeSearch(String action, int targetHash, int senderId, String fileName) {
        if (isMyHash(targetHash)) {
            if (gui != null) gui.showRoutingAnimation(java.util.Arrays.asList(this.id, this.id));
            processSearchLocally(action, fileName, senderId);
        } else {
            int nextHop = getNextHop(targetHash);
            logToGUI("[TRANSIT] Menerima request Hash " + targetHash + ", meneruskan ke Successor Node " + nextHop + ".");
            if (gui != null) gui.showRoutingAnimation(java.util.Arrays.asList(this.id, nextHop));
            try {
                FileTransferHandler.sendRoutingMessage(nodeIps.get(nextHop), 8000 + nextHop, action, targetHash, senderId, fileName);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal routing ke Node " + nextHop);
            }
        }
    }

    public int getNextHop(int targetHash) {
        int successor = fingerTable[0].successor;
        logToGUI("[ROUTING] Mencari target Hash " + targetHash + "...");
        logToGUI("[ROUTING] Hash " + targetHash + " tidak ada di interval Node " + this.id + ".");
        
        if (inRangeInclusiveRight(targetHash, this.id, successor)) {
            logToGUI("[ROUTING] Hash " + targetHash + " masuk rentang Successor terdekat (Node " + successor + ").");
            return successor;
        }
        
        for (int i = 4; i >= 0; i--) {
            int finger = fingerTable[i].successor;
            if (inRangeExclusive(finger, this.id, targetHash)) {
                logToGUI("[ROUTING] Perhitungan O(log N): Hash " + targetHash + " ada di rentang Finger[" + i + "]. Lompat ke Node " + finger + ".");
                return finger;
            }
        }
        logToGUI("[ROUTING] Fallback ke Successor Node " + successor);
        return successor;
    }

    private boolean isMyHash(int targetHash) {
        return inRangeInclusiveRight(targetHash, getPredecessor(), this.id);
    }
    
    private void processSearchLocally(String action, String fileName, int senderId) {
        File targetFile = new File(storageDir, fileName);
        if (!targetFile.exists()) {
            logToGUI("[ERROR] File '" + fileName + "' tidak ditemukan di storage lokal!");
            if (senderId != this.id) { // Balas ke pengirim bahwa pencarian gagal
                try {
                    FileTransferHandler.sendRoutingMessage(nodeIps.get(senderId), 8000 + senderId, "SEARCH_NOT_FOUND", 0, this.id, fileName);
                } catch (IOException e) {
                    logToGUI("[ERROR] Gagal membalas pesan SEARCH_NOT_FOUND ke Node " + senderId);
                }
            } else {
                if (gui != null) gui.showNeumorphicError("Pencarian Gagal!\nFile '" + fileName + "' tidak ada di Node ini.\nPastikan nama file dan ekstensi persis sama!");
            }
            return;
        }
        
        logToGUI("[FOUND] File '" + fileName + "' ditemukan di Node ini!");
        
        if ("SEARCH_AND_OPEN_REMOTE".equals(action)) {
            logToGUI("[OPEN] Mengirim stream file sementara ke Node " + senderId + " untuk dibuka.");
            try {
                FileTransferHandler.sendFile(nodeIps.get(senderId), 8000 + senderId, "OPEN_REMOTE_RESPONSE", 0, this.id, targetFile);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal mengirim stream file ke Node " + senderId);
            }
        } 
        else if ("SEARCH_AND_DOWNLOAD".equals(action)) {
            logToGUI("[DOWNLOAD] Mengirim kembali file fisik ke Node " + senderId + "...");
            try {
                FileTransferHandler.sendFile(nodeIps.get(senderId), 8000 + senderId, "DOWNLOAD_RESPONSE", 0, this.id, targetFile);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal mengirim balasan ke Node " + senderId);
            }
        }
    }

    // ==========================================
    // UTILITAS & SIMULASI ROUTING
    // ==========================================
    
    public int getTargetNode(int targetHash) {
        return findFirstActiveNode(targetHash);
    }
    
    public String simulateRoutingPath(int targetHash) {
        int targetNode = getTargetNode(targetHash);
        if (targetNode == this.id) {
            return "Node " + this.id + " (Lokal)";
        }
        
        StringBuilder path = new StringBuilder();
        path.append("Node ").append(this.id);
        
        int currentNode = this.id;
        int maxJumps = 10; // Mencegah infinite loop jika ada bug
        
        while (currentNode != targetNode && maxJumps > 0) {
            int nextHop = calculateNextHopForNode(currentNode, targetHash);
            path.append(" ➔ Node ").append(nextHop);
            currentNode = nextHop;
            maxJumps--;
        }
        return path.toString();
    }
    
    // Broadcast pesan teks (Log) ke semua node aktif
    public void broadcastAnnouncement(String message) {
        for (int n : activeNodes) {
            if (n != this.id) {
                try {
                    FileTransferHandler.sendRoutingMessage(nodeIps.get(n), 8000 + n, "ANNOUNCEMENT", 0, this.id, message);
                } catch (IOException e) {
                    // Abaikan jika node lain offline
                }
            }
        }
    }
    
    private int calculateNextHopForNode(int nodeId, int targetHash) {
        // Membangun Virtual Finger Table untuk node lain
        int[] vFinger = new int[5];
        for (int i = 1; i <= 5; i++) {
            int start = (nodeId + (1 << (i - 1))) % 32;
            vFinger[i - 1] = findFirstActiveNode(start);
        }
        
        int successor = vFinger[0];
        if (inRangeInclusiveRight(targetHash, nodeId, successor)) {
            return successor;
        }
        for (int i = 4; i >= 0; i--) {
            int finger = vFinger[i];
            if (inRangeExclusive(finger, nodeId, targetHash)) {
                return finger;
            }
        }
        return successor;
    }
    
    private void saveFileLocally(File sourceFile, String destName) {
        File destFile = new File(storageDir, destName);
        if (!sourceFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
            try (FileInputStream fis = new FileInputStream(sourceFile);
                 FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, read);
                }
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal menyalin file lokal: " + e.getMessage());
                return;
            }
        }
        logToGUI("[SUCCESS] Berhasil menyimpan file ke " + destFile.getAbsolutePath());
        
        // Umumkan ke seluruh jaringan bahwa file ini sekarang tersedia
        broadcastAnnouncement("📢 [INFO GLOBAL] File baru tersedia untuk di-search: '" + destName + "' (Disimpan di Node " + this.id + ")");
        
        if (gui != null) {
            int hash = Math.abs(destName.hashCode()) % 32;
            gui.addTableRow(destName, hash, "Node " + this.id, simulateRoutingPath(hash), "Tersimpan Lokal", destFile.getAbsolutePath());
        }
    }
    
    private boolean inRangeInclusiveRight(int val, int start, int end) {
        if (start < end) return val > start && val <= end;
        else return val > start || val <= end;
    }
    
    private boolean inRangeExclusive(int val, int start, int end) {
        if (start < end) return val > start && val < end;
        else return val > start || val < end;
    }

    // ==========================================
    // HANDLER JARINGAN (SOCKET SERVER)
    // ==========================================
    
    private class ClientHandler implements Runnable {
        private Socket socket;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
                String action = dis.readUTF();
                int targetHash = dis.readInt();
                int senderId = dis.readInt();
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                
                if ("DOWNLOAD_RESPONSE".equals(action)) {
                    logToGUI("[RECEIVE] Menerima unduhan file '" + fileName + "'.");
                    File destFile = receiveFileBytes(dis, fileName, fileSize);
                    logToGUI("[SUCCESS] File " + fileName + " tersimpan di node ini.");
                    if (gui != null) {
                        int hash = Math.abs(fileName.hashCode()) % 32;
                        gui.addTableRow(fileName, hash, "Node " + senderId, simulateRoutingPath(hash), "Hasil Download", destFile.getAbsolutePath());
                        gui.openFile(destFile);
                    }
                } 
                else if ("OPEN_REMOTE_RESPONSE".equals(action)) {
                    logToGUI("[RECEIVE] Menerima stream file '" + fileName + "' untuk dibuka (Remote View).");
                    File tempFile = receiveFileBytes(dis, "TEMP_" + fileName, fileSize);
                    tempFile.deleteOnExit(); // Dihapus otomatis oleh OS saat aplikasi ditutup
                    if (gui != null) gui.openFile(tempFile);
                }
                else if ("SEARCH_NOT_FOUND".equals(action)) {
                    logToGUI("[ERROR] Pencarian gagal: File '" + fileName + "' tidak ditemukan di jaringan P2P (Node " + senderId + " kosong).");
                    if (gui != null) {
                        gui.showNeumorphicError("Pencarian Gagal!\nFile '" + fileName + "' tidak ditemukan di jaringan.\n\nTips: Pastikan nama file beserta ekstensinya (misal: Laporan.pdf) SAMA PERSIS karena DHT menggunakan Exact Hash Match.");
                    }
                }
                else if ("ANNOUNCEMENT".equals(action)) {
                    // fileName digunakan sebagai payload teks pengumuman
                    logToGUI(fileName);
                }
                else if (fileSize > 0 && "UPLOAD".equals(action)) {
                    if (isMyHash(targetHash)) {
                        logToGUI("[RECEIVE] Menerima file '" + fileName + "' (Hash: " + targetHash + "). Menyimpan ke lokal...");
                        File destFile = receiveFileBytes(dis, fileName, fileSize);
                        logToGUI("[SUCCESS] File " + fileName + " berhasil disimpan.");
                        
                        // Umumkan ke seluruh jaringan bahwa file ini sekarang tersedia
                        broadcastAnnouncement("📢 [INFO GLOBAL] File baru tersedia untuk di-search: '" + fileName + "' (Disimpan di Node " + id + ")");
                        
                        if (gui != null) {
                            gui.addTableRow(fileName, targetHash, "Node " + id, "Node " + senderId + " ➔ Node " + id, "Tersimpan Remote", destFile.getAbsolutePath());
                        }
                    } else {
                        logToGUI("[TRANSIT] Menerima stream file '" + fileName + "' (Hash: " + targetHash + "), meneruskan ke Successor Node...");
                        File tempFile = receiveFileBytes(dis, "temp_" + fileName, fileSize);
                        routeOrProcessUpload(targetHash, senderId, tempFile, fileName);
                        tempFile.delete(); // Bersihkan memori sementara
                    }
                } 
                else {
                    routeSearch(action, targetHash, senderId, fileName);
                }
                
            } catch (Exception e) {
                // Jangan log exception socket tertutup
            }
        }
        
        private File receiveFileBytes(DataInputStream dis, String fileName, long fileSize) throws IOException {
            File destFile = new File(storageDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                while (totalRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalRead);
                    int read = dis.read(buffer, 0, bytesToRead);
                    if (read == -1) break;
                    fos.write(buffer, 0, read);
                    totalRead += read;
                }
            }
            return destFile;
        }
    }
}
