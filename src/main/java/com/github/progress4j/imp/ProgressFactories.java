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
import java.util.function.Function;

import com.github.progress4j.IProgressFactory;
import com.github.progress4j.IProgressView;

public enum ProgressFactories implements IProgressFactory {
  
  IDLE(new ProgressIdleFactory()),
  
  LINE(new ProgressFrameLineFactory()),

  BOX(new ProgressFrameFactory()),
  
  SIMPLE(new ProgressFrameLineFactory(true)),

  THREAD(new MultiThreadedProgressFactory());

  private final IProgressFactory factory;
  
  ProgressFactories(IProgressFactory factory) {
    this.factory = factory;
  }

  public IProgressView get() {
    return factory.get();
  }

  @Override
  public void interrupt() {
    factory.interrupt();
  }
  
  @Override
  public boolean ifCanceller(Runnable code) {
    return factory.ifCanceller(code);
  }

  @Override
  public void cancel(Thread currentThread) {
    factory.cancel(currentThread);
  }
  
  public static <T> T withSimple(Function<IProgressView, T> function) {
    return withFactory(SIMPLE, function);
  }
  
  public static <T> T withLine(Function<IProgressView, T> function) {
    return withFactory(LINE, function);
  }

  public static <T> T withBox(Function<IProgressView, T> function) {
    return withFactory(BOX, function);
  }

  public static <T> T withThread(Function<IProgressView, T> function) {
    return withFactory(THREAD, function);
  }

  private static <T> T withFactory(IProgressFactory factory, Function<IProgressView, T> function) {
    IProgressView progress = factory.get();
    try {
      return function.apply(progress);
    } finally {
      progress.dispose();
    }
  }
}
