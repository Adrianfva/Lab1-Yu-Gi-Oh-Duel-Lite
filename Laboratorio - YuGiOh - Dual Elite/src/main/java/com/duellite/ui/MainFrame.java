package com.duellite.ui;

import com.duellite.core.BattleListener;
import com.duellite.core.DuelEngine;
import com.duellite.domain.Card;
import com.duellite.net.YgoApiClient;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainFrame extends JFrame implements BattleListener {
    private final ExecutorService pool = Executors.newFixedThreadPool(6);
    private final YgoApiClient api = new YgoApiClient();
    private final DuelEngine engine = new DuelEngine();
    private final JTextArea log = new JTextArea();
    private final JButton[] choose = { new JButton("Jugar 1"), new JButton("Jugar 2"), new JButton("Jugar 3") };

    private final CardSlot[] playerSlots = { new CardSlot("Jugador - Carta 1"), new CardSlot("Jugador - Carta 2"), new CardSlot("Jugador - Carta 3") };
    private final CardSlot[] aiSlots = { new CardSlot("IA - Carta 1"), new CardSlot("IA - Carta 2"), new CardSlot("IA - Carta 3") };

    private final List<Card> player = new ArrayList<>();
    private final List<Card> ai = new ArrayList<>();
    private final boolean[] usedPlayer = new boolean[3];
    private final boolean[] usedAi = new boolean[3];
    private final Random rng = new Random();
    private boolean duelRunning = false;
    private boolean decksReady = false;

    public MainFrame(){
        super("Duel Lite");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(new Dimension(1080, 720));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8,8));

        // Encabezado simple
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Duel Lite - Mini Duelo"));
        add(top, BorderLayout.NORTH);

        // Panel central con dos filas de cartas
        JPanel center = new JPanel(new GridLayout(2,1,8,8));
        center.add(buildRowPanel("Jugador", playerSlots, null));
        center.add(buildRowPanel("Máquina", aiSlots, null));
        add(center, BorderLayout.CENTER);

        // Panel izquierdo con botones verticales
        JPanel left = new JPanel();
        left.setLayout(new GridLayout(5,1,6,6));
        left.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        left.add(new JLabel("Acciones"));
        for (JButton b : choose) left.add(b);
        add(left, BorderLayout.WEST);

        // Log a la derecha
        log.setEditable(false);
        JScrollPane logScroll = new JScrollPane(log);
        logScroll.setPreferredSize(new Dimension(360, 100));
        add(logScroll, BorderLayout.EAST);

        engine.addListener(this);
        for (int i=0;i<3;i++){
            final int idx = i;
            choose[i].addActionListener(e -> onChoose(idx));
            choose[i].setEnabled(false);
        }

        // Cargar cartas automáticamente al abrir; sólo habilitar inicio cuando estén listas
        beginLoadDecks();
    }

    private JPanel buildRowPanel(String title, CardSlot[] slots, JButton[] actions){
        JPanel p = new JPanel(new GridLayout(1,3,8,8));
        for (int i=0;i<3;i++){
            JPanel cell = new JPanel(new BorderLayout());
            cell.add(slots[i], BorderLayout.CENTER);
            p.add(cell);
        }
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private void startDuel(){
        if (duelRunning || !decksReady) { append("Aún no se han cargado las cartas.\n"); return; }
        duelRunning = true;
        for (JButton b : choose) b.setEnabled(true);
        append("Duelo listo. Turno inicial: "+ (engine.randomFirstTurnIsPlayer()?"Jugador":"Máquina") +"\n");
    }

    private void beginLoadDecks(){
        append("Cargando cartas desde la API...\n");
        for (CardSlot cs : playerSlots) cs.setLoading();
        for (CardSlot cs : aiSlots) cs.setLoading();
        player.clear(); ai.clear();
        for (int i=0;i<3;i++){ usedPlayer[i]=false; usedAi[i]=false; }
        for (int i=0;i<3;i++){
            final int idx = i;
            pool.submit(() -> loadIntoSlot(idx, true));
            pool.submit(() -> loadIntoSlot(idx, false));
        }
        pool.submit(() -> {
            try {
                while (player.size()<3 || ai.size()<3) Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            SwingUtilities.invokeLater(() -> {
                decksReady = true;
                append("¡Cartas listas! El duelo comienza...\n");
                startDuel();  // Iniciar automáticamente
            });
        });
    }

    private void loadIntoSlot(int index, boolean isPlayer){
        try {
            Card c = api.fetchRandomMonster();
            ImageIcon icon = fetchIcon(c.getImageUrl());
            SwingUtilities.invokeLater(() -> {
                if (isPlayer){
                    ensureSize(player, index);
                    player.set(index, c);
                    playerSlots[index].showCard(c, icon);
                } else {
                    ensureSize(ai, index);
                    ai.set(index, c);
                    aiSlots[index].showCard(c, icon);
                }
            });
        } catch (Exception ex){
            SwingUtilities.invokeLater(() -> append("Error cargando carta: "+ex.getMessage()+"\n"));
        }
    }

    private static void ensureSize(List<Card> list, int idx){
        while (list.size()<=idx) list.add(null);
    }

    private ImageIcon fetchIcon(String url){
        try { return new ImageIcon(new URL(url)); } catch(Exception e){ return new ImageIcon(); }
    }

    private void onChoose(int idx){
        if (!duelRunning) { append("Aún no inicia el duelo.\n"); return; }
        if (idx<0 || idx>=3 || usedPlayer[idx]) { append("Carta no disponible.\n"); return; }
        Card pc = player.get(idx);
        usedPlayer[idx] = true;

        int aiIdx = pickAiIndex();
        if (aiIdx<0) { append("IA sin cartas disponibles.\n"); return; }
        Card ac = ai.get(aiIdx);
        usedAi[aiIdx] = true;

        boolean playerAtk = true; // sin opción de defensa manual
        boolean aiAtk = new Random().nextBoolean();
        engine.resolveTurn(pc, ac, playerAtk, aiAtk);

        choose[idx].setEnabled(false);
    }

    private int pickAiIndex(){
        List<Integer> free = new ArrayList<>();
        for (int i=0;i<3;i++) if (!usedAi[i]) free.add(i);
        if (free.isEmpty()) return -1;
        return free.get(rng.nextInt(free.size()));
    }

    private void append(String s){ log.append(s); log.setCaretPosition(log.getDocument().getLength()); }

    // BattleListener
    @Override public void onTurn(String playerCard, String aiCard, String winner) {
        append("Turno: '"+playerCard+"' vs '"+aiCard+"' -> Gana: "+winner+"\n");
    }
    @Override public void onScoreChanged(int playerScore, int aiScore) {
        append("Marcador: "+playerScore+" - "+aiScore+"\n");
        if (playerScore==2 || aiScore==2){
            append("Ganador final: "+ (playerScore==2?"Héroe":"CPU") +"\n");
            duelRunning = false;
            for (JButton b : choose) b.setEnabled(false);
        }
    }
    @Override public void onDuelEnded(String winner) {
        // engine también dispara este evento; la UI ya maneja cierre en onScoreChanged
    }
}
