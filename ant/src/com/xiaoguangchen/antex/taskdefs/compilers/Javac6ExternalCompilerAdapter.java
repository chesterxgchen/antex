package com.xiaoguangchen.antex.taskdefs.compilers;

import com.xiaoguangchen.antex.taskdefs.Javac6;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.compilers.DefaultCompilerAdapter;
import org.apache.tools.ant.types.Commandline;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

/**
 * The implementation of the javac6 compiler for JDK 1.6 using an external process
 *
 * mimic AptExternalCompilerAdapter
 *
 *
 */
public class Javac6ExternalCompilerAdapter extends DefaultCompilerAdapter {


    /**
     * Get the facade task that fronts this adapter
     *
     * @return task instance
     * @see DefaultCompilerAdapter#getJavac()
     */
    protected Javac6 getJavac6() {
        return (Javac6) getJavac();
    }

    /**
     * Performs a compile using the Javac externally.
     * @return true  the compilation was successful.
     * @throws org.apache.tools.ant.BuildException if there is a problem.
     */
    public boolean execute() throws BuildException {
        attributes.log("Using external javac6 compiler", Project.MSG_VERBOSE);


        // Setup the apt executable
        Javac6 javac6 = getJavac6();
        Commandline cmd = new Commandline();
        cmd.setExecutable(javac6.getJavac6Executable());
        
        setupModernJavacCommandlineSwitches(cmd);

        setJavacCommandlineSwitches(javac6, cmd);
        
        int firstFileName = cmd.size();
        //add the files
        logAndAddFilesToCompile(cmd);

        //run
        return 0 == executeExternalCompile(cmd.getCommandline(),  firstFileName,
                true);

    }


    /**
     * Using the front end arguments, set up the command line to run Apt
     *
     * @param javac6
     * @param cmd command that is set up with the various switches from the task
     *            options
     */
    protected void setJavacCommandlineSwitches(Javac6 javac6, Commandline cmd) {

        if (javac6.getProcessingOption() != null) {
            cmd.createArgument().setValue("-proc:" + javac6.getProcessingOption());
        }


        // Process the processor(s)
        Vector processors = javac6.getProcessors();
        Enumeration elements = processors.elements();
        Javac6.Processor processor;
        StringBuffer arg = null;

        int elmtsize = 0;
        if (elements.hasMoreElements()) {
            cmd.createArgument().setValue("-processor");
        }

        while (elements.hasMoreElements()) {
            elmtsize++;
            processor = (Javac6.Processor) elements.nextElement();
            arg = new StringBuffer();
            if (processor.getClassName() != null) {
                arg.append(processor.getClassName());
            }
            if (elements.hasMoreElements())
                arg.append(",");
        }

        if (elmtsize > 0 && arg != null)
            cmd.createArgument().setValue(arg.toString());
        
        // Process the processorpath
        Path factoryPath = javac6.getProcessorpath();
        if (factoryPath != null) {
            cmd.createArgument().setValue("-processorpath");
            cmd.createArgument().setPath(factoryPath);
        }

        File sourcegendir = javac6.getSourcegendir();
        if (sourcegendir != null) {
            cmd.createArgument().setValue("-s");
            cmd.createArgument().setFile(sourcegendir);
        }

        // Process the processor options
        Vector options = javac6.getOptions();
        elements = options.elements();
        Javac6.Option opt;
        arg = null;
        while (elements.hasMoreElements()) {
            opt = (Javac6.Option) elements.nextElement();
            arg = new StringBuffer();
            arg.append("-A").append(opt.getName());
            if (opt.getValue() != null) {
                arg.append("=").append(opt.getValue());
            }
            cmd.createArgument().setValue(arg.toString());
        }

        // Process the Jvmarg options
        Vector jvmargs = javac6.getJvmargs();
        elements = jvmargs.elements();
        Javac6.Jvmarg jvmarg;
        arg = null;
        while (elements.hasMoreElements()) {
            jvmarg = (Javac6.Jvmarg) elements.nextElement();
            arg = new StringBuffer();
            if (jvmarg.getValue() != null) {
                arg.append("-J").append(jvmarg.getValue());
            }
            cmd.createArgument().setValue(arg.toString());
        }
    }

}

