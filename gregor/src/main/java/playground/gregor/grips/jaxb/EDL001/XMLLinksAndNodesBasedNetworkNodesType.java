//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.08.01 at 04:42:06 PM CEST 
//


package playground.gregor.grips.jaxb.EDL001;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for linksAndNodesBasedNetworkNodesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="linksAndNodesBasedNetworkNodesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="node" type="{http://matsim.svn.sourceforge.net/viewvc/matsim/playgrounds/trunk/gregor/xsd}linksAndNodesBasedNetworkNodeType" maxOccurs="unbounded" minOccurs="2"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "linksAndNodesBasedNetworkNodesType", propOrder = {
    "node"
})
public class XMLLinksAndNodesBasedNetworkNodesType {

    @XmlElement(required = true)
    protected List<XMLLinksAndNodesBasedNetworkNodeType> node;

    /**
     * Gets the value of the node property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the node property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNode().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link XMLLinksAndNodesBasedNetworkNodeType }
     * 
     * 
     */
    public List<XMLLinksAndNodesBasedNetworkNodeType> getNode() {
        if (node == null) {
            node = new ArrayList<XMLLinksAndNodesBasedNetworkNodeType>();
        }
        return this.node;
    }

}
