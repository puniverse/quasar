/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2013-2015, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *  
 *   or (per the licensee's choosing)
 *  
 * under the terms of the GNU Lesser General Public License version 3.0
 * as published by the Free Software Foundation.
 */
 /*
 * Copyright (c) 2008-2013, Matthias Mann
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Matthias Mann nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package co.paralleluniverse.fibers.instrument;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Instrumentation ANT task</p>
 *
 * <p>
 * It requires one or more FileSet elements pointing to class files that should
 * be instrumented.</p>
 * <p>
 * Classes that are referenced from the instrumented classes are searched in
 * the classpath of the task. If a referenced class is not found a warning is
 * generated and the instrumentation will result in less efficent code.</p>
 *
 * The following options can be set:<ul>
 * <li>check - default: false<br>The resulting code is run through a verifier.</li>
 * <li>verbose - default: false<br>The name of each processed class and all suspendable method calles is displayed.</li>
 * <li>debug - default: false<br>Prints internal debugging information.</li>
 * <li>allowmonitors - default: false<br>Allows the use of synchronized statements - this is DANGEROUS !</li>
 * <li>allowblocking - default: false<br>Allows the use known blocking calls like Thread.sleep, Object.wait etc.</li>
 * </ul>
 *
 * @see <a href="http://ant.apache.org/manual/CoreTypes/fileset.html">ANT FileSet</a>
 * @author Matthias Mann
 */
public class InstrumentationTask extends Task {
    private final ArrayList<FileSet> filesets = new ArrayList<>();
    private boolean check;
    private boolean verbose;
    private boolean allowMonitors;
    private boolean allowBlocking;
    private boolean debug;
    private boolean writeClasses = true;
    private final ArrayList<WorkListEntry> workList = new ArrayList<>();

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setAllowMonitors(boolean allowMonitors) {
        this.allowMonitors = allowMonitors;
    }

    public void setAllowBlocking(boolean allowBlocking) {
        this.allowBlocking = allowBlocking;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setWriteClasses(boolean writeClasses) {
        this.writeClasses = writeClasses;
    }

    @Override
    public void execute() throws BuildException {
        try {
            final List<URL> urls = new ArrayList<>();
            for (FileSet fs : filesets)
                urls.add(fs.getDir().toURI().toURL());
            final ClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
            final QuasarInstrumentor instrumentor = new QuasarInstrumentor(true);

            instrumentor.setCheck(check);
            instrumentor.setVerbose(verbose);
            instrumentor.setDebug(debug);
            instrumentor.setAllowMonitors(allowMonitors);
            instrumentor.setAllowBlocking(allowBlocking);
            instrumentor.setLog(new Log() {
                @Override
                public void log(LogLevel level, String msg, Object... args) {
                    final int msgLevel;
                    switch (level) {
                        case DEBUG:
                            msgLevel = Project.MSG_DEBUG;
                            break;
                        case INFO:
                            msgLevel = Project.MSG_INFO;
                            break;
                        case WARNING:
                            msgLevel = Project.MSG_WARN;
                            break;
                        default:
                            throw new AssertionError("Unhandled log level: " + level);
                    }
                    InstrumentationTask.this.log(level + ": " + String.format(msg, args), msgLevel);
                }

                @Override
                public void error(String msg, Throwable ex) {
                    InstrumentationTask.this.log("ERROR: " + msg, ex, Project.MSG_ERR);
                }
            });

            for (FileSet fs : filesets) {
                final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                final String[] includedFiles = ds.getIncludedFiles();

                for (String filename : includedFiles) {
                    if (filename.endsWith(".class")) {
                        File file = new File(fs.getDir(), filename);
                        if (file.isFile()) {
                            final String className = instrumentor.checkClass(cl, file);
                            workList.add(new WorkListEntry(className, file));
                        } else
                            log("File not found: " + filename);
                    }
                }
            }

            instrumentor.log(LogLevel.INFO, "Instrumenting " + workList.size() + " classes");

            for (WorkListEntry f : workList)
                instrumentClass(cl, instrumentor, f);

        } catch (Exception ex) {
            log(ex.getMessage());
            throw new BuildException(ex.getMessage(), ex);
        }
    }

    private void instrumentClass(ClassLoader cl, QuasarInstrumentor instrumentor, WorkListEntry entry) {
        if (!instrumentor.shouldInstrument(entry.name))
            return;
        try {
            try (FileInputStream fis = new FileInputStream(entry.file)) {
                final byte[] newClass = instrumentor.instrumentClass(cl, entry.name, fis);

                if (writeClasses) {
                    try (FileOutputStream fos = new FileOutputStream(entry.file)) {
                        fos.write(newClass);
                    }
                }
            }
        } catch (IOException ex) {
            throw new BuildException("Instrumenting file " + entry.file, ex);
        }
    }

    public static class WorkListEntry {
        public final String name;
        public final File file;

        public WorkListEntry(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }
}
