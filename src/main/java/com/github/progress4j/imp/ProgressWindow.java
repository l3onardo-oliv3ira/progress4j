package com.github.progress4j.imp;

import static com.github.utils4j.imp.Strings.computeTabs;
import static com.github.utils4j.imp.SwingTools.invokeLater;
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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.progress4j.ICanceller;
import com.github.progress4j.IStageEvent;
import com.github.progress4j.IStepEvent;
import com.github.utils4j.imp.Args;
import com.github.utils4j.imp.SimpleFrame;
import com.github.utils4j.imp.Stack;
import com.github.utils4j.imp.SwingTools;

import net.miginfocom.swing.MigLayout;

@SuppressWarnings("serial")
class ProgressWindow extends SimpleFrame implements ICanceller {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProgressWindow.class);  

  private static final int MIN_DETAIL_HEIGHT = 312; 
  
  private static final int MIN_WIDTH = 450;
  
  private static final int MIN_HEIGHT = 154;
  
  private final JTextArea textArea = new JTextArea();
  
  private final JProgressBar progressBar = new JProgressBar();
  
  private final JPanel southPane = new JPanel();
  
  private final Map<Thread, List<Runnable>> cancels = new HashMap<>(2);
  
  private final Stack<ProgressState> stackState = new Stack<>();
  
  private int currentHeight = MIN_DETAIL_HEIGHT;
  
  ProgressWindow() {
    this(Images.PROGRESS_ICON.asImage().orElse(null));
  }
  
  ProgressWindow(Image icon) {
    this(icon, Images.LOG.asIcon().orElse(null));
  }

  ProgressWindow(Image icon, ImageIcon log) {
    super("Progresso", icon);
    final JPanel contentPane = new JPanel();
    
    contentPane.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.add(north(log), BorderLayout.NORTH);
    contentPane.add(center(), BorderLayout.CENTER);
    contentPane.add(south(), BorderLayout.SOUTH);

    resetProgress();
    setMinimumWindowDimension();
    setContentPane(contentPane);
    setLocationRelativeTo(null);
    setAutoRequestFocus(true);
  }

  private void setMinimumWindowDimension() {
    setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        Dimension windowDimension = ProgressWindow.this.getSize();
        Dimension minimumDimension = ProgressWindow.this.getMinimumSize();
        windowDimension.width = Math.max(windowDimension.width, minimumDimension.width);
        windowDimension.height =  Math.max(windowDimension.height, minimumDimension.height);
        ProgressWindow.this.setSize(windowDimension);
      }
    });    
  }

  private JPanel south() {
    JButton btnLimpar = new JButton("Limpar");
    btnLimpar.addActionListener((e) -> onClear(e));
    JButton cancelButton = new JButton("Cancelar");
    cancelButton.addActionListener((e) -> onEscPressed(e));
    southPane.setLayout(new MigLayout("fillx", "push[][]", "[][]"));
    southPane.add(btnLimpar);
    southPane.add(cancelButton);
    southPane.setVisible(false);
    return southPane;
  }

  private JScrollPane center() {
    textArea.setRows(8);
    textArea.setEditable(false);
    JScrollPane centerPane = new JScrollPane();
    centerPane.setViewportView(textArea);
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
      setBounds(getBounds().x, getBounds().y, getBounds().width, 154);
      seeDetailsPane.setText("<html><u>Ver detalhes</u></html>");
    }
    southPane.setVisible(show);
  }
  
  protected void onClear(ActionEvent e) {
    textArea.setText("");
  }
  
  @Override
  protected void onEscPressed(ActionEvent e) {
    int option = JOptionPane.showConfirmDialog(null, 
      "Deseja mesmo cancelar a operação?", 
      "Cancelamento da operação", 
      JOptionPane.YES_NO_OPTION
    );
    if (option == JOptionPane.YES_OPTION) {
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
      this.resetProgress();
    });
  }

  final void stepToken(IStepEvent e) {
    final int step = e.getStep();
    final int total = e.getTotal();
    final boolean indeterminated = e.isIndeterminated();
    final String message = e.getMessage();
    final StringBuilder text = new StringBuilder(computeTabs(e.getStackSize()));
    final String log;
    if (indeterminated) {
      log = text.append(message).toString();
    } else {
      log = text.append(format("Passo %s de %s: %s", step, total, message)).toString();
    }
    LOGGER.info(log);
    invokeLater(() -> {
      if (!indeterminated) {
        progressBar.setValue(step);
      }
      textArea.append(log + "\n\r");
    });
  }
  
  final void stageToken(IStageEvent e) {
    final String tabSize = computeTabs(e.getStackSize());
    final String message = e.getMessage();
    final String text = tabSize + message;
    LOGGER.info(text);
    invokeLater(() -> {
      if (!e.isEnd())
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
    SwingTools.invokeLater(() -> new ProgressWindow().reveal());
  }
}


