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

package com.bt4vt.repository.domain;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

/**
 * Transit stop departure
 *
 * @author Ben Sechrist
 */
@Root(name = "NextDepartures")
public class Departure {

  @Element(name = "RouteName")
  private String routeName;

  @Element(name = "AdjustedDepartureTime_TripNotes")
  private String notes;

  public String getRouteName() {
    return routeName;
  }

  public String getNotes() {
    return notes;
  }
}