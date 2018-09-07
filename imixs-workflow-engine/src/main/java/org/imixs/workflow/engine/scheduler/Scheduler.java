package org.imixs.workflow.engine.scheduler;

import org.imixs.workflow.ItemCollection;

/**
 * This interface is used to implement a Scheduler managed by the
 * SchedulerService.
 * 
 * @see SchedulerService
 * @author rsoika
 * @version 1.0
 */
public interface Scheduler {

	public final static String ITEM_SCHEDULER_NAME = "txtname";
	public final static String ITEM_SCHEDULER_ENABLED = "_scheduler_enabled";
	public final static String ITEM_SCHEDULER_CLASS = "_scheduler_class";
	public final static String ITEM_SCHEDULER_DEFINITION = "_scheduler_definition";

	/**
	 * The run method is called by the ScheduelrService during a timer timeout
	 * event. The SchedulerService provides a configuration object containing
	 * information for the processor of a concrete implementation:
	 * <ul>
	 * <li>type - fixed to value 'scheduler'</li>
	 * <li>_scheduler_definition - the chron/calendar definition for the Java EE
	 * timer service.</li>
	 * <li>_scheduler_enabled - boolean indicates if the scheduler is
	 * enabled/disabled</li>
	 * <li>_scheduler_class - class name of the scheduler implementation</li>
	 * <li>_scheduelr_log - optional log information generated by the scheduler
	 * implementation
	 * </ul>
	 * Beside these standard attributes a scheduler configuration can contain
	 * additional application specific information.
	 * <p>
	 * After the run method is finished the scheduelrService will save the scheduler
	 * configuration if a configuration object is returned. In case of an exception
	 * the Timer service will be canceled. To cancel the timer programmatically, an
	 * implementation must set the item _scheduler_enabled to 'false'.
	 * <p>
	 * To start or stop the timer service the methods start() and stop() from the
	 * SchedulerService can be called.
	 * 
	 * @param scheduler the scheduler configuration
	 * @return updated scheduler configuration
	 */
	public ItemCollection run(ItemCollection job) throws SchedulerException;
}
