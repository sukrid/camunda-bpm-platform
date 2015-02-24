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

import java.util.List;

import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessInstanceModificationEventTest extends PluggableProcessEngineTestCase {

  protected static final String INTERMEDIATE_TIMER_CATCH_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.intermediateTimerCatch.bpmn20.xml";
  protected static final String MESSAGE_START_EVENT_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.messageStart.bpmn20.xml";
  protected static final String TIMER_START_EVENT_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.timerStart.bpmn20.xml";
  protected static final String ONE_TASK_PROCESS = "org/camunda/bpm/engine/test/api/runtime/oneTaskProcess.bpmn20.xml";
  protected static final String TERMINATE_END_EVENT_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.terminateEnd.bpmn20.xml";
  protected static final String CANCEL_END_EVENT_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.cancelEnd.bpmn20.xml";

  @Deployment(resources = INTERMEDIATE_TIMER_CATCH_PROCESS)
  public void testStartBeforeIntermediateCatchEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("intermediateCatchEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task", taskInstance.getActivityId());
    assertEquals(1, taskInstance.getExecutionIds().length);

    ActivityInstance catchEventInstance = getChildInstanceForActivity(updatedTree, "intermediateCatchEvent");
    assertNotNull(catchEventInstance);
    assertEquals(0, catchEventInstance.getChildActivityInstances().length);
    assertEquals("intermediateCatchEvent", catchEventInstance.getActivityId());
    assertEquals(1, catchEventInstance.getExecutionIds().length);

    // and there is a timer job
    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);
    assertEquals(catchEventInstance.getExecutionIds()[0], job.getExecutionId());

  }

  @Deployment(resources = MESSAGE_START_EVENT_PROCESS)
  public void testStartBeforeMessageStartEvent() {
    runtimeService.correlateMessage("startMessage");
    ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
    assertNotNull(processInstance);

    EventSubscription startEventSubscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertNotNull(startEventSubscription);

    String processInstanceId = processInstance.getId();

    // when I start before the message start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    for (ActivityInstance child : updatedTree.getChildActivityInstances()) {
      assertNotNull(child);
      assertEquals(0, child.getChildActivityInstances().length);
      assertEquals("task", child.getActivityId());
      assertEquals(1, child.getExecutionIds().length);
    }

    // and there is only the message start event subscription
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery().singleResult();
    assertNotNull(subscription);
    assertEquals(startEventSubscription.getId(), subscription.getId());
  }

  @Deployment(resources = TIMER_START_EVENT_PROCESS)
  public void testStartBeforeTimerStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    Job startTimerJob = managementService.createJobQuery().singleResult();
    assertNotNull(startTimerJob);

    // when I start before the timer start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    for (ActivityInstance child : updatedTree.getChildActivityInstances()) {
      assertNotNull(child);
      assertEquals(0, child.getChildActivityInstances().length);
      assertEquals("task", child.getActivityId());
      assertEquals(1, child.getExecutionIds().length);
    }

    // and there is only one timer job
    Job job = managementService.createJobQuery().singleResult();
    assertNotNull(job);
    assertEquals(startTimerJob.getId(), job.getId());
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testStartBeforNoneStartEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // when I start before the none start event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theStart")
      .execute();

    // then there are two instances of "task"
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    for (ActivityInstance child : updatedTree.getChildActivityInstances()) {
      assertNotNull(child);
      assertEquals(0, child.getChildActivityInstances().length);
      assertEquals("theTask", child.getActivityId());
      assertEquals(1, child.getExecutionIds().length);
    }

    // and the process can be ended as usual
    List<Task> tasks = taskService.createTaskQuery().list();
    assertEquals(2, tasks.size());

    for (Task task : tasks) {
      taskService.complete(task.getId());
    }

    assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = ONE_TASK_PROCESS)
  public void testStartBeforeNoneEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
    String processInstanceId = processInstance.getId();

    // when I start before the none end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("theEnd")
      .execute();

    // then there is no effect
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance child = updatedTree.getChildActivityInstances()[0];
    assertNotNull(child);
    assertEquals(0, child.getChildActivityInstances().length);
    assertEquals("theTask", child.getActivityId());
    assertEquals(1, child.getExecutionIds().length);
  }

  @Deployment(resources = TERMINATE_END_EVENT_PROCESS)
  public void testStartBeforeTerminateEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    // when I start before the terminate end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("terminateEnd")
      .execute();

    // then the process instance is terminated
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNull(updatedTree);
    assertProcessEnded(processInstanceId);
  }

  @Deployment(resources = CANCEL_END_EVENT_PROCESS)
  public void testStartBeforeCancelEndEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    Task txTask = taskService.createTaskQuery().singleResult();
    assertEquals("txTask", txTask.getTaskDefinitionKey());

    // when I start before the cancel end event
    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("cancelEnd")
      .execute();

    // then the subprocess instance is cancelled
    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance child = updatedTree.getChildActivityInstances()[0];
    assertNotNull(child);
    assertEquals(0, child.getChildActivityInstances().length);
    assertEquals("theTask", child.getActivityId());
    assertEquals(1, child.getExecutionIds().length);

    Task afterCancellationTask = taskService.createTaskQuery().singleResult();
    assertNotNull(afterCancellationTask);
    assertTrue(!txTask.getId().equals(afterCancellationTask.getId()));
    assertEquals("afterCancellation", afterCancellationTask.getTaskDefinitionKey());
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
