/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2001, 2008 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *******************************************************************************/
package org.imixs.workflow.engine.scheduler;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;

/**
 * This SchedulerSaveService is used to save configurations in a new
 * transaction. The service is only called by the SchedulerService in case a
 * scheduler throws a SchedulerException or a RuntimeExcepiton.
 * 
 * @see SchedulerService for details
 * @author rsoika
 * @version 1.0
 */
@Stateless
@LocalBean
@DeclareRoles({ "org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SchedulerConfigurationService {

	@Resource
	SessionContext ctx;

	@Inject
	DocumentService documentService;

	private static Logger logger = Logger.getLogger(SchedulerConfigurationService.class.getName());

	/**
	 * This method saves a configuration in a new transaction. This is needed case
	 * of a runtime exception
	 * 
	 */
	@TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
	public void storeConfigurationInNewTransaction(ItemCollection config) {
		logger.finest(" ....saving scheduler configuration by new transaciton...");
		config.removeItem("$version");
		config = documentService.save(config);
	}
}
