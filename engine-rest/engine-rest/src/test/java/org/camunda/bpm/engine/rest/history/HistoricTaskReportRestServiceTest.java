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
package org.camunda.bpm.engine.rest.history;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import org.camunda.bpm.engine.AuthorizationException;
import org.camunda.bpm.engine.history.HistoricTaskInstanceReport;
import org.camunda.bpm.engine.history.HistoricTaskInstanceReportResult;
import org.camunda.bpm.engine.rest.AbstractRestServiceTest;
import org.camunda.bpm.engine.rest.util.container.TestContainerRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response.Status;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.path.json.JsonPath.from;
import static org.camunda.bpm.engine.rest.helper.MockProvider.EXAMPLE_HISTORIC_TASK_END_TIME;
import static org.camunda.bpm.engine.rest.helper.MockProvider.EXAMPLE_HISTORIC_TASK_REPORT_COUNT;
import static org.camunda.bpm.engine.rest.helper.MockProvider.EXAMPLE_HISTORIC_TASK_REPORT_DEFINITION;
import static org.camunda.bpm.engine.rest.helper.MockProvider.EXAMPLE_HISTORIC_TASK_START_TIME;
import static org.camunda.bpm.engine.rest.helper.MockProvider.createMockHistoricTaskInstanceReport;
import static org.camunda.bpm.engine.rest.helper.MockProvider.createMockHistoricTaskInstanceReportWithProcDef;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Stefan Hentschel.
 */
public class HistoricTaskReportRestServiceTest extends AbstractRestServiceTest {


  @ClassRule
  public static TestContainerRule rule = new TestContainerRule();

  protected static final String TASK_REPORT_URL = TEST_RESOURCE_ROOT_PATH + "/history/task/report";

  protected HistoricTaskInstanceReport mockedReportQuery;
  protected SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
  protected Date startDate;
  protected Date endDate;

  @Before
  public void setUpRuntimeData() throws ParseException {
    mockedReportQuery = setUpMockHistoricProcessInstanceReportQuery();

    startDate = format.parse(EXAMPLE_HISTORIC_TASK_START_TIME);
    endDate = format.parse(EXAMPLE_HISTORIC_TASK_END_TIME);

  }

  private HistoricTaskInstanceReport setUpMockHistoricProcessInstanceReportQuery() {
    HistoricTaskInstanceReport mockedReportQuery = mock(HistoricTaskInstanceReport.class);

    List<HistoricTaskInstanceReportResult> taskReportResults = createMockHistoricTaskInstanceReport();
    List<HistoricTaskInstanceReportResult> taskReportResultsWithProcDef = createMockHistoricTaskInstanceReportWithProcDef();
    when(mockedReportQuery.completedAfter(startDate)).thenReturn(mockedReportQuery);
    when(mockedReportQuery.completedBefore(endDate)).thenReturn(mockedReportQuery);
    when(mockedReportQuery.countByTaskDefinitionKey()).thenReturn(taskReportResults);
    when(mockedReportQuery.countByProcessDefinitionKey()).thenReturn(taskReportResultsWithProcDef);

    when(processEngine.getHistoryService().createHistoricTaskInstanceReport()).thenReturn(mockedReportQuery);

    return mockedReportQuery;
  }

  @Test
  public void testEmptyReport() {
    given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
      .when()
        .get(TASK_REPORT_URL);

    verify(mockedReportQuery).countByTaskDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  public void testMissingAuthorization() {
    String message = "not authorized";
    when(mockedReportQuery.countByTaskDefinitionKey()).thenThrow(new AuthorizationException(message));

    given()
      .then()
        .expect()
          .statusCode(Status.FORBIDDEN.getStatusCode())
          .contentType(ContentType.JSON)
          .body("type", equalTo(AuthorizationException.class.getSimpleName()))
          .body("message", equalTo(message))
      .when()
        .get(TASK_REPORT_URL);
  }

  @Test
  public void testHistoricTaskReport() {
    Response response = given()
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
          .body("[0].definition", equalTo(EXAMPLE_HISTORIC_TASK_REPORT_DEFINITION))
          .body("[0].count", equalTo(EXAMPLE_HISTORIC_TASK_REPORT_COUNT.intValue()))
      .when()
        .get(TASK_REPORT_URL);

    String content = response.asString();
    List<String> reports = from(content).getList("");
    Assert.assertEquals("There should be one report returned.", 1, reports.size());
    Assert.assertNotNull("The returned report should not be null.", reports.get(0));
  }

  @Test
  public void testHistoricTaskReportWithCompleteBefore() {
    given()
      .queryParam("completedBefore", EXAMPLE_HISTORIC_TASK_END_TIME)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
      .when()
        .get(TASK_REPORT_URL);

    verify(mockedReportQuery).completedBefore(endDate);
    verify(mockedReportQuery).countByTaskDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  public void testHistoricTaskReportWithCompleteAfter() {
    given()
      .queryParam("completedAfter", EXAMPLE_HISTORIC_TASK_START_TIME)
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
      .when()
        .get(TASK_REPORT_URL);

    verify(mockedReportQuery).completedAfter(startDate);
    verify(mockedReportQuery).countByTaskDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  public void testHistoricTaskReportWithGroupByProcDef() {
    given()
      .queryParam("groupBy", "processDefinition")
      .then()
        .expect()
          .statusCode(Status.OK.getStatusCode())
          .contentType(ContentType.JSON)
      .when()
        .get(TASK_REPORT_URL);

    verify(mockedReportQuery).countByProcessDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  public void testHistoricTaskReportWithGroupByTaskDef() {
    given()
      .queryParam("groupBy", "taskDefinition")
      .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
      .when()
      .get(TASK_REPORT_URL);

    verify(mockedReportQuery).countByTaskDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }

  @Test
  public void testHistoricTaskReportWithGroupByAnyDef() {
    // should return same definitions as task definition
    given()
      .queryParam("groupBy", "anotherDefinition")
      .then()
      .expect()
      .statusCode(Status.OK.getStatusCode())
      .contentType(ContentType.JSON)
      .when()
      .get(TASK_REPORT_URL);

    verify(mockedReportQuery).countByTaskDefinitionKey();
    verifyNoMoreInteractions(mockedReportQuery);
  }
}
