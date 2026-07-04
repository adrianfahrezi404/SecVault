package secvault;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;

public class NodeGUI extends JFrame {
    private P2PNode node;
    
    private NeumorphicTextField txtFilename;
    private NeumorphicButton btnPilih;
    private NeumorphicButton btnSearchRemote;
    private NeumorphicButton btnSearchDownload;
    private NeumorphicButton btnUpload;
    
    private JTextArea txtLog;
    private JTable table;
    private DefaultTableModel tableModel;
    
    private File selectedFile = null;
    private String masterKey;
    
    // --- Palet Warna Neumorphism ---
    private final Color baseColor = new Color(224, 229, 236);     // Abu-abu kebiruan terang (#E0E5EC)
    private final Color darkShadow = new Color(163, 177, 198);    // Shadow Bawah-Kanan (#A3B1C6)
    private final Color lightShadow = new Color(255, 255, 255);   // Highlight Atas-Kiri (#FFFFFF)
    private final Color textColor = new Color(74, 85, 104);       // Teks abu-abu gelap (#4A5568)
    
    public NodeGUI(P2PNode node) {
        this.node = node;
        node.setGUI(this);
        
        // --- 1. Autentikasi Master Key ---
        String key = JOptionPane.showInputDialog(null, 
            "Masukkan Master Key untuk Brankas (SecVault):", 
            "Autentikasi Node " + node.id, 
            JOptionPane.WARNING_MESSAGE);
            
        if (key == null || key.trim().isEmpty()) {
            System.exit(0);
        }
        this.masterKey = key;
        
        setTitle("SecVault - Node " + node.id + " [Red Team Network]");
        setSize(850, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Root Panel dengan spasi
        JPanel rootPanel = new JPanel(new BorderLayout(15, 15));
        rootPanel.setBackground(baseColor);
        rootPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        setContentPane(rootPanel);
        
        // --- Panel Atas (Header & Input) ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 20));
        topPanel.setBackground(baseColor);
        topPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        JLabel lblHeader = new JLabel("SecVault Node " + node.id);
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblHeader.setForeground(textColor);
        topPanel.add(lblHeader, BorderLayout.NORTH);
        
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        inputPanel.setBackground(baseColor);
        
        btnPilih = new NeumorphicButton("Pilih File");
        txtFilename = new NeumorphicTextField(20);
        
        btnUpload = new NeumorphicButton("Upload");
        btnSearchRemote = new NeumorphicButton("Search (Open)");
        btnSearchDownload = new NeumorphicButton("Search (Download)");
        
        inputPanel.add(btnPilih);
        inputPanel.add(txtFilename);
        inputPanel.add(btnUpload);
        inputPanel.add(btnSearchRemote);
        inputPanel.add(btnSearchDownload);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        
        rootPanel.add(topPanel, BorderLayout.NORTH);
        
        // --- Panel Tengah (Tabel JTable) ---
        String[] columns = {"Nama File", "Lokasi Node", "Status", "Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        
        table.getColumnModel().getColumn(3).setMinWidth(0);
        table.getColumnModel().getColumn(3).setMaxWidth(0);
        table.getColumnModel().getColumn(3).setWidth(0);
        
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createEmptyBorder());
        scrollTable.getViewport().setBackground(baseColor);
        scrollTable.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        NeumorphicPanel panelCenter = new NeumorphicPanel(new BorderLayout());
        
        JLabel lblStatus = new JLabel("Encrypted Evidence Ledger (Klik ganda baris untuk Dekripsi Sementara)");
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblStatus.setForeground(textColor);
        lblStatus.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        panelCenter.add(lblStatus, BorderLayout.NORTH);
        panelCenter.add(scrollTable, BorderLayout.CENTER);
        
        // --- Panel Bawah (Log Console JTextArea) ---
        txtLog = new JTextArea(12, 40);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        txtLog.setBackground(baseColor);
        txtLog.setForeground(textColor);
        txtLog.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createEmptyBorder());
        scrollLog.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        NeumorphicPanel panelBottom = new NeumorphicPanel(new BorderLayout());
        
        JLabel lblLog = new JLabel("Log Aktivitas Jaringan");
        lblLog.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblLog.setForeground(textColor);
        lblLog.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        panelBottom.add(lblLog, BorderLayout.NORTH);
        panelBottom.add(scrollLog, BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelCenter, panelBottom);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(20);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(baseColor);
        
        // Remove split pane borders
        BasicSplitPaneUI splitPaneUI = (BasicSplitPaneUI) splitPane.getUI();
        splitPaneUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
        
        rootPanel.add(splitPane, BorderLayout.CENTER);
        
        // ==========================================
        // EVENT LISTENERS
        // ==========================================
        
        btnPilih.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Pilih file fisik");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                selectedFile = chooser.getSelectedFile();
                txtFilename.setText(selectedFile.getName());
            }
        });
        
        btnUpload.addActionListener(e -> {
            if (selectedFile != null && selectedFile.getName().equals(txtFilename.getText().trim())) {
                try {
                    // Buat folder sementara untuk menjaga nama asli agar DHT hash tepat
                    File tempDir = new File(System.getProperty("java.io.tmpdir"), "secvault_" + System.currentTimeMillis());
                    tempDir.mkdirs();
                    tempDir.deleteOnExit();
                    
                    File encryptedPayload = new File(tempDir, selectedFile.getName());
                    encryptedPayload.deleteOnExit();
                    
                    try (java.io.FileInputStream fis = new java.io.FileInputStream(selectedFile);
                         java.io.FileOutputStream fos = new java.io.FileOutputStream(encryptedPayload)) {
                        CryptoUtils.encryptStream(fis, fos, masterKey);
                    }
                    
                    node.uploadFile(encryptedPayload); // Upload payload yang sudah terenkripsi AES
                } catch (Exception ex) {
                    appendLog("[ERROR] Gagal mengenkripsi file: " + ex.getMessage());
                }
            } else {
                String manualName = txtFilename.getText().trim();
                if (!manualName.isEmpty()) {
                    appendLog("[ERROR] Anda harus memilih file fisik via tombol 'Pilih File' sebelum meng-upload!");
                }
            }
        });
        
        btnSearchRemote.addActionListener(e -> {
            String filename = txtFilename.getText().trim();
            if (!filename.isEmpty()) {
                node.searchAndOpenFile(filename, false);
            }
        });
        
        btnSearchDownload.addActionListener(e -> {
            String filename = txtFilename.getText().trim();
            if (!filename.isEmpty()) {
                node.searchAndOpenFile(filename, true);
            }
        });
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                if (me.getClickCount() == 2) {
                    JTable target = (JTable)me.getSource();
                    int row = target.getSelectedRow();
                    if (row != -1) {
                        String filePath = (String) tableModel.getValueAt(row, 3);
                        if (filePath != null && !filePath.isEmpty()) {
                            openFile(new File(filePath));
                        }
                    }
                }
            }
        });
    }
    
    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setBackground(baseColor);
        table.setForeground(textColor);
        
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.getTableHeader().setBackground(baseColor); 
        table.getTableHeader().setForeground(textColor);
        table.getTableHeader().setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        table.setShowVerticalLines(false); 
        table.setShowHorizontalLines(true);
        table.setGridColor(darkShadow);
        table.setIntercellSpacing(new Dimension(0, 1));
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(baseColor);
                c.setForeground(textColor);
                setBorder(new EmptyBorder(0, 15, 0, 15)); 
                return c;
            }
        });
    }
    
    public void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
    
    public void addTableRow(String filename, int nodeId, String status, String filePath) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addRow(new Object[]{filename, "Node " + nodeId, status, filePath});
        });
    }
    
    // ==========================================
    // LOGIKA BUKA FILE CERDAS (Smart Viewer)
    // ==========================================
    
    private void openFile(File file) {
        if (!file.exists()) {
            appendLog("[ERROR] Evidence fisik tidak ditemukan: " + file.getAbsolutePath());
            return;
        }

        try {
            // Lakukan Dekripsi ke Temporary File
            File tempDecrypted = File.createTempFile("evidence_", "_" + file.getName());
            tempDecrypted.deleteOnExit(); // Sangat Penting: Lenyapkan saat aplikasi ditutup
            
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempDecrypted)) {
                CryptoUtils.decryptStream(fis, fos, masterKey);
            }
            
            // Baca nama asli file untuk smart viewer logic
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                new ImagePreviewDialog(this, tempDecrypted).setVisible(true);
                appendLog("[DECRYPT] Menampilkan pratinjau bukti: " + file.getName());
            } else {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(tempDecrypted);
                    appendLog("[DECRYPT] Mendelegasikan dekripsi bukti " + file.getName() + " ke aplikasi OS.");
                } else {
                    showNeumorphicError("Sistem operasi Anda tidak mendukung fitur Desktop API.");
                }
            }
        } catch (Exception ex) {
            showNeumorphicError("Master Key Salah atau File Korup!\nGagal mendekripsi bukti: " + ex.getMessage());
        }
    }
    
    private void showNeumorphicError(String message) {
        JDialog dialog = new JDialog(this, "Peringatan Sistem", true);
        dialog.setSize(450, 220);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(baseColor);
        
        NeumorphicPanel rootPanel = new NeumorphicPanel(new BorderLayout(15, 15));
        
        JLabel lblMsg = new JLabel("<html><div style='text-align: center; color: #4A5568'>" + message.replace("\n", "<br>") + "</div></html>");
        lblMsg.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblMsg.setHorizontalAlignment(SwingConstants.CENTER);
        
        rootPanel.add(lblMsg, BorderLayout.CENTER);
        
        NeumorphicButton btnClose = new NeumorphicButton("Mengerti");
        btnClose.addActionListener(e -> dialog.dispose());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(baseColor);
        bottomPanel.add(btnClose);
        
        rootPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        dialog.setContentPane(rootPanel);
        dialog.setVisible(true);
    }
    
    // ==========================================
    // CLASS UI NEUMORPHISM
    // ==========================================

    class NeumorphicPanel extends JPanel {
        private int shadowSize = 8;
        private int radius = 25;

        public NeumorphicPanel(LayoutManager layout) {
            super(layout);
            setBackground(baseColor);
            setBorder(new EmptyBorder(shadowSize + 15, shadowSize + 15, shadowSize + 15, shadowSize + 15));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Convex Shadow
            for (int i = 0; i <= shadowSize; i++) {
                int alpha = 70 - (i * 70 / shadowSize);
                
                // Light shadow (Top-Left)
                g2.setColor(new Color(lightShadow.getRed(), lightShadow.getGreen(), lightShadow.getBlue(), alpha));
                g2.fillRoundRect(shadowSize - i, shadowSize - i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                
                // Dark shadow (Bottom-Right)
                g2.setColor(new Color(darkShadow.getRed(), darkShadow.getGreen(), darkShadow.getBlue(), alpha));
                g2.fillRoundRect(shadowSize + i, shadowSize + i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            }
            
            // Base Shape
            g2.setColor(baseColor);
            g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class NeumorphicButton extends JButton {
        private int shadowSize = 6;
        private int radius = 20;

        public NeumorphicButton(String text) {
            super(text);
            setForeground(textColor);
            setFont(new Font("SansSerif", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(shadowSize + 5, shadowSize + 15, shadowSize + 5, shadowSize + 15));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            boolean pressed = getModel().isPressed();
            
            if (!pressed) {
                // Convex (Not Pressed)
                for (int i = 0; i <= shadowSize; i++) {
                    int alpha = 90 - (i * 90 / shadowSize);
                    g2.setColor(new Color(lightShadow.getRed(), lightShadow.getGreen(), lightShadow.getBlue(), alpha));
                    g2.fillRoundRect(shadowSize - i, shadowSize - i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                    
                    g2.setColor(new Color(darkShadow.getRed(), darkShadow.getGreen(), darkShadow.getBlue(), alpha));
                    g2.fillRoundRect(shadowSize + i, shadowSize + i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                }
                g2.setColor(baseColor);
                g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            } else {
                // Concave (Pressed / Inset Shadow)
                g2.setColor(baseColor);
                g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                
                Shape baseShape = new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                g2.setClip(baseShape);
                
                for (int i = 1; i <= shadowSize; i++) {
                    int alpha = 90 - (i * 90 / shadowSize);
                    
                    // Dark Inner Shadow (Top-Left) -> offset drawing to Bottom-Right
                    g2.setColor(new Color(darkShadow.getRed(), darkShadow.getGreen(), darkShadow.getBlue(), alpha));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(shadowSize + i, shadowSize + i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                    
                    // Light Inner Shadow (Bottom-Right) -> offset drawing to Top-Left
                    g2.setColor(new Color(lightShadow.getRed(), lightShadow.getGreen(), lightShadow.getBlue(), alpha));
                    g2.drawRoundRect(shadowSize - i, shadowSize - i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                }
            }
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    class NeumorphicTextField extends JTextField {
        private int shadowSize = 6;
        private int radius = 20;

        public NeumorphicTextField(int columns) {
            super(columns);
            setOpaque(false);
            setForeground(textColor);
            setFont(new Font("SansSerif", Font.PLAIN, 15));
            setBorder(new EmptyBorder(shadowSize + 8, shadowSize + 15, shadowSize + 8, shadowSize + 15));
            setBackground(new Color(0, 0, 0, 0)); // Transparent so paintComponent handles it
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Concave (Inset Shadow)
            g2.setColor(baseColor);
            g2.fillRoundRect(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            
            Shape baseShape = new RoundRectangle2D.Float(shadowSize, shadowSize, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            g2.setClip(baseShape);
            
            for (int i = 1; i <= shadowSize; i++) {
                int alpha = 80 - (i * 80 / shadowSize);
                
                // Dark Inner Shadow (Top-Left)
                g2.setColor(new Color(darkShadow.getRed(), darkShadow.getGreen(), darkShadow.getBlue(), alpha));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(shadowSize + i, shadowSize + i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
                
                // Light Inner Shadow (Bottom-Right)
                g2.setColor(new Color(lightShadow.getRed(), lightShadow.getGreen(), lightShadow.getBlue(), alpha));
                g2.drawRoundRect(shadowSize - i, shadowSize - i, getWidth() - shadowSize*2, getHeight() - shadowSize*2, radius, radius);
            }
            
            g2.dispose();
            super.paintComponent(g);
        }
    }
    
    // Sembunyikan visual Scrollbar tapi biarkan fungsinya ada
    class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = baseColor.darker();
            this.trackColor = baseColor;
        }
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }
        private JButton createZeroButton() {
            JButton button = new JButton();
            Dimension zeroDim = new Dimension(0,0);
            button.setPreferredSize(zeroDim);
            button.setMinimumSize(zeroDim);
            button.setMaximumSize(zeroDim);
            return button;
        }
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(163, 177, 198, 100)); // Warna bayangan yang sangat transparan
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);
            g2.dispose();
        }
    }
    
    // ==========================================
    // JDIALOG CUSTOM IMAGE PREVIEW
    // ==========================================

    class ImagePreviewDialog extends JDialog {
        public ImagePreviewDialog(Frame owner, File imageFile) {
            super(owner, "Pratinjau Gambar Neumorphism", true);
            getContentPane().setBackground(baseColor);
            
            NeumorphicPanel rootPanel = new NeumorphicPanel(new BorderLayout(10, 10));
            
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(imageFile);
                if (img != null) {
                    // Logika scaling proporsional maksimal 800x600
                    int maxWidth = 800;
                    int maxHeight = 600;
                    int imgWidth = img.getWidth();
                    int imgHeight = img.getHeight();
                    
                    if (imgWidth > maxWidth || imgHeight > maxHeight) {
                        double widthRatio = (double) maxWidth / imgWidth;
                        double heightRatio = (double) maxHeight / imgHeight;
                        double scale = Math.min(widthRatio, heightRatio);
                        
                        imgWidth = (int) (imgWidth * scale);
                        imgHeight = (int) (imgHeight * scale);
                    }
                    
                    Image scaledImg = img.getScaledInstance(imgWidth, imgHeight, Image.SCALE_SMOOTH);
                    JLabel imgLabel = new JLabel(new ImageIcon(scaledImg));
                    imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    
                    // Beri bingkai tebal putih/terang untuk estetika
                    imgLabel.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,100), 5));
                    
                    rootPanel.add(imgLabel, BorderLayout.CENTER);
                    setSize(imgWidth + 100, imgHeight + 150);
                } else {
                    rootPanel.add(new JLabel("Gagal memuat gambar (format mungkin rusak).", SwingConstants.CENTER), BorderLayout.CENTER);
                    setSize(400, 200);
                }
            } catch (Exception e) {
                rootPanel.add(new JLabel("Error: " + e.getMessage(), SwingConstants.CENTER), BorderLayout.CENTER);
                setSize(400, 200);
            }
            
            NeumorphicButton btnClose = new NeumorphicButton("Tutup Pratinjau");
            btnClose.addActionListener(e -> dispose());
            
            JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            bottomPanel.setBackground(baseColor);
            bottomPanel.add(btnClose);
            
            rootPanel.add(bottomPanel, BorderLayout.SOUTH);
            
            setContentPane(rootPanel);
            setLocationRelativeTo(owner);
        }
    }
}
