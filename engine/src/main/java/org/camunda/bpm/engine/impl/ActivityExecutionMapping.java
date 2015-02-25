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
package org.camunda.bpm.engine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.EnsureUtil;

/**
 * @author Thorben Lindhauer
 *
 */
public class ActivityExecutionMapping {

  protected Map<ScopeImpl, List<ExecutionEntity>> activityExecutionMapping;
  protected CommandContext commandContext;
  protected String processInstanceId;

  public ActivityExecutionMapping(CommandContext commandContext, String processInstanceId) {
    this.commandContext = commandContext;
    this.processInstanceId = processInstanceId;
    this.activityExecutionMapping = new HashMap<ScopeImpl, List<ExecutionEntity>>();

    initialize();
  }

  protected void initialize() {
    ExecutionEntity processInstance = commandContext.getExecutionManager().findExecutionById(processInstanceId);

    List<ExecutionEntity> executions = fetchExecutionsForProcessInstance(processInstance);
    executions.add(processInstance);

    List<ExecutionEntity> leaves = findLeaves(executions);

    assignExecutionsToActivities(leaves);
  }

  protected void assignExecutionsToActivities(List<ExecutionEntity> leaves) {
    for (ExecutionEntity leaf : leaves) {
      ActivityImpl activity = leaf.getActivity();
      assignToActivity(leaf, activity);
    }
  }

  protected void assignToActivity(ExecutionEntity execution, ScopeImpl activity) {
    EnsureUtil.ensureNotNull("activityId", activity);
    List<ExecutionEntity> executionsForActivity = activityExecutionMapping.get(activity);
    if (executionsForActivity == null) {
      executionsForActivity = new ArrayList<ExecutionEntity>();
      activityExecutionMapping.put(activity, executionsForActivity);
    }

    executionsForActivity.add(execution);

    ExecutionEntity parent = execution.getParent();
    if (activity.isScope() && !execution.isScope()) {
      executionsForActivity.add(parent);
      parent = parent.getParent();
    }

    // TODO: is this correct?
    assignToActivity(parent, activity.getParentScope());
  }

  protected List<ExecutionEntity> fetchExecutionsForProcessInstance(ExecutionEntity execution) {
    List<ExecutionEntity> executions = new ArrayList<ExecutionEntity>();
    executions.addAll(execution.getExecutions());
    for (ExecutionEntity child : execution.getExecutions()) {
      executions.addAll(fetchExecutionsForProcessInstance(child));
    }

    return executions;
  }

  protected List<ExecutionEntity> findLeaves(List<ExecutionEntity> executions) {
    List<ExecutionEntity> leaves = new ArrayList<ExecutionEntity>();

    for (ExecutionEntity execution : executions) {
      if (execution.getExecutions().isEmpty()) {
        leaves.add(execution);
      }
    }

    return leaves;
  }
}
