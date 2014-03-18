package uk.gov.gds.performance.collector;

import com.equalexperts.logging.LogMessageContractTest;

import static com.equalexperts.logging.EnumContractRunner.EnumToTest;

@EnumToTest(value= CollectorLogMessage.class)
public class CollectorLogMessageTest extends LogMessageContractTest<CollectorLogMessage> {
}