package com.fishinspace;

import javax.swing.*;

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
public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Asteroid Destroyer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new AsteroidDestroyer());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

