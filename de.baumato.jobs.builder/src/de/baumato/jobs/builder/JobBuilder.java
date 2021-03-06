/*******************************************************************************
 * Copyright (c) 2014 Tobias Baumann.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tobias Baumann - initial API and implementation
 ******************************************************************************/
/**
 *
 */
package de.baumato.jobs.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Builder to create and schedule jobs. For details on jobs, see
 * http://www.eclipse.org/articles/Article-Concurrency/jobs-api.html
 */
public class JobBuilder {

  private static final String DEFAULT_TITLE = "Operation in progress...";

  public enum JobKind {
    DEFAULT, USER, SYSTEM;
  }

  String title = DEFAULT_TITLE;
  Object family = null;
  IRunnableWithProgress progressRunnable = null;
  JobKind kind = JobKind.DEFAULT;
  Integer priority = null;
  ImageDescriptor image = null;
  String jobCompletionTitle = null;
  UserFeedback userFeedback = null;
  IJobChangeListener listener = null;
  ISchedulingRule schedulingRule = null;

  /** package private constructor */
  JobBuilder() {}

  /**
   * Sets the title of the job, that is e.g. shown in the progress view.
   *
   * @param title the job title, not null
   * @return this
   */
  public JobBuilder title(String title) {
    checkNotNull(title, "Given title is null");
    checkArgument(title.trim().length() > 0, "Given title is empty");
    this.title = title;
    return this;
  }

  /**
   * This given image is used to associate an <code>ImageDescriptor</code> with the Job. If the Job
   * is shown in the UI, this descriptor is used to create an icon that represents the Job. (e.g. in
   * the progress view).
   *
   * @param image
   * @return this
   */
  public JobBuilder image(ImageDescriptor image) {
    this.image = image;
    return this;
  }

  /**
   * Defines that the job is a system job. System jobs, by default, do not appear in the Progress
   * view (unless the view is in verbose mode) and do not animate the status line. If neither
   * isSystemJob nor isUserJob is called, then the job is a default job. A default job will show UI
   * affordance.
   *
   * @see #isUserJob()
   * @see #isDefaultJob()
   * @return this
   */
  public JobBuilder isSystemJob() {
    kind = JobKind.SYSTEM;
    return this;
  }

  /**
   * Defines that the job to create is a user job. User jobs and default jobs will show UI
   * affordances when running. In addition, a user job will show a progress dialog to the user with
   * the option to be run in the background. If neither isSystemJob nor isUserJob is called, then
   * the job is a default job.
   *
   * @see #isSystemJob()
   * @see #isDefaultJob()
   * @return this
   */
  public JobBuilder isUserJob() {
    kind = JobKind.USER;
    return this;
  }

  /**
   * Defines that the job to create is a default job (not a user job and not a system job). A
   * default job will show UI affordances when running. But in comparision to a user job no progress
   * dialog is shown to the user.
   *
   * @see #isUserJob()
   * @see #isSystemJob()
   * @return this
   */
  public JobBuilder isDefaultJob() {
    kind = JobKind.DEFAULT;
    return this;
  }

  /**
   * Takes a runnable to be processed in the job. If an exception occurs the exception is wrapped
   * and the job returns an error {@code IStatus}. If the job has a UI representation the progress
   * is unknown.
   *
   * @param runnable the runnable
   * @return this
   */
  public JobBuilder runnable(Runnable runnable) {
    this.progressRunnable =
        new RunnableAdapter(title, checkNotNull(runnable, "Given runnable is null."));
    return this;
  }

  /**
   * Takes a runnable with progress to monitor the job.
   *
   * @param runnable
   * @return this
   */
  public JobBuilder runnable(IRunnableWithProgress runnable) {
    this.progressRunnable = checkNotNull(runnable, "Given runnable is null.");
    return this;
  }

  /**
   * This does the same as {@link #userFeedback(String, UserFeedbackRunnable)} but with a
   * default job completion title.
   *
   * @param userFeedback the user feedback runnable to run (in UI thread)
   * @return this
   */
  public JobBuilder userFeedback(UserFeedbackRunnable userFeedback) {
    return userFeedback(null, userFeedback);
  }

  /**
   * <p>
   * If the user does not choose to run the job in the background, then they will know when the job
   * has completed because the progress dialog will close (and this feedback gets executed).
   * However, if they choose to run the job in the background (by using the dialog button or the
   * preference), they will not know when the job has completed.
   * <p>
   * If this method is used and the progress dialog is not modal, it causes the job to remain in the
   * progress view. A hyperlink with the given title is created and when the user clicks on it, the
   * given
   * <tt>UserFeedbackRunnable<tt> gets executed in the UI thread to show the results of the finished job.
   * This allows to not interrupt the user because the job results are not displayed immediately.
   * For an immediate feedback use {@link #immediateUserFeedback(String, UserFeedbackRunnable)}
   *
   * @param jobCompletionTitle, may be null or empty to use the default text
   * @param userFeedback the runnable to run (in UI thread)
   * @return this
   */
  public JobBuilder userFeedback(String jobCompletionTitle, UserFeedbackRunnable userFeedback) {
    this.jobCompletionTitle = jobCompletionTitle;
    this.userFeedback =
        new UserFeedback(checkNotNull(userFeedback, "The given user feedback runnable is null."),
            false);
    return this;
  }

  /**
   * This method does the same as {@link #immediateUserFeedback(String, UserFeedbackRunnable)}
   * but a default job completion title.
   *
   * @param userFeedback the runnable to run (in UI thread)
   * @return this
   */
  public JobBuilder immediateUserFeedback(UserFeedbackRunnable userFeedback) {
    return immediateUserFeedback(null, userFeedback);
  }

  /**
   * The given feedback gets given to the user as soon as the job is finished. It is not taken into
   * account whether the job runs in the background. The feedback runs in the UI thread.
   *
   * @see #userFeedback(String, UserFeedbackRunnable)
   * @param jobCompletionTitle, may be null or empty to use the default text
   * @param userFeedback the runnable to run
   * @return this
   */
  public JobBuilder immediateUserFeedback(String jobCompletionTitle,
      UserFeedbackRunnable userFeedback) {
    this.jobCompletionTitle = jobCompletionTitle;
    this.userFeedback =
        new UserFeedback(checkNotNull(userFeedback, "The given user feedback runnable is null."),
            true);
    return this;
  }

  /**
   * Sets Job.SHORT as priority which gives the job a higher priority than the default Job.LONG.
   *
   * @see #lowPriority()
   * @see #lowestPriority()
   * @return this.
   */
  public JobBuilder highPriority() {
    priority = Job.SHORT;
    return this;
  }

  /**
   * Sets Job.LONG as priority which gives the job a low priority. This is the default.
   *
   * @see #highPriority()
   * @see #lowestPriority()
   * @return this
   */
  public JobBuilder lowPriority() {
    priority = Job.LONG;
    return this;
  }

  /**
   * Sets Job.BUILD as priority. Build jobs run generally after all other background jobs are
   * completed.
   *
   * @see #highPriority()
   * @see #lowPriority()
   * @return this
   */
  public JobBuilder lowestPriority() {
    priority = Job.BUILD;
    return this;
  }

  /**
   * Adds the given listener to the job to be created. Consider to use {@code JobChangeAdapter} for
   * a more compact notation.
   *
   * @param listener the listener to add
   * @return this
   */
  public JobBuilder addJobChangeListener(IJobChangeListener listener) {
    this.listener = listener;
    return this;
  }

  /**
   * <p>
   * Sets a family to the job. If none gets set, then the job title is used as family.
   * <p>
   * The Eclipse platform provides a job manager that applies several job operations to an entire
   * family. These operations include cancel, find, join, sleep, and wakeUp.
   *
   * @param family the family to set
   * @return this
   */
  public JobBuilder family(Object family) {
    this.family = family;
    return this;
  }

  /**
   * <p>
   * Sets a ISchedulingRule to the job.
   * <p>
   * The ISchedulingRule interface allows clients to define locks that can be used to ensure
   * exclusive access to resources when required while preventing deadlock from occurring in the
   * situation where multiple running jobs try to access the same resources. A scheduling rule can
   * be assigned to a job before it is scheduled. This allows the job manager to ensure that the
   * required resources are available before starting the job.
   * <p>
   * Use only one of: {@link #schedulingRule(ISchedulingRule)}, {@link #runsNotConcurrently(String)}.
   *
   * @param rule the scheduling rule to set
   * @return this
   */
  public JobBuilder schedulingRule(ISchedulingRule rule) {
    this.schedulingRule = rule;
    return this;
  }

  /**
   * <p>
   * This sets a special scheduling rule using the jobs title as scheduling rule name ensuring that
   * all jobs scheduled with this schedulingRuleName do not run concurrently but sequentially. The
   * job title should be set beforehand.
   * <p>
   * Use only one of: {@link #schedulingRule(ISchedulingRule)}, {@link #runsNotConcurrently(String)}.
   *
   * @param schedulingRuleName the name scheduling rule
   * @return this
   */
  public JobBuilder runsNotConcurrently() {
    return schedulingRule(new NotConcurrentlyRule(title));
  }

  /**
   * <p>
   * This sets a special scheduling rule {@link #schedulingRule(ISchedulingRule)} using the given
   * name ensuring that all jobs scheduled with this schedulingRuleName do not run concurrently but
   * sequentially.
   * <p>
   * Use only one of: {@link #schedulingRule(ISchedulingRule)}, {@link #runsNotConcurrently(String)}.
   *
   * @param schedulingRuleName the name scheduling rule
   * @return this
   */
  public JobBuilder runsNotConcurrently(String schedulingRuleName) {
    return schedulingRule(new NotConcurrentlyRule(schedulingRuleName));
  }

  /**
   * Builds the job with behaviour set by this builder.
   *
   * @return the built job
   */
  public Job build() {
    checkState(progressRunnable != null, "The job's runnable is not set.");
    return new InternalJob(this);
  }

  /**
   * Builds the job and schedules it. This is useful if you don't want to add job listener before
   * scheduling.
   *
   * @return the job
   */
  public Job buildAndSchedule() {
    Job job = build();
    job.schedule();
    return job;
  }

  /**
   * Builds the job and schedules this job to be run after the specified delay.
   *
   * If this job is currently running, it will be rescheduled with the specified delay as soon as it
   * finishes. If this method is called multiple times while the job is running, the job will still
   * only be rescheduled once, with the most recent delay value that was provided.
   *
   * Scheduling a job that is waiting or sleeping has no effect.
   *
   * @param delay a time delay in given time unit before the job should run
   * @param timeUnit the time unit of the delay
   * @return the job
   */
  public Job buildAndScheduleWithDelay(long delay, TimeUnit timeUnit) {
    Job job = build();
    job.schedule(timeUnit.toMillis(delay));
    return job;
  }
}
