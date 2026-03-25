package dev.skilltree.models;

/**
 * Estados posibles de un nodo en el árbol de habilidades.
 */
public enum NodeState {
    /**
     * Nodo ya desbloqueado por el jugador.
     */
    UNLOCKED,

    /**
     * Prerequisitos cumplidos y el jugador puede desbloquearlo (si tiene puntos).
     */
    AVAILABLE,

    /**
     * Prerequisitos no cumplidos aún.
     */
    LOCKED,

    /**
     * Un nodo mutuamente exclusivo ya fue elegido, bloqueando este nodo.
     */
    EXCLUSIVE_BLOCKED
}
