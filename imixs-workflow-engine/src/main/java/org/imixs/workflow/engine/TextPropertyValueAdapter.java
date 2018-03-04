package org.imixs.workflow.engine;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.event.Observes;

import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.util.XMLParser;

/**
 * The TextPropertyValueAdapter replaces text fragments with named system
 * property values.
 * 
 * @author rsoika
 *
 */
@Stateless
public class TextPropertyValueAdapter {

	@EJB
	WorkflowService workflowService;

	private static Logger logger = Logger.getLogger(AbstractPlugin.class.getName());

	/**
	 * This method reacts on CDI events of the type TextEvent and parses a string
	 * for xml tag <propertyvalue>. Those tags will be replaced with the
	 * corresponding system property value.
	 * 
	 * 
	 */
	public void onEvent(@Observes TextEvent event) {
		String text = event.getText();

		// lower case <propertyValue> into <propertyvalue>
		if (text.contains("<propertyValue") || text.contains("</propertyValue>")) {
			logger.warning("Deprecated <propertyValue> tag should be lowercase <propertyvalue> !");
			text = text.replace("<propertyValue", "<propertyvalue");
			text = text.replace("</propertyValue>", "</propertyvalue>");
		}

		List<String> tagList = XMLParser.findTags(text, "propertyvalue");
		logger.finest("......"+tagList.size() + " tags found");
		// test if a <value> tag exists...
		for (String tag : tagList) {

			// now we have the start and end position of a tag and also the
			// start and end pos of the value

			// read the property Value
			String sPropertyKey = XMLParser.findTagValue(tag, "propertyvalue");

			String vValue = workflowService.getPropertyService().getProperties().getProperty(sPropertyKey);
			if (vValue == null) {
				logger.warning(
						"[AbstractPlugin] propertyvalue '" + sPropertyKey + "' is not defined in imixs.properties!");
				vValue = "";
			}

			// now replace the tag with the result string
			int iStartPos = text.indexOf(tag);
			int iEndPos = text.indexOf(tag) + tag.length();

			// now replace the tag with the result string
			text = text.substring(0, iStartPos) + vValue + text.substring(iEndPos);

		}

		event.setText(text);

	}

	/**
	 * This method returns a formated a string object.
	 * 
	 * In case a Separator is provided, multiValues will be separated by the
	 * provided separator.
	 * 
	 * If no separator is provide, only the first value will returned.
	 * 
	 * The format and locale attributes can be used to format number and date
	 * values.
	 * 
	 */
	public static String formatItemValues(List<?> aItem, String aSeparator, String sFormat, Locale locale,
			String sPosition) {

		StringBuffer sBuffer = new StringBuffer();

		if (aItem == null || aItem.size() == 0)
			return "";

		// test if a position was defined?
		if (sPosition == null || sPosition.isEmpty()) {
			// no - we iterate over all...
			for (Object aSingleValue : aItem) {
				String aValue = formatObjectValue(aSingleValue, sFormat, locale);
				sBuffer.append(aValue);
				// append delimiter only if a separator is defined
				if (aSeparator != null) {
					sBuffer.append(aSeparator);
				} else {
					// no separator, so we can exit with the first value
					break;
				}
			}
		} else {
			// evaluate position
			if ("last".equalsIgnoreCase(sPosition)) {
				sBuffer.append(aItem.get(aItem.size() - 1));
			} else {
				// default first poistion
				sBuffer.append(aItem.get(0));
			}

		}

		String sString = sBuffer.toString();

		// cut last separator
		if (aSeparator != null && sString.endsWith(aSeparator)) {
			sString = sString.substring(0, sString.lastIndexOf(aSeparator));
		}

		return sString;

	}

	/**
	 * This helper method test the type of an object provided by a itemcollection
	 * and formats the object into a string value.
	 * 
	 * Only Date Objects will be formated into a modified representation. other
	 * objects will be returned using the toString() method.
	 * 
	 * If an optional format is provided this will be used to format date objects.
	 * 
	 * @param o
	 * @return
	 */
	private static String formatObjectValue(Object o, String format, Locale locale) {

		Date dateValue = null;

		// now test the objct type to date
		if (o instanceof Date) {
			dateValue = (Date) o;
		}

		if (o instanceof Calendar) {
			Calendar cal = (Calendar) o;
			dateValue = cal.getTime();
		}

		// format date string?
		if (dateValue != null) {
			String singleValue = "";
			if (format != null && !"".equals(format)) {
				// format date with provided formater
				try {
					SimpleDateFormat formatter = null;
					if (locale != null) {
						formatter = new SimpleDateFormat(format, locale);
					} else {
						formatter = new SimpleDateFormat(format);
					}
					singleValue = formatter.format(dateValue);
				} catch (Exception ef) {
					Logger logger = Logger.getLogger(AbstractPlugin.class.getName());
					logger.warning("AbstractPlugin: Invalid format String '" + format + "'");
					logger.warning("AbstractPlugin: Can not format value - error: " + ef.getMessage());
					return "" + dateValue;
				}
			} else
				// use standard formate short/short
				singleValue = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(dateValue);

			return singleValue;
		}

		return o.toString();
	}

}
