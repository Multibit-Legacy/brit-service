package org.multibit.hd.brit_server.testing;

import java.io.File;

/**
 * <p>[Pattern] to provide the following to {@link Object}:</p>
 * <ul>
 * <li></li>
 * </ul>
 * <p>Example:</p>
 * <pre>
 * </pre>
 *
 * @since 0.0.1
 *  
 */
public class FixtureUtils {

  /**
   * Make a file reference - a file can be referenced as, say,
   * brit-service/src/test/resources/matcher/gpg/pubring.gpg
   * or
   * ./src/test/resources/matcher/gpg/pubring.gpg
   * depending on whether you are running via an IDE or in Maven
   *
   * @param rootFilename The root filename (relative to the root of the project directory e.g. src/test/resources/matcher/gpg/pubring.gpg
   *                     in the example above)
   *
   * @return File reference
   */
  public static File makeFile(String moduleName, String rootFilename) {
    File file = new File(moduleName + File.separator + rootFilename);
    if (!file.exists()) {
      file = new File("." + File.separator + rootFilename);
    }
    return file;
  }

}
