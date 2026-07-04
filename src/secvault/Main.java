package secvault;

import javax.swing.*;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        // Node aktif dalam jaringan P2P (berdasarkan instruksi)
        int[] activeNodes = {3, 7, 11, 17, 22, 28};
        
        // --- KONFIGURASI IP ADDRESS KELOMPOK (GANTI IP DI BAWAH INI SEBELUM COMPILE) ---
        HashMap<Integer, String> nodeIps = new HashMap<>();
        nodeIps.put(3, "100.115.92.1");   // GANTI DENGAN IP MESIN NODE 3 (misal IP ZeroTier/Tailscale)
        nodeIps.put(7, "100.101.44.5");   // GANTI DENGAN IP MESIN NODE 7
        nodeIps.put(11, "127.0.0.1");     // GANTI DENGAN IP MESIN NODE 11
        nodeIps.put(17, "127.0.0.1");     // GANTI DENGAN IP MESIN NODE 17
        nodeIps.put(22, "127.0.0.1");     // GANTI DENGAN IP MESIN NODE 22
        nodeIps.put(28, "127.0.0.1");     // GANTI DENGAN IP MESIN NODE 28
        // -------------------------------------------------------------------------------
        
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
                // Karena hanya ada 1 jendela, kita atur ke tengah layar
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
