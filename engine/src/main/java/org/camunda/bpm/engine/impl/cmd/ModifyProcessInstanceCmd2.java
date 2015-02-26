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
package org.camunda.bpm.engine.impl.cmd;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.ActivityExecutionMapping;
import org.camunda.bpm.engine.impl.ActivityInstantiationInstruction;
import org.camunda.bpm.engine.impl.ProcessInstanceModificationBuilderImpl;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.util.EnsureUtil;

/**
 * @author Thorben Lindhauer
 *
 */
public class ModifyProcessInstanceCmd2 implements Command<Void> {

  protected ProcessInstanceModificationBuilderImpl builder;

  public ModifyProcessInstanceCmd2(ProcessInstanceModificationBuilderImpl processInstanceModificationBuilder) {
    this.builder = processInstanceModificationBuilder;
  }

  public Void execute(CommandContext commandContext) {
    String processInstanceId = builder.getProcessInstanceId();
    ExecutionEntity processInstance = commandContext.getExecutionManager().findExecutionById(processInstanceId);

    ProcessDefinitionImpl processDefinition = processInstance.getProcessDefinition();

    for (ActivityInstantiationInstruction startInstruction : builder.getActivitiesToStartBefore()) {
      ActivityImpl activity = processDefinition.findActivity(startInstruction.getActivityId());

      EnsureUtil.ensureNotNull("activity", activity);

      ActivityExecutionMapping mapping = new ActivityExecutionMapping(commandContext, processInstanceId);

      List<PvmActivity> activitiesToInstantiate = new ArrayList<PvmActivity>();
      activitiesToInstantiate.add(activity);

      ScopeImpl parentScope = activity.getParentScope();
      ScopeImpl scope = activity.getScope();

      Set<ExecutionEntity> parentActivityExecutions = mapping.getExecutions(parentScope);
      while (parentActivityExecutions.isEmpty()) {
        ActivityImpl parentActivity = (ActivityImpl) parentScope;
        if (parentScope == scope) {
          activitiesToInstantiate.add(parentActivity);

        }
        scope = parentActivity.getScope();
        parentScope = parentActivity.getParentScope();
        parentActivityExecutions = mapping.getExecutions(parentScope);
      }

      if (parentActivityExecutions.size() > 1) {
        throw new ProcessEngineException("Cannot yet deal with cases in which there is more than 1 execution");
      }

      Collections.reverse(activitiesToInstantiate);
      ExecutionEntity scopeExecution = parentActivityExecutions.iterator().next();
      scopeExecution.executeActivities(parentScope, activitiesToInstantiate,
          startInstruction.getVariables(), startInstruction.getVariablesLocal());
    }

    return null;
  }


}
