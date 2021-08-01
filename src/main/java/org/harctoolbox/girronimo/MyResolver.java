package org.harctoolbox.girronimo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import javax.xml.namespace.NamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class MyResolver implements NamespaceContext {

    Map<String, String> prefixNamespace;
    Map<String, String> namespacePrefix;

    MyResolver() {
        prefixNamespace = new HashMap<>(0);
        namespacePrefix = new HashMap<>(0);
    }

    MyResolver(Document document) {
        NamedNodeMap attrs = document.getDocumentElement().getAttributes();
        prefixNamespace = new HashMap<>(attrs.getLength());
        namespacePrefix = new HashMap<>(attrs.getLength());

        // If there is a default, non-null namespace, assign the prefix "default" to it.
        String defaultNsAttribute = document.getDocumentElement().getAttribute(XMLNS_ATTRIBUTE);
        if (!defaultNsAttribute.isEmpty()) {
            prefixNamespace.put("default", defaultNsAttribute);
            namespacePrefix.put(defaultNsAttribute, "default");
        }

        for (int i = 0; i < attrs.getLength(); i++) {
            Node att = attrs.item(i);
            String namespaceURI = att.getNamespaceURI();
            if (namespaceURI != null && namespaceURI.equals(XMLNS_ATTRIBUTE_NS_URI)) {
                prefixNamespace.put(att.getLocalName(), att.getNodeValue());
                namespacePrefix.put(att.getNodeValue(), att.getLocalName());
            }
        }
    }

    @Override
    public String getNamespaceURI(String prefix) {
        return prefixNamespace.get(prefix);
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return namespacePrefix.get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return prefixNamespace.keySet().iterator();
    }
}
