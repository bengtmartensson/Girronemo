/*
Copyright (C) 2021 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.girronimo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class Girronimo {
    protected static final String DEFAULT_CHARSET = "US-ASCII";

    private final static char[][] translateTable = new char[][] {
        { '\u2013', '-' },
        { '\uff0b', '+' },
        { '\u201c', '\"' },
        { '\u201d', '\"' },
        { '\u2019', '\'' }
    };

    protected static String replaceFunnyChars(String s) {
        String result = s;
        for (char[] pair : translateTable) {
            char from = pair[0];
            if (s.indexOf(from) != -1)
                result = result.replace(from, pair[1]);
        }
        return result;
    }

    protected final Document document;
    protected final NamespaceContext resolver;
    private final XPathFactory xpathFactory;
    private final String title;
    protected final Decoder decoder;
    protected RemoteSet remoteSet;

    protected Girronimo(String filename) throws IOException, SAXException, IrpParseException {
        decoder = new Decoder();
        title = "Girronimofied version of " + filename;
        document = XmlUtils.openXmlFile(new File(filename));
        resolver = new MyResolver(document);
        xpathFactory = XPathFactory.newInstance();
    }

    protected Document toDocument() {
        return remoteSet.toDocument(title, false, true, false, false);
    }

    protected void print(OutputStream ostr, String encoding) throws UnsupportedEncodingException {
        XmlUtils.printDOM(ostr,toDocument(), encoding);
    }

    protected void print(String out, String charSet) throws FileNotFoundException, UnsupportedEncodingException {
        print(IrCoreUtils.getPrintStream(out), charSet);
    }

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    protected void print() {
        try {
            print(System.out, DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException ex) {
        }
    }

    protected Object evalXPath(Node node, String xpath, QName returnType) throws XPathExpressionException {
        XPath xpather = xpathFactory.newXPath();
        xpather.setNamespaceContext(resolver);
        XPathExpression xpathExpression = xpather.compile(xpath);
        return xpathExpression.evaluate(node, XPathConstants.NODESET);
    }

    protected NodeList evalXPath(Node node, String xpath) throws XPathExpressionException {
        return (NodeList) evalXPath(node, xpath, XPathConstants.NODESET);
    }

    protected NodeList getElements(Element element, String qualifiedName) {
        String[] arr = qualifiedName.split(":");
        String namespaceURI = resolver.getNamespaceURI(arr[0]);
        String localName = arr[1];
        return element.getElementsByTagNameNS(namespaceURI, localName);
    }
}
