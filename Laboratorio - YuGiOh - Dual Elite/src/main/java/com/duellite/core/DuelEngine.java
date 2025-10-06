package com.duellite.core;

import com.duellite.domain.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DuelEngine {
    private final List<BattleListener> listeners = new ArrayList<>();
    private final Random rng = new Random();
    private int playerScore = 0;
    private int aiScore = 0;

    public void addListener(BattleListener l){ listeners.add(l); }
    public void removeListener(BattleListener l){ listeners.remove(l); }

    public boolean randomFirstTurnIsPlayer(){ return rng.nextBoolean(); }

    // Reglas del enunciado: ATK vs ATK, o ATK (atacante) vs DEF (defensor)
    public void resolveTurn(Card playerCard, Card aiCard, boolean playerInAttack, boolean aiInAttack){
        String winner;
        if (playerInAttack && aiInAttack) {
            winner = playerCard.getAtk() >= aiCard.getAtk() ? "Jugador" : "CPU";
        } else if (playerInAttack && !aiInAttack) {
            winner = playerCard.getAtk() >= aiCard.getDef() ? "Jugador" : "CPU";
        } else if (!playerInAttack && aiInAttack) {
            winner = aiCard.getAtk() > playerCard.getDef() ? "CPU" : "Jugador";
        } else { // ambos defensa: comparar DEF
            winner = playerCard.getDef() >= aiCard.getDef() ? "Jugador" : "CPU";
        }

        if ("Jugador".equals(winner)) playerScore++; else aiScore++;
        for (BattleListener l : listeners) l.onTurn(playerCard.getName(), aiCard.getName(), winner);
        for (BattleListener l : listeners) l.onScoreChanged(playerScore, aiScore);
        if (playerScore == 2 || aiScore == 2) {
            String w = playerScore == 2 ? "Jugador" : "CPU";
            for (BattleListener l : listeners) l.onDuelEnded(w);
        }
    }
}
