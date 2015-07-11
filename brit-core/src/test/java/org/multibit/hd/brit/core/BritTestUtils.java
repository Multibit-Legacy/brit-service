package org.multibit.hd.brit.core;

/**
 * Copyright 2014 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;

public class BritTestUtils {

  public static final String TEST_MATCHER_PUBLIC_KEYRING_FILE = "src/test/resources/matcher/gpg/pubring.gpg";

  public static final String TEST_MATCHER_SECRET_KEYRING_FILE = "src/test/resources/matcher/gpg/secring.gpg";

  public static final String TEST_MATCHER_PUBLIC_KEY_FILE = "src/test/resources/matcher/export-to-payer/matcher-key.asc";

  private static final String MODULE_PREFIX = "brit-core";

  /**
   * The password used in the generation of the test PGP keys
   */
  public static final char[] TEST_DATA_PASSWORD = "password".toCharArray();


  /**
   * Make a file reference - a file can be referenced as, say,
   *     brit-core/src/test/resources/redeemer1/gpg/pubring.gpg
   * or
   *     ./src/test/resources/redeemer1/gpg/pubring.gpg
   * depending on whether you are running via an IDE or in Maven
   * @param rootFilename The root filename (relative to the root of the module directory e.g. src/test/resources/redeemer1/gpg/pubring.gpg
   *                     in the example above)
   * @return File reference
   */
  public static File makeFile(String rootFilename) {
    File file = new File(MODULE_PREFIX + File.separator + rootFilename);
    if (!file.exists()) {
      file =  new File("." + File.separator + rootFilename);
    }
    return file;
  }
}
