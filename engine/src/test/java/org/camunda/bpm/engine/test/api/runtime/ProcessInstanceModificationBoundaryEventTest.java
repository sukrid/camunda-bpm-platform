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
public class ProcessInstanceModificationBoundaryEventTest extends PluggableProcessEngineTestCase {

  protected static final String INTERRUPTING_BOUNDARY_EVENT = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEvent.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEvent.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventInsideSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventInsideSubProcess.bpmn20.xml";

  protected static final String INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.interruptingBoundaryEventOnSubProcess.bpmn20.xml";
  protected static final String NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.nonInterruptingBoundaryEventOnSubProcess.bpmn20.xml";

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  public void testTask1AndStartBeforeTaskAfterBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  public void testTask1AndStartBeforeBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  public void testTask2AndStartBeforeTaskAfterBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT)
  public void testTask2AndStartBeforeBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  public void testTask1AndStartBeforeTaskAfterNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  public void testTask1AndStartBeforeNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  public void testTask2AndStartBeforeTaskAfterNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT)
  public void testTask2AndStartBeforeNonInterruptingBoundaryEvent() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("boundaryEvent")
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

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask1AndStartBeforeTaskAfterBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask1", taskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask1AndStartBeforeBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask1AndStartBeforeTaskAfterNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask1", taskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask1AndStartBeforeNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask1");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask1", taskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask2AndStartBeforeTaskAfterBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask2", taskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask2AndStartBeforeBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask2AndStartBeforeTaskAfterNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerTaskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask2", taskInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_INSIDE_SUBPROCESS)
  public void testTask2AndStartBeforeNonInterruptingBoundaryEventInsideSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    String taskId = taskService.createTaskQuery().singleResult().getId();
    taskService.complete(taskId);

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("innerBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(1, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(2, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance innerTaskAfterBoundaryEventInstance = getChildInstanceForActivity(subProcessInstance, "innerTaskAfterBoundaryEvent");
    assertNotNull(innerTaskAfterBoundaryEventInstance);
    assertEquals(0, innerTaskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("innerTaskAfterBoundaryEvent", innerTaskAfterBoundaryEventInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask2");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask2", taskInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  public void testStartBeforeTaskAfterBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask", taskInstance.getActivityId());

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  public void testStartBeforeBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  public void testStartBeforeTaskAfterNonInterruptingBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask", taskInstance.getActivityId());

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
  }

  @Deployment(resources = NON_INTERRUPTING_BOUNDARY_EVENT_ON_SUBPROCESS)
  public void testStartBeforeNonInterruptingBoundaryEventOnSubProcess() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("process");
    String processInstanceId = processInstance.getId();

    runtimeService
      .createProcessInstanceModification(processInstanceId)
      .startBeforeActivity("taskAfterBoundaryEvent")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstanceId);
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());
    assertEquals(processInstanceId, updatedTree.getProcessInstanceId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance subProcessInstance = getChildInstanceForActivity(updatedTree, "subProcess");
    assertNotNull(subProcessInstance);
    assertEquals(1, subProcessInstance.getChildActivityInstances().length);
    assertEquals("subProcess", subProcessInstance.getActivityId());

    ActivityInstance taskInstance = getChildInstanceForActivity(subProcessInstance, "innerTask");
    assertNotNull(taskInstance);
    assertEquals(0, taskInstance.getChildActivityInstances().length);
    assertEquals("innerTask", taskInstance.getActivityId());

    ActivityInstance taskAfterBoundaryEventInstance = getChildInstanceForActivity(updatedTree, "taskAfterBoundaryEvent");
    assertNotNull(taskAfterBoundaryEventInstance);
    assertEquals(0, taskAfterBoundaryEventInstance.getChildActivityInstances().length);
    assertEquals("taskAfterBoundaryEvent", taskAfterBoundaryEventInstance.getActivityId());
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
