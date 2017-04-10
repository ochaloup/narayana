/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc. and/or its affiliates,
 * and individual contributors as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 *
 * (C) 2010,
 * @author JBoss, by Red Hat.
 */
package com.arjuna.mw.wstx.logging;


import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;
import static org.jboss.logging.annotations.Message.Format.MESSAGE_FORMAT;

import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.oasis_open.docs.ws_tx.wscoor._2006._06.CoordinationContextType;

import com.arjuna.mw.wsas.activity.ActivityHierarchy;
import com.arjuna.mw.wst.TxContext;
import com.arjuna.mwlabs.wscf.model.twophase.arjunacore.ATCoordinator;
import com.arjuna.mwlabs.wst.at.remote.ContextManager;
import com.arjuna.wsc11.ContextFactory;
import com.arjuna.wst.Volatile2PCParticipant;

/**
 * i18n log messages for the wstx module.
 *
 * @author Jonathan Halliday (jonathan.halliday@redhat.com) 2010-06
 */
@MessageLogger(projectCode = "ARJUNA")
public interface wstxI18NLogger {

    /*
        Message IDs are unique and non-recyclable.
        Don't change the purpose of existing messages.
          (tweak the message text or params for clarification if you like).
        Allocate new messages by following instructions at the bottom of the file.
     */

    @Message(id = 45001, value = "Error in {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst_client_JaxHCP_1(String arg0, @Cause() Throwable arg1);

	@Message(id = 45002, value = "Error in {0} Unknown context type: {1}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst_client_JaxHCP_2(String arg0, String arg1);

//	@Message(id = 45003, value = "Unknown context type:", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mw_wst_client_JaxHCP_3();

	@Message(id = 45004, value = "WSTX Initialisation: init failed", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mw_wst_deploy_WSTXI_1(@Cause() Throwable arg0);

	@Message(id = 45005, value = "{0} not found.", format = MESSAGE_FORMAT)
	public String get_mw_wst_deploy_WSTXI_21(String arg0);

	@Message(id = 45006, value = "Failed to create document: {0}", format = MESSAGE_FORMAT)
	public String get_mw_wst_deploy_WSTXI_22(String arg0);

	@Message(id = 45007, value = "Missing WSTX Initialisation", format = MESSAGE_FORMAT)
	public String get_mw_wst_deploy_WSTXI_23();

	@Message(id = 45008, value = "Error in {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst_service_JaxHCP_1(String arg0, @Cause() Throwable arg1);

	@Message(id = 45009, value = "Error in {0} Unknown context type: {1}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst_service_JaxHCP_2(String arg0, String arg2);

//	@Message(id = 45010, value = "Unknown context type:", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mw_wst_service_JaxHCP_3();

	@Message(id = 45011, value = "Error in {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst11_client_JaxHC11P_1(String arg0, @Cause() Throwable arg1);

	@Message(id = 45012, value = "Error in {0} Unknown context type: {1}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst11_client_JaxHC11P_2(String arg0, String arg1);

//	@Message(id = 45013, value = "Unknown context type:", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mw_wst11_client_JaxHC11P_3();

	@Message(id = 45014, value = "WSTX11 Initialisation: init failed", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mw_wst11_deploy_WSTXI_1(@Cause() Throwable arg0);

	@Message(id = 45015, value = "{0} not found.", format = MESSAGE_FORMAT)
	public String get_mw_wst11_deploy_WSTXI_21(String arg0);

	@Message(id = 45016, value = "Failed to create document: {0}", format = MESSAGE_FORMAT)
	public String get_mw_wst11_deploy_WSTXI_22(String arg0);

	@Message(id = 45017, value = "Missing WSTX Initialisation", format = MESSAGE_FORMAT)
	public String get_mw_wst11_deploy_WSTXI_23();

	@Message(id = 45018, value = "Error in {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst11_service_JaxHC11P_1(String arg0, @Cause() Throwable arg1);

	@Message(id = 45019, value = "Error in {0} Unknown context type: {1}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mw_wst11_service_JaxHC11P_2(String arg0, String arg1);

//	@Message(id = 45020, value = "Unknown context type:", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mw_wst11_service_JaxHC11P_3();

	@Message(id = 45021, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_Context11FactoryImple_1(String arg0, String arg1);

	@Message(id = 45022, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_Context11FactoryImple_3();

	@Message(id = 45023, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_ContextFactoryImple_1(String arg0, String arg1);

	@Message(id = 45024, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_ContextFactoryImple_3();

	@Message(id = 45025, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_Registrar11Imple_1(String arg0, String arg1);

	@Message(id = 45026, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_RegistrarImple_1(String arg0, String arg1);

	@Message(id = 45027, value = "ignoring context {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_context_ArjunaContextImple_1(String arg0);

	@Message(id = 45028, value = "One context was null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_local_ContextManager_1();

	@Message(id = 45029, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_local_LocalContextFactoryImple_1(String arg0, String arg1);

	@Message(id = 45030, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_local_LocalContextFactoryImple_11();

	@Message(id = 45031, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_local_LocalRegistrarImple_1(String arg0, String arg1);

	@Message(id = 45032, value = "Not implemented!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_local_TransactionManagerImple_1();

	@Message(id = 45033, value = "comms timeout attempting to cancel WS-AT participant {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mwlabs_wst_at_participants_DurableTwoPhaseCommitParticipant_cancel_1(String arg0);

	@Message(id = 45034, value = "comms timeout attempting to commit WS-AT participant {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_participants_DurableTwoPhaseCommitParticipant_confirm_1(String arg0);

	@Message(id = 45035, value = "comms timeout attempting to prepare WS-AT participant {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_at_participants_DurableTwoPhaseCommitParticipant_prepare_1(String arg0);

	@Message(id = 45036, value = "Not implemented!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_remote_Transaction11ManagerImple_1();

	@Message(id = 45037, value = "Not implemented!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_remote_TransactionManagerImple_1();

	@Message(id = 45038, value = "Received context is null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_remote_UserTransaction11Imple__2();

	@Message(id = 45039, value = "Received context is null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_at_remote_UserTransactionImple_2();

	@Message(id = 45040, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_Context11FactoryImple_1(String arg0, String arg1);

	@Message(id = 45041, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_Context11FactoryImple_3();

	@Message(id = 45042, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_ContextFactoryImple_1(String arg0, String arg1);

	@Message(id = 45043, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_ContextFactoryImple_3();

	@Message(id = 45044, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_LocalContextFactoryImple_1(String arg0, String arg1);

	@Message(id = 45045, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_Registrar11Imple_1(String arg0, String arg1);

	@Message(id = 45046, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_RegistrarImple_1(String arg0, String arg1);

	@Message(id = 45047, value = "ignoring context {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_context_ArjunaContextImple_1(String arg0);

	@Message(id = 45048, value = "One context was null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_local_ContextManager_1();

	@Message(id = 45049, value = "Invalid type URI:", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_local_LocalContextFactoryImple_11();

	@Message(id = 45050, value = "Invalid type URI: < {0} , {1} >", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst_ba_local_LocalRegistrarImple_1(String arg0, String arg1);

//	@Message(id = 45051, value = "Invalid address.", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mwlabs_wst_ba_remote_UserBusinessActivityImple_1();

	@Message(id = 45052, value = "Received context is null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_remote_UserBusinessActivityImple_2();

	@Message(id = 45053, value = "No termination context!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst_ba_remote_UserBusinessActivityImple_3();

	@Message(id = 45054, value = "Participant not persistable.", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mwlabs_wst_util_PersistableParticipantHelper_1();

	@Message(id = 45055, value = "Error persisting participant.", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mwlabs_wst_util_PersistableParticipantHelper_2(@Cause() Throwable arg0);

	@Message(id = 45056, value = "Error restoring participant.", format = MESSAGE_FORMAT)
	@LogMessage(level = ERROR)
	public void error_mwlabs_wst_util_PersistableParticipantHelper_3(@Cause() Throwable arg0);

	@Message(id = 45057, value = "ignoring context {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst11_at_context_ArjunaContextImple_1(String arg0);

	@Message(id = 45058, value = "ignoring context {0}", format = MESSAGE_FORMAT)
	@LogMessage(level = WARN)
	public void warn_mwlabs_wst11_ba_context_ArjunaContextImple_1(String arg0);

//	@Message(id = 45059, value = "Invalid address.", format = MESSAGE_FORMAT)
//	@LogMessage(level = WARN)
//	public void warn_mwlabs_wst11_ba_remote_UserBusinessActivityImple_1();

	@Message(id = 45060, value = "Received context is null!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst11_ba_remote_UserBusinessActivityImple_2();

	@Message(id = 45061, value = "No termination context!", format = MESSAGE_FORMAT)
	public String get_mwlabs_wst11_ba_remote_UserBusinessActivityImple_3();

    @Message(id = 45062, value = "Coordinator cancelled the activity", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
   	public void warn_mwlabs_wst11_ba_coordinator_cancelled_activity();

    @Message(id = 45063, value = "Fail to register sub completion coordinator at {0} for activity hierarchy {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_failToRegisterSubCompletionCoordinator(String url, ActivityHierarchy activityHierarchy, @Cause() Throwable ex);
    
    @Message(id = 45064, value = "Fail to register sub completion coordinator for RPC at {0} for activity hierarchy {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_failToRegisterSubCompletionRpcCoordinator(String url, ActivityHierarchy activityHierarchy, @Cause() Throwable ex);
    
    @Message(id = 45065, value = "Cannot get user activity for AT coordinator {0}", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_cannotGetATUserActivity(ATCoordinator coordinator, @Cause() Throwable ex);
    
    @Message(id = 45066, value = "Cannot register volatile participant {0} to identifier {1}. Cause could actually be no activity or"
        + " activity already registered", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_cannotEnlistVolatileParticipant(Volatile2PCParticipant tpp, String id, @Cause() Throwable ex);
    
    @Message(id = 45067, value = "Cannot resume transaction {0} by transaction manager {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_cannotResumeTransaction(TxContext txContext, ContextManager manager, @Cause() Throwable ex);
    
    @Message(id = 45068, value = "Cannot establish activation coordinator service", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_cannotEstablishActivationCoordinatorService(@Cause() Throwable ex);
    
    @Message(id = 45069, value = "Cannot enlist completion participant {0} to tx id {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_cannotEnlistCompletionParticipants(W3CEndpointReference completionParticipant, String id, @Cause() Throwable ex);
    
    @Message(id = 45070, value = "Fail to commit txn {0} with id {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_failCommittingTransaction(TxContext txn, String id, @Cause() Throwable ex);
    
    @Message(id = 45071, value = "Transaction manager {0} cannot suspend", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_cannotSuspendTransaction(ContextManager manager, @Cause() Throwable ex);
    
    @Message(id = 45072, value = "Cannot enlist completion standalone participant for tx id {0}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_cannotEnlistCompletionStandaloneParticipants(String id, @Cause() Throwable ex);
    
    @Message(id = 45073, value = "Cannot dissacociate cleanup id {0}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_cannotDissasociateId(String id, @Cause() Throwable ex);

    @Message(id = 45074, value = "Issue during initialisation context factory {0}", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_issueOnContextFactoryInitialisation(ContextFactory cf, @Cause() Throwable ex);

    @Message(id = 45075, value = "Failure during creation coordination context of uri {0} and context type {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = ERROR)
    public void error_failureOnCoordinationContextCreation(String uri, CoordinationContextType contextType, @Cause() Throwable ex);

    @Message(id = 45076, value = "Failure during creation coordination context of uri {0} and context type {1}", format = MESSAGE_FORMAT)
    @LogMessage(level = WARN)
    public void warn_failureOnCoordinationContextCreation(String uri, CoordinationContextType contextType, @Cause() Throwable ex);

    /*
        Allocate new messages directly above this notice.
          - id: use the next id number in numeric sequence. Don't reuse ids.
          The first two digits of the id(XXyyy) denote the module
            all message in this file should have the same prefix.
          - value: default (English) version of the log message.
          - level: according to severity semantics defined at http://docspace.corp.redhat.com/docs/DOC-30217
          Debug and trace don't get i18n. Everything else MUST be i18n.
          By convention methods with String return type have prefix get_,
            all others are log methods and have prefix <level>_
     */
}
