package org.multibit.hd.brit.rest.utils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * <p>
 * Utility to provide the following to application:</p>
 * <ul>
 * <li>Methods to operate on streams</li>
 * </ul>
 */
public class StreamUtils {

  /**
   * Fully copy an input stream to a String
   *
   * @param is The {@link java.io.InputStream}
   *
   * @return A String encoded in UTF8
   *
   * @throws java.io.IOException
   */
  public static String toString(final InputStream is) throws IOException {
    return toString(is, Charsets.UTF_8);
  }

  /**
   * Full copy an input stream to a String
   *
   * @param is The {@link java.io.InputStream}
   * @param cs The {@link java.nio.charset.Charset}
   *
   * @return A String encoded in the charset
   *
   * @throws java.io.IOException
   */
  public static String toString(final InputStream is, final Charset cs) throws IOException {

    // Use Java7 try-with-resources to guarantee closure
    try (InputStreamReader isr = new InputStreamReader(is, cs)) {
      return CharStreams.toString(isr);
    }
  }
}
