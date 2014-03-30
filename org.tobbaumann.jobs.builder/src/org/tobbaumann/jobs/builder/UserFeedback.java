/*******************************************************************************
 * Copyright (c) 2014 tobbaumann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     tobbaumann - initial API and implementation
 ******************************************************************************/
package org.tobbaumann.jobs.builder;

import org.eclipse.core.runtime.IStatus;

class UserFeedback {

  UserFeedbackRunnable runnable;
  boolean performFeedbackImmediately;

  UserFeedback(UserFeedbackRunnable runnable, boolean performFeedbackImmedtiately) {
    this.runnable = runnable;
    this.performFeedbackImmediately = performFeedbackImmedtiately;
  }

  void performUserFeedback(IStatus jobResult, boolean immediateFeedback) {
    runnable.performUserFeedback(jobResult, immediateFeedback);
  }
}
