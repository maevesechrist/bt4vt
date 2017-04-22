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

package com.bt4vt.external.bt4u;

/**
 * Callback for asynchronous requests to BT4U server.
 *
 * @author Ben Sechrist
 */
public interface Callback<T> {

  /**
   * Called when any response is returned from the originating request.
   * @param t the result
   */
  void onResult(T t);

  /**
   * Called when the status code returned from a BT4U request is unexpected.
   * @param statusCode the HTTP status code
   * @param body the body of the HTTP response
   */
  void onFail(int statusCode, String body);

  /**
   * Called when an HTTP request throws an exception (i.e. Timeout).
   * @param exception the exception that occurred
   */
  void onException(Exception exception);
}
