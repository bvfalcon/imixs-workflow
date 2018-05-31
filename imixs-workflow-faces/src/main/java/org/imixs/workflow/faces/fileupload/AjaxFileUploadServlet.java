package org.imixs.workflow.faces.fileupload;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.imixs.workflow.FileData;

@WebServlet(urlPatterns = { "/fileupload/*" })
// configure Servlet 3.0 multipart. Limit file size to 100MB, 500MB Request Size
@MultipartConfig(fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024 * 100, maxRequestSize = 1024 * 1024 * 100
		* 5)
public class AjaxFileUploadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final String REQUEST_METHOD_POST = "POST";
	private static final String REQUEST_METHOD_GET = "GET";
	private static final String CONTENT_TYPE_MULTIPART = "multipart/";
	private static final String CONTENT_DISPOSITION = "content-disposition";
	private static final String CONTENT_DISPOSITION_FILENAME = "filename";
	public static final String IMIXS_FILEDATA_LIST = "IMIXS_FILEDATA_LIST";

	private static Logger logger = Logger.getLogger(AjaxFileUploadServlet.class.getName());

	@Inject
	FileUploadController fileUploadController;

	/**
	 * This method gets the current fileList form the current user session. In case
	 * no fileList is yet stored, the method creates a new empty one.
	 * 
	 * @param httpRequest
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<FileData> getFileList(HttpServletRequest httpRequest) {
		List<FileData> fileDataList = (List<FileData>) httpRequest.getSession().getAttribute(IMIXS_FILEDATA_LIST);
		if (fileDataList == null) {
			fileDataList = new ArrayList<FileData>();
			// store file content into session
			httpRequest.getSession().setAttribute(IMIXS_FILEDATA_LIST, fileDataList);
		}
		return fileDataList;
	}

	/**
	 * Stores a fileDataList into the current user session
	 * 
	 * @param httpRequest
	 * @param fileDataList
	 */
	private void setFileList(HttpServletRequest httpRequest, List<FileData> fileDataList) {

		if (fileDataList == null) {
			fileDataList = new ArrayList<FileData>();
		}
		// store file content into session
		httpRequest.getSession().setAttribute(IMIXS_FILEDATA_LIST, fileDataList);
	}

	/**
	 * Upload files to stored in the current user session
	 */
	@Override
	protected void doPost(HttpServletRequest httpRequest, HttpServletResponse response)
			throws ServletException, IOException {
		if (isPostFileUploadRequest(httpRequest)) {
			List<FileData> fileDataList = getFileList(httpRequest);
			logger.finest("......add files...");

			addFilesold(httpRequest);
			// store file content into session
			setFileList(httpRequest, fileDataList);
			
			// now update the workitem....
			if (fileUploadController != null) {
				logger.info("conversation id=" + fileUploadController.getCID());
				// check workitem... issue
				if (fileUploadController.getWorkitem() != null) {
					logger.info("alles rogger in cambocha");
					fileUploadController.updateWorkitem(fileDataList);
				}
			}
			
			
			
			writeJsonMetadata(fileDataList, response, httpRequest.getRequestURI());

		}
	}

	/**
	 * Delete a existing file form the fileData list stored in the current user
	 * session
	 */
	@Override
	protected void doDelete(HttpServletRequest httpRequest, HttpServletResponse response)
			throws ServletException, IOException {

		List<FileData> fileDataList = getFileList(httpRequest);
		int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
		String filename = httpRequest.getRequestURI().substring(iCancel + 12);
		removeFile(httpRequest, filename);
		// store file content into session
		setFileList(httpRequest, fileDataList);
		// get context url from request uri
		String contextURL = httpRequest.getRequestURI();
		// cut last /....
		contextURL = contextURL.substring(0, contextURL.lastIndexOf('/') + 1);
		writeJsonMetadata(fileDataList, response, contextURL);
	}

	/**
	 * Getter method to return the file content from the fileData list stored in the
	 * current user
	 */
	@Override
	protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
			throws ServletException, IOException {

		// check cancel upload...
		if (isGetFileUploadRequest(httpRequest)) {
			int iCancel = httpRequest.getRequestURI().indexOf("/fileupload/");
			String filename = httpRequest.getRequestURI().substring(iCancel + 12);

			// urldecoding...
			filename = URLDecoder.decode(filename, "UTF-8");

			FileData fileData = getFile(httpRequest, filename);
			// write contenremoveFile(filename);
			if (fileData != null) {
				writeFileContent(httpResponse, fileData);
			} else {
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}

		// request just the currently uploaded files in json format
		if (isGetRefreshFileUploadRequest(httpRequest)) {
			String contextURL = httpRequest.getRequestURI();
			writeJsonMetadata(this.getFileList(httpRequest), httpResponse, contextURL);
		}
	}

	/**
	 * checks if the httpRequest is a fileupload
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isPostFileUploadRequest(HttpServletRequest httpRequest) {
		String sContentType = httpRequest.getContentType();
		logger.finest("......contentType=" + sContentType);

		return (REQUEST_METHOD_POST.equalsIgnoreCase(httpRequest.getMethod()) && httpRequest.getContentType() != null
				&& sContentType.toLowerCase().startsWith(CONTENT_TYPE_MULTIPART));
	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetFileUploadRequest(HttpServletRequest httpRequest) {
		String uri = httpRequest.getRequestURI();

		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod())
				&& !(uri.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * checks if the httpRequest is a fileupload get request...
	 * 
	 * @param httpRequest
	 * @return
	 */
	private boolean isGetRefreshFileUploadRequest(HttpServletRequest httpRequest) {

		String uri = httpRequest.getRequestURI();
		return (REQUEST_METHOD_GET.equalsIgnoreCase(httpRequest.getMethod())
				&& (uri.endsWith("/fileupload") || uri.endsWith("/fileupload/")));

	}

	/**
	 * Returns a file attachment located in the property $file of the specified
	 * workitem
	 * 
	 * The file name will be encoded. With a URLDecode the filename is decoded in
	 * different formats and searched in the file list. This is not a nice solution.
	 * 
	 * @param uniqueid
	 * @return
	 * @throws IOException
	 */
	private void writeFileContent(ServletResponse response, FileData fileData) throws IOException {
		logger.finest("......write file content...");
		ServletOutputStream output = response.getOutputStream();
		output.write(fileData.getContent());
		// now return json string of uploaded files....
		response.setContentType(fileData.getContentType());
		output.close();
	}

	/**
	 * test and extracts the filename of a http request part. The method returns
	 * null if the part dose not contain a file
	 * 
	 * @param part
	 * @return - filename or null if not a file content
	 */
	private String getFilename(Part part) {
		for (String cd : part.getHeader(CONTENT_DISPOSITION).split(";")) {
			if (cd.trim().startsWith(CONTENT_DISPOSITION_FILENAME)) {
				return cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
			}
		}
		return null;
	}

	/**
	 * removes an uploaded file from the fileDataList...
	 * 
	 * @param file
	 *            - filename to be removed
	 */
	private List<FileData> removeFile(HttpServletRequest httpRequest, String file) {
		int pos = -1;

		List<FileData> fileDataList = getFileList(httpRequest);
		if (file == null)
			return fileDataList;

		for (int i = 0; i < fileDataList.size(); i++) {
			FileData fileData = fileDataList.get(i);
			if (file.equals(fileData.getName())) {
				pos = i;
				break;
			}
		}
		// found?
		if (pos > -1) {
			logger.finest("......remove file '" + file + "'");
			fileDataList.remove(pos);

			// store file content into session
			setFileList(httpRequest, fileDataList);
		}

		return fileDataList;
	}

	/**
	 * This method converts mulitple files from the httpRequest into FileData
	 * objects.
	 * 
	 * @param httpRequest
	 */
	private List<FileData> getFilesFromRequest(HttpServletRequest httpRequest) {
		logger.finest("......Looping parts");

		List<FileData> fileDataList = new ArrayList<FileData>();
		try {
			for (Part p : httpRequest.getParts()) {
				byte[] b = new byte[(int) p.getSize()];
				p.getInputStream().read(b);
				p.getInputStream().close();
				// params.put(p.getName(), new String[] { new String(b) });

				// test if part contains a file
				String fileName = getFilename(p);
				if (fileName != null) {

					/*
					 * issue #106
					 * 
					 * https://developer.jboss.org/message/941661#941661
					 * 
					 * Here we test of the encoding and try to convert to utf-8.
					 */
					byte fileNameISOBytes[] = fileName.getBytes("iso-8859-1");
					String fileNameUTF8 = new String(fileNameISOBytes, "UTF-8");
					if (fileName.length() != fileNameUTF8.length()) {
						// convert to utf-8
						logger.finest("......filename seems to be ISO-8859-1 encoded");
						fileName = new String(fileName.getBytes("iso-8859-1"), "utf-8");
					}

					// extract the file content...
					FileData fileData = null;
					logger.finest("......filename : " + fileName + ", contentType " + p.getContentType());
					fileData = new FileData(fileName, b, p.getContentType());
					fileDataList.add(fileData);

				}
			}

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		return fileDataList;
	}

	/**
	 * This method adds mulitple files into the FileDataList stored in the current
	 * user session
	 * 
	 * @param httpRequest
	 */
	private void addFilesold(HttpServletRequest httpRequest) {
		logger.finest("......Looping parts");

		try {
			for (Part p : httpRequest.getParts()) {
				byte[] b = new byte[(int) p.getSize()];
				p.getInputStream().read(b);
				p.getInputStream().close();
				// params.put(p.getName(), new String[] { new String(b) });

				// test if part contains a file
				String fileName = getFilename(p);
				if (fileName != null) {

					/*
					 * issue #106
					 * 
					 * https://developer.jboss.org/message/941661#941661
					 * 
					 * Here we test of the encoding and try to convert to utf-8.
					 */
					byte fileNameISOBytes[] = fileName.getBytes("iso-8859-1");
					String fileNameUTF8 = new String(fileNameISOBytes, "UTF-8");
					if (fileName.length() != fileNameUTF8.length()) {
						// convert to utf-8
						logger.finest("......filename seems to be ISO-8859-1 encoded");
						fileName = new String(fileName.getBytes("iso-8859-1"), "utf-8");
					}

					// extract the file content...
					FileData fileData = null;
					logger.finest("......filename : " + fileName + ", contentType " + p.getContentType());
					fileData = new FileData(fileName, b, p.getContentType());
					if (fileData != null) {
						// remove existing file
						List<FileData> fileDataList = removeFile(httpRequest, fileData.getName());
						// add new fileData..
						fileDataList.add(fileData);
						// store file content into session
						setFileList(httpRequest, fileDataList);
					}

				}
			}

		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (ServletException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

	}

	/**
	 * gets an uploaded fileData from the fileDataList stored in the crrent user
	 * session...
	 * 
	 * @param file
	 *            - filename to be searched for
	 */
	private FileData getFile(HttpServletRequest httpRequest, String file) {
		List<FileData> fileDataList = getFileList(httpRequest);

		FileData result = null;
		if (file == null)
			return null;
		for (int i = 0; i < fileDataList.size(); i++) {
			FileData fileData = fileDataList.get(i);
			if (file.equals(fileData.getName())) {
				result = fileData;
				break;
			}
		}
		return result;
	}


	/**
	 * This method write a JSON meta data structure for uploaded files into the
	 * httpResponse.
	 * 
	 * 
	 * <code>
			{
			    "files": [
			        {
			            "url": "0:0:0:0:0:0:0:1",
			            "thumbnail_url": "",
			            "name": "start.gif",
			            "type": "image/gif",
			            "size": 128,
			            "delete_url": "",
			            "delete_type": "DELETE"
			        }
			    ]
			}
	 *  </code>
	 * 
	 * 
	 * 
	 * 
	 * * @see https://github.com/blueimp/jQuery-File-Upload/wiki/JSON-Response
	 * @throws IOException 
	 * 
	 */
	private void writeJsonMetadata(List<FileData> fileDataList, ServletResponse response,String context_url) throws IOException {
		logger.finest("......write JSON meta data...");
		response.setContentType("application/json;charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		String result = "{ \"files\":[";
		for (int i = 0; i < fileDataList.size(); i++) {

			FileData fileData = fileDataList.get(i);

			result += "{ \"url\": \"" + context_url + fileData.getName() + "\",";
			result += "\"thumbnail_url\": \"\",";
			result += "\"name\": \"" + fileData.getName() + "\",";
			result += "\"type\": \"" + fileData.getContentType() + "\",";
			result += "\"size\": " + fileData.getContent().length + ",";
			result += "\"delete_url\": \"\",";
			result += "\"delete_type\": \"DELETE\"";

			// last element?
			if (i < fileDataList.size() - 1)
				result += "},";
			else
				result += "}";
		}

		result += "]}";

		out.write(result);
		out.close();
		
		
	}

}
