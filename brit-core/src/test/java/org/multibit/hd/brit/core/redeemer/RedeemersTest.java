package org.multibit.hd.brit.core.redeemer;

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

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.brit.core.redeemer.Redeemer;
import org.multibit.hd.brit.core.redeemer.RedeemerConfig;

import static org.fest.assertions.api.Assertions.assertThat;

public class RedeemersTest {

  @Before
  public void setUp() throws Exception {
  }

  @Test
  public void testCreateRedeemer() throws Exception {
    ECKey redeemerECKey = new ECKey();
    RedeemerConfig redeemerConfig = new RedeemerConfig(redeemerECKey);

    Redeemer redeemer = org.multibit.hd.brit.core.redeemer.Redeemers.newBasicRedeemer(redeemerConfig);

    // Check the ECKey is stored correctly
    assertThat(redeemer.getConfig().getRedeemerECKey()).isEqualTo(redeemerECKey);
  }
}
