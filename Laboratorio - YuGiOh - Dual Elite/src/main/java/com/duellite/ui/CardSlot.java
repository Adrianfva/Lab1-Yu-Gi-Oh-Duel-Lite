package com.duellite.ui;

import com.duellite.domain.Card;

import javax.swing.*;
import java.awt.*;

public class CardSlot extends JPanel {
    private final JLabel img = new JLabel();
    private final JLabel name = new JLabel("-", SwingConstants.CENTER);
    private final JLabel stats = new JLabel("ATK 0 DEF 0", SwingConstants.CENTER);
    private Card card;

    public CardSlot(String title){
        setLayout(new BorderLayout(6,6));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2, true), title));
        setBackground(new Color(245, 248, 255));
        img.setHorizontalAlignment(SwingConstants.CENTER);
        img.setPreferredSize(new Dimension(240, 160));
        img.setOpaque(true);
        img.setBackground(Color.white);
        add(img, BorderLayout.CENTER);
        JPanel south = new JPanel(new GridLayout(2,1));
        south.setOpaque(false);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 13f));
        stats.setFont(stats.getFont().deriveFont(Font.PLAIN, 12f));
        south.add(name);
        south.add(stats);
        add(south, BorderLayout.SOUTH);
    }

    public void setLoading(){
        name.setText("Cargando...");
        stats.setText("");
        img.setIcon(null);
    }

    public void showCard(Card c, ImageIcon icon){
        this.card = c;
        name.setText(c.getName());
        stats.setText("ATK "+c.getAtk()+" DEF "+c.getDef());
        img.setIcon(scaleToLabel(icon, img));
    }

    public Card getCard(){ return card; }

    private static ImageIcon scaleToLabel(ImageIcon src, JLabel label){
        if (src == null || src.getIconWidth()<=0 || label.getWidth()==0) return src;
        int w = Math.max(1, label.getWidth()-12);
        int h = Math.max(1, label.getHeight()-12);
        Image scaled = src.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
