/*
 * Copyright [2024] [author: Sasi Peri] [company: FourthQuest]
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package io.github.sasiperi.logsafe.logger.in;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;

/**
 * This Request Wrapper class is created to log the request body at the start of request. As OOTB
 * wrapper has limitation to log the request body at the start of the request. Thus we decided to
 * add this custom-wrapper as suggested by Spring Dev and Ref:
 * https://github.com/spring-projects/spring-framework/pull/24533#issuecomment-589188646
 * @author sasiperi
 * @since 11.20.2023
 */
public class RepeatableContentCachingRequestWrapper extends ContentCachingRequestWrapper {
  public RepeatableContentCachingRequestWrapper(HttpServletRequest request) throws IOException {
    super(request);
    StreamUtils.drain(super.getInputStream());
  }

  @Override
  public ServletInputStream getInputStream() {
    return new ByteServletInputStream(getContentAsByteArray());
  }

  private static class ByteServletInputStream extends ServletInputStream {

    private final InputStream is;

    private ByteServletInputStream(byte[] content) {
      this.is = new ByteArrayInputStream(content);
    }

    @Override
    public boolean isFinished() {
      return true;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {}

    @Override
    public int read() throws IOException {
      return this.is.read();
    }

    @Override
    public void close() throws IOException {
      this.is.close();
    }
  }
}
