package com.quadmeup.parser;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.util.PropertySource.Util;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.quadmeup.dto.DataRecord;
import com.quadmeup.exceptions.MalformedRecordException;

public class XmlParser implements ParserInterface {

    private static final String XML_ID_TAG = "id";
    private static final String XML_VALUE_ATTRIBUTE = "value";
    private static final String XML_DONE_ATTRIBUTE = "done";


    @Override
    public DataRecord parse(String input) throws MalformedRecordException {
         try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(input));
            Document document = builder.parse(is);

            NodeList idNodes = document.getElementsByTagName(XML_ID_TAG);
            
            DataRecord dataRecord = new DataRecord();

            NodeList doneNodes = document.getElementsByTagName(XML_DONE_ATTRIBUTE);

            if (doneNodes.getLength() > 0) {
                dataRecord.setId(null);
                dataRecord.setStatus("done");

                return dataRecord;
            } else if (idNodes.getLength() > 0) {
                Node idNode = idNodes.item(0);
                NamedNodeMap attributes = idNode.getAttributes();
                Node valueAttribute = attributes.getNamedItem(XML_VALUE_ATTRIBUTE);

                dataRecord.setId(valueAttribute.getNodeValue());
                dataRecord.setStatus("ok");

                return dataRecord;
            }

            throw new MalformedRecordException();

        } catch (Exception e) {
            throw new MalformedRecordException();
        }

    }
    


}
