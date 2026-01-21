package com.fishinspace;

import java.util.Vector;

/**
 * AsteroidDestroyer.java
 *
 * A complete, Asteroid Destroyer game created with Java Swing.
 *
 * --- CONTROLS ---
 * Arrow Up:    Thrust
 * Arrow Down:  Brake (with BOOSTER powerup)
 * Arrow Left:  Rotate Left
 * Arrow Right: Rotate Right
 * Spacebar:    Fire Bullet
 * R Key:       Restart Game (after Game Over)
 *
 * Compile: javac AsteroidDestroyer.java
 * Run:     java AsteroidDestroyer
 */
public class AsteroidDestroyer extends javax.swing.JPanel implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    // --- Game Constants ---
    private static final int PANEL_WIDTH = 800;
    private static final int PANEL_HEIGHT = 600;
    private static final int SHIP_SIZE = 20;
    private static final double SHIP_TURN_SPEED = 0.05; // radians
    private static final double SHIP_THRUST_POWER = 0.1;
    private static final double SHIP_DRAG = 0.98; // friction
    private static final int BULLET_SPEED = 7;
    private static final int BULLET_COOLDOWN = 15; // frames
    private static final int ASTEROID_INIT_COUNT = 5;
    private static final int ASTEROID_MAX_SPEED = 2;
    private static final int ASTEROID_SIZE_LARGE = 60;
    private static final int ASTEROID_SIZE_MEDIUM = 30;
    private static final int ASTEROID_SIZE_SMALL = 15;
    private static final int SCORE_LARGE_ASTEROID = 20;
    private static final int SCORE_MEDIUM_ASTEROID = 50;
    private static final int SCORE_SMALL_ASTEROID = 100;

    // --- PowerUp Constants ---
    private static final int POWERUP_SIZE = 20;
    private static final int POWERUP_DURATION = 1200; // 20s @ 60fps
    private static final int POWERUP_DROP_MIN = 5;
    private static final int POWERUP_DROP_MAX = 10;
    
    // PowerUp types as int constants
    private static final int POWERUP_NONE = 0;
    private static final int POWERUP_AIM_BEAM = 1;
    private static final int POWERUP_DOUBLE_SHOT = 2;
    private static final int POWERUP_BOOSTER = 3;
    private static final int POWERUP_RAPID_FIRE = 4;

    // --- Game State ---
    private javax.swing.Timer gameTimer;
    private java.util.Random random;
    private boolean inGame;
    private int score;
    // Added start & pause state
    private boolean started; // false until user presses ENTER
    private boolean paused;  // toggled by P key

    private Vector powerUps;
    private int asteroidsDestroyedSinceLastPowerUp;
    private int asteroidsUntilNextPowerUp;
    private int activePowerUp;
    private int powerUpTimeRemaining;

    // --- Ship ---
    private double shipX, shipY;
    private double shipVelX, shipVelY;
    private double shipAngle;

    // --- Input Flags ---
    private boolean rotatingLeft;
    private boolean rotatingRight;
    private boolean thrusting;
    private boolean braking;

    // --- Objects ---
    private Vector bullets;
    private Vector asteroids;
    private int bulletCooldownTimer;

    public AsteroidDestroyer() {
        setPreferredSize(new java.awt.Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(java.awt.Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        random = new java.util.Random();
        bullets = new Vector();
        asteroids = new Vector();
        powerUps = new Vector();

        initGame();
        gameTimer = new javax.swing.Timer(16, this); // ~60 FPS
        gameTimer.start();
    }

    private void initGame() {
        shipX = PANEL_WIDTH / 2.0;
        shipY = PANEL_HEIGHT / 2.0;
        shipVelX = 0;
        shipVelY = 0;
        shipAngle = -Math.PI / 2; // up

        rotatingLeft = false;
        rotatingRight = false;
        thrusting = false;
        braking = false;

        bullets.clear();
        asteroids.clear();
        powerUps.clear();

        for (int i = 0; i < ASTEROID_INIT_COUNT; i++) {
            spawnAsteroid(ASTEROID_SIZE_LARGE);
        }

        score = 0;
        inGame = true;
        started = false; // show start screen initially
        paused = false;
        bulletCooldownTimer = 0;

        asteroidsDestroyedSinceLastPowerUp = 0;
        asteroidsUntilNextPowerUp = POWERUP_DROP_MIN + random.nextInt(POWERUP_DROP_MAX - POWERUP_DROP_MIN + 1);
        activePowerUp = POWERUP_NONE;
        powerUpTimeRemaining = 0;

        if (gameTimer != null && !gameTimer.isRunning()) {
            gameTimer.start();
        }
    }

    private void spawnAsteroid(int size) {
        double x, y;
        double angle = random.nextDouble() * 2 * Math.PI;
        double speed = (random.nextDouble() * (ASTEROID_MAX_SPEED - 1)) + 1;

        int edge = random.nextInt(4);
        if (edge == 0) { // top
            x = random.nextDouble() * PANEL_WIDTH;
            y = -size / 2.0;
        } else if (edge == 1) { // right
            x = PANEL_WIDTH + size / 2.0;
            y = random.nextDouble() * PANEL_HEIGHT;
        } else if (edge == 2) { // bottom
            x = random.nextDouble() * PANEL_WIDTH;
            y = PANEL_HEIGHT + size / 2.0;
        } else { // left
            x = -size / 2.0;
            y = random.nextDouble() * PANEL_HEIGHT;
        }

        double dx = Math.cos(angle) * speed;
        double dy = Math.sin(angle) * speed;
        asteroids.add(new Asteroid(x, y, dx, dy, size));
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        // Only update when game started and not paused
        if (inGame && started && !paused) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        updateShip();
        updateBullets();
        updateAsteroids();
        updatePowerUps();
        checkCollisions();

        if (asteroids.isEmpty()) {
            spawnAsteroid(ASTEROID_SIZE_LARGE);
            spawnAsteroid(ASTEROID_SIZE_LARGE);
        }

        if (bulletCooldownTimer > 0) bulletCooldownTimer--;

        if (powerUpTimeRemaining > 0) {
            powerUpTimeRemaining--;
            if (powerUpTimeRemaining == 0) {
                activePowerUp = POWERUP_NONE;
            }
        }
    }

    private void updateShip() {
        if (rotatingLeft) shipAngle -= SHIP_TURN_SPEED;
        if (rotatingRight) shipAngle += SHIP_TURN_SPEED;

        double thrustPower = SHIP_THRUST_POWER;
        if (activePowerUp == POWERUP_BOOSTER) {
            thrustPower *= 1.5;
        }
        if (thrusting) {
            shipVelX += Math.cos(shipAngle) * thrustPower;
            shipVelY += Math.sin(shipAngle) * thrustPower;
        }
        if (braking && activePowerUp == POWERUP_BOOSTER) {
            shipVelX *= 0.9;
            shipVelY *= 0.9;
        }
        shipVelX *= SHIP_DRAG;
        shipVelY *= SHIP_DRAG;

        shipX += shipVelX;
        shipY += shipVelY;

        // Wrap coordinates
        if (shipX < 0) shipX = PANEL_WIDTH; 
        else if (shipX > PANEL_WIDTH) shipX = 0;
        if (shipY < 0) shipY = PANEL_HEIGHT; 
        else if (shipY > PANEL_HEIGHT) shipY = 0;
    }

    private void updateBullets() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = (Bullet) bullets.get(i);
            b.move();
            if (b.x < 0 || b.x > PANEL_WIDTH || b.y < 0 || b.y > PANEL_HEIGHT) {
                bullets.remove(i);
            }
        }
    }

    private void updateAsteroids() {
        for (int i = 0; i < asteroids.size(); i++) {
            Asteroid a = (Asteroid) asteroids.get(i);
            a.move();
            // Wrap coordinates
            if (a.x < 0) a.x = PANEL_WIDTH; 
            else if (a.x > PANEL_WIDTH) a.x = 0;
            if (a.y < 0) a.y = PANEL_HEIGHT; 
            else if (a.y > PANEL_HEIGHT) a.y = 0;
        }
    }

    private void updatePowerUps() {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = (PowerUp) powerUps.get(i);
            p.update();
            double dist = Math.sqrt(Math.pow(shipX - p.x, 2) + Math.pow(shipY - p.y, 2));
            if (dist < (SHIP_SIZE + POWERUP_SIZE) / 2.0) {
                activePowerUp = p.type;
                powerUpTimeRemaining = POWERUP_DURATION;
                powerUps.remove(i);
            }
        }
    }

    private void fireBullet() {
        int cooldown = BULLET_COOLDOWN;
        if (activePowerUp == POWERUP_RAPID_FIRE) {
            cooldown = (int) (BULLET_COOLDOWN / 1.5);
        }
        if (bulletCooldownTimer <= 0) {
            double dx = Math.cos(shipAngle) * BULLET_SPEED;
            double dy = Math.sin(shipAngle) * BULLET_SPEED;
            if (activePowerUp == POWERUP_DOUBLE_SHOT) {
                double offsetAngle = Math.PI / 16;
                double leftAngle = shipAngle - offsetAngle;
                double rightAngle = shipAngle + offsetAngle;
                bullets.add(new Bullet(shipX, shipY, Math.cos(leftAngle) * BULLET_SPEED, Math.sin(leftAngle) * BULLET_SPEED));
                bullets.add(new Bullet(shipX, shipY, Math.cos(rightAngle) * BULLET_SPEED, Math.sin(rightAngle) * BULLET_SPEED));
            } else {
                bullets.add(new Bullet(shipX, shipY, dx, dy));
            }
            bulletCooldownTimer = cooldown;
        }
    }

    private void checkCollisions() {
        for (int i = bullets.size() - 1; i >= 0; i--) {
            Bullet b = (Bullet) bullets.get(i);
            for (int j = asteroids.size() - 1; j >= 0; j--) {
                Asteroid a = (Asteroid) asteroids.get(j);
                double dist = Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
                if (dist < a.size / 2.0) {
                    bullets.remove(i);
                    splitAsteroid(j);
                    break;
                }
            }
        }
        for (int i = asteroids.size() - 1; i >= 0; i--) {
            Asteroid a = (Asteroid) asteroids.get(i);
            double dist = Math.sqrt(Math.pow(shipX - a.x, 2) + Math.pow(shipY - a.y, 2));
            if (dist < (a.size / 2.0) + (SHIP_SIZE / 2.0)) {
                inGame = false;
                gameTimer.stop();
                break;
            }
        }
    }

    private void splitAsteroid(int asteroidIndex) {
        Asteroid a = (Asteroid) asteroids.remove(asteroidIndex);
        if (a.size == ASTEROID_SIZE_LARGE) {
            score += SCORE_LARGE_ASTEROID;
            asteroids.add(new Asteroid(a.x, a.y, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, ASTEROID_SIZE_MEDIUM));
            asteroids.add(new Asteroid(a.x, a.y, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, ASTEROID_SIZE_MEDIUM));
        } else if (a.size == ASTEROID_SIZE_MEDIUM) {
            score += SCORE_MEDIUM_ASTEROID;
            asteroids.add(new Asteroid(a.x, a.y, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, ASTEROID_SIZE_SMALL));
            asteroids.add(new Asteroid(a.x, a.y, random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, ASTEROID_SIZE_SMALL));
        } else {
            score += SCORE_SMALL_ASTEROID;
        }
        asteroidsDestroyedSinceLastPowerUp++;
        if (asteroidsDestroyedSinceLastPowerUp >= asteroidsUntilNextPowerUp) {
            int[] types = {POWERUP_AIM_BEAM, POWERUP_DOUBLE_SHOT, POWERUP_BOOSTER, POWERUP_RAPID_FIRE};
            int randomType = types[random.nextInt(types.length)];
            powerUps.add(new PowerUp(a.x, a.y, randomType));
            asteroidsDestroyedSinceLastPowerUp = 0;
            asteroidsUntilNextPowerUp = POWERUP_DROP_MIN + random.nextInt(POWERUP_DROP_MAX - POWERUP_DROP_MIN + 1);
        }
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        java.awt.Graphics2D g2d = (java.awt.Graphics2D) g;
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Start screen
        if (!started) {
            drawStartScreen(g2d);
            return;
        }

        if (inGame) {
            drawAimBeam(g2d);
            drawShip(g2d);
            drawBullets(g2d);
            drawAsteroids(g2d);
            drawPowerUps(g2d);
            drawScore(g2d);
            drawActivePowerUp(g2d);
            if (paused) {
                drawPausedOverlay(g2d);
            }
        } else {
            drawGameOver(g2d);
        }
    }

    // Start screen with instructions
    private void drawStartScreen(java.awt.Graphics2D g2d) {
        String title = "Space Invaders";
        String msg = "Press ENTER to Start";
        String controls = "Arrows: Move  |  SPACE: Shoot  |  P: Pause  |  R: Restart";

        g2d.setColor(java.awt.Color.WHITE);
        java.awt.Font largeFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 75);
        java.awt.Font mediumFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 30);
        java.awt.Font smallFont = new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 16);
        java.awt.FontMetrics metricsLarge = g2d.getFontMetrics(largeFont);
        java.awt.FontMetrics metricsMedium = g2d.getFontMetrics(mediumFont);
        g2d.setFont(largeFont);
        g2d.drawString(title, (PANEL_WIDTH - metricsLarge.stringWidth(title)) / 2, PANEL_HEIGHT / 2 - 40);
        g2d.setFont(mediumFont);
        g2d.drawString(msg, (PANEL_WIDTH - metricsMedium.stringWidth(msg)) / 2, PANEL_HEIGHT / 2 + 10);
        g2d.setFont(smallFont);
        java.awt.FontMetrics metricsSmall = g2d.getFontMetrics(smallFont);
        g2d.drawString(controls, (PANEL_WIDTH - metricsSmall.stringWidth(controls)) / 2, PANEL_HEIGHT / 2 + 50);
    }

    // Pause overlay
    private void drawPausedOverlay(java.awt.Graphics2D g2d) {
        String msg = "PAUSED";
        java.awt.Font font = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 40);
        java.awt.FontMetrics m = g2d.getFontMetrics(font);
        g2d.setColor(new java.awt.Color(0,0,0,150));
        g2d.fillRect(0,0,PANEL_WIDTH,PANEL_HEIGHT);
        g2d.setColor(java.awt.Color.YELLOW);
        g2d.setFont(font);
        g2d.drawString(msg, (PANEL_WIDTH - m.stringWidth(msg)) / 2, PANEL_HEIGHT / 2);
    }

    private void drawAimBeam(java.awt.Graphics2D g2d) {
        if (activePowerUp == POWERUP_AIM_BEAM) {
            g2d.setColor(java.awt.Color.GREEN);
            java.awt.Stroke dashed = new java.awt.BasicStroke(1, java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0);
            g2d.setStroke(dashed);
            double endX = shipX + Math.cos(shipAngle) * 1000;
            double endY = shipY + Math.sin(shipAngle) * 1000;
            g2d.drawLine((int) shipX, (int) shipY, (int) endX, (int) endY);
            g2d.setStroke(new java.awt.BasicStroke());
        }
    }

    private void drawShip(java.awt.Graphics2D g2d) {
        java.awt.Polygon shipShape = new java.awt.Polygon();
        shipShape.addPoint(SHIP_SIZE / 2, 0);
        shipShape.addPoint(-SHIP_SIZE / 2, -SHIP_SIZE / 3);
        shipShape.addPoint(-SHIP_SIZE / 2, SHIP_SIZE / 3);
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(shipX, shipY);
        g2d.rotate(shipAngle);
        if (thrusting) {
            g2d.setColor(java.awt.Color.ORANGE);
            g2d.fillPolygon(new int[]{-SHIP_SIZE / 2, -SHIP_SIZE, -SHIP_SIZE / 2}, new int[]{-SHIP_SIZE / 4, 0, SHIP_SIZE / 4}, 3);
        }
        g2d.setColor(java.awt.Color.CYAN);
        g2d.draw(shipShape);
        g2d.setTransform(oldTransform);
    }

    private void drawBullets(java.awt.Graphics2D g2d) {
        g2d.setColor(java.awt.Color.YELLOW);
        for (int i = 0; i < bullets.size(); i++) {
            Bullet b = (Bullet) bullets.get(i);
            g2d.fillOval((int) b.x - 2, (int) b.y - 2, 4, 4);
        }
    }

    private void drawAsteroids(java.awt.Graphics2D g2d) {
        g2d.setColor(java.awt.Color.GRAY);
        for (int i = 0; i < asteroids.size(); i++) {
            Asteroid a = (Asteroid) asteroids.get(i);
            g2d.drawOval((int) (a.x - a.size / 2.0), (int) (a.y - a.size / 2.0), a.size, a.size);
        }
    }

    private void drawPowerUps(java.awt.Graphics2D g2d) {
        for (int i = 0; i < powerUps.size(); i++) {
            PowerUp p = (PowerUp) powerUps.get(i);
            java.awt.Color color;
            String label;
            if (p.type == POWERUP_AIM_BEAM) {
                color = java.awt.Color.GREEN; 
                label = "A";
            } else if (p.type == POWERUP_DOUBLE_SHOT) {
                color = java.awt.Color.BLUE; 
                label = "D";
            } else if (p.type == POWERUP_BOOSTER) {
                color = java.awt.Color.ORANGE; 
                label = "B";
            } else if (p.type == POWERUP_RAPID_FIRE) {
                color = java.awt.Color.RED; 
                label = "R";
            } else {
                color = java.awt.Color.WHITE; 
                label = "?";
            }
            g2d.setColor(color);
            g2d.fillRect((int) (p.x - POWERUP_SIZE / 2), (int) (p.y - POWERUP_SIZE / 2), POWERUP_SIZE, POWERUP_SIZE);
            g2d.setColor(java.awt.Color.BLACK);
            g2d.drawString(label, (int) p.x - 4, (int) p.y + 5);
        }
    }

    private void drawScore(java.awt.Graphics2D g2d) {
        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        g2d.drawString("Score: " + score, 10, 25);
        // Controls hint (pause / restart) in top-right
        String ctrl = "P: Pause | R: Restart";
        java.awt.FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(ctrl);
        g2d.drawString(ctrl, PANEL_WIDTH - w - 10, 25);
    }

    private void drawActivePowerUp(java.awt.Graphics2D g2d) {
        if (activePowerUp != POWERUP_NONE && powerUpTimeRemaining > 0) {
            g2d.setColor(java.awt.Color.WHITE);
            g2d.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 16));
            String name;
            if (activePowerUp == POWERUP_AIM_BEAM) {
                name = "AIM BEAM";
            } else if (activePowerUp == POWERUP_DOUBLE_SHOT) {
                name = "DOUBLE SHOT";
            } else if (activePowerUp == POWERUP_BOOSTER) {
                name = "BOOSTER";
            } else if (activePowerUp == POWERUP_RAPID_FIRE) {
                name = "RAPID FIRE";
            } else {
                name = "";
            }
            int timeLeft = powerUpTimeRemaining / 60;
            g2d.drawString("PowerUp: " + name + " (" + timeLeft + "s)", 10, 50);
        }
    }

    private void drawGameOver(java.awt.Graphics2D g2d) {
        String msg = "Game Over";
        String scoreMsg = "Final Score: " + score;
        String restartMsg = "Press 'R' to Restart";
        String quitMsg = "Press 'Q' to Quit";
        java.awt.Font largeFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 75);
        java.awt.Font mediumFont = new java.awt.Font("Monospaced", java.awt.Font.BOLD, 30);
        java.awt.FontMetrics metricsLarge = g2d.getFontMetrics(largeFont);
        g2d.setColor(java.awt.Color.RED);
        g2d.setFont(largeFont);
        g2d.drawString(msg, (PANEL_WIDTH - metricsLarge.stringWidth(msg)) / 2, PANEL_HEIGHT / 2 - 50);
        g2d.setColor(java.awt.Color.WHITE);
        g2d.setFont(mediumFont);
        java.awt.FontMetrics metricsMedium = g2d.getFontMetrics(mediumFont);
        g2d.drawString(scoreMsg, (PANEL_WIDTH - metricsMedium.stringWidth(scoreMsg)) / 2, PANEL_HEIGHT / 2 + 20);
        g2d.drawString(restartMsg, (PANEL_WIDTH - metricsMedium.stringWidth(restartMsg)) / 2, PANEL_HEIGHT / 2 + 60);
        g2d.drawString(quitMsg, (PANEL_WIDTH - metricsMedium.stringWidth(quitMsg)) / 2, PANEL_HEIGHT / 2 + 100);
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        int key = e.getKeyCode();

        // Start screen handling
        if (!started) {
            if (key == java.awt.event.KeyEvent.VK_ENTER) {
                started = true;
            }
            return;
        }

        // Restart anytime
        if (key == java.awt.event.KeyEvent.VK_R) { initGame(); return; }

        // Quit on Q (after start)
        if (key == java.awt.event.KeyEvent.VK_Q) { System.exit(0); }

        // Pause toggle
        if (key == java.awt.event.KeyEvent.VK_P && inGame) { paused = !paused; return; }
        if (!inGame || paused) { return; }

        if (key == java.awt.event.KeyEvent.VK_LEFT)  rotatingLeft = true;
        if (key == java.awt.event.KeyEvent.VK_RIGHT) rotatingRight = true;
        if (key == java.awt.event.KeyEvent.VK_UP)    thrusting = true;
        if (key == java.awt.event.KeyEvent.VK_DOWN)  braking = true;
        if (key == java.awt.event.KeyEvent.VK_SPACE) fireBullet();
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        if (!inGame || !started || paused) return;
        int key = e.getKeyCode();
        if (key == java.awt.event.KeyEvent.VK_LEFT)  rotatingLeft = false;
        if (key == java.awt.event.KeyEvent.VK_RIGHT) rotatingRight = false;
        if (key == java.awt.event.KeyEvent.VK_UP)    thrusting = false;
        if (key == java.awt.event.KeyEvent.VK_DOWN)  braking = false;
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {}
}

class Asteroid {
    public double x, y, dx, dy;
    public int size;

    public Asteroid(double x, double y, double dx, double dy, int size) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.size = size;
    }

    public void move() {
        x += dx;
        y += dy;
    }
}

class Bullet {
    public double x, y, dx, dy;

    public Bullet(double x, double y, double dx, double dy) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
    }

    public void move() {
        x += dx;
        y += dy;
    }
}

class PowerUp {
    public double x, y;
    public int type;

    public PowerUp(double x, double y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
    }
}

