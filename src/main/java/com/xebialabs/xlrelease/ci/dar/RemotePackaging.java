/**
 * Copyright (c) 2013, XebiaLabs B.V., All rights reserved.
 *
 *
 * The Deployit plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/deployit-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xlrelease.ci.dar;

import java.io.File;

import hudson.remoting.Callable;

/**
 * Wrapper for the packaging operation.
 * It must be executed on the target system where the project artifact was built.
 * @see <a href="https://wiki.jenkins-ci.org/display/JENKINS/Distributed+builds">Jenkins distributed builds</a>
 */
public class RemotePackaging implements Callable<File, RuntimeException> {

    private File targetDir;


    public RemotePackaging forDeploymentPackage() {
        return this;
    }

    public RemotePackaging withTargetDir(File targetDir) {
        this.targetDir = targetDir;
        return this;
    }

    public RemotePackaging usingConfig() {
        return this;
    }

    public RemotePackaging usingDescriptors() {
        return this;
    }


    /**
     * Call to be executed via jenkins virtual channel
     */
    public File call() throws RuntimeException {
        targetDir.mkdirs();

        return null;
    }


}
