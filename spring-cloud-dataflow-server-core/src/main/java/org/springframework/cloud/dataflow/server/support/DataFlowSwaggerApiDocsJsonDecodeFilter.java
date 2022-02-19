package org.springframework.cloud.dataflow.server.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order
public class DataFlowSwaggerApiDocsJsonDecodeFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(DataFlowSwaggerApiDocsJsonDecodeFilter.class);

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;


    @Value("${springdoc.swagger-ui.configUrl:/v3/api-docs/swagger-config}")
    private String swaggerUiConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequestWrapper httpServletRequestWrapper = new HttpServletRequestWrapper((HttpServletRequest) request);
        final ContentCachingResponseWrapper httpServletResponseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

        String apiDocsPathContext = StringUtils.substringBeforeLast(apiDocsPath, "/");
        String swaggerUiConfigContext = StringUtils.substringBeforeLast(swaggerUiConfig, "/");
        if (httpServletRequestWrapper.getServletPath().startsWith(apiDocsPathContext) ||
                httpServletRequestWrapper.getServletPath().startsWith(swaggerUiConfigContext)) {
            // if api-docs path is requested, use wrapper classes, so that the body gets cached.
            chain.doFilter(httpServletRequestWrapper, httpServletResponseWrapper);

            ServletOutputStream outputStream = httpServletResponseWrapper.getResponse().getOutputStream();

            LOG.debug("Request for Swagger api-docs detected - unescaping json content.");
            String content = new String(httpServletResponseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
            content = StringUtils.stripStart(content, "\"");
            content = StringUtils.stripEnd(content, "\"");
            if (LOG.isTraceEnabled()) {
                LOG.trace("Using decoded JSON for serving api-docs: {}", content);
            }
            outputStream.write(StringEscapeUtils.unescapeJson(content).getBytes(StandardCharsets.UTF_8));
        } else {
            // all other scdf related api calls do nothing.
            chain.doFilter(request, response);
        }
    }
}
