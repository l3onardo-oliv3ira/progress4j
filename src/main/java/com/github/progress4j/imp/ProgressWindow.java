/*
* MIT License
* 
* Copyright (c) 2022 Leonardo de Lima Oliveira
* 
* https://github.com/l3onardo-oliv3ira
* 
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
* 
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/


package com.github.progress4j.imp;

import static com.github.utils4j.gui.imp.SwingTools.invokeLater;
import static com.github.utils4j.gui.imp.SwingTools.setFixedMinimumSize;
import static com.github.utils4j.imp.Strings.computeTabs;
import static com.github.utils4j.imp.Throwables.tryRun;
import static java.lang.String.format;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.progress4j.IStageEvent;
import com.github.progress4j.IStepEvent;
import com.github.utils4j.ICanceller;
import com.github.utils4j.gui.imp.Dialogs;
import com.github.utils4j.gui.imp.SimpleFrame;
import com.github.utils4j.imp.Args;
import com.github.utils4j.imp.Stack;

import net.miginfocom.swing.MigLayout;

class ProgressWindow extends SimpleFrame implements ICanceller {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProgressWindow.class);  
  
  private static final int MIN_WIDTH = 450;
  
  private static final int MIN_HEIGHT = 154;
  
  private static final int MIN_DETAIL_HEIGHT = 312; 

  private final JPanel southPane = new JPanel();

  private final JTextArea textArea = new JTextArea();
  
  private final JScrollPane centerPane = new JScrollPane();
  
  private final JProgressBar progressBar = new JProgressBar();
  
  private final Stack<ProgressState> stackState = new Stack<>();

  private final Map<Thread, List<Runnable>> cancels = new HashMap<>(2);  
  
  private int currentHeight = MIN_DETAIL_HEIGHT;
  
  ProgressWindow() {
    this(Images.PROGRESS_ICON.asImage().orElse(null));
  }
  
  ProgressWindow(Image icon) {
    this(icon, Images.LOG.asIcon().orElse(null));
  }

  ProgressWindow(Image icon, ImageIcon log) {
    super("Progresso", icon);    
    setFixedMinimumSize(this, new Dimension(MIN_WIDTH, MIN_HEIGHT));
    setupLayout(log);
    resetProgress();
    setLocationRelativeTo(null);
    setAutoRequestFocus(true);
  }

  private void setupLayout(ImageIcon log) {
    JPanel contentPane = new JPanel();    
    contentPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.add(north(log), BorderLayout.NORTH);
    contentPane.add(center(), BorderLayout.CENTER);
    contentPane.add(south(), BorderLayout.SOUTH);
    setContentPane(contentPane);
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent windowEvent) {        
        onEscPressed(null);
      }
    });
  }

  private JPanel south() {
    JButton cancelButton = new JButton("Cancelar");
    cancelButton.addActionListener(this::onEscPressed);
    JButton btnLimpar = new JButton("Limpar");
    btnLimpar.setPreferredSize(cancelButton.getPreferredSize());
    btnLimpar.addActionListener(this::onClear);    
    southPane.setLayout(new MigLayout("fillx", "push[][]", "[][]"));
    southPane.add(btnLimpar);
    southPane.add(cancelButton);
    southPane.setVisible(false);
    return southPane;
  }

  private JScrollPane center() {
    textArea.setRows(8);
    textArea.setEditable(false);
    centerPane.setViewportView(textArea);
    centerPane.setVisible(false);
    return centerPane;
  }

  private JPanel north(ImageIcon log) {
    final JPanel northPane = new JPanel();
    northPane.setLayout(new GridLayout(3, 1, 0, 0));
    JLabel activityLabel = new JLabel("Registro de atividades");
    activityLabel.setIcon(log);
    activityLabel.setHorizontalAlignment(SwingConstants.LEFT);
    activityLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
    northPane.add(activityLabel);
    northPane.add(progressBar);
    JLabel seeDetailsPane = new JLabel("<html><u>Ver detalhes</u></html>");
    seeDetailsPane.setVerticalAlignment(SwingConstants.BOTTOM);
    seeDetailsPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    seeDetailsPane.setHorizontalAlignment(SwingConstants.CENTER);
    seeDetailsPane.setVerticalAlignment(SwingConstants.CENTER);
    seeDetailsPane.setForeground(Color.BLUE);
    seeDetailsPane.setFont(new Font("Tahoma", Font.ITALIC, 12));
    seeDetailsPane.addMouseListener(new MouseAdapter(){  
      public void mouseClicked(MouseEvent e) {
        setDetail(seeDetailsPane);
      }
    });
    northPane.add(seeDetailsPane);
    return northPane;
  }
  
  private void setDetail(JLabel seeDetailsPane) {
    boolean show = seeDetailsPane.getText().contains("Ver");
    if (show) {
      setBounds(getBounds().x, getBounds().y, getBounds().width, currentHeight);
      seeDetailsPane.setText("<html><u>Esconder detalhes</u></html>");
    }else {
      currentHeight = Math.max(getBounds().height, MIN_DETAIL_HEIGHT);
      setBounds(getBounds().x, getBounds().y, getBounds().width, MIN_HEIGHT);
      seeDetailsPane.setText("<html><u>Ver detalhes</u></html>");
    }
    centerPane.setVisible(show);
    southPane.setVisible(show);
  }
  
  protected void onClear(ActionEvent e) {
    textArea.setText("");
  }
  
  @Override
  protected void onEscPressed(ActionEvent e) {
    Dialogs.Choice choice = Dialogs.getBoolean(
      "Deseja mesmo cancelar a opera????o?",
      "Cancelamento da opera????o", 
      false
    );
    if (choice == Dialogs.Choice.YES) {
      this.cancel();
      this.unreveal();
    }
  }

  private void resetProgress() {
    this.textArea.setText("");
    this.progressBar.setIndeterminate(false);
    this.progressBar.setMaximum(0);
    this.progressBar.setMinimum(0);
    this.progressBar.setValue(-1);
    this.progressBar.setStringPainted(true);
    this.progressBar.setString("");
    this.stackState.clear();
  }

  final void reveal() {
    invokeLater(() -> { 
      this.setLocationRelativeTo(null);
      this.showToFront(); 
    });
  }
  
  final void exit() {
    invokeLater(super::close);
  }

  final void unreveal() {
    invokeLater(() -> {
      this.setVisible(false);      
    });
  }

  private long lineNumber = 0;
  final void stepToken(IStepEvent e) {
    final int step = e.getStep();
    final int total = e.getTotal();
    final boolean indeterminated = e.isIndeterminated();
    final String message = e.getMessage();
    final StringBuilder text = computeTabs(e.getStackSize());
    final String log;
    if (indeterminated || e.isInfo()){
      log = text.append(message).toString();
    } else {
      log = text.append(format("Passo %s de %s: %s", step, total, message)).toString();
    }
    LOGGER.info(log);
    invokeLater(() -> {
      if (!indeterminated) {
        progressBar.setValue(step);
      }
      if (lineNumber++ > 800) {
        textArea.setText(""); //auto clean
        lineNumber = 0;
      }
      textArea.append(log + "\n\r");
    });
  }
  
  final void stageToken(IStageEvent e) {
    final StringBuilder tabSize = computeTabs(e.getStackSize());
    final String message = e.getMessage();
    final String text = tabSize.append(message).toString();
    LOGGER.info(text);
    invokeLater(() -> {
      if (e.isStart())
        this.stackState.push(new ProgressState(this.progressBar));
      final boolean indeterminated = e.isIndeterminated();
      progressBar.setIndeterminate(indeterminated);
      if (!indeterminated) {
        progressBar.setMaximum(e.getTotal());
        progressBar.setMinimum(0);
        progressBar.setValue(e.getStep());
      }
      this.progressBar.setString(message);
      textArea.append(text + "\n\r");
      if (e.isEnd() && !this.stackState.isEmpty())
        this.stackState.pop().restore(this.progressBar);
    });    
  }

  final synchronized void cancel() {
    Runnable cancelCode = () -> {
      cancels.entrySet().stream()
        .peek(k -> {
          Thread key = k.getKey();
          if (key != Thread.currentThread()) { 
            key.interrupt();
          }
        })
        .map(k -> k.getValue())
        .flatMap(Collection::stream)
        .forEach(r -> tryRun(r::run)); 
      cancels.clear();
    };
    invokeLater(cancelCode);
  }
  
  @Override
  public synchronized final void cancelCode(Runnable cancelCode) {
    Args.requireNonNull(cancelCode, "cancelCode is null");
    Thread thread = Thread.currentThread();
    List<Runnable> codes = cancels.get(thread);
    if (codes == null)
      cancels.put(thread, codes = new ArrayList<>(2));
    codes.add(cancelCode);
  }
  
  private static class ProgressState {
    private String message;
    private int maximum;
    private int minimum;
    private int value;
    private boolean indeterminated;
    
    private ProgressState(JProgressBar bar) {
      message = bar.getString();
      maximum = bar.getMaximum();
      minimum = bar.getMinimum();
      indeterminated = bar.isIndeterminate();
      value = bar.getValue();
    }
    
    private void restore(JProgressBar bar) {
      bar.setString(message);
      bar.setMaximum(maximum);
      bar.setMinimum(minimum);
      bar.setValue(value);
      bar.setIndeterminate(indeterminated);
    }
  }
  
  public static void main(String[] args) {
    invokeLater(() -> new ProgressWindow().reveal());
  }
}


