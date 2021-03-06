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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import net.opengis.fes.x20.ComparisonOperatorType;
import net.opengis.fes.x20.ComparisonOperatorsType;
import net.opengis.fes.x20.FilterCapabilitiesDocument.FilterCapabilities;
import net.opengis.fes.x20.ScalarCapabilitiesType;
import net.opengis.fes.x20.SpatialCapabilitiesType;
import net.opengis.fes.x20.TemporalCapabilitiesType;
import net.opengis.gml.x32.CodeType;
import net.opengis.gml.x32.EnvelopeDocument;
import net.opengis.gml.x32.EnvelopeType;
import net.opengis.gml.x32.TimePeriodType;
import net.opengis.ows.x11.AllowedValuesDocument.AllowedValues;
import net.opengis.ows.x11.ContactType;
import net.opengis.ows.x11.DCPDocument;
import net.opengis.ows.x11.DomainType;
import net.opengis.ows.x11.LanguageStringType;
import net.opengis.ows.x11.OperationDocument;
import net.opengis.ows.x11.RequestMethodType;
import net.opengis.ows.x11.ResponsiblePartySubsetType;
import net.opengis.ows.x11.ValueType;
import net.opengis.sos.x20.CapabilitiesDocument;
import net.opengis.sos.x20.CapabilitiesType;
import net.opengis.sos.x20.CapabilitiesType.Contents;
import net.opengis.sos.x20.ContentsType;
import net.opengis.sos.x20.ObservationOfferingDocument;
import net.opengis.sos.x20.ObservationOfferingType;
import net.opengis.sos.x20.ObservationOfferingType.ResultTime;
import net.opengis.swes.x20.AbstractContentsType.Offering;
import net.opengis.swes.x20.AbstractOfferingType.RelatedFeature;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.xmlbeans.XmlException;
import org.n52.oxf.OXFException;
import org.n52.oxf.ows.ServiceDescriptor;
import org.n52.oxf.ows.capabilities.Contact;
import org.n52.oxf.ows.capabilities.DCP;
import org.n52.oxf.ows.capabilities.DatasetParameter;
import org.n52.oxf.ows.capabilities.GetRequestMethod;
import org.n52.oxf.ows.capabilities.IBoundingBox;
import org.n52.oxf.ows.capabilities.IDiscreteValueDomain;
import org.n52.oxf.ows.capabilities.ITime;
import org.n52.oxf.ows.capabilities.OnlineResource;
import org.n52.oxf.ows.capabilities.Operation;
import org.n52.oxf.ows.capabilities.OperationsMetadata;
import org.n52.oxf.ows.capabilities.Parameter;
import org.n52.oxf.ows.capabilities.PostRequestMethod;
import org.n52.oxf.ows.capabilities.RequestMethod;
import org.n52.oxf.ows.capabilities.ServiceContact;
import org.n52.oxf.ows.capabilities.ServiceIdentification;
import org.n52.oxf.ows.capabilities.ServiceProvider;
import org.n52.oxf.sos.capabilities.ObservationOffering;
import org.n52.oxf.sos.capabilities.SOSContents;
import org.n52.oxf.util.web.HttpClient;
import org.n52.oxf.util.web.HttpClientException;
import org.n52.oxf.util.web.SimpleHttpClient;
import org.n52.oxf.valueDomains.StringValueDomain;
import org.n52.oxf.valueDomains.filter.ComparisonFilter;
import org.n52.oxf.valueDomains.filter.FilterValueDomain;
import org.n52.oxf.valueDomains.filter.IFilter;
import org.n52.oxf.valueDomains.spatial.BoundingBox;
import org.n52.oxf.valueDomains.time.TemporalValueDomain;
import org.n52.oxf.valueDomains.time.TimePeriod;

public class SOSCapabilitiesMapper_200 {

    public ServiceDescriptor mapCapabilities(CapabilitiesDocument capabilitiesDoc) throws OXFException {

        String version = mapVersion(capabilitiesDoc);
        CapabilitiesType capabilities = capabilitiesDoc.getCapabilities();
        ServiceProvider serviceProvider = mapServiceProvider(capabilities);
        OperationsMetadata operationsMetadata = mapOperationsMetadata(capabilities);
        ServiceIdentification serviceIdentification = mapServiceIdentification(capabilities);
        // OperationsMetadata operationsMetadata = null;
        SOSContents contents = mapContents(capabilitiesDoc);

        // addDatasetParameterFromContentsSection(operationsMetadata, contents);

        ServiceDescriptor serviceDesc = new ServiceDescriptor(version,
                                                              serviceIdentification,
                                                              serviceProvider,
                                                              operationsMetadata,
                                                              contents);
        return serviceDesc;
    }

    private void addDatasetParameterFromContentsSection(OperationsMetadata operationsMetadata, SOSContents contents) {
        Operation getObservationOp = operationsMetadata.getOperationByName("GetObservation");

        for (int i = 0; i < contents.getDataIdentificationCount(); i++) {
            ObservationOffering obsOff = contents.getDataIdentification(i);

            //
            // --- id:
            //
            StringValueDomain idDomain = new StringValueDomain(obsOff.getIdentifier());
            DatasetParameter idParam = new DatasetParameter("id",
                                                            false,
                                                            idDomain,
                                                            obsOff,
                                                            Parameter.COMMON_NAME_RESOURCE_ID);
            getObservationOp.addParameter(idParam);

            //
            // --- bbox:
            //
            IBoundingBox[] bboxDomain = obsOff.getBoundingBoxes();
            for (IBoundingBox bbox : bboxDomain) {
                DatasetParameter bboxParam = new DatasetParameter("bbox",
                                                                  false,
                                                                  bbox,
                                                                  obsOff,
                                                                  Parameter.COMMON_NAME_BBOX);
                getObservationOp.addParameter(bboxParam);
            }

            //
            // --- srs:
            //
            if (obsOff.getAvailableCRSs() != null) {
                StringValueDomain srsDomain = new StringValueDomain(obsOff.getAvailableCRSs());
                DatasetParameter srsParam = new DatasetParameter("srs",
                                                                 false,
                                                                 srsDomain,
                                                                 obsOff,
                                                                 Parameter.COMMON_NAME_SRS);
                getObservationOp.addParameter(srsParam);
            }

            //
            // --- time:
            //
            IDiscreteValueDomain<ITime> temporalDomain = obsOff.getTemporalDomain();
            DatasetParameter eventTimeParam = new DatasetParameter("eventTime",
                                                                   false,
                                                                   temporalDomain,
                                                                   obsOff,
                                                                   Parameter.COMMON_NAME_TIME);
            getObservationOp.addParameter(eventTimeParam);
        }
    }

    private OperationsMetadata mapOperationsMetadata(CapabilitiesType capabilities) {

        if ( !capabilities.isSetOperationsMetadata()) {
            return null;
        }

        net.opengis.ows.x11.OperationsMetadataDocument.OperationsMetadata operationsMetadata = capabilities.getOperationsMetadata();

        //
        // map the operations:
        //

        OperationDocument.Operation[] xb_operations = operationsMetadata.getOperationArray();
        Operation[] oc_operations = new Operation[xb_operations.length];
        for (int i = 0; i < xb_operations.length; i++) {
            OperationDocument.Operation xb_operation = xb_operations[i];

            String oc_operationName = xb_operation.getName();

            //
            // map the operations DCPs:
            //

            DCPDocument.DCP[] xb_dcps = xb_operation.getDCPArray();
            DCP[] oc_dcps = new DCP[xb_dcps.length];
            for (int j = 0; j < xb_dcps.length; j++) {
                DCPDocument.DCP xb_dcp = xb_dcps[j];

                //
                // map the RequestMethods:
                //

                List<RequestMethod> oc_requestMethods = new ArrayList<RequestMethod>();

                RequestMethodType[] xb_getRequestMethods = xb_dcp.getHTTP().getGetArray();
                for (int k = 0; k < xb_getRequestMethods.length; k++) {
                    RequestMethodType xb_getRequestMethod = xb_getRequestMethods[k];
                    OnlineResource oc_onlineRessource = new OnlineResource(xb_getRequestMethod.getHref());
                    RequestMethod oc_requestMethod = new GetRequestMethod(oc_onlineRessource);
                    oc_requestMethods.add(oc_requestMethod);
                }

                RequestMethodType[] xb_postRequestMethods = xb_dcp.getHTTP().getPostArray();
                for (int k = 0; k < xb_postRequestMethods.length; k++) {
                    RequestMethodType xb_postRequestMethod = xb_postRequestMethods[k];
                    OnlineResource oc_onlineRessource = new OnlineResource(xb_postRequestMethod.getHref());
                    RequestMethod oc_requestMethod = new PostRequestMethod(oc_onlineRessource);
                    oc_requestMethods.add(oc_requestMethod);
                }

                oc_dcps[j] = new DCP(oc_requestMethods);
            }

            //
            // map the operations parameters:
            //

            DomainType[] xb_parameters = xb_operation.getParameterArray();

            List<Parameter> oc_parameters = new ArrayList<Parameter>();

            for (int j = 0; j < xb_parameters.length; j++) {

                DomainType xb_parameter = xb_parameters[j];

                String parameterName = xb_parameter.getName();

                if (parameterName.equalsIgnoreCase("eventTime")) {
                    // do nothing! because the eventTime Parameter will be added from Contents section
                }
                else {

                    //
                    // map the parameters' values to StringValueDomains
                    //

                    AllowedValues xb_allowedValues = xb_parameter.getAllowedValues();

                    if (xb_allowedValues != null) {
                        ValueType[] xb_values = xb_allowedValues.getValueArray();

                        StringValueDomain oc_values = new StringValueDomain();
                        for (int k = 0; k < xb_values.length; k++) {
                            ValueType xb_value = xb_values[k];

                            oc_values.addPossibleValue(xb_value.getStringValue());
                        }

                        Parameter oc_parameter = new Parameter(parameterName, true, oc_values, null);
                        oc_parameters.add(oc_parameter);
                    }
                }
            }

            Parameter[] parametersArray = new Parameter[oc_parameters.size()];
            oc_parameters.toArray(parametersArray);

            oc_operations[i] = new Operation(oc_operationName, parametersArray, null, oc_dcps);
        }

        return new OperationsMetadata(oc_operations);
    }

    public static void main(String[] args) {
        try {
            HttpClient client = new SimpleHttpClient();
            String request = "http://sensorweb.demo.52north.org/52nSOSv3_200/sos?REQUEST=GetCapabilities&SERVICE=SOS";
            
            HttpResponse response = client.executeGet(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream responseStream = responseEntity.getContent();
            new SOSCapabilitiesMapper_200().mapCapabilities(CapabilitiesDocument.Factory.parse(responseStream));
        }
        catch (HttpClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (XmlException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (OXFException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private SOSContents mapContents(CapabilitiesDocument capabilitiesDoc) throws OXFException {
        CapabilitiesType capabilities = capabilitiesDoc.getCapabilities();
        Contents xb_contents = capabilities.getContents();
        ContentsType contentsType = xb_contents.getContents();
        String[] observablePropertys = contentsType.getObservablePropertyArray();
        String[] responseFormats = contentsType.getResponseFormatArray();
        Offering[] xb_obsOfferings = contentsType.getOfferingArray();
        ArrayList<ObservationOffering> oc_obsOffList = new ArrayList<ObservationOffering>();
        for (int i = 0; i < xb_obsOfferings.length; i++) {
            ObservationOfferingDocument xb_obsOfferingDoc = null;
            try {
                xb_obsOfferingDoc = ObservationOfferingDocument.Factory.parse(xb_obsOfferings[i].newInputStream());
            }
            catch (XmlException e) {
                throw new OXFException("Could not parse DOM node.", e);
            }
            catch (IOException e) {
                throw new OXFException("Could not parse DOM node.", e);
            }
            ObservationOfferingType xb_obsOffering = xb_obsOfferingDoc.getObservationOffering();

            if ( !xb_obsOffering.isSetObservedArea()) {
                continue; // does not contain any observations/features
            }

            // identifier
            String oc_identifier = xb_obsOffering.getIdentifier();

            // title (take the first name or if name does not exist take the id)
            String oc_title;
            CodeType[] xb_names = xb_obsOffering.getNameArray();
            if (xb_names != null && xb_names.length > 0 && xb_names[0].getStringValue() != null) {
                oc_title = xb_names[0].getStringValue();
            }
            else {
                oc_title = xb_obsOffering.getIdentifier();
            }

            IBoundingBox[] oc_bbox = null;
            String[] oc_availabaleCRSs = null;
            EnvelopeDocument envelopeDoc = null;
            try {
                envelopeDoc = EnvelopeDocument.Factory.parse(xb_obsOffering.getObservedArea().newInputStream());
            }
            catch (IOException e) {
                throw new OXFException("Could not get DOM node.", e);
            }
            catch (XmlException e) {
                throw new OXFException("Could not get DOM node.", e);
            }

            EnvelopeType envelope = envelopeDoc.getEnvelope();
            if (envelope != null) {
                // availableCRSs:
                String oc_crs = envelope.getSrsName();
                if (oc_crs != null) {
                    oc_availabaleCRSs = new String[] {oc_crs};
                }
                else {
                    // TODO Think about throwing an exception because of one missing attribute because this
                    // situation can occur often when a new offering is set up and no data is available. Set
                    // it null and then it's okay! (ehjuerrens@52north.org)
                    // throw new
                    // NullPointerException("SRS not specified in the Capabilities' 'gml:Envelope'-tag.");
                }

                // BoundingBox:
                oc_bbox = new IBoundingBox[1];
                List xb_lowerCornerList = envelope.getLowerCorner().getListValue();
                double[] oc_lowerCornerList = new double[xb_lowerCornerList.size()];

                List xb_upperCornerList = envelope.getUpperCorner().getListValue();
                double[] oc_upperCornerList = new double[xb_upperCornerList.size()];

                for (int j = 0; j < xb_lowerCornerList.size(); j++) {
                    oc_lowerCornerList[j] = (Double) xb_lowerCornerList.get(j);
                }
                for (int j = 0; j < xb_upperCornerList.size(); j++) {
                    oc_upperCornerList[j] = (Double) xb_upperCornerList.get(j);
                }

                oc_bbox[0] = new BoundingBox(oc_crs, oc_lowerCornerList, oc_upperCornerList);
            }

            // outputFormats:
            String[] oc_outputFormats = (String[]) ArrayUtils.addAll(responseFormats,
                                                                     xb_obsOffering.getResponseFormatArray());

            // TemporalDomain:
            List<ITime> oc_timeList = new ArrayList<ITime>();
            ResultTime resultTime = xb_obsOffering.getResultTime();
            if (resultTime != null && resultTime.getTimePeriod() != null) {
                TimePeriodType xb_timePeriod = resultTime.getTimePeriod();
                String beginPos = xb_timePeriod.getBeginPosition().getStringValue();
                String endPos = xb_timePeriod.getEndPosition().getStringValue();
                if ( !beginPos.equals("") && !endPos.equals("")) {
                    TimePeriod oc_timePeriod = new TimePeriod(beginPos, endPos);
                    oc_timeList.add(oc_timePeriod);
                }
            }
            IDiscreteValueDomain<ITime> oc_temporalDomain = new TemporalValueDomain(oc_timeList);

            // result:
            FilterValueDomain filterDomain = new FilterValueDomain();
            if (capabilities.getFilterCapabilities() != null) {
                FilterCapabilities filterCaps = capabilities.getFilterCapabilities().getFilterCapabilities();
                if (filterCaps != null) {
                    processScalarFilterCapabilities(filterDomain, filterCaps.getScalarCapabilities());
                    processSpatialFilterCapabilities(filterDomain, filterCaps.getSpatialCapabilities());
                    processTemporalFilterCapabilities(filterDomain, filterCaps.getTemporalCapabilities());
                }
            }

            String[] oc_respModes = xb_obsOffering.getResponseFormatArray();
            String[] oc_obsProps = (String[]) ArrayUtils.addAll(xb_obsOffering.getObservablePropertyArray(),
                                                                observablePropertys);
            String[] oc_procedures = new String[] {xb_obsOffering.getProcedure()};

            RelatedFeature[] oc_foiIDs = xb_obsOffering.getRelatedFeatureArray();
            String[] oc_relatedFeatures = new String[oc_foiIDs.length];
            for (int j = 0; j < oc_foiIDs.length; j++) {
                // FIXME this is 52n SOS 2.0 specific
                oc_relatedFeatures[j] = oc_foiIDs[j].getFeatureRelationship().getTarget().getHref();
            }

            String oc_fees = capabilities.getServiceIdentification().getFees();
            // TODO: these variables should also be initialized
            String oc_pointOfContact = null;
            Locale[] oc_language = null;
            String[] oc_keywords = null;
            String oc_abstractDescription = null;

            ObservationOffering oc_obsOff = new ObservationOffering(oc_title,
                                                                    oc_identifier,
                                                                    oc_bbox,
                                                                    oc_outputFormats,
                                                                    oc_availabaleCRSs,
                                                                    oc_fees,
                                                                    oc_language,
                                                                    oc_pointOfContact,
                                                                    oc_temporalDomain,
                                                                    oc_abstractDescription,
                                                                    oc_keywords,
                                                                    oc_procedures,
                                                                    oc_relatedFeatures,
                                                                    oc_obsProps,
                                                                    null, // not supported in SOS 2.0
                                                                    oc_respModes,
                                                                    filterDomain);

            oc_obsOffList.add(oc_obsOff);
        }

        return new SOSContents(oc_obsOffList);
    }

    private void processScalarFilterCapabilities(FilterValueDomain filterDomain,
                                                 ScalarCapabilitiesType scalarCapabilities) {
        if (scalarCapabilities != null) {
            ComparisonOperatorsType comparisonOperators = scalarCapabilities.getComparisonOperators();
            if (comparisonOperators != null) {
                ComparisonOperatorType[] xb_compOpsArray = comparisonOperators.getComparisonOperatorArray();
                for (ComparisonOperatorType compOp : xb_compOpsArray) {
                    if (compOp.equals(ComparisonOperatorType.EQUAL)) {
                        IFilter filter = new ComparisonFilter(ComparisonFilter.PROPERTY_IS_EQUAL_TO);
                        filterDomain.addPossibleValue(filter);
                    }
                    else if (compOp.equals(ComparisonOperatorType.GREATER_THAN)) {
                        IFilter filter = new ComparisonFilter(ComparisonFilter.PROPERTY_IS_GREATER_THAN);
                        filterDomain.addPossibleValue(filter);
                    }
                    else if (compOp.equals(ComparisonOperatorType.LESS_THAN)) {
                        IFilter filter = new ComparisonFilter(ComparisonFilter.PROPERTY_IS_LESS_THAN);
                        filterDomain.addPossibleValue(filter);
                    }
                    else if (compOp.equals(ComparisonOperatorType.NOT_EQUAL)) {
                        IFilter filter = new ComparisonFilter(ComparisonFilter.PROPERTY_IS_NOT_EQUAL_TO);
                        filterDomain.addPossibleValue(filter);
                    }
                }
            }
        }
    }

    private void processSpatialFilterCapabilities(FilterValueDomain filterDomain,
                                                  SpatialCapabilitiesType spatialCapabilities) {
        if (spatialCapabilities != null) {
            // TODO implement
            // throw new NotImplementedException();
        }
    }

    private void processTemporalFilterCapabilities(FilterValueDomain filterDomain,
                                                   TemporalCapabilitiesType temporalCapabilities) {
        if (temporalCapabilities != null) {
            // TODO implement
            // throw new NotImplementedException();
        }

    }

    private ServiceIdentification mapServiceIdentification(CapabilitiesType capabilities) {

        if ( !capabilities.isSetServiceIdentification()) {
            return null;
        }

        net.opengis.ows.x11.ServiceIdentificationDocument.ServiceIdentification serviceIdentification = capabilities.getServiceIdentification();

        String oc_title = serviceIdentification.getTitleArray(0).getStringValue();
        String oc_serviceType = serviceIdentification.getServiceType().getStringValue();
        String[] oc_serviceTypeVersions = serviceIdentification.getServiceTypeVersionArray();

        String oc_fees = serviceIdentification.getFees();
        String[] oc_accessConstraints = serviceIdentification.getAccessConstraintsArray();
        String oc_abstract = null;
        if (oc_accessConstraints.length != 0) {
            oc_abstract = serviceIdentification.getAbstractArray(0).getStringValue();
        }
        String[] oc_keywords = null;

        Vector<String> oc_keywordsVec = new Vector<String>();
        for (int i = 0; i < serviceIdentification.getKeywordsArray().length; i++) {
            LanguageStringType[] xb_keywords = serviceIdentification.getKeywordsArray(i).getKeywordArray();
            for (LanguageStringType xb_keyword : xb_keywords) {
                oc_keywordsVec.add(xb_keyword.getStringValue());
            }
        }
        oc_keywords = new String[oc_keywordsVec.size()];
        oc_keywordsVec.toArray(oc_keywords);

        return new ServiceIdentification(oc_title,
                                         oc_serviceType,
                                         oc_serviceTypeVersions,
                                         oc_fees,
                                         oc_accessConstraints,
                                         oc_abstract,
                                         oc_keywords);
    }

    private String mapVersion(CapabilitiesDocument capsDoc) {
        return capsDoc.getCapabilities().getVersion();
    }

    private static ServiceProvider mapServiceProvider(CapabilitiesType capabilities) {
        if ( !capabilities.isSetServiceProvider()) {
            return null;
        }
        net.opengis.ows.x11.ServiceProviderDocument.ServiceProvider serviceProvider = capabilities.getServiceProvider();
        String name = serviceProvider.getProviderName();
        ResponsiblePartySubsetType contact = serviceProvider.getServiceContact();

        String individualName = contact.getIndividualName();
        String organisationName = null;
        String positionName = contact.getPositionName();
        ContactType contactInfo = contact.getContactInfo(); // TODO parse info
        Contact info = new Contact(null, null, null, null, null, null);
        ServiceContact serviceContact = new ServiceContact(individualName, organisationName, positionName, info);

        return new ServiceProvider(name, serviceContact);
    }

}