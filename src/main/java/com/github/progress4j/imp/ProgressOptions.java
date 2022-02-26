package com.github.progress4j.imp;

import java.util.function.Consumer;

import com.github.progress4j.IProgress;
import com.github.progress4j.IProgressView;
import com.github.progress4j.IStage;
import com.github.progress4j.IStageEvent;
import com.github.progress4j.IState;
import com.github.progress4j.IStepEvent;

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;

public enum ProgressOptions implements IProgressView {
  IDLE; 
  
  @Override
  public void begin(IStage stage) {
  }
  
  @Override
  public void begin(IStage stage, int total) {
  }

  @Override
  public void step(String mensagem, Object ... params) {
  }

  @Override
  public void end() {
  }

  @Override
  public <T extends Throwable> T abort(T e) {
    return e;
  }

  @Override
  public Observable<IStepEvent> stepObservable() {
    return Observable.empty();
  }

  @Override
  public Observable<IStageEvent> stageObservable() {
    return Observable.empty();
  }
  
  @Override
  public IProgress stackTracer(Consumer<IState> consumer) {
    return this;
  }

  @Override
  public boolean isClosed() {
    return false;
  }

  @Override
  public IProgressView reset() {
    return this;
  }

  @Override
  public void display() {
    
  }

  @Override
  public void undisplay() {
    
  }

  @Override
  public void dispose() {
    
  }

  @Override
  public BehaviorSubject<IProgress> disposeObservable() {
    return null;
  }

  @Override
  public String getName() {
    return IDLE.getName();
  }

  @Override
  public void cancelCode(Runnable cancelCode) {
    
  }

  @Override
  public Throwable getAbortCause() {
    return null;
  }

  @Override
  public void info(String mensagem, Object... params) throws InterruptedException {
    
  }
}