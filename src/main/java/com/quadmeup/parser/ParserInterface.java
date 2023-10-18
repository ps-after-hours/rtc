package com.quadmeup.parser;

import com.quadmeup.dto.DataRecord;
import com.quadmeup.exceptions.MalformedRecordException;

public interface ParserInterface {

    DataRecord parse(String input) throws MalformedRecordException;
}
