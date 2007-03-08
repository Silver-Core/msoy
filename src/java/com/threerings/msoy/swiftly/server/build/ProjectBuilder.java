//
// $Id$

package com.threerings.msoy.swiftly.server.build;

import com.threerings.msoy.swiftly.data.BuildResult;

import java.io.File;

/**
 * Defines the project builder interface
 */
public interface ProjectBuilder
{
    /**
     * Build the given project in the provided build directory. It is the
     * caller's responsibility to clean this directory.
     */
    public BuildResult build (File buildDir) throws ProjectBuilderException;
}
