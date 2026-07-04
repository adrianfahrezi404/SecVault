package secvault;

import javax.swing.*;
import java.io.File;
import java.util.Scanner;

public class MainConsole {

    public static void main(String[] args) {
        int[] activeNodes = {3, 7, 11, 17, 22, 28};
        
        int nodeId = -1;
        if (args.length > 0) {
            nodeId = Integer.parseInt(args[0]);
        } else {
            System.out.print("Masukkan ID Node yang ingin dijalankan (3, 7, 11, 17, 22, 28): ");
            Scanner scanner = new Scanner(System.in);
            nodeId = scanner.nextInt();
        }

        boolean valid = false;
        for (int n : activeNodes) {
            if (n == nodeId) {
                valid = true;
                break;
            }
        }
        
        if (!valid) {
            System.out.println("ID Node tidak valid. Keluar.");
            System.exit(1);
        }

        // Peta IP Address untuk localhost test
        java.util.HashMap<Integer, String> nodeIps = new java.util.HashMap<>();
        for (int n : activeNodes) {
            nodeIps.put(n, "127.0.0.1"); // Default localhost sesuai konteks
        }

        P2PNode node = new P2PNode(nodeId, activeNodes, nodeIps);
        
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\n=== MENU P2P NODE " + nodeId + " ===");
            System.out.println("1. Upload File Fisik");
            System.out.println("2. Cari & Buka File di Node Tujuan (Remote Open)");
            System.out.println("3. Cari & Download File (Download lalu Buka Lokal)");
            System.out.println("4. Keluar");
            System.out.print("Pilih opsi: ");
            
            int choice = in.nextInt();
            in.nextLine(); // consume newline
            
            switch (choice) {
                case 1:
                    // Gunakan JFileChooser untuk memilih file secara grafis
                    SwingUtilities.invokeLater(() -> {
                        JFileChooser fileChooser = new JFileChooser();
                        fileChooser.setDialogTitle("Pilih file untuk di-upload");
                        int result = fileChooser.showOpenDialog(null);
                        if (result == JFileChooser.APPROVE_OPTION) {
                            File selectedFile = fileChooser.getSelectedFile();
                            node.uploadFile(selectedFile);
                        }
                    });
                    break;
                case 2:
                    System.out.print("Masukkan nama file yang dicari: ");
                    String name1 = in.nextLine();
                    node.searchAndOpenFile(name1, false);
                    break;
                case 3:
                    System.out.print("Masukkan nama file yang dicari: ");
                    String name2 = in.nextLine();
                    node.searchAndOpenFile(name2, true);
                    break;
                case 4:
                    System.out.println("Mematikan node...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Opsi tidak valid.");
            }
        }
    }
}
