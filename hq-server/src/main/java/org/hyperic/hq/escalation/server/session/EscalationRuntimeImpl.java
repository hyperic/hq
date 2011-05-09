/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.escalation.server.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.alert.data.AlertRepository;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.escalation.data.EscalationStateRepository;
import org.hyperic.hq.events.ActionExecutionInfo;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.galert.data.GalertLogRepository;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;



/**
 * This class manages the runtime execution of escalation chains. The
 * persistence part of the escalation engine lives within
 * {@link EscalationManagerImpl}
 * 
 * This class does very little within hanging onto and remembering state data.
 * It only knows about the escalation state ID and the time that it is to wake
 * up and perform the next actions.
 * 
 * The workflow looks something like this:
 * 
 * /- EscalationRunner Runtime ->[ScheduleWatcher] -- EscalationRunner \_
 * EscalationRunner | ->EsclManager.executeState
 * 
 * 
 * The Runtime puts {@link EscalationState}s into the schedule. When the
 * schedule determines the state's time is ready to run, the task is passed off
 * into an EscalationRunner (which comes from a thread pool) and kicked off.
 */
@Component
public class EscalationRuntimeImpl implements EscalationRuntime {

	private final ThreadLocal _batchUnscheduleTxnListeners = new ThreadLocal();

	private final Timer _schedule = new Timer("EscalationRuntime", true);
	private final Map _stateIdsToTasks = new HashMap();
	private final Map _esclEntityIdsToStateIds = new HashMap();

	private final Semaphore _mutex = new Semaphore(1);

	private final Set _uncomittedEscalatingEntities = Collections
			.synchronizedSet(new HashSet());
	private final ThreadPoolExecutor _executor;
	private final EscalationStateRepository escalationStateRepository;
	private AuthzSubjectManager authzSubjectManager;
	private AlertRepository alertRepository;
	private final Log log = LogFactory.getLog(EscalationRuntime.class);
	private GalertLogRepository galertLogRepository;
	private ConcurrentStatsCollector concurrentStatsCollector;
	
	@Autowired
	public EscalationRuntimeImpl(EscalationStateRepository escalationStateRepository,
			AuthzSubjectManager authzSubjectManager, AlertRepository alertRepository,
			GalertLogRepository galertLogRepository, ConcurrentStatsCollector concurrentStatsCollector) {
		this.escalationStateRepository = escalationStateRepository;
		this.authzSubjectManager = authzSubjectManager;
		this.alertRepository = alertRepository;
		this.galertLogRepository = galertLogRepository;
		this.concurrentStatsCollector = concurrentStatsCollector;
		// Want threads to never die (XXX, scottmf, keeping current
		// functionality to get rid of
		// backport apis but don't think this is a good idea)
		// 3 threads to service requests
		_executor = new ThreadPoolExecutor(3, 3, Long.MAX_VALUE, TimeUnit.SECONDS, new LinkedBlockingQueue());
	}
	
	@PostConstruct
	public void initStatsCollection() {
		concurrentStatsCollector.register(ConcurrentStatsCollector.ESCALATION_EXECUTE_STATE_TIME);
	}

	/**
	 * This class is invoked when the clock daemon wakes up and decides that it
	 * is time to look at an escalation.
	 */
	private class ScheduleWatcher extends TimerTask {
		private Integer _stateId;
		private Executor _executor;

		private ScheduleWatcher(Integer stateId, Executor executor) {
			_stateId = stateId;
			_executor = executor;
		}

		public void run() {
			try {
				_executor.execute(new EscalationRunner(_stateId));
			}  catch(Throwable t) {
				log.error(t,t); 
			}
		}
	}

	/**
	 * Unschedule the execution of an escalation state. The unschedule will only
	 * occur if the transaction successfully commits.
	 */
	public void unscheduleEscalation(EscalationState state) {
		final Integer stateId = state.getId();
		TransactionSynchronizationManager
				.registerSynchronization(new TransactionSynchronization() {
					public void suspend() {
					}

					public void resume() {
					}

					public void flush() {
					}

					public void beforeCompletion() {
					}

					public void beforeCommit(boolean readOnly) {
					}

					public void afterCompletion(int status) {
					}

					public void afterCommit() {
						unscheduleEscalation_(stateId);
					}
				});
	}

	/**
	 * Unschedule the execution of all escalation states associated with this
	 * entity that performs escalations. The unschedule will only occur if the
	 * transaction successfully commits.
	 */
	public void unscheduleAllEscalationsFor(PerformsEscalations def) {
		BatchUnscheduleEscalationsTransactionListener batchTxnListener = (BatchUnscheduleEscalationsTransactionListener) _batchUnscheduleTxnListeners
				.get();

		if (batchTxnListener == null) {
			batchTxnListener = new BatchUnscheduleEscalationsTransactionListener();
			_batchUnscheduleTxnListeners.set(batchTxnListener);
			TransactionSynchronizationManager
					.registerSynchronization(batchTxnListener);
		}

		batchTxnListener.unscheduleAllEscalationsFor(def);
	}

	/**
	 * A txn listener that unschedules escalations in batch. This class is not
	 * thread safe. We assume that this txn listener is called back by the same
	 * thread originally unscheduling the escalations.
	 */
	private class BatchUnscheduleEscalationsTransactionListener implements
			TransactionSynchronization {
		private final Set _escalationsToUnschedule;

		public BatchUnscheduleEscalationsTransactionListener() {
			_escalationsToUnschedule = new HashSet();
		}

		public void afterCompletion(int status) {
		}

		public void beforeCompletion() {
		}

		public void flush() {
		}

		public void resume() {
		}

		public void suspend() {
		}

		/**
		 * Unscheduled all escalations associated with this entity.
		 * 
		 * @param def
		 *            The entity that performs escalations.
		 */
		public void unscheduleAllEscalationsFor(PerformsEscalations def) {
			_escalationsToUnschedule.add(new EscalatingEntityIdentifier(def));
		}

		public void afterCommit() {
			try {
				unscheduleAllEscalations_((EscalatingEntityIdentifier[]) _escalationsToUnschedule
						.toArray(new EscalatingEntityIdentifier[_escalationsToUnschedule
								.size()]));

			} finally {
				_batchUnscheduleTxnListeners.set(null);
			}
		}

		public void beforeCommit(boolean readOnly) {
			deleteAllEscalations_((EscalatingEntityIdentifier[]) _escalationsToUnschedule
					.toArray(new EscalatingEntityIdentifier[_escalationsToUnschedule
							.size()]));
		}

	}

	private void deleteAllEscalations_(
			EscalatingEntityIdentifier[] escalatingEntities) {
		List<Integer> stateIds = new ArrayList<Integer>(escalatingEntities.length);

		synchronized (_stateIdsToTasks) {
			for (int i = 0; i < escalatingEntities.length; i++) {
				Integer stateId = (Integer) _esclEntityIdsToStateIds
						.get(escalatingEntities[i]);
				// stateId may be null if an escalation has not been scheduled
				// for this escalating entity.
				if (stateId != null) {
					stateIds.add(stateId);
				}

			}
		}
		if(!stateIds.isEmpty()) {
		    escalationStateRepository.deleteByIds(stateIds);
		}
	}

	private void unscheduleEscalation_(Integer stateId) {
		synchronized (_stateIdsToTasks) {
			doUnscheduleEscalation_(stateId);
			_esclEntityIdsToStateIds.values().remove(stateId);
		}
	}

	private void unscheduleAllEscalations_(
			EscalatingEntityIdentifier[] esclEntityIds) {
		synchronized (_stateIdsToTasks) {
			for (int i = 0; i < esclEntityIds.length; i++) {
				Integer stateId = (Integer) _esclEntityIdsToStateIds
						.remove(esclEntityIds[i]);
				doUnscheduleEscalation_(stateId);
			}
		}
	}

	private void doUnscheduleEscalation_(Integer stateId) {
		if (stateId != null) {
			TimerTask task = (TimerTask) _stateIdsToTasks.remove(stateId);

			if (task != null) {
				task.cancel();
				log.debug("Canceled state[" + stateId + "]");
			} else {
				log.debug("Canceling state[" + stateId + "] but was "
						+ "not found");
			}
		}
	}

	/**
	 * Acquire the mutex.
	 * 
	 * @throws InterruptedException
	 */
	public void acquireMutex() throws InterruptedException {
		_mutex.acquire();
	}

	/**
	 * Release the mutex.
	 */
	public void releaseMutex() {
		_mutex.release();
	}

	/**
	 * Add the uncommitted escalation state for this entity performing
	 * escalations to the uncommitted escalation state cache. This cache is used
	 * to track escalation states that have been scheduled but are not visible
	 * to other threads prior to the transaction commit.
	 * 
	 * @param def
	 *            The entity that performs escalations.
	 * @return <code>true</code> if there is already an uncommitted escalation
	 *         state.
	 */
	public boolean addToUncommittedEscalationStateCache(PerformsEscalations def) {
		return !_uncomittedEscalatingEntities
				.add(new EscalatingEntityIdentifier(def));
	}

	/**
	 * Remove the uncommitted escalation state for this entity performing
	 * escalations from the uncommitted escalation state cache.
	 * 
	 * @param def
	 *            The entity that performs escalations.
	 * @param postTxnCommit
	 *            <code>true</code> to remove post txn commit;
	 *            <code>false</code> to remove immediately.
	 */
	public void removeFromUncommittedEscalationStateCache(
			final PerformsEscalations def, boolean postTxnCommit) {
		if (postTxnCommit) {
			boolean addedTxnListener = false;
			try {
				TransactionSynchronizationManager
						.registerSynchronization(new TransactionSynchronization() {

							public void suspend() {
							}

							public void resume() {
							}

							public void flush() {
							}

							public void beforeCompletion() {
							}

							public void beforeCommit(boolean readOnly) {
							}

							public void afterCompletion(int status) {
							}

							public void afterCommit() {
								removeFromUncommittedEscalationStateCache(def,
										false);
							}
						});

				addedTxnListener = true;
			} finally {
				if (!addedTxnListener) {
					removeFromUncommittedEscalationStateCache(def, false);
				}
			}
		} else {
			_uncomittedEscalatingEntities
					.remove(new EscalatingEntityIdentifier(def));
		}

	}

	/**
	 * This method introduces an escalation state to the runtime. The escalation
	 * will be invoked according to the next action time of the state.
	 * 
	 * If the state had been previously scheduled, it will be rescheduled with
	 * the new time.
	 */
	public void scheduleEscalation(final EscalationState state) {
		final long schedTime = state.getNextActionTime();
		TransactionSynchronizationManager
				.registerSynchronization(new TransactionSynchronization() {
					public void suspend() {
					}

					public void resume() {
					}

					public void flush() {
					}

					public void beforeCompletion() {
					}

					public void beforeCommit(boolean readOnly) {
					}

					public void afterCompletion(int status) {
					}

					public void afterCommit() {
						scheduleEscalation_(state, schedTime);
					}
				});
	}

	private void scheduleEscalation_(EscalationState state, long schedTime) {
		Integer stateId = state.getId();

		if (stateId == null) {
			throw new IllegalStateException("Cannot schedule a "
					+ "transient escalation state (stateId=null).");
		}

		synchronized (_stateIdsToTasks) {
			   ScheduleWatcher task = (ScheduleWatcher) _stateIdsToTasks.get(stateId);

			if (task != null) {
				// Previously scheduled. Unschedule
				task.cancel();
				log.debug("Rescheduling state[" + stateId + "]");
			} else {
				log.debug("Scheduling state[" + stateId + "]");
			}

			 task = new ScheduleWatcher(stateId, _executor);
			 _schedule.schedule(task, new Date(schedTime));

			_stateIdsToTasks.put(stateId, task);
			_esclEntityIdsToStateIds.put(new EscalatingEntityIdentifier(state),
					stateId);
		}
	}

	private boolean escIsValid(EscalationState s) {
		final boolean debug = log.isDebugEnabled();
		EscalationAlertType alertType = s.getAlertType();
		AlertInterface alert = null;
		// HHQ-3499, need to make sure that the alertId that is pointed to by
		// the escalation still exists
		if (alertType instanceof GalertEscalationAlertType) {
			alert = galertLogRepository.findOne(new Integer(s.getAlertId()));
		} else if (alertType instanceof ClassicEscalationAlertType) {
			alert = alertRepository.findOne(new Integer(s.getAlertId()));
		}
		if (alert == null) {
			if (debug)
				log.debug("Alert with id[" + s.getAlertId()
						+ " and escalation type ["
						+ s.getAlertType().getClass().getName()
						+ "] was not found.");
			return false;
		}
		AlertDefinitionInterface def = alert.getAlertDefinitionInterface();
		if (def == null) {
			if (debug)
				log.debug("AlertDef from alertid=" + s.getAlertId()
						+ " was not found.");
			endEscalation(s);
			return false;
		}
		Integer r = def.getResource();
		if (r == null) {
			if (debug)
				log.debug("Resource from alertid=" + s.getAlertId()
						+ " was not found.");
			endEscalation(s);
			return false;
		}
		return true;
	}

	/**
	 * Check if the escalation state or its associated escalating entity has
	 * been deleted.
	 * 
	 * @param s
	 *            The escalation state.
	 * @return <code>true</code> if the escalation state or escalating entity
	 *         has been deleted.
	 */
	@Transactional(readOnly = true)
	private boolean hasEscalationStateOrEscalatingEntityBeenDeleted(
			EscalationState escalationState) {
		if (escalationState == null) {
			return true;
		}

		try {
			PerformsEscalations alertDefinition = escalationState
					.getAlertType()
					.findDefinition(
							new Integer(escalationState.getAlertDefinitionId()));

			// galert defs may be deleted from the DB when the group is deleted,
			// so we may get a null value.
			return alertDefinition == null || alertDefinition.isDeleted();
		} catch (Throwable e) {
			return true;
		}
	}

	@Transactional
	public void endEscalation(EscalationState escalationState) {
		if (escalationState != null) {
			// make sure we have the updated state to avoid StaleStateExceptions
			escalationState = escalationStateRepository.findOne(escalationState.getId());
			if (escalationState == null) {
				return;
			}
			escalationStateRepository.delete(escalationState);
			unscheduleEscalation(escalationState);
		}
	}

	@Transactional
	public void executeState(Integer stateId) {
		// Use a get() so that the state is retrieved from the
		// database (in case the escalation state was deleted
		// in a separate session when ending an escalation).
		// The get() will return null if the escalation state
		// does not exist.
		EscalationState escalationState = escalationStateRepository.findOne(stateId);

		if (hasEscalationStateOrEscalatingEntityBeenDeleted(escalationState)) {
			// just to be safe
			endEscalation(escalationState);

			return;
		}

		Escalation escalation = escalationState.getEscalation();
		int actionIdx = escalationState.getNextAction();

		// XXX -- Need to make sure the application is running before
		// we allow this to proceed
		final boolean debug = log.isDebugEnabled();
		if (debug)
			log.debug("Executing state[" + escalationState.getId() + "]");

		if (actionIdx >= escalation.getActions().size()) {
			if (escalation.isRepeat() && escalation.getActions().size() > 0) {
				actionIdx = 0; // Loop back
			} else {
				if (debug) {
					log.debug("Reached the end of the escalation state["
							+ escalationState.getId() + "].  Ending it");
				}
				endEscalation(escalationState);

				return;
			}
		}

		EscalationAction escalationAction = (EscalationAction) escalation
				.getActions().get(actionIdx);
		Action action = escalationAction.getAction();
		// TODO this needs to be looked at further. Ideally, I should be able to
		// call getEscalatables and do a simple null check or catch an expected
		// HQ exception and not worry about checking for alert types explicitly
		// but after talking with folks about it, sounds like it would require
		// touching a lot more plumbing code...
		if (!escIsValid(escalationState)) {
			if (debug)
				log.debug("alert cannot be escalated, since it is not valid.");
			endEscalation(escalationState);
			return;
		}
		Escalatable esc = getEscalatable(escalationState);

		// HQ-1348: End escalation if alert is already fixed
		if (esc.getAlertInfo().isFixed()) {
			endEscalation(escalationState);

			return;
		}

		// Always make sure that we increase the state offset of the
		// escalation so we don't loop fo-eva
		Random random = new Random();
		long offset = 65000 + random.nextInt(25000);
		long nextTime = System.currentTimeMillis()
				+ Math.max(offset, escalationAction.getWaitTime());

		if (debug) {
			log.debug("Moving onto next state of escalation, but waiting for "
					+ escalationAction.getWaitTime() + " ms");
		}

		escalationState.setNextAction(actionIdx + 1);
		escalationState.setNextActionTime(nextTime);
		escalationState.setAcknowledgedBy(null);
		scheduleEscalation(escalationState);

		try {
			EscalationAlertType type = escalationState.getAlertType();
			// HHQ-3784 to avoid deadlocks use the this table order when
			// updating/inserting:
			// 1) EAM_ESCALATION_STATE, 2) EAM_ALERT, 3) EAM_ALERT_ACTION_LOG
			AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
			ActionExecutionInfo execInfo = new ActionExecutionInfo(esc
					.getShortReason(), esc.getLongReason(), esc.getAuxLogs());
			String detail = action.executeAction(esc.getAlertInfo(), execInfo);

			type.changeAlertState(esc, overlord,
					EscalationStateChange.ESCALATED);
			type.logActionDetails(esc, action, detail, null);
		} catch (Exception e) {
			log.error("Unable to execute action [" + action.getClassName()
					+ "] for escalation definition ["
					+ escalationState.getEscalation().getName() + "]", e);
		}
	}

	@Transactional(readOnly = true)
	public Escalatable getEscalatable(EscalationState escalationState) {
		return escalationState.getAlertType().findEscalatable(
				new Integer(escalationState.getAlertId()));
	}

}
