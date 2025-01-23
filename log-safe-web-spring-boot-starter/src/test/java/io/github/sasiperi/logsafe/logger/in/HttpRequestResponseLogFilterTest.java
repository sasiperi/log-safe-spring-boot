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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class HttpRequestResponseLogFilterTest {

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @Mock
    private HttpLogFilterHelper logFilterHelper;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HandlerExecutionChain handlerChain;

    @Mock
    private HandlerMethod handlerMethod;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private HttpRequestResponseLogFilter filter;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldLogRequestAndResponseWhenBothFlagsAreTrue() throws Exception { 
        // Arrange
    	filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, true, true);

        when(handlerMapping.getHandler(request)).thenReturn(handlerChain);
        when(handlerChain.getHandler()).thenReturn(handlerMethod);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Capture arguments passed to filter chain
        ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
        ArgumentCaptor<ServletResponse> responseCaptor = ArgumentCaptor.forClass(ServletResponse.class);

       
        // Verify logging behavior
        verify(logFilterHelper).logRequest(any(RepeatableContentCachingRequestWrapper.class), eq(handlerMethod));
        verify(filterChain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        // Assert that the request was wrapped correctly
        assertTrue(requestCaptor.getValue() instanceof RepeatableContentCachingRequestWrapper);

        // Assert that the response was wrapped correctly
        assertTrue(responseCaptor.getValue() instanceof ContentCachingResponseWrapper);
        
        verify(logFilterHelper).logResponse(any(ContentCachingResponseWrapper.class), eq(handlerMethod));
    }

    @Test
    void shouldNotLogWhenNoHandlerFound() throws Exception {
        // Arrange
    	filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, true, true);

        when(handlerMapping.getHandler(request)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verifyNoInteractions(logFilterHelper);
        
        // Verify the filter chain proceeds correctly
        verify(filterChain).doFilter(request, response);
        
        
    }

    @Test
    void shouldLogOnlyLogRequestWhenRequestFlagTrue() throws Exception {
        // Arrange
        filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, true, false);

        when(handlerMapping.getHandler(request)).thenReturn(handlerChain);
        when(handlerChain.getHandler()).thenReturn(handlerMethod);

        // Act
        filter.doFilterInternal(request, response, filterChain);
        
        // Verify the request is logged
        verify(logFilterHelper, times(1)).logRequest(any(), eq(handlerMethod));
        
        // Verify that the response is not logged
        verify(logFilterHelper, never()).logResponse(any(), eq(handlerMethod));
        
        // Verify the filter chain proceeds correctly
        verify(filterChain).doFilter(any(), any());
        
        
    }

    @Test
    void shouldNotLogAndProceedWhenExceptionInHandlerMappingOccuredAndErrorLG001Logged(CapturedOutput output) throws Exception {
        // Arrange
    	filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, true, true);

        when(handlerMapping.getHandler(request)).thenThrow(new RuntimeException("Error"));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verifyNoInteractions(logFilterHelper);
        // Verify filter chain proceeds as expected
        verify(filterChain).doFilter(request, response);
        
        // Verify log contains the expected error code and message
        assertTrue(output.getOut().contains("LG000: Error"),"Expected log output with error code LG000");
    }

    @Test
    void shouldNotLogRequestOrResponseWhenFlagsAreFalse() throws Exception {
        // Arrange
        filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, false, false);
        when(handlerMapping.getHandler(request)).thenReturn(handlerChain);
        
        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verifyNoInteractions(logFilterHelper);
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void shouldLogResponseLoggedAndBodyResetWhenResponseFlagTrue() throws Exception {
        // Arrange
        filter = new HttpRequestResponseLogFilter(handlerMapping, logFilterHelper, false, true);

        when(handlerMapping.getHandler(request)).thenReturn(handlerChain);
        when(handlerChain.getHandler()).thenReturn(handlerMethod);

        MockHttpServletResponse responseWrapper = spy(new MockHttpServletResponse());

        // Act
        filter.doFilterInternal(request, responseWrapper, filterChain);

        
        // Capture arguments passed to filter chain
        ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
        ArgumentCaptor<ServletResponse> responseCaptor = ArgumentCaptor.forClass(ServletResponse.class);

       
        // Verify logging behavior
        verify(filterChain).doFilter(requestCaptor.capture(), responseCaptor.capture());

        // Assert that the request was wrapped correctly
        assertTrue(requestCaptor.getValue() instanceof RepeatableContentCachingRequestWrapper);

        // Assert that the response was wrapped correctly
        assertTrue(responseCaptor.getValue() instanceof ContentCachingResponseWrapper);
        
        //verify that the logResponse called once.
        verify(logFilterHelper, times(1)).logResponse(any(ContentCachingResponseWrapper.class), eq(handlerMethod));
        // Verify the response is not logged
        verify(logFilterHelper, never()).logRequest(any(), eq(handlerMethod));
        
        
        // Verify the filter chain proceeds correctly
        verify(filterChain).doFilter(any(), any());
    }
}