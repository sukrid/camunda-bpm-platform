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

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionStartContext;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;

/**
 * @author Thorben Lindhauer
 *
 */
public class PvmAtomicOperationActivityStartStack extends PvmAtomicOperationActivityInstanceStart {

  public String getCanonicalName() {
    return "activity-stack-start";
  }

  protected ScopeImpl getScope(PvmExecutionImpl execution) {
    return execution.getActivity();
  }

  protected String getEventName() {
    return ExecutionListener.EVENTNAME_START;
  }

  protected void eventNotificationsCompleted(PvmExecutionImpl execution) {
    super.eventNotificationsCompleted(execution);

    ExecutionStartContext executionStartContext = execution.getExecutionStartContext();
    if (executionStartContext==null) {
      // The ProcessInstanceStartContext is set on the process instance / parent execution - grab it from there:
      PvmExecutionImpl executionToUse = execution;
      while (executionStartContext==null) {
        executionToUse = execution.getParent();
        executionStartContext = executionToUse.getExecutionStartContext();
      }
    }

    // TODO: where to dispose the start context? not here, because of recursive instantiation but where?

    PvmActivity activity = execution.getActivity();
    List<PvmActivity> activityStack = executionStartContext.getActivityStack();
    if (activity == activityStack.get(0)) {

      executionStartContext.executionStarted(execution);

      // TODO: this won't dispose a start context on a higher execution
      execution.disposeProcessInstanceStartContext();

      // TODO: this does not respect asyncBefore; it's only treated in transition_create_scope
      // but we can't call it here
      execution.performOperation(ACTIVITY_EXECUTE);

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
      executionToUse.performOperation(ACTIVITY_START_STACK);
    }
  }

}
