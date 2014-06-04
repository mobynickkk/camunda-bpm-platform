/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.connect.interceptor;

import org.camunda.bpm.connect.ConnectorRequest;

/**
 * Represents a request invocation / request execution.
 *
 * @see RequestInterceptor#handleInvocation(ConnectorInvocation)
 *
 * @author Daniel Meyer
 *
 */
public interface ConnectorInvocation {

  /**
   * The underlying raw request.
   * @return the raw request as executed by the connector
   */
  public Object getTarget();

  /**
   * <p>The connector request as created through the API. Accessing the request from an
   * interceptor may be useful for setting additional properties on the raw request
   * (returned by {@link #getTarget()}) that are not supported by the connector.</p>
   *
   * <p>NOTE: setting parameters on the request (via) {@link ConnectorRequest#setRequestParameter(String, Object)}
   * will not have any effects once the request is executed.</p>
   *
   * @return the connector request
   */
  public ConnectorRequest<?> getRequest();

  /**
   * Makes the request proceed through the interceptor chain.
   * {@link RequestInterceptor} implementations are responsible for
   * calling {@link #proceed()} on the invocation.
   *
   * @return the result of the invocation.
   * @throws Exception
   */
  public Object proceed() throws Exception;

}
