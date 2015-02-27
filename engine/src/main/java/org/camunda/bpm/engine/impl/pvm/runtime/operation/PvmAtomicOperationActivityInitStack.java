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
public class PvmAtomicOperationActivityInitStack implements PvmAtomicOperation {

  public String getCanonicalName() {
    return "activity-stack-init";
  }

  protected ExecutionStartContext getExecutionStartContext(PvmExecutionImpl execution) {
    ExecutionStartContext executionStartContext = execution.getExecutionStartContext();
    if (executionStartContext==null) {
      PvmExecutionImpl startContextExecution = getStartContextExecution(execution);
      executionStartContext = startContextExecution.getExecutionStartContext();
    }

    return executionStartContext;
  }

  protected void disposeStartContext(PvmExecutionImpl execution) {
    PvmExecutionImpl parent = execution;
    while (parent != null && parent.getExecutionStartContext() != null) {
      parent.disposeExecutionStartContext();
      parent = parent.getParent();
    }
  }

  public void execute(PvmExecutionImpl execution) {
    ExecutionStartContext executionStartContext = getExecutionStartContext(execution);

    // TODO: where to dispose the start context? not here, because of recursive instantiation but where?

    List<PvmActivity> activityStack = executionStartContext.getActivityStack();
    PvmActivity currentActivity = activityStack.remove(0);

    PvmExecutionImpl propagatingExecution = execution;
    if (currentActivity.isScope()) {
      propagatingExecution = execution.createExecution();
      execution.setActive(false);
      propagatingExecution.setActivity(currentActivity);
      propagatingExecution.initialize();

    }
    else {
      propagatingExecution.setActivity(currentActivity);

    }
    propagatingExecution.performOperation(ACTIVITY_INIT_STACK_NOTIFY_LISTENER_START);


//      executionStartContext.executionStarted(execution);

//    if (activity == activityStack.get(activityStack.size() - 1)) {
//      PvmExecutionImpl startContextExecution = getStartContextExecution(execution);
//      // TODO: this won't dispose a start context on a higher execution
////      startContextExecution.disposeExecutionStartContext();
//      executionStartContext.applyVariables(execution);
////      disposeStartContext(startContextExecution);
//
//      execution.performOperation(ACTIVITY_START_CREATE_SCOPE);
//    } else {
//      int index = activityStack.indexOf(activity);
//      // starting the next one
//      activity = activityStack.get(index+1);
//
//      PvmExecutionImpl propagatingExecution = execution;
//      if (activity.isScope() && activity != activityStack.get(activityStack.size() - 1)) {
//        execution.setActive(false);
//        execution.setActivity(null);
//        propagatingExecution = execution.createExecution();
//        execution.disposeExecutionStartContext();
//
//        propagatingExecution.setActivity(activity);
//        propagatingExecution.initialize();
//
//      } else {
//        // and search for the correct execution to set the Activity to
//        propagatingExecution.setActivity(activity);
//
//      }
//
//
//      if (activity == activityStack.get(activityStack.size() - 1)) {
//        propagatingExecution.performOperation(ACTIVITY_INIT_STACK);
//      } else {
//        propagatingExecution.performOperation(ACTIVITY_INIT_STACK_NOTIFY_LISTENER_START);
//      }
//
//    }
  }

  public boolean isAsync(PvmExecutionImpl instance) {
    return false;
  }

  public PvmExecutionImpl getStartContextExecution(PvmExecutionImpl execution) {
//    PvmExecutionImpl parent = execution;
//    while (parent.getExecutionStartContext() == null) {
//      parent = parent.getParent();
//    }
//    return parent;
    return execution;
  }

}
