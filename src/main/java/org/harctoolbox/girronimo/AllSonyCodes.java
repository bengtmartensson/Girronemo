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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPathExpressionException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.Remote.MetaData;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.irp.Decoder;
import org.harctoolbox.irp.IrpParseException;
import static org.harctoolbox.xml.XmlUtils.ENGLISH;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class AllSonyCodes extends Girronimo {

    private static final String FILENAME = "src/main/fods/All Sony Codes V1.0.fods";
    private static final String DATE_FORMATSTRING = "yyyy-MM-dd_HH:mm:ss";
    private static final int INITIAL_CAPACITY = 200;

    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void main(String[] args) {
        try {
            String outfile = args.length >= 1 ? args[0] : "All_Sony_Codes V1.0.girr";
            String encoding = args.length >= 2 ? args[1] : DEFAULT_CHARSET;
            AllSonyCodes groker = new AllSonyCodes(FILENAME);
            groker.print(outfile, encoding);
            System.out.println(outfile + " successfully written");
        } catch (IOException | SAXException | XPathExpressionException | IrpParseException | Pronto.NonProntoFormatException | InvalidArgumentException | GirrException ex) {
            Logger.getLogger(AllSonyCodes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private final Map<String, Element> pageIndex = new LinkedHashMap<>(INITIAL_CAPACITY);
    private final Map<String, Remote> remotes = new LinkedHashMap<>(INITIAL_CAPACITY);

    private AllSonyCodes(String filename) throws IOException, SAXException, XPathExpressionException, IrpParseException, Pronto.NonProntoFormatException, InvalidArgumentException, GirrException {
        super(filename);
        setupIndex();
        NodeList coverPageRows = getElements(pageIndex.get("Cover Page"), "table:table-row");
        int length = coverPageRows.getLength();
        Map<String, List<CommandSet>> commandSetsMap = new LinkedHashMap<>(INITIAL_CAPACITY);
        for (int i = 1; i < length; i++) { // First is column names, so ignore it
            Element row = (Element) coverPageRows.item(i);
            String visible = row.getAttributeNS(resolver.getNamespaceURI("table"), "visibility");
            if (visible.equals("collapse"))
                continue;

            NodeList cells = getElements(row, "table:table-cell");
            if (cells.getLength() < 5)
                continue;

            String devNumber = getElements((Element) cells.item(0), "text:a").item(0).getTextContent().trim();
            String device = getElements((Element) cells.item(1), "text:p").item(0).getTextContent().trim();
            //int bits = Integer.parseInt(getElements((Element) cells.item(2), "text:p").item(0).getTextContent());
            NodeList commentNodes = getElements((Element) cells.item(3), "text:p");
            String comment = commentNodes.getLength() > 0 ? commentNodes.item(0).getTextContent() : "";
            CommandSet commandSetMap = grokPage(devNumber, comment); //, device, bits, comment);
            if (commandSetMap.isEmpty())
                continue;

            List<CommandSet> list = commandSetsMap.get(device);
            if (list == null) {
                list = new ArrayList<>(4);
                commandSetsMap.put(device, list);
            }
            list.add(commandSetMap);
        }

        for (Map.Entry<String, List<CommandSet>> kvp : commandSetsMap.entrySet()) {
            String name = kvp.getKey();
            List<CommandSet> commandSets = kvp.getValue();
            MetaData metaData = new MetaData(name);
            Remote remote = new Remote(metaData, null, null, commandSets, null);
            remotes.put(name, remote);
        }
        String creationDate = new SimpleDateFormat(DATE_FORMATSTRING).format(new Date());
        Map<String, String> notes = null;
        String tool = "Girronimo";
        remoteSet = new RemoteSet(System.getProperty("user.name"), filename, creationDate, tool, null, null, null, notes, remotes);
    }

    private void setupIndex() {
        NodeList nodeList = getElements(document.getDocumentElement(), "table:table");
        int length = nodeList.getLength();
        String tableNS = resolver.getNamespaceURI("table");
        for (int i = 0; i < length; i++) {
            Element el = (Element) nodeList.item(i);
            String name = el.getAttributeNS(tableNS, "name");
            pageIndex.put(name, el);
        }
    }

    private CommandSet grokPage(String devNumber, String comment) throws Pronto.NonProntoFormatException, InvalidArgumentException, GirrException {
        Element page = pageIndex.get(devNumber);
        NodeList rows = getElements(page, "table:table-row");
        int length = rows.getLength();
        Map<String, Command> commands = new LinkedHashMap<>(length);

        for (int i = 0; i < length; i++) {
            Command command = grokRow((Element) rows.item(i));
            if (command != null)
                commands.put(command.getName(), command);
        }
        Map<String, String> notes = null;
        if (!comment.isEmpty()) {
            notes = new HashMap<>(1);
            notes.put(ENGLISH, comment);
        }
        return new CommandSet(devNumber, notes, commands, null, null);
    }

    private Command grokRow(Element row) throws Pronto.NonProntoFormatException, InvalidArgumentException, GirrException {
        NodeList cells = getElements(row, "table:table-cell");
        if (cells.getLength() < 6)
            return null;

        Element prontoCell = (Element) cells.item(5);
        NodeList nl = getElements(prontoCell, "text:p");
        if (nl.getLength() < 1)
            return null;
        String pronto = nl.item(0).getTextContent();
        if (pronto.length() < 20)
            return null;
        IrSignal irSignal = Pronto.parse(pronto);
        Decoder.SimpleDecodesSet decodes = decoder.decodeIrSignal(irSignal);
        Decoder.Decode decode = decodes.first();
        NodeList pList = getElements((Element) cells.item(4), "text:p");
        if (pList.getLength() == 0)
            return null;
        String name = replaceFunnyChars(pList.item(0).getTextContent());
        Command command = new Command(name, null, decode.getName(), decode.getMap(), true);
        return command;
    }
}
