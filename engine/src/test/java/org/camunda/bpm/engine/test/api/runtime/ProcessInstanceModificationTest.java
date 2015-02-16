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
 * @author Thorben Lindhauer
 *
 */
public class ProcessInstanceModificationTest extends PluggableProcessEngineTestCase {

  protected static final String PARALLEL_GATEWAY_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.parallelGateway.bpmn20.xml";
  protected static final String EXCLUSIVE_GATEWAY_PROCESS = "org/camunda/bpm/engine/test/api/runtime/ProcessInstanceModificationTest.exclusiveGateway.bpmn20.xml";

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
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

    runtimeService
      .createProcessInstanceModification(processInstance.getId())
      .startBeforeActivity("task2")
      .execute();

    ActivityInstance updatedTree = runtimeService.getActivityInstance(processInstance.getId());
    assertNotNull(updatedTree);
    assertEquals(processInstance.getProcessDefinitionId(), updatedTree.getActivityId());

    assertEquals(2, updatedTree.getChildActivityInstances().length);

    ActivityInstance task1Instance = getInstanceForActivity(updatedTree, "task1");
    assertNotNull(task1Instance);
    assertEquals(0, task1Instance.getChildActivityInstances().length);
    assertEquals("task1", task1Instance.getActivityId());
    assertEquals(updatedTree.getId(), task1Instance.getParentActivityInstanceId());

    ActivityInstance task2Instance = getInstanceForActivity(updatedTree, "task2");
    assertNotNull(task2Instance);
    assertEquals(0, task2Instance.getChildActivityInstances().length);
    assertEquals("task2", task2Instance.getActivityId());
    assertEquals(updatedTree.getId(), task2Instance.getParentActivityInstanceId());
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCreationWithVariables() {
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

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

    ActivityInstance task2Instance = getInstanceForActivity(updatedTree, "task2");
    assertNotNull(task2Instance);
    assertEquals(1, task2Instance.getExecutionIds().length);
    String task2ExecutionId = task2Instance.getExecutionIds()[0];

    assertEquals("procInstValue", runtimeService.getVariableLocal(processInstance.getId(), "procInstVar"));
    assertEquals("localValue", runtimeService.getVariableLocal(task2ExecutionId, "localVar"));
  }

  @Deployment(resources = EXCLUSIVE_GATEWAY_PROCESS)
  public void testCancellationAndCreation() {
    // TODO: The problem is here that the process instance execution gets deleted
    // from the database although it should; this is due to tree compactation

    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("parallelGateway");

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
    ActivityInstance instance = getInstanceForActivity(activityInstance, activityId);
    if (instance != null) {
      return instance.getId();
    }
    return null;
  }

  public ActivityInstance getInstanceForActivity(ActivityInstance activityInstance, String activityId) {
    for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
      if (childInstance.getActivityId().equals(activityId)) {
        return childInstance;
      }
    }

    return null;
  }
}
