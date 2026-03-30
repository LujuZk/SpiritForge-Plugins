package dev.skilltree.models;

/**
 * Modo del árbol de habilidades.
 * POINTS = comportamiento actual (gastar puntos ganados por nivel).
 * LEVEL  = el costo del nodo es el nivel requerido; no se gastan puntos.
 */
public enum TreeMode {
    POINTS,
    LEVEL;

    public static TreeMode fromString(String s) {
        if (s != null && s.equalsIgnoreCase("LEVEL")) return LEVEL;
        return POINTS;
    }
}
