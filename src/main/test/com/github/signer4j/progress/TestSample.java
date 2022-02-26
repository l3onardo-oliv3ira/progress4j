package com.github.signer4j.progress;

import com.github.signer4j.progress.imp.ProgressFactory;
import com.github.utils4j.imp.Threads;

public class TestSample {
  enum Stage implements IStage {
    PROCESSING
  }
  public static void main(String[] args) throws InterruptedException {
    ProgressFactory f = new ProgressFactory();
    Thread t = new Thread(() -> {
      IProgressView progress = f.get();
      progress.display();
      try {
        int total = 200;
        progress.begin(Stage.PROCESSING, total);
        for(int i = 1; i <= total; i++) {
          progress.step("Operação  " + i);
          Threads.sleep(100);
        }
        progress.end();
        Threads.sleep(100);
      }catch(Exception e) {
        e.printStackTrace();
      }finally {
        progress.undisplay();
        progress.dispose();
      }
    });
    t.start();
    t.join();
  }
}
