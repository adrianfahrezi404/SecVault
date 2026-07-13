package secvault;

import java.io.*;
import java.net.Socket;

public class FileTransferHandler {

    /**
     * Mengirim file fisik beserta metadatanya melalui Socket.
     * @param ip IP tujuan (Node Successor)
     * @param port Port tujuan
     * @param action Aksi (contoh: "UPLOAD", "DOWNLOAD_RESPONSE")
     * @param targetHash Hash dari nama file
     * @param senderId ID node pengirim asli
     * @param file Objek File fisik yang akan dikirim
     */
    public static void sendFile(String ip, int port, String action, int targetHash, int senderId, File file) throws IOException {
        sendFile(ip, port, action, targetHash, senderId, file, file.getName());
    }

    public static void sendFile(String ip, int port, String action, int targetHash, int senderId, File file, String originalFileName) throws IOException {
        try (Socket socket = new Socket(ip, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             FileInputStream fis = new FileInputStream(file)) {
            
            // 1. Kirim Metadata via DataOutputStream
            dos.writeUTF(action);
            dos.writeInt(targetHash);
            dos.writeInt(senderId);
            dos.writeUTF(originalFileName);
            dos.writeLong(file.length());
            
            // 2. Kirim Byte Stream Fisik File
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
        }
    }

    /**
     * Mengirim pesan routing murni (tanpa payload stream file), misalnya untuk SEARCH.
     */
    public static void sendRoutingMessage(String ip, int port, String action, int targetHash, int senderId, String fileName) throws IOException {
        try (Socket socket = new Socket(ip, port);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            
            dos.writeUTF(action);
            dos.writeInt(targetHash);
            dos.writeInt(senderId);
            dos.writeUTF(fileName);
            // Ukuran file 0 menandakan tidak ada payload fisik file yang mengikuti
            dos.writeLong(0); 
            dos.flush();
        }
    }
}
