package snakegame;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends JFrame {

    static final String NAME1 = "GLOW", NAME2 = "WORM";
    static final int W = 700, H = 650;
    static final int[][] STARS = makeStars();

    CardLayout cards = new CardLayout();
    JPanel container = new JPanel(cards);
    GamePanel gamePanel = new GamePanel(this);
    ScorePanel scorePanel = new ScorePanel(this);

    public SnakeGame() {
        super(NAME1 + " " + NAME2);
        container.add(new MenuPanel(this), "menu");
        container.add(new DifficultyPanel(this), "difficulty");
        container.add(gamePanel, "game");
        container.add(scorePanel, "scores");
        add(container);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        Sound.init();
        Sound.startMusic();
        showMenu();
    }

    void showMenu() { gamePanel.stopGame(); cards.show(container, "menu"); }
    void showDifficulty() { cards.show(container, "difficulty"); }
    void showScores() { scorePanel.refresh(); cards.show(container, "scores"); }

    void showGame(int difficulty) {
        cards.show(container, "game");
        gamePanel.diff = difficulty;
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    // Shared helpers
    static int[][] makeStars() {
        Random r = new Random(7);
        int[][] s = new int[70][3];
        for (int[] st : s) { st[0] = r.nextInt(W); st[1] = r.nextInt(H); st[2] = 1 + r.nextInt(3); }
        return s;
    }

    static Color C(int r, int g, int b, int a) { return new Color(r, g, b, a); }
    static Color C(int r, int g, int b) { return new Color(r, g, b); }
    static Color alpha(Color c, int a) { return C(c.getRed(), c.getGreen(), c.getBlue(), a); }
    static Color hsb(float h, float s, float b) { return Color.getHSBColor(h, s, b); }
    static void stroke(Graphics2D g, float w) { g.setStroke(new BasicStroke(w)); }
    static void circle(Graphics2D g, int cx, int cy, int r, Color c) { g.setColor(c); g.fillOval(cx - r, cy - r, 2 * r, 2 * r); }
    static void fill(Graphics2D g, Color c, int x, int y, int w, int h) { g.setColor(c); g.fillRect(x, y, w, h); }
    static void antialias(Graphics2D g) { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); }

    static void drawCentered(Graphics2D g, String s, int y) {
        g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y);
    }

    static void txt(Graphics2D g, String s, int y, int size, int style, Color c) {
        g.setFont(new Font("Arial", style, size));
        g.setColor(c);
        drawCentered(g, s, y);
    }

    // text with a drop shadow
    static void shadowTxt(Graphics2D g, String s, int y, int size, Color shadow, Color c, int off) {
        txt(g, s, y + off, size, Font.BOLD, shadow);
        txt(g, s, y, size, Font.BOLD, c);
    }

    static void drawBackground(Graphics2D g, int phase) {
        float h1 = (phase % 1200) / 1200f, h2 = ((phase + 400) % 1200) / 1200f;
        g.setPaint(new GradientPaint(0, 0, hsb(h1, 0.7f, 0.35f), W, H, hsb(h2, 0.7f, 0.2f)));
        g.fillRect(0, 0, W, H);
        for (int i = 0; i < STARS.length; i++) {
            int bright = 120 + (int) (120 * Math.abs(Math.sin((phase + i * 17) / 20.0)));
            g.setColor(C(255, 255, 255, bright));
            g.fillOval(STARS[i][0], STARS[i][1], STARS[i][2], STARS[i][2]);
        }
    }

    static JButton makeButton(String text, int y, Runnable action) {
        return makeButton(text, y, action, C(0, 200, 120));
    }

    static JButton makeButton(String text, int y, Runnable action, Color color) {
        FancyButton b = new FancyButton(text, color);
        b.setBounds(250, y, 200, 50);
        b.addActionListener(e -> { Sound.play(Sound.click); action.run(); });
        return b;
    }

    static JButton place(JButton b, int x, int y, int w, int h) { b.setBounds(x, y, w, h); return b; }

    // fixed size, absolute layout, 30ms animation timer
    static class AnimPanel extends JPanel {
        int phase = 0;
        AnimPanel() {
            setPreferredSize(new Dimension(W, H));
            setLayout(null);
            new Timer(30, e -> { tick(); repaint(); }).start();
        }
        void tick() { phase++; }
    }

    // FANCY Button
    static class FancyButton extends JButton {
        Color base;

        FancyButton(String text, Color base) {
            super(text);
            this.base = base;
            setFont(new Font("Arial", Font.BOLD, 20));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            antialias(g2);
            boolean hover = getModel().isRollover(), press = getModel().isArmed();
            Color top = press ? base.darker() : hover ? base.brighter() : base;
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), base.darker()));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            g2.setColor(C(255, 255, 255, hover ? 220 : 90));
            stroke(g2, 2);
            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 30, 30);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Color.WHITE);
            g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // Main menu
    static class MenuPanel extends AnimPanel {
        float[][] orbs = new float[18][4];

        MenuPanel(SnakeGame f) {
            add(makeButton("START", 330, f::showDifficulty, C(90, 200, 160)));
            add(makeButton("HIGH SCORE", 395, f::showScores, C(160, 130, 255)));
            add(makeButton("EXIT", 460, () -> System.exit(0), C(255, 120, 150)));
            Random r = new Random(3);
            for (float[] o : orbs) {
                o[0] = r.nextInt(W);
                o[1] = r.nextInt(H);
                o[2] = 8 + r.nextInt(18);
                o[3] = 0.4f + r.nextFloat() * 1.2f;
            }
        }

        @Override
        void tick() {
            phase++;
            for (float[] o : orbs) {
                o[1] -= o[3];
                if (o[1] < -o[2]) o[1] = H + o[2];
            }
        }

        void pastelBackground(Graphics2D g2) {
            float h1 = (phase % 1800) / 1800f;
            g2.setPaint(new GradientPaint(0, 0, hsb(h1, 0.25f, 1f), W, H, hsb(h1 + 0.18f, 0.30f, 1f)));
            g2.fillRect(0, 0, W, H);
            for (int i = 0; i < STARS.length; i++) {
                int a = 90 + (int) (110 * Math.abs(Math.sin((phase + i * 17) / 25.0)));
                g2.setColor(C(255, 255, 255, a));
                int s = STARS[i][2] + 1;
                g2.fillOval(STARS[i][0], STARS[i][1], s, s);
            }
        }

        void glowTitle(Graphics2D g2, String s, int y, float hueShift) {
            int x = (W - g2.getFontMetrics().stringWidth(s)) / 2;
            float h = (phase % 360) / 360f + hueShift;
            g2.setColor(C(255, 255, 255, 40));
            for (int k = 10; k >= 4; k -= 2) {
                g2.drawString(s, x - k, y);
                g2.drawString(s, x + k, y);
                g2.drawString(s, x, y - k);
                g2.drawString(s, x, y + k);
            }
            g2.setColor(C(110, 80, 160, 90));
            g2.drawString(s, x + 5, y + 6);
            g2.setColor(Color.WHITE);
            for (int ox = -3; ox <= 3; ox += 3)
                for (int oy = -3; oy <= 3; oy += 3)
                    if (ox != 0 || oy != 0) g2.drawString(s, x + ox, y + oy);
            g2.setPaint(new GradientPaint(x, y - 60, hsb(h, 0.55f, 0.95f), x, y, hsb(h + 0.2f, 0.60f, 0.80f)));
            g2.drawString(s, x, y);
        }

        void drawBubbles(Graphics2D g2) {
            for (int i = 0; i < orbs.length; i++) {
                float[] o = orbs[i];
                Color c = hsb(((phase / 3 + i * 20) % 360) / 360f, 0.35f, 1f);
                int s = (int) o[2], x = (int) o[0], y = (int) o[1];
                circle(g2, x, y, s, alpha(c, 90));
                g2.setColor(C(255, 255, 255, 150));
                stroke(g2, 2);
                g2.drawOval(x - s, y - s, s * 2, s * 2);
                g2.fillOval(x - s / 2, y - s / 2, s / 3 + 2, s / 3 + 2);
                stroke(g2, 1);
            }
        }

        void drawGiantSnake(Graphics2D g2) {
            int segs = 26, gap = 17;
            int headX = (phase * 4) % (W + segs * gap + 100) - 50;
            for (int i = segs - 1; i >= 0; i--) {
                int x = headX - i * gap;
                if (x < -30 || x > W + 30) continue;
                int y = 255 + (int) (Math.sin(x / 45.0 + phase / 12.0) * 22);
                int size = Math.max(15, 28 - i / 2);
                g2.setColor(hsb(((phase * 2 + i * 12) % 360) / 360f, 0.5f, 1f));
                g2.fillOval(x - size / 2, y - size / 2, size, size);
                g2.setColor(C(255, 255, 255, 220));
                stroke(g2, 2);
                g2.drawOval(x - size / 2, y - size / 2, size, size);
                stroke(g2, 1);
                if (i != 0) continue;
                if ((phase / 6) % 2 == 0) { // flicking tongue
                    g2.setColor(C(255, 110, 140));
                    stroke(g2, 3);
                    g2.drawLine(x + 14, y, x + 28, y);
                    g2.drawLine(x + 28, y, x + 34, y - 5);
                    g2.drawLine(x + 28, y, x + 34, y + 5);
                    stroke(g2, 1);
                }
                g2.setColor(Color.WHITE);
                g2.fillOval(x + 1, y - 10, 10, 10);
                g2.fillOval(x + 1, y + 1, 10, 10);
                g2.setColor(C(60, 40, 90));
                g2.fillOval(x + 5, y - 7, 4, 4);
                g2.fillOval(x + 5, y + 4, 4, 4);
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            antialias(g2);
            pastelBackground(g2);
            drawBubbles(g2);
            drawGiantSnake(g2);

            g2.setFont(new Font("Arial", Font.BOLD, 78));
            int bob = (int) (5 * Math.sin(phase / 15.0));
            glowTitle(g2, NAME1, 110 + bob, 0f);
            glowTitle(g2, NAME2, 190 - bob, 0.15f);

            txt(g2, "Eat. Glow. Don't crash!", 550, 18, Font.ITALIC, C(120, 80, 160));
            txt(g2, "Arrows / WASD = move     P = pause     ESC = menu", 590, 13, Font.PLAIN, C(120, 80, 160, 170));
        }
    }

    // Difficulty page
    static class DifficultyPanel extends AnimPanel {
        static final String[] NAMES = {"EASY", "NORMAL", "HARD"};
        static final String[] DESC = {"Fewer obstacles", "Moderate obstacles", "More obstacles"};
        static final String[] SUB = {"Perfect for beginners", "Recommended", "For experienced players"};
        static final String[] SYMBOLS = {"•", "★", "✦"};
        static final Color[] COLORS = {C(90, 200, 160), C(255, 170, 60), C(240, 80, 110)};
        static final int CARD_W = 190, CARD_H = 255, GAP = 20;
        static final int START_X = (W - (CARD_W * 3 + GAP * 2)) / 2;

        DifficultyPanel(SnakeGame f) {
            for (int i = 0; i < 3; i++) {
                final int d = i;
                add(place(makeButton("PLAY", 395, () -> f.showGame(d), COLORS[i]),
                        START_X + (CARD_W + GAP) * i + 25, 395, 140, 45));
            }
            add(place(makeButton("BACK", 565, f::showMenu, C(140, 90, 255)), W / 2 - 100, 565, 200, 45));
        }

        void drawCard(Graphics2D g2, int x, int y, int idx) {
            int w = CARD_W, h = CARD_H;
            Color color = COLORS[idx];
            int glow = 25 + (int) (8 * Math.sin((phase + idx * 30) / 12.0));
            g2.setColor(alpha(color, 25));
            g2.fillRoundRect(x - glow / 2, y - glow / 2, w + glow, h + glow, 35, 35);

            g2.setPaint(new GradientPaint(x, y, C(255, 255, 255, 240), x, y + h, C(245, 245, 255, 220)));
            g2.fillRoundRect(x, y, w, h, 30, 30);
            g2.setColor(alpha(color, 210));
            stroke(g2, 3);
            g2.drawRoundRect(x, y, w, h, 30, 30);
            stroke(g2, 1);

            int pulse = 30 + (int) (3 * Math.sin(phase / 8.0));
            circle(g2, x + w / 2, y + 18, pulse, alpha(color, 35));
            circle(g2, x + w / 2, y + 44, 24, color);

            // icon
            g2.setFont(new Font("Arial", Font.BOLD, 24));
            g2.setColor(Color.WHITE);
            g2.drawString(SYMBOLS[idx], x + w / 2 - g2.getFontMetrics().stringWidth(SYMBOLS[idx]) / 2, y + 53);

            cardText(g2, NAMES[idx], x + w / 2, y + 105, 25, Font.BOLD, C(55, 45, 75));
            cardText(g2, DESC[idx], x + w / 2, y + 132, 12, Font.PLAIN, C(80, 70, 100));
            cardText(g2, SUB[idx], x + w / 2, y + 153, 12, Font.ITALIC, C(120, 105, 135));

            String stars = "";
            for (int i = 0; i < 3; i++) stars += i <= idx ? "★" : "☆";
            cardText(g2, stars, x + w / 2, y + 188, 18, Font.BOLD, color);

            g2.setColor(C(120, 110, 140, 70));
            g2.fillRoundRect(x + 30, y + 205, w - 60, 1, 1, 1);
        }

        void cardText(Graphics2D g2, String s, int cx, int y, int size, int style, Color c) {
            g2.setFont(new Font("Arial", style, size));
            g2.setColor(c);
            g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, y);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            antialias(g2);
            drawBackground(g2, phase);
            fill(g2, C(0, 0, 0, 80), 0, 0, W, H);

            shadowTxt(g2, "CHOOSE YOUR MODE", 84, 42, C(0, 0, 0, 130), Color.WHITE, 5);
            txt(g2, "How much of a challenge are you ready for?", 113, 15, Font.PLAIN, C(255, 255, 255, 210));

            for (int i = 0; i < 3; i++) drawCard(g2, START_X + (CARD_W + GAP) * i, 145, i);

            txt(g2, "Normal mode is recommended for your first game", 535, 12, Font.PLAIN, C(255, 255, 255, 180));
            txt(g2, "Use ← → ↑ ↓ or WASD to move", 555, 12, Font.PLAIN, C(255, 255, 255, 120));
        }
    }

    // High score page
    static class ScorePanel extends AnimPanel {
        int appear = 0;
        List<Scores.Entry> list = new ArrayList<>();

        static final Color[] RANK = {C(255, 215, 0), C(205, 210, 220), C(205, 127, 50), C(80, 160, 255), C(180, 100, 255)};
        static final String[] TITLE = {"LEGEND", "MASTER", "PRO", "ROOKIE", "NEWBIE"};

        ScorePanel(SnakeGame f) {
            add(place(makeButton("RESET SCORES", 515, () -> {
                int r = JOptionPane.showConfirmDialog(this, "Reset all high scores?", NAME1 + " " + NAME2, JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION) { Scores.reset(); refresh(); }
            }, C(255, 120, 150)), W / 2 - 100, 515, 200, 45));
            add(place(makeButton("BACK", 570, f::showMenu), W / 2 - 100, 570, 200, 45));
        }

        @Override
        void tick() { phase++; appear++; }

        void refresh() { list = Scores.load(); appear = 0; }

        void drawTrophy(Graphics2D g2, int cx) {
            int bob = (int) (4 * Math.sin(phase / 10.0));
            g2.translate(0, bob);
            g2.setColor(C(255, 200 + (int) (30 * Math.sin(phase / 8.0)), 40));
            g2.fillRect(cx - 32, 20, 64, 30);
            g2.fillArc(cx - 32, 20, 64, 60, 180, 180);
            stroke(g2, 5);
            g2.drawArc(cx - 52, 25, 40, 28, 90, 180);
            g2.drawArc(cx + 12, 25, 40, 28, 270, 180);
            stroke(g2, 1);
            g2.fillRect(cx - 6, 80, 12, 14);
            g2.fillRoundRect(cx - 24, 94, 48, 10, 6, 6);
            g2.setColor(C(255, 255, 255, 120));
            g2.fillRoundRect(cx - 22, 24, 8, 36, 8, 8);
            g2.translate(0, -bob);

            int[] sxs = {-85, 80, -60, 65}, sys = {35, 25, 85, 80};
            g2.setColor(Color.WHITE);
            stroke(g2, 2);
            for (int k = 0; k < 4; k++) {
                int size = 3 + (int) (4 * Math.abs(Math.sin((phase + k * 20) / 10.0)));
                int sx = cx + sxs[k], sy = sys[k];
                g2.drawLine(sx - size, sy, sx + size, sy);
                g2.drawLine(sx, sy - size, sx, sy + size);
            }
            stroke(g2, 1);
        }

        void drawRow(Graphics2D g2, int i) {
            int t = appear - i * 6;
            if (t < 0) return;
            int x = 150 - (t >= 20 ? 0 : (20 - t) * 20), y = 185 + i * 62;
            boolean has = i < list.size();
            Color c = RANK[i], dim = C(255, 255, 255, 90);

            g2.setPaint(new GradientPaint(x, y, alpha(c, has ? 120 : 40), x + 400, y, C(0, 0, 0, has ? 120 : 60)));
            g2.fillRoundRect(x, y, 400, 52, 26, 26);

            if (has) {
                int a = i == 0 ? 150 + (int) (105 * Math.abs(Math.sin(phase / 12.0))) : 255;
                g2.setColor(alpha(c, a));
                stroke(g2, i == 0 ? 3 : 2);
            } else {
                g2.setColor(C(255, 255, 255, 50));
                stroke(g2, 2);
            }
            g2.drawRoundRect(x, y, 400, 52, 26, 26);
            stroke(g2, 1);

            // medal
            g2.setColor(c);
            g2.fillOval(x + 8, y + 8, 36, 36);
            g2.setColor(C(40, 40, 40));
            g2.setFont(new Font("Arial", Font.BOLD, 20));
            String n = "" + (i + 1);
            g2.drawString(n, x + 26 - g2.getFontMetrics().stringWidth(n) / 2, y + 33);

            // name, rank title, score
            g2.setColor(has ? Color.WHITE : dim);
            g2.drawString(has ? list.get(i).name : "---", x + 60, y + 27);
            g2.setFont(new Font("Arial", Font.BOLD, 11));
            g2.setColor(has ? c : dim);
            g2.drawString(TITLE[i], x + 60, y + 44);
            g2.setFont(new Font("Arial", Font.BOLD, 30));
            g2.setColor(has ? (i == 0 ? RANK[0] : Color.WHITE) : dim);
            String s = has ? "" + list.get(i).score : "---";
            g2.drawString(s, x + 385 - g2.getFontMetrics().stringWidth(s), y + 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            antialias(g2);
            drawBackground(g2, phase);
            fill(g2, C(0, 0, 0, 90), 0, 0, W, H);
            drawTrophy(g2, W / 2);
            shadowTxt(g2, "HIGH SCORES", 150, 44, C(0, 0, 0, 120), RANK[0], 4);
            for (int i = 0; i < 40; i++)   // rainbow underline
                fill(g2, hsb(((phase * 2 + i * 9) % 360) / 360f, 0.8f, 1f), 150 + i * 10, 162, 10, 4);
            for (int i = 0; i < 5; i++) drawRow(g2, i);
        }
    }

    // High score file
    static class Scores {
        static final String FILE = "highscore.txt";

        static class Entry {
            String name;
            int score;
            Entry(String name, int score) { this.name = name; this.score = score; }
        }

        static ArrayList<Entry> load() {
            ArrayList<Entry> list = new ArrayList<>();
            try {
                for (String line : Files.readAllLines(Paths.get(FILE))) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int idx = line.lastIndexOf(',');
                    try {
                        list.add(idx < 0
                                ? new Entry("Player", Integer.parseInt(line))
                                : new Entry(line.substring(0, idx).trim(), Integer.parseInt(line.substring(idx + 1).trim())));
                    } catch (NumberFormatException ex) { }
                }
            } catch (IOException ex) { }
            list.sort((a, b) -> b.score - a.score);
            return list;
        }

        static boolean qualifies(int score) {
            if (score <= 0) return false;
            ArrayList<Entry> l = load();
            return l.size() < 5 || score > l.get(l.size() - 1).score;
        }

        static void add(String name, int score) {
            ArrayList<Entry> list = load();
            list.add(new Entry(name, score));
            list.sort((a, b) -> b.score - a.score);
            while (list.size() > 5) list.remove(list.size() - 1);
            try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
                for (Entry e : list) pw.println(e.name + "," + e.score);
            } catch (IOException ex) { }
        }

        static int best() { ArrayList<Entry> l = load(); return l.isEmpty() ? 0 : l.get(0).score; }

        static void reset() { try { new FileWriter(FILE, false).close(); } catch (IOException ex) { } }
    }

    // Main game logic
    static class GamePanel extends JPanel {

        static final int CELL = 25, COLS = 28, ROWS = 24, BOARD_TOP = 50;
        static final String[] DIFF_NAMES = {"EASY", "NORMAL", "HARD"};
        static final int[] START_DELAY = {170, 140, 110};
        static final int[] SPEED_STEP = {3, 5, 6};
        static final int[] MIN_DELAY = {80, 60, 45};
        static final int[] WALLS = {2, 3, 4};
        static final int POISON_SCORE_PENALTY = 20, POISON_SPEED_LOSS = 5;

        SnakeGame frame;
        ArrayList<Point> snake = new ArrayList<>(), obstacles = new ArrayList<>(), inputQueue = new ArrayList<>();
        Point food = new Point(10, 10), special = new Point(0, 0);
        boolean hasSpecial = false, gameOver = false, paused = false, countdownActive = false;
        int specialType = 0, specialTicks = 0, specialMax = 1;
        int dx = 1, dy = 0, nextDx = 1, nextDy = 0;
        int countdown = 0, score = 0, best = 0, delay = 140, phase = 0, diff = 1, level = 1, levelMsg = 0;
        int foodsEaten = 0, goldenEaten = 0, poisonEaten = 0;
        long elapsedMs = 0;
        String lastName = "Player";
        Random random = new Random();

        Timer countdownTimer;
        Timer gameTimer = new Timer(140, e -> tick());
        Timer animTimer = new Timer(30, e -> { phase++; if (levelMsg > 0) levelMsg--; repaint(); });
        JButton againBtn, menuBtn;

        GamePanel(SnakeGame f) {
            frame = f;
            setPreferredSize(new Dimension(W, H));
            setFocusable(true);
            setLayout(null);

            countdownTimer = new Timer(1000, e -> {
                countdown--;
                repaint();
                if (countdown <= 0) {
                    countdownActive = false;
                    countdownTimer.stop();
                    gameTimer.start();
                    requestFocusInWindow();
                }
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_LEFT: case KeyEvent.VK_A: queueDirection(-1, 0); break;
                        case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: queueDirection(1, 0); break;
                        case KeyEvent.VK_UP: case KeyEvent.VK_W: queueDirection(0, -1); break;
                        case KeyEvent.VK_DOWN: case KeyEvent.VK_S: queueDirection(0, 1); break;
                        case KeyEvent.VK_P: if (!gameOver && !countdownActive) paused = !paused; break;
                        case KeyEvent.VK_M: Sound.toggleMusic(); break;
                        case KeyEvent.VK_ESCAPE: frame.showMenu(); break;
                        case KeyEvent.VK_SPACE: case KeyEvent.VK_ENTER: if (gameOver) startGame(); break;
                    }
                }
            });

            againBtn = place(makeButton("PLAY AGAIN", 430, this::startGame), W / 2 - 100, 430, 200, 50);
            menuBtn = place(makeButton("MAIN MENU", 495, frame::showMenu, C(140, 90, 255)), W / 2 - 100, 495, 200, 50);
            againBtn.setVisible(false);
            menuBtn.setVisible(false);
            add(againBtn);
            add(menuBtn);
            animTimer.start();
        }

        void startGame() {
            gameTimer.stop();
            countdownTimer.stop();
            snake.clear();
            obstacles.clear();
            inputQueue.clear();
            snake.add(new Point(5, 5));
            snake.add(new Point(4, 5));
            snake.add(new Point(3, 5));
            dx = nextDx = 1;
            dy = nextDy = 0;
            score = 0;
            level = 1;
            levelMsg = 0;
            foodsEaten = goldenEaten = poisonEaten = 0;
            elapsedMs = 0;
            hasSpecial = false;
            delay = START_DELAY[diff];
            gameTimer.setDelay(delay);
            best = Scores.best();
            gameOver = paused = false;
            countdown = 3;
            countdownActive = true;
            againBtn.setVisible(false);
            menuBtn.setVisible(false);
            placeFood();
            countdownTimer.start();
            repaint();
            requestFocusInWindow();
        }

        void stopGame() {
            gameTimer.stop();
            countdownTimer.stop();
            countdownActive = false;
            inputQueue.clear();
            againBtn.setVisible(false);
            menuBtn.setVisible(false);
        }

        void queueDirection(int newDx, int newDy) {
            if (gameOver || countdownActive) return;
            Point last = inputQueue.isEmpty() ? new Point(dx, dy) : inputQueue.get(inputQueue.size() - 1);
            if (newDx == -last.x && newDy == -last.y) return;   // no reversing
            if (newDx == last.x && newDy == last.y) return;     // no duplicates
            if (inputQueue.size() < 2) inputQueue.add(new Point(newDx, newDy));
        }

        Point randomCell() { return new Point(random.nextInt(COLS), random.nextInt(ROWS)); }

        boolean free(Point p) {
            return !snake.contains(p) && !p.equals(food) && !obstacles.contains(p) && !(hasSpecial && p.equals(special));
        }

        void placeFood() {
            Point p;
            do { p = randomCell(); } while (!free(p));
            food = p;
        }

        void spawnSpecial() {
            for (int tries = 0; tries < 50; tries++) {
                Point p = randomCell();
                if (free(p)) {
                    special = p;
                    specialType = random.nextInt(10) < 7 ? 0 : 1;   // 70% golden star, 30% poison
                    specialMax = Math.max(20, 6500 / delay);
                    specialTicks = specialMax;
                    hasSpecial = true;
                    return;
                }
            }
        }

        void addObstacles(int n) {
            int tries = 0;
            Point head = snake.get(0);
            while (n > 0 && tries++ < 500 && obstacles.size() < 25) {
                Point p = randomCell();
                if (Math.abs(p.x - head.x) + Math.abs(p.y - head.y) >= 5 && free(p)) {
                    obstacles.add(p);
                    n--;
                }
            }
        }

        void checkLevel() {
            int newLevel = score / 50 + 1;
            if (newLevel > level) {
                int gained = newLevel - level;
                level = newLevel;
                levelMsg = 45;
                Sound.play(Sound.levelUp);
                addObstacles(gained * WALLS[diff]);
            }
        }

        int speedNumber() { return (START_DELAY[diff] - delay) / SPEED_STEP[diff] + 1; }

        void tick() {
            if (paused || gameOver) return;
            elapsedMs += delay;
            move();
        }

        void setDelay(int d) { delay = d; gameTimer.setDelay(delay); }

        void move() {
            if (!inputQueue.isEmpty()) {
                Point q = inputQueue.remove(0);
                dx = q.x;
                dy = q.y;
            } else {
                dx = nextDx;
                dy = nextDy;
            }
            nextDx = dx;
            nextDy = dy;

            Point head = snake.get(0);
            Point nh = new Point(head.x + dx, head.y + dy);

            if (diff == 0) {   // easy: wrap around the edges
                nh.x = (nh.x + COLS) % COLS;
                nh.y = (nh.y + ROWS) % ROWS;
            } else if (nh.x < 0 || nh.x >= COLS || nh.y < 0 || nh.y >= ROWS) {
                crash();
                return;
            }

            boolean eatFood = nh.equals(food);
            boolean eatSpecial = hasSpecial && nh.equals(special);
            boolean willGrow = eatFood || (eatSpecial && specialType == 0);

            // the tail moves away this tick unless we grow
            int check = willGrow ? snake.size() : snake.size() - 1;
            for (int i = 0; i < check; i++)
                if (snake.get(i).equals(nh)) { crash(); return; }
            if (obstacles.contains(nh)) { crash(); return; }

            snake.add(0, nh);
            boolean grew = false;

            if (eatFood) {
                score += 10;
                foodsEaten++;
                Sound.play(Sound.eat);
                placeFood();
                setDelay(Math.max(MIN_DELAY[diff], delay - SPEED_STEP[diff]));
                grew = true;
                checkLevel();
            } else if (eatSpecial) {
                hasSpecial = false;
                if (specialType == 0) {
                    score += 50;
                    goldenEaten++;
                    Sound.play(Sound.bonus);
                    grew = true;
                    checkLevel();
                } else {
                    poisonEaten++;
                    Sound.play(Sound.poison);
                    score = Math.max(0, score - POISON_SCORE_PENALTY);
                    setDelay(Math.min(START_DELAY[diff], delay + POISON_SPEED_LOSS * SPEED_STEP[diff]));
                    for (int extra = 0; extra < 3 && snake.size() > 4; extra++)
                        snake.remove(snake.size() - 1);
                }
            }

            if (!grew) snake.remove(snake.size() - 1);

            if (!hasSpecial) {
                if (random.nextInt(20) == 0) spawnSpecial();
            } else if (--specialTicks <= 0) {
                hasSpecial = false;
            }
        }

        void crash() {
            gameOver = true;
            stopGame();
            againBtn.setVisible(true);
            menuBtn.setVisible(true);
            Sound.play(Sound.over);
            SwingUtilities.invokeLater(this::finishGame);
        }

        void finishGame() {
            boolean top = Scores.qualifies(score);
            String name = "-";
            if (top) {
                name = askName();
                Scores.add(name, score);
                best = Scores.best();
            }
            writeReport(top, name);
            requestFocusInWindow();
        }

        String askName() {
            Object r = JOptionPane.showInputDialog(this, "New high score: " + score + "!\nEnter your name:",
                    NAME1 + " " + NAME2, JOptionPane.PLAIN_MESSAGE, null, null, lastName);
            String n = (r == null ? lastName : r.toString()).replace(",", "").replace("\n", "").trim();
            if (n.length() > 10) n = n.substring(0, 10);
            if (n.isEmpty()) n = "Player";
            return lastName = n;
        }

        void writeReport(boolean top, String name) {
            long sec = elapsedMs / 1000;
            try (PrintWriter pw = new PrintWriter(new FileWriter("report.txt"))) {
                pw.println("=== " + NAME1 + " " + NAME2 + " - GAME REPORT ===");
                pw.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                pw.println("Difficulty: " + DIFF_NAMES[diff]);
                pw.println("Final score: " + score);
                pw.println("Level reached: " + level);
                pw.println("Time played: " + (sec / 60) + "m " + (sec % 60) + "s");
                pw.println("Snake length: " + snake.size());
                pw.println("Normal food eaten: " + foodsEaten);
                pw.println("Golden stars eaten: " + goldenEaten);
                pw.println("Poison eaten: " + poisonEaten);
                pw.println("Walls on the board: " + obstacles.size());
                pw.println("Made the top 5: " + (top ? "Yes (" + name + ")" : "No"));
            } catch (IOException ex) { }
        }

        Polygon makeStar(int cx, int cy, int outer, int inner, double rot) {
            Polygon p = new Polygon();
            for (int i = 0; i < 10; i++) {
                double ang = rot - Math.PI / 2 + i * Math.PI / 5;
                int rr = (i % 2 == 0) ? outer : inner;
                p.addPoint(cx + (int) (Math.cos(ang) * rr), cy + (int) (Math.sin(ang) * rr));
            }
            return p;
        }

        // drawing
        int px(Point p) { return p.x * CELL + CELL / 2; }
        int py(Point p) { return BOARD_TOP + p.y * CELL + CELL / 2; }
        void overlay(Graphics2D g2, int a) { fill(g2, C(0, 0, 0, a), 0, BOARD_TOP, W, H - BOARD_TOP); }

        void drawGrid(Graphics2D g2) {
            g2.setColor(C(255, 255, 255, 18));
            for (int i = 0; i <= COLS; i++) g2.drawLine(i * CELL, BOARD_TOP, i * CELL, H);
            for (int i = 0; i <= ROWS; i++) g2.drawLine(0, BOARD_TOP + i * CELL, W, BOARD_TOP + i * CELL);
        }

        void drawObstacles(Graphics2D g2) {
            for (Point o : obstacles) {
                int x = o.x * CELL, y = BOARD_TOP + o.y * CELL;
                g2.setColor(C(70, 55, 110));
                g2.fillRoundRect(x + 1, y + 1, CELL - 2, CELL - 2, 8, 8);
                g2.setColor(C(170, 140, 230));
                stroke(g2, 2);
                g2.drawRoundRect(x + 3, y + 3, CELL - 6, CELL - 6, 6, 6);
                stroke(g2, 1);
            }
        }

        void drawFood(Graphics2D g2) {
            int cx = px(food), cy = py(food);
            int r = 9 + (int) (2 * Math.sin(phase / 3.0));
            circle(g2, cx, cy, r + 6, C(255, 80, 80, 60));
            circle(g2, cx, cy, r + 3, C(255, 80, 80, 110));
            circle(g2, cx, cy, r, C(255, 60, 60));
            g2.setColor(C(255, 255, 255, 170));
            g2.fillOval(cx - r / 2, cy - r / 2 - 2, r / 2, r / 2);
        }

        void drawSpecial(Graphics2D g2) {
            int sx = px(special), sy = py(special);
            double frac = (double) specialTicks / specialMax;
            int pulse = (int) (2 * Math.sin(phase / 3.0));
            if (specialType == 0) {
                circle(g2, sx, sy, 17, C(255, 215, 0, 60));
                Polygon star = makeStar(sx, sy, 11 + pulse, 5, phase / 25.0);
                g2.setColor(C(255, 215, 0));
                g2.fillPolygon(star);
                g2.setColor(Color.WHITE);
                g2.drawPolygon(star);
            } else {
                circle(g2, sx, sy, 17, C(170, 60, 220, 70));
                circle(g2, sx, sy, 9 + pulse, C(170, 60, 220));
                g2.setColor(Color.WHITE);
                stroke(g2, 2);
                g2.drawLine(sx - 4, sy - 4, sx + 4, sy + 4);
                g2.drawLine(sx - 4, sy + 4, sx + 4, sy - 4);
            }
            stroke(g2, 3);   // countdown ring
            g2.setColor(C(255, 255, 255, 210));
            g2.drawArc(sx - 15, sy - 15, 30, 30, 90, (int) (360 * frac));
            stroke(g2, 1);
        }

        void drawSnake(Graphics2D g2) {
            int n = snake.size();
            for (int i = n - 1; i >= 0; i--) {
                Point p = snake.get(i);
                int x = px(p), y = py(p);
                Color c = hsb((phase / 2 + i * 8) % 360 / 360f, 0.8f, 1f);
                int size = i == 0 ? 24 : i == n - 1 ? 13 : 20;

                if (i < n - 1) {   // connect to the next segment
                    Point next = snake.get(i + 1);
                    boolean wrapped = diff == 0 && (Math.abs(p.x - next.x) > 1 || Math.abs(p.y - next.y) > 1);
                    if (!wrapped) {
                        g2.setColor(c);
                        g2.setStroke(new BasicStroke(Math.min(size, 18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        g2.drawLine(x, y, px(next), py(next));
                        stroke(g2, 1);
                    }
                }
                circle(g2, x, y, (size + 6) / 2, alpha(c, 60));
                g2.setColor(c);
                g2.fillOval(x - size / 2, y - size / 2, size, size);
                if (i == 0) drawEyes(g2, x, y);
            }
        }

        void drawEyes(Graphics2D g2, int x, int y) {
            int ex1, ey1, ex2, ey2;
            if (dx == 1)       { ex1 = x + 2; ey1 = y - 8; ex2 = x + 2; ey2 = y + 1; }
            else if (dx == -1) { ex1 = x - 9; ey1 = y - 8; ex2 = x - 9; ey2 = y + 1; }
            else if (dy == -1) { ex1 = x - 8; ey1 = y - 9; ex2 = x + 1; ey2 = y - 9; }
            else               { ex1 = x - 8; ey1 = y + 2; ex2 = x + 1; ey2 = y + 2; }
            g2.setColor(Color.WHITE);
            g2.fillOval(ex1, ey1, 7, 7);
            g2.fillOval(ex2, ey2, 7, 7);
            g2.setColor(Color.BLACK);
            g2.fillOval(ex1 + 2, ey1 + 2, 3, 3);
            g2.fillOval(ex2 + 2, ey2 + 2, 3, 3);
        }

        void drawHud(Graphics2D g2) {
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(Color.WHITE);
            g2.drawString("Score: " + score, 10, 22);
            drawCentered(g2, "Level " + level + "    Speed " + speedNumber(), 22);
            String b = "Best: " + best;
            g2.drawString(b, W - 10 - g2.getFontMetrics().stringWidth(b), 22);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.setColor(C(255, 255, 255, 130));
            g2.drawString(DIFF_NAMES[diff] + (diff == 0 ? " • WRAP-AROUND" : ""), 10, 42);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            antialias(g2);
            drawBackground(g2, phase);
            overlay(g2, 110);
            drawGrid(g2);
            drawObstacles(g2);
            drawFood(g2);
            if (hasSpecial) drawSpecial(g2);
            drawSnake(g2);
            drawHud(g2);

            if (levelMsg > 0) {
                int a = Math.min(255, levelMsg * 12);
                shadowTxt(g2, "LEVEL UP!", 108, 50, C(0, 0, 0, a / 2), C(255, 215, 0, a), 4);
                txt(g2, "Faster + new walls!", 138, 18, Font.BOLD, C(255, 255, 255, a));
            }
            if (countdownActive) {
                overlay(g2, 90);
                txt(g2, "" + countdown, H / 2, 92, Font.BOLD, Color.WHITE);
                txt(g2, "GET READY!", H / 2 + 42, 22, Font.BOLD, Color.WHITE);
            }
            if (paused) {
                overlay(g2, 140);
                txt(g2, "PAUSED", H / 2, 48, Font.BOLD, Color.WHITE);
            }
            if (gameOver) {
                overlay(g2, 160);
                txt(g2, "GAME OVER", H / 2 - 70, 56, Font.BOLD, C(255, 80, 80));
                txt(g2, "Score: " + score + "     Level: " + level, H / 2 - 25, 26, Font.BOLD, Color.WHITE);
                txt(g2, "(or press SPACE to restart, ESC for menu)", H / 2 + 10, 16, Font.PLAIN, C(255, 255, 255, 190));
            }
        }
    }

    // Game sound system
    static class Sound {
        static final float RATE = 22050f;
        static Clip music, eat, over, click, levelUp, bonus, poison;

        static void init() {
            try {
                click = makeClip(tone(620, 45, 0.16));
                eat = makeClip(join(tone(620, 65, 0.18), tone(820, 90, 0.16)));
                over = makeClip(join(tone(360, 130, 0.16), tone(300, 130, 0.14), tone(240, 160, 0.12), tone(180, 260, 0.10)));
                levelUp = makeClip(join(tone(523, 80, 0.15), tone(659, 80, 0.15), tone(784, 90, 0.15), tone(1047, 180, 0.13)));
                bonus = makeClip(join(tone(880, 55, 0.13), tone(1100, 55, 0.13), tone(1320, 110, 0.12)));
                poison = makeClip(join(tone(260, 90, 0.12), tone(210, 100, 0.11), tone(160, 160, 0.10)));
                music = makeClip(melody());
            } catch (Exception e) {
                System.out.println("Sound not available: " + e.getMessage());
            }
        }
        static byte[] tone(double freq, int ms, double vol) {
            int n = (int) (RATE * ms / 1000);
            byte[] b = new byte[n];
            if (freq <= 0) return b;
            for (int i = 0; i < n; i++) {
                double wave = Math.sin(2 * Math.PI * freq * (i / RATE));
                double env = Math.min(Math.min(1.0, i / (RATE * 0.015)), Math.min(1.0, (n - i) / (RATE * 0.025)));
                b[i] = (byte) (wave * vol * env * 127);
            }
            return b;
        }

        static byte[] join(byte[]... parts) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (byte[] p : parts) out.write(p, 0, p.length);
            return out.toByteArray();
        }

        static byte[] melody() {
            double[] notes = {
                392, 330, 392, 330, 440, 392, 330, 262,
                294, 349, 294, 349, 494, 440, 349, 294,
                262, 330, 392, 523, 392, 330, 392, 0,
                523, 0, 523, 392, 330, 262, 196, 0};
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (double f : notes) {
                byte[] t = tone(f, 190, 0.075);
                out.write(t, 0, t.length);
            }
            return out.toByteArray();
        }

        static Clip makeClip(byte[] data) throws Exception {
            AudioFormat fmt = new AudioFormat(RATE, 8, 1, true, false);
            Clip c = AudioSystem.getClip();
            c.open(new AudioInputStream(new ByteArrayInputStream(data), fmt, data.length));
            return c;
        }

        static void play(Clip c) {
            if (c == null) return;
            c.stop();
            c.setFramePosition(0);
            c.start();
        }

        static void startMusic() { if (music != null) music.loop(Clip.LOOP_CONTINUOUSLY); }

        static void toggleMusic() {
            if (music == null) return;
            if (music.isRunning()) music.stop();
            else music.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SnakeGame().setVisible(true));
    }
}