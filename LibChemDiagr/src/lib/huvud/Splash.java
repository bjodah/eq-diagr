package lib.huvud;

/**
 * Copyright (C) 2015 I.Puigdomenech.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 * 
 * @author Ignasi Puigdomenech */
public class Splash extends javax.swing.JFrame {

  /**
   * Creates new form Splash
   * @param i0
   */
  public Splash(int i0) {
    initComponents();
    //---- Centre window on parent/screen
    int left,top;
    java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
    left = Math.max(0,(screenSize.width-this.getWidth())/2);
    top = Math.max(0,(screenSize.height-this.getHeight())/2);
    this.setLocation(Math.min(screenSize.width-this.getWidth()-20,left),
                     Math.min(screenSize.height-this.getHeight()-20, top));
    jLabelDatabaseIcon.setVisible(false);
    jLabelSpanaIcon.setVisible(false);
    if(i0 == 0) {jLabelDatabaseIcon.setVisible(true);} else if(i0 == 1) {jLabelSpanaIcon.setVisible(true);}
    javax.swing.border.Border bevelBorder = javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED);
    getRootPane().setBorder(bevelBorder);
    this.setVisible(true);
  }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel2 = new javax.swing.JLabel();
        jLabelSpanaIcon = new javax.swing.JLabel();
        jLabelDatabaseIcon = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        setUndecorated(true);
        setResizable(false);

        jLabel2.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        jLabel2.setText("Loading, please wait ...");

        jLabelSpanaIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lib/huvud/images/Spana_icon_32x32.gif"))); // NOI18N

        jLabelDatabaseIcon.setIcon(new javax.swing.ImageIcon(getClass().getResource("/lib/huvud/images/DataBase.gif"))); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(jLabelSpanaIcon)
                .addGap(0, 0, 0)
                .addComponent(jLabelDatabaseIcon)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelDatabaseIcon)
                            .addComponent(jLabelSpanaIcon)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jLabel2)))
                .addContainerGap(39, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabelDatabaseIcon;
    private javax.swing.JLabel jLabelSpanaIcon;
    // End of variables declaration//GEN-END:variables
}
