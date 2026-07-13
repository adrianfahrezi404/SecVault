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
    private NeumorphicRingPanel ringPanel;
    
    // --- Palet Warna Neumorphism ---
    private final Color baseColor = new Color(236, 240, 243);        // Clean Off-White
    private final Color darkShadow = new Color(209, 217, 230);       // Soft Cool Grey
    private final Color lightShadow = new Color(255, 255, 255);      // Pure White
    private final Color textColor = Color.decode("#475569");         // Slate Gray
    
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
        
        btnSearchRemote = new NeumorphicButton("Cari & Buka");
        btnSearchRemote.setToolTipText("Cari file yang sudah dikirim dan buka langsung dari node tempat file tersebut tersimpan.");
        
        btnSearchDownload = new NeumorphicButton("Cari & Download");
        btnSearchDownload.setToolTipText("Cari file yang sudah dikirim dan unduh (tarik) kembali ke laptop/node Anda ini.");
        
        inputPanel.add(btnPilih);
        inputPanel.add(txtFilename);
        inputPanel.add(btnUpload);
        inputPanel.add(btnSearchRemote);
        inputPanel.add(btnSearchDownload);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        
        rootPanel.add(topPanel, BorderLayout.NORTH);
        
        // --- Panel Tengah (Tabel JTable) ---
        String[] columns = {"Nama File", "Hash", "Disimpan Di", "Jalur Routing", "Status", "Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);
        table.getColumnModel().getColumn(5).setWidth(0);
        
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(250);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        
        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createEmptyBorder());
        scrollTable.getViewport().setBackground(baseColor);
        scrollTable.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        NeumorphicPanel panelCenter = new NeumorphicPanel(new BorderLayout());
        
        JLabel lblStatus = new JLabel("Encrypted Evidence Ledger (Klik ganda baris untuk Dekripsi Sementara)");
        lblStatus.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblStatus.setForeground(textColor);
        lblStatus.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        panelCenter.add(lblStatus, BorderLayout.NORTH);
        panelCenter.add(scrollTable, BorderLayout.CENTER);
        
        // --- Panel Bawah (Log Console JTextArea) ---
        txtLog = new JTextArea(12, 40);
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.BOLD, 13));
        txtLog.setBackground(baseColor);
        txtLog.setForeground(textColor);
        txtLog.setBorder(null); // Menghilangkan border bawaan
        txtLog.setMargin(new Insets(10, 15, 10, 15)); // Padding agar teks tidak mepet
        txtLog.setLineWrap(true);
        txtLog.setWrapStyleWord(true);
        
        JScrollPane scrollLog = new JScrollPane(txtLog);
        scrollLog.setBorder(BorderFactory.createEmptyBorder());
        scrollLog.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        
        NeumorphicPanel panelBottom = new NeumorphicPanel(new BorderLayout());
        
        JLabel lblLog = new JLabel("Log Aktivitas Jaringan");
        lblLog.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblLog.setForeground(textColor);
        lblLog.setBorder(new EmptyBorder(0, 0, 15, 0));
        
        panelBottom.add(lblLog, BorderLayout.NORTH);
        panelBottom.add(scrollLog, BorderLayout.CENTER);
        
        JSplitPane splitPaneLeft = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelCenter, panelBottom);
        splitPaneLeft.setDividerLocation(250);
        splitPaneLeft.setDividerSize(20);
        splitPaneLeft.setBorder(BorderFactory.createEmptyBorder());
        splitPaneLeft.setBackground(baseColor);
        
        // Remove split pane borders
        BasicSplitPaneUI splitPaneUI = (BasicSplitPaneUI) splitPaneLeft.getUI();
        splitPaneUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
        
        // --- TAMBAHAN RADAR & FINGER TABLE ---
        ringPanel = new NeumorphicRingPanel();
        
        JPanel rightPanel = new JPanel(new BorderLayout(15, 15));
        rightPanel.setBackground(baseColor);
        rightPanel.setBorder(new EmptyBorder(0, 15, 0, 0));
        rightPanel.add(ringPanel, BorderLayout.CENTER);
        
        // Tabel Finger Table
        String[] ftCols = {"Index", "Start", "Interval", "Successor"};
        DefaultTableModel ftModel = new DefaultTableModel(ftCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable ftTable = new JTable(ftModel);
        styleTable(ftTable);
        JScrollPane ftScroll = new JScrollPane(ftTable);
        ftScroll.setPreferredSize(new Dimension(300, 140));
        ftScroll.setBorder(BorderFactory.createEmptyBorder());
        
        NeumorphicPanel ftContainer = new NeumorphicPanel(new BorderLayout());
        JLabel lblFT = new JLabel("Tabel Routing (Finger Table DHT)");
        lblFT.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblFT.setForeground(textColor);
        lblFT.setBorder(new EmptyBorder(0, 0, 10, 0));
        ftContainer.add(lblFT, BorderLayout.NORTH);
        ftContainer.add(ftScroll, BorderLayout.CENTER);
        
        rightPanel.add(ftContainer, BorderLayout.SOUTH);
        
        // Isi data Finger Table (Dari Node) dengan Rumus Lengkap MIT Chord
        if (node.fingerTable != null) {
            for (int i = 0; i < node.fingerTable.length; i++) {
                secvault.FingerTable ft = node.fingerTable[i];
                if (ft != null) {
                    int nextStart = (i == node.fingerTable.length - 1) ? node.id : node.fingerTable[i+1].start;
                    String interval = "[" + ft.start + ", " + nextStart + ")";
                    ftModel.addRow(new Object[]{i + 1, ft.start, interval, "Node " + ft.successor});
                }
            }
        }
        
        // --- AUTO-PING UNTUK DETEKSI NODE MATI/HIDUP ---
        startPingTimer();
        
        // Bagi layar menjadi Kiri (Tabel+Log) dan Kanan (Radar + FT)
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPaneLeft, rightPanel);
        mainSplit.setDividerLocation(500);
        mainSplit.setDividerSize(20);
        mainSplit.setBorder(BorderFactory.createEmptyBorder());
        mainSplit.setBackground(baseColor);
        
        BasicSplitPaneUI mainSplitUI = (BasicSplitPaneUI) mainSplit.getUI();
        mainSplitUI.getDivider().setBorder(BorderFactory.createEmptyBorder());
        
        rootPanel.add(mainSplit, BorderLayout.CENTER);
        
        // EVENT LISTENERS
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
                    
                    // --- P2PNode.routeOrProcessUpload akan mengurus animasi Radarnya secara otomatis ---
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
                        String filePath = (String) tableModel.getValueAt(row, 5);
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
    
    public void addTableRow(String filename, int hash, String location, String routingPath, String status, String filePath) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addRow(new Object[]{filename, hash, location, routingPath, status, filePath});
        });
    }
    
    public void showRoutingAnimation(java.util.List<Integer> path) {
        SwingUtilities.invokeLater(() -> {
            if (ringPanel != null) {
                ringPanel.drawRoutingPath(path);
            }
        });
    }
    
    // Guard supaya tidak ada beberapa pengecekan ping yang tumpang tindih (overlap)
    private final java.util.concurrent.atomic.AtomicBoolean isPingingNow =
            new java.util.concurrent.atomic.AtomicBoolean(false);

    // Thread pool khusus untuk ping paralel ke semua node (dibuat sekali, dipakai berulang)
    private final java.util.concurrent.ExecutorService pingExecutor =
            java.util.concurrent.Executors.newFixedThreadPool(6);

    private void startPingTimer() {
        // Melakukan ping ke semua IP node setiap 3 detik secara background
        javax.swing.Timer timer = new javax.swing.Timer(3000, e -> {
            // Kalau pengecekan sebelumnya masih berjalan, lewati tick ini
            if (!isPingingNow.compareAndSet(false, true)) {
                return;
            }

            java.util.List<java.util.concurrent.Future<Integer>> futures = new java.util.ArrayList<>();

            for (java.util.Map.Entry<Integer, String> entry : node.nodeIps.entrySet()) {
                int id = entry.getKey();
                String ip = entry.getValue();

                futures.add(pingExecutor.submit(() -> {
                    if (id == node.id) {
                        return id; // Diri sendiri selalu aktif
                    }
                    try (java.net.Socket s = new java.net.Socket()) {
                        // Coba buka socket koneksi (Ping) dengan timeout lebih longgar (2000ms) untuk Hotspot HP
                        s.connect(new java.net.InetSocketAddress(ip, 8000 + id), 2000);
                        return id;
                    } catch (Exception ex) {
                        return null; 
                    }
                }));
            }

            new Thread(() -> {
                java.util.List<Integer> onlineNodes = new java.util.ArrayList<>();
                for (java.util.concurrent.Future<Integer> f : futures) {
                    try {
                        // Beri waktu toleransi nunggu hasil hingga 2.5 detik (2500ms)
                        Integer result = f.get(2500, java.util.concurrent.TimeUnit.MILLISECONDS);
                        if (result != null) onlineNodes.add(result);
                    } catch (Exception ex) {
                        // Timeout/error pada task individual, anggap node itu offline
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    if (ringPanel != null) {
                        ringPanel.setOnlineNodes(onlineNodes);
                    }
                });

                isPingingNow.set(false);
            }).start();
        });
        timer.start();
    }
    
    // LOGIKA BUKA FILE CERDAS (Smart Viewer)
    public void openFile(File file) {
        if (!file.exists()) {
            appendLog("[ERROR] Evidence fisik tidak ditemukan: " + file.getAbsolutePath());
            return;
        }

        try {
            // Lakukan Dekripsi ke Temporary File di direktori proyek lokal
            // (Linux Snap apps seperti VLC tidak memiliki izin membaca folder /tmp/ bawaan OS)
            File secureDir = new File("./temp_decrypted_node_" + node.id);
            secureDir.mkdirs();
            secureDir.deleteOnExit();
            
            File tempDecrypted = new File(secureDir, "EVIDENCE_" + file.getName());
            tempDecrypted.deleteOnExit(); // Lenyapkan saat aplikasi ditutup
            
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file);
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(tempDecrypted)) {
                CryptoUtils.decryptStream(fis, fos, masterKey);
            }
            
            // Baca nama asli file untuk smart viewer logic
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                new ImagePreviewDialog(tempDecrypted).setVisible(true);
                appendLog("[DECRYPT] Menampilkan pratinjau bukti: " + file.getName());
            } else {
                boolean opened = false;
                if (Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().open(tempDecrypted);
                        appendLog("[DECRYPT] Mendelegasikan dekripsi bukti " + file.getName() + " ke aplikasi OS.");
                        opened = true;
                    } catch (Exception e) {
                        // Gagal via Desktop API (Sering terjadi bug URI %20 di Linux GNOME)
                    }
                }
                
                if (!opened) {
                    // Fallback Manual
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("linux")) {
                        if (name.endsWith(".mp3") || name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") || name.endsWith(".ogg")) {
                            Runtime.getRuntime().exec(new String[]{"firefox", tempDecrypted.getAbsolutePath()});
                            appendLog("[DECRYPT] Membuka media via Firefox: " + file.getName());
                        } else {
                            Runtime.getRuntime().exec(new String[]{"xdg-open", tempDecrypted.getAbsolutePath()});
                            appendLog("[DECRYPT] Membuka via xdg-open: " + file.getName());
                        }
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec(new String[]{"open", tempDecrypted.getAbsolutePath()});
                        appendLog("[DECRYPT] Membuka via open (Mac): " + file.getName());
                    } else if (os.contains("win")) {
                        Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", tempDecrypted.getAbsolutePath()});
                        appendLog("[DECRYPT] Membuka via CMD: " + file.getName());
                    } else {
                        showNeumorphicError("Sistem operasi Anda tidak mendukung fitur untuk membuka file ini secara otomatis.");
                    }
                }
            }
        } catch (Exception ex) {
            showNeumorphicError("Master Key Salah atau File Korup!\nGagal mendekripsi bukti: " + ex.getMessage());
        }
    }
    
    public void showNeumorphicError(String message) {
        JDialog dialog = new JDialog(this, "Peringatan Sistem", true);
        dialog.setSize(450, 220);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(baseColor);
        
        NeumorphicPanel rootPanel = new NeumorphicPanel(new BorderLayout(15, 15));
        
        JLabel lblMsg = new JLabel("<html><div style='text-align: center; color: #2C3E50; width: 350px;'>" + message.replace("\n", "<br>") + "</div></html>");
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
    
    // CLASS UI NEUMORPHISM
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
            setForeground(Color.decode("#334155"));
            setFont(new Font("SansSerif", Font.BOLD, 12));
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
            setForeground(Color.decode("#334155"));
            setFont(new Font("SansSerif", Font.BOLD, 13));
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
            this.thumbColor = new Color(180, 190, 200);
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
            g2.setColor(new Color(150, 160, 170, 100)); // Warna bayangan yang sangat transparan
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);
            g2.dispose();
        }
    }

    // JFRAME CUSTOM IMAGE PREVIEW (Bisa Minimize & Maximize)
    class ImagePreviewDialog extends JFrame {
        public ImagePreviewDialog(File imageFile) {
            super("Pratinjau Gambar Neumorphism - " + imageFile.getName());
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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
            setLocationRelativeTo(null);
        }
    }
}