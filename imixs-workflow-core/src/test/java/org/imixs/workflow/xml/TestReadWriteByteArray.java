package org.imixs.workflow.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.imixs.workflow.ItemCollection;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class write a ItemColleciton into a byte array and reads is via the
 * XMLItemCollectionAdapter
 * 
 * 
 * @author rsoika
 */
public class TestReadWriteByteArray {



	/**
	 * Write a ItemCollection into a byte array and reads it back
	 * 
	 */
	@Test

	public void testWriteRead() {
		byte[] empty = { 0 };
		// PHASE I.
		// Create example IemCollection....
		ItemCollection itemColSource = new ItemCollection();
		itemColSource.replaceItemValue("txtTitel", "Hello");

		// create list of list...
		List<List<String>> valueList = new ArrayList<List<String>>();

		List<String> list1 = new ArrayList<String>();
		list1.add("Berlin");
		list1.add("Munich");
		valueList.add(list1);

		List<String> list2 = new ArrayList<String>();
		list2.add("John");
		list2.add("Sam");
		valueList.add(list2);

		itemColSource.replaceItemValue("_listdata", valueList);

		// create list of map...
		List<Map<String, List<Object>>> mapList = new ArrayList<Map<String, List<Object>>>();

		ItemCollection i1 = new ItemCollection();
		i1.replaceItemValue("_name", "some data");
		i1.replaceItemValue("_city", list1);
		ItemCollection i2 = new ItemCollection();
		i2.replaceItemValue("_name", "other data");
		i2.replaceItemValue("_city", "Munich");
		mapList.add(i1.getAllItems());
		mapList.add(i2.getAllItems());
		itemColSource.replaceItemValue("_mapdata", mapList);

		// add a file

		itemColSource.addFile(empty, "test.txt", null);

		// PHASE II.
		// write into byte[]
		byte[] data = null;
		try {
			data = XMLDocumentAdapter.writeItemCollection(itemColSource);
			// test if we found some data
			Assert.assertTrue(data.length > 100);
		} catch (JAXBException | IOException e) {
			e.printStackTrace();
			Assert.fail();
		}

		// PHASE III.
		// read from byte[]
		try {
			ItemCollection resultItemCollection = XMLDocumentAdapter.readItemCollection(data);
			Assert.assertNotNull(resultItemCollection);
			Assert.assertNotSame(itemColSource, resultItemCollection);
			// verify content
			Assert.assertEquals(itemColSource.getItemValue("_name"), resultItemCollection.getItemValue("_name"));
			Assert.assertEquals(itemColSource.getItemValue("_listdata"),
					resultItemCollection.getItemValue("_listdata"));
			Assert.assertEquals(itemColSource.getItemValue("_mapdata"), resultItemCollection.getItemValue("_mapdata"));

			List<String> fileNames = itemColSource.getFileNames();
			Assert.assertNotNull(fileNames);
			Assert.assertEquals(1, fileNames.size());
			Map<String, List<Object>> files = itemColSource.getFiles();
			Assert.assertNotNull(files);
			List<Object> testFile = files.get("test.txt");
			Assert.assertEquals(2, testFile.size());
			Assert.assertEquals("application/unknown", testFile.get(0).toString());
			Assert.assertEquals(empty, testFile.get(1));

		} catch (JAXBException | IOException e) {
			e.printStackTrace();
			Assert.fail();
		}

	}

}
