import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.Border;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class Visualizer {

    static class SandpilePanel extends JPanel {
        private final SandpileSimulation.Sandpile sp;
        private boolean showGrid = true;
        private final Color[] palette = new Color[] {
            new Color(0, 0, 0),        // 0
            new Color(255, 236, 203),  // 1
            new Color(255, 207, 121),  // 2
            new Color(255, 178, 38)    // 3
        };

        SandpilePanel(SandpileSimulation.Sandpile sp) {
            this.sp = sp;

            setBackground(Color.BLACK);
            setPreferredSize(new Dimension(256, 256));
            setDoubleBuffered(true);

            Border blackborder = BorderFactory.createLineBorder(Color.black, 12);
            setBorder(blackborder);

            setLayout(new GridBagLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Container parent = getParent();
            if (parent != null) {
                int w = parent.getWidth();
                int h = parent.getHeight();

                if (w == 0 || h == 0) return new Dimension(256, 256);

                int size = Math.min(w, h);

                return new Dimension(size, size);
            }
            return new Dimension(256, 256);
        }

        public void setShowGrid(boolean show) {
            this.showGrid = show;
            repaint();
        }

        public void refresh() {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int[][] grid = sp.snapshot();
            int n = grid.length;

            Insets in = getInsets();
            int x0 = in.left, y0 = in.top;

            int w = getWidth() - in.left - in.right;
            int h = getHeight() - in.top - in.bottom;

            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            double cellW = w / (double) n;
            double cellH = h / (double) n;

            // Draw cells
            for (int r = 0; r < n; r++) {
                double y = y0 + r * cellH;
                for (int c = 0; c < n; c++) {
                    double x = x0 + c * cellW;
                    int v = grid[r][c];
                    int idx = Math.max(0, Math.min(v, 3));
                    g2.setColor(palette[idx]);
                    g2.fill(new java.awt.geom.Rectangle2D.Double(x, y, cellW, cellH));
                }
            }

            if (showGrid && cellW >= 6 && cellH >= 6) {
                g2.setColor(new Color(0,0,0,70));
                for (int r = 0; r <= n; r++) {
                    double yy = y0 + r * cellH;
                    g2.draw(new java.awt.geom.Line2D.Double(x0, yy, x0 + w, yy));
                }
                for (int c = 0; c <= n; c++) {
                    double xx = x0 + c * cellW;
                    g2.draw(new java.awt.geom.Line2D.Double(xx, y0, xx, y0+h));
                }
            }

            g2.dispose();
        }
    }

    // -------- Main Visualizer Controls --------
    private JFrame frame;
    private SandpileSimulation.Sandpile sandpile;
    private SandpilePanel panel;
    private JLabel statusLabel;

    private SwingWorker<Void, Void> worker;
    private JSlider speedSlider;

    public Visualizer(int n, long seed) {
        sandpile = new SandpileSimulation.Sandpile(n, seed);
        panel = new SandpilePanel(sandpile);

        frame = new JFrame("Sandpile Visualization (Relaxation Only)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.getContentPane().setLayout(new BorderLayout());

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(Color.DARK_GRAY);

        centerWrapper.add(panel, new GridBagConstraints());


        frame.add(centerWrapper, BorderLayout.CENTER);
        frame.add(createControls(), BorderLayout.SOUTH);
        frame.setSize(400, 400);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        updateStatus("Ready");
    }

    private JPanel createControls() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton dropBtn = new JButton("Drop 1");
        JButton autoBtn = new JButton("Auto Run");
        JButton stopBtn = new JButton("Stop");
        JButton resetBtn = new JButton("Reset");
        JButton printBtn = new JButton("Print");

        JCheckBox gridChk = new JCheckBox("Grid", true);
        statusLabel = new JLabel("Status");

        speedSlider = new JSlider(1, 60, 10);
        speedSlider.setPreferredSize(new Dimension(150, 40));
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setMajorTickSpacing(10);
        speedSlider.setMinorTickSpacing(1);

        dropBtn.addActionListener(e -> doDrop());
        autoBtn.addActionListener(e -> startAuto());
        stopBtn.addActionListener(e -> stopAuto());
        resetBtn.addActionListener(e -> {
            stopAuto();
            sandpile.reset();
            panel.refresh();
            updateStatus("Reset");
        });
        printBtn.addActionListener(e -> {
            sandpile.print(64);
            updateStatus("Printed");
        });

        gridChk.addActionListener(e -> panel.setShowGrid(gridChk.isSelected()));

        p.add(dropBtn);
        p.add(autoBtn);
        p.add(stopBtn);
        p.add(resetBtn);
        p.add(printBtn);
        p.add(new JLabel("Speed:"));
        p.add(speedSlider);
        p.add(gridChk);
        p.add(Box.createHorizontalStrut(10));
        p.add(statusLabel);
        return p;
    }

    private void doDrop() {
        stopAuto();
        int avalancheSize = sandpile.dropGrainAndRelax();
        panel.refresh();
        updateStatus("Avalanche size = " + avalancheSize);
    }

    private void startAuto() {
        stopAuto();
        worker = new SwingWorker<>() {

            @Override
            protected Void doInBackground() {
                while (!isCancelled()) {
                    int aval = sandpile.dropGrainAndRelax();
                    SwingUtilities.invokeLater(panel::refresh);

                    long interval = 1000L / speedSlider.getValue();
                    try { Thread.sleep(interval); } catch (InterruptedException ignored) {}

                    SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Auto | Avalanche=" + aval + " | Grains=" + totalGrains())
                    );
                }
                return null;
            }

            @Override
            protected void done() {
                updateStatus("Stopped");
            }
        };
        updateStatus("Running…");
        worker.execute();
    }

    private void stopAuto() {
        if (worker != null && !worker.isDone()) worker.cancel(true);
        worker = null;
    }

    private long totalGrains() {
        int[][] grid = sandpile.snapshot();
        long sum = 0;
        for (int[] row : grid)
            for (int v : row)
                sum += v;
        return sum;
    }

    private void updateStatus(String s) {
        statusLabel.setText(s + " | Grains=" + totalGrains());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Visualizer(64, 42));
    }
}
