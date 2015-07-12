package org.multibit.hd.brit.rest.servlets;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.ws.rs.core.HttpHeaders;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * <p>Request wrapper to provide the following to application:</p>
 * <ul>
 * <li>Adjustment of Accept-Language headers</li>
 * </ul>
 * <p>Problems can occur with the following:</p>
 * <ul>
 * <li><code>en_US</code> from Opera</li>
 * <li><code>en-419</code> from browsers with Spanish (Latin-America) set</li>
 * <li><code>chrome://global/locale/intl.properties</code> from browsers with Quick Locale Switcher installed</li>
 * </ul>
 */
public class SafeHeadersRequest extends HttpServletRequestWrapper {

  public SafeHeadersRequest(HttpServletRequest request) {
    super(request);
  }

  public String getHeader(String name) {

    HttpServletRequest request = (HttpServletRequest) getRequest();

    Preconditions.checkNotNull(request, "'request' must be present");

    String value = request.getHeader(name);

    return modifyAcceptLanguage(name, value);
  }

  @Override
  public Enumeration<String> getHeaders(String name) {

    List<String> list = Lists.newArrayList();

    HttpServletRequest request = (HttpServletRequest) getRequest();

    Preconditions.checkNotNull(request, "'request' must be present");

    Enumeration e = request.getHeaders(name);

    if (e != null) {
      while (e.hasMoreElements()) {
        String value = (String) e.nextElement();
        list.add(modifyAcceptLanguage(name, value));
      }
    }

    return Collections.enumeration(list);
  }

  /**
   * @param name  The header name
   * @param value The header value
   *
   * @return A safe Accept-Language header
   */
  private String modifyAcceptLanguage(String name, String value) {

    if (value != null && HttpHeaders.ACCEPT_LANGUAGE.equalsIgnoreCase(name)) {

      return value
        .replace("es-419", "es") // Fix es-419 issue
        .replace("_", "-") // Fix en_US issue
        .replace("chrome://global/locale/intl.properties", "en") // Fix Firefox QLS issue
        ;
    }

    // Default to returning whatever was passed in
    return value;

  }
}
