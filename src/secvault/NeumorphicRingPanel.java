package secvault;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;

public class NeumorphicRingPanel extends JPanel {
    // Palet warna dasar Neumorphism
    private final Color baseColor = new Color(236, 240, 243);
    private final Color darkShadow = new Color(209, 217, 230);
    private final Color lightShadow = new Color(255, 255, 255);
    private final Color textColor = Color.decode("#475569");

    // Palet Warna Status Node
    private final Color onlineColor = Color.decode("#0284C7");    // Biru Elegan
    private final Color offlineColor = Color.decode("#94A3B8");   // Abu-abu Node Mati
    private final Color pathColor = Color.decode("#F59E0B");      // Oranye Garis Rute
    private final Color successorColor = Color.decode("#10B981"); // Hijau Target
    private final Color emptyColor = new Color(215, 220, 225);    // Very light grey

    // Node yang dikonfigurasi dalam arsitektur sistem (Tetap akan tampil di GUI)
    private final int[] configuredNodes = {3, 7, 11, 17, 22, 28};
    
    // Status aktual dari hasil Ping jaringan
    private Set<Integer> onlineNodes = new HashSet<>(Arrays.asList(3, 7, 11, 17, 22, 28));
    
    // Path Hop-by-Hop
    private List<Integer> currentPath;

    public NeumorphicRingPanel() {
        setBackground(baseColor);
        setPreferredSize(new Dimension(500, 550));
        setOpaque(true);
    }

    /**
     * Memperbarui daftar node yang aktif (hidup) berdasarkan ping
     */
    public void setOnlineNodes(List<Integer> onlineIds) {
        if (onlineIds != null) {
            this.onlineNodes = new HashSet<>(onlineIds);
            repaint();
        }
    }

    /**
     * Menggambar animasi garis rute (contoh: [3, 7, 11])
     */
    public void drawRoutingPath(List<Integer> hopPath) {
        this.currentPath = hopPath;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int padding = 60; // Padding dikurangi agar cincin tidak crash di resolusi sempit
        int diameter = Math.max(50, Math.min(width, height) - (padding * 2)); // Amankan diameter minimum
        int centerX = width / 2;
        int centerY = (height / 2) - 30; 
        int radius = diameter / 2;

        // ===============================================
        // 1. Gambar Base Ring (Lingkaran Utama Neumorphism)
        // ===============================================
        g2.setColor(baseColor);
        g2.fillOval(centerX - radius, centerY - radius, diameter, diameter);
        
        // Efek Inset Shadow
        Shape ringClip = new java.awt.geom.Ellipse2D.Float(centerX - radius, centerY - radius, diameter, diameter);
        g2.setClip(ringClip);
        int shadowSize = 8;
        for (int i = 1; i <= shadowSize; i++) {
            int alpha = 80 - (i * 80 / shadowSize);
            g2.setColor(new Color(darkShadow.getRed(), darkShadow.getGreen(), darkShadow.getBlue(), alpha));
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(centerX - radius + i, centerY - radius + i, diameter, diameter);
            
            g2.setColor(new Color(lightShadow.getRed(), lightShadow.getGreen(), lightShadow.getBlue(), alpha));
            g2.drawOval(centerX - radius - i, centerY - radius - i, diameter, diameter);
        }
        g2.setClip(null);

        // ===============================================
        // 2. Gambar Garis Rute (Hop-by-Hop) dengan Panah
        // ===============================================
        if (currentPath != null && currentPath.size() >= 2) {
            g2.setColor(new Color(pathColor.getRed(), pathColor.getGreen(), pathColor.getBlue(), 200));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            for (int i = 0; i < currentPath.size() - 1; i++) {
                int startNode = currentPath.get(i);
                int endNode = currentPath.get(i + 1);
                Point p1 = getNodePosition(startNode, centerX, centerY, radius);
                Point p2 = getNodePosition(endNode, centerX, centerY, radius);
                
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
                drawArrow(g2, p1.x, p1.y, p2.x, p2.y, 14); 
            }
        }

        // ===============================================
        // 3. Gambar 32 Titik Slot DHT
        // ===============================================
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();

        for (int i = 0; i < 32; i++) {
            Point p = getNodePosition(i, centerX, centerY, radius);
            boolean isConfigured = isConfiguredNode(i);
            
            if (isConfigured) {
                // --- Node Terkonfigurasi (Tetap tampil walau mati) ---
                int nodeRadius = 14;
                boolean isOnline = onlineNodes.contains(i);
                
                Color nodeColor = isOnline ? onlineColor : offlineColor; 
                
                if (currentPath != null && currentPath.contains(i)) {
                    if (currentPath.get(currentPath.size() - 1) == i) {
                        nodeColor = successorColor; // Hijau (Akhir)
                    } else {
                        nodeColor = pathColor;      // Oranye (Dilewati)
                    }
                }
                
                // Efek 3D Shadow Neumorphism (Convex) - Tetap dirender walau offline agar tombol terlihat
                g2.setColor(lightShadow);
                g2.fillOval(p.x - nodeRadius - 2, p.y - nodeRadius - 2, nodeRadius * 2, nodeRadius * 2);
                g2.setColor(darkShadow);
                g2.fillOval(p.x - nodeRadius + 2, p.y - nodeRadius + 2, nodeRadius * 2, nodeRadius * 2);
                
                // Fill warna utama (Biru jika online, Abu-abu jika offline)
                g2.setColor(nodeColor);
                g2.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                
                // Teks Label "N{id}"
                String label = "N" + i;
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.setColor(textColor);
                double angle = (i / 32.0) * 2 * Math.PI - (Math.PI / 2);
                int labelX = p.x + (int) (Math.cos(angle) * 30) - (g2.getFontMetrics().stringWidth(label) / 2);
                int labelY = p.y + (int) (Math.sin(angle) * 30) + (g2.getFontMetrics().getAscent() / 2);
                g2.drawString(label, labelX, labelY);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 11)); 
                
            } else {
                // --- Slot Kosong Mutlak (Titik pipih kecil) ---
                int emptyRadius = 4;
                g2.setColor(emptyColor); 
                g2.fillOval(p.x - emptyRadius, p.y - emptyRadius, emptyRadius * 2, emptyRadius * 2);
                
                // Label angka slot (0-31)
                String label = String.valueOf(i);
                g2.setColor(new Color(156, 163, 175)); 
                double angle = (i / 32.0) * 2 * Math.PI - (Math.PI / 2);
                int labelX = p.x + (int) (Math.cos(angle) * 20) - (fm.stringWidth(label) / 2);
                int labelY = p.y + (int) (Math.sin(angle) * 20) + (fm.getAscent() / 2);
                g2.drawString(label, labelX, labelY);
            }
        }

        // ===============================================
        // 4. Gambar Legend di Kiri Bawah
        // ===============================================
        drawLegend(g2, 20, height - 120);

        g2.dispose();
    }

    private boolean isConfiguredNode(int id) {
        for (int n : configuredNodes) {
            if (n == id) return true;
        }
        return false;
    }

    private Point getNodePosition(int nodeId, int centerX, int centerY, int radius) {
        double angle = (nodeId / 32.0) * 2 * Math.PI - (Math.PI / 2);
        int x = centerX + (int) (Math.cos(angle) * radius);
        int y = centerY + (int) (Math.sin(angle) * radius);
        return new Point(x, y);
    }
    
    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, int arrowOffset) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = 12;
        
        int targetX = x2 - (int) (arrowOffset * Math.cos(angle));
        int targetY = y2 - (int) (arrowOffset * Math.sin(angle));
        
        int xA = targetX - (int) (arrowSize * Math.cos(angle - Math.PI / 6));
        int yA = targetY - (int) (arrowSize * Math.sin(angle - Math.PI / 6));
        int xB = targetX - (int) (arrowSize * Math.cos(angle + Math.PI / 6));
        int yB = targetY - (int) (arrowSize * Math.sin(angle + Math.PI / 6));
        
        g2.drawLine(targetX, targetY, xA, yA);
        g2.drawLine(targetX, targetY, xB, yB);
    }

    private void drawLegend(Graphics2D g2, int x, int y) {
        int dotSize = 12;
        int spacing = 20;
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        
        // Node Online
        g2.setColor(onlineColor);
        g2.fillOval(x, y, dotSize, dotSize);
        g2.setColor(textColor);
        g2.drawString("Node Online (Aktif)", x + 20, y + 10);
        
        // Node Offline
        g2.setColor(offlineColor);
        g2.fillOval(x, y + spacing, dotSize, dotSize);
        g2.setColor(textColor);
        g2.drawString("Node Offline (Mati)", x + 20, y + spacing + 10);
        
        // Dilewati saat Search
        g2.setColor(pathColor);
        g2.fillOval(x, y + spacing * 2, dotSize, dotSize);
        g2.setColor(textColor);
        g2.drawString("Dilewati saat Search", x + 20, y + spacing * 2 + 10);
        
        // Successor (Ditemukan)
        g2.setColor(successorColor);
        g2.fillOval(x, y + spacing * 3, dotSize, dotSize);
        g2.setColor(textColor);
        g2.drawString("Successor (Ditemukan)", x + 20, y + spacing * 3 + 10);
        
        // Slot Kosong
        g2.setColor(emptyColor);
        g2.fillOval(x + 2, y + spacing * 4 + 2, dotSize - 4, dotSize - 4); 
        g2.setColor(textColor);
        g2.drawString("Slot Kosong", x + 20, y + spacing * 4 + 10);
    }
}
