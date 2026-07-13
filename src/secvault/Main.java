package secvault;

import javax.swing.*;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        // Node aktif dalam jaringan P2P (berdasarkan instruksi)
        int[] activeNodes = {3, 7, 11, 17, 22, 28};
        
        // Membaca konfigurasi IP dari file eksternal (nodes.txt)
        HashMap<Integer, String> nodeIps = new HashMap<>();
        java.io.File configFile = new java.io.File("nodes.txt");
        
        if (!configFile.exists()) {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(configFile))) {
                pw.println("# Konfigurasi IP Jaringan P2P SecVault");
                pw.println("# ----------------------------------------");
                pw.println("# Anda bisa MENGGABUNGKAN (mix) Localhost dan Jaringan secara bersamaan!");
                pw.println("# -> Gunakan 127.0.0.1 jika Node dijalankan di laptop Anda sendiri.");
                pw.println("# -> Gunakan IP Jaringan teman (misal: 192.168.43.10) jika Node berjalan di laptop teman.");
                for (int n : activeNodes) {
                    pw.println("Node-" + n + "=127.0.0.1");
                }
                System.out.println("[INFO] File 'nodes.txt' berhasil dibuat dengan default localhost.");
            } catch (Exception e) {
                System.out.println("[ERROR] Gagal membuat nodes.txt: " + e.getMessage());
            }
        }
        
        try (java.util.Scanner scanner = new java.util.Scanner(configFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[0].replace("Node-", "").trim());
                        nodeIps.put(id, parts[1].trim());
                    } catch (Exception ignore) {}
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Gagal membaca nodes.txt: " + e.getMessage());
        }

        // Fallback jika file error/isinya kurang
        if (nodeIps.size() < 6) {
            for (int n : activeNodes) {
                nodeIps.putIfAbsent(n, "127.0.0.1");
            }
        }
        // -------------------------------------------------------------------------------

        // Tampilkan IP WiFi laptop ini sendiri di konsol, supaya gampang dicocokkan ke map di atas
        try {
            java.util.Enumeration<java.net.NetworkInterface> nets = java.net.NetworkInterface.getNetworkInterfaces();
            System.out.println("=== IP Address laptop ini (cocokkan dengan nodeIps di atas) ===");
            while (nets.hasMoreElements()) {
                java.net.NetworkInterface netIf = nets.nextElement();
                if (netIf.isLoopback() || !netIf.isUp()) continue;
                java.util.Enumeration<java.net.InetAddress> addrs = netIf.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    java.net.InetAddress addr = addrs.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        System.out.println("  " + netIf.getDisplayName() + " -> " + addr.getHostAddress());
                    }
                }
            }
            System.out.println("================================================================");
        } catch (Exception ex) {
            System.out.println("Gagal membaca IP lokal: " + ex.getMessage());
        }
        
        // Menyiapkan opsi String untuk JComboBox di JOptionPane
        String[] nodeOptions = {"3", "7", "11", "17", "22", "28"};
        
        // Memastikan antarmuka Swing berjalan di Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            
            String selected = null;
            if (args.length > 0) {
                selected = args[0];
            } else {
                // Menampilkan Dialog Pemilihan (Combo Box)
                selected = (String) JOptionPane.showInputDialog(
                        null,
                        "Pilih ID Node yang akan dijalankan di laptop ini:",
                        "Inisialisasi Node Jaringan P2P",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        nodeOptions,
                        nodeOptions[0]
                );
            }
            
            // Validasi Input: Membatalkan atau menutup dialog
            if (selected == null) {
                JOptionPane.showMessageDialog(null, 
                        "Pemilihan dibatalkan. Program akan ditutup.", 
                        "Batal", 
                        JOptionPane.WARNING_MESSAGE);
                System.exit(0);
            }
            
            try {
                int selectedNodeId = Integer.parseInt(selected);
                
                // Validasi Input: Memastikan ID berada dalam array activeNodes
                boolean isValid = false;
                for (int n : activeNodes) {
                    if (n == selectedNodeId) {
                        isValid = true;
                        break;
                    }
                }
                
                if (!isValid) {
                    JOptionPane.showMessageDialog(null, 
                            "ID Node tidak valid! Program akan ditutup.", 
                            "Error Input", 
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
                
                // Jalankan Single Node: Buka koneksi dan GUI HANYA untuk node yang dipilih
                P2PNode node = new P2PNode(selectedNodeId, activeNodes, nodeIps);
                
                NodeGUI gui = new NodeGUI(node);
                gui.setLocationRelativeTo(null); 
                gui.setVisible(true);
                
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, 
                        "Format ID tidak valid! Program akan ditutup.", 
                        "Error Input", 
                        JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        });
    }
}