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
package org.camunda.bpm.engine.test.api.runtime;

import java.util.Collections;
import java.util.List;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.history.HistoricVariableInstance;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.examples.bpmn.executionlistener.RecorderExecutionListener;
import org.camunda.bpm.engine.test.examples.bpmn.executionlistener.RecorderExecutionListener.RecordedEvent;

/**
 * @author Thorben Lindhauer
 *
 */
public class ProcessInstanceModificationTest extends PluggableProcessEngineTestCase {

  // TODO: separate the individual aspects (history, listener, events, variables, basics) into individual classes

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_ASYNC_TASK_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGatewayAsyncTask.bpmn20.xml";
  protected static final String SUBPROCESS_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocess.bpmn20.xml";
  protected static final String SUBPROCESS_LISTENER_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocessListeners.bpmn20.xml";
  protected static final String SUBPROCESS_BOUNDARY_EVENTS_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.subprocessBoundaryEvents.bpmn20.xml";


  // TODO: improve assertions on activity instance trees

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  public void testCancellation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(1, updatedTree.getChildActivityInstances().length);
    assertEquals("task2", updatedTree.getChildActivityInstances()[0].getActivityId());
  }

  @Deployment(resources = PARALLEL_GATEWAY_PROCESS)
  public void testCancellationThatEndsProcessInstance() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task2"))
      .execute();

    assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCreation() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance task1Instance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(task1Instance);
    assertEquals(0, task1Instance.getChildActivityInstances().length);
    assertEquals("task1", task1Instance.getActivityId());
    assertEquals(updatedTree.getId(), task1Instance.getParentActivityInstanceId());

    ActivityInstance task2Instance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(task2Instance);
    assertEquals(0, task2Instance.getChildActivityInstances().length);
    assertEquals("task2", task2Instance.getActivityId());
    assertEquals(updatedTree.getId(), task2Instance.getParentActivityInstanceId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_ASYNC_TASK_PROCESS)
  public void testCreationAsync() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .execute();

    // the task does not yet exist because it is started asynchronously
    Task task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertNull(task);

    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);

    executeAvailableJobs();
    task = taskService.createTaskQuery().taskDefinitionKey("task2").singleResult();
    assertNotNull(task);
  }

  @Deployment(resources = SUBPROCESS_PROCESS)
  public void testCreationInNestedScope() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance outerTaskInstance = getChildInstanceForActivity(updatedTree, "outerTask");
    assertNotNull(outerTaskInstance);
    assertEquals(0, outerTaskInstance.getChildActivityInstances().length);
    assertEquals("outerTask", outerTaskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask");
    assertNotNull(innerTaskInstance);
    assertEquals(0, innerTaskInstance.getChildActivityInstances().length);
    assertEquals("innerTask", innerTaskInstance.getActivityId());
  }

  // TODO: message event subscription test

  @Deployment(resources = SUBPROCESS_BOUNDARY_EVENTS_PROCESS)
  public void testCreationEventSubscription() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("subprocess");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .execute();

    // TODO: also assert that we have an additional execution for the inner timer

    // then two timer jobs should have been created
    assertEquals(2, managementService.createJobQuery().count());
    Job innerJob = managementService.createJobQuery().activityId("innerTimer").singleResult();
    assertNotNull(innerJob);
    assertEquals(runtimeService.createExecutionQuery().activityId("innerTask").singleResult().getId(),
        innerJob.getExecutionId());

    Job outerJob = managementService.createJobQuery().activityId("outerTimer").singleResult();
    assertNotNull(outerJob);
    assertEquals(runtimeService.createExecutionQuery().activityId("subProcess").singleResult().getId(),
        outerJob.getExecutionId());

    // when executing the jobs
    managementService.executeJob(innerJob.getId());

    Task innerBoundaryTask = taskService.createTaskQuery().taskDefinitionKey("innerAfterBoundaryTask").singleResult();
    assertNotNull(innerBoundaryTask);

    managementService.executeJob(outerJob.getId());

    Task outerBoundaryTask = taskService.createTaskQuery().taskDefinitionKey("outerAfterBoundaryTask").singleResult();
    assertNotNull(outerBoundaryTask);

  }

  @Deployment(resources = SUBPROCESS_LISTENER_PROCESS)
  public void testListenerInvocation() {
    RecorderExecutionListener.clear();

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
        "subprocess",
        Collections.<String, Object>singletonMap("listener", new RecorderExecutionListener()));

    assertTrue(RecorderExecutionListener.getRecordedEvents().isEmpty());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("innerTask")
      .execute();

    List<RecordedEvent> recordedEvents = RecorderExecutionListener.getRecordedEvents();
    assertEquals(2, recordedEvents.size());

    ActivityInstance activityInstanceTree = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstance subprocessInstance = getChildInstanceForActivity(activityInstanceTree, "subProcess");
    ActivityInstance innerTaskInstance = getChildInstanceForActivity(subprocessInstance, "innerTask");

    RecordedEvent firstEvent = recordedEvents.get(0);
    RecordedEvent secondEvent = recordedEvents.get(1);

    assertEquals("subProcess", firstEvent.getActivityId());
    assertEquals(subprocessInstance.getId(), firstEvent.getActivityInstanceId());
    assertEquals(ExecutionListener.EVENTNAME_START, secondEvent.getEventName());

    assertEquals("innerTask", secondEvent.getActivityId());
    assertEquals(innerTaskInstance.getId(), secondEvent.getActivityInstanceId());
    assertEquals(ExecutionListener.EVENTNAME_START, secondEvent.getEventName());

    RecorderExecutionListener.clear();

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(innerTaskInstance.getId())
      .execute();

    // this is due to skipCustomListeners configuration on deletion
    assertTrue(RecorderExecutionListener.getRecordedEvents().isEmpty());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCreationWithVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance task2Instance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(task2Instance);
    assertEquals(1, task2Instance.getExecutionIds().length);
    String task2ExecutionId = task2Instance.getExecutionIds()[0];

    assertEquals("procInstValue", runtimeService.getVariableLocal(processInstance.getId(), "procInstVar"));
    assertEquals("localValue", runtimeService.getVariableLocal(task2ExecutionId, "localVar"));
  }

  // TODO: move this test case somewhere so that it is only executed with appropriate
  // history level
  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCreationWithVariablesInHistory() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .setVariable("procInstVar", "procInstValue")
      .setVariableLocal("localVar", "localValue")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    ActivityInstance task2Instance = getChildInstanceForActivity(updatedTree, "task2");

    HistoricVariableInstance procInstVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("procInstVar")
      .singleResult();

    assertNotNull(procInstVariable);
    assertEquals(updatedTree.getId(), procInstVariable.getActivityInstanceId());
    assertEquals("procInstVar", procInstVariable.getName());
    assertEquals("procInstValue", procInstVariable.getValue());

    HistoricVariableInstance localVariable = historyService.createHistoricVariableInstanceQuery()
      .variableName("localVar")
      .singleResult();

    // TODO: problem is that we already fire the history events on variable creation in the INIT operation
    // but should be in the START operation. Could be implemented via ExecutionStartContext
    assertNotNull(localVariable);
    assertEquals(task2Instance.getId(), localVariable.getActivityInstanceId());
    assertEquals("localVar", localVariable.getName());
    assertEquals("localValue", localVariable.getValue());

  }

  // TODO: test activity instance id on deletion

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCancellationAndCreation() {
    // TODO: The problem is here that the process instance execution gets deleted
    // from the database although it should not; this is due to tree compactation

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("exclusiveGateway");

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("task2")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstance.getId(), updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);
    assertEquals("task2", updatedTree.getChildActivityInstances()[0].getActivityId());
  }

  public String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
    ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }

  /**
   * Important that only the direct children are considered here. If you change this,
   * the test assertions are not as tight anymore.
   */
  public ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      if (childInstance.getActivityId().equals(activityId)) {
        return childInstance;
      }
    }

    return null;
  }
}
