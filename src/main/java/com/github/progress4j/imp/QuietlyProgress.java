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

import static com.github.utils4j.imp.Throwables.runQuietly;

import com.github.progress4j.IProgress;
import com.github.progress4j.IQuietlyProgress;
import com.github.progress4j.IStage;

public class QuietlyProgress extends ProgressWrapper implements IQuietlyProgress {
  
  public static IQuietlyProgress wrap(IProgress progress) {
    return new QuietlyProgress(progress);
  }
  
  private QuietlyProgress(IProgress progress) {
    super(progress);
  }
  
  @Override
  public void begin(IStage stage) {
    runQuietly(() -> progress.begin(stage));
  }

  @Override
  public void begin(IStage stage, int total) {
    runQuietly(() -> progress.begin(stage, total));
  }
  
  @Override
  public void begin(String stage) {
    runQuietly(() -> progress.begin(stage));
  }

  @Override
  public void begin(String stage, int total) {
    runQuietly(() -> progress.begin(stage, total));
  }

  @Override
  public void step(String mensagem, Object... params) {
    runQuietly(() -> progress.step(mensagem, params));
  }
  
  @Override
  public void skip(long steps) {
    runQuietly(() -> progress.skip(steps));
  }
  
  @Override
  public void info(String mensagem, Object... params) {
    runQuietly(() -> progress.info(mensagem, params));
  }
  
  @Override
  public void end() {
    runQuietly(() -> progress.end());
  }
}
