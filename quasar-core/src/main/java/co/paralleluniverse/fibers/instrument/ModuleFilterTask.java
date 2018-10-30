/*
 * Quasar: lightweight threads and actors for the JVM.
 * Copyright (c) 2018, Parallel Universe Software Co. All rights reserved.
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
package co.paralleluniverse.fibers.instrument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

/**
 * <p>
 * Instrumentation ANT task</p>
 *
 */
public class ModuleFilterTask extends Task {
    private final ArrayList<FileSet> filesets = new ArrayList<>();
    private String mod;

    public void addFileSet(FileSet fs) {
        filesets.add(fs);
    }

    public void setModule(String mod) {
        this.mod = mod;
    }

    @Override
    public void execute() throws BuildException {
        try {
            final List<URL> urls = new ArrayList<>();


            for (FileSet fs : filesets) {
                final DirectoryScanner ds = fs.getDirectoryScanner(getProject());
                final String[] includedFiles = ds.getIncludedFiles();

                for (String filename : includedFiles) {
                    if (filename.endsWith("module-info.class")) {
                        File file = new File(fs.getDir(), filename);
                        if (file.isFile()) {
                            filter(file);
                        } else
                            log("File not found: " + filename);
                    }
                }
            }

        } catch (Exception ex) {
            log(ex.getMessage());
            throw new BuildException(ex.getMessage(), ex);
        }
    }

    private void filter(File file) {
        try {
            ClassWriter cw = null;
            try (FileInputStream fis = new FileInputStream(file)) {
                ClassReader cr = new ClassReader(fis);
                cw = new ClassWriter(cr, 0);
                cr.accept(new ClassVisitor(Opcodes.ASM7, cw) {
                    @Override
                    public ModuleVisitor visitModule(String name, int access, String version) {
                        return new ModuleVisitor(Opcodes.ASM7, super.visitModule(name, access, version)) {
                            @Override
                            public void visitRequire(String module, int access, String version) {
                                if (!module.contains(mod)) {
                                    super.visitRequire(module, access, version);
                                }
                            }
                        };
                    }
                }, 0);
            }
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(cw.toByteArray());
            }
        } catch (IOException ex) {
            throw new BuildException("Filterin module-info file " + file, ex);
        }
    }
}
