/**
 * ﻿Copyright (C) 2012
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */
package org.n52.oxf.sos.util;

public class SosUtil {

    /**
     * The Type of the service which is connectable by this ServiceAdapter
     */
    public static final String SERVICE_TYPE = "SOS";
    
    /**
     * the Versions of the services which are connectable by this ServiceAdapter.
     * Array contains: [1.0.0, 2.0.0]
     */
    public static final String[] SUPPORTED_VERSIONS = { "1.0.0", "2.0.0" };

    public static boolean isVersion100(String version) {
        return SUPPORTED_VERSIONS[0].equals(version);
    }

    public static boolean isVersion200(String version) {
        return SUPPORTED_VERSIONS[1].equals(version);
    }

}
