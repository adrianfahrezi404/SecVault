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
        routeOrProcessUpload(hash, this.id, file);
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

    private void routeOrProcessUpload(int targetHash, int senderId, File file) {
        if (isMyHash(targetHash)) {
            // Targetnya adalah diri sendiri (local)
            logToGUI("[RECEIVE] Menerima file '" + file.getName() + "' (Hash: " + targetHash + "). Menyimpan ke lokal...");
            saveFileLocally(file, file.getName());
        } else {
            int nextHop = getNextHop(targetHash);
            logToGUI("[FORWARD] Meneruskan hash " + targetHash + " ke Node " + nextHop + " (IP: " + nodeIps.get(nextHop) + ")");
            try {
                FileTransferHandler.sendFile(nodeIps.get(nextHop), 8000 + nextHop, "UPLOAD", targetHash, senderId, file);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal upload ke Node " + nextHop + " - " + e.getMessage());
            }
        }
    }
    
    private void routeSearch(String action, int targetHash, int senderId, String fileName) {
        if (isMyHash(targetHash)) {
            processSearchLocally(action, fileName, senderId);
        } else {
            int nextHop = getNextHop(targetHash);
            logToGUI("[TRANSIT] Menerima request Hash " + targetHash + ", meneruskan ke Successor Node " + nextHop + ".");
            try {
                FileTransferHandler.sendRoutingMessage(nodeIps.get(nextHop), 8000 + nextHop, action, targetHash, senderId, fileName);
            } catch (IOException e) {
                logToGUI("[ERROR] Gagal routing ke Node " + nextHop);
            }
        }
    }

    private int getNextHop(int targetHash) {
        int successor = fingerTable[0].successor;
        if (inRangeInclusiveRight(targetHash, this.id, successor)) {
            return successor;
        }
        for (int i = 4; i >= 0; i--) {
            int finger = fingerTable[i].successor;
            if (inRangeExclusive(finger, this.id, targetHash)) {
                return finger;
            }
        }
        return successor;
    }

    private boolean isMyHash(int targetHash) {
        return inRangeInclusiveRight(targetHash, getPredecessor(), this.id);
    }
    
    private void processSearchLocally(String action, String fileName, int senderId) {
        File targetFile = new File(storageDir, fileName);
        if (!targetFile.exists()) {
            logToGUI("[ERROR] File '" + fileName + "' tidak ditemukan di storage lokal!");
            return;
        }
        
        logToGUI("[FOUND] File '" + fileName + "' ditemukan di Node ini!");
        
        if ("SEARCH_AND_OPEN_REMOTE".equals(action)) {
            logToGUI("[OPEN] Membuka file secara otomatis...");
            openFileInDesktop(targetFile);
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
    // UTILITAS
    // ==========================================
    
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
        if (gui != null) {
            gui.addTableRow(destName, this.id, "Tersimpan", destFile.getAbsolutePath());
        }
    }
    
    private void openFileInDesktop(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                logToGUI("[ERROR] Desktop API tidak didukung pada OS ini.");
            }
        } catch (IOException e) {
            logToGUI("[ERROR] Gagal membuka aplikasi: " + e.getMessage());
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
                    if (gui != null) gui.addTableRow(fileName, id, "Tersimpan (Download)", destFile.getAbsolutePath());
                    openFileInDesktop(destFile);
                } 
                else if (fileSize > 0 && "UPLOAD".equals(action)) {
                    if (isMyHash(targetHash)) {
                        logToGUI("[RECEIVE] Menerima file '" + fileName + "' (Hash: " + targetHash + "). Menyimpan ke lokal...");
                        File destFile = receiveFileBytes(dis, fileName, fileSize);
                        logToGUI("[SUCCESS] File " + fileName + " berhasil disimpan.");
                        if (gui != null) gui.addTableRow(fileName, id, "Tersimpan", destFile.getAbsolutePath());
                    } else {
                        logToGUI("[TRANSIT] Menerima stream file '" + fileName + "' (Hash: " + targetHash + "), meneruskan ke Successor Node...");
                        File tempFile = receiveFileBytes(dis, "temp_" + fileName, fileSize);
                        routeOrProcessUpload(targetHash, senderId, tempFile);
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
