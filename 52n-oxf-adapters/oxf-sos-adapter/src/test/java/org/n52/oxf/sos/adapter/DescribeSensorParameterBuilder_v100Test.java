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
package org.n52.oxf.sos.adapter;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;
import org.n52.oxf.sos.adapter.wrapper.builder.DescribeSensorParamterBuilder_v100;

/**
 * Test of correctness for:
 * 		- legal and illegal constructor parameters
 * 		- applying and getting parameters
 * 
 * @author Eric
 */
public class DescribeSensorParameterBuilder_v100Test {

	/**
	 * Checks the behaviour on valid constructor parameters.
	 */
	@Test
	public void testValidConstructorParameters() {
		new DescribeSensorParamterBuilder_v100("", "");
	}
	
	/**
	 * Checks the behaviour on invalid constructor parameters.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidConstructorParameters() {
		new DescribeSensorParamterBuilder_v100(null, null);
		new DescribeSensorParamterBuilder_v100("", null);
		new DescribeSensorParamterBuilder_v100(null, "");
	}
	
	/**
	 * Checks, whether the mandatory parameters were applied correctly.
	 */
	@Test
	public void testApplyingAndGettingMandatoryParameters() {
		DescribeSensorParamterBuilder_v100 dspb = new DescribeSensorParamterBuilder_v100
				("sensorId", DescribeSensorParamterBuilder_v100.OUTPUT_FORMAT_SENSORML);
		
		HashMap<String, String> hm = (HashMap<String, String>) dspb.getParameters();
		String parMan_01 = hm.get(ISOSRequestBuilder.DESCRIBE_SENSOR_PROCEDURE_PARAMETER);
		String parMan_02 = hm.get(ISOSRequestBuilder.DESCRIBE_SENSOR_OUTPUT_FORMAT);
		
		assertEquals("sensorId", parMan_01);
		assertEquals(DescribeSensorParamterBuilder_v100.OUTPUT_FORMAT_SENSORML, parMan_02);
	}

}
