package com.quadmeup.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quadmeup.dto.DataRecord;
import com.quadmeup.exceptions.MalformedRecordException;

public class JsonParser implements ParserInterface {

    @Override
    public DataRecord parse(String input) throws MalformedRecordException {

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(input, DataRecord.class);
        } catch (Exception e) {
            throw new MalformedRecordException();
        } 
    }
}
