package org.multibit.hd.brit.core.matcher;

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

import com.google.common.io.Files;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.brit.core.BritTestUtils;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;

public class MatchersTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testCreateMatcher() throws Exception {

    // Find the example Matcher PGP secret key ring file
    File matcherSecretKeyFile = BritTestUtils.makeFile(BritTestUtils.TEST_MATCHER_SECRET_KEYRING_FILE);
    MatcherConfig matcherConfig = new MatcherConfig(matcherSecretKeyFile, BritTestUtils.TEST_DATA_PASSWORD);

    // Create a random temporary directory for the matcher store to use
    File matcherStoreDirectory = Files.createTempDir();
    MatcherStore matcherStore = MatcherStores.newBasicMatcherStore(matcherStoreDirectory);

    Matcher matcher = Matchers.newBasicMatcher(matcherConfig, matcherStore);
    assertThat(matcher).isNotNull();

    // Check the Matcher PGP private key is stored properly
    assertThat(matcher.getConfig().getMatcherSecretKeyringFile()).isEqualTo(matcherSecretKeyFile);
  }
}
