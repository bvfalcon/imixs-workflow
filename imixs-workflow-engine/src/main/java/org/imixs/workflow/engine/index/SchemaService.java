/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
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
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.workflow.engine.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The IndexSchemaService provides the index Schema.
 * <p>
 * The schema is defined by the following properties:
 * 
 * <ul>
 * <li>index.fields - content which will be indexed</li>
 * <li>index.fields.analyse - fields indexed as analyzed keyword fields</li>
 * <li>index.fields.noanalyse - fields indexed without analyze</li>
 * <li>index.fields.store - fields stored in the index</li>
 * <li>index.operator - default operator</li>
 * <li>index.splitwhitespace - split text on whitespace prior to analysis</li>
 * </ul>
 * 
 * 
 * @version 1.0
 * @author rsoika
 */
@Singleton
public class SchemaService {

	/*
	 * index.fields index.fields.analyse index.fields.noanalyse index.fields.store
	 * 
	 * index.operator index.splitwhitespace
	 * 
	 * 
	 */

	public static final String ANONYMOUS = "ANONYMOUS";

	@Inject
	@ConfigProperty(name = "index.fields", defaultValue = "")
	private String indexFields;

	@Inject
	@ConfigProperty(name = "index.fields.analyze", defaultValue = "")
	private String indexFieldsAnalyse;

	@Inject
	@ConfigProperty(name = "index.fields.noanalyze", defaultValue = "")
	private String indexFieldsNoAnalyse;

	@Inject
	@ConfigProperty(name = "index.fields.store", defaultValue = "")
	private String indexFieldsStore;

	@Inject
	private DocumentService documentService;

	private List<String> fieldList = null;
	private List<String> fieldListAnalyse = null;
	private List<String> fieldListNoAnalyse = null;
	private List<String> fieldListStore = null;

	// default field lists
	private static List<String> DEFAULT_SEARCH_FIELD_LIST = Arrays.asList("$workflowsummary", "$workflowabstract");
	private static List<String> DEFAULT_NOANALYSE_FIELD_LIST = Arrays.asList("$modelversion", "$taskid", "$processid",
			"$workitemid", "$uniqueidref", "type", "$writeaccess", "$modified", "$created", "namcreator", "$creator",
			"$editor", "$lasteditor", "$workflowgroup", "$workflowstatus", "txtworkflowgroup", "name", "txtname",
			"$owner", "namowner", "txtworkitemref", "$uniqueidsource", "$uniqueidversions", "$lasttask", "$lastevent",
			"$lasteventdate");
	private static List<String> DEFAULT_STORE_FIELD_LIST = Arrays.asList("type", "$taskid", "$writeaccess",
			"$workflowsummary", "$workflowabstract", "$workflowgroup", "$workflowstatus", "$modified", "$created",
			"$lasteventdate", "$creator", "$editor", "$lasteditor", "$owner", "namowner");

	private static Logger logger = Logger.getLogger(SchemaService.class.getName());

	/**
	 * PostContruct event - The method loads the lucene index properties from the
	 * imixs.properties file from the classpath. If no properties are defined the
	 * method terminates.
	 * 
	 */
	@PostConstruct
	void init() {

		logger.finest("......lucene FulltextFieldList=" + indexFields);
		logger.finest("......lucene IndexFieldListAnalyse=" + indexFieldsAnalyse);
		logger.finest("......lucene IndexFieldListNoAnalyse=" + indexFieldsNoAnalyse);

		// compute search field list
		fieldList = new ArrayList<String>();
		// add all static default field list
		fieldList.addAll(DEFAULT_SEARCH_FIELD_LIST);
		if (indexFields != null && !indexFields.isEmpty()) {
			StringTokenizer st = new StringTokenizer(indexFields, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName) && !fieldList.contains(sName)) {
					fieldList.add(sName);
				}
			}
		}

		// compute Index field list (Analyze)
		fieldListAnalyse = new ArrayList<String>();
		if (indexFieldsAnalyse != null && !indexFieldsAnalyse.isEmpty()) {
			StringTokenizer st = new StringTokenizer(indexFieldsAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// do not add internal fields
				if (!"$uniqueid".equals(sName) && !"$readaccess".equals(sName)) {
					fieldListAnalyse.add(sName);
				}
			}
		}

		// compute Index field list (NoAnalyze)
		fieldListNoAnalyse = new ArrayList<String>();
		// add all static default field list
		fieldListNoAnalyse.addAll(DEFAULT_NOANALYSE_FIELD_LIST);
		if (indexFieldsNoAnalyse != null && !indexFieldsNoAnalyse.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(indexFieldsNoAnalyse, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				// Issue #560 - avoid duplicates from indexFieldsAnalyse
				if (!fieldListNoAnalyse.contains(sName) && !indexFieldsAnalyse.contains(sName)) {
					fieldListNoAnalyse.add(sName);
				}
			}
		}

		// compute Index field list (Store)
		fieldListStore = new ArrayList<String>();
		// add all static default field list
		fieldListStore.addAll(DEFAULT_STORE_FIELD_LIST);
		if (indexFieldsStore != null && !indexFieldsStore.isEmpty()) {
			// add additional field list from imixs.properties
			StringTokenizer st = new StringTokenizer(indexFieldsStore, ",");
			while (st.hasMoreElements()) {
				String sName = st.nextToken().toLowerCase().trim();
				if (!fieldListStore.contains(sName))
					fieldListStore.add(sName);
			}
		}

		// Issue #518:
		// if a field of the indexFieldListStore is not part of the
		// fieldListAnalyse add not part of fieldListNoAnalyse , than we add these
		// fields to the indexFieldListAnalyse. This is to guaranty that we store the
		// field value in any case.
		for (String fieldName : fieldListStore) {
			if (!fieldListAnalyse.contains(fieldName) && !fieldListNoAnalyse.contains(fieldName)) {
				// add this field into he indexFieldListAnalyse
				fieldListAnalyse.add(fieldName);
			}
		}

	}

	public List<String> getFieldList() {
		return fieldList;
	}

	public List<String> getFieldListAnalyse() {
		return fieldListAnalyse;
	}

	public List<String> getFieldListNoAnalyse() {
		return fieldListNoAnalyse;
	}

	public List<String> getFieldListStore() {
		return fieldListStore;
	}

	/**
	 * Returns the Lucene schema configuration
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		ItemCollection config = new ItemCollection();

		config.replaceItemValue("lucence.fulltextFieldList", fieldList);
		config.replaceItemValue("lucence.indexFieldListAnalyze", fieldListAnalyse);
		config.replaceItemValue("lucence.indexFieldListNoAnalyze", fieldListNoAnalyse);
		config.replaceItemValue("lucence.indexFieldListStore", fieldListStore);

		return config;
	}

	/**
	 * Returns the extended search term for a given query. The search term will be
	 * extended with a users roles to test the read access level of each workitem
	 * matching the search term.
	 * 
	 * @param sSearchTerm
	 * @return extended search term
	 * @throws QueryException
	 *             in case the searchtem is not understandable.
	 */
	public String getExtendedSearchTerm(String sSearchTerm) throws QueryException {
		// test if searchtem is provided
		if (sSearchTerm == null || "".equals(sSearchTerm)) {
			logger.warning("No search term provided!");
			return "";
		}
		// extend the Search Term if user is not ACCESSLEVEL_MANAGERACCESS
		if (!documentService.isUserInRole(DocumentService.ACCESSLEVEL_MANAGERACCESS)) {
			// get user names list
			List<String> userNameList = documentService.getUserNameList();
			// create search term (always add ANONYMOUS)
			String sAccessTerm = "($readaccess:" + ANONYMOUS;
			for (String aRole : userNameList) {
				if (!"".equals(aRole))
					sAccessTerm += " OR $readaccess:\"" + aRole + "\"";
			}
			sAccessTerm += ") AND ";
			sSearchTerm = sAccessTerm + sSearchTerm;
		}
		logger.finest("......lucene final searchTerm=" + sSearchTerm);

		return sSearchTerm;
	}

	/**
	 * This helper method escapes special characters found in a lucene search term.
	 * The method can be used by clients to prepare a search phrase.
	 * <p>
	 * Special characters are characters that are part of the lucene query syntax
	 * <p>
	 * <code>+ - && || ! ( ) { } [ ] ^ " ~ * ? : \ /</code>
	 * <p>
	 * Clients should use the method normalizeSearchTerm() instead of
	 * escapeSearchTerm() to prepare a user input for a lucene search.
	 * 
	 * @see normalizeSearchTerm
	 * @param searchTerm
	 * @param ignoreBracket
	 *            - if true brackes will not be escaped.
	 * @return escaped search term
	 */
	public String escapeSearchTerm(String searchTerm, boolean ignoreBracket) {
		if (searchTerm == null || searchTerm.isEmpty()) {
			return searchTerm;
		}

		// this is the code from the QueryParser.escape() method without the '*'
		// char!
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < searchTerm.length(); i++) {
			char c = searchTerm.charAt(i);
			// These characters are part of the query syntax and must be escaped
			// (ignore brackets!)
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == ':' || c == '^' || c == '[' || c == ']'
					|| c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
				sb.append('\\');
			}

			// escape bracket?
			if (!ignoreBracket && (c == '(' || c == ')')) {
				sb.append('\\');
			}

			sb.append(c);
		}
		return sb.toString();

	}

	public String escapeSearchTerm(String searchTerm) {
		return escapeSearchTerm(searchTerm, false);
	}

	/**
	 * This method normalizes a search term. The method can be used by clients to
	 * prepare a search phrase. The serach term will be lowercased and special
	 * characters will be replaced by a blank separator
	 * <p>
	 * e.g. 'europe/berlin' will be normalized to 'europe berlin'
	 * <p>
	 * In case the searchTerm contains numbers the method escapes special characters
	 * instead of replacing with a blank:
	 * <p>
	 * e.g. 'r-555/333' will be converted into 'r\-555\/333'
	 * <p>
	 * Special characters are characters that are part of the lucene query syntax
	 * <p>
	 * <code>+ - && || ! ( ) { } [ ] ^ " ~ ? : \ /</code>
	 * <p>
	 * 
	 * @param searchTerm
	 * @return normalized search term
	 * 
	 */
	public String normalizeSearchTerm(String searchTerm) {

		if (searchTerm == null) {
			return "";
		}
		if (searchTerm.trim().isEmpty()) {
			return "";
		}

		// lowercase
		searchTerm = searchTerm.toLowerCase();

		if (containsDigit(searchTerm)) {
			return escapeSearchTerm(searchTerm, false);
		}

		// now replace Special Characters with blanks
		// + - && || ! ( ) { } [ ] ^ " ~ * ? : \ /
		// this is the code from the QueryParser.escape() method without the '*'
		// char!
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < searchTerm.length(); i++) {
			char c = searchTerm.charAt(i);
			// These characters are part of the query syntax and must be escaped
			// (ignore brackets!)
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == ':' || c == '^' || c == '[' || c == ']'
					|| c == '\"' || c == '{' || c == '}' || c == '~' || c == '?' || c == '|' || c == '&' || c == '/') {
				sb.append(' ');
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Test if a string contains a number. Seems to be faster than regex.
	 * <p>
	 * See:
	 * https://stackoverflow.com/questions/18590901/check-if-a-string-contains-numbers-java
	 * 
	 * @param s
	 * @return
	 */
	private final boolean containsDigit(String s) {
		boolean containsDigit = false;
		if (s != null && !s.isEmpty()) {
			for (char c : s.toCharArray()) {
				if (containsDigit = Character.isDigit(c)) {
					break;
				}
			}
		}
		return containsDigit;
	}

}
