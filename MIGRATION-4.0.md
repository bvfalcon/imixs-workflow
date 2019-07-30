# Migration Notes 4.0.0

This document contains migration notes about the migration from Imixs-Workflow version 3.x to version 4.x.

With version 4.0.0 a new database schema was introduced. Data is now only stored in one table named 'DOCUMENT' 

	CREATE TABLE `DOCUMENT` (
	  `ID` varchar(255) NOT NULL,
	  `CREATED` datetime DEFAULT NULL,
	  `DATA` longblob,
	  `MODIFIED` datetime DEFAULT NULL,
	  `TYPE` varchar(255) DEFAULT NULL,
	  `VERSION` int(11) DEFAULT NULL,
	  PRIMARY KEY (`ID`)
	)

To migrate data form version 3.x to version 4.x the latest [Imixs-Admin Client](http://www.imixs.org/doc/administration.html) provides a migration task which can be used for a smooth migration.

## Migration Guide

1. Migrate Workflow Models - (new package org.imixs.workflow.engine - see details below)

2. Undeploy the existing application from your application server (containing Imixs-Workflow 3.x)

3. Backup Database (recommended)
 
4. Shutdown the application server and remove existing Lucene index from filesystem (also remove old artifacts)
 
5. Extend imixs.properties param "FieldListNoAnalyse" with the values "txtemail, datdate, datfrom, datto, numsequencenumber, txtUsername"

6. Restart the  the application server and deploy new application (containing Imixs-Workflow 4.x) 
 
7. Start Migration Job from Imixs-Admin Interface
 
8. Migrate the plugin list of your workflow models and upload the new models into your application
 


# Coding Guidelines

The following section contains information about new coding guidelines to be followed in a migration from Imixs-Workflow version 3.x to version 4.x.


## How to Migrate BPMN Models

In Imixs-Workflow 4.x the plug-in package has changed from 'org.imixs.workflow.plugins....'  to 'org.imixs.workflow.engine.plugins...'.
The plugin 'org.imixs.workflow.lucene.LucenePlugin' can be removed form the model. 

You need to change the plugin classes in your model files and upload the new models through the rest API

	curl --user admin:adminadmin --request POST -Tmymodel.bpmn http://localhost:8080/workflow/rest-service/model/bpmn
 

## Packages

The following java packages are deprecated in version 4.x but still available to support the migration:

  * org.imxis.workflow.jee.ejb
  * org.imxis.workflow.jee.jpa
  * org.imxis.workflow.jee.util
  * org.imxis.workflow.plugins.jee
 
The packages will be removed in future releases. 

The following new packages are introduced with version 4.x :

  * org.imixs.workflow.engine - contains the services EJB classes
  * org.imixs.workflow.engine.jpa - contains the persistence JPA classes
  * org.imixs.workflow.engine.plugins - contains all plug-in classes
 
## Persistence API

With version 4.x a new persistence layer was introduced. There is now only one single JPA entity bean class

	org.imixs.workflow.jpa.Document

The Document class replaces the deprecated Entity Class with all additional index classes.

The package 'org.imxis.workflow.jee.jpa' is still available in version 4.x to support the migration path. We will drop this package with version 4.1.x finally. 
 
 
## EJBs

All Imixs Service EJBs are moved into the package '_org.imixs.workflow.ejb_'. The EntityService EJB was replaced with the new [DocumentService](http://www.imixs.org/doc/engine/documentservice.html) EJB.  

## Migrate JPQL Statements

JPQL Statements can be replaced with corresponding Lucene Search terms.

		String sQuery = "SELECT config FROM Entity AS config " + " JOIN config.textItems AS t2"
				+ " WHERE config.type = '" + TYPE_CONFIGURATION + "'" + " AND t2.itemName = 'txtname'" + " AND t2.itemValue = '"
				+ NAME + "'" + " ORDER BY t2.itemValue asc";
		Collection<ItemCollection> col = entityService.findAllEntities(sQuery, 0, 1);

...to be replaced with....
		
		String searchTerm="(type:\"" + TYPE_CONFIGURATION  +"\" AND txtname:\"" + NAME + "\")";
		Collection<ItemCollection> col = documentService.find(sQuery, 0, 1);
		
Read the [Query Syntax](http://www.imixs.org/doc/engine/queries.html) for more details.

## Rest API

The old rest api to post workitems in JSON/XML format is supported by a backport using the resource /v3/

     /v3/workflow/
     /v3/entity/
     /v3/model/

**NOTE:** This api is deprecated and will be droped with future releases!

## XML

The XML schema changed. The tag 'entity' was renamed with 'document'. The tag 'name' is now an attribute of the document tag.
See example below:

	   <document>
	        <item name="$created">
	            <value xsi:type="xs:dateTime">2015-04-19T10:50:15.579+02:00</value>
	        </item>
	        <item name="$modified">
	            <value xsi:type="xs:dateTime">2016-08-30T16:58:07.821+02:00</value>
	        </item>
	        <item name="$uniqueid">
	            <value xsi:type="xs:string">14cd0def79b-c3826b4</value>
	        </item>
	        <item name="$version">
	            <value xsi:type="xs:int">1</value>
	        </item>
	        <item name="members">
	            <value xsi:type="xs:string">anna</value>
	            <value xsi:type="xs:string">tom</value>
	            <value xsi:type="xs:string">bob</value>
	        </item>
	        .....
	    </document>

