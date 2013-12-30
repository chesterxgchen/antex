package com.xiaoguangchen.antex.utils;

import java.net.InetAddress;
import java.util.Locale;

/**
 * <p>Copyright: Copyright (c) 2006 Managing Digital Content LLC.</p>
 * <p/>
 * <p>Company: Managing Digital Content LLC. </p>
 * $Id: $
 *
 * @author chester chen (xiaoguang chen) chesterxgchen@yahoo.com
 */
public class NetUtils {

	public static final String WINDOWS  = "windows";
	public static final String UNIX     = "unix";
	public static final String LINUX    = "linux";
	public static final String MAC      = "mac";
	/**
	 * return a low case os name
	 */
	public static String getOS()
	{
	  String os = System.getProperty("os.name");
	  if (os.toUpperCase(Locale.ENGLISH).startsWith("WINDOWS"))
		os = WINDOWS;
	  else if (os.toUpperCase(Locale.ENGLISH).startsWith("UNIX"))
		os = UNIX;
	  else if (os.toUpperCase(Locale.ENGLISH).startsWith("LINUX"))
		os = LINUX;
      else if (os.toUpperCase(Locale.ENGLISH).startsWith("MAC"))
		os = MAC;
	  return os;
	}

	/**
	 * return a low case os name
	 */
	public static boolean isWindows()
	{
	  return WINDOWS.equals(getOS());
	}

	/**
	 * return a low case os name
	 */
	public static boolean isUnix()
	{
		String os = getOS();
		return UNIX.equals(os) || LINUX.equals(os);
	}


	/**
	 * getLocalHostName return the local host name. It returns only
	 * the name without the fully qualified DNS name.
	 *
	 * @return String local host name in lowercase.
	 *

	 */
	public static String getLocalHostName() throws java.net.UnknownHostException
	{

		String hostname = InetAddress.getLocalHost().getHostName().toLowerCase();
		int periodPos = hostname.indexOf('.');
		if (periodPos != -1){
			hostname = hostname.substring(0, periodPos);
		}
		return hostname;
	}

	/**
	 * getFullyQualifiedLocalHostName gets the fully qualified domain name.
	 * This is a best effort method, meaning we may not be able to return
	 * the FQDN depending on the underlying system configuration.
	 *
	 * @return String  fully qualified local host name in lowercase.
	 */
	public static String getFullyQualifiedLocalHostName() throws java.net.UnknownHostException
	{
		 return InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
	}


	public static String getLocalHostDomainName() throws java.net.UnknownHostException
	{
		String fqLocalHostName =  InetAddress.getLocalHost().getCanonicalHostName().toLowerCase();
		assert fqLocalHostName != null;

		String domainName = null;
		int index = fqLocalHostName.indexOf(".");
		if (index >=0) {
			domainName =  fqLocalHostName.substring(index + 1);
		}

		return domainName;
	}

}
