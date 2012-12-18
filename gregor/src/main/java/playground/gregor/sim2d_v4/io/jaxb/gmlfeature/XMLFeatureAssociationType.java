//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.12.18 at 02:24:41 PM CET 
//


package playground.gregor.sim2d_v4.io.jaxb.gmlfeature;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment02.XMLFeatureCollectionType;
import playground.gregor.sim2d_v4.io.jaxb.sim2denvironment02.XMLSim2DEnvironmentSectionType;
import playground.gregor.sim2d_v4.io.jaxb.xlink.XMLActuateType;
import playground.gregor.sim2d_v4.io.jaxb.xlink.XMLShowType;
import playground.gregor.sim2d_v4.io.jaxb.xlink.XMLTypeType;


/**
 * 
 *         An instance of this type (e.g. a featureMember) can either 
 *         enclose or point to a feature (or feature collection); this 
 *         type can be restricted in an application schema to allow only 
 *         specified features as valid participants in the association. 
 *         When serving as a simple link that references a remote feature 
 *         instance, the value of the gml:remoteSchema attribute can be 
 *         used to locate a schema fragment that constrains the target 
 *         instance.
 *       
 * 
 * <p>Java class for FeatureAssociationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FeatureAssociationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;element ref="{http://www.opengis.net/gml}_Feature"/>
 *       &lt;/sequence>
 *       &lt;attGroup ref="{http://www.w3.org/1999/xlink}simpleAttrs"/>
 *       &lt;attribute ref="{http://www.opengis.net/gml}remoteSchema"/>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FeatureAssociationType", propOrder = {
    "feature"
})
public class XMLFeatureAssociationType {

    @XmlElementRef(name = "_Feature", namespace = "http://www.opengis.net/gml", type = JAXBElement.class)
    protected JAXBElement<? extends XMLAbstractFeatureType> feature;
    @XmlAttribute(namespace = "http://www.opengis.net/gml")
    @XmlSchemaType(name = "anyURI")
    protected String remoteSchema;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected XMLTypeType type;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected String href;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected String role;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected String arcrole;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected String title;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected XMLShowType show;
    @XmlAttribute(namespace = "http://www.w3.org/1999/xlink")
    protected XMLActuateType actuate;

    /**
     * Gets the value of the feature property.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link XMLSim2DEnvironmentSectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLAbstractFeatureCollectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLFeatureCollectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLAbstractFeatureType }{@code >}
     *     
     */
    public JAXBElement<? extends XMLAbstractFeatureType> getFeature() {
        return feature;
    }

    /**
     * Sets the value of the feature property.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link XMLSim2DEnvironmentSectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLAbstractFeatureCollectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLFeatureCollectionType }{@code >}
     *     {@link JAXBElement }{@code <}{@link XMLAbstractFeatureType }{@code >}
     *     
     */
    public void setFeature(JAXBElement<? extends XMLAbstractFeatureType> value) {
        this.feature = ((JAXBElement<? extends XMLAbstractFeatureType> ) value);
    }

    /**
     * Gets the value of the remoteSchema property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRemoteSchema() {
        return remoteSchema;
    }

    /**
     * Sets the value of the remoteSchema property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRemoteSchema(String value) {
        this.remoteSchema = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link XMLTypeType }
     *     
     */
    public XMLTypeType getType() {
        if (type == null) {
            return XMLTypeType.SIMPLE;
        } else {
            return type;
        }
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLTypeType }
     *     
     */
    public void setType(XMLTypeType value) {
        this.type = value;
    }

    /**
     * Gets the value of the href property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHref() {
        return href;
    }

    /**
     * Sets the value of the href property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHref(String value) {
        this.href = value;
    }

    /**
     * Gets the value of the role property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the value of the role property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRole(String value) {
        this.role = value;
    }

    /**
     * Gets the value of the arcrole property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getArcrole() {
        return arcrole;
    }

    /**
     * Sets the value of the arcrole property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setArcrole(String value) {
        this.arcrole = value;
    }

    /**
     * Gets the value of the title property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the value of the title property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTitle(String value) {
        this.title = value;
    }

    /**
     * Gets the value of the show property.
     * 
     * @return
     *     possible object is
     *     {@link XMLShowType }
     *     
     */
    public XMLShowType getShow() {
        return show;
    }

    /**
     * Sets the value of the show property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLShowType }
     *     
     */
    public void setShow(XMLShowType value) {
        this.show = value;
    }

    /**
     * Gets the value of the actuate property.
     * 
     * @return
     *     possible object is
     *     {@link XMLActuateType }
     *     
     */
    public XMLActuateType getActuate() {
        return actuate;
    }

    /**
     * Sets the value of the actuate property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLActuateType }
     *     
     */
    public void setActuate(XMLActuateType value) {
        this.actuate = value;
    }

}
