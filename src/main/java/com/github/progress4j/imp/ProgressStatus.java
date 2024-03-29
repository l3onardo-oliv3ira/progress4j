package com.github.progress4j.imp;

import static com.github.utils4j.imp.Throwables.runQuietly;

import java.io.File;

import com.github.progress4j.IProgress;
import com.github.progress4j.IStage;
import com.github.utils4j.imp.Args;
import com.github.utils4j.imp.DownloadStatus;
import com.github.utils4j.imp.Sizes;

public class ProgressStatus extends DownloadStatus {
  
  private long total;  
  
  private long increment = 1;
  
  private final IStage stage;  
  
  private final IProgress progress;

  public ProgressStatus(IProgress progress, String stage) {
    this(progress, new Stage(stage), null);
  }
  
  public ProgressStatus(IProgress progress, IStage stage) {
    this(progress, stage, null);
  }

  public ProgressStatus(IProgress progress, String stage, File saveHere) {
    this(progress, stage, saveHere, true);
  }
  
  public ProgressStatus(IProgress progress, IStage stage, File saveHere) {
    this(progress, stage, saveHere, false);
  }  

  public ProgressStatus(IProgress progress, String stage, boolean rejectEmpty) { 
    this(progress, new Stage(stage), rejectEmpty);
  }

  public ProgressStatus(IProgress progress, IStage stage, boolean rejectEmpty) { 
    this(progress, stage, null, rejectEmpty);
  }
  
  public ProgressStatus(IProgress progress, String stage, File saveHere, boolean rejectEmpty) { 
    this(progress, new Stage(stage), saveHere, rejectEmpty);
  }  

  public ProgressStatus(IProgress progress, IStage stage, File saveHere, boolean rejectEmpty) { 
    super(rejectEmpty, saveHere);
    this.stage = Args.requireNonNull(stage, "stage is null");
    this.progress = Args.requireNonNull(progress, "progress is null");    
  }

  @Override
  protected void onStepStart(long total) throws InterruptedException {
    this.total = total;
    this.increment = 1;
    if (total > 0)
      progress.begin(stage, 100);
    else
      progress.begin(stage);
  }
  
  @Override
  protected void onStepEnd() throws InterruptedException {
    progress.end();
    
  }
  
  @Override
  protected void onStepFail(Throwable e) {
    super.onStepFail(e);
    runQuietly(this::onStepEnd);
  }
  
  @Override
  protected void onStepStatus(long written) throws InterruptedException {
    if (total > 0) {
      handlePositive(written);
    } else {
      handleNegative(written);
    }
  }
  
  private void handleNegative(long written) throws InterruptedException {
    long now = System.currentTimeMillis();
    if (now - increment > 2000L) {      
      progress.info("Baixados " + Sizes.defaultFormat(written));
      increment = now;      
    }
  }

  private void handlePositive(long written) throws InterruptedException {
    float percent = 100f * written / total;
    if (percent >= increment) {
      progress.step("Baixados %d%%", increment++);
    }
    if (increment <= percent) {  // <= or < ?
      long diff = (long)(percent - increment + 1);
      progress.skip(diff);
      increment += diff;
    }
  } 
}
