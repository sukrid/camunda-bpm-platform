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


import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.ProcessInstanceModificationBuilderImpl;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.deploy.DeploymentCache;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.runtime.ActivityInstance;

/**
 * @author Thorben Lindhauer
 *
 */
public class ModifyProcessInstanceCmd implements Command<Void> {

  protected ProcessInstanceModificationBuilderImpl builder;

  public ModifyProcessInstanceCmd(ProcessInstanceModificationBuilderImpl processInstanceModificationBuilder) {
    this.builder = processInstanceModificationBuilder;
  }

  public Void execute(CommandContext commandContext) {
    String processInstanceId = builder.getProcessInstanceId();

    ensureNotNull("processInstanceId", processInstanceId);
    ActivityInstance processInstanceTree = new GetActivityInstanceCmd(processInstanceId).execute(commandContext);
    ActivityInstanceLookup treeLookup = new ActivityInstanceLookup(processInstanceTree);

    DeploymentCache deploymentCache = Context
        .getProcessEngineConfiguration()
        .getDeploymentCache();
    ProcessDefinitionEntity processDefinition = deploymentCache
        .findDeployedProcessDefinitionById(processInstanceTree.getProcessDefinitionId());

    // 1. collect executions to keep
    Set<String> activityIdsToInstantiate = builder.getActivitiesToStartBefore();

    Map<String, ActivityImpl> activitiesToInstantiate = resolveActivities(processDefinition, activityIdsToInstantiate);
    // TODO: add start after activities here
    Map<String, String> activityAncestorMapping = executionsToKeep(commandContext, treeLookup, activitiesToInstantiate);
    Set<String> ancestorsToKeep = new HashSet<String>(activityAncestorMapping.values());

    // 2. determine top-most executions to remove
    // TODO: the result should never contain the scope execution in which scope we want to
    // start a new activity (e.g. process instance in a one-execution process)
    Set<String> removableExecutions = executionsToDelete(
        commandContext,
        treeLookup,
        builder.getActivityInstancesToCancel(),
        ancestorsToKeep);

    // 3. remove executions
    for (String executionId : removableExecutions) {
      ExecutionEntity execution = commandContext.getDbEntityManager().getCachedEntity(ExecutionEntity.class, executionId);
      // TODO: what would be an appriopriate deletion reason? Should we also add there the activity instance
      // id because of which this execution had to die?
      execution.deleteCascade("activity cancellation", true);
      // TODO: additional execution.remove() required?
    }

    // 4. start new
    instantiate(commandContext, activitiesToInstantiate, activityAncestorMapping);

    return null;
  }

  protected Map<String, ActivityImpl> resolveActivities(ProcessDefinitionEntity processDefinition, Set<String> activityIds) {
    Map<String, ActivityImpl> activities = new HashMap<String, ActivityImpl>();

    for (String activityId : activityIds) {
      ActivityImpl activity = processDefinition.findActivity(activityId);
      ensureNotNull("Cannot find activity " + activityId + " in process definition " + processDefinition.getId(),
          "activityId", activity);
      activities.put(activityId, activity);
    }

    return activities;
  }

  protected void instantiate(CommandContext commandContext, Map<String, ActivityImpl> activitiesToInstantiate,
      Map<String, String> ancestorMapping) {

    for (String activityId : activitiesToInstantiate.keySet()) {
      ActivityImpl activity = activitiesToInstantiate.get(activityId);
      String ancestorExecutionId = ancestorMapping.get(activityId);

      ExecutionEntity ancestor = commandContext.getDbEntityManager().getCachedEntity(ExecutionEntity.class, ancestorExecutionId);

      // bottom up stack of activities to activity of ancestor execution
      List<ActivityImpl> activityStackToInstantiate = getActivitiesToInstantiate(ancestor, activity);

      // iterate in top down fashion and remove those list elements for which executions already exist
      for (int i = activityStackToInstantiate.size() - 1; i >= 0; i--) {
        ExecutionEntity childForActivity = getChildExecutionForActivity(ancestor, activityStackToInstantiate.get(i));

        if (childForActivity != null) {
          ancestor = childForActivity;
          activityStackToInstantiate.remove(i);
        } else {
          break;
        }
      }

      // TODO: this is quite a hack
      List<PvmActivity> pvmActivities = new ArrayList<PvmActivity>(activityStackToInstantiate);
      Collections.reverse(pvmActivities);

      ancestor.executeActivities(pvmActivities);
//      ExecutionEntity lowestParentExecution = findLowestParentExecution(ancestor, activity);

      // TODO: start the activity from the lowest parent
    }
  }

  protected ExecutionEntity getChildExecutionForActivity(ExecutionEntity execution, ActivityImpl activity) {
    for (ExecutionEntity child : execution.getExecutions()) {
      if (child.getActivity() == null) {
        ExecutionEntity childExecution = getChildExecutionForActivity(child, activity);
        if (childExecution != null) {
          return childExecution;
        }
      }

      if (child.getActivity() == activity) {
        return child;
      }
    }

    return null;
  }

  protected List<ActivityImpl> getActivitiesToInstantiate(ExecutionEntity ancestorExecution, ActivityImpl activity) {
    // ancestorExecution.getActivity() can be null if it is the naked process instance (in which all other executions have been removed)
//    ensureNotNull("ancestorActivity", ancestorExecution.getActivity());

    List<ActivityImpl> bottomUpScopes = new ArrayList<ActivityImpl>();

    ActivityImpl currentScopeActivity = activity;
    while (currentScopeActivity != null && currentScopeActivity != ancestorExecution.getActivity()) {
      bottomUpScopes.add(currentScopeActivity);
      currentScopeActivity = currentScopeActivity.getParentScopeActivity();
    }

    return bottomUpScopes;
  }

  /**
   * maps activity id -> ancestor execution id
   */
  protected Map<String, String> executionsToKeep(CommandContext commandContext,
      ActivityInstanceLookup activityInstanceTree, Map<String, ActivityImpl> activityInstantiations) {
    Map<String, String> executionsToKeep = new HashMap<String, String>();

    for (String activityId : activityInstantiations.keySet()) {
      ActivityImpl activity = activityInstantiations.get(activityId);
      ActivityInstance parentActivityInstance = findAncestorActivityInstance(activityInstanceTree, activity);
      String rootExecution = getRootExecution(commandContext, parentActivityInstance);
      executionsToKeep.put(activityId, rootExecution);
    }

    return executionsToKeep;
  }

  protected String getRootExecution(CommandContext commandContext, ActivityInstance activityInstance) {
    String[] executionIds = activityInstance.getExecutionIds();

    if (executionIds.length == 1) {
      return executionIds[0];
    }

    Set<String> executionIdSet = new HashSet<String>();
    Collections.addAll(executionIdSet, executionIds);

    for (String executionId : executionIds) {
      ExecutionEntity execution = commandContext
          .getDbEntityManager()
          .getCachedEntity(ExecutionEntity.class, executionId);

      if (!executionIdSet.contains(execution.getParentId())) {
        return executionId;
      }
    }

    throw new ProcessEngineException("Could not determine parent execution; tree may be invalid");
  }

  protected ActivityInstance findAncestorActivityInstance(ActivityInstanceLookup tree, ActivityImpl activity) {
    ActivityImpl parentScope = activity.getParentScopeActivity();


    while (parentScope != null) {
      Set<ActivityInstance> parentScopeInstances = tree.getInstancesForActivity(parentScope.getId());

      if (parentScopeInstances.isEmpty()) {
        parentScope = parentScope.getParentScopeActivity();
      } else if (parentScopeInstances.size() == 1) {
        return parentScopeInstances.iterator().next();
      } else {
        throw new ProcessEngineException("Ambiguous parent activity instance for activity " + activity.getActivityId());
      }
    }

    // if none found, the root is the parent
    return tree.getRootInstance();
  }

  // TODO: break this into multiple parts/methods?
  protected Set<String> executionsToDelete(CommandContext commandContext, ActivityInstanceLookup treeLookup,
      Set<String> activityInstanceCancellations, Set<String> executionsToKeep) {
    Set<String> executionsToDelete = new HashSet<String>();

    for (String instanceToCancel : activityInstanceCancellations) {
      ActivityInstance instance = treeLookup.getInstance(instanceToCancel);
      String currentExecutionId = getRootExecution(commandContext, instance);

      ExecutionEntity topMostDeletableExecution = commandContext
          .getDbEntityManager()
          .getCachedEntity(ExecutionEntity.class, currentExecutionId);

      boolean canRemoveParent = true;

      // we can also remove the parent if there are no siblings that should not be removed
      // TODO: improve the code complexity of this loop
      while (canRemoveParent) {
        ExecutionEntity parentExecution = topMostDeletableExecution.getParent();

        if (parentExecution == null || executionsToKeep.contains(parentExecution.getId())) {
          break;
        }

        List<ExecutionEntity> siblings = parentExecution.getExecutions();

        for (ExecutionEntity sibling : siblings) {
          if (sibling != topMostDeletableExecution && !executionsToDelete.contains(sibling.getId())) {
            canRemoveParent = false;
            break;
          }
        }

        if (canRemoveParent) {
          topMostDeletableExecution = parentExecution;

          // remove sibling IDs from set of deletions because all siblings are contained
          for (ExecutionEntity sibling : siblings) {
            executionsToDelete.remove(sibling.getId());
          }
        }
      }

      executionsToDelete.add(topMostDeletableExecution.getId());
    }

    return executionsToDelete;
  }

  /**
   * An index over activity instances by their id and their activity id
   *
   * @author Thorben Lindhauer
   */
  public class ActivityInstanceLookup {
    protected ActivityInstance activityInstance;
    protected Map<String, Set<ActivityInstance>> instancesForActivity = new HashMap<String, Set<ActivityInstance>>();
    protected Map<String, ActivityInstance> instancesById = new HashMap<String, ActivityInstance>();

    public ActivityInstanceLookup(ActivityInstance activityInstance) {
      this.activityInstance = activityInstance;
      initializeLookup(activityInstance);
    }

    protected void initializeLookup(ActivityInstance activityInstance) {
      addInstanceForActivity(activityInstance.getActivityId(), activityInstance);
      instancesById.put(activityInstance.getId(), activityInstance);

      for (ActivityInstance child : activityInstance.getChildActivityInstances()) {
        initializeLookup(child);
      }
    }

    protected void addInstanceForActivity(String activityId, ActivityInstance activityInstance) {
      Set<ActivityInstance> activityInstances = instancesForActivity.get(activityId);
      if (activityInstances == null) {
        activityInstances = new HashSet<ActivityInstance>();
        instancesForActivity.put(activityId, activityInstances);
      }
      activityInstances.add(activityInstance);
    }

    public Set<ActivityInstance> getInstancesForActivity(String activityId) {
      return instancesForActivity.get(activityId);
    }

    public ActivityInstance getInstance(String activityInstanceId) {
      return instancesById.get(activityInstanceId);
    }

    public ActivityInstance getRootInstance() {
      return activityInstance;
    }


  }

}
