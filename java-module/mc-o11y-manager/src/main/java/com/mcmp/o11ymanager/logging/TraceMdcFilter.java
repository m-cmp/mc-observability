package com.mcmp.o11ymanager.logging;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TraceMdcFilter extends OncePerRequestFilter {

  private static final Log logger = LogFactory.getLog(TraceMdcFilter.class);

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    try {
      Span span = Span.current();
      SpanContext ctx = span.getSpanContext();

      MDC.put("trace_id", ctx.isValid() ? ctx.getTraceId() : "");
      MDC.put("span_id", ctx.isValid() ? ctx.getSpanId() : "");
      MDC.put("trace_flags", ctx.isValid() ? String.format("%02x", ctx.getTraceFlags().asByte()) : "00");

      MDC.put("component", "o11y-manager");


      StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
      for (StackTraceElement el : stackTrace) {
        if (!el.getClassName().startsWith("java.") &&
            !el.getClassName().startsWith("jakarta.") &&
            !el.getClassName().startsWith("org.springframework") &&
            !el.getClassName().startsWith("sun.") &&
            !el.getClassName().contains("Filter") &&
            !el.getMethodName().equals("doFilterInternal")) {

          MDC.put("code.function", el.getClassName() + "." + el.getMethodName() + "()");
          MDC.put("code.file", el.getFileName());
          MDC.put("code.lineno", String.valueOf(el.getLineNumber()));
          break;
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}