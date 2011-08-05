//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.08.04 at 02:05:46 PM CEST 
//


package playground.gregor.grips.jaxb.inspire.commontransportelements;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.datatype.XMLGregorianCalendar;
import net.opengis.gml.v_3_2_1.PointPropertyType;
import playground.gregor.grips.jaxb.inspire.geographicalnames.GeographicalNameType;
import playground.gregor.grips.jaxb.inspire.network.NetworkElementType;


/**
 * <p>Java class for TransportPointType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TransportPointType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:x-inspire:specification:gmlas:Network:3.2}NetworkElementType">
 *       &lt;sequence>
 *         &lt;element name="geographicalName" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;sequence>
 *                   &lt;element ref="{urn:x-inspire:specification:gmlas:GeographicalNames:3.0}GeographicalName"/>
 *                 &lt;/sequence>
 *                 &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="geometry" type="{http://www.opengis.net/gml/3.2}PointPropertyType"/>
 *         &lt;element name="validFrom">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>dateTime">
 *                 &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="validTo" minOccurs="0">
 *           &lt;complexType>
 *             &lt;simpleContent>
 *               &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>dateTime">
 *                 &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
 *               &lt;/extension>
 *             &lt;/simpleContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TransportPointType", propOrder = {
    "geographicalName",
    "geometry",
    "validFrom",
    "validTo"
})
@XmlSeeAlso({
    MarkerPostType.class
})
public abstract class TransportPointType
    extends NetworkElementType
{

    @XmlElementRef(name = "geographicalName", namespace = "urn:x-inspire:specification:gmlas:CommonTransportElements:3.0", type = JAXBElement.class)
    protected JAXBElement<TransportPointType.GeographicalName> geographicalName;
    @XmlElement(required = true)
    protected PointPropertyType geometry;
    @XmlElement(required = true, nillable = true)
    protected TransportPointType.ValidFrom validFrom;
    @XmlElementRef(name = "validTo", namespace = "urn:x-inspire:specification:gmlas:CommonTransportElements:3.0", type = JAXBElement.class)
    protected JAXBElement<TransportPointType.ValidTo> validTo;

    /**
     * Gets the value of the geographicalName property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TransportPointType.GeographicalName }{@code >}
     *     
     */
    public JAXBElement<TransportPointType.GeographicalName> getGeographicalName() {
        return geographicalName;
    }

    /**
     * Sets the value of the geographicalName property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TransportPointType.GeographicalName }{@code >}
     *     
     */
    public void setGeographicalName(JAXBElement<TransportPointType.GeographicalName> value) {
        this.geographicalName = ((JAXBElement<TransportPointType.GeographicalName> ) value);
    }

    public boolean isSetGeographicalName() {
        return (this.geographicalName!= null);
    }

    /**
     * Gets the value of the geometry property.
     * 
     * @return
     *     possible object is
     *     {@link PointPropertyType }
     *     
     */
    public PointPropertyType getGeometry() {
        return geometry;
    }

    /**
     * Sets the value of the geometry property.
     * 
     * @param value
     *     allowed object is
     *     {@link PointPropertyType }
     *     
     */
    public void setGeometry(PointPropertyType value) {
        this.geometry = value;
    }

    public boolean isSetGeometry() {
        return (this.geometry!= null);
    }

    /**
     * Gets the value of the validFrom property.
     * 
     * @return
     *     possible object is
     *     {@link TransportPointType.ValidFrom }
     *     
     */
    public TransportPointType.ValidFrom getValidFrom() {
        return validFrom;
    }

    /**
     * Sets the value of the validFrom property.
     * 
     * @param value
     *     allowed object is
     *     {@link TransportPointType.ValidFrom }
     *     
     */
    public void setValidFrom(TransportPointType.ValidFrom value) {
        this.validFrom = value;
    }

    public boolean isSetValidFrom() {
        return (this.validFrom!= null);
    }

    /**
     * Gets the value of the validTo property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link TransportPointType.ValidTo }{@code >}
     *     
     */
    public JAXBElement<TransportPointType.ValidTo> getValidTo() {
        return validTo;
    }

    /**
     * Sets the value of the validTo property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link TransportPointType.ValidTo }{@code >}
     *     
     */
    public void setValidTo(JAXBElement<TransportPointType.ValidTo> value) {
        this.validTo = ((JAXBElement<TransportPointType.ValidTo> ) value);
    }

    public boolean isSetValidTo() {
        return (this.validTo!= null);
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;sequence>
     *         &lt;element ref="{urn:x-inspire:specification:gmlas:GeographicalNames:3.0}GeographicalName"/>
     *       &lt;/sequence>
     *       &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "geographicalName"
    })
    public static class GeographicalName {

        @XmlElement(name = "GeographicalName", namespace = "urn:x-inspire:specification:gmlas:GeographicalNames:3.0", required = true)
        protected GeographicalNameType geographicalName;
        @XmlAttribute
        protected List<String> nilReason;

        /**
         * Gets the value of the geographicalName property.
         * 
         * @return
         *     possible object is
         *     {@link GeographicalNameType }
         *     
         */
        public GeographicalNameType getGeographicalName() {
            return geographicalName;
        }

        /**
         * Sets the value of the geographicalName property.
         * 
         * @param value
         *     allowed object is
         *     {@link GeographicalNameType }
         *     
         */
        public void setGeographicalName(GeographicalNameType value) {
            this.geographicalName = value;
        }

        public boolean isSetGeographicalName() {
            return (this.geographicalName!= null);
        }

        /**
         * Gets the value of the nilReason property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the nilReason property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getNilReason().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getNilReason() {
            if (nilReason == null) {
                nilReason = new ArrayList<String>();
            }
            return this.nilReason;
        }

        public boolean isSetNilReason() {
            return ((this.nilReason!= null)&&(!this.nilReason.isEmpty()));
        }

        public void unsetNilReason() {
            this.nilReason = null;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>dateTime">
     *       &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class ValidFrom {

        @XmlValue
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar value;
        @XmlAttribute
        protected List<String> nilReason;

        /**
         * Gets the value of the value property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setValue(XMLGregorianCalendar value) {
            this.value = value;
        }

        public boolean isSetValue() {
            return (this.value!= null);
        }

        /**
         * Gets the value of the nilReason property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the nilReason property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getNilReason().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getNilReason() {
            if (nilReason == null) {
                nilReason = new ArrayList<String>();
            }
            return this.nilReason;
        }

        public boolean isSetNilReason() {
            return ((this.nilReason!= null)&&(!this.nilReason.isEmpty()));
        }

        public void unsetNilReason() {
            this.nilReason = null;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;simpleContent>
     *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>dateTime">
     *       &lt;attribute name="nilReason" type="{http://www.opengis.net/gml/3.2}NilReasonType" />
     *     &lt;/extension>
     *   &lt;/simpleContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "value"
    })
    public static class ValidTo {

        @XmlValue
        @XmlSchemaType(name = "dateTime")
        protected XMLGregorianCalendar value;
        @XmlAttribute
        protected List<String> nilReason;

        /**
         * Gets the value of the value property.
         * 
         * @return
         *     possible object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public XMLGregorianCalendar getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link XMLGregorianCalendar }
         *     
         */
        public void setValue(XMLGregorianCalendar value) {
            this.value = value;
        }

        public boolean isSetValue() {
            return (this.value!= null);
        }

        /**
         * Gets the value of the nilReason property.
         * 
         * <p>
         * This accessor method returns a reference to the live list,
         * not a snapshot. Therefore any modification you make to the
         * returned list will be present inside the JAXB object.
         * This is why there is not a <CODE>set</CODE> method for the nilReason property.
         * 
         * <p>
         * For example, to add a new item, do as follows:
         * <pre>
         *    getNilReason().add(newItem);
         * </pre>
         * 
         * 
         * <p>
         * Objects of the following type(s) are allowed in the list
         * {@link String }
         * 
         * 
         */
        public List<String> getNilReason() {
            if (nilReason == null) {
                nilReason = new ArrayList<String>();
            }
            return this.nilReason;
        }

        public boolean isSetNilReason() {
            return ((this.nilReason!= null)&&(!this.nilReason.isEmpty()));
        }

        public void unsetNilReason() {
            this.nilReason = null;
        }

    }

}
