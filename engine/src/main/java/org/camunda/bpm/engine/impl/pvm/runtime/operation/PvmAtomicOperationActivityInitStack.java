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
package org.camunda.bpm.engine.impl.pvm.runtime.operation;

import java.util.List;

import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionStartContext;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class PvmAtomicOperationActivityInitStack implements PvmAtomicOperation {

  public String getCanonicalName() {
    return "activity-stack-init";
  }

  public void execute(PvmExecutionImpl execution) {

    ExecutionStartContext executionStartContext = execution.getExecutionStartContext();
    if (executionStartContext==null) {
      // The ProcessInstanceStartContext is set on the process instance / parent execution - grab it from there:
      PvmExecutionImpl startContextExecution = getStartContextExecution(execution);
      executionStartContext = startContextExecution.getExecutionStartContext();
    }

    // TODO: where to dispose the start context? not here, because of recursive instantiation but where?

    PvmActivity activity = execution.getActivity();
    List<PvmActivity> activityStack = executionStartContext.getActivityStack();
    if (activity == activityStack.get(activityStack.size() - 1)) {

//      executionStartContext.executionStarted(execution);

      // TODO: this won't dispose a start context on a higher execution
      PvmExecutionImpl startContextExecution = getStartContextExecution(execution);
      startContextExecution.disposeExecutionStartContext();
      executionStartContext.applyVariables(execution);

      // TODO: this does not respect asyncBefore; it's only treated in transition_create_scope
      // but we can't call it here
//      execution.performOperation(ACTIVITY_EXECUTE);

    } else {
      int index = activityStack.indexOf(activity);
      // starting the next one
      activity = activityStack.get(index+1);

      // and search for the correct execution to set the Activity to
      PvmExecutionImpl executionToUse = execution;
      if (executionToUse.getActivity().isScope()) {
        // TODO: is this required?

        executionToUse.setActive(false); // Deactivate since we jump to a node further down the hierarchy
        executionToUse = executionToUse.createExecution();
        executionToUse.initialize();
//        executionToUse = executionToUse.getExecutions().get(0);
      }
      executionToUse.setActivity(activity);
      executionToUse.performOperation(ACTIVITY_INIT_STACK);
    }
  }

  public boolean isAsync(PvmExecutionImpl instance) {
    return false;
  }

  public PvmExecutionImpl getStartContextExecution(PvmExecutionImpl execution) {
    PvmExecutionImpl parent = execution;
    while (parent.getExecutionStartContext() == null) {
      parent = parent.getParent();
    }
    return parent;
  }

}
