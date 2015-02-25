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
package org.camunda.bpm.engine.test.util;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.Assert;

/**
 * @author Thorben Lindhauer
 *
 */
public class ExecutionTreeAssertion {

  protected String expectedActivityId;
  protected List<ExecutionTreeAssertion> childAssertions = new ArrayList<ExecutionTreeAssertion>();

  public void addChildAssertion(ExecutionTreeAssertion childAssertion) {
    this.childAssertions.add(childAssertion);
  }

  public void setExpectedActivityId(String expectedActivityId) {
    this.expectedActivityId = expectedActivityId;
  }

  /**
   * This assumes that all children have been fetched
   */
  protected boolean matches(ExecutionEntity execution) {
    String actualActivityId = execution.getActivityId();
    if (expectedActivityId == null && actualActivityId != null) {
      return false;
    } else if (expectedActivityId != null && !expectedActivityId.equals(execution.getActivityId())) {
      return false;
    }

    if (execution.getExecutions().size() != childAssertions.size()) {
      return false;
    }

    List<ExecutionTreeAssertion> unmatchedChildAssertions = new ArrayList<ExecutionTreeAssertion>(childAssertions);
    for (ExecutionEntity child : execution.getExecutions()) {
      for (ExecutionTreeAssertion childAssertion : unmatchedChildAssertions) {
        if (childAssertion.matches(child)) {
          unmatchedChildAssertions.remove(childAssertion);
          break;
        }
      }
    }

    if (!unmatchedChildAssertions.isEmpty()) {
      return false;
    }

    return true;
  }

  public void assertExecution(ExecutionEntity execution) {
    boolean matches = matches(execution);
    if (!matches) {
      StringBuilder errorBuilder = new StringBuilder();
      errorBuilder.append("Expected tree: \n");
      describe(this, "", errorBuilder);
      errorBuilder.append("Actual tree: \n");
      describe(execution, "", errorBuilder);
      Assert.fail(errorBuilder.toString());
    }
  }

  public static void describe(ExecutionEntity execution, String prefix, StringBuilder errorBuilder) {
    errorBuilder.append(prefix);
    errorBuilder.append(execution);
    errorBuilder.append("\n");
    for (ExecutionEntity child : execution.getExecutions()) {
      describe(child, prefix + "  ", errorBuilder);
    }
  }

  public static void describe(ExecutionTreeAssertion assertion, String prefix, StringBuilder errorBuilder) {
    errorBuilder.append(prefix);
    errorBuilder.append(assertion);
    errorBuilder.append("\n");
    for (ExecutionTreeAssertion child : assertion.childAssertions) {
      describe(child, prefix + "  ", errorBuilder);
    }
  }

  public String toString() {
    return "[activityId=" + expectedActivityId + "]";
  }

}
