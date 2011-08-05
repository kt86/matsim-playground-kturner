//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.08.01 at 04:42:06 PM CEST 
//


package playground.gregor.grips.jaxb.EDL001;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for networkType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="networkType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SRS" type="{http://matsim.svn.sourceforge.net/viewvc/matsim/playgrounds/trunk/gregor/xsd}SRSType"/>
 *         &lt;choice>
 *           &lt;element name="linksBasedNetwork" type="{http://matsim.svn.sourceforge.net/viewvc/matsim/playgrounds/trunk/gregor/xsd}linksBasedNetworkType"/>
 *           &lt;element name="linksAndNodesBasedNetwork" type="{http://matsim.svn.sourceforge.net/viewvc/matsim/playgrounds/trunk/gregor/xsd}linksAndNodesBasedNetworkType"/>
 *           &lt;element name="osmBasedNetwork" type="{http://matsim.svn.sourceforge.net/viewvc/matsim/playgrounds/trunk/gregor/xsd}osmBasedNetworkType"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "networkType", propOrder = {
    "srs",
    "linksBasedNetwork",
    "linksAndNodesBasedNetwork",
    "osmBasedNetwork"
})
public abstract class XMLNetworkType {

    @XmlElement(name = "SRS", required = true)
    protected XMLSRSType srs;
    protected XMLLinksBasedNetworkType linksBasedNetwork;
    protected XMLLinksAndNodesBasedNetworkType linksAndNodesBasedNetwork;
    protected XMLOsmBasedNetworkType osmBasedNetwork;

    /**
     * Gets the value of the srs property.
     * 
     * @return
     *     possible object is
     *     {@link XMLSRSType }
     *     
     */
    public XMLSRSType getSRS() {
        return srs;
    }

    /**
     * Sets the value of the srs property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLSRSType }
     *     
     */
    public void setSRS(XMLSRSType value) {
        this.srs = value;
    }

    /**
     * Gets the value of the linksBasedNetwork property.
     * 
     * @return
     *     possible object is
     *     {@link XMLLinksBasedNetworkType }
     *     
     */
    public XMLLinksBasedNetworkType getLinksBasedNetwork() {
        return linksBasedNetwork;
    }

    /**
     * Sets the value of the linksBasedNetwork property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLLinksBasedNetworkType }
     *     
     */
    public void setLinksBasedNetwork(XMLLinksBasedNetworkType value) {
        this.linksBasedNetwork = value;
    }

    /**
     * Gets the value of the linksAndNodesBasedNetwork property.
     * 
     * @return
     *     possible object is
     *     {@link XMLLinksAndNodesBasedNetworkType }
     *     
     */
    public XMLLinksAndNodesBasedNetworkType getLinksAndNodesBasedNetwork() {
        return linksAndNodesBasedNetwork;
    }

    /**
     * Sets the value of the linksAndNodesBasedNetwork property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLLinksAndNodesBasedNetworkType }
     *     
     */
    public void setLinksAndNodesBasedNetwork(XMLLinksAndNodesBasedNetworkType value) {
        this.linksAndNodesBasedNetwork = value;
    }

    /**
     * Gets the value of the osmBasedNetwork property.
     * 
     * @return
     *     possible object is
     *     {@link XMLOsmBasedNetworkType }
     *     
     */
    public XMLOsmBasedNetworkType getOsmBasedNetwork() {
        return osmBasedNetwork;
    }

    /**
     * Sets the value of the osmBasedNetwork property.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLOsmBasedNetworkType }
     *     
     */
    public void setOsmBasedNetwork(XMLOsmBasedNetworkType value) {
        this.osmBasedNetwork = value;
    }

}
