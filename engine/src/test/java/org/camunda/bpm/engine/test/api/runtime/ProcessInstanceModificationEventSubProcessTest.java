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

import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Roman Smirnov
 *
 */
public class ProcessInstanceModificationEventSubProcessTest extends PluggableProcessEngineTestCase {

  protected static final String INTERRUPTING_EVENT_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingEventSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_EVENT_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingEventSubProcess.bpmn20.xml";
  protected static final String INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingEventSubProcessInsideSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingEventSubProcessInsideSubProcess.bpmn20.xml";

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeTaskInsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeTaskInsideEventSubProcessAndCancelTaskOutsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeStartEventInsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeTaskInsideNonInterruptingEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeTaskInsideNonInterruptingEventSubProcessAndCancelTaskOutsideEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    ActivityInstance tree = runtimeService.getActivityInstance(processInstance.getId());

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .cancelActivityInstance(getInstanceIdForActivity(tree, "task1"))
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeStartEventInsideNonInterruptingEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS)
  public void testStartBeforeNonInterruptingEventSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeTaskInsideEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeStartEventInsideEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeTaskInsideEventSubProcessInsideSubProcessTask2ShouldStay() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task2", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeStartEventInsideEventSubProcessInsideSubProcessTask2ShouldBeCancelled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeEventSubProcessInsideSubProcessTask2ShouldBeCancelled() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(updatedTree, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeTaskInsideNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeStartEventInsideNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeNonInterruptingEventSubProcessInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    // TODO: the eventSubProcess should be set to scope

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task1", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeTaskInsideNonInterruptingEventSubProcessInsideSubProcessTask2ShouldStay() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcessTask")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task2", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeStartEventInsideNonInterruptingEventSubProcessInsideSubProcessTask2ShouldStay() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventProcessStart")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task2", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_EVENT_SUBPROCESS_INSIDE_SUBPROCESS)
  public void testStartBeforeNonInterruptingEventSubProcessInsideSubProcessTask2ShouldStay() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("eventSubProcess")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskInstance = getChildInstanceForActivity(updatedTree, "task2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("task2", taskInstance.getActivityId());

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "eventSubProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcess", subProcessInstance.getActivityId());

    ActivityInstance eventSubProcessTaskInstance = getChildInstanceForActivity(subProcessInstance, "eventSubProcessTask");
    assertNotNull(eventSubProcessTaskInstance);
    assertEquals(0, eventSubProcessTaskInstance.getChildActivityInstances().length);
    assertEquals("eventSubProcessTask", eventSubProcessTaskInstance.getActivityId());
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
