package com.xiaoguangchen.antex.taskdefs;

/*
 * Copyright  2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/**
 * <p>Title: BuildNumber</p>
 *
 * <p>Description: This extends Apache Ant's buildnumber task to
 * allow String as build number instead of just number.
 *
 * This task increments the number. The build number could contains any string or numbers.
 * If the last nubmer is number, the number will be incremented by 1, otherwise, and number will be append by ".0"
 *
 * By default the buildNumber file is a property file:
 * the content should be like
 * build.number=version_0.1.234
 *
 * By set isPropertyFile to false, the buildnumber file could simply a non-proerty file
 * the buildnumber file should be a single line with number
 *
 * project_version_0.1.234
 *
 * </p>
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.util.FileUtils;
import com.xiaoguangchen.antex.utils.PropertyLoader;

public class BuildNumber extends Task {
	/**
	 * The name of the property in which the build number is stored,
	 * if the property name is not set
	 */
	private static final String DEFAULT_PROPERTY_NAME = "build.number";

	/** The default filename to use if no file specified.  */
	private static final String DEFAULT_FILENAME = DEFAULT_PROPERTY_NAME;

	/** The File in which the build number is stored.  */
	private File m_buildnumberFile;
	/**
	 * property name, if null or empty set to default property name. If the name is set
	 * but different from the default name, use the user-defined name
	 */
	private String m_name;

	private boolean m_isPropertyFile;


	// template variable
	private  Properties m_currentWorkingProperties;


	public BuildNumber()
	{
		super();
		m_isPropertyFile = true;
	}

	/**
	 * The file in which the build number is stored. Defaults to
	 * "build.number" if not specified.
	 *
	 * @param file the file in which build number is stored.
	 */
	public void setFile(final File file) {
		m_buildnumberFile = file;
	}

	public void setIsPropertyFile(String isPropertyFile) {
		m_isPropertyFile = (null == isPropertyFile) ||
			(isPropertyFile.equalsIgnoreCase("true") ||
			isPropertyFile.equalsIgnoreCase("yes")) ;
	}

	public boolean getIsPropertyFile( ) {
	   return  m_isPropertyFile ;
	}


	public void setName(String name) {
		m_name = name;
	}

	public String getName() {
		return m_name;
	}

	private String getPropertyName() {
	  if (m_name == null || m_name.length() == 0)
		  return DEFAULT_PROPERTY_NAME;
	  return m_name;
	}

	/**
	 * Run task.
	 *
	 * @exception BuildException if an error occurs
	 */
	public void execute() throws BuildException {
		File savedFile = m_buildnumberFile;

		validate();

		String buildNumber = getNextBuildNumber( );
		setBuildNumberToFile(buildNumber);

		m_buildnumberFile = savedFile;

		//Finally set the property
		getProject().setNewProperty(getPropertyName(),String.valueOf(buildNumber));
	}

	public void setBuildNumberToFile(String buildNumber)
	{
		if (m_isPropertyFile)
			setBuildNumberToPropertyFile(buildNumber);
		else {

			try {
				writeStringToFile(buildNumber, m_buildnumberFile);
			} catch (IOException ex) {
				final String message = "Error while writing " + m_buildnumberFile;
				throw new BuildException(message, ex);
			}
		}
	}

	public void setBuildNumberToPropertyFile(String buildNumber)
	{

		// Write the properties file back out
		FileOutputStream output = null;

		try {
			output = new FileOutputStream(m_buildnumberFile);
			final String header = "Auto generated build number. Do not edit!";
			m_currentWorkingProperties.clear();
			m_currentWorkingProperties.put(getPropertyName(),buildNumber);
			m_currentWorkingProperties.store(output, header);
		} catch (final IOException ioe) {
			final String message = "Error while writing " + m_buildnumberFile;
			throw new BuildException(message, ioe);
		} finally {
			if (null != output) {
				try {
					output.close();
				} catch (final IOException ioe) {
					getProject().log("error closing output stream " + ioe, Project.MSG_ERR);
				}
			}

		}
	}


	public final String getNextBuildNumber()
	{
		if (getIsPropertyFile())
		{
			final Properties properties = loadProperties();

			m_currentWorkingProperties = properties;

			return getNextBuildNumber(properties);
		}
		else {
			try {
				String buildNumber = readFileContents(m_buildnumberFile).trim();

				//remove new line
				int cfrIndex = buildNumber.lastIndexOf("\n");
				if (cfrIndex > 0)
					buildNumber = buildNumber.substring(0, cfrIndex-1);

				return getNextBuildNumber(buildNumber);

			} catch (IOException ex) {
				throw new BuildException(ex);
			}


		}
	}


	/**
	 * Utility method to retrieve build number from properties object.
	 *
	 * @param properties the properties to retrieve build number from
	 * @return the build number or if no number in properties object
	 * @throws BuildException if build.number property is not an integer
	 */
	private String getNextBuildNumber(final Properties properties) throws BuildException {
		String value =properties.getProperty(getPropertyName());
		if (value == null)
			 value = properties.getProperty(DEFAULT_PROPERTY_NAME);

		if (value == null)  value = "0";
		final String buildNumber = value.trim();
		return getNextBuildNumber(buildNumber);
	}


	/**
	 * Utility method to retrieve next build number from original build number
	 *
	 * @param originalBuildNumber String
	 *
	 * @return String next build number
	 * @throws BuildException
	 */
	private String getNextBuildNumber(String originalBuildNumber) throws BuildException {
//System.out.println(" orginal builld number =" + originalBuildNumber);
		StringBuffer strbuf;
		int lastIndex = originalBuildNumber.length() -1;
		if (lastIndex > 1)
		   strbuf = new StringBuffer(originalBuildNumber.substring(0, lastIndex));
		else
		  strbuf = new StringBuffer(originalBuildNumber);

		char lastChar = originalBuildNumber.charAt(lastIndex);
		String lastLetter = originalBuildNumber.substring(lastIndex);
		if (Character.isDigit(lastChar)) {
			 int lastDigit = Integer.valueOf(lastLetter) + 1;
			strbuf.append(lastDigit);
		}
		else
		  strbuf.append(lastLetter).append(".0");

//System.out.println(" builld number =" + strbuf.toString());
		return strbuf.toString();
	}

	/**
	 * Utility method to load properties from file.
	 *
	 * @return the loaded properties
	 * @throws BuildException
	 */

	private Properties loadProperties() throws BuildException {
		return PropertyLoader.loadPropertiesFromFile(m_buildnumberFile);
	}


	/**
	 * Validate that the task parameters are valid.
	 *
	 * @throws BuildException if parameters are invalid
	 */
	private void validate() throws BuildException {
		if (null == m_buildnumberFile) {
			m_buildnumberFile = getProject().resolveFile(DEFAULT_FILENAME);
		}

		if (!m_buildnumberFile.exists()) {
			try {
				FileUtils.newFileUtils().createNewFile(m_buildnumberFile);
			} catch (final IOException ioe) {
				final String message =
					m_buildnumberFile + " doesn't exist and new file can't be created.";
				throw new BuildException(message, ioe);
			}
		}

		if (!m_buildnumberFile.canRead()) {
			final String message = "Unable to read from " + m_buildnumberFile + ".";

			throw new BuildException(message);
		}

		if (!m_buildnumberFile.canWrite()) {
			final String message = "Unable to write to " + m_buildnumberFile + ".";

			throw new BuildException(message);
		}
	}

	/**
		 * Write the given <code>String</code> to the given <code>File</code>.
		 * Encoding is UTF-8.
		 */
	public void writeStringToFile(String s, File f)
		throws IOException
	{
		Writer out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
		try
		{
			out.write(s);
		}
		finally
		{
			out.close();
		}
	}

	public String readContents(Reader r)
	   throws IOException
	 {
	   StringBuffer sb = new StringBuffer();

	   char buf[] = new char[1000];
	   int charsRead;

	   while ((charsRead = r.read(buf)) > 0)
	   {
		 sb.append(buf, 0, charsRead);
	   }

	   return sb.toString();
	 }


	 /**
	  * Read the contents of <code>File</code> and return as a <code>String</code>.
	  * Encoding is assumed to be UTF-8.
	  */
	 public String readFileContents(File f)
	   throws IOException
	 {
	   Reader in = new InputStreamReader(new FileInputStream(f), "UTF-8");
	   try
	   {
		 return readContents(in);
	   }
	   finally
	   {
		 in.close();
	   }
	}
}

