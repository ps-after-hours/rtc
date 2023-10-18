package com.quadmeup;

import java.util.Optional;
import java.util.Random;

import com.quadmeup.enums.SinkType;
import com.quadmeup.enums.SourceType;
import com.quadmeup.exceptions.DoneException;
import com.quadmeup.parser.JsonParser;
import com.quadmeup.parser.XmlParser;

public class TaskProvider {
    
    private static final String BASE_URL = Optional.ofNullable(System.getenv("BASE_URL")).orElse("http://localhost:7299");
    private static final String SOURCE_A_URL = BASE_URL + "/source/a";
    private static final String SOURCE_B_URL = BASE_URL + "/source/b";
    private static final String SINK_URL = BASE_URL + "/sink/a";


    private SampleStorage sampleStorage;
    private Random random;

    public TaskProvider(final SampleStorage sampleStorage) {
        this.sampleStorage = sampleStorage;
        this.random = new Random();

        //Let's ensure we unlock both sources
        sampleStorage.unlock(SourceType.A);
        sampleStorage.unlock(SourceType.B);


    }

    private SourceType getRandom() {
        return random.nextBoolean() ? SourceType.A : SourceType.B;
    }

    private SourceType getWithLockStatus() {

        boolean selected = false;
        SourceType sourceType = null;

        while (!selected) {
            sourceType = getRandom();

            if (!sampleStorage.isLocked(sourceType)) {
                selected = true;
            }

            if (sampleStorage.isDone(sourceType)) {
                //This looks strange for only 2 sources, but if we had more, this would look much better
                selected = false;
            }
        }

        //We shuld release the lock for the other source now
        if (sourceType == SourceType.A) {
            sampleStorage.unlock(SourceType.B);
        } else {
            sampleStorage.unlock(SourceType.A);
        }

        return sourceType;
    }

    public AcquisitionTask getSourceTask() throws DoneException {
        
        if (sampleStorage.isDone(SourceType.A) && sampleStorage.isDone(SourceType.B)) {
            //We are done, no more tasks
            throw new DoneException();
        }

        final SourceType sourceType = getWithLockStatus();

        //This can be done in a more flexible way.
        if (SourceType.A == sourceType) {
            return new AcquisitionTask(sampleStorage, SOURCE_A_URL, SourceType.A, new JsonParser());
        } else {
            return new AcquisitionTask(sampleStorage, SOURCE_B_URL, SourceType.B, new XmlParser());
        }
        
    }

    public SinkTask getSinkTask(SinkType sinkType, String id) {
        return new SinkTask(SINK_URL, sinkType, sampleStorage, id);
    }

}
