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

import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.runtime.PvmExecutionImpl;


/**
 * @author Daniel Meyer
 * @author Roman Smirnov
 *
 */
public class PvmAtomicOperationActivityInitStackConcurrent extends PvmAtomicOperationCreateConcurrentExecution {

  protected void concurrentExecutionCreated(PvmExecutionImpl propagatingExecution) {
    ActivityImpl activity = propagatingExecution.getActivity();
    if (activity.isScope()) {
      propagatingExecution.setActive(false);
      propagatingExecution.setActivity(null);
      propagatingExecution = propagatingExecution.createExecution();
      propagatingExecution.setActivity(activity);
      propagatingExecution.initialize();
    }
    // TODO: otherwise no variable-history-events for process instance variables are thrown
    if (propagatingExecution.getParent().getExecutionStartContext() != null) {
      propagatingExecution.getParent().disposeExecutionStartContext();
    }

    propagatingExecution.performOperation(ACTIVITY_INIT_STACK_NOTIFY_LISTENER_START);

//    propagatingExecution.performOperation(ACTIVITY_INIT_STACK);


  }

  public String getCanonicalName() {
    return "activity-start-stack-concurrent";
  }

}
