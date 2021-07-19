// SPDX-License-Identifier: Apache-2.0

package org.ampretia;

import javax.inject.Inject;

import org.ampretia.actors.Market;
import org.ampretia.actors.Trader;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain    
public class Main implements QuarkusApplication {
  
  @Inject
  Trader t;

  @Inject 
  Market m;

  @Override
  public int run(String... args) throws Exception {   
  
    // create players
    
    t.go();

    

    return 0;
 }
}