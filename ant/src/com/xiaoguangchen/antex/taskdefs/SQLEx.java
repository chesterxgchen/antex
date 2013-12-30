/**
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

package com.xiaoguangchen.antex.taskdefs;

import java.io.*;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.JDBCTask;
import org.apache.tools.ant.types.EnumeratedAttribute;
import org.apache.tools.ant.types.FileSet;

/**
 * This SQL extension is based on originally based on Apache Ant SQLExec. Here I modified
 * so it can handle SQL stored procedures, function and trigger and native SQLs.
 *
 * The SQLEx now can take an Oracle SQL PLus statement and run through this ant task and execute the
 * the same sql statement via JDBC. Similarly, this codes works for SQL Server and DB2.
 *
 * Executes a series of SQL callable statements on a database using JDBC
 *
 * <ul>
 *      <li>Statement:
 *          <p>Statements can either be read in from a text file using the <i>src</i> attribute or from
 * between the enclosing SQL tags.
 *          </p>
 *        <p> file redirect:
 *            support file redirect. For example in Oracle @ is used redirect. for example, @createuser.sql
 *            will load the createuser.sql and execute that file
          </p>

 *       </li>
 *      <li> Delimiter:
 *      <p> Multiple statements can be provided, separated by semicolons (or the
 *          defined <i>delimiter</i>). </p>
 *      <p> When mixing stored procedure or table creation syntax with select/update/delete
 *          SQL statement, there are two layers of delimiters: one is used to separate
 *          block of statement (such as create table, procedure etc): an outside delimeter;
 *          and another one for inner statement (delimiter used within stored procedures).
 *
 *          In most case; the inner delimiter would be semicolon. The outside delimiter can be mixed of semicolon ;
 *          with of the following GO (SQL Server), RUN (for ORACLE) or @ for DB2.
 *          Therefore the actually the delimiter needs to determined by the type of statement.</p>
 *       <p>One can also supply one sql statement as text without delimiter. In such case, one need to set
 *          allowNonDelimiterSQL=true. The default is false. One this option set to true. The whole SQL statement
 *          will be executed as one statement even though no delimiter is provided.
        </p>
 *
 *       </li>
 *        <li> Comments:
 *             <P> Individual lines within the statements can be commented using either --, //
 *                 or REM at the start of the line.
 *             <P> Database Vendor specific comment symbols can be configured: for example:
 *                 !echo, ! echo, @echo, echo (DB2)
 *                 !!echo, !! echo, (SQL Server)
 *                 PROMPT, show errors, . (Oracle)
 *             <p> Multiple lines can be commented using C-Style comments (Oracle style of comments)
 *
 *        </li>
 *        <li> stored procedure execution
 *             <P> Convert native sql stored procedure call into JDBC stored procedure call.
          </li>
 *        <li> keepConnectionOpen, keepStatementOpen
 *            <p> By default, the jdbc connect and statement are closed when execution is completed. But if set these options
 *                to be true, the sub-class can use the sqlex to return result sets and processing the resultset.
 *            </p>
 *        </li>
 *
 *        <li> autocommit:
 *             <p>The <i>autocommit</i> attribute specifies whether auto-commit should be turned on or
 *                off whilst executing the statements. If auto-commit is turned on each statement will
 *                be executed and committed. If it is turned off the statements will all be executed as one transaction.</p>
 *
 *        </li>
 *        <li> on error:
 *            <p>The <i>onerror</i> attribute specifies how to proceed when an error occurs during the execution
 *             of one of the statements. The possible values are: <b>continue</b> execution, only show the error;
 *             <b>stop</b> execution and commit transaction; and <b>abort</b> execution and transaction and fail task.
 *            </p>
 *        </li>
 * </ul>
 *
 *
 * @ant.task name="sqlex" category="database"
 */
public class SQLEx extends JDBCTask {

    /**
     * delimiters we support, "normal" and "row"
     */
    public static class DelimiterType extends EnumeratedAttribute {
        public static final String NORMAL = "normal";
        public static final String ROW = "row";
        public String[] getValues() {
            return new String[] {NORMAL, ROW};
        }
    }

    static final int OTHER          = 0;
    static final int ORACLE         = 1;
    static final int DB2            = 2;
    static final int MS_SQLSERVER   = 3;


    private int goodSql = 0;

    private int totalSql = 0;

    /**
     * if true, keep it open for sub-class to close it.
     * As subclass may need the results to generate
     */
    protected boolean keepConnectionOpen = false;

    /**
     * if true, keep it open for sub-class to close it.
     * As subclass may need the results to generate
     */
    protected boolean keepStatementOpen = false;

    /**
     * Database connection
     */
    protected Connection conn = null;

    /**
     * files to load
     */
    private Vector filesets = new Vector();

    /**
     * individual line comment symbol, line starts with this token with be removed
     */
    private Vector< LineCommentSymbol> lineCommentSymbols = new Vector< LineCommentSymbol>();


    /**
     * map specific string to other string
     * for example, map ORACLE "exec"
     * to "call" so that "exec mystoredprocedure" would converted to
     * { call mystoredprocedure" }
     */
    private Vector mappedTokens = new Vector();

    /**
     * redirect, redirect file to another file
     *
     * for example, in Oracle <pre> <code>@</code> </pre> is used
     * in test.sql contains  @testsql.sql
     * redirect to file testsql.sql for actual sql statement
     *
     */
    private String redirect = null;

    /**
     * SQL statement
     */
    protected CallableStatement cstatement = null;


    /**
     * allow non-delimiter SQL
     */
    private boolean allowNonDelimiterSQL =false;

    /**
     * SQL input file
     */
    private File srcFile = null;

    /**
     * SQL input command
     */
    private String sqlCommand = "";

    /**
     * SQL transactions to perform
     */
    private Vector transactions = new Vector();

    /**
     * SQL Statement delimiter
     */
    private String delimiter =  null;


    /**
     * The delimiter type indicating whether the delimiter will
     * only be recognized on a line by itself
     */
    private String delimiterType = DelimiterType.NORMAL;

    /**
     * Print SQL results.
     */
    private boolean print = false;

    /**
     * Print header columns.
     */
    private boolean showheaders = true;

    /**
     * Results Output file.
     */
    private File output = null;


    /**
     * Action to perform if an error is found
     **/
    private String onError = "abort";

    /**
     * Encoding to use when reading SQL statements from a file
     */
    private String encoding = null;

    /**
     * Append to an existing file or overwrite it?
     */
    private boolean append = false;

    /**
     * Keep the format of a sql block?
     */
    private boolean keepformat = false;

    /**
     * Argument to Statement.setEscapeProcessing
     *
     * @since Ant 1.6
     */
    private boolean escapeProcessing = true;


    protected void setKeepConnectionOpen(boolean open)
    {
       this.keepConnectionOpen = open;
    }

    protected void setKeepStatementOpen(boolean open)
    {
       this.keepStatementOpen = open;
    }

    /**
     * Set the name of the SQL file to be run.
     * Required unless statements are enclosed in the build file
     */
    public void setSrc(File srcFile) {
        this.srcFile = srcFile;
    }

    /**
     * Set an inline SQL command to execute.
     * NB: Properties are not expanded in this text.
     */
    public void addText(String sql) {
        this.sqlCommand += sql;
    }


    /**
     * Set the file redirect symbol, option
     */
    public void setRedirect(String symbol) {
        this.redirect = symbol;
    }



    /**
     * Adds a set of files (nested fileset attribute).
     */
    public void addFileset(FileSet set) {
        filesets.addElement(set);
    }


    /**
     * Add a SQL transaction to execute
     */
    public Transaction createTransaction() {
        Transaction t = new Transaction();
        transactions.addElement(t);
        return t;
    }

    /**
     * Add LineCommentSymbols
     */
    public LineCommentSymbol createLineCommentSymbol() {
        LineCommentSymbol t = new LineCommentSymbol();
        this.lineCommentSymbols.addElement(t);
        return t;
    }



    public TokenMapping createTokenMapping() {
      TokenMapping t = new TokenMapping();
      this.mappedTokens.addElement(t);
      return t;
    }



    /**
     * Set the file encoding to use on the SQL files read in
     *
     * @param encoding the encoding to use on the files
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * Set the delimiter that separates SQL statements. Defaults to &quot;;&quot;;
     * optional
     *
     * <p>For example, set this to "go" and delimitertype to "ROW" for
     * Sybase ASE or MS SQL Server.</p>
     */
    public void setDelimiter(String s) {
        this.delimiter = s;
    }

    /**
     * Set the delimiter type: "normal" or "row" (default "normal").
     *
     * <p>The delimiter type takes two values - normal and row. Normal
     * means that any occurrence of the delimiter terminate the SQL
     * command whereas with row, only a line containing just the
     * delimiter is recognized as the end of the command.</p>
     */
    public void setDelimiterType(DelimiterType delimiterType) {
        this.delimiterType = delimiterType.getValue();
    }

    /**
     * Print result sets from the statements;
     * optional, default false
     */
    public void setPrint(boolean print) {
        this.print = print;
    }

    /**
     * Print headers for result sets from the
     * statements; optional, default true.
     */
    public void setShowheaders(boolean showheaders) {
        this.showheaders = showheaders;
    }

    /**
     * Set the output file;
     * optional, defaults to the Ant log.
     */
    public void setOutput(File output) {
        this.output = output;
    }

    /**
     * whether output should be appended to or overwrite
     * an existing file.  Defaults to false.
     *
     * @since Ant 1.5
     */
    public void setAppend(boolean append) {
        this.append = append;
    }


    /**
     * Action to perform when statement fails: continue, stop, or abort
     * optional; default &quot;abort&quot;
     */
    public void setOnerror(OnError action) {
        this.onError = action.getValue();
    }

    /**
     * whether or not format should be preserved.
     * Defaults to false.
     *
     * @param keepformat The keepformat to set
     */
    public void setKeepformat(boolean keepformat) {
      this.keepformat = keepformat;
    }
    /**
     * Set escape processing for statements.
     *
     * @since Ant 1.6
     */
    public void setEscapeProcessing(boolean enable) {
        escapeProcessing = enable;
    }

    public void setAllowNonDelimiterSQL(boolean allow)
    {
      this.allowNonDelimiterSQL = allow;
    }


    public void mappingInit()
    {
        //predefined generic tokens:
        TokenMapping tm = new TokenMapping();
        tm.setFrom("exec");
        tm.setTo("call");

        this.mappedTokens.addElement(tm);

        tm = new TokenMapping();
        tm.setFrom("execute");
        tm.setTo("call");
        this.mappedTokens.addElement(tm);
    }

    public void commentInit()
    {
      String[] commentsymbols = new String[] {"--", "REM", "//" };

        for (String commentsymbol : commentsymbols) {
            LineCommentSymbol symbol = new LineCommentSymbol();
            symbol.setSymbol(commentsymbol);
            this.lineCommentSymbols.addElement(symbol);
        }
      String[] vendorsymbols = null;

      //vendor specific
      switch (getVendor()) {
        case ORACLE:
            vendorsymbols = new String[] {
                "show errors", ".", "PROMPT"};
            redirect = "@";
            delimiter = (delimiter == null) ? ";": delimiter;
            break;
          case MS_SQLSERVER:
            vendorsymbols = new String[] {
                "echo", "rem", "!!echo", "!! echo"};
            redirect = "osql -i";
            delimiter = (delimiter == null) ? "GO": delimiter;
            break;
          case DB2:
            vendorsymbols = new String[] {
                "echo", "@echo", "!echo", "! echo"};
            redirect = "db2 -stf";
            delimiter = (delimiter == null) ? "@": delimiter;
            break;

          case OTHER:
            redirect = null;
            break;
      }

      if (vendorsymbols != null)
      {
        for (int i = 0; i < vendorsymbols.length; ++i) {
           LineCommentSymbol symbol = new LineCommentSymbol();
           symbol.setSymbol(vendorsymbols[i]);
           this.lineCommentSymbols.addElement(symbol);
        }
      }
    }

    public void dbInit()
    {
      mappingInit();
      commentInit();
    }


    public int getVendor() throws BuildException
    {
      String jdbcUrl = getUrl();
      if (jdbcUrl == null || jdbcUrl.length() == 0)
        throw new BuildException("jdbc url must be provided", getLocation());

      if (jdbcUrl.trim().startsWith("jdbc:oracle"))
          return ORACLE;
      else if (jdbcUrl.trim().startsWith("jdbc:db2"))
          return DB2;
      else if (jdbcUrl.trim().startsWith("jdbc:microsoft:sqlserver"))
          return MS_SQLSERVER;
      else
         return OTHER;

    }

    public void validate() throws BuildException
    {
      if (srcFile == null && sqlCommand.length() == 0  && filesets.isEmpty())
      {
        if (transactions.size() == 0)
        {
          throw new BuildException("Source file or fileset, "
                                   + "transactions or sql statement "
                                   + "must be set!", getLocation());
        }
      }

      if (srcFile != null && !srcFile.exists()) {
        throw new BuildException("Source file does not exist!", getLocation());
      }
    }

    /**
     * Load the sql file and then execute it
     */
    public void execute() throws BuildException {


        dbInit();

        Vector savedTransaction = (Vector) transactions.clone();
        String savedSqlCommand = sqlCommand;
        sqlCommand = sqlCommand.trim();

        try {

            validate();

            // deal with the filesets
            for (int i = 0; i < filesets.size(); i++) {
                FileSet fs = (FileSet) filesets.elementAt(i);
                DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                File srcDir = fs.getDir(getProject());

                String[] srcFiles = ds.getIncludedFiles();

                // Make a transaction for each file
                for (int j = 0; j < srcFiles.length; j++) {
                    Transaction t = createTransaction();
                    t.setSrc(new File(srcDir, srcFiles[j]));
                }
            }

            // Make a transaction group for the outer command
            Transaction t = createTransaction();
            t.setSrc(srcFile);
            t.addText(sqlCommand);
            conn = getConnection();
            if (!isValidRdbms(conn)) {
                return;
            }

            try {
                PrintStream out = System.out;

                try {
                    if (output != null) {
                        log("Opening PrintStream to output file " + output,  Project.MSG_VERBOSE);
                        out = new PrintStream(
                                  new BufferedOutputStream(
                                      new FileOutputStream(output
                                                           .getAbsolutePath(),
                                                           append)));
                    }

                    // Process all transactions
                    for (Enumeration e = transactions.elements();
                         e.hasMoreElements();) {

                        ((Transaction) e.nextElement()).runTransaction(out);
                        if (!isAutocommit()) {
                            log("Committing transaction", Project.MSG_VERBOSE);
                            conn.commit();
                        }
                    }
                } finally {
                    if (out != null && out != System.out) {
                        out.close();
                    }
                }

            } catch (IOException e) {
                if (!isAutocommit() && conn != null && onError.equals("abort")) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                throw new BuildException(e, getLocation());
            } catch (SQLException e) {
                if (!isAutocommit() && conn != null && onError.equals("abort")) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
                throw new BuildException(e, getLocation());
            } finally {
                try {
                    if (cstatement != null && !keepStatementOpen) {
                        cstatement.close();
                    }
                    if (conn != null && !keepConnectionOpen) {
                        conn.close();
                    }
                } catch (SQLException ex) {
                    // ignore
                }
            }

            log(goodSql + " of " + totalSql
                + " SQL statements executed successfully");
        } finally {
            transactions = savedTransaction;
            sqlCommand = savedSqlCommand;
            try {
              if (conn != null && !keepConnectionOpen)
                conn.close();
            }
            catch (SQLException ignore) {}
        }
    }

    private boolean isStartWithCommentSymbol(String line)
    {
      if (line == null || "".equals(line))
        return false;

      line = line.trim();

      for ( LineCommentSymbol s: lineCommentSymbols )
      {
        if (!s.getCaseSensitive())
        {
          if (line.toUpperCase().startsWith(s.getSymbol().toUpperCase()))
            return true;
        }
        else {
          if (line.startsWith(s.getSymbol()))
            return true;
        }
      }

      return false;
    }

    private boolean isCallable(String sql)
    {
      sql = sql.trim();
      boolean callable = sql.toLowerCase().startsWith("call ");
      if (callable) return callable;

      if (sql.startsWith("{"))
      {
        return sql.substring(1).startsWith("call ");
      }

      return false;

    }

    private String covert(String sql )
    {
      if (sql == null || "".equals(sql))
        return sql;

      sql = sql.trim();

      for (Iterator it=mappedTokens.iterator(); it.hasNext(); )
      {

        Object obj = it.next();

        TokenMapping convertToken = (TokenMapping) obj;

        int index = -1;
        if (!convertToken.getCaseSensitive())
        {
            index = sql.toLowerCase().indexOf(convertToken.getFrom().toLowerCase());
        }
        else{
            index = sql.indexOf(convertToken.getFrom());
        }

        if (index >= 0)
        {
            if (convertToken.getFirstWordOnly()) // only need to covert firstword
            {
                if (convertToken.getMatchWholeWord())
                {

                    String[] tokens = sql.split(" ");
                    boolean match = convertToken.getCaseSensitive() ?
                                    tokens[0].equals(convertToken.getFrom()):
                                    tokens[0].equalsIgnoreCase(convertToken.getFrom());

                    if (match) {
                        tokens[0] = convertToken.getTo();

                        StringBuffer sb = new StringBuffer();

                        for (String token : tokens) sb.append(token).append(" ");

                        return sb.toString();
                    }
                }
                else {
                    return sql.replaceFirst(convertToken.getFrom(), convertToken.getTo());
                }
              }
              else {
                    sql = sql.replaceAll(convertToken.getFrom(), convertToken.getTo());
              }

         }
      } // end of for loop

      return sql;

    }


    public String parseSQL(String sql)
    {
      // convert only first token to proper syntax, such EXEC to call
      sql = covert(sql);
      if (isCallable(sql))
      {
        StringBuffer sb = new StringBuffer("{");
        sb.append(sql).append(" }");

        sql = sb.toString();
      }

      return sql;
    }



    /**
     * Exec the sql statement.
     */
    protected void execSQL(String sql, List args, PrintStream out) throws SQLException
    {
        // trim space, strored procedurs is very sensitive to front white space
        sql = sql.trim();
        // Check and ignore empty statements
        if ("".equals(sql)) {
            return;
        }

        ResultSet resultSet = null;
        try {

            totalSql++;
            log("SQL: " + sql, Project.MSG_VERBOSE);


            cstatement = conn.prepareCall(sql);

            if (args != null && !args.isEmpty()) {
                ListIterator it = args.listIterator();
                while (it.hasNext()) {
                    int index = it.nextIndex();
                    cstatement.setObject(index + 1, it.next());
                }
            }

            int updateCount = 0, updateCountTotal = 0;
            int returncode = cstatement.executeUpdate();
            updateCount = cstatement.getUpdateCount();
            resultSet = cstatement.getResultSet();

            boolean ret = (resultSet != null);
            do {
              if (!ret) {
                if (updateCount != -1) {
                  updateCountTotal += updateCount;
                }
              } else {
                if (print) {
                  printResults(resultSet, out);
                }
              }
              ret = cstatement.getMoreResults();
              if (ret) {
                updateCount = cstatement.getUpdateCount();
                resultSet = cstatement.getResultSet();
              }
            } while (ret);

            log(updateCountTotal + " rows affected", Project.MSG_VERBOSE);

            if (print) {
                StringBuffer line = new StringBuffer();
                line.append(updateCountTotal).append(" rows affected");
                out.println(line);
            }

            SQLWarning warning = conn.getWarnings();
            while (warning != null) {
                log(warning + " sql warning", Project.MSG_VERBOSE);
                warning = warning.getNextWarning();
            }

            conn.clearWarnings();
            goodSql++;

        } catch (SQLException e) {

          log("Failed to execute: " + sql, Project.MSG_ERR);

          e.printStackTrace();

          if (!onError.equals("continue")) {
            throw e;
          }

          log(e.toString(), Project.MSG_ERR);
        }
        finally {
          if (cstatement != null && !keepStatementOpen) {
            cstatement.close();
          }
        }
    }



    /**
     * print any results in the statement
     * @deprecated use {@link #printResults(java.sql.ResultSet, java.io.PrintStream)
     *             the two arg version} instead.
     * @param out the place to print results
     * @throws SQLException on SQL problems.
     */
    protected void printResults(PrintStream out) throws SQLException {
        ResultSet rs = null;
        rs = cstatement.getResultSet();
        try {
            printResults(rs, out);
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * print any results in the result set.
     * @param rs the resultset to print information about
     * @param out the place to print results
     * @throws SQLException on SQL problems.
     * @since Ant 1.6.3
     */
    protected void printResults(ResultSet rs, PrintStream out) throws SQLException {
        if (rs != null) {
            log("Processing new result set.", Project.MSG_VERBOSE);
            ResultSetMetaData md = rs.getMetaData();
            int columnCount = md.getColumnCount();
            StringBuffer line = new StringBuffer();
            if (showheaders) {
                for (int col = 1; col < columnCount; col++) {
                     line.append(md.getColumnName(col));
                     line.append(",");
                }
                line.append(md.getColumnName(columnCount));
                out.println(line);
                line = new StringBuffer();
            }
            while (rs.next()) {
                boolean first = true;
                for (int col = 1; col <= columnCount; col++) {
                    String columnValue = rs.getString(col);
                    if (columnValue != null) {
                        columnValue = columnValue.trim();
                    }

                    if (first) {
                        first = false;
                    } else {
                        line.append(",");
                    }
                    line.append(columnValue);
                }
                out.println(line);
                line = new StringBuffer();
            }
        }
        out.println();
    }


    /**
     * The action a task should perform on an error,
     * one of "continue", "stop" and "abort"
     */
    public static class OnError extends EnumeratedAttribute {
        public String[] getValues() {
            return new String[] {"continue", "stop", "abort"};
        }
    }



    ///////////////////////////////////////////////////////////////////////

    /**
     * Contains the definition of a new transaction element.
     * Transactions allow several files or blocks of statements
     * to be executed using the same JDBC connection and commit
     * operation in between.
     */
    public class Transaction {

        private File     tSrcFile = null;
        private String   tSqlCommand = "";

        private Vector   tArgs = new Vector();
        //Transaction delimiter, it not set use sql level delimiter
        private String   tDelimiter = null;

        public Transaction()
        {
          //default to outl sql delimiter
          this.tDelimiter = delimiter;
        }


        public void setSrc(File src) {
            this.tSrcFile = src;
        }

        public void addText(String sql) {
            this.tSqlCommand += sql;
        }

        public void setTransactionDelimiter(String s) {
          this.tDelimiter = s;
        }

        protected void runTransaction(PrintStream out)
            throws IOException, SQLException {

            if (tSqlCommand.length() != 0) {
                log("Executing commands", Project.MSG_INFO);
                runStatements(new StringReader(tSqlCommand), out);
            }

            if (tSrcFile != null) {
                log("Executing file: " + tSrcFile.getAbsolutePath(),  Project.MSG_INFO);
                Reader reader =
                    (encoding == null) ? new FileReader(tSrcFile)
                                       : new InputStreamReader( new FileInputStream(tSrcFile), encoding);
                try {
                    runStatements(reader, out);
                } finally {
                    reader.close();
                }
            }
        }

        protected String getExecDelimiter()
        {
            return (tDelimiter == null) ? delimiter: tDelimiter;
        }

        protected String getStatementExecDelimiter(String sql)
        {
            String execDelimiter = getExecDelimiter();

            String sql2 = sql.toUpperCase();
            if (sql2 != null && sql2.length() > 0)
            {
                if (sql2.startsWith("CREATE OR REPLACE") ||
                    sql2.startsWith("CREATE TRIGGER") ||
                    sql2.startsWith("CREATE PROCEDURE") ||
                    sql2.startsWith("CREATE FUNCTION") )
                {
                    execDelimiter = delimiter; // SQL delimiter (outer delimiter)
                    return execDelimiter;
                }

                //try again by take out the white spaces
                int createIndex = sql2.indexOf("CREATE");
                if (sql2.startsWith("CREATE") )
                {
                    String createStr = sql2.substring(createIndex).trim();
                    if (createStr.startsWith("OR") ||
                        createStr.startsWith("TRIGGER") ||
                        createStr.startsWith("PROCEDURE") ||
                        createStr.startsWith("FUNCTION"))
                    {

                        execDelimiter = delimiter;
                        return execDelimiter;
                    }
                }
            }

            return execDelimiter;
        }

        // start a new transaction for the redirect file
        protected void processRedirectFile(String filename, PrintStream out)
            throws SQLException, IOException
        {
          Transaction t = new Transaction();
          //set parent deliminator to the nested transimiteDeliminator
          t.setTransactionDelimiter(this.tDelimiter);

          // trim off whitespace
          filename = filename.trim();

          String sqlExecDelimiter = getStatementExecDelimiter(filename);

          // ignore arguments (such as sqlplus arguments) if any
          int wsIndex = filename.indexOf(" ");
          if (filename != null && wsIndex > 0 ) // has white space
          {
              filename = filename.substring(0, wsIndex);
          }

          int deIndex = filename.indexOf(sqlExecDelimiter);

          if (filename != null && deIndex > 0 ) // has demilimiter
          {
              filename = filename.substring(0, deIndex);
          }
          if (tSrcFile != null)
              t.setSrc(new File(tSrcFile.getParentFile(), filename));
          else
              t.setSrc(new File(filename));

            t.runTransaction(out);
        }

        // start a new transaction for the redirect file
        /**
         *
         * @param sqlbuf StringBuffer, resulting sql statement
         * @param line String, current line read from the file
         * @param inCommentBlock boolean, indicating that if the current line is in a comment block
         *        if inCommentBlock = true. This is the case when comment started from previous line
         *        but did not end in previous line.
         * @return String, return remaining sql statement after comments;
         * @throws SQLException
         * @throws IOException
         */
        protected boolean processBlockComments(StringBuffer sqlbuf, String line, boolean inCommentBlock)
            throws SQLException, IOException
        {

          /* C-Style COMMENTS*/
          int startIndex = line.indexOf("/*");

          if (inCommentBlock && startIndex >= 0)
          {
            log(" comments must ends with */ before start another commment with /* ", Project.MSG_VERBOSE);
          }

          if (startIndex >= 0)
          {
            inCommentBlock = true;
            String preline=line.substring(0, startIndex);

            if (!"".equals(preline)) sqlbuf.append(" ").append(preline);

            line = line.substring(startIndex+2);
            //reset startIndex
            startIndex = -1;
          }

          int endIndex = line.indexOf("*/");

          if (!inCommentBlock && endIndex >= 0) {
            log(" comments must start with /* before ends with /* ", Project.MSG_VERBOSE);
          }

          if (inCommentBlock && endIndex >= 0) {
            inCommentBlock =false;
            line=line.substring(endIndex + 2);
            inCommentBlock= processBlockComments(sqlbuf, line, inCommentBlock);
          }

          if (inCommentBlock && keepformat ) {
            if (!"".equals(line))
                sqlbuf.append(" ").append(line);

            line = "";
          }

          if (!inCommentBlock)
            sqlbuf.append(line);

          return inCommentBlock;
        }


         protected void runStatements(Reader reader, PrintStream out )
            throws SQLException, IOException
        {

            StringBuffer sql = new StringBuffer();
            String line = "";

            BufferedReader in = new BufferedReader(reader);

            boolean inBlockComments = false;
            while ((line = in.readLine()) != null)
            {

              if (!keepformat) {
                 line = line.trim();
                 sql.append(" ");
               } else {
                 sql.append("\n");
               }

               if (redirect != null && line.startsWith(redirect) )
               {
                 String filename = line.substring(redirect.length());
                 processRedirectFile(filename,out);
               }
               else {
                 boolean startedWithCommentSymbol = isStartWithCommentSymbol(line);
                 line = getProject().replaceProperties(line);

                 if (startedWithCommentSymbol)
                  continue;

                inBlockComments = processBlockComments(sql, line, inBlockComments);

                // SQL defines "--" as a comment to EOL
                // and in Oracle it may contain a hint
                // so we cannot just remove it, instead we must end it


                  if (line.indexOf("--") >= 0) {
                    sql.append("\n");
                  }
                  else {                     //take care tailing while space
                    sql.replace(0, sql.length(), sql.toString().trim());
                  }

               // determine which level deimiter (Outer SQL, or transaction level) to use
               // depending on sql syntax

               String sqlstr = sql.toString();
               String sqlExecDelimiter = getStatementExecDelimiter(sqlstr);

                   if (delimiterType != null && sqlExecDelimiter != null) {
                       if ((delimiterType.equals(DelimiterType.NORMAL)
                               && sqlstr.toUpperCase().endsWith(sqlExecDelimiter.toUpperCase()))
                               ||
                               (delimiterType.equals(DelimiterType.ROW)
                                       && line.trim().equalsIgnoreCase(sqlExecDelimiter))) {
                           // not support argument yet.
                           sqlstr = parseSQL(sql.substring(0, sql.length() - sqlExecDelimiter.length()));
                           execSQL(sqlstr, null, out);
                           sql.replace(0, sql.length(), "");
                           sqlstr = null;
                       }
                   }

               }

                // Catch any statements not followed by ;
              if (!sql.equals("") && allowNonDelimiterSQL)
              {
                String sqlstr = parseSQL(sql.toString());
                execSQL(sqlstr, null, out);
              }
            }

        }



     /**
      * Add nested args
      */
     public Arg createArg() {
       Arg t = new Arg();
       this.tArgs.addElement(t);
       return t;
     }

     //argument
     public class Arg {
       private String arg  = null; //default

       public void setArg(String arg) {
           this.arg = arg;
       }

       public String getArg() {
         return this.arg;
       }
     }

    } // end of Transaction class

    /**
     * LineCommentSymbol
     */
    public class LineCommentSymbol {
      private String    symbol = "";
      private boolean   caseSensitive  = false;

      public void setSymbol(String symbol) {
        this.symbol = symbol;
      }

      public String getSymbol() {
        return this.symbol;
      }

      public void addText(String symbol) {
              this.symbol =symbol;
     }

     public void setCaseSensitive(boolean caseSensitive)
     {
       this.caseSensitive = caseSensitive;
     }

     public boolean getCaseSensitive()
     {
       return this.caseSensitive;
     }


   }

   public class TokenMapping {
     private String    from = ""; // map from
     private String    to   = "";   // map to

     private boolean   caseSensitive  = false;
     private boolean   matchWholeWord = true;
     private boolean   firstWordOnly  = false;

     public void setFrom(String from)
     {
       this.from = from;
     }

     public void setTo(String to)
     {
       this.to = to;
     }

     public String getFrom()
     {
       return this.from;
     }

     public String getTo()
     {
       return this.to;
     }

     public void setCaseSensitive(boolean caseSensitive)
     {
       this.caseSensitive = caseSensitive;
     }

     public boolean getCaseSensitive()
     {
       return this.caseSensitive;
     }

     public void setMatchWholeWord(boolean matchWholeWord)
     {
       this.matchWholeWord = matchWholeWord;
     }

     public boolean getMatchWholeWord()
     {
       return this.matchWholeWord;
     }

     public void setFirstWordOnly(boolean firstWordOnly)
     {
       this.firstWordOnly = firstWordOnly;
     }

     public boolean getFirstWordOnly()
     {
       return this.firstWordOnly;
     }

   }


}
