/*
 * Copyright 2015 Ben Sechrist
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bt4vt.repository.bt;

import com.bt4vt.repository.domain.Departure;
import com.bt4vt.repository.exception.FetchException;
import com.google.inject.Singleton;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Fetches departures from BT4U.
 *
 * @author Ben Sechrist
 */
@Singleton
public class DepartureFetcher extends Fetcher {

  private static final String BT_DEPARTURES_PATH = "http://216.252.195.248/webservices/bt4u_webservice.asmx/GetNextDepartures?routeShortName={shortName}&stopCode={code}";

  /**
   * Returns a list of all departures for the given route <code>shortName</code> and
   * <code>stopCode</code>.
   * @param shortName the route short name
   * @param stopCode the stop code
   * @return list of departures
   * @throws FetchException Non serious error occurs fetching departures
   */
  public List<Departure> get(String shortName, int stopCode) throws FetchException {
    try {
      String url = BT_DEPARTURES_PATH
          .replace("{shortName}", shortName)
          .replace("{code}", String.valueOf(stopCode));
      return fetch(new URL(url)).departures;
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
