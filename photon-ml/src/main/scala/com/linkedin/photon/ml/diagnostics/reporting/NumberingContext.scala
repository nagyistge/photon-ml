/*
 * Copyright 2016 LinkedIn Corp. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linkedin.photon.ml.diagnostics.reporting

/**
 * Track a (possibly nested) set of counters. The basic protocol is this:
 *
 * <ul>
 *   <li><tt>nc.enterContext()</tt> -- start a new level of nesting. Initially, we
 *   nest at level 0 (i.e. no counters) so it is an error to call nextItem without having
 *   called enterContext at least one more time than you have called exitContext.</li>
 *   <li><tt>nc.nextItem()</tt> returns a list representing the path to the item on whose behalf nextItem has been
 *   called. To make this more concrete, imagine you are walking a tree depth-first. Each time you enter a node, you
 *   call enterContext, nextItem, and then recurse on the node's children. Imagine you get path (a, b, c, d, e) -- this
 *   represents the path from the root (a) via its child (b) and subsequent descendants (c, d) to finally reach node
 *   e.</li>
 *   <li><tt>nc.exitContext</tt> pops the innermost counter from the stack and resumes counting from where the previous
 *   scope left off.</li>
 * </ul>
 *
 * The intent is to provide a generic way of numbering nested lists, like chapters, in a way that makes sense.
 */
class NumberingContext {
  private[this] var counters:List[Int] = List.empty

  def enterContext():Unit = counters = 0 :: counters
  def exitContext():Unit = counters = counters.tail
  def nextItem():List[Int] = {
    counters = (counters.head + 1) :: counters.tail
    counters.reverse
  }
}
